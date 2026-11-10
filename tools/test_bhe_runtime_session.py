"""Offline safety contracts; never starts a game or attaches to a process."""

import sys
sys.dont_write_bytecode = True

import hashlib
import inspect
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import threading
import types
import unittest
from unittest.mock import patch

try:
    import bhe_runtime_session as harness
except ModuleNotFoundError:
    harness = None


class AtomicJsonReplacementTests(unittest.TestCase):
    """Only owned repository files; no launcher or live-process dependency."""
    def setUp(self):
        fixture_parent = Path(__file__).resolve().parents[1] / "target/bhe-runtime-test-fixtures"
        fixture_parent.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix="atomic-json-", dir=fixture_parent)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.environment = patch.dict(os.environ, {name: str(self.root) for name in ("TEMP", "TMP", "TMPDIR")})
        self.environment.start()
        self.addCleanup(self.environment.stop)
        self.path = self.root / "state.json"
        self.initial = b'{"generation": 0}\r\n'
        self.path.write_bytes(self.initial)

    @staticmethod
    def blocked_error(code):
        error = PermissionError(13, "owned fixture replacement blocked")
        error.winerror = code
        return error

    @unittest.skipUnless(os.name == "nt", "requires Windows ordinary-read sharing semantics")
    def test_real_windows_reader_release_allows_atomic_replacement(self):
        reader = self.path.open("rb")
        self.addCleanup(reader.close)
        waits = []
        def release_reader(seconds):
            waits.append(seconds)
            self.assertEqual(self.path.read_bytes(), self.initial)
            self.assertEqual(reader.read(), self.initial)
            pending = self.path.with_name("state.json.new").read_bytes()
            self.assertEqual(json.loads(pending), {"generation": 1})
            reader.close()
        with patch.object(harness.time, "sleep", side_effect=release_reader):
            harness.write_json(self.path, {"generation": 1})
        self.assertEqual(len(waits), 1)
        self.assertGreater(waits[0], 0)
        self.assertLessEqual(waits[0], .05)
        self.assertEqual(json.loads(self.path.read_bytes()), {"generation": 1})
        self.assertFalse(self.path.with_name("state.json.new").exists())

    def test_transient_windows_codes_retry_same_temp_without_changing_original(self):
        real_replace = Path.replace
        for code in (5, 32, 33):
            with self.subTest(winerror=code):
                self.path.write_bytes(self.initial)
                state = types.SimpleNamespace(now=0.0, attempts=0, sleeps=[], pending=None)
                def advance(seconds):
                    self.assertGreater(seconds, 0)
                    self.assertLessEqual(seconds, .05)
                    state.sleeps.append(seconds)
                    state.now += seconds
                def replacement(source, target):
                    state.attempts += 1
                    self.assertEqual(Path(target).read_bytes(), self.initial)
                    content = source.read_bytes()
                    if state.pending is None:
                        state.pending = content
                    self.assertEqual(content, state.pending)
                    self.assertFalse(content.startswith(b"\xef\xbb\xbf"))
                    self.assertNotIn(b"\n", content.replace(b"\r\n", b""))
                    if state.attempts <= 2:
                        raise self.blocked_error(code)
                    return real_replace(source, target)
                with patch.object(Path, "replace", autospec=True, side_effect=replacement), \
                        patch.object(harness.time, "monotonic", side_effect=lambda: state.now), \
                        patch.object(harness.time, "sleep", side_effect=advance), \
                        patch.object(harness.json, "dump", wraps=json.dump) as dump:
                    harness.write_json(self.path, {"generation": 2, "text": "状态"})
                self.assertEqual(state.attempts, 3)
                self.assertEqual(len(state.sleeps), 2)
                self.assertEqual(dump.call_count, 1)
                self.assertEqual(json.loads(self.path.read_bytes()), {"generation": 2, "text": "状态"})

    def test_permanent_sharing_failure_is_bounded_and_preserves_original_and_error(self):
        failure = self.blocked_error(32)
        clock = types.SimpleNamespace(now=0.0, sleeps=[])
        def advance(seconds):
            self.assertGreater(seconds, 0)
            self.assertLessEqual(seconds, .05)
            self.assertEqual(self.path.read_bytes(), self.initial)
            clock.sleeps.append(seconds)
            clock.now += seconds
        with patch.object(Path, "replace", autospec=True, side_effect=failure) as replacement, \
                patch.object(harness.time, "monotonic", side_effect=lambda: clock.now), \
                patch.object(harness.time, "sleep", side_effect=advance), \
                patch.object(harness.json, "dump", wraps=json.dump) as dump:
            with self.assertRaises(PermissionError) as raised:
                harness.write_json(self.path, {"generation": 3})
        self.assertIs(raised.exception, failure)
        self.assertEqual(raised.exception.winerror, 32)
        self.assertGreater(len(clock.sleeps), 0)
        self.assertLessEqual(len(clock.sleeps), 40)
        self.assertLessEqual(sum(clock.sleeps), 1.000001)
        self.assertLessEqual(replacement.call_count, 41)
        self.assertEqual(dump.call_count, 1)
        self.assertEqual(self.path.read_bytes(), self.initial)
        self.assertEqual(json.loads(self.path.with_name("state.json.new").read_bytes()), {"generation": 3})

    def test_unrelated_os_errors_fail_immediately_without_retry(self):
        for code in (None, 2, 112):
            with self.subTest(winerror=code):
                failure = OSError(28, "unrelated filesystem failure")
                if code is not None:
                    failure.winerror = code
                with patch.object(Path, "replace", autospec=True, side_effect=failure) as replacement, \
                        patch.object(harness.time, "sleep") as sleep:
                    with self.assertRaises(OSError) as raised:
                        harness.write_json(self.path, {"generation": 4})
                self.assertIs(raised.exception, failure)
                self.assertEqual(replacement.call_count, 1)
                sleep.assert_not_called()
                self.assertEqual(self.path.read_bytes(), self.initial)

