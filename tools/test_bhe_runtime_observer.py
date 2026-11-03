"""Offline observer contracts. Node simulates memory; no game or Frida is used."""

import sys
sys.dont_write_bytecode = True

import hashlib
import json
from pathlib import Path
import shutil
import struct
import subprocess
import unittest
from unittest.mock import patch

import bhe_runtime_session as harness

try:
    import bhe_runtime_observer as observer
except ModuleNotFoundError:
    observer = None


def pe_fixture():
    data = bytearray(0x231000)
    data[:2] = b"MZ"
    struct.pack_into("<I", data, 0x3C, 0x80)
    data[0x80:0x84] = b"PE\0\0"
    struct.pack_into("<HH", data, 0x84, 0x14C, 1)
    struct.pack_into("<H", data, 0x94, 0xE0)
    struct.pack_into("<H", data, 0x98, 0x10B)
    struct.pack_into("<I", data, 0xB4, 0x400000)
    struct.pack_into("<I", data, 0xD0, 0x240000)
    struct.pack_into("<IIII", data, 0x180, 0x230000, 0x1000, 0x230000, 0x200)
    for rva in (0x1FE030, 0x220A6C, 0x1FF58F, 0x21CC30):
        offset = rva - 0x1000 + 0x200
        data[offset:offset + 16] = b"\x90" * 16
    offset = 0x220A6C - 0x1000 + 0x200
    data[offset:offset + 6] = bytes.fromhex("89be38180000")
    return bytes(data)


NODE_MODEL = r'''
const assert = require('node:assert/strict');
const vm = require('node:vm');
const events = [], hooks = new Map(), memory = new Map(), signatures = new Map();
const base = 0x400000, player = 0x10000000, enemy = 0x10010000;
const preview = 0x10020000, descriptor = 0x10030000, unit = 0x10040000, stack = 0x10050000;
let unreadable = null, sendFails = false;
class Pointer {
  constructor(value) { this.value = Number(value); }
  add(value) { return new Pointer(this.value + Number(value)); }
  sub(value) { return new Pointer(this.value - Number(value.value ?? value)); }
  equals(value) { return this.value === value.value; }
  isNull() { return this.value === 0; }
  toString() { return '0x' + this.value.toString(16); }
  toInt32() { return this.value | 0; }
  toUInt32() { return this.value >>> 0; }
  readU8() { return this.readU32() & 255; }
  readU32() { if(this.value === unreadable) throw Error('simulated unreadable'); return (memory.get(this.value) ?? 0) >>> 0; }
  readS32() { return this.readU32() | 0; }
  readPointer() { return new Pointer(this.readU32()); }
  readByteArray(length) {
    const bytes = signatures.get(this.value);
    if (!bytes || bytes.length !== length) throw Error('missing signature memory');
    return Uint8Array.from(bytes).buffer;
  }
}
const ptr = value => new Pointer(value);
const put = (address, value) => memory.set(address, value);
const global = (rva, value) => put(base + rva, value);
function reset() {
  memory.clear();
  global(0x46CD23, 1); global(0x46CD54, 113); global(0x46CE14, preview);
  global(0x46CF88, player); global(0x479A5C, 100); global(0x46CE8C, 0); global(0x46CEA8, 0);
  put(stack + 4, descriptor); put(descriptor, 30); put(descriptor + 4, 0);
  put(preview + 0x108, 1); put(preview + 0x144, enemy); put(preview + 0x150, descriptor);
  put(player + 0x158, unit); put(unit + 0x2C, 113);
  put(enemy + 0x158, unit); put(player + 0x18DC, 30); put(enemy + 0x18DC, -1);
  put(player + 0x1618, 2000); put(player + 0x161C, 2000);
}
function context(registers) {
  return {context: Object.fromEntries(Object.entries(registers).map(([k,v]) => [k,ptr(v)]))};
}
function fire(rva, registers) { hooks.get(base + rva).onEnter.call(context(registers)); }
function request() { fire(0x1FE030, {ecx:preview, esp:stack}); }
function start(actor=player, weapon=30) { fire(0x220A6C, {esi:actor, ebx:weapon}); }
function ready() { put(player + 0x18DC, -1); fire(0x1FF58F, {esi:preview}); }
const ofType = type => events.filter(e => e.type === type);
const runtime = {
  Process: {arch:'ia32', pointerSize:4, mainModule:{base:ptr(base), path:'fixture.exe'},
    findRangeByAddress:p => p.isNull() ? null : {base:ptr(p.value & 0xffff0000), size:0x10000, protection:'rw-'}},
  Interceptor: {attach:(address, callbacks) => { hooks.set(address.value, callbacks); return {detach:()=>hooks.delete(address.value)}; }},
  send:e => { if(sendFails) throw Error('simulated send failure'); events.push(e); },
  ptr, Uint8Array, ArrayBuffer
};
for(const hook of PROFILE.hooks) signatures.set(base + hook.rva, Array.from(Buffer.from(hook.expected, 'hex')));
reset();
'''


