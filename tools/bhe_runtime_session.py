"""Prepare and observe one isolated BSDX session. No UI automation or EXE patches."""

import argparse
import ctypes
from ctypes import wintypes
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import shutil
import subprocess
import sys
import threading
import time


PROJECT_ROOT = Path(__file__).resolve().parents[1]
RUNTIME_ROOT = PROJECT_ROOT / "target" / "bhe-playable-runtime"
CRT_OFFSET = 0x2789F8
SAVE_SUFFIX = Path("GIGA/BALDRSKY_DIVEX/Save")
NVIDIA_DRS_SOURCE = Path(os.environ.get("ProgramData", r"C:\ProgramData")) / "NVIDIA Corporation/Drs"
NVIDIA_DRS_COPY = Path("driver-cache/Drs")


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def check_no_links(path):
    """Reject junctions/symlinks, including an existing ancestor of an output."""
    path = Path(os.path.abspath(path))
    for item in (path, *path.parents):
        if item.is_symlink() or (item.exists() and
                                getattr(item.lstat(), "st_file_attributes", 0) & 0x400):
            raise ValueError(f"Reparse point/symlink is not allowed: {item}")
    return path.resolve()


def file_record(path):
    path = check_no_links(path)
    if not path.is_file():
        raise ValueError(f"Not a regular file: {path}")
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            digest.update(chunk)
    return {"size": path.stat().st_size, "sha256": digest.hexdigest()}


def tree_manifest(directory):
    directory = check_no_links(directory)
    if not directory.is_dir():
        raise ValueError(f"Missing directory: {directory}")
    result = {}
    for current, dirs, files in os.walk(directory, followlinks=False):
        for name in dirs + files:
            check_no_links(Path(current) / name)
        for name in sorted(files):
            path = Path(current) / name
            result[path.relative_to(directory).as_posix()] = file_record(path)
    return dict(sorted(result.items()))


def manifest_difference(actual, expected):
    added = sorted(actual.keys() - expected.keys())
    removed = sorted(expected.keys() - actual.keys())
    changed = sorted(key for key in actual.keys() & expected.keys() if actual[key] != expected[key])
    return {"unchanged": not (added or removed or changed), "added": added,
            "removed": removed, "changed": changed}


def compare_manifest(directory, expected):
    return manifest_difference(tree_manifest(directory), expected)


def require_unchanged(directory, expected, label):
    result = compare_manifest(directory, expected)
    if not result["unchanged"]:
        raise ValueError(f"{label} changed: {result}")
    return result


def check_crt(exe_path):
    with Path(exe_path).open("rb") as stream:
        if stream.read(2) != b"MZ":
            raise ValueError("EXE has no MZ signature")
        stream.seek(CRT_OFFSET)
        actual = stream.read(2)
    if actual != b"\x8b\xff":
        raise ValueError(f"Refusing global CRT bypass/unknown profile at 0x2789F8: {actual.hex()}")
    return actual.hex()


def case_path(runtime_root, case_name):
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_-]{0,63}", case_name):
        raise ValueError("Case name must be 1-64 ASCII letters, digits, underscores or hyphens")
    if case_name.upper() in {"CON", "PRN", "AUX", "NUL", *[f"COM{i}" for i in range(10)],
                             *[f"LPT{i}" for i in range(10)]}:
        raise ValueError("Windows device names are not valid case names")
    root = check_no_links(runtime_root)
    path = check_no_links(root / case_name)
    if path.parent != root:
        raise ValueError("Case must be a direct child of the runtime root")
    return path


def write_json(path, value):
    """Only used for owned session files; replacement is atomic for status readers."""
    path = Path(path)
    temporary = path.with_name(path.name + ".new")
    with temporary.open("w", encoding="utf-8", newline="\r\n") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    temporary.replace(path)


