"""Offline acceptance tests. Run python -B tools/test_bhe_runtime_sampler.py."""
import sys
sys.dont_write_bytecode = True
import copy
import ctypes
import json
import os
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch

try:
    import bhe_runtime_sampler as s
except ImportError:
    s = None

ROOT = Path(__file__).resolve().parents[1]


class Memory:
    def __init__(self):
        self.data = {}
        self.after = None

    def put(self, address, value, fmt="<I"):
        for n, byte in enumerate(struct.pack(fmt, value)):
            self.data[address + n] = byte

    def read(self, address, size):
        if self.after:
            self.after(address, size)
        try:
            return bytes(self.data[address + n] for n in range(size))
        except KeyError:
            raise OSError("short/unreadable fixture") from None


def fixture():
    m = Memory()
    # Hand-authored actor, vector, gravity, target and empty list memory.
    for base, size in ((0x10000, 0x1940), (0x20000, 0x1940),
                       (0x30000, 0x30), (0x40000, 0x30),
                       (0x50000, 0x30), (0x60000, 0x30), (0x70000, 0x30)):
        for address in range(base, base + size):
            m.data[address] = 0
    for actor in (0x10000, 0x20000):
        m.put(actor + 0x158, 0x30000)
        m.put(actor + 0x14a4, 0x40000)
        m.put(actor + 0x1880, 0x60000)
        m.put(actor + 0x161c, 2000)
        m.put(actor + 0x1618, 3000)
    m.put(0x3002c, 109)
    m.put(0x40014, 0x50000)
    m.put(0x4001c, 2357248)
    m.put(0x50020, 51200)
    m.put(0x114b8, 0x20000)
    m.put(0x879a5c, 7)
    m.put(0x86cf88, 0x10000)
    m.put(0x870c9d, 1, "<B")  # fixed object 0x86f700 + 0x159d
    m.put(0x870ca0, 0)
    m.put(0x86ce88, 0x70000)
    m.put(0x70000, 0x70000)
    m.put(0x70004, 0x70000)
    for address in (0x86ce8c, 0x86cea8, 0x8750e8, 0x8750f4, 0x8750f8):
        m.put(address, 0)
    return m


class FakeClock:
    def __init__(self, memory=None, sequence=()):
        self.now = 0.0
        self.memory = memory
        self.sequence = iter(sequence)
        self.sleeps = []

    def time(self):
        return self.now

    def sleep(self, seconds):
        self.sleeps.append(seconds)
        self.now += seconds
        if self.memory:
            value = next(self.sequence, None)
            if value is not None:
                counter, active = value
                self.memory.put(0x870ca0, counter)
                self.memory.put(0x870c9d, active, "<B")