class RuntimeSessionTests(unittest.TestCase):
    def setUp(self):
        if harness is None:
            self.fail("The isolated runtime session implementation is missing")
        fixture_parent = Path(__file__).resolve().parents[1] / "target" / "bhe-runtime-test-fixtures"
        fixture_parent.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=fixture_parent)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name, value in (("PROJECT_ROOT", self.root),
                            ("RUNTIME_ROOT", self.root / "target/bhe-playable-runtime")):
            replacement = patch.object(harness, name, value)
            replacement.start()
            self.addCleanup(replacement.stop)
        self.base = self.root / "base"
        self.base.mkdir()
        (self.base / "Config.pac").write_bytes(b"base config")
        (self.base / "Update2.pac").write_bytes(b"base update")
        (self.base / "BaldrSky.exe").write_bytes(b"old base executable")
        self.exe = self.root / "new.exe"
        with self.exe.open("wb") as stream:
            stream.write(b"MZ")
            stream.seek(0x2789F8)
            stream.write(b"\x8b\xff")
        self.pac = self.root / "Update3.pac"
        self.pac.write_bytes(b"new resources")
        self.save = self.root / "original docs" / "GIGA" / "BALDRSKY_DIVEX" / "Save"
        self.save.mkdir(parents=True)
        (self.save / "Config.dat").write_bytes(b"user configuration")
        (self.save / "Script.dat").write_bytes(b"user progress")
        self.runtime = self.root / "target" / "bhe-playable-runtime"
        self.app_manifest = self.root / "supplied.manifest"
        self.app_manifest.write_bytes(b"<assembly/>")  # Offline copy fixture, never used to launch.
        self.seed = self.root / "independent-save-seed"
        self.seed.mkdir()
        (self.seed / "Script.dat").write_bytes(b"preserved progress with repaired title")
        self.driver = self.root / "ProgramData/NVIDIA Corporation/Drs"
        (self.driver / "nested").mkdir(parents=True)
        (self.driver / "nvdrssel.bin").write_bytes(b"\x01")
        (self.driver / "nvAppTimestamps").write_bytes(b"old")
        (self.driver / "nested/db.bin").write_bytes(b"database")
        driver_patch = patch.object(harness, "NVIDIA_DRS_SOURCE", self.driver)
        driver_patch.start()
        self.addCleanup(driver_patch.stop)

    def prepare(self, case="case-one", **overrides):
        args = dict(base_dir=self.base, exe_path=self.exe, pac_paths=[self.pac],
                    save_dir=self.save, case_name=case, runtime_root=self.runtime)
        args.update(overrides)
        return harness.prepare_case(**args)

    def require_vanilla_mode(self):
        if "vanilla" not in inspect.signature(harness.prepare_case).parameters:
            self.fail("Explicit vanilla preparation mode is missing")

    def require_diagnostic_input_modes(self):
        if not {"fresh_save", "save_seed_dir", "app_manifest_path"}.issubset(inspect.signature(harness.prepare_case).parameters):
            self.fail("Explicit save-seed and application-manifest controls are missing")

    def test_fresh_save_keeps_original_receipt_and_alias_origin_but_private_seed_empty(self):
        self.require_diagnostic_input_modes()
        case = self.prepare("fresh-save", fresh_save=True)
        manifest = harness.verify_prepared(case, self.runtime)
        self.assertTrue(manifest["fresh_save"])
        self.assertEqual(manifest["seed_mode"], "fresh")
        self.assertIsNone(manifest["seed_source"])
        self.assertEqual(manifest["save_seed_files"], {})
        self.assertEqual(sorted(manifest["original_save_files"]), ["Config.dat", "Script.dat"])
        self.assertEqual(manifest["original_save"], str(self.save))
        self.assertEqual(manifest["original_documents"], str(self.save.parents[2]))
        private = case / "docs/GIGA/BALDRSKY_DIVEX/Save"
        self.assertEqual(list(private.iterdir()), [])
        self.assertEqual((self.save / "Script.dat").read_bytes(), b"user progress")
        (private / "unexpected.dat").write_bytes(b"changed before launch")
        with self.assertRaises(ValueError):
            harness.verify_prepared(case, self.runtime)

    def test_independent_seed_copy_and_source_or_private_drift_are_checked(self):
        self.require_diagnostic_input_modes()
        for changed in ("source", "copy"):
            with self.subTest(changed=changed):
                (self.seed / "Script.dat").write_bytes(b"preserved progress with repaired title")
                case = self.prepare("seed-" + changed, save_seed_dir=self.seed)
                manifest = harness.verify_prepared(case, self.runtime)
                self.assertFalse(manifest["fresh_save"])
                self.assertEqual(manifest["seed_mode"], "provided")
                self.assertEqual(manifest["seed_source"], str(self.seed))
                self.assertEqual(list(manifest["save_seed_files"]), ["Script.dat"])
                self.assertEqual(manifest["original_save"], str(self.save))
                private = case / "docs/GIGA/BALDRSKY_DIVEX/Save/Script.dat"
                self.assertEqual(private.read_bytes(), b"preserved progress with repaired title")
                target = self.seed / "Script.dat" if changed == "source" else private
                target.write_bytes(b"changed after prepare")
                with self.assertRaises(ValueError):
                    harness.verify_prepared(case, self.runtime)
                self.assertEqual((self.save / "Script.dat").read_bytes(), b"user progress")

    def test_explicit_app_manifest_copy_hash_and_drift_work_in_both_modes(self):
        self.require_diagnostic_input_modes()
        for vanilla in (False, True):
            for changed in ("source", "copy"):
                with self.subTest(vanilla=vanilla, changed=changed):
                    self.app_manifest.write_bytes(b"<assembly/>")
                    before_exe = self.exe.read_bytes()
                    case = self.prepare(f"manifest-{int(vanilla)}-{changed}", vanilla=vanilla,
                                        pac_paths=[] if vanilla else [self.pac], app_manifest_path=self.app_manifest)
                    manifest = harness.verify_prepared(case, self.runtime)
                    copied = case / "game/BaldrSky.exe.manifest"
                    self.assertEqual(copied.read_bytes(), b"<assembly/>")
                    self.assertEqual(manifest["app_manifest_source"], str(self.app_manifest))
                    self.assertEqual(manifest["game_files"]["BaldrSky.exe.manifest"],
                                     manifest["source_files"][str(self.app_manifest)])
                    self.assertEqual(self.exe.read_bytes(), before_exe)
                    (self.app_manifest if changed == "source" else copied).write_bytes(b"changed manifest")
                    with self.assertRaises(ValueError):
                        harness.verify_prepared(case, self.runtime)

    def test_independent_seed_alias_and_both_extra_inputs_are_protected(self):
        self.require_diagnostic_input_modes()
        case = self.prepare("extra-protection", save_seed_dir=self.seed, app_manifest_path=self.app_manifest)
        manifest = harness.verify_prepared(case, self.runtime)
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        config = {"allowed": [str(case)], "repository": [str(harness.PROJECT_ROOT)],
                  "protected": harness.protected_input_paths(manifest),
                  "saveAliasSources": [manifest["original_save"]],
                  "saveAliasTarget": "\\??\\" + str(case / "docs/GIGA/BALDRSKY_DIVEX/Save")}
        script = harness.isolation_policy(config)
        script += "\nconst paths=" + json.dumps([str(self.save / "Script.dat"), str(self.seed / "Script.dat"),
                                                str(self.app_manifest)]) + ";"
        script += "\nconsole.log(JSON.stringify({writes:paths.map(permitted),realAlias:permitted(aliasSavePath(paths[0])),seedAlias:aliasSavePath(paths[1])}));"
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertEqual(json.loads(result.stdout), {"writes": [False, False, False], "realAlias": True, "seedAlias": None})

    def test_diagnostic_inputs_reject_conflict_overlap_and_links(self):
        self.require_diagnostic_input_modes()
        with self.assertRaises(ValueError):
            self.prepare("conflict", fresh_save=True, save_seed_dir=self.seed)
        overlap = self.runtime / "earlier-seed"
        overlap.mkdir(parents=True)
        (overlap / "keep.dat").write_bytes(b"keep")
        with self.assertRaises(ValueError):
            self.prepare("overlap", save_seed_dir=self.runtime)
        self.assertEqual((overlap / "keep.dat").read_bytes(), b"keep")
        sibling_case = self.prepare("sibling-seed", save_seed_dir=overlap)
        self.assertEqual((sibling_case / "docs/GIGA/BALDRSKY_DIVEX/Save/keep.dat").read_bytes(), b"keep")
        link = self.root / "manifest-link"
        try:
            link.symlink_to(self.app_manifest)
        except OSError:
            self.skipTest("Symlinks unavailable")
        with self.assertRaises(ValueError):
            self.prepare("linked-manifest", app_manifest_path=link)
        seed_link = self.root / "seed-link"
        seed_link.symlink_to(self.seed, target_is_directory=True)
        with self.assertRaises(ValueError):
            self.prepare("linked-seed", save_seed_dir=seed_link)

    def test_vanilla_copies_base_without_extra_pac_and_keeps_all_preservation(self):
        self.require_vanilla_mode()
        original_exe = self.base / "BaldrSky.exe"
        shutil.copy2(self.exe, original_exe)
        before = original_exe.read_bytes()
        case = self.prepare("vanilla", exe_path=original_exe, pac_paths=[], vanilla=True)
        manifest = harness.verify_prepared(case, self.runtime)
        self.assertTrue(manifest["vanilla"])
        self.assertEqual(manifest["pac_sources"], [])
        self.assertEqual(manifest["game_files"], manifest["base_files"])
        self.assertFalse((case / "game/Update3.pac").exists())
        self.assertEqual(original_exe.read_bytes(), before)
        self.assertEqual((case / "docs/GIGA/BALDRSKY_DIVEX/Save/Script.dat").read_bytes(), b"user progress")
        self.assertEqual((case / "driver-cache/Drs/nvAppTimestamps").read_bytes(), b"old")

    def test_default_still_requires_pac_and_vanilla_rejects_selected_pac(self):
        with self.assertRaises(ValueError):
            self.prepare("missing-pac", pac_paths=[])
        self.require_vanilla_mode()
        with self.assertRaises(ValueError):
            self.prepare("conflicting-mode", vanilla=True)
        self.assertFalse(self.runtime.exists())

    def test_vanilla_rejects_unselected_update_overlay_in_base(self):
        self.require_vanilla_mode()
        (self.base / "Update7.pac").write_bytes(b"stale overlay")
        with self.assertRaises(ValueError):
            self.prepare("vanilla-overlay", pac_paths=[], vanilla=True)
        self.assertFalse(self.runtime.exists())

    def test_cli_requires_exactly_one_of_vanilla_or_pac(self):
        self.require_vanilla_mode()
        self.require_diagnostic_input_modes()
        args = ["prepare", "--case", "vanilla-cli", "--base", str(self.base), "--exe", str(self.exe),
                "--save-dir", str(self.save)]
        for mode in ([], ["--vanilla", "--pac", str(self.pac)]):
            with self.subTest(mode=mode), self.assertRaises(SystemExit) as error:
                harness.main(args + mode)
            self.assertEqual(error.exception.code, 2)
        prepare = harness.prepare_case
        with patch.object(harness, "prepare_case", side_effect=lambda **kwargs: prepare(**kwargs, runtime_root=self.runtime)):
            self.assertEqual(harness.main(args + ["--vanilla", "--fresh-save", "--app-manifest", str(self.app_manifest)]), 0)
        manifest = harness.verify_prepared(self.runtime / "vanilla-cli", self.runtime)
        self.assertTrue(manifest["vanilla"])
        self.assertEqual(manifest["pac_sources"], [])
        self.assertEqual(manifest["seed_mode"], "fresh")
        self.assertEqual(manifest["app_manifest_source"], str(self.app_manifest))

    def test_cli_seed_override_is_mutually_exclusive_with_fresh_save(self):
        self.require_diagnostic_input_modes()
        args = ["prepare", "--case", "seed-cli", "--base", str(self.base), "--exe", str(self.exe),
                "--save-dir", str(self.save), "--pac", str(self.pac), "--save-seed-dir", str(self.seed)]
        with self.assertRaises(SystemExit) as error:
            harness.main(args + ["--fresh-save"])
        self.assertEqual(error.exception.code, 2)
        prepare = harness.prepare_case
        with patch.object(harness, "prepare_case", side_effect=lambda **kwargs: prepare(**kwargs, runtime_root=self.runtime)):
            self.assertEqual(harness.main(args), 0)
        self.assertEqual(harness.verify_prepared(self.runtime / "seed-cli", self.runtime)["seed_source"], str(self.seed))

    def test_prepare_copies_exact_bytes_and_preserves_all_inputs(self):
        original = {str(path): path.read_bytes() for directory in (self.base, self.save)
                    for path in directory.rglob("*") if path.is_file()}
        original[str(self.exe)] = self.exe.read_bytes()
        original[str(self.pac)] = self.pac.read_bytes()
        case = self.prepare()
        self.assertEqual(case, self.runtime / "case-one")
        self.assertEqual((case / "game" / "BaldrSky.exe").read_bytes(), original[str(self.exe)])
        self.assertEqual((case / "game" / "Update3.pac").read_bytes(), b"new resources")
        self.assertEqual((case / "docs/GIGA/BALDRSKY_DIVEX/Save/Script.dat").read_bytes(), b"user progress")
        for path, content in original.items():
            self.assertEqual(Path(path).read_bytes(), content)
        manifest = harness.verify_prepared(case, self.runtime)
        expected_hash = hashlib.sha256(b"new resources").hexdigest()
        self.assertEqual(manifest["game_files"]["Update3.pac"]["sha256"], expected_hash)

    def test_existing_case_is_never_overwritten_or_cleaned(self):
        case = self.runtime / "existing"
        case.mkdir(parents=True)
        sentinel = case / "keep.txt"
        sentinel.write_bytes(b"keep my work")
        with self.assertRaises((ValueError, FileExistsError)):
            self.prepare("existing")
        self.assertEqual(list(case.iterdir()), [sentinel])
        self.assertEqual(sentinel.read_bytes(), b"keep my work")

    def test_case_names_cannot_escape_or_alias_windows_paths(self):
        for name in ("../outside", "..\\outside", "C:\\outside", "nested/case", "CON", "NUL.txt", "case.", ""):
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.prepare(name)
        self.assertFalse(self.runtime.exists())

    def test_runtime_root_inside_input_tree_is_rejected_before_copy(self):
        with self.assertRaises(ValueError):
            self.prepare(runtime_root=self.base / "target")
        self.assertFalse((self.base / "target").exists())

    def test_global_crt_return_patch_is_rejected_before_any_output(self):
        with self.exe.open("r+b") as stream:
            stream.seek(0x2789F8)
            stream.write(b"\xc3\x90")
        with self.assertRaises(ValueError):
            self.prepare()
        self.assertFalse(self.runtime.exists())

    def test_preflight_detects_modified_game_bytes(self):
        case = self.prepare()
        (case / "game" / "Update3.pac").write_bytes(b"different resources")
        with self.assertRaises(ValueError):
            harness.verify_prepared(case, self.runtime)
        self.assertFalse((case / "launch.lock").exists())

    def test_preflight_detects_changed_original_save(self):
        case = self.prepare()
        (self.save / "Script.dat").write_bytes(b"new user progress")
        with self.assertRaises(ValueError):
            harness.verify_prepared(case, self.runtime)
        self.assertEqual((self.save / "Script.dat").read_bytes(), b"new user progress")

    def test_prepare_copies_complete_driver_tree_without_linking_or_modifying_source(self):
        case = self.prepare("driver-seed")
        copied = case / "driver-cache/Drs"
        self.assertTrue(copied.is_dir(), "The driver state directory was not copied")
        self.assertEqual((copied / "nvdrssel.bin").read_bytes(), b"\x01")
        self.assertEqual((copied / "nvAppTimestamps").read_bytes(), b"old")
        self.assertEqual((copied / "nested/db.bin").read_bytes(), b"database")
        manifest = harness.verify_prepared(case, self.runtime)
        self.assertEqual(manifest["driver_state"]["file_count"], 3)
        self.assertEqual(manifest["driver_state"]["total_bytes"], 12)
        self.assertEqual(sorted(manifest["driver_state"]["files"]), ["nested/db.bin", "nvAppTimestamps", "nvdrssel.bin"])
        (copied / "nested/db.bin").write_bytes(b"private change")
        self.assertEqual((self.driver / "nested/db.bin").read_bytes(), b"database")
        self.assertEqual((self.driver / "nvAppTimestamps").read_bytes(), b"old")

    def test_preflight_reports_original_driver_drift_without_changing_the_manifest(self):
        case = self.prepare("driver-drift-source")
        before_manifest = (case / "manifest.json").read_bytes()
        baseline = json.loads(before_manifest)["driver_state"]["files"]
        (self.driver / "nvAppTimestamps").write_bytes(b"external update")
        verified = harness.verify_prepared(case, self.runtime)
        audit = verified["driver_state_preflight"]
        self.assertEqual(audit["preparation_baseline"], baseline)
        self.assertEqual(audit["observed"], harness.tree_manifest(self.driver))
        self.assertEqual(audit["difference"]["changed"], ["nvAppTimestamps"])
        self.assertFalse(audit["difference"]["unchanged"])
        self.assertEqual(audit["attribution"], "not_determined_external_writers_possible")
        self.assertEqual((case / "manifest.json").read_bytes(), before_manifest)
        self.assertEqual(verified["driver_state"]["files"], baseline)
        self.assertEqual((case / "driver-cache/Drs/nvAppTimestamps").read_bytes(), b"old")

    def test_preflight_still_rejects_changed_private_driver_copy(self):
        case = self.prepare("driver-drift-copy")
        (case / "driver-cache/Drs/nvAppTimestamps").write_bytes(b"private changed")
        with self.assertRaisesRegex(ValueError, "Copied driver state tree"):
            harness.verify_prepared(case, self.runtime)

    def test_native_driver_state_is_rejected_before_creating_output(self):
        before = harness.tree_manifest(self.driver)
        with self.assertRaisesRegex(ValueError, "Native driver-state"):
            self.prepare("native-driver", native_driver_state=True)
        self.assertFalse(self.runtime.exists())
        self.assertEqual(harness.tree_manifest(self.driver), before)

    def test_private_driver_policy_redirects_drs_and_rejects_original_disk_writes(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        with self.exe.open("r+b") as stream:
            for offset, instruction in ((0x3643B9, "ff1560a27d00"), (0x3643CD, "ff1574a27d00")):
                stream.seek(offset)
                stream.write(bytes.fromhex(instruction))
        case = self.prepare("driver-policy", save_seed_dir=self.seed, app_manifest_path=self.app_manifest)
        manifest = harness.verify_prepared(case, self.runtime)
        with patch.object(harness, "native_path", side_effect=lambda value: str(value)):
            source = harness.isolation_script(case, manifest)
        policy = source.split("\nfunction cloneObjectAttributes", 1)[0]
        paths = [str(self.driver / "nvAppTimestamps"), str(self.driver / "new-cache.bin"),
                 str(self.save / "Script.dat"), str(self.base / "Config.pac"), str(self.exe),
                 str(self.pac), str(self.seed / "Script.dat"), str(self.app_manifest),
                 str(harness.PROJECT_ROOT / "unrelated.txt")]
        policy += "\nconst paths=" + json.dumps(paths) + ";"
        policy += "\nconsole.log(JSON.stringify({writes:paths.map(permitted),driverAlias:aliasDriverState(paths[0]),saveAlias:permitted(aliasSavePath(paths[2]))}));"
        result = subprocess.run([node, "-"], input=policy, text=True, capture_output=True, check=True)
        self.assertEqual(json.loads(result.stdout), {
            "writes": [False] * 9,
            "driverAlias": "\\??\\" + str(case / "driver-cache/Drs/nvAppTimestamps"), "saveAlias": True})

    def test_native_driver_cli_rejects_before_copying(self):
        args = ["prepare", "--case", "native-cli", "--base", str(self.base), "--exe", str(self.exe),
                "--save-dir", str(self.save), "--vanilla", "--fresh-save", "--native-driver-state"]
        prepare = harness.prepare_case
        with patch.object(harness, "prepare_case", side_effect=lambda **kwargs: prepare(**kwargs, runtime_root=self.runtime)):
            self.assertEqual(harness.main(args), 1)
        self.assertFalse((self.runtime / "native-cli").exists())

    def test_capture_log_is_a_new_empty_private_file_without_copying_the_original(self):
        with patch.object(harness.shutil, "copy2", wraps=shutil.copy2) as copied:
            case = self.prepare("capture-log")
        private = case / "driver-cache/ShadowPlay/CaptureCore.log"
        self.assertTrue(private.is_file(), "FILE_OPEN needs a pre-existing private CaptureCore.log")
        self.assertEqual(private.read_bytes(), b"")
        self.assertNotIn(r"C:\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log",
                         [str(call.args[0]) for call in copied.call_args_list])
        manifest = harness.verify_prepared(case, self.runtime)
        self.assertEqual(manifest["capture_log"], {
            "source": r"C:\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log",
            "relative_path": "driver-cache/ShadowPlay/CaptureCore.log"})

    def test_capture_log_preflight_rejects_missing_changed_or_redirected_private_file(self):
        for mutation in ("missing", "changed", "mapping"):
            with self.subTest(mutation=mutation):
                case = self.prepare("capture-preflight-" + mutation)
                private = case / "driver-cache/ShadowPlay/CaptureCore.log"
                if mutation == "missing":
                    private.unlink(missing_ok=True)
                elif mutation == "changed":
                    private.parent.mkdir(parents=True, exist_ok=True)
                    private.write_bytes(b"changed before launch")
                else:
                    manifest_path = case / "manifest.json"
                    manifest = json.loads(manifest_path.read_text())
                    manifest["capture_log"] = {"source": r"C:\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log",
                                               "relative_path": "../outside.log"}
                    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
                with self.assertRaises(ValueError):
                    harness.verify_prepared(case, self.runtime)

    def test_capture_log_alias_matches_only_the_exact_file_and_never_allows_the_original(self):
        node = shutil.which("node")
        self.assertIsNotNone(node, "Node is required for exact CaptureCore.log policy checks")
        original = r"C:\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log"
        device = r"\Device\HarddiskVolume1\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log"
        target = r"\??\D:\case\driver-cache\ShadowPlay\CaptureCore.log"
        config = {"allowed": [r"D:\case"], "captureLogSources": [original, device], "captureLogTarget": target}
        negatives = [original + suffix for suffix in ("Other", ".bak", ":stream", "\\child", "\\..\\Other.log", "\0")]
        negatives += [original.replace("ShadowPlay", "ShadowPlayOther"), original.replace("CaptureCore.log", "Other.log"),
                      original.replace("ShadowPlay", "Other"), original.replace("NVIDIA Corporation", "NVIDIA CorporationOther"),
                      r"C:\ProgramData\NVIDIA Corporation\ShadowPlay", r"C:\ProgramData\unrelated.log"]
        positives = [original, "\\??\\" + original, "\\\\?\\" + original, original.upper(), device]
        script = harness.isolation_policy(config) + "\nconst originals=" + json.dumps(positives + negatives) + ";"
        script += "\nif(typeof aliasCaptureLog!=='function'){console.log('missing');process.exit(0)}"
        script += "\nconsole.log(JSON.stringify({aliases: originals.map(aliasCaptureLog), allowed: originals.map(permitted), redirectedAllowed:permitted(aliasCaptureLog(originals[0]))}));"
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "Exact CaptureCore.log alias is missing")
        self.assertEqual(json.loads(result.stdout), {"aliases": [target] * len(positives) + [None] * len(negatives),
                                                   "allowed": [False] * (len(positives) + len(negatives)),
                                                   "redirectedAllowed": True})

    def test_capture_log_route_uses_cloned_attributes_and_records_its_own_event(self):
        node = shutil.which("node")
        self.assertIsNotNone(node, "Node is required for the CaptureCore.log route check")
        with self.exe.open("r+b") as stream:
            for offset, instruction in ((0x3643B9, "ff1560a27d00"), (0x3643CD, "ff1574a27d00")):
                stream.seek(offset)
                stream.write(bytes.fromhex(instruction))
        case = self.prepare("capture-route")
        manifest = harness.verify_prepared(case, self.runtime)
        with patch.object(harness, "native_path", side_effect=lambda value: str(value)):
            source = harness.isolation_script(case, manifest)
        policy = source.split("\nfunction cloneObjectAttributes", 1)[0]
        route = "function saveObjectRoute" + source.split("function saveObjectRoute", 1)[1].split("function deny", 1)[0]
        script = policy + r'''
const assert = require('node:assert/strict'), ps=4, captured=[];
function objectPath(attributes) { return attributes.path; }
let clones=0;
function cloneObjectAttributes(attributes, target, size) {
  clones++; assert.equal(size,4);
  return {pointer:{path:target},keepalive:[attributes,target]};
}
function announce(type,payload){captured.push({type,...payload});}
''' + route + "\nconst original={path:" + json.dumps("\\??\\C:\\ProgramData\\NVIDIA Corporation\\ShadowPlay\\CaptureCore.log") + "};"
        script += r'''
const before=JSON.stringify(original), mapped=saveObjectRoute(original);
assert.notEqual(mapped.attributes,original);
assert.equal(clones,1); assert.equal(JSON.stringify(original),before);
assert.equal(mapped.kind,'capture_log'); assert.equal(permitted(mapped.path),true);
assert.equal(reportSaveRoute('NtCreateFile',mapped,0),0);
assert.equal(captured[0].type,'capture_log_redirected');
assert.equal(captured[0].succeeded,true); assert.equal(captured[0].from,original.path);
console.log('ok');
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(result.stdout.strip(), "ok")

    def test_cryptnet_prepare_creates_only_empty_private_directories_and_pins_the_mapping(self):
        with patch.object(harness.shutil, "copytree", wraps=shutil.copytree) as copied:
            case = self.prepare("cryptnet-empty")
        root = case / "driver-cache/CryptnetUrlCache"
        self.assertTrue((root / "MetaData").is_dir())
        self.assertTrue((root / "Content").is_dir())
        self.assertEqual(list((root / "MetaData").iterdir()), [])
        self.assertEqual(list((root / "Content").iterdir()), [])
        original = r"C:\Users\30250\AppData\LocalLow\Microsoft\CryptnetUrlCache"
        self.assertNotIn(original, [str(call.args[0]) for call in copied.call_args_list])
        manifest = harness.verify_prepared(case, self.runtime)
        self.assertEqual(manifest["cryptnet_cache"], {"source": original,
            "relative_path": "driver-cache/CryptnetUrlCache",
            "metadata_file": "329B6147266C1E26CD774EA22B79EC2E"})
        self.assertIn(original, harness.protected_input_paths(manifest))

    def test_cryptnet_preflight_refuses_changed_mapping_missing_or_nonempty_cache(self):
        for mutation in ("mapping", "missing", "nonempty", "extra_directory"):
            with self.subTest(mutation=mutation):
                case = self.prepare("cryptnet-preflight-" + mutation)
                root = case / "driver-cache/CryptnetUrlCache"
                if mutation == "mapping":
                    path = case / "manifest.json"
                    manifest = json.loads(path.read_text())
                    manifest["cryptnet_cache"] = {"source": r"C:\other", "relative_path": "../other",
                                                 "metadata_file": "329B6147266C1E26CD774EA22B79EC2E"}
                    path.write_text(json.dumps(manifest), encoding="utf-8")
                elif mutation == "missing":
                    if (root / "Content").exists():
                        (root / "Content").rmdir()
                else:
                    (root / "MetaData").mkdir(parents=True, exist_ok=True)
                    if mutation == "nonempty":
                        (root / "MetaData/329B6147266C1E26CD774EA22B79EC2E").write_bytes(b"unexpected before launch")
                    else:
                        (root / "unexpected").mkdir()
                with self.assertRaises(ValueError):
                    harness.verify_prepared(case, self.runtime)

    def test_cryptnet_alias_is_only_the_observed_metadata_file_and_external_paths_remain_denied(self):
        node = shutil.which("node")
        self.assertIsNotNone(node)
        root = r"C:\Users\30250\AppData\LocalLow\Microsoft\CryptnetUrlCache"
        device = r"\Device\HarddiskVolume1\Users\30250\AppData\LocalLow\Microsoft\CryptnetUrlCache"
        leaf = "329B6147266C1E26CD774EA22B79EC2E"
        original = root + "\\MetaData\\" + leaf
        target_root = r"\??\D:\case\driver-cache\CryptnetUrlCache"
        config = {"allowed": [r"D:\case"], "protected": [root, device],
                  "cryptnetCacheSources": [root, device], "cryptnetCacheTarget": target_root,
                  "cryptnetMetadataFile": leaf}
        positives = [original, "\\??\\" + original, "\\\\?\\" + original, original.lower(), device + "\\MetaData\\" + leaf]
        negatives = [root, root + "\\MetaData", root + "\\Content", root + "\\Content\\" + leaf,
                     original + ":stream", original + "x", original + "\\child", original + "\\", original + "\0",
                     root + "\\MetaData\\" + "A" * 32, root + "\\MetaData\\" + "G" * 32,
                     root + "\\MetaData\\" + leaf[:-1], root + "\\Other\\" + leaf,
                     root + "\\MetaData\\nested\\" + leaf, root + "\\MetaData\\..\\MetaData\\" + leaf,
                     root + "\\.\\MetaData\\" + leaf, original.replace("CryptnetUrlCache", "CryptnetUrlCacheOther"),
                     original.replace("Microsoft", "MicrosoftOther"), original.replace("LocalLow", "Local")]
        script = harness.isolation_policy(config) + "\nconst paths=" + json.dumps(positives + negatives) + ";"
        script += "\nif(typeof aliasCryptnetCache!=='function'){console.log('missing');process.exit(0)}"
        script += "\nconsole.log(JSON.stringify({aliases:paths.map(aliasCryptnetCache), direct:paths.map(permitted), redirected:permitted(aliasCryptnetCache(paths[0]))}));"
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "The observed Cryptnet metadata alias is missing")
        self.assertEqual(json.loads(result.stdout), {
            "aliases": [target_root + "\\MetaData\\" + leaf] * len(positives) + [None] * len(negatives),
            "direct": [False] * (len(positives) + len(negatives)), "redirected": True})

    def test_cryptnet_file_route_clones_attributes_and_preserves_original_request(self):
        node = shutil.which("node")
        self.assertIsNotNone(node)
        with self.exe.open("r+b") as stream:
            for offset, instruction in ((0x3643B9, "ff1560a27d00"), (0x3643CD, "ff1574a27d00")):
                stream.seek(offset)
                stream.write(bytes.fromhex(instruction))
        case = self.prepare("cryptnet-route")
        manifest = harness.verify_prepared(case, self.runtime)
        with patch.object(harness, "native_path", side_effect=lambda value: str(value)):
            source = harness.isolation_script(case, manifest)
        policy = source.split("\nfunction cloneObjectAttributes", 1)[0]
        route = "function saveObjectRoute" + source.split("function saveObjectRoute", 1)[1].split("function deny", 1)[0]
        script = policy + r'''
