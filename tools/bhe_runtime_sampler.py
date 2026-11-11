"""Identity-checked, read-only BSDX RPM sampler; never launches or controls a game.

Counter is the fixed replay consumer's counter, NOT a native input-frame number.
Values are observations guarded by re-reads, not an atomic process snapshot.
"""
import sys
sys.dont_write_bytecode = True

import argparse
import ctypes
from ctypes import wintypes
import json
import os
from pathlib import Path
import re
import struct
import time

from bhe_runtime_session import (RUNTIME_ROOT, OwnedProcess, case_path,
                                 check_no_links, file_record, utc_now)

EXPECTED_SHA = "1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae"
PROCESS_ACCESS = 0x0010 | 0x1000  # VM_READ | QUERY_LIMITED_INFORMATION, only.
REPLAY = 0x86F700  # Object, never dereference this address as a pointer.
TICK, PLAYER = 0x879A5C, 0x86CF88
MAX_RING = 4096
MAX_ROWS = 10000
MAX_ERRORS = 2000
MAX_BYTES = 32 * 1024 * 1024
POLL_SECONDS = .005
WATCHED = {(1, 105), (1, 215), (1, 216), (1, 217), (7, 323)}


class SampleRejected(RuntimeError):
    pass


class CaseStateError(ValueError):
    """State-file I/O/validation failure; does not imply process death."""


def read_case_state(path):
    """Short-lived, bounded state read; retry only Windows OPEN sharing denials."""
    try:
        path = check_no_links(path)
        deadline = time.monotonic() + 1.0
        for attempt in range(41):
            try:
                stream = path.open("rb")
                break
            except OSError as exc:
                transient = (getattr(exc, "winerror", None) in (5, 32, 33) or
                             isinstance(exc, PermissionError) and exc.errno == 13)
                if os.name != "nt" or not transient or attempt == 40:
                    raise
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise
                time.sleep(min(.025, remaining))
        with stream:
            payload = stream.read(1024 * 1024 + 1)
        if len(payload) > 1024 * 1024:
            raise ValueError("Oversize case state")
        state = json.loads(payload.decode("utf-8"))
        if not isinstance(state, dict):
            raise ValueError("Case state must be a JSON object")
        return state
    except (OSError, ValueError) as exc:
        raise CaseStateError(f"Case state read failed: {exc}") from exc

def pointer(value):
    if not isinstance(value, int) or not 0x10000 <= value <= 0xFFFFFFFC or value % 4:
        raise SampleRejected(f"Invalid 32-bit pointer: {value!r}")
    return value


class Reader:
    def __init__(self, read):
        self._read = read

    def read(self, address, size):
        if not 0 <= address <= 0xFFFFFFFF or not 0 < size <= 0x10000 or address + size > 0x100000000:
            raise ValueError("Read outside bounded 32-bit address space")
        result = self._read(address, size)
        if len(result) != size:
            raise OSError(f"Short read at {address:#x}: {len(result)}/{size}")
        return result

    def u32(self, address):
        return struct.unpack("<I", self.read(address, 4))[0]

    def i32(self, address):
        return struct.unpack("<i", self.read(address, 4))[0]

    def u8(self, address):
        return self.read(address, 1)[0]


def require_running_state(state, case):
    if (state.get("case") != str(case) or state.get("status") != "running" or
            state.get("terminal") is True or state.get("owned_process_exit_confirmed") is True or
            state.get("exit_code") is not None):
        raise CaseStateError("Case identity mismatch or non-running/terminal case")


def load_case(case_arg):
    raw = str(case_arg)
    if re.fullmatch(r"[A-Za-z0-9_-]+", raw):
        case = case_path(RUNTIME_ROOT, raw)
    else:
        case = check_no_links(Path(raw))
        if case != case_path(RUNTIME_ROOT, case.name):
            raise ValueError("Case must be a direct child of repository runtime root")
    if not case.is_dir():
        raise ValueError("Case directory does not exist")
    state = read_case_state(case / "state.json")
    require_running_state(state, case)
    identity = state.get("process", {})
    for key in ("pid", "creation_filetime"):
        if type(identity.get(key)) is not int or not 0 < identity[key] <= (0xFFFFFFFF if key == "pid" else 0xFFFFFFFFFFFFFFFF):
            raise ValueError(f"Missing/invalid process {key}")
    image = check_no_links(identity.get("image", ""))
    if image != check_no_links(case / "game/BaldrSky.exe") or not image.is_file():
        raise ValueError("Unexpected case executable path")
    if file_record(image)["sha256"] != EXPECTED_SHA:
        raise ValueError("Unexpected executable SHA256")
    return case, identity