def prepare_case(*, base_dir, exe_path, pac_paths, save_dir, case_name, runtime_root=RUNTIME_ROOT, vanilla=False,
                 fresh_save=False, save_seed_dir=None, app_manifest_path=None):
    case = case_path(runtime_root, case_name)
    if case.exists():
        raise FileExistsError(f"Case already exists; nothing removed: {case}")
    base = check_no_links(base_dir)
    exe = check_no_links(exe_path)
    save = check_no_links(save_dir)
    if fresh_save and save_seed_dir is not None:
        raise ValueError("fresh_save and save_seed_dir are mutually exclusive")
    seed_mode = "fresh" if fresh_save else ("provided" if save_seed_dir is not None else "original")
    seed_source = None if fresh_save else check_no_links(save_seed_dir if save_seed_dir is not None else save)
    app_manifest = check_no_links(app_manifest_path) if app_manifest_path is not None else None
    driver_source = check_no_links(NVIDIA_DRS_SOURCE)
    pacs = [check_no_links(path) for path in pac_paths]
    if not base.is_dir() or not save.is_dir():
        raise ValueError("A base directory and save seed directory are required")
    if (vanilla and pacs) or (not vanilla and not pacs):
        raise ValueError("Choose vanilla with no additional PACs, or select at least one PAC")
    if [part.casefold() for part in save.parts[-3:]] != ["giga", "baldrsky_divex", "save"]:
        raise ValueError("Save seed must end with GIGA/BALDRSKY_DIVEX/Save")
    extra_sources = [path for path in (seed_source, app_manifest) if path is not None]
    for source in (base, exe, save, driver_source, *pacs):
        if source == case.parent or source.is_relative_to(case.parent) or case.parent.is_relative_to(source):
            raise ValueError(f"Runtime root and input overlap: {source}")
    for source in extra_sources:
        if source == case or source.is_relative_to(case) or case.is_relative_to(source):
            raise ValueError(f"Case and diagnostic input overlap: {source}")
    names = [path.name.casefold() for path in pacs]
    if len(names) != len(set(names)) or any(not re.fullmatch(r"update[3-7]\.pac", name) for name in names):
        raise ValueError("Select unique Update3.pac through Update7.pac files explicitly")
    for path in base.iterdir():
        if re.fullmatch(r"update[3-7]\.pac", path.name, re.I) and path.name.casefold() not in names:
            raise ValueError(f"Unselected overlay in base could mask current output: {path}")
    check_crt(exe)
    source_base = tree_manifest(base)
    source_save = tree_manifest(save)
    source_seed = {} if seed_source is None else (source_save if seed_source == save else tree_manifest(seed_source))
    source_files = {str(path): file_record(path) for path in (exe, *pacs, *([app_manifest] if app_manifest is not None else []))}
    documents = case / "docs"
    if not str(documents).isascii() or len(str(documents)) + len(str(SAVE_SUFFIX)) + 2 >= 260:
        raise ValueError("Isolated Documents must be ASCII and fit the game's MAX_PATH buffer")
    case.mkdir(parents=True, exist_ok=False)
    state = {"status": "preparing", "case": str(case), "created_at": utc_now(),
             "owned_directory": str(case), "source_files_deleted": False,
             "fresh_save": bool(fresh_save), "seed_mode": seed_mode,
             "seed_source": str(seed_source) if seed_source is not None else None,
             "app_manifest_source": str(app_manifest) if app_manifest is not None else None}
    write_json(case / "state.json", state)
    try:
        game = case / "game"
        shutil.copytree(base, game)
        # These are newly owned copies. Original inputs never become destinations.
        existing_exe = next((p for p in game.iterdir() if p.name.casefold() == "baldrsky.exe"), None)
        destination_exe = existing_exe or game / "BaldrSky.exe"
        shutil.copy2(exe, destination_exe)
        expected_game = dict(source_base)
        expected_game[destination_exe.name] = source_files[str(exe)]
        app_manifest_name = None
        if app_manifest is not None:
            app_manifest_name = destination_exe.name + ".manifest"
            existing_manifest = next((p for p in game.iterdir() if p.name.casefold() == app_manifest_name.casefold()), None)
            destination_manifest = existing_manifest or game / app_manifest_name
            shutil.copy2(app_manifest, destination_manifest)
            app_manifest_name = destination_manifest.name
            expected_game[app_manifest_name] = source_files[str(app_manifest)]
        for pac in pacs:
            existing_pac = next((p for p in game.iterdir() if p.name.casefold() == pac.name.casefold()), None)
            destination = existing_pac or game / pac.name
            shutil.copy2(pac, destination)
            expected_game[destination.name] = source_files[str(pac)]
        if seed_source is None:
            (documents / SAVE_SUFFIX).mkdir(parents=True)
        else:
            shutil.copytree(seed_source, documents / SAVE_SUFFIX)
        driver_copy = case / NVIDIA_DRS_COPY
        driver_before = tree_manifest(driver_source)
        state["driver_state_copy_check"] = {"source": str(driver_source), "before": driver_before}
        write_json(case / "state.json", state)
        shutil.copytree(driver_source, driver_copy)
        driver_after = tree_manifest(driver_source)
        driver_copied = tree_manifest(driver_copy)
        source_check = manifest_difference(driver_after, driver_before)
        copy_check = manifest_difference(driver_copied, driver_before)
        state["driver_state_copy_check"].update(after=driver_after, copy=driver_copied,
                                              source_check=source_check, copy_check=copy_check)
        write_json(case / "state.json", state)
        if not source_check["unchanged"] or not copy_check["unchanged"]:
            raise ValueError("NVIDIA Drs tree changed during copying or copied bytes differ; evidence retained")
        require_unchanged(game, expected_game, "Copied game")
        require_unchanged(documents / SAVE_SUFFIX, source_seed, "Private save seed")
        require_unchanged(base, source_base, "Original base")
        require_unchanged(save, source_save, "Original save")
        if seed_source is not None:
            require_unchanged(seed_source, source_seed, "Save seed source")
        for path, record in source_files.items():
            if file_record(path) != record:
                raise ValueError(f"Input changed during copy: {path}")
        manifest = {"schema": 1, "case": str(case), "created_at": utc_now(), "vanilla": bool(vanilla),
                    "fresh_save": bool(fresh_save), "seed_mode": seed_mode,
                    "seed_source": str(seed_source) if seed_source is not None else None,
                    "app_manifest_source": str(app_manifest) if app_manifest is not None else None,
                    "app_manifest_name": app_manifest_name,
                    "base_dir": str(base), "exe_source": str(exe), "pac_sources": [str(p) for p in pacs],
                    "exe_name": destination_exe.name, "original_save": str(save),
                    "original_documents": str(save.parents[2]), "isolated_documents": str(documents),
                    "original_save_files": source_save, "save_seed_files": source_seed, "base_files": source_base,
                    "source_files": source_files, "game_files": expected_game, "crt_bytes": "8bff",
                    "driver_state": {"source": str(driver_source), "relative_path": NVIDIA_DRS_COPY.as_posix(),
                                     "files": driver_before, "file_count": len(driver_before),
                                     "total_bytes": sum(item["size"] for item in driver_before.values())}}
        write_json(case / "manifest.json", manifest)
        state.update(status="prepared", manifest_sha256=file_record(case / "manifest.json")["sha256"])
        write_json(case / "state.json", state)
        return case
    except BaseException as exc:
        state.update(status="prepare_failed", error=repr(exc), finished_at=utc_now())
        write_json(case / "state.json", state)
        raise