class ObserverTests(unittest.TestCase):
    def setUp(self):
        if observer is None:
            self.fail("The passive gameplay observer is missing")
        self.image = pe_fixture()

    def profile(self, data=None):
        data = self.image if data is None else bytes(data)
        with patch.object(Path, "read_bytes", return_value=data), patch.object(
                observer, "KNOWN_EXE_SHA256", hashlib.sha256(data).hexdigest()):
            return observer.profile_for_exe(Path("synthetic-in-memory.exe"))

    def run_node(self, body, *, before=""):
        executable = shutil.which("node")
        self.assertIsNotNone(executable, "Node is required for the offline memory model")
        profile = self.profile()
        source = "const PROFILE=" + json.dumps(profile) + ";\n" + NODE_MODEL + before
        source += "\nconst SOURCE=" + json.dumps(observer._render(profile)) + ";\n"
        source += body
        result = subprocess.run([executable, "-"], input=source, text=True, capture_output=True,
                                cwd=Path(__file__).resolve().parents[1], timeout=15)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_exact_profile_maps_rva_through_pe_sections(self):
        profile = self.profile()
        self.assertEqual([h["rva"] for h in profile["hooks"]],
                         [0x1FE030, 0x220A6C, 0x1FF58F, 0x21CC30])
        self.assertEqual(profile["hooks"][1]["expected"], "89be38180000" + "90" * 10)

    def test_unknown_exe_hash_is_rejected_without_using_pe_claims(self):
        with patch.object(Path, "read_bytes", return_value=self.image):
            with self.assertRaisesRegex(ValueError, "SHA256"):
                observer.profile_for_exe(Path("unknown.exe"))

    def test_known_hash_does_not_bypass_start_instruction_signature(self):
        data = bytearray(self.image)
        data[0x220A6C - 0x1000 + 0x200] ^= 1
        with self.assertRaisesRegex(ValueError, "START"):
            self.profile(data)

    def test_wrong_machine_image_base_or_truncated_pe_is_rejected(self):
        for offset, value in ((0x84, b"\x64\x86"), (0xB4, b"\0\0\x50\0"), (0x80, b"NOPE")):
            data = bytearray(self.image)
            data[offset:offset + len(value)] = value
            with self.subTest(offset=offset), self.assertRaises(ValueError):
                self.profile(data)
        with self.assertRaises(ValueError):
            self.profile(self.image[:128])

    def test_unbacked_or_overlapping_hook_section_is_rejected(self):
        data = bytearray(self.image)
        struct.pack_into("<I", data, 0x188, 1)
        with self.assertRaises(ValueError):
            self.profile(data)
        data = bytearray(self.image)
        struct.pack_into("<H", data, 0x86, 2)
        data[0x1A0:0x1C8] = data[0x178:0x1A0]
        with self.assertRaises(ValueError):
            self.profile(data)

    def test_runtime_checks_all_signatures_before_attaching_any_hook(self):
        self.run_node("assert.throws(()=>vm.runInNewContext(SOURCE,runtime), /signature/); assert.equal(hooks.size,0);",
                      before="signatures.get(base+0x21CC30)[0]^=1;\n")

    def test_wrong_runtime_architecture_is_rejected(self):
        self.run_node("assert.throws(()=>vm.runInNewContext(SOURCE,runtime)); assert.equal(hooks.size,0);",
                      before="runtime.Process.arch='x64'; runtime.Process.pointerSize=8;\n")

    def test_partial_install_failure_detaches_owned_hooks(self):
        self.run_node("assert.throws(()=>vm.runInNewContext(SOURCE,runtime),/attach failure/); assert.equal(hooks.size,0); assert.equal(ofType('gameplay_observer_ready').length,0);",
                      before="const attach=runtime.Interceptor.attach; runtime.Interceptor.attach=(p,c)=>{if(hooks.size===2)throw Error('attach failure');return attach(p,c)};\n")

    def test_same_request_start_and_native_ready_form_one_closed_chain(self):
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); start(); ready(); ready(); assert.equal(ofType('sou_menu_request').length,1); assert.equal(ofType('sou_menu_matched_start').length,1); const e=ofType('sou_menu_ready'); assert.equal(e.length,1); assert.equal(e[0].requestSeq,1); assert.equal(e[0].startSeq,1); assert.equal(e[0].weapon,30); assert.equal(e[0].tama,0); assert.equal(e[0].script,0);")

    def test_ready_without_accepted_start_never_closes(self):
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); ready(); assert.equal(ofType('sou_menu_ready').length,0);")

    def test_wrong_actor_weapon_descriptor_or_variant_cannot_accept_start(self):
        scenarios = ["put(preview+0x150,descriptor+0x100); start();",
                     "put(descriptor+4,1); start();", "start(enemy);", "start(player,31);"]
        for scenario in scenarios:
            with self.subTest(scenario=scenario):
                self.run_node("vm.runInNewContext(SOURCE,runtime); request();" + scenario + "ready(); assert.equal(ofType('sou_menu_matched_start').length,0); assert.equal(ofType('sou_menu_ready').length,0);")

    def test_ready_identity_phase_variant_active_and_object_gates(self):
        mutations = ["global(0x46CF88,enemy);", "global(0x46CE14,preview+0x100);",
                     "put(preview+0x150,descriptor+0x100);", "put(descriptor+4,1);",
                     "put(descriptor,31);", "put(preview+0x108,2);",
                     "put(enemy+0x18DC,2);", "global(0x46CE8C,1);", "global(0x46CEA8,1);",
                     "global(0x46CD23,0);", "put(player+0x18DC,30);"]
        for mutation in mutations:
            with self.subTest(mutation=mutation):
                self.run_node("vm.runInNewContext(SOURCE,runtime); request(); start(); put(player+0x18DC,-1);" + mutation + "fire(0x1FF58F,{esi:preview}); assert.equal(ofType('sou_menu_ready').length,0);")

    def test_superseded_request_is_not_closed_by_next_requests_ready(self):
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); start(); request(); ready(); assert.equal(ofType('sou_menu_superseded').length,1); assert.equal(ofType('sou_menu_ready').length,0); start(); ready(); const e=ofType('sou_menu_ready'); assert.equal(e.length,1); assert.equal(e[0].requestSeq,2); assert.equal(e[0].startSeq,2);")

    def test_request_sequence_is_monotonic_and_out_of_range_is_not_accepted(self):
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); request(); put(descriptor,153); request(); put(descriptor,152); request(); assert.deepEqual(ofType('sou_menu_request').map(e=>e.requestSeq),[1,2,3]); assert.equal(ofType('sou_menu_range_alert').length,1);")
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); start(); put(descriptor,153); request(); put(descriptor,30); ready(); assert.equal(ofType('sou_menu_ready').length,0); const alert=ofType('sou_menu_range_alert'); assert.equal(alert.length,1); assert.equal(alert[0].invalidatedRequestSeq,1); request(); start(); ready(); assert.deepEqual(ofType('sou_menu_request').map(e=>e.requestSeq),[1,2]); assert.equal(ofType('sou_menu_ready').at(-1).requestSeq,2);")

    def test_release_is_an_observation_and_never_synthesizes_ready(self):
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); start(); fire(0x21CC30,{ecx:player,esp:stack}); assert.equal(ofType('gameplay_release_or_cancel').length,1); assert.equal(ofType('sou_menu_ready').length,0); put(unit+0x2C,104); start(); assert.equal(ofType('gameplay_start').at(-1).meka,104);")

    def test_bad_memory_and_failed_diagnostics_never_escape_game_callback(self):
        self.run_node("vm.runInNewContext(SOURCE,runtime); unreadable=descriptor; assert.doesNotThrow(request); assert.equal(ofType('gameplay_observer_error').length,1); sendFails=true; assert.doesNotThrow(request);")
        self.run_node("vm.runInNewContext(SOURCE,runtime); request(); start(); unreadable=descriptor; assert.doesNotThrow(request); unreadable=null; ready(); assert.equal(ofType('sou_menu_ready').length,0); assert.equal(ofType('gameplay_observer_error').length,1);")

    def test_only_passive_hooks_and_reads_are_emitted(self):
        source = observer._render(self.profile())
        for forbidden in ("NativeFunction", "NativeCallback", "Memory.", ".write", "replace(",
                          "setExceptionHandler", "setInterval", "setTimeout", "retval"):
            self.assertNotIn(forbidden, source)

    def test_launch_cli_observation_flag_is_opt_in(self):
        with patch.object(harness, "launch_case", return_value=0) as launch:
            self.assertEqual(harness.main(["launch", "--case", "offline", "--observe-gameplay"]), 0)
            self.assertTrue(launch.call_args.kwargs["observe_gameplay"])
        with patch.object(harness, "launch_case", return_value=0) as launch:
            self.assertEqual(harness.main(["launch", "--case", "offline"]), 0)
            self.assertFalse(launch.call_args.kwargs.get("observe_gameplay", False))

    def test_observer_profile_and_all_script_generation_precede_create_process(self):
        case = Path(__file__).resolve().parents[1] / "target/bhe-observer-checks/not-created"
        self.assertFalse(case.exists())
        with patch.object(harness, "isolation_script", return_value="offline isolation"), \
                patch.object(Path, "read_bytes", return_value=b"unknown EXE"), \
                patch.object(harness, "OwnedProcess") as native:
            with self.assertRaisesRegex(ValueError, "SHA256"):
                harness._launch_case(case, prepared_manifest={"exe_name": "unknown.exe"},
                                     observe_gameplay=True)
            native.assert_not_called()
        self.assertFalse(case.exists(), "Unknown profile must fail before launch.lock or process creation")

    def test_observer_installation_precedes_the_combined_scripts_hook_ready(self):
        self.run_node("vm.runInNewContext(SOURCE+\"\\nsend({type:'hook_ready'});\",runtime); assert.deepEqual(events.map(e=>e.type),['gameplay_observer_ready','hook_ready']);")
        self.run_node("assert.throws(()=>vm.runInNewContext(SOURCE+\"\\nsend({type:'hook_ready'});\",runtime)); assert.equal(ofType('hook_ready').length,0);",
                      before="signatures.get(base+0x1FE030)[0]^=1;\n")


if __name__ == "__main__":
    unittest.main()