def output_path(case, name):
    case = check_no_links(case)
    if case != case_path(RUNTIME_ROOT, case.name):
        raise ValueError("Output case outside runtime root")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_-]{0,119}\.json", name):
        raise ValueError("Output must be a plain ASCII .json basename")
    stem = name[:-5].upper()
    if stem in {"CON", "PRN", "AUX", "NUL", *[f"COM{i}" for i in range(10)],
                *[f"LPT{i}" for i in range(10)], "STATE", "MANIFEST"}:
        raise ValueError("Reserved output basename")
    result = check_no_links(case / name)
    if result.parent != case:
        raise ValueError("Output escaped case")
    return result


def write_exclusive(path, value, max_bytes=MAX_BYTES):
    path = check_no_links(path)
    # Revalidate even if invoked without the CLI's earlier path check.
    if path != output_path(path.parent, path.name):
        raise ValueError("Unsafe output path")
    payload = (json.dumps(value, ensure_ascii=False, indent=2) + "\n").replace("\n", "\r\n").encode("utf-8")
    if len(payload) > max_bytes:
        raise ValueError("Output byte limit exceeded; no file created")
    with path.open("xb") as stream:
        stream.write(payload)


def verify_identity(expected, actual):
    for key in ("pid", "creation_filetime"):
        if actual.get(key) != expected.get(key):
            raise ValueError(f"Live process {key} mismatch")
    if check_no_links(actual.get("image", "")) != check_no_links(expected.get("image", "")):
        raise ValueError("Live process image mismatch")


class NativeProcess:
    """One RPM handle; no write/injection/process-control capabilities."""
    def __init__(self, expected, kernel=None):
        self.expected = expected
        self.kernel = kernel
        self.process_handle = None
        # Reuse session's identity helper, with irrelevant thread metadata ignored.
        self.tid = expected.get("thread_id")

    def __enter__(self):
        if self.kernel is None:
            self.kernel = ctypes.WinDLL("kernel32", use_last_error=True)
            signatures = {
                "OpenProcess": ([wintypes.DWORD, wintypes.BOOL, wintypes.DWORD], wintypes.HANDLE),
                "CloseHandle": ([wintypes.HANDLE], wintypes.BOOL),
                "GetProcessId": ([wintypes.HANDLE], wintypes.DWORD),
                "GetProcessTimes": ([wintypes.HANDLE] + [ctypes.POINTER(wintypes.FILETIME)] * 4, wintypes.BOOL),
                "QueryFullProcessImageNameW": ([wintypes.HANDLE, wintypes.DWORD, wintypes.LPWSTR,
                                                ctypes.POINTER(wintypes.DWORD)], wintypes.BOOL),
                "GetExitCodeProcess": ([wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD)], wintypes.BOOL),
                "ReadProcessMemory": ([wintypes.HANDLE, ctypes.c_void_p, ctypes.c_void_p,
                                       ctypes.c_size_t, ctypes.POINTER(ctypes.c_size_t)], wintypes.BOOL),
            }
            for name, (args, result) in signatures.items():
                function = getattr(self.kernel, name)
                function.argtypes, function.restype = args, result
        self.process_handle = self.kernel.OpenProcess(PROCESS_ACCESS, False, self.expected["pid"])
        if not self.process_handle:
            raise ctypes.WinError(ctypes.get_last_error())
        try:
            verify_identity(self.expected, self.current_identity())
        except BaseException:
            self.__exit__(None, None, None)
            raise
        return self

    current_identity = OwnedProcess.current_identity

    def require_live_identity(self):
        verify_identity(self.expected, self.current_identity())
        exit_code = wintypes.DWORD()
        if not self.kernel.GetExitCodeProcess(self.process_handle, ctypes.byref(exit_code)):
            raise ctypes.WinError(ctypes.get_last_error())
        if exit_code.value != 259:
            raise RuntimeError(f"Process exited: {exit_code.value}")

    def read(self, address, size):
        buffer, count = ctypes.create_string_buffer(size), ctypes.c_size_t()
        if not self.kernel.ReadProcessMemory(self.process_handle, ctypes.c_void_p(address), buffer,
                                             size, ctypes.byref(count)) or count.value != size:
            raise OSError(f"RPM failed/short read at {address:#x}: {count.value}/{size}")
        return buffer.raw

    def __exit__(self, *unused):
        if self.process_handle:
            self.kernel.CloseHandle(self.process_handle)
            self.process_handle = None