def verify_prepared(case_dir, runtime_root=RUNTIME_ROOT):
    case = check_no_links(case_dir)
    if case != case_path(runtime_root, case.name):
        raise ValueError("Case is outside the configured runtime root")
    manifest = json.loads((case / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("schema") != 1 or manifest.get("case") != str(case):
        raise ValueError("Manifest does not belong to this case")
    if manifest["isolated_documents"] != str(case / "docs"):
        raise ValueError("Manifest redirects Documents outside this case")
    if Path(manifest["exe_name"]).name != manifest["exe_name"] or manifest["exe_name"].casefold() != "baldrsky.exe":
        raise ValueError("Unexpected launch executable")
    save = check_no_links(manifest["original_save"])
    if save.parents[2] != Path(manifest["original_documents"]) or save.is_relative_to(case):
        raise ValueError("Original save root is inconsistent")
    require_unchanged(case / "game", manifest["game_files"], "Prepared game")
    require_unchanged(save, manifest["original_save_files"], "Original save")
    seed_files = manifest.get("save_seed_files", manifest["original_save_files"])
    seed_source = manifest.get("seed_source", manifest["original_save"])
    if seed_source is not None:
        require_unchanged(seed_source, seed_files, "Save seed source")
    require_unchanged(case / "docs" / SAVE_SUFFIX, seed_files, "Isolated save seed")
    app_manifest_source = manifest.get("app_manifest_source")
    if app_manifest_source is not None:
        if file_record(app_manifest_source) != manifest["source_files"].get(app_manifest_source):
            raise ValueError("Application manifest source changed after preparation")
    driver = manifest.get("driver_state")
    if (not isinstance(driver, dict) or driver.get("source") != str(check_no_links(NVIDIA_DRS_SOURCE))
            or driver.get("relative_path") != NVIDIA_DRS_COPY.as_posix()):
        raise ValueError("Missing or unexpected driver state tree; prepare a fresh case")
    require_unchanged(driver["source"], driver["files"], "Original driver state tree")
    require_unchanged(case / NVIDIA_DRS_COPY, driver["files"], "Copied driver state tree")
    check_crt(case / "game" / manifest["exe_name"])
    return manifest


class OwnedProcessIdentityError(RuntimeError):
    """Transfer ownership of a created suspended process even when identity lookup fails."""

    def __init__(self, process, error):
        super().__init__(f"Created process {process.pid}, but identity lookup failed: {error!r}")
        self.process = process


class OwnedProcess:
    """One Windows process, retained handles and identity; never kills by name/PID lookup."""

    def __init__(self, executable, directory):
        self.executable = Path(executable).resolve()
        self.kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        self.process_handle = None
        self.thread_handle = None
        self.closed = []
        self.resumed = False
        self.identity = None

        class StartupInfo(ctypes.Structure):
            _fields_ = [("cb", wintypes.DWORD), ("lpReserved", wintypes.LPWSTR),
                        ("lpDesktop", wintypes.LPWSTR), ("lpTitle", wintypes.LPWSTR),
                        *[(name, wintypes.DWORD) for name in ("dwX", "dwY", "dwXSize", "dwYSize",
                          "dwXCountChars", "dwYCountChars", "dwFillAttribute", "dwFlags")],
                        ("wShowWindow", wintypes.WORD), ("cbReserved2", wintypes.WORD),
                        ("lpReserved2", ctypes.POINTER(ctypes.c_ubyte)),
                        ("hStdInput", wintypes.HANDLE), ("hStdOutput", wintypes.HANDLE),
                        ("hStdError", wintypes.HANDLE)]

        class ProcessInfo(ctypes.Structure):
            _fields_ = [("hProcess", wintypes.HANDLE), ("hThread", wintypes.HANDLE),
                        ("dwProcessId", wintypes.DWORD), ("dwThreadId", wintypes.DWORD)]

        signatures = {
            "CreateProcessW": ([wintypes.LPCWSTR, wintypes.LPWSTR, ctypes.c_void_p, ctypes.c_void_p,
                                wintypes.BOOL, wintypes.DWORD, ctypes.c_void_p, wintypes.LPCWSTR,
                                ctypes.POINTER(StartupInfo), ctypes.POINTER(ProcessInfo)], wintypes.BOOL),
            "GetProcessTimes": ([wintypes.HANDLE, *[ctypes.POINTER(wintypes.FILETIME)] * 4], wintypes.BOOL),
            "QueryFullProcessImageNameW": ([wintypes.HANDLE, wintypes.DWORD, wintypes.LPWSTR,
                                            ctypes.POINTER(wintypes.DWORD)], wintypes.BOOL),
            "GetProcessId": ([wintypes.HANDLE], wintypes.DWORD),
            "ResumeThread": ([wintypes.HANDLE], wintypes.DWORD),
            "WaitForSingleObject": ([wintypes.HANDLE, wintypes.DWORD], wintypes.DWORD),
            "GetExitCodeProcess": ([wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD)], wintypes.BOOL),
            "TerminateProcess": ([wintypes.HANDLE, wintypes.UINT], wintypes.BOOL),
            "CloseHandle": ([wintypes.HANDLE], wintypes.BOOL),
        }
        for name, (args, result) in signatures.items():
            function = getattr(self.kernel, name)
            function.argtypes, function.restype = args, result
        startup, info = StartupInfo(), ProcessInfo()
        startup.cb = ctypes.sizeof(startup)
        command = ctypes.create_unicode_buffer(subprocess.list2cmdline([str(self.executable)]))
        if not self.kernel.CreateProcessW(str(self.executable), command, None, None, False,
                                          0x00000004 | 0x00000200, None, str(directory),
                                          ctypes.byref(startup), ctypes.byref(info)):
            raise ctypes.WinError(ctypes.get_last_error())
        self.process_handle, self.thread_handle = info.hProcess, info.hThread
        self.pid, self.tid = info.dwProcessId, info.dwThreadId
        try:
            self.identity = self.current_identity()
        except BaseException as exc:
            # The caller must retain these exact handles until exit is confirmed.
            # Constructor failure must not hide ownership or close a live process.
            raise OwnedProcessIdentityError(self, exc) from exc

    def current_identity(self):
        creation, exit_time, kernel_time, user_time = [wintypes.FILETIME() for _ in range(4)]
        if not self.kernel.GetProcessTimes(self.process_handle, ctypes.byref(creation), ctypes.byref(exit_time),
                                           ctypes.byref(kernel_time), ctypes.byref(user_time)):
            raise ctypes.WinError(ctypes.get_last_error())
        size = wintypes.DWORD(32768)
        buffer = ctypes.create_unicode_buffer(size.value)
        if not self.kernel.QueryFullProcessImageNameW(self.process_handle, 0, buffer, ctypes.byref(size)):
            raise ctypes.WinError(ctypes.get_last_error())
        ticks = (creation.dwHighDateTime << 32) | creation.dwLowDateTime
        return {"pid": int(self.kernel.GetProcessId(self.process_handle)), "thread_id": self.tid,
                "creation_filetime": ticks, "image": str(Path(buffer.value).resolve())}

    def require_identity(self):
        if self.current_identity() != self.identity or self.identity["image"].casefold() != str(self.executable).casefold():
            raise RuntimeError("Process identity mismatch; refusing process operation")

    def poll(self):
        result = self.kernel.WaitForSingleObject(self.process_handle, 0)
        if result == 258:
            return None
        if result != 0:
            raise ctypes.WinError(ctypes.get_last_error())
        code = wintypes.DWORD()
        if not self.kernel.GetExitCodeProcess(self.process_handle, ctypes.byref(code)):
            raise ctypes.WinError(ctypes.get_last_error())
        return int(code.value)

    def resume(self):
        self.require_identity()
        if self.resumed:
            raise RuntimeError("Primary thread already resumed")
        previous = self.kernel.ResumeThread(self.thread_handle)
        if previous != 1:
            raise RuntimeError(f"Unexpected primary thread suspend count: {previous}")
        self.resumed = True

    def terminate(self):
        if self.poll() is None:
            if self.identity is not None:
                self.require_identity()
            elif self.resumed:
                raise RuntimeError("Cannot terminate a resumed process without verified identity")
            # With no identity, this is the original CreateProcessW handle for a
            # never-resumed child. No PID lookup/reopen can select another process.
            if not self.kernel.TerminateProcess(self.process_handle, 0xE0000001):
                raise ctypes.WinError(ctypes.get_last_error())
            if self.kernel.WaitForSingleObject(self.process_handle, 10000) != 0:
                raise RuntimeError("Owned process did not terminate within 10 seconds")

    def close(self):
        for name in ("thread_handle", "process_handle"):
            handle = getattr(self, name)
            if handle:
                if self.kernel.CloseHandle(handle):
                    self.closed.append(name)
                    setattr(self, name, None)
                else:
                    raise ctypes.WinError(ctypes.get_last_error())


def native_path(path):
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    query = kernel.QueryDosDeviceW
    query.argtypes = [wintypes.LPCWSTR, wintypes.LPWSTR, wintypes.DWORD]
    query.restype = wintypes.DWORD
    path = str(Path(path).resolve())
    buffer = ctypes.create_unicode_buffer(32768)
    if not query(path[:2], buffer, len(buffer)):
        raise ctypes.WinError(ctypes.get_last_error())
    return buffer.value + path[2:]


def isolation_policy(config):
    """The same pure JavaScript path policy is executed by offline boundary tests."""
    return "const config = " + json.dumps(config) + ";\n" + r'''
function canonicalPath(value) {
  let s = value.replace(/\//g, '\\');
  if (s.startsWith('\\\\?\\')) s = s.slice(4);
  if (s.startsWith('\\??\\')) s = s.slice(4);
  const parts = [];
  for (const part of s.split('\\')) {
    if (part === '.') continue;
    if (part === '..') { parts.pop(); continue; }
    parts.push(part);
  }
  return parts.join('\\').replace(/\\+$/, '');
}
function normalize(value) { return canonicalPath(value).toLowerCase(); }
const allowed = config.allowed.map(normalize);
const protectedRoots = (config.protected || []).map(normalize);
const repositoryRoots = (config.repository || []).map(normalize);
function writeScope(path) {
  if (typeof path !== 'string' || path.length === 0 || path.includes('\0')) return 'unresolved';
  let raw = path.replace(/\//g, '\\');
  if (raw.startsWith('\\\\?\\') || raw.startsWith('\\??\\')) raw = raw.slice(4);
  const p = normalize(path);
  const dos = /^[a-z]:\\/i.test(raw) && /^[a-z]:(?:\\|$)/i.test(p);
  const volume = /^\\device\\harddiskvolume\d+(?:\\|$)/i.test(raw) &&
    /^\\device\\harddiskvolume\d+(?:\\|$)/i.test(p);
  if (!dos && !volume) return 'unresolved';
  if (protectedRoots.some(root => p === root || p.startsWith(root + '\\') || p.startsWith(root + ':')))
    return 'protected_input';
  if (allowed.some(root => p === root || p.startsWith(root + '\\'))) return 'case';
  if (repositoryRoots.some(root => p === root || p.startsWith(root + '\\') || p.startsWith(root + ':')))
    return 'repository';
  return 'external';
}
function permitted(path) { return ['case', 'external'].includes(writeScope(path)); }
function metadataOpenOnly(access, options, disposition, existingDirectory) {
  // Opening an existing directory for attributes/MAXIMUM_ALLOWED is not a
  // data write. Actual metadata mutation still goes through the write guard.
  return existingDirectory && disposition === 1 && (options & 0x1000) === 0 &&
    (access & 0x500d0056) === 0;
}
function directoryCreateCollision(options, disposition, existingDirectory) {
  return disposition === 2 && (options & 1) !== 0 && (options & 0x40) === 0 && existingDirectory;
}
function hidDeviceOpen(path, disposition, options) {
  // DirectInput's observed HID interface namespace is a device, not a disk file.
  // No create/overwrite, directory, delete-on-close, or general Device exemption.
  return disposition === 1 && (options & 0x1001) === 0 &&
    /^\\\?\?\\hid#[^\\/#]+#[^\\/#]+#\{4d1e55b2-f16f-11cf-88cb-001111000030\}(?:\\kbd)?$/i.test(path);
}
function mmcssDeviceOpen(path, access, disposition, options) {
  // The audio scheduler opens this kernel object with disposition 0, not a disk file.
  // Accept only observed read rights, no directory/delete-on-close or other devices.
  return typeof path === 'string' && path.toLowerCase() === '\\device\\mmcss\\mmthread' &&
    (disposition === 0 || disposition === 1) && (options & 0x1001) === 0 &&
    (access & ~0x00120089) === 0;
}
function aliasSavePath(path) {
  const canonical = canonicalPath(path);
  const lower = canonical.toLowerCase();
  for (const source of config.saveAliasSources || []) {
    const root = normalize(source);
    if (lower === root || lower.startsWith(root + '\\'))
      return config.saveAliasTarget + canonical.slice(root.length);
  }
  return null;
}
function aliasDriverState(path) {
  const canonical = canonicalPath(path);
  const name = canonical.toLowerCase();
  for (const source of config.driverStateSources || []) {
    const root = normalize(source);
    if (name === root || name.startsWith(root + '\\'))
      return config.driverStateTarget + canonical.slice(root.length);
  }
  return null;
}
function redirectDocuments(pidls, pidl, succeeded, gameCall, readPath, writePath) {
  if (!succeeded || !gameCall || !pidls.has(pidl)) return false;
  const actual = readPath();
  if (normalize(actual) !== normalize(config.documents))
    throw new Error('CSIDL_PERSONAL returned unexpected Documents: ' + actual);
  writePath(config.redirect);
  pidls.delete(pidl); // Only consume after the outer game call was successfully redirected.
  return true;
}
'''


def isolation_structures():
    """Native structures are cloned; caller-owned pointers/buffers stay untouched."""
    return r'''
function cloneObjectAttributes(attributes, target, ps) {
  const size = ps === 4 ? 24 : 48;
  if (attributes.readU32() !== size) throw new Error('Unsupported OBJECT_ATTRIBUTES length');
  const byteLength = target.length * 2;
  if (byteLength > 65532) throw new Error('Redirected name exceeds UNICODE_STRING capacity');
  const text = Memory.allocUtf16String(target);
  const unicode = Memory.alloc(ps === 4 ? 8 : 16);
  unicode.writeByteArray(new Uint8Array(ps === 4 ? 8 : 16));
  unicode.writeU16(byteLength);
  unicode.add(2).writeU16(byteLength + 2);
  unicode.add(ps === 4 ? 4 : 8).writePointer(text);
  const out = Memory.alloc(size);
  Memory.copy(out, attributes, size);
  out.add(ps === 4 ? 4 : 8).writePointer(ptr(0)); // absolute NT target, no inherited RootDirectory
  out.add(ps === 4 ? 8 : 16).writePointer(unicode);
  return {pointer: out, keepalive: [out, unicode, text]};
}
function cloneRenameInformation(information, length, target, ps) {
  const nameStart = ps * 2 + 4;
  if (length < nameStart || information.add(ps * 2).readU32() > length - nameStart)
    throw new Error('Invalid rename/link information length');
  const text = Memory.allocUtf16String(target);
  const bytes = target.length * 2;
  const out = Memory.alloc(nameStart + bytes);
  Memory.copy(out, information, nameStart); // preserve flags/ReplaceIfExists and padding
  out.add(ps).writePointer(ptr(0));
  out.add(ps * 2).writeU32(bytes);
  Memory.copy(out.add(nameStart), text, bytes);
  return {pointer: out, length: nameStart + bytes, keepalive: [out, text]};
}
function completeDirectoryCollision(handle, ioStatus, ps) {
  if (!handle.isNull()) handle.writePointer(ptr(0));
  if (!ioStatus.isNull()) {
    ioStatus.writePointer(ptr(0xc0000035));
    ioStatus.add(ps).writePointer(ptr(4)); // FILE_EXISTS
  }
  return -1073741771; // STATUS_OBJECT_NAME_COLLISION; original create is not called.
}
'''


def exception_observer():
    """Record loaded-module metadata and raw context without invoking symbol services."""
    return r'''
function exceptionAddressEvidence(address, errors, label) {
  const out = {address: address.toString()};
  try {
    const module = Process.findModuleByAddress(address);
    out.module = module === null ? null : {name: module.name, path: module.path,
      base: module.base.toString(), offset: address.sub(module.base).toString()};
  } catch(e) { errors.push(label + ' module: ' + e.message); }
  return out;
}
function observeException(details) {
  const payload = {classification: 'unclassified_observed_exception', diagnostic_errors: [],
    backtrace: {status: 'unavailable', reason: 'no_symbol_resolution'}};
  try {
    payload.exceptionType = details.type;
    payload.address = details.address.toString();
    payload.memory = details.memory || null;
    payload.context = details.context;
    payload.location = exceptionAddressEvidence(details.address, payload.diagnostic_errors, 'exception');
  } catch(e) { payload.diagnostic_errors.push('observer: ' + e.message); }
  try { announce('exception', payload); } catch (_) { /* A failed observer must not handle the original exception. */ }
  return false;
}
'''


def protected_input_paths(manifest):
    paths = [manifest["original_save"], manifest["base_dir"], manifest["driver_state"]["source"],
             manifest["exe_source"], *manifest["pac_sources"]]
    paths.extend(path for path in (manifest.get("seed_source"), manifest.get("app_manifest_source")) if path is not None)
    return list(dict.fromkeys(paths))


def isolation_script(case, manifest):
    # Verified sub_764FA0 calls: keep return-address gating specific to this build.
    with (case / "game" / manifest["exe_name"]).open("rb") as stream:
        for offset, expected in ((0x3643B9, bytes.fromhex("ff1560a27d00")),
                                 (0x3643CD, bytes.fromhex("ff1574a27d00"))):
            stream.seek(offset)
            if stream.read(len(expected)) != expected:
                raise ValueError(f"Unknown Documents call site at file offset 0x{offset:X}")
    driver_source = manifest["driver_state"]["source"]
    protected = protected_input_paths(manifest)
    config = {"documents": manifest["original_documents"], "redirect": str(case / "docs"),
              "allowed": [str(case), native_path(case)], "originalSave": manifest["original_save"],
              "repository": [str(PROJECT_ROOT), native_path(PROJECT_ROOT)],
              "protected": protected + [native_path(path) for path in protected],
              "saveAliasSources": [manifest["original_save"], native_path(manifest["original_save"])],
              "saveAliasTarget": "\\??\\" + str(case / "docs" / SAVE_SUFFIX),
              "driverStateSources": [driver_source, native_path(driver_source)],
              "driverStateTarget": "\\??\\" + str(case / NVIDIA_DRS_COPY)}
    return isolation_policy(config) + isolation_structures() + exception_observer() + r'''
const hooks = [];
const pidls = new Set();
const retained = [];
const ps = Process.pointerSize;
const main = Process.mainModule;
const locationReturn = main.base.add(0x364fbf);
const pathReturn = main.base.add(0x364fd3);
function announce(type, data) { send(Object.assign({type: type}, data)); }
function inspectWrite(api, path, details) {
  const scope = writeScope(path);
  if (scope === 'external') {
    try { announce('external_write', Object.assign({api: api, path: path,
      observation: 'permitted request; actual disk mutation not asserted'}, details || {})); }
    catch (_) { /* Optional observation must not turn normal OS I/O into a failure. */ }
  }
  return scope;
}
function exp(module, name) { return Process.getModuleByName(module).getExportByName(name); }
const fileType = new NativeFunction(exp('kernel32.dll', 'GetFileType'), 'uint32', ['pointer'], 'stdcall');
function ipcPath(path) {
  const p = normalize(path);
  return p.startsWith('\\device\\namedpipe\\') || p.startsWith('pipe\\') ||
    p.startsWith('\\\\.\\pipe\\');
}
function ipcHandle(handle) { return [2, 3].includes(fileType(handle)); }
const finalPath = new NativeFunction(exp('kernel32.dll', 'GetFinalPathNameByHandleW'),
  'uint32', ['pointer', 'pointer', 'uint32', 'uint32'], 'stdcall');
const queryAttributes = new NativeFunction(exp('ntdll.dll', 'NtQueryAttributesFile'),
  'int32', ['pointer', 'pointer'], 'stdcall');
function existingDirectory(attributes) {
  const basic = Memory.alloc(40);
  return queryAttributes(attributes, basic) === 0 && (basic.add(32).readU32() & 0x10) !== 0;
}
function handlePath(handle) {
  const out = Memory.alloc(65536);
  const n = finalPath(handle, out, 32768, 0);
  if (n === 0 || n >= 32768) throw new Error('Cannot resolve file handle ' + handle);
  return out.readUtf16String(n);
}
function objectPath(attributes) {
  if (attributes.isNull()) throw new Error('Missing OBJECT_ATTRIBUTES');
  const root = attributes.add(ps === 4 ? 4 : 8).readPointer();
  const u = attributes.add(ps === 4 ? 8 : 16).readPointer();
  if (u.isNull()) throw new Error('Missing object name');
  const length = u.readU16();
  const buffer = u.add(ps === 4 ? 4 : 8).readPointer();
  let name = buffer.readUtf16String(length / 2);
  if (!root.isNull()) name = handlePath(root) + '\\' + name;
  return name;
}
function saveObjectRoute(attributes) {
  const from = objectPath(attributes);
  const saveTarget = aliasSavePath(from);
  const to = saveTarget === null ? aliasDriverState(from) : saveTarget;
  if (to === null) return {attributes: attributes, path: from, from: from, to: null, keepalive: []};
  const copy = cloneObjectAttributes(attributes, to, ps);
  return {attributes: copy.pointer, path: to, from: from, to: to, keepalive: copy.keepalive,
    kind: saveTarget === null ? 'driver' : 'save'};
}
function reportSaveRoute(api, route, result) {
  if (route.to !== null) announce(route.kind === 'driver' ? 'driver_state_redirected' : 'save_path_redirected',
    {api: api, from: route.from, to: route.to,
    status: '0x' + (result >>> 0).toString(16), succeeded: result >= 0});
  // Keep all nested native allocations reachable until the original call returns.
  route.keepalive.length;
  return result;
}
function deny(api, path, reason, status, details) {
  announce('guard_violation', Object.assign({api: api, path: path, reason: reason}, details || {}));
  if (status && !status.isNull()) {
    try { status.writeU32(0xc0000022); status.add(ps).writePointer(ptr(0)); } catch (_) {}
  }
  return -1073741790; // STATUS_ACCESS_DENIED, never execute the original operation.
}
function writable(access, options, disposition) {
  return (access & 0x520d0116) !== 0 || (options & 0x1000) !== 0 || disposition !== 1;
}
function replace(name, types, callback) {
  const address = exp('ntdll.dll', name);
  const original = new NativeFunction(address, 'int32', types, 'stdcall');
  const replacement = new NativeCallback(function (...args) {
    return callback(original, args, this && this.returnAddress ? this.returnAddress.toString() : null);
  },
    'int32', types, 'stdcall');
  retained.push(original, replacement);
  Interceptor.replace(address, replacement);
  hooks.push(name);
}
replace('NtCreateFile', ['pointer','uint32','pointer','pointer','pointer','uint32','uint32','uint32','uint32','pointer','uint32'],
  function(original, a, caller) {
    let path = null;
    const details = {access: '0x' + a[1].toString(16), options: '0x' + a[8].toString(16),
      disposition: a[7], caller: caller};
    try {
      const route = saveObjectRoute(a[2]);
      a[2] = route.attributes;
      path = route.path;
      if (a[7] === 2 && (a[8] & 1) !== 0 && directoryCreateCollision(a[8], a[7], existingDirectory(a[2]))) {
        const result = completeDirectoryCollision(a[0], a[3], ps);
        announce('directory_create_collision', Object.assign({api: 'NtCreateFile', path: path,
          status: '0xc0000035', nativeCreateCalled: false}, details));
        return reportSaveRoute('NtCreateFile', route, result);
      }
      const hid = hidDeviceOpen(path, a[7], a[8]);
      if (hid) announce('hid_device_open', Object.assign({api: 'NtCreateFile', path: path}, details));
      const mmcss = mmcssDeviceOpen(path, a[1], a[7], a[8]);
      if (mmcss) announce('mmcss_device_open', Object.assign({api: 'NtCreateFile', path: path}, details));
      if (writable(a[1], a[8], a[7]) && !hid && !mmcss && !ipcPath(path)) {
        const scope = inspectWrite('NtCreateFile', path, details);
        if (!['case', 'external'].includes(scope)) {
          if (scope === 'unresolved' || !metadataOpenOnly(a[1], a[8], a[7], existingDirectory(a[2])))
            return deny('NtCreateFile', path, 'write rejected: ' + scope, a[3], details);
          announce('directory_metadata_open', Object.assign({api: 'NtCreateFile', path: path}, details));
        }
      }
      return reportSaveRoute('NtCreateFile', route, original(...a));
    } catch (e) { return deny('NtCreateFile', path, e.message, a[3], details); }
  });
replace('NtOpenFile', ['pointer','uint32','pointer','pointer','uint32','uint32'], function(original, a, caller) {
  let path = null;
  const details = {access: '0x' + a[1].toString(16), options: '0x' + a[5].toString(16),
    disposition: 1, caller: caller};
  try {
    const route = saveObjectRoute(a[2]);
    a[2] = route.attributes;
    path = route.path;
    const hid = hidDeviceOpen(path, 1, a[5]);
    if (hid) announce('hid_device_open', Object.assign({api: 'NtOpenFile', path: path}, details));
    const mmcss = mmcssDeviceOpen(path, a[1], 1, a[5]);
    if (mmcss) announce('mmcss_device_open', Object.assign({api: 'NtOpenFile', path: path}, details));
    if (writable(a[1], a[5], 1) && !hid && !mmcss && !ipcPath(path)) {
      const scope = inspectWrite('NtOpenFile', path, details);
      if (!['case', 'external'].includes(scope)) {
        if (scope === 'unresolved' || !metadataOpenOnly(a[1], a[5], 1, existingDirectory(a[2])))
          return deny('NtOpenFile', path, 'write rejected: ' + scope, a[3], details);
        announce('directory_metadata_open', Object.assign({api: 'NtOpenFile', path: path}, details));
      }
    }
    return reportSaveRoute('NtOpenFile', route, original(...a));
  } catch (e) { return deny('NtOpenFile', path, e.message, a[3], details); }
});
replace('NtDeleteFile', ['pointer'], function(original, a) {
  let path = null;
  try {
    const route = saveObjectRoute(a[0]);
    a[0] = route.attributes;
    path = route.path;
    const scope = inspectWrite('NtDeleteFile', path);
    if (!permitted(path)) return deny('NtDeleteFile', path, 'delete rejected: ' + scope);
    return reportSaveRoute('NtDeleteFile', route, original(...a));
  }
  catch (e) { return deny('NtDeleteFile', path, e.message); }
});
for (const name of ['NtQueryAttributesFile', 'NtQueryFullAttributesFile']) {
  replace(name, ['pointer','pointer'], function(original, a) {
    try {
      const route = saveObjectRoute(a[0]);
      a[0] = route.attributes;
      return reportSaveRoute(name, route, original(...a));
    } catch(e) { return deny(name, null, e.message); }
  });
}
replace('NtWriteFile', ['pointer','pointer','pointer','pointer','pointer','pointer','uint32','pointer','pointer'], function(original, a) {
  if (ipcHandle(a[0])) return original(...a);
  let path = null;
  try {
    path = handlePath(a[0]);
    const scope = inspectWrite('NtWriteFile', path, {bytesRequested: a[6]});
    if (!permitted(path)) return deny('NtWriteFile', path, 'write rejected: ' + scope, a[4]);
  }
  catch (e) { return deny('NtWriteFile', path, e.message, a[4]); }
  return original(...a);
});
replace('NtSetInformationFile', ['pointer','pointer','pointer','uint32','uint32'], function(original, a) {
  if (a[4] === 14) return original(...a); // FilePositionInformation only moves a read/write cursor.
  if (ipcHandle(a[0])) return original(...a);
  let path = null;
  try {
    path = handlePath(a[0]);
    const scope = inspectWrite('NtSetInformationFile', path, {informationClass: a[4]});
    if (!permitted(path)) return deny('NtSetInformationFile', path, 'metadata write rejected: ' + scope, a[1]);
    // Rename/link destinations can escape even when the source handle is inside.
    if ([10,11,56,57,65,66,72,73].includes(a[4])) {
      if (a[3] < ps * 2 + 4) throw new Error('Truncated rename/link information');
      const root = a[2].add(ps).readPointer();
      const lengthOffset = ps * 2;
      const n = a[2].add(lengthOffset).readU32();
      if (n > a[3] - lengthOffset - 4) throw new Error('Invalid rename/link name length');
      let destination = a[2].add(lengthOffset + 4).readUtf16String(n / 2);
      if (!root.isNull()) destination = handlePath(root) + '\\' + destination;
      else if (!destination.includes(':') && !destination.startsWith('\\'))
        destination = path.slice(0, path.lastIndexOf('\\') + 1) + destination;
      const saveTarget = aliasSavePath(destination);
      const aliased = saveTarget === null ? aliasDriverState(destination) : saveTarget;
      if (aliased !== null) {
        if (!permitted(aliased)) return deny('NtSetInformationFile', aliased, 'invalid Save alias target', a[1]);
        const copy = cloneRenameInformation(a[2], a[3], aliased, ps);
        a[2] = copy.pointer;
        a[3] = copy.length;
        return reportSaveRoute('NtSetInformationFile', {from: destination, to: aliased, keepalive: copy.keepalive,
          kind: saveTarget === null ? 'driver' : 'save'}, original(...a));
      }
      const destinationScope = inspectWrite('NtSetInformationFile', destination, {operation: 'rename/link target'});
      if (!permitted(destination)) return deny('NtSetInformationFile', destination, 'rename/link rejected: ' + destinationScope, a[1]);
    }
  } catch(e) { return deny('NtSetInformationFile', path, e.message, a[1]); }
  return original(...a);
});
replace('NtCreateUserProcess', ['pointer','pointer','uint32','uint32','pointer','pointer','uint32','uint32','pointer','pointer','pointer'],
  function(original, a) {
    return deny('NtCreateUserProcess', null, 'child processes would not inherit the isolation hooks');
  });
Interceptor.attach(exp('shell32.dll', 'SHGetSpecialFolderLocation'), {
  onEnter(args) {
    this.personal = (args[1].toUInt32() & 0xff) === 5;
    this.gameCall = this.returnAddress.equals(locationReturn);
    this.output = args[2];
    this.caller = this.returnAddress.toString();
    if (this.personal || this.gameCall)
      announce('documents_location_enter', {csidl: args[1].toUInt32(), caller: this.caller, gameCall: this.gameCall});
  },
  onLeave(result) {
    if (!this.personal && !this.gameCall) return;
    const pidl = result.toInt32() === 0 ? this.output.readPointer().toString() : null;
    if (this.personal && this.gameCall && pidl !== null) pidls.add(pidl);
    announce('documents_location_result', {result: result.toInt32(), pidl: pidl,
      caller: this.caller, gameCall: this.gameCall});
  }
});
hooks.push('SHGetSpecialFolderLocation');
for (const name of ['SHGetPathFromIDListA', 'SHGetPathFromIDListW']) {
  Interceptor.attach(exp('shell32.dll', name), {
    onEnter(args) {
      this.pidl = args[0].toString(); this.output = args[1];
      this.gameCall = name === 'SHGetPathFromIDListA' && this.returnAddress.equals(pathReturn);
      this.caller = this.returnAddress.toString();
    },
    onLeave(result) {
      if (!pidls.has(this.pidl) && !this.gameCall) return;
      try {
        const wide = name.endsWith('W');
        const succeeded = result.toInt32() !== 0;
        const actual = succeeded ? (wide ? this.output.readUtf16String() : this.output.readAnsiString()) : null;
        announce('documents_path_result', {api: name, result: result.toInt32(), pidl: this.pidl,
          tracked: pidls.has(this.pidl), gameCall: this.gameCall, caller: this.caller, path: actual});
        const redirected = redirectDocuments(pidls, this.pidl, succeeded, this.gameCall, () => actual, value => {
          if (wide) this.output.writeUtf16String(value);
          else this.output.writeUtf8String(value); // validated ASCII, MAX_PATH bounded
        });
        if (redirected) announce('documents_redirected', {api: name, from: actual, to: config.redirect, caller: this.caller});
      } catch(e) { announce('isolation_error', {api: name, error: e.message}); }
    }
  });
  hooks.push(name);
}
Process.setExceptionHandler(observeException);
Interceptor.flush();
announce('hook_ready', {hooks: hooks, module: main.path, base: main.base.toString(),
  pointerSize: ps, repository: config.repository, protectedInputs: config.protected,
  documentsLocationReturn: locationReturn.toString(),
  documentsPathReturn: pathReturn.toString(), saveAliasSources: config.saveAliasSources, saveAliasTarget: config.saveAliasTarget,
  driverStateSources: config.driverStateSources, driverStateTarget: config.driverStateTarget,
  policy: 'input/save preservation: case and resolved external OS writes allowed; repository neighbors and original inputs protected; exceptions never handled'});
'''


def launch_case(case_dir, *, timeout=0, ready_timeout=30, runtime_root=RUNTIME_ROOT):
    if os.name != "nt":
        raise RuntimeError("Launching is supported only on Windows")
    import frida
    case = check_no_links(case_dir)
    manifest = verify_prepared(case, runtime_root)
    stop_file = case / "STOP"
    if stop_file.exists():
        raise ValueError("STOP already exists; prepare a fresh case")
    lock = case / "launch.lock"
    with lock.open("x", encoding="utf-8", newline="\r\n") as stream:
        json.dump({"launcher_pid": os.getpid(), "created_at": utc_now()}, stream)
        stream.write("\n")
    state = {"status": "launching", "started_at": utc_now(), "case": str(case),
             "fresh_save": manifest.get("fresh_save", False), "seed_mode": manifest.get("seed_mode", "original"),
             "seed_source": manifest.get("seed_source", manifest["original_save"]),
             "app_manifest_source": manifest.get("app_manifest_source"),
             "launcher_pid": os.getpid(), "manifest_sha256": file_record(case / "manifest.json")["sha256"],
             "game_files": manifest["game_files"], "original_save": manifest["original_save"],
             "process": None, "hook_ready": False, "redirect_count": 0, "exception_count": 0,
             "save_path_redirect_count": 0, "save_path_redirect_success_count": 0,
             "driver_state_redirect_count": 0, "driver_state_redirect_success_count": 0,
             "external_write_count": 0,
             "guard_violations": [], "stop_reason": None, "exit_code": None,
             "closed_handles": [], "frida_detached": False, "runtime_acceptance": "not_evaluated"}
    events = queue.Queue()
    cancel = threading.Event()
    attachment = {}
    child = None
    worker = None
    failure = None
    write_json(case / "state.json", state)
    log = (case / "events.jsonl").open("x", encoding="utf-8", newline="\r\n", buffering=1)

    def record(event):
        event = {"at": utc_now(), **event}
        log.write(json.dumps(event, ensure_ascii=False, default=str) + "\n")
        return event

    def on_message(message, data):
        events.put({"event": "frida_message", "message": message})

    def install():
        session = None
        try:
            if cancel.is_set():
                return
            child.require_identity()
            session = frida.attach(child.pid)
            attachment["session"] = session
            session.on("detached", lambda reason, crash: events.put(
                {"event": "frida_detached", "reason": reason, "crash": str(crash) if crash else None}))
            if cancel.is_set():
                return
            script = session.create_script(isolation_script(case, manifest), name="bhe-save-isolation")
            attachment["script"] = script
            script.on("message", on_message)
            script.load()
            events.put({"event": "hooks_loaded"})
        except BaseException as exc:
            events.put({"event": "isolation_install_failed", "error": repr(exc)})

    def consume():
        nonlocal failure
        while True:
            try:
                event = events.get_nowait()
            except queue.Empty:
                break
            record(event)
            message = event.get("message", {})
            payload = message.get("payload", {}) if message.get("type") == "send" else {}
            kind = payload.get("type")
            if kind == "hook_ready":
                state["hook_ready"] = True
                state["hooks"] = payload
            elif kind == "documents_redirected":
                state["redirect_count"] += 1
            elif kind == "save_path_redirected":
                state["save_path_redirect_count"] += 1
                if payload.get("succeeded"):
                    state["save_path_redirect_success_count"] += 1
            elif kind == "driver_state_redirected":
                state["driver_state_redirect_count"] += 1
                if payload.get("succeeded"):
                    state["driver_state_redirect_success_count"] += 1
            elif kind == "external_write":
                state["external_write_count"] += 1
            elif kind == "exception":
                state["exception_count"] += 1
                state["last_exception"] = payload
            elif kind in ("guard_violation", "isolation_error"):
                state["guard_violations"].append(payload)
                failure = kind
            if message.get("type") == "error" or event["event"] == "isolation_install_failed":
                failure = "isolation_install_failed"
            if event["event"] == "frida_detached":
                state["frida_detached"] = True
                state["detach_reason"] = event.get("reason")
                if (event.get("reason") != "process-terminated" and child and child.process_handle
                        and child.poll() is None):
                    failure = "frida_detached_while_process_alive"

    try:
        # Recheck immediately before creating a process; no shared source is copied at launch.
        require_unchanged(manifest["original_save"], manifest["original_save_files"], "Original save")
        child = OwnedProcess(case / "game" / manifest["exe_name"], case / "game")
        state["process"] = child.identity
        state["status"] = "suspended_installing_hooks"
        record({"event": "created_suspended", "process": child.identity})
        write_json(case / "state.json", state)
        worker = threading.Thread(target=install, name="frida-isolation-installer", daemon=True)
        worker.start()
        deadline = time.monotonic() + ready_timeout
        while not state["hook_ready"]:
            consume()
            if failure or child.poll() is not None:
                raise RuntimeError(failure or "Process exited before isolation was ready")
            if time.monotonic() >= deadline:
                raise TimeoutError("Isolation hook_ready deadline exceeded; primary thread was not resumed")
            if stop_file.exists():
                raise InterruptedError("STOP requested before resume")
            time.sleep(0.1)
        consume()
        if failure:
            raise RuntimeError(failure)
        child.resume()
        state.update(status="running", resumed_at=utc_now())
        record({"event": "resumed", "process": child.identity})
        write_json(case / "state.json", state)
        print(json.dumps({"status": "running", "pid": child.pid, "state": str(case / "state.json")}), flush=True)
        start = last_status = time.monotonic()
        while child.poll() is None:
            consume()
            if failure:
                raise RuntimeError(failure)
            elapsed = time.monotonic() - start
            if stop_file.exists():
                state["stop_reason"] = "STOP_file"
                break
            if timeout > 0 and elapsed >= timeout:
                state["stop_reason"] = "timeout"
                break
            if time.monotonic() - last_status >= 1:
                state["runtime_seconds"] = round(elapsed, 3)
                write_json(case / "state.json", state)
                last_status = time.monotonic()
            time.sleep(0.1)
        consume()
    except KeyboardInterrupt:
        state["stop_reason"] = "keyboard_interrupt"
    except InterruptedError:
        state["stop_reason"] = "STOP_file"
    except BaseException as exc:
        if isinstance(exc, OwnedProcessIdentityError):
            child = exc.process
            state["process"] = {"pid": child.pid, "thread_id": child.tid,
                                "image": str(child.executable), "identity_verified": False}
        failure = failure or repr(exc)
        try:
            record({"event": "launcher_error", "error": repr(exc)})
        except BaseException as log_exc:
            state["cleanup_log_error"] = repr(log_exc)
    finally:
        cancel.set()

        def cleanup_record(event):
            # Logging is best effort; it must never skip termination or release a guard.
            try:
                record(event)
            except BaseException as exc:
                state["cleanup_log_error"] = repr(exc)

        def publish_retained_state():
            state.update(status="failed", failure=failure, cleanup_pending=True,
                         retained_handles={"process": child.process_handle, "thread": child.thread_handle},
                         guard_session_retained=attachment.get("session") is not None)
            try:
                write_json(case / "state.json", state)
            except BaseException as exc:
                state["cleanup_state_error"] = repr(exc)

        # Keep the guard installed until the owned process is confirmed dead.
        if child is not None:
            while state["exit_code"] is None:
                try:
                    if child.poll() is None:
                        cleanup_record({"event": "terminating_owned_process", "process": state["process"],
                                        "reason": failure or state["stop_reason"] or "launcher_cleanup"})
                        child.terminate()
                    state["exit_code"] = child.poll()
                    if state["exit_code"] is None:
                        raise RuntimeError("Owned process exit is still unconfirmed")
                except BaseException as exc:
                    failure = failure or repr(exc)
                    state["cleanup_error"] = repr(exc)
                    publish_retained_state()
                    # Do not return from launch_case: doing so could release the
                    # Frida session or let Python exit while the game is alive.
                    try:
                        time.sleep(0.25)
                    except BaseException as wait_exc:
                        state["cleanup_wait_error"] = repr(wait_exc)
            if worker:
                # A late attach/load still belongs to this owner. Wait for it
                # before the single detach and before releasing the PID's handle.
                while worker.is_alive():
                    try:
                        worker.join(timeout=0.25)
                    except BaseException as exc:
                        failure = failure or repr(exc)
                        state["cleanup_error"] = repr(exc)
                    if worker.is_alive():
                        publish_retained_state()
                state["installer_thread_alive"] = worker.is_alive()
            state["cleanup_pending"] = False
            if state["exit_code"] is not None:
                session = attachment.get("session")
                if session:
                    try:
                        session.detach()
                        state["frida_detached"] = True
                    except Exception as exc:
                        state["detach_note"] = repr(exc)
                try:
                    child.close()
                except Exception as exc:
                    failure = failure or repr(exc)
                    state["handle_close_error"] = repr(exc)
                state["closed_handles"] = child.closed
            try:
                consume()
            except BaseException as exc:
                failure = failure or repr(exc)
                state["cleanup_log_error"] = repr(exc)
        try:
            state["original_save_check"] = compare_manifest(manifest["original_save"], manifest["original_save_files"])
            if not state["original_save_check"]["unchanged"]:
                failure = "original_save_changed"
            driver = manifest["driver_state"]
            try:
                state["original_driver_state_check"] = compare_manifest(driver["source"], driver["files"])
                state["original_driver_state_check"]["attribution"] = "not_determined_external_writers_possible"
            except Exception as exc:
                state["original_driver_state_check"] = {"unchanged": None, "error": repr(exc),
                    "attribution": "not_determined_external_writers_possible"}
            try:
                state["driver_state_copy_after"] = tree_manifest(case / NVIDIA_DRS_COPY)
            except Exception as exc:
                state["driver_state_copy_after"] = {"error": repr(exc)}
            state["game_files_after"] = tree_manifest(case / "game")
            payload_names = manifest["game_files"].keys() | state["game_files_after"].keys()
            changed_payload = sorted(name for name in payload_names
                                     if name.lower().endswith((".exe", ".pac", ".manifest")) and
                                     state["game_files_after"].get(name) != manifest["game_files"].get(name))
            if changed_payload:
                failure = "runtime_payload_changed"
                state["changed_payload"] = changed_payload
        except BaseException as exc:
            failure = failure or repr(exc)
        if not failure and not state["stop_reason"]:
            if not state["hook_ready"] or (state["redirect_count"] == 0 and state["save_path_redirect_success_count"] == 0):
                failure = "documents_or_successful_save_io_redirection_was_not_observed"
            elif state["exit_code"] != 0:
                failure = f"abnormal_exit_{state['exit_code']}"
        state.update(status="failed" if failure else ("stopped" if state["stop_reason"] else "exited"),
                     failure=failure, finished_at=utc_now())
        cleanup_record({"event": "finished", "status": state["status"], "failure": failure})
        write_json(case / "state.json", state)
        log.close()
    print(json.dumps({"status": state["status"], "failure": failure, "state": str(case / "state.json")}), flush=True)
    return 1 if failure else (2 if state["stop_reason"] else 0)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    prepare = sub.add_parser("prepare", help="Copy explicit inputs into a new, never-reused case")
    prepare.add_argument("--case", required=True)
    prepare.add_argument("--base", type=Path, required=True)
    prepare.add_argument("--exe", type=Path, required=True)
    mode = prepare.add_mutually_exclusive_group(required=True)
    mode.add_argument("--pac", type=Path, action="append")
    mode.add_argument("--vanilla", action="store_true", help="Use the explicit base/EXE without additional PACs")
    prepare.add_argument("--save-dir", type=Path, required=True)
    seed_mode = prepare.add_mutually_exclusive_group()
    seed_mode.add_argument("--fresh-save", action="store_true", help="Create only an empty private Save; preserve the real --save-dir")
    seed_mode.add_argument("--save-seed-dir", type=Path, help="Copy this independent seed while preserving the real --save-dir")
    prepare.add_argument("--app-manifest", type=Path, help="Copy the explicitly supplied application manifest into this case")
    launch = sub.add_parser("launch", help="Start one prepared case with isolation installed before resume")
    launch.add_argument("--case", required=True)
    launch.add_argument("--timeout", type=float, default=0, help="Seconds; 0 waits for exit, Ctrl+C or case/STOP")
    launch.add_argument("--ready-timeout", type=float, default=30)
    args = parser.parse_args(argv)
    try:
        if args.command == "prepare":
            result = prepare_case(base_dir=args.base, exe_path=args.exe, pac_paths=args.pac or [],
                                  save_dir=args.save_dir, case_name=args.case, vanilla=args.vanilla,
                                  fresh_save=args.fresh_save, save_seed_dir=args.save_seed_dir,
                                  app_manifest_path=args.app_manifest)
            print(json.dumps({"status": "prepared", "case": str(result)}))
            return 0
        if args.timeout < 0 or args.ready_timeout <= 0 or args.ready_timeout > 60:
            raise ValueError("timeout must be >= 0; ready-timeout must be in (0, 60]")
        return launch_case(case_path(RUNTIME_ROOT, args.case), timeout=args.timeout, ready_timeout=args.ready_timeout)
    except Exception as exc:
        print(json.dumps({"status": "refused", "error": repr(exc)}), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