class SamplerTests(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(s, "runtime sampler implementation missing")

    def test_integer_reads_stay_32_bit_and_short_reads_fail(self):
        m = Memory()
        m.put(0x10000, 0xffffffff)
        r = s.Reader(m.read)
        self.assertEqual(r.u32(0x10000), 4294967295)
        self.assertEqual(r.i32(0x10000), -1)
        with self.assertRaises(OSError):
            s.Reader(lambda a, n: b"\0").u32(0x10000)
        with self.assertRaises(ValueError):
            r.u32(0x100000000)

    def test_snapshot_reads_primary_hp_and_fixed_replay_object(self):
        row = s.snapshot(s.Reader(fixture().read), 109)
        self.assertEqual((row["counter"], row["hp"], row["maxHp"]), (0, 2000, 3000))
        self.assertEqual((row["positionXRaw"], row["gravityRaw"]), (2357248, 51200))
        self.assertEqual(row["target"]["actor"], "0x20000")
        self.assertEqual(row["globalFlagsRaw"], {"0x8750e8": 0, "0x8750f4": 0, "0x8750f8": 0})

    def test_snapshot_rejects_changed_tick_counter_player_and_active(self):
        for address, value, fmt in ((0x879a5c, 8, "<I"), (0x870ca0, 1, "<I"),
                                    (0x86cf88, 0x20000, "<I"), (0x870c9d, 0, "<B")):
            with self.subTest(address=address):
                m = fixture()
                m.after = lambda a, n: m.put(address, value, fmt) if a == 0x10000 else None
                with self.assertRaises(s.SampleRejected):
                    s.snapshot(s.Reader(m.read), 109)
        with self.assertRaises(s.SampleRejected):
            s.snapshot(s.Reader(fixture().read), 113)

    def test_ring_validates_closure_and_tracks_watched_actor(self):
        m = fixture()
        m.put(0x86ce8c, 1)
        m.put(0x70000, 0x71000)
        m.put(0x70004, 0x71000)
        for offset, value in ((0, 0x70000), (4, 0x70000), (8, 0x10000)):
            m.put(0x71000 + offset, value)
        m.put(0x115b8, 1)
        m.put(0x115bc, 215)
        m.put(0x115ec, 17)
        m.put(0x114ac, 0x20000)
        m.put(0x114c4, 0x20000)
        groups, watched = s.read_tama(s.Reader(m.read))
        self.assertEqual(groups, {"1/215": 1})
        self.assertEqual((watched[0]["lifetimeClock"], watched[0]["parent"]), (17, "0x20000"))

    def test_ring_refuses_short_corrupt_cyclic_invalid_and_oversize(self):
        for kind in ("short", "prev", "cycle", "null", "oversize", "early", "changed"):
            with self.subTest(kind=kind):
                m = fixture()
                m.put(0x86ce8c, 1)
                m.put(0x70000, 0x71000)
                m.put(0x70004, 0x71000)
                for offset, value in ((0, 0x70000), (4, 0x70000), (8, 0x10000)):
                    m.put(0x71000 + offset, value)
                if kind == "short":
                    del m.data[0x71008]
                elif kind == "prev":
                    m.put(0x71004, 0x72000)
                elif kind == "cycle":
                    m.put(0x71000, 0x71000)
                elif kind == "null":
                    m.put(0x70000, 0)
                elif kind == "oversize":
                    m.put(0x86ce8c, 5000)
                elif kind == "early":
                    m.put(0x70000, 0x70000)
                elif kind == "changed":
                    m.after = lambda a, n: m.put(0x86ce8c, 2) if a == 0x115b8 else None
                with self.assertRaises((s.SampleRejected, OSError, ValueError)):
                    s.read_tama(s.Reader(m.read))

    def test_complete_run_latest_row_and_no_extra_input_frames_claim(self):
        m = fixture()
        clock = FakeClock(m, [(0, 1), (1, 1), (2, 0)])
        def advance(seconds):
            clock.sleep(seconds)
            m.put(0x1161c, 1999)
        result = s.sample(s.Reader(m.read), meka=109, end_counter=2,
                          wait_seconds=1, play_seconds=1, clock=clock.time, sleep=advance)
        self.assertTrue(result["complete"])
        self.assertEqual(result["status"], "complete")
        self.assertEqual(result["acceptedRows"], 3)
        self.assertEqual(result["rows"][0]["hp"], 1999)
        self.assertEqual([r["counter"] for r in result["rows"]], [0, 1, 2])
        self.assertTrue(all(t >= .005 for t in clock.sleeps))

    def test_snapshot_advance_updates_terminal_last_counter(self):
        m = fixture()
        clock = FakeClock(m, [(479, 1)])
        tick_reads = []
        def advance_between_probe_and_snapshot(address, size):
            if address == 0x879a5c:
                tick_reads.append(1)
                if len(tick_reads) == 5:
                    m.put(0x870ca0, 480)
                    m.put(0x870c9d, 0, "<B")
        m.after = advance_between_probe_and_snapshot
        result = s.sample(s.Reader(m.read), end_counter=480, wait_seconds=1, play_seconds=1,
                          clock=clock.time, sleep=clock.sleep)
        self.assertEqual((result["status"], result["complete"], result["lastCounter"]),
                         ("complete", True, 480))
        self.assertEqual([row["counter"] for row in result["rows"]], [0, 480])

    def test_regressive_terminal_snapshot_is_rejected_before_retention(self):
        m = fixture()
        clock = FakeClock(m, [(500, 1)])
        tick_reads = []
        def regress_between_probe_and_snapshot(address, size):
            if address == 0x879a5c:
                tick_reads.append(1)
                if len(tick_reads) == 5:
                    m.put(0x870ca0, 480)
                    m.put(0x870c9d, 0, "<B")
        m.after = regress_between_probe_and_snapshot
        result = s.sample(s.Reader(m.read), end_counter=480, wait_seconds=1, play_seconds=1,
                          clock=clock.time, sleep=clock.sleep)
        self.assertEqual((result["status"], result["complete"], result["lastCounter"]),
                         ("replay_reset", False, 0))
        self.assertEqual([row["counter"] for row in result["rows"]], [0])
        self.assertEqual(result["replayReset"],
                         {"stage": "snapshot", "previousCounter": 500, "counter": 480})

    def test_start_timeout_play_timeout_and_rejected_attempt_are_distinct(self):
        m = fixture()
        m.put(0x870c9d, 0, "<B")
        clock = FakeClock()
        result = s.sample(s.Reader(m.read), meka=109, end_counter=2,
                          wait_seconds=.01, play_seconds=.01, clock=clock.time, sleep=clock.sleep)
        self.assertEqual((result["status"], result["started"], result["complete"]),
                         ("start_timeout", False, False))
        m.put(0x870c9d, 1, "<B")
        clock = FakeClock()
        result = s.sample(s.Reader(m.read), meka=113, end_counter=2,
                          wait_seconds=.01, play_seconds=.01, clock=clock.time, sleep=clock.sleep)
        self.assertEqual(result["status"], "play_timeout")
        self.assertEqual(result["readErrors"][0]["counter"], 0)
        self.assertEqual(result["acceptedRows"], 0)

    def test_row_and_error_bounds_fail_without_partial_success(self):
        for expected_meka, sequence in ((109, [(1, 1), (2, 0)]), (113, [])):
            m = fixture()
            clock = FakeClock(m, sequence)
            result = s.sample(s.Reader(m.read), meka=expected_meka, end_counter=2,
                              wait_seconds=1, play_seconds=1, max_rows=1, max_errors=1,
                              clock=clock.time, sleep=clock.sleep)
            self.assertEqual(result["status"], "limit_exceeded")
            self.assertFalse(result["complete"])
            self.assertLessEqual(len(result["rows"]), 1)
            self.assertLessEqual(len(result["readErrors"]), 1)


    def test_total_retained_bytes_stop_sampling_before_large_output(self):
        m = fixture()
        clock = FakeClock(m, [(1, 1), (2, 0)])
        result = s.sample(s.Reader(m.read), meka=109, end_counter=2,
                          wait_seconds=1, play_seconds=1, max_bytes=100,
                          clock=clock.time, sleep=clock.sleep)
        self.assertEqual(result["status"], "limit_exceeded")
        self.assertFalse(result["complete"])
        self.assertEqual(result["acceptedRows"], 0)

    def test_guard_failure_retains_prior_samples_and_marks_process_error(self):
        m = fixture()
        clock = FakeClock()
        calls = []
        def guard():
            calls.append(1)
            if len(calls) > 1:
                raise ValueError("identity changed")
        result = s.sample(s.Reader(m.read), end_counter=2, wait_seconds=1, play_seconds=1,
                          guard=guard, clock=clock.time, sleep=clock.sleep)
        self.assertEqual((result["status"], result["acceptedRows"], result["complete"]),
                         ("process_error", 1, False))
        self.assertEqual(result["failure"], "identity changed")

class CaseTests(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(s, "runtime sampler implementation missing")
        self.temp = tempfile.TemporaryDirectory(prefix="sampler-test-", dir=ROOT / "target/bhe-playable-runtime")
        self.case = Path(self.temp.name)
        self.env = patch.dict(os.environ, {"TEMP": str(self.case), "TMP": str(self.case), "TMPDIR": str(self.case)})
        self.env.start()
        (self.case / "game").mkdir()
        self.exe = self.case / "game/BaldrSky.exe"
        self.exe.write_bytes(b"fixture-only")
        self.state = {"case": str(self.case), "status": "running", "process":
                      {"pid": 123, "creation_filetime": 456, "image": str(self.exe)}}
        self.save()
        self.hash = patch.object(s, "file_record", return_value={"sha256": s.EXPECTED_SHA})
        self.hash.start()

    def tearDown(self):
        self.hash.stop()
        self.env.stop()
        self.temp.cleanup()

    def save(self):
        (self.case / "state.json").write_text(json.dumps(self.state), encoding="utf-8")

    def test_case_paths_terminal_state_identity_and_hash_refused(self):
        self.assertEqual(s.load_case(self.case)[0], self.case)
        for invalid in (ROOT, self.case / "..", "../escape", "CON"):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                s.load_case(invalid)
        for key, value in (("status", "stopped"), ("case", str(ROOT)), ("terminal", True),
                           ("owned_process_exit_confirmed", True), ("exit_code", 0)):
            original = copy.deepcopy(self.state)
            self.state[key] = value
            self.save()
            with self.assertRaises(ValueError):
                s.load_case(self.case)
            self.state = original
        self.save()
        with patch.object(s, "file_record", return_value={"sha256": "wrong"}), self.assertRaises(ValueError):
            s.load_case(self.case)
        for changes in ({"pid": 124}, {"creation_filetime": 457}, {"image": str(ROOT / "wrong.exe")}):
            live = dict(self.state["process"], **changes)
            with self.assertRaises(ValueError):
                s.verify_identity(self.state["process"], live)

    def test_output_exclusive_safe_basename_and_byte_bound(self):
        for name in ("../a.json", "a/b.json", "C:a.json", "CON.json", "state.json", "a.json:stream"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                s.output_path(self.case, name)
        path = s.output_path(self.case, "sample.json")
        s.write_exclusive(path, {"complete": False})
        before = path.read_bytes()
        with self.assertRaises(FileExistsError):
            s.write_exclusive(path, {})
        self.assertEqual(path.read_bytes(), before)
        self.assertFalse(before.startswith(b"\xef\xbb\xbf"))
        self.assertNotIn(b"\n", before.replace(b"\r\n", b""))
        with self.assertRaises(ValueError):
            s.write_exclusive(self.case / "large.json", {"x": "x" * 50}, max_bytes=20)
        self.assertFalse((self.case / "large.json").exists())

    def test_cli_final_identity_failure_preserves_rows_and_writes_failure(self):
        from contextlib import redirect_stdout, redirect_stderr
        import io
        m = fixture()
        clock = FakeClock(m, [(1, 1), (2, 0)])
        real_sample = s.sample
        class Process:
            def __init__(self):
                self.calls = 0
            def __enter__(self):
                return self
            def __exit__(self, *args):
                pass
            def read(self, address, size):
                return m.read(address, size)
            def require_live_identity(self):
                self.calls += 1
                if self.calls == 4:
                    raise ValueError("final identity mismatch")
        def timed_sample(reader, **kwargs):
            return real_sample(reader, **kwargs, clock=clock.time, sleep=clock.sleep)
        with patch.object(s, "NativeProcess", return_value=Process()), patch.object(s, "sample", side_effect=timed_sample):
            with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
                code = s.main(["--case", str(self.case), "--output", "cli.json", "--end-counter", "2"])
        result = json.loads((self.case / "cli.json").read_text(encoding="utf-8"))
        self.assertEqual((code, result["status"], result["complete"], result["acceptedRows"]),
                         (1, "process_error", False, 3))
        self.assertEqual(result["failure"], "final identity mismatch")

    def test_reparse_case_is_refused(self):
        with patch.object(Path, "is_symlink", return_value=True), self.assertRaises(ValueError):
            s.load_case(self.case)


class NativeTests(unittest.TestCase):
    def test_open_uses_read_only_bits_and_identity_failure_closes_handle(self):
        self.assertIsNotNone(s, "runtime sampler implementation missing")
        class Kernel:
            def __init__(self):
                self.access = None
                self.closed = []
            def OpenProcess(self, access, inherit, pid):
                self.access = access
                return 12
            def CloseHandle(self, handle):
                self.closed.append(handle)
        kernel = Kernel()
        identity = {"pid": 123, "creation_filetime": 456, "image": "wrong.exe"}
        with patch.object(s.NativeProcess, "current_identity", return_value=dict(identity, pid=124)):
            with self.assertRaises(ValueError):
                with s.NativeProcess(identity, kernel=kernel):
                    self.fail("identity mismatch must refuse")
        self.assertEqual(kernel.access, 0x1010)
        self.assertEqual(kernel.closed, [12])


if __name__ == "__main__":
    unittest.main()