# Offsets confirmed against bhe_runtime_observer.py and the 2026-11-06 sampler.
ACTOR_FIELDS = {
    "activeWeapon": (0x18DC, "i"), "group": (0x15B8, "I"), "skill": (0x15BC, "I"),
    "entry": (0x15C0, "I"), "block": (0x15C4, "I"), "clock": (0x1480, "I"),
    "state": (0x188C, "I"), "control": (0x18EC, "i"), "pendingReaction": (0x18F0, "i"),
    "reaction": (0x18F4, "i"), "nextState": (0x1890, "i"), "hp": (0x161C, "i"),
    "maxHp": (0x1618, "i"), "heatRaw": (0x1910, "i"), "capacityRaw": (0x1914, "i"),
    # 61E310/6221E0: zero heat and replay input do not prove actor readiness.
    "heatState": (0x18E8, "i"), "hitstop": (0x160C, "i"),
    "actualEdge": (0x15AC, "I"), "actualHeld": (0x15B0, "I"),
    "normalGate": (0x1900, "i"), "sdGate": (0x1904, "i"), "sdBudgetRaw": (0x1928, "i"),
    "x": (0xA0, "i"), "y": (0xA4, "i"), "z": (0xA8, "i"),
    "fallModifier": (0x177C, "i"), "jumpModifier": (0x1780, "i"),
    "skillSpeedRaw": (0x1758, "i"), "skillDirectionRaw": (0x1744, "i"),
    "armorWazaRaw": (0x193C, "i"), "armorContributionCacheRaw": (0x17C0, "i"),
    "protectionFlags": (0x17C4, "I"),
}


def actor_row(r, address):
    address = pointer(address)
    data = r.read(address, 0x1940)
    row = {name: struct.unpack_from("<" + fmt, data, offset)[0]
           for name, (offset, fmt) in ACTOR_FIELDS.items()}
    at = lambda offset: struct.unpack_from("<I", data, offset)[0]
    row.update(actor=hex(address), meka=r.u32(pointer(at(0x158)) + 0x2C))
    manager = pointer(at(0x14A4))
    vector = r.read(manager, 0x24)
    gravity = pointer(struct.unpack_from("<I", vector, 0x14)[0])
    row.update(positionXRaw=struct.unpack_from("<i", vector, 0x1C)[0],
               positionYRaw=struct.unpack_from("<i", vector, 0x20)[0],
               vzRaw=r.i32(gravity + 0x10), gravityRaw=r.i32(gravity + 0x20))
    motion = at(0x1880)
    if motion:
        motion = pointer(motion)
        row.update(downTime=r.i32(motion + 0x1C), bounceLevel=r.i32(motion + 0x20),
                   bounceSpeed=r.i32(motion + 0x24))
    else:
        row.update(downTime=None, bounceLevel=None, bounceSpeed=None)
    return row, at(0x14B8)