const assert=require('node:assert/strict'),ps=4,events=[];
function objectPath(attributes){return attributes.path;}
let clones=0;
function cloneObjectAttributes(attributes,target,size){clones++;assert.equal(size,4);return {pointer:{path:target},keepalive:[attributes,target]};}
function announce(type,payload){events.push({type,...payload});}
''' + route + "\nconst original={path:" + json.dumps("\\??\\C:\\Users\\30250\\AppData\\LocalLow\\Microsoft\\CryptnetUrlCache\\MetaData\\329B6147266C1E26CD774EA22B79EC2E") + "};"
        script += r'''
const before=JSON.stringify(original),mapped=saveObjectRoute(original);
assert.notEqual(mapped.attributes,original);assert.equal(clones,1);
assert.equal(JSON.stringify(original),before);assert.equal(mapped.kind,'cryptnet_cache');
assert.equal(permitted(mapped.path),true);assert.equal(permitted(original.path),false);
assert.equal(reportSaveRoute('NtCreateFile',mapped,0),0);
assert.equal(events[0].type,'cryptnet_cache_redirected');assert.equal(events[0].succeeded,true);
console.log('ok');
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(result.stdout.strip(), "ok")

    def test_driver_drift_during_copy_is_rejected_with_evidence_retained(self):
        real_copytree = shutil.copytree

        def copy_and_change(source, destination, *args, **kwargs):
            result = real_copytree(source, destination, *args, **kwargs)
            if Path(source) == self.driver:
                (self.driver / "external-update.bin").write_bytes(b"external")
            return result

        with patch.object(harness.shutil, "copytree", side_effect=copy_and_change):
            with self.assertRaises(ValueError):
                self.prepare("driver-copy-drift")
        case = self.runtime / "driver-copy-drift"
        self.assertTrue((case / "driver-cache/Drs/nvdrssel.bin").exists())
        state = json.loads((case / "state.json").read_text())
        self.assertEqual(state["status"], "prepare_failed")
        self.assertEqual(state["driver_state_copy_check"]["source_check"]["added"], ["external-update.bin"])
        self.assertFalse(state["driver_state_copy_check"]["source_check"]["unchanged"])

    def test_runtime_driver_source_drift_is_reported_without_fault_attribution(self):
        self.assertIn("native_driver_state", inspect.signature(harness.prepare_case).parameters,
                      "Explicit native driver-state diagnostic mode is missing")
        self._check_runtime_driver_source_drift(native=False)

    def test_private_driver_exit_audit_survives_original_save_read_error(self):
        self._check_runtime_driver_source_drift(native=False, save_audit_error=True)

    def test_private_driver_exit_audit_preserves_original_save_difference_failure(self):
        self._check_runtime_driver_source_drift(native=False, change_save=True)

    def test_prelaunch_driver_drift_alone_is_recorded_and_does_not_fail_a_private_run(self):
        self._check_runtime_driver_source_drift(native=False, prelaunch_drift=True, runtime_drift=False)

    def test_prelaunch_and_runtime_driver_drift_are_separate_observations(self):
        self._check_runtime_driver_source_drift(native=False, prelaunch_drift=True)

    def test_prelaunch_driver_drift_never_masks_a_real_save_preservation_failure(self):
        self._check_runtime_driver_source_drift(native=False, prelaunch_drift=True, change_save=True)

    def _check_runtime_driver_source_drift(self, *, native, save_audit_error=False, change_save=False,
                                         prelaunch_drift=False, runtime_drift=True):
        (self.driver / "nvAppTimestamps").write_bytes(b"old")
        case = self.prepare("driver-runtime-drift-" + str(native), native_driver_state=native)
        manifest_bytes = (case / "manifest.json").read_bytes()
        preparation = json.loads(manifest_bytes)["driver_state"]["files"]
        if prelaunch_drift:
            (self.driver / "nvAppTimestamps").write_bytes(b"external update before run")
        run_before = harness.tree_manifest(self.driver)
        child = FakeOwnedProcess()
        child.poll = lambda: 0 if child.dead.is_set() else None

        def resume():
            if runtime_drift:
                changed_path = self.driver / ("nested/db.bin" if prelaunch_drift else "nvAppTimestamps")
                changed_path.write_bytes(b"external update during run")
            if change_save:
                (self.save / "Script.dat").write_bytes(b"unexpected user save change")
            child.dead.set()

        child.resume = resume

        def create_owned_process(*args, **kwargs):
            initial_state = json.loads((case / "state.json").read_text())
            self.assertEqual(initial_state.get("original_driver_state_before"), run_before,
                             "The actual run-before snapshot must be saved before process creation")
            return child

        class ReadySession:
            def __init__(self):
                self.callbacks = {}

            def on(self, name, callback):
                self.callbacks[name] = callback

            def create_script(self, source, name):
                return self

            def load(self):
                for payload in ({"type": "hook_ready"}, {"type": "save_path_redirected", "succeeded": True}):
                    self.callbacks["message"]({"type": "send", "payload": payload}, None)

            def detach(self):
                pass

        original_open = Path.open

        def open_with_save_audit_failure(path, *args, **kwargs):
            if save_audit_error and child.dead.is_set() and path.is_relative_to(self.save):
                raise OSError("original Save read failure")
            return original_open(path, *args, **kwargs)

        with patch.object(harness, "OwnedProcess", side_effect=create_owned_process), \
                patch.object(harness, "isolation_script", return_value="offline"), \
                patch.object(Path, "open", autospec=True, side_effect=open_with_save_audit_failure), \
                patch.dict(sys.modules, {"frida": types.SimpleNamespace(attach=lambda pid: ReadySession())}):
            result = harness.launch_case(case, runtime_root=self.runtime)
        state = json.loads((case / "state.json").read_text())
        if save_audit_error or change_save:
            self.assertEqual(result, 1)
            self.assertEqual(state["status"], "failed")
            if save_audit_error:
                self.assertIn("original Save read failure", state["failure"])
            else:
                self.assertEqual(state["failure"], "original_save_changed")
        else:
            self.assertEqual(result, 0)
            self.assertEqual(state["status"], "exited")
            self.assertIsNone(state["failure"])
        self.assertIn("original_driver_state_check", state,
                      "Save audit failure must not skip the independent driver audit")
        expected_all = (["nested/db.bin", "nvAppTimestamps"] if prelaunch_drift and runtime_drift else ["nvAppTimestamps"])
        expected_runtime = (["nested/db.bin"] if prelaunch_drift else ["nvAppTimestamps"]) if runtime_drift else []
        self.assertEqual(state["original_driver_state_preparation_baseline"], preparation)
        self.assertEqual(state["original_driver_state_before"], run_before)
        self.assertEqual(state["original_driver_state_prelaunch_check"]["changed"], ["nvAppTimestamps"] if prelaunch_drift else [])
        self.assertEqual(state["original_driver_state_check"]["changed"], expected_all)
        self.assertEqual(state["original_driver_state_runtime_check"]["changed"], expected_runtime)
        self.assertEqual(state["original_driver_state_runtime_check"]["unchanged"], not runtime_drift)
        self.assertEqual((case / "manifest.json").read_bytes(), manifest_bytes,
                         "Driver drift audit must not rewrite the preparation manifest")
        self.assertEqual(state["original_driver_state_check"]["attribution"], "not_determined_external_writers_possible")
        if native:
            self.assertFalse((case / "driver-cache").exists())
            self.assertNotIn("driver_state_copy_after", state)
            self.assertEqual(state["original_driver_state_before"]["nvAppTimestamps"]["sha256"],
                             hashlib.sha256(b"old").hexdigest())
            self.assertEqual(state["original_driver_state_after"]["nvAppTimestamps"]["sha256"],
                             hashlib.sha256(b"external update during run").hexdigest())
        else:
            self.assertEqual((case / "driver-cache/Drs/nvAppTimestamps").read_bytes(), b"old")
        if not save_audit_error:
            self.assertEqual(state["original_save_check"]["unchanged"], not change_save)

    def test_preflight_rejects_case_outside_expected_root(self):
        case = self.prepare()
        with self.assertRaises(ValueError):
            harness.verify_prepared(case, self.root / "another-runtime")

    def test_unselected_base_overlay_cannot_silently_override_new_package(self):
        (self.base / "Update7.pac").write_bytes(b"stale override")
        with self.assertRaises(ValueError):
            self.prepare()
        self.assertFalse(self.runtime.exists())

    def test_preflight_detects_added_and_removed_save_files(self):
        case = self.prepare()
        (self.save / "Config.dat").unlink()
        (self.save / "unexpected.dat").write_bytes(b"new")
        result = harness.compare_manifest(self.save, json.loads((case / "manifest.json").read_text())["original_save_files"])
        self.assertEqual(result["removed"], ["Config.dat"])
        self.assertEqual(result["added"], ["unexpected.dat"])
        self.assertFalse(result["unchanged"])

    def test_symlink_input_is_rejected_instead_of_following_external_data(self):
        link = self.base / "external.pac"
        try:
            link.symlink_to(self.pac)
        except OSError:
            self.skipTest("Symlinks are unavailable for this account")
        with self.assertRaises(ValueError):
            self.prepare()

    def test_case_write_exception_does_not_allow_repo_neighbors_or_path_escape(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable for offline JavaScript execution")
        if not hasattr(harness, "isolation_policy"):
            self.fail("Executable isolated path policy is missing")
        script = harness.isolation_policy({
            "allowed": [r"D:\repo\target\case", r"\Device\HarddiskVolume4\repo\target\case"],
            "repository": [r"D:\repo", r"\Device\HarddiskVolume4\repo"],
            "protected": [r"C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save"]})
        script += "\nconst paths = " + json.dumps([
            r"D:\repo\target\case\docs\Save.dat", r"D:\repo\target\case-other\Save.dat",
            r"D:\repo\target\case\..\..\original\Save.dat",
            r"C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save\Config.dat",
            r"\??\D:\repo\target\case\docs\Save.dat", r"\Device\HarddiskVolume4\repo\target\case\game\Config.dat",
            r"D:\repo\target\case\sub\..\docs\Config.dat", r"\\?\D:\repo\target\case\docs\Save.dat",
            r"D:\repo\src\code.java", r"\Device\HarddiskVolume4\repo\README.md",
            r"D:\repo\target\case:outside-stream",
        ]) + ";\nconsole.log(JSON.stringify(paths.map(permitted)));"
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertEqual(json.loads(result.stdout), [True, False, False, False, True, True, True, True, False, False, False])

    def test_external_os_disk_writes_are_rejected_with_original_inputs_protected(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable for offline JavaScript execution")
        config = {"allowed": [r"D:\case"],
                  "repository": [r"D:\repo"],
                  "protected": [r"C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save", r"E:\base",
                                r"C:\ProgramData\NVIDIA Corporation\Drs", r"E:\inputs\Game.exe", r"E:\inputs\Update3.pac",
                                r"\Device\HarddiskVolume8\inputs\Game.exe"]}
        script = harness.isolation_policy(config)
        script += "\nconsole.log(JSON.stringify(" + json.dumps([
            r"C:\Users\30250\AppData\Local\Microsoft\Windows\Caches\observed.db",
            r"C:\Users\30250\AppData\Local\Microsoft\Windows\Caches-other\unexpected.db",
            r"C:\Users\30250\AppData\Local\Microsoft\Windows\Caches\..\unexpected.db",
            r"C:\Users\30250\AppData\Local\Temp\unexpected.db",
            r"C:\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log",
            r"\Device\HarddiskVolume1\ProgramData\NVIDIA Corporation\ShadowPlay\CaptureCore.log",
            r"C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save\Config.dat",
            r"E:\base\Update3.pac",
            r"C:\ProgramData\NVIDIA Corporation\Drs\nvdrsdb0.bin",
            r"E:\inputs\Game.exe", r"E:\inputs\Update3.pac",
            r"\Device\HarddiskVolume8\inputs\Game.exe",
            r"E:\inputs\Game.exe:stream",
        ]) + ".map(permitted)));"
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertEqual(json.loads(result.stdout), [False] * 13)

    def test_unresolved_write_targets_still_fail_closed_in_preservation_mode(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        script = harness.isolation_policy({"allowed": [r"D:\case"], "repository": [r"D:\repo"]})
        script += "\nconst paths=" + json.dumps([
            "", None, "relative.log", r"C:relative.log", r"\Device\UnknownDevice\data",
            "C:\\logs\\bad" + chr(0) + "name", r"D:\case\..\..\relative.log",
        ]) + ";\nconsole.log(JSON.stringify(paths.map(p=>{try{return permitted(p);}catch(e){return 'threw';}})));"
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertEqual(json.loads(result.stdout), [False] * 7)

    def test_existing_directory_metadata_open_does_not_allow_creation_or_data_writes(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable for offline JavaScript execution")
        script = harness.isolation_policy({"allowed": []})
        script += r'''
if (typeof metadataOpenOnly === 'undefined') { console.log('missing'); process.exit(0); }
console.log(JSON.stringify([
  metadataOpenOnly(0x02000000, 1, 1, true),
  metadataOpenOnly(0x00100180, 1, 1, true),
  metadataOpenOnly(0x02000000, 1, 1, false),
  metadataOpenOnly(0x02000000, 1, 3, true),
  metadataOpenOnly(0x02000000, 0x1001, 1, true),
  metadataOpenOnly(0x40000000, 1, 1, true),
  metadataOpenOnly(2, 1, 1, true),
  metadataOpenOnly(0x10000, 1, 1, true)
]));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "Metadata-only open classification is missing")
        self.assertEqual(json.loads(result.stdout), [True, True, False, False, False, False, False, False])

    def test_documents_pidl_is_consumed_only_after_successful_outer_game_redirection(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable for offline JavaScript execution")
        script = harness.isolation_policy({"allowed": [], "documents": r"C:\Users\30250\Documents",
                                           "redirect": r"D:\case\docs"})
        script += r'''
if (typeof redirectDocuments === 'undefined') { console.log('missing'); process.exit(0); }
const pidls = new Set(['0x1234']);
const writes = [];
const read = () => 'C:\\Users\\30250\\Documents';
const outcome = [];
outcome.push(redirectDocuments(pidls, '0x1234', false, true, read, x => writes.push(x)));
outcome.push(pidls.has('0x1234'));
outcome.push(redirectDocuments(pidls, '0x1234', true, false, read, x => writes.push(x)));
outcome.push(pidls.has('0x1234'));
try { redirectDocuments(pidls, '0x1234', true, true, read, () => { throw Error('write failed'); }); }
catch(e) { outcome.push(pidls.has('0x1234')); }
outcome.push(redirectDocuments(pidls, '0x1234', true, true, read, x => writes.push(x)));
outcome.push(pidls.has('0x1234'));
console.log(JSON.stringify({outcome, writes}));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "Safe PIDL redirection is missing")
        self.assertEqual(json.loads(result.stdout), {"outcome": [False, True, False, True, True, True, False],
                                                     "writes": [r"D:\case\docs"]})

    def test_directory_create_collision_and_exact_save_alias_policy(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        config = {"allowed": [r"D:\case"], "saveAliasSources": [r"C:\Documents\GIGA\BALDRSKY_DIVEX\Save",
                  r"\Device\HarddiskVolume1\Documents\GIGA\BALDRSKY_DIVEX\Save"],
                  "saveAliasTarget": r"\??\D:\case\docs\GIGA\BALDRSKY_DIVEX\Save"}
        script = harness.isolation_policy(config) + r'''
if (typeof aliasSavePath === 'undefined' || typeof directoryCreateCollision === 'undefined') {
  console.log('missing'); process.exit(0);
}
console.log(JSON.stringify({
  collision: [[0x204021,2,true],[0x204021,2,false],[0x204021,3,true],[0x204020,2,true],[0x204061,2,true]]
    .map(x => directoryCreateCollision(...x)),
  paths: [
    '\\??\\C:\\Documents\\GIGA\\BALDRSKY_DIVEX\\Save\\ConfigOK.dat',
    '\\Device\\HarddiskVolume1\\Documents\\GIGA\\BALDRSKY_DIVEX\\Save',
    'C:\\Documents\\GIGA\\BALDRSKY_DIVEX\\SaveOther\\Config.dat',
    'C:\\Documents\\GIGA\\BALDRSKY_DIVEX\\Save\\..\\Other.dat',
    'C:\\Documents\\GIGA\\BALDRSKY_DIVEX\\Save\\Sub\\..\\NewSave.dat'
  ].map(aliasSavePath)
}));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "Save fallback policy is missing")
        self.assertEqual(json.loads(result.stdout), {
            "collision": [True, False, False, False, False],
            "paths": [r"\??\D:\case\docs\GIGA\BALDRSKY_DIVEX\Save\ConfigOK.dat",
                      r"\??\D:\case\docs\GIGA\BALDRSKY_DIVEX\Save", None, None,
                      r"\??\D:\case\docs\GIGA\BALDRSKY_DIVEX\Save\NewSave.dat"]})

    def test_observed_hid_namespace_allows_open_only_and_never_disk_paths(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        script = harness.isolation_policy({"allowed": []}) + r'''
if (typeof hidDeviceOpen === 'undefined') { console.log('missing'); process.exit(0); }
const hid = '\\??\\hid#vid_0b05&pid_19b6&col06#7&22cddfa1&0&0005#{4d1e55b2-f16f-11cf-88cb-001111000030}';
const asus = '\\??\\hid#asuf1204&col04#4&1356c909&0&0003#{4d1e55b2-f16f-11cf-88cb-001111000030}';
console.log(JSON.stringify([
  hidDeviceOpen(hid, 1, 0x60),
  hidDeviceOpen(asus, 1, 0x60),
  hidDeviceOpen(hid.toUpperCase() + '\\KBD', 1, 0x60),
  hidDeviceOpen(hid, 2, 0x60),
  hidDeviceOpen(hid, 3, 0x60),
  hidDeviceOpen(hid, 4, 0x60),
  hidDeviceOpen(hid, 5, 0x60),
  hidDeviceOpen(hid, 1, 0x1060),
  hidDeviceOpen(hid, 1, 0x61),
  hidDeviceOpen('\\Device\\HarddiskVolume1\\Save\\Config.dat', 1, 0x60),
  hidDeviceOpen('\\Device\\HidUSB0', 1, 0x60),
  hidDeviceOpen('\\??\\C:\\original\\' + hid.slice(4), 1, 0x60),
  hidDeviceOpen(hid + '\\kbd\\Config.dat', 1, 0x60),
  hidDeviceOpen(hid.replace('4d1e55b2', '00000000'), 1, 0x60),
  hidDeviceOpen(hid + '\\..\\Save\\Config.dat', 1, 0x60)
]));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "HID open-only classification is missing")
        self.assertEqual(json.loads(result.stdout), [True, True, True] + [False] * 12)

    def test_mmcss_thread_device_allows_only_exact_read_only_observed_opens(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        script = harness.isolation_policy({"allowed": []}) + r'''
if (typeof mmcssDeviceOpen === 'undefined') { console.log('missing'); process.exit(0); }
const device = '\\Device\\MMCSS\\MmThread';
console.log(JSON.stringify({
  allowed: [
    mmcssDeviceOpen(device, 0x120089, 0, 0),
    mmcssDeviceOpen(device, 0x120089, 1, 0),
    mmcssDeviceOpen(device.toLowerCase(), 0x120089, 0, 0),
    mmcssDeviceOpen(device, 0x80, 1, 0x40)
  ],
  paths: [null, '', device + 'Other', device + '\\child', device + '\\', device + '\n',
    '\\Device\\MMCSS', '\\Device\\Other\\MmThread',
    '\\Device\\HarddiskVolume1\\MMCSS\\MmThread',
    '\\??\\C:\\Device\\MMCSS\\MmThread', device + '\\..\\Save\\Config.dat']
    .map(path => mmcssDeviceOpen(path, 0x120089, 0, 0)),
  access: [0x2, 0x4, 0x10, 0x20, 0x40, 0x100, 0x10000, 0x40000, 0x80000,
    0x1000000, 0x2000000, 0x10000000, 0x20000000, 0x40000000, 0x80000000]
    .map(right => mmcssDeviceOpen(device, 0x120089 | right, 0, 0)),
  dispositions: [-1, 2, 3, 4, 5, 6].map(value => mmcssDeviceOpen(device, 0x120089, value, 0)),
  options: [0x1000, 1, 0x1001].map(value => mmcssDeviceOpen(device, 0x120089, 0, value))
}));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "MMCSS read-only device classification is missing")
        self.assertEqual(json.loads(result.stdout), {
            "allowed": [True] * 4, "paths": [False] * 11, "access": [False] * 15,
            "dispositions": [False] * 6, "options": [False] * 3})

    def test_driver_tree_alias_has_directory_boundary_not_programdata_or_other_nvidia_paths(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        config = {"allowed": [r"D:\case"],
                  "driverStateSources": [r"C:\ProgramData\NVIDIA Corporation\Drs",
                    r"\Device\HarddiskVolume1\ProgramData\NVIDIA Corporation\Drs"],
                  "driverStateTarget": r"\??\D:\case\driver-cache\Drs"}
        script = harness.isolation_policy(config) + r'''
if (typeof aliasDriverState === 'undefined') { console.log('missing'); process.exit(0); }
console.log(JSON.stringify([
  '\\??\\C:\\ProgramData\\NVIDIA Corporation\\Drs\\nvdrssel.bin',
  '\\Device\\HarddiskVolume1\\ProgramData\\NVIDIA Corporation\\Drs\\nvAppTimestamps',
  'C:\\PROGRAMDATA\\NVIDIA CORPORATION\\DRS\\NewFile.bin',
  'C:\\ProgramData\\NVIDIA Corporation\\Drs\\nested\\db.bin',
  'C:\\ProgramData\\NVIDIA Corporation\\Drs\\nvdrsdb0.bin',
  'C:\\ProgramData\\NVIDIA Corporation\\Drs',
  'C:\\ProgramData\\NVIDIA Corporation\\DrsOther\\data.bin',
  'C:\\ProgramData\\NVIDIA Corporation\\Drs\\..\\Other\\data.bin',
  'C:\\ProgramData\\other.bin'
].map(aliasDriverState)));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        self.assertNotEqual(result.stdout.strip(), "missing", "Driver directory alias is missing")
        root = "\\??\\D:\\case\\driver-cache\\Drs"
        self.assertEqual(json.loads(result.stdout), [root + suffix for suffix in (
            "\\nvdrssel.bin", "\\nvAppTimestamps", "\\NewFile.bin", "\\nested\\db.bin", "\\nvdrsdb0.bin", "")] + [None] * 3)

    def test_exception_observer_records_module_context_without_symbol_or_stack_resolution(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        if not hasattr(harness, "exception_observer"):
            self.fail("Read-only exception evidence helper is missing")
        script = r'''
function ptr(value) { return {value, toString: () => '0x' + value.toString(16), sub: other => ptr(value-other.value)}; }
const context = {pc: ptr(0x1010)};
const Process = {findModuleByAddress: address => ({name:'module.dll', path:'C:\\Windows\\module.dll', base:ptr(0x1000)})};
let symbolCalls=0, backtraceCalls=0;
const DebugSymbol = {fromAddress: address => {symbolCalls++; throw Error('symbol resolution may perform file I/O');}};
const Backtracer = {ACCURATE: 1};
const Thread = {backtrace: (givenContext, mode) => {
  backtraceCalls++; throw Error('unwinding may load symbols');
}};
const captured=[];
function announce(type,payload) { captured.push({type,...payload}); }
'''
        script += harness.exception_observer() + r'''
const details={type:'system',address:ptr(0x1010),memory:{operation:'read',address:ptr(0x5000)},context};
const before=JSON.stringify(details);
const handled=observeException(details);
console.log(JSON.stringify({handled,event:captured[0],symbolCalls,backtraceCalls,unchanged:before===JSON.stringify(details)}));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        data = json.loads(result.stdout)
        self.assertFalse(data["handled"])
        self.assertEqual(data["event"]["classification"], "unclassified_observed_exception")
        self.assertEqual(data["event"]["location"]["module"]["name"], "module.dll")
        self.assertEqual(data["event"]["location"]["module"]["offset"], "0x10")
        self.assertEqual(data["symbolCalls"], 0)
        self.assertEqual(data["backtraceCalls"], 0)
        self.assertTrue(data["unchanged"])
        self.assertEqual(data["event"]["context"]["pc"]["value"], 0x1010)
        self.assertEqual(data["event"]["memory"]["operation"], "read")
        self.assertEqual(data["event"]["backtrace"], {"status": "unavailable", "reason": "no_symbol_resolution"})

    def test_exception_diagnostic_and_logging_failures_still_return_false(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        if not hasattr(harness, "exception_observer"):
            self.fail("Read-only exception evidence helper is missing")
        script = r'''
const Process={findModuleByAddress:()=>{throw Error('module lookup unavailable');}};
let symbolCalls=0,backtraceCalls=0;
const DebugSymbol={fromAddress:()=>{symbolCalls++;throw Error('symbol lookup forbidden');}};
const Thread={backtrace:()=>{backtraceCalls++;throw Error('unwind forbidden');}};
const Backtracer={ACCURATE:1};
const captured=[];
let failLog=false;
function announce(type,payload){if(failLog)throw Error('logger unavailable');captured.push({type,...payload});}
'''
        script += harness.exception_observer() + r'''
const details={type:'system',address:{toString:()=> '0x1234'},context:{},memory:null};
const first=observeException(details);
failLog=true;
const second=observeException(details);
console.log(JSON.stringify({first,second,errors:captured[0].diagnostic_errors,classification:captured[0].classification,symbolCalls,backtraceCalls}));
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True, check=True)
        data = json.loads(result.stdout)
        self.assertFalse(data["first"])
        self.assertFalse(data["second"])
        self.assertEqual(len(data["errors"]), 1)
        self.assertEqual(data["symbolCalls"], 0)
        self.assertEqual(data["backtraceCalls"], 0)
        self.assertEqual(data["classification"], "unclassified_observed_exception")

    def test_save_alias_clones_native_structures_and_preserves_original_buffers(self):
        node = shutil.which("node")
        if not node:
            self.skipTest("Node is unavailable")
        if not hasattr(harness, "isolation_structures"):
            self.fail("Native Save alias structure implementation is missing")
        # Only native memory allocation is substituted. The production cloning
        # functions execute unchanged over actual byte buffers for x86 and x64.
        script = r'''
let pointerSize = 4;
const heap = Buffer.alloc(65536);
let next = 256;
class Pointer {
  constructor(address) { this.address = Number(address); }
  add(offset) { return new Pointer(this.address + offset); }
  isNull() { return this.address === 0; }
  readU16() { return heap.readUInt16LE(this.address); }
  readU32() { return heap.readUInt32LE(this.address); }
  writeU16(v) { heap.writeUInt16LE(v, this.address); return this; }
  writeU32(v) { heap.writeUInt32LE(v >>> 0, this.address); return this; }
  readPointer() { return new Pointer(pointerSize === 4 ? this.readU32() : Number(heap.readBigUInt64LE(this.address))); }
  writePointer(v) {
    if (pointerSize === 4) this.writeU32(v.address);
    else heap.writeBigUInt64LE(BigInt(v.address), this.address);
    return this;
  }
  readByteArray(n) { return Uint8Array.from(heap.subarray(this.address, this.address + n)); }
  writeByteArray(value) { Buffer.from(value).copy(heap, this.address); return this; }
  readUtf16String(n) { return heap.toString('utf16le', this.address, this.address + n * 2); }
}
function ptr(value) { return new Pointer(value); }
const Memory = {
  alloc(size) { const out = ptr(next); next += (size + 15) & ~15; return out; },
  allocUtf16String(value) { const b = Buffer.from(value + '\0', 'utf16le'); const out = this.alloc(b.length); out.writeByteArray(b); return out; },
  copy(to, from, n) { heap.copy(heap, to.address, from.address, from.address + n); }
};
'''
        script += harness.isolation_structures() + r'''
const assert = require('node:assert/strict');
for (const ps of [4,8]) {
  pointerSize = ps;
  const size = ps === 4 ? 24 : 48;
  const rootOffset = ps === 4 ? 4 : 8;
  const nameOffset = ps === 4 ? 8 : 16;
  const attr = Memory.alloc(size);
  for (let i = 0; i < size; i++) heap[attr.address + i] = i;
  attr.writeU32(size);
  attr.add(rootOffset).writePointer(ptr(0x1234));
  const before = Buffer.from(attr.readByteArray(size));
  const target = '\\??\\D:\\case\\docs\\Save\\New.dat';
  const copy = cloneObjectAttributes(attr, target, ps);
  assert.deepEqual(Buffer.from(attr.readByteArray(size)), before);
  assert.equal(copy.pointer.add(rootOffset).readPointer().address, 0);
  const us = copy.pointer.add(nameOffset).readPointer();
  assert.equal(us.readU16(), target.length * 2);
  assert.equal(us.add(2).readU16(), target.length * 2 + 2);
  const text = us.add(ps === 4 ? 4 : 8).readPointer();
  assert.equal(text.readUtf16String(target.length), target);
  const copied = Buffer.from(copy.pointer.readByteArray(size));
  copied.set(before.subarray(rootOffset, rootOffset + ps), rootOffset);
  copied.set(before.subarray(nameOffset, nameOffset + ps), nameOffset);
  assert.deepEqual(copied, before);
  const nameStart = ps * 2 + 4;
  const rename = Memory.alloc(nameStart + 12);
  rename.writeU32(0x1357);
  rename.add(ps).writePointer(ptr(0x1234));
  rename.add(ps * 2).writeU32(12);
  const renameBefore = Buffer.from(rename.readByteArray(nameStart + 12));
  const changed = cloneRenameInformation(rename, nameStart + 12, target, ps);
  assert.deepEqual(Buffer.from(rename.readByteArray(nameStart + 12)), renameBefore);
  assert.equal(changed.pointer.readU32(), 0x1357);
  assert.equal(changed.pointer.add(ps).readPointer().address, 0);
  assert.equal(changed.pointer.add(ps * 2).readU32(), target.length * 2);
  assert.equal(changed.pointer.add(nameStart).readUtf16String(target.length), target);
  assert.equal(changed.length, nameStart + target.length * 2);
  const io = Memory.alloc(ps * 2);
  const handle = Memory.alloc(ps);
  handle.writePointer(ptr(0x9876));
  assert.equal(completeDirectoryCollision(handle, io, ps), -1073741771);
  assert.equal(handle.readPointer().address, 0);
  assert.equal(io.readU32(), 0xc0000035);
  assert.equal(io.add(ps).readPointer().address, 4);
}
console.log('native structures preserved');
'''
        result = subprocess.run([node, "-"], input=script, text=True, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), "native structures preserved")

    def test_late_installer_keeps_guard_until_failed_termination_eventually_exits(self):
        case = self.prepare("late-installer")
        child = FakeOwnedProcess()
        late_load = threading.Event()
        loaded = threading.Event()
        session = FakeSession(child, late_load, loaded)
        results = []

        def run():
            results.append(harness.launch_case(case, runtime_root=self.runtime, ready_timeout=0.05))

        with patch.object(harness, "OwnedProcess", return_value=child), \
                patch.object(harness, "isolation_script", return_value="offline"), \
                patch.dict(sys.modules, {"frida": types.SimpleNamespace(attach=lambda pid: session)}):
            runner = threading.Thread(target=run)
            runner.start()
            try:
                self.assertTrue(child.termination_attempted.wait(3), "Cleanup never attempted termination")
                late_load.set()
                self.assertTrue(loaded.wait(3), "Late load never completed")
                runner.join(0.3)
                self.assertEqual(session.detached_while_alive, 0, "Installer removed the live guard")
                self.assertTrue(runner.is_alive(), "Launcher returned while its process was still alive")
                self.assertEqual(child.closed, [])
                state = json.loads((case / "state.json").read_text())
                self.assertEqual(state["status"], "failed")
                self.assertTrue(state["cleanup_pending"])
            finally:
                child.dead.set()
                late_load.set()
                runner.join(5)
            self.assertFalse(runner.is_alive())
        self.assertEqual(results, [1])
        self.assertEqual(session.detached_while_alive, 0)
        self.assertEqual(session.detached_after_exit, 1)
        self.assertEqual(child.closed, ["thread_handle", "process_handle"])

    def test_termination_does_not_depend_on_log_write_succeeding(self):
        case = self.prepare("log-failure")
        child = FakeOwnedProcess(terminate_succeeds=True)
        original_open = Path.open

        def open_with_failure(path, *args, **kwargs):
            stream = original_open(path, *args, **kwargs)
            if path.name == "events.jsonl":
                return FailingCleanupLog(stream)
            return stream

        def attach_failure(pid):
            raise RuntimeError("offline install failure")

        with patch.object(harness, "OwnedProcess", return_value=child), \
                patch.dict(sys.modules, {"frida": types.SimpleNamespace(attach=attach_failure)}), \
                patch.object(Path, "open", open_with_failure):
            result = harness.launch_case(case, runtime_root=self.runtime, ready_timeout=0.05)
        self.assertTrue(child.termination_attempted.is_set(), "Log failure skipped child termination")
        self.assertTrue(child.dead.is_set())
        self.assertEqual(child.closed, ["thread_handle", "process_handle"])
        self.assertEqual(result, 1)

    def test_identity_failure_retains_handles_when_termination_fails_or_times_out(self):
        for terminate_result in (False, True):
            with self.subTest(terminate_result=terminate_result):
                kernel = FakeKernel(terminate_result)
                process = harness.OwnedProcess.__new__(harness.OwnedProcess)
                with patch.object(harness.ctypes, "WinDLL", return_value=kernel), \
                        patch.object(harness.OwnedProcess, "current_identity", side_effect=OSError("identity unavailable")):
                    with self.assertRaises(Exception) as raised:
                        process.__init__(self.exe, self.base)
                self.assertEqual(kernel.closed, [], "Unconfirmed process handles were closed")
                self.assertIs(getattr(raised.exception, "process", None), process,
                              "The caller lost ownership of the newly created suspended process")
                with self.assertRaises((OSError, RuntimeError)):
                    process.terminate()
                self.assertEqual(kernel.closed, [])
                self.assertEqual(process.process_handle, 0x111)
                self.assertEqual(process.thread_handle, 0x222)

    def test_identity_failure_is_closed_by_launch_owner_after_confirmed_exit(self):
        case = self.prepare("identity-cleanup")
        kernel = FakeKernel(True, exit_after_termination=True)
        with patch.object(harness.ctypes, "WinDLL", return_value=kernel), \
                patch.object(harness.OwnedProcess, "current_identity", side_effect=OSError("identity unavailable")), \
                patch.dict(sys.modules, {"frida": types.SimpleNamespace()}):
            result = harness.launch_case(case, runtime_root=self.runtime)
        state = json.loads((case / "state.json").read_text())
        self.assertEqual(result, 1)
        self.assertFalse(state["process"]["identity_verified"])
        self.assertEqual(state["exit_code"], 1)
        self.assertEqual(kernel.closed, [0x222, 0x111])
        self.assertEqual(state["closed_handles"], ["thread_handle", "process_handle"])
        self.assertTrue(state["original_save_check"]["unchanged"])


    def cleanup_state(self, case):
        return {"launcher_pid": os.getpid(), "owned_process_exit_confirmed": True,
                "guard_cleanup_complete": True, "cleanup_pending": False,
                "manifest_sha256": harness.file_record(case / "manifest.json")["sha256"]}

    def test_other_runtime_root_is_rejected_before_creating_anything(self):
        outside = self.root / "other-target"
        with self.assertRaisesRegex(ValueError, "repository target"):
            self.prepare(runtime_root=outside)
        self.assertFalse(outside.exists())
        self.assertFalse(self.runtime.exists())

    def test_case_temp_environment_is_local_and_restored(self):
        case = self.prepare()
        keys = ("TEMP", "TMP", "TMPDIR", "PYTHONDONTWRITEBYTECODE")
        before = {key: os.environ.get(key) for key in keys}
        previous_temp = tempfile.tempdir
        with harness.case_temp_environment(case) as local:
            self.assertEqual(local, case / "tmp")
            self.assertEqual(Path(tempfile.gettempdir()), local)
            for key in ("TEMP", "TMP", "TMPDIR"):
                self.assertEqual(os.environ[key], str(local))
            self.assertEqual(os.environ["PYTHONDONTWRITEBYTECODE"], "1")
        self.assertEqual(before, {key: os.environ.get(key) for key in keys})
        self.assertEqual(tempfile.tempdir, previous_temp)

    def test_finished_cleanup_removes_only_reproducible_input_copies(self):
        case = self.prepare()
        manifest = harness.verify_prepared(case, self.runtime)
        (case / "game/Config.pac").write_bytes(b"changed unique evidence")
        added = case / "game/new-result.txt"
        added.write_text("new diagnostic", encoding="utf-8")
        before_save = harness.tree_manifest(case / "docs")
        original_base = harness.tree_manifest(self.base)
        result = harness.remove_verified_input_copies(case, manifest, self.cleanup_state(case), self.runtime)
        self.assertIn("Update3.pac", result["removed"])
        self.assertIn("Config.pac", result["retained_changed_or_unique"])
        self.assertFalse((case / "game/BaldrSky.exe").exists())
        self.assertEqual((case / "game/Config.pac").read_bytes(), b"changed unique evidence")
        self.assertEqual(added.read_text(encoding="utf-8"), "new diagnostic")
        self.assertEqual(before_save, harness.tree_manifest(case / "docs"))
        self.assertEqual(original_base, harness.tree_manifest(self.base))
        self.assertEqual(result["errors"], [])

    def test_cleanup_keeps_a_copy_when_its_original_was_removed(self):
        case = self.prepare()
        manifest = harness.verify_prepared(case, self.runtime)
        self.pac.unlink()
        result = harness.remove_verified_input_copies(case, manifest, self.cleanup_state(case), self.runtime)
        self.assertTrue((case / "game/Update3.pac").is_file())
        self.assertIn("Update3.pac", result["retained_changed_or_unique"])

    def test_cleanup_refuses_unconfirmed_or_foreign_launcher(self):
        case = self.prepare()
        manifest = harness.verify_prepared(case, self.runtime)
        before = harness.tree_manifest(case / "game")
        for field, value in (("owned_process_exit_confirmed", False),
                             ("guard_cleanup_complete", False), ("cleanup_pending", True),
                             ("launcher_pid", os.getpid() + 1)):
            state = self.cleanup_state(case)
            state[field] = value
            with self.assertRaises(ValueError):
                harness.remove_verified_input_copies(case, manifest, state, self.runtime)
            self.assertEqual(before, harness.tree_manifest(case / "game"))

    def test_cleanup_rejects_escape_ads_drive_relative_and_empty_path_parts(self):
        case = self.prepare()
        original = harness.verify_prepared(case, self.runtime)
        before = harness.tree_manifest(case / "game")
        for key in ("../Config.pac", "D:Config.pac", "Config.pac:stream", "sub/./Config.pac",
                    "sub//Config.pac", "/Config.pac", "sub/../Config.pac"):
            manifest = json.loads(json.dumps(original))
            manifest["game_files"][key] = {"size": 1, "sha256": "0" * 64}
            with self.assertRaises(ValueError):
                harness.remove_verified_input_copies(case, manifest, self.cleanup_state(case), self.runtime)
            self.assertEqual(before, harness.tree_manifest(case / "game"))

    def test_artifact_lease_includes_a_directory_source_and_releases_on_failure(self):
        build = self.root / "target/bhe-playable-builds/run_20261031_000000_001"
        build.mkdir(parents=True)
        marker = {"schema": "nexas.bhe-playable-managed/v1", "projectRoot": str(self.root),
                  "buildsRoot": str(build.parent), "runName": build.name,
                  "runId": "00000000-0000-0000-0000-000000000001", "state": "COMPLETE"}
        (build / ".bhe-playable-owned.json").write_text(json.dumps(marker), encoding="utf-8")
        (build / ".artifact-use.lock").write_bytes(b"\x00")
        acquired = []
        with patch.object(harness, "_shared_artifact_lock", side_effect=lambda stream, value: acquired.append(value)):
            with self.assertRaisesRegex(RuntimeError, "body failed"):
                with harness.artifact_use_leases([build, build / "result.exe"]):
                    self.assertEqual(acquired, [True])
                    raise RuntimeError("body failed")
        self.assertEqual(acquired, [True, False])

    def test_unverified_managed_artifact_is_not_used(self):
        build = self.root / "target/bhe-playable-builds/run_20261031_000000_002"
        build.mkdir(parents=True)
        marker = {"schema": "nexas.bhe-playable-managed/v1", "projectRoot": str(self.root),
                  "buildsRoot": str(build.parent), "runName": build.name,
                  "runId": "00000000-0000-0000-0000-000000000002", "state": "BUILDING"}
        (build / ".bhe-playable-owned.json").write_text(json.dumps(marker), encoding="utf-8")
        (build / ".artifact-use.lock").write_bytes(b"\x00")
        with self.assertRaisesRegex(ValueError, "unverified managed build"):
            with harness.artifact_use_leases([build]):
                self.fail("A build still being produced must not be leased for launch")



class FakeOwnedProcess:
    """External process boundary only: lifecycle decisions remain in launch_case."""

    def __init__(self, terminate_succeeds=False):
        self.pid = 901
        self.identity = {"pid": 901, "creation_filetime": 123, "image": "offline.exe"}
        self.process_handle = 0x111
        self.thread_handle = 0x222
        self.closed = []
        self.dead = threading.Event()
        self.termination_attempted = threading.Event()
        self.terminate_succeeds = terminate_succeeds

    def require_identity(self):
        pass

    def poll(self):
        return 1 if self.dead.is_set() else None

    def terminate(self):
        self.termination_attempted.set()
        if self.terminate_succeeds:
            self.dead.set()
        else:
            raise RuntimeError("simulated termination failure")

    def close(self):
        if not self.dead.is_set():
            raise AssertionError("Closing live process handles")
        self.closed.extend(["thread_handle", "process_handle"])
        self.thread_handle = self.process_handle = None


class FakeSession:
    def __init__(self, child, late_load, loaded):
        self.child, self.late_load, self.loaded = child, late_load, loaded
        self.detached_while_alive = self.detached_after_exit = 0

    def on(self, name, callback):
        pass

    def create_script(self, source, name):
        return self

    def load(self):
        if not self.late_load.wait(3):
            raise RuntimeError("offline late-load deadline")
        self.loaded.set()

    def detach(self):
        if self.child.dead.is_set():
            self.detached_after_exit += 1
        else:
            self.detached_while_alive += 1


class FailingCleanupLog:
    def __init__(self, stream):
        self.stream = stream

    def write(self, value):
        if '"event": "terminating_owned_process"' in value:
            raise OSError("simulated log disk failure")
        return self.stream.write(value)

    def close(self):
        self.stream.close()


class FakeNativeFunction:
    def __init__(self, callback):
        self.callback = callback

    def __call__(self, *args):
        return self.callback(*args)


class FakeKernel:
    def __init__(self, terminate_result, exit_after_termination=False):
        self.closed = []
        self.terminated = False
        self.terminate_result = terminate_result
        self.exit_after_termination = exit_after_termination
        self.CreateProcessW = FakeNativeFunction(self.create)
        self.TerminateProcess = FakeNativeFunction(self.terminate)
        self.CloseHandle = FakeNativeFunction(lambda handle: self.closed.append(handle) or True)
        self.WaitForSingleObject = FakeNativeFunction(
            lambda *args: 0 if self.terminated and self.exit_after_termination else 258)
        self.GetExitCodeProcess = FakeNativeFunction(self.exit_code)
        for name in ("GetProcessTimes", "QueryFullProcessImageNameW", "GetProcessId", "ResumeThread"):
            setattr(self, name, FakeNativeFunction(lambda *args: 0))

    def create(self, *args):
        info = args[-1]._obj
        info.hProcess, info.hThread = 0x111, 0x222
        info.dwProcessId, info.dwThreadId = 901, 902
        return True

    def terminate(self, *args):
        self.terminated = self.terminate_result
        return self.terminate_result

    def exit_code(self, handle, code):
        code._obj.value = 1
        return True


if __name__ == "__main__":
    unittest.main()