def read_tama(r, max_nodes=MAX_RING):
    count, sentinel = r.u32(0x86CE8C), pointer(r.u32(0x86CE88))
    if count > min(max_nodes, MAX_RING):
        raise SampleRejected(f"TAMA count exceeds bound: {count}")
    ends = r.read(sentinel, 8)
    current, tail = struct.unpack("<II", ends)
    previous, seen, nodes, groups, watched = sentinel, set(), [], {}, []
    while current != sentinel:
        current = pointer(current)
        if current in seen or len(nodes) >= count:
            raise SampleRejected("TAMA cyclic ring or count mismatch")
        seen.add(current)
        data = r.read(current, 12)
        following, backward, actor = struct.unpack("<III", data)
        if backward != previous:
            raise SampleRejected("TAMA prev link mismatch")
        actor = pointer(actor)
        group, skill = r.u32(actor + 0x15B8), r.u32(actor + 0x15BC)
        key = f"{group}/{skill}"
        groups[key] = groups.get(key, 0) + 1
        if (group, skill) in WATCHED:
            watched.append({"actor": hex(actor), "group": group, "skill": skill,
                            "entry": r.u32(actor + 0x15C0), "block": r.u32(actor + 0x15C4),
                            "clock": r.u32(actor + 0x1480), "lifetimeClock": r.u32(actor + 0x15EC),
                            "parent": hex(r.u32(actor + 0x14AC)), "root": hex(r.u32(actor + 0x14C4))})
        nodes.append((current, data))
        previous, current = current, following
    if len(nodes) != count or previous != tail:
        raise SampleRejected("TAMA short ring / sentinel closure mismatch")
    if (r.u32(0x86CE8C) != count or r.u32(0x86CE88) != sentinel or
            r.read(sentinel, 8) != ends or any(r.read(a, 12) != data for a, data in nodes)):
        raise SampleRejected("TAMA ring changed during traversal")
    return groups, watched


def markers(r):
    return (r.u32(TICK), r.u32(REPLAY + 0x15A0), r.u32(PLAYER), r.u8(REPLAY + 0x159D))


def snapshot(r, meka):
    before = markers(r)
    tick, counter, player, active = before
    row, target = actor_row(r, player)
    if row["meka"] != meka:
        raise SampleRejected(f"Unexpected player meka: {row['meka']} expected {meka}")
    row.update(tick=tick, counter=counter, replayActive=active, player=hex(player))
    row["target"] = actor_row(r, target)[0] if target else None
    row["tama"], row["script"] = r.u32(0x86CE8C), r.u32(0x86CEA8)
    row["tamaGroups"], row["watchedTama"] = read_tama(r)
    row["globalFlagsRaw"] = {hex(a): r.u32(a) for a in (0x8750E8, 0x8750F4, 0x8750F8)}
    if row["tama"] != sum(row["tamaGroups"].values()) or row["script"] != r.u32(0x86CEA8):
        raise SampleRejected("TAMA/SCRIPT counts changed during sample")
    if markers(r) != before:
        raise SampleRejected("tick/counter/player/replayActive changed during sample")
    return row


def sample(r, *, meka=109, end_counter=480, wait_seconds=600, play_seconds=60,
           clock=time.monotonic, sleep=time.sleep, guard=lambda: None,
           max_rows=MAX_ROWS, max_errors=MAX_ERRORS, max_bytes=MAX_BYTES - 65536):
    if (not 0 <= meka <= 0xFFFFFFFF or not 1 <= end_counter <= 100000 or
            not 0 < wait_seconds <= 3600 or not 0 < play_seconds <= 600 or
            not 1 <= max_rows <= MAX_ROWS or not 1 <= max_errors <= MAX_ERRORS or not 0 < max_bytes <= MAX_BYTES - 65536):
        raise ValueError("Sampler arguments exceed bounds")
    result = {"schema": "nexas.bhe-rpm-sampler/v1", "source":
              "identity-checked read-only RPM; latest accepted row per observed replay counter; non-atomic guarded reads",
              "at": utc_now(), "started": False, "complete": False, "status": "waiting",
              "lastCounter": None, "acceptedRows": 0, "rows": [], "readErrors": [],
              "units": {"x_y_z": "integer map coordinates", "positionXRaw_positionYRaw_vzRaw_gravityRaw": "fixed point /1024",
                        "skillSpeedRaw": "skill speed /1024; not whole actor velocity", "skillDirectionRaw": "degrees times1024",
                        "modifiers": "integer percent", "globalFlagsRaw": "uninterpreted raw bits"}}
    rows, deadline, last_counter = {}, clock() + wait_seconds, None
    retained_bytes, row_sizes = 0, {}
    while True:
        if clock() >= deadline:
            result["status"] = "play_timeout" if result["started"] else "start_timeout"
            break
        counter = None
        try:
            guard()  # Failure is fatal, never merely a rejected memory sample.
        except Exception as exc:
            result.update(status="case_state_error" if isinstance(exc, CaseStateError) else "process_error",
                          failure=str(exc)[:1000])
            break
        try:
            _, counter, _, active = markers(r)
            if not result["started"]:
                if active != 1 or counter > 5:
                    sleep(POLL_SECONDS)
                    continue
                result["started"] = True
                deadline = clock() + play_seconds
            if last_counter is not None and counter < last_counter:
                result.update(status="replay_reset", failure="Replay counter decreased",
                              replayReset={"stage": "probe", "previousCounter": last_counter, "counter": counter})
                break
            last_counter = counter
            row = snapshot(r, meka)
            counter = row["counter"]
            if counter < last_counter:
                result.update(status="replay_reset", failure="Replay counter decreased",
                              replayReset={"stage": "snapshot", "previousCounter": last_counter, "counter": counter})
                break
            if counter not in rows and len(rows) >= max_rows:
                result["status"] = "limit_exceeded"
                break
            # Conservative serialization budget also bounds retained Python observations.
            row_size = 2 * len(json.dumps(row, ensure_ascii=False, indent=2).encode("utf-8")) + 1024
            next_bytes = retained_bytes - row_sizes.get(counter, 0) + row_size
            if next_bytes > max_bytes:
                result["status"] = "limit_exceeded"
                break
            retained_bytes, row_sizes[counter] = next_bytes, row_size
            rows[counter] = row
            last_counter = result["lastCounter"] = counter
            if row["replayActive"] == 0:
                if counter >= end_counter:
                    result.update(status="complete", complete=True)
                else:
                    result.update(status="ended_early", failure="Replay inactive before required end counter")
                break
        except (OSError, ValueError, SampleRejected, struct.error) as exc:
            if len(result["readErrors"]) >= max_errors:
                result["status"] = "limit_exceeded"
                break
            error = {"counter": counter, "error": str(exc)[:1000]}
            error_size = 2 * len(json.dumps(error, ensure_ascii=False).encode("utf-8")) + 1024
            if retained_bytes + error_size > max_bytes:
                result["status"] = "limit_exceeded"
                break
            result["readErrors"].append(error)
            retained_bytes += error_size
        sleep(POLL_SECONDS)
    result["rows"] = [rows[key] for key in sorted(rows)]
    result["acceptedRows"] = len(rows)
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--case", required=True)
    parser.add_argument("--output", required=True, help="Exclusive JSON basename within case")
    parser.add_argument("--meka", type=int, default=109)
    parser.add_argument("--end-counter", type=int, default=480)
    parser.add_argument("--wait-seconds", type=float, default=600)
    parser.add_argument("--play-seconds", type=float, default=60)
    args = parser.parse_args(argv)
    path = None
    try:
        case, identity = load_case(args.case)
        path = output_path(case, args.output)
        if path.exists():
            raise FileExistsError(path)
        with NativeProcess(identity) as process:
            result = sample(Reader(process.read), meka=args.meka, end_counter=args.end_counter,
                            wait_seconds=args.wait_seconds, play_seconds=args.play_seconds, guard=process.require_live_identity)
            # Completion requires the original process/case still to be verifiable.
            try:
                process.require_live_identity()
                state = read_case_state(case / "state.json")
                require_running_state(state, case)
                try:
                    verify_identity(identity, state.get("process", {}))
                except ValueError as exc:
                    raise CaseStateError(f"Case state process identity mismatch: {exc}") from exc
                process.require_live_identity()
            except Exception as exc:
                result.update(status="case_state_error" if isinstance(exc, CaseStateError) else "process_error",
                              complete=False, failure=str(exc)[:1000])
        result.update(process=identity, case=str(case), expectedExeSha256=EXPECTED_SHA,
                      requested={"meka": args.meka, "endCounter": args.end_counter,
                                 "waitSeconds": args.wait_seconds, "playSeconds": args.play_seconds})
        write_exclusive(path, result)
        print(json.dumps({"status": result["status"], "complete": result["complete"], "output": str(path)}))
        return 0 if result["complete"] else 1
    except Exception as exc:
        failure = {"schema": "nexas.bhe-rpm-sampler/v1", "at": utc_now(),
                   "status": "case_state_error" if isinstance(exc, CaseStateError) else "error",
                   "complete": False, "failure": str(exc)[:1000]}
        if path is not None and not path.exists():
            write_exclusive(path, failure)
        print(json.dumps(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())