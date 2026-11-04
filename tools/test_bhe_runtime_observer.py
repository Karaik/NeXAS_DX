"""Offline observer contracts. Node simulates memory; no game or Frida is used."""

import sys
sys.dont_write_bytecode = True

import hashlib
import inspect
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
    offset = 0x0C2600 - 0x1000 + 0x200
    data[offset:offset + 10] = bytes.fromhex("568bf180be9d15000000")
    return bytes(data)


NODE_MODEL = r'''
const assert = require('node:assert/strict');
const vm = require('node:vm');
const events = [], hooks = new Map(), memory = new Map(), signatures = new Map();
const base = 0x400000, player = 0x10000000, enemy = 0x10010000;
const preview = 0x10020000, descriptor = 0x10030000, unit = 0x10040000, stack = 0x10050000;
const manager = 0x10060000, staleManager = 0x10070000, foreignActor = 0x10080000;
const replay = 0x10090000, input = 0x100A0000, otherReplay = 0x100B0000;
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
  put(enemy + 0x1618, 6000); put(enemy + 0x161C, 5900);
  put(player + 0x154, manager); put(manager + 8, player); put(staleManager + 8, player);
  put(manager + 0xCC, 2); put(manager + 0xD8, 3);
  put(replay + 0x28, input); put(otherReplay + 0x28, input);
  put(replay + 0x159D, 1); put(otherReplay + 0x159D, 1);
}
function context(registers) {
  return {context: Object.fromEntries(Object.entries(registers).map(([k,v]) => [k,ptr(v)]))};
}
function fire(rva, registers) {
  const listener = hooks.get(base + rva);
  (typeof listener === 'function' ? listener : listener.onEnter).call(context(registers));
}
function request() { fire(0x1FE030, {ecx:preview, esp:stack}); }
function start(actor=player, weapon=30) { fire(0x220A6C, {esi:actor, ebx:weapon}); }
function ready() { put(player + 0x18DC, -1); fire(0x1FF58F, {esi:preview}); }
const ofType = type => events.filter(e => e.type === type);
const runtime = {
  Process: {arch:'ia32', pointerSize:4, mainModule:{base:ptr(base), path:'fixture.exe'},
    findRangeByAddress:p => p.isNull() ? null : {base:ptr(p.value & 0xffff0000), size:0x10000, protection:'rw-'}},
  Interceptor: {attach:(address, callbacks) => {
    const instruction = [0x220A6C,0x1FF58F,0x223308,0x22389C].includes(address.value-base);
    assert.equal(typeof callbacks, instruction ? 'function' : 'object',
      instruction ? 'Mid-instruction hooks require a direct instruction probe' : 'Function entry keeps its call-listener ABI');
    if (!instruction) assert.equal(typeof callbacks.onEnter,'function');
    hooks.set(address.value, callbacks); return {detach:()=>hooks.delete(address.value)};
  }},
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

    def profile(self, data=None, *, combat=False):
        data = self.image if data is None else bytes(data)
        if combat:
            self.assertIn("observe_combat", inspect.signature(observer.profile_for_exe).parameters,
                          "The opt-in combat profile is not implemented")
        with patch.object(Path, "read_bytes", return_value=data), patch.object(
                observer, "KNOWN_EXE_SHA256", hashlib.sha256(data).hexdigest()):
            options = {"observe_combat": True} if combat else {}
            return observer.profile_for_exe(Path("synthetic-in-memory.exe"), **options)

    def run_node(self, body, *, before="", combat=False):
        executable = shutil.which("node")
        self.assertIsNotNone(executable, "Node is required for the offline memory model")
        profile = self.profile(combat=combat)
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

    def test_combat_adds_only_the_seven_explicit_hooks_when_enabled(self):
        self.assertEqual(len(self.profile()["hooks"]), 4)
        profile = self.profile(combat=True)
        self.assertEqual([hook["rva"] for hook in profile["hooks"]][4:],
                         [0x223308, 0x209090, 0x2097D0, 0x0C2600, 0x22389C, 0x21E760, 0x21B540])
        self.assertTrue(all(len(hook["expected"]) == 32 for hook in profile["hooks"]))
        self.run_node("vm.runInNewContext(SOURCE,runtime); assert.equal(hooks.size,11); assert.equal(ofType('gameplay_observer_ready').length,1);",
                      combat=True)

    def test_mid_instruction_probes_are_functions_and_true_entries_keep_on_enter_objects(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime);
for(const rva of [0x220A6C,0x1FF58F,0x223308,0x22389C])
  assert.equal(typeof hooks.get(base+rva),'function');
for(const rva of [0x1FE030,0x21CC30,0x209090,0x2097D0,0x0C2600,0x21E760,0x21B540]) {
  const callback=hooks.get(base+rva);
  assert.equal(typeof callback,'object'); assert.equal(typeof callback.onEnter,'function');
}
request(); start(); ready(); assert.equal(ofType('sou_menu_ready').length,1);
fire(0x223308,{esi:enemy,ecx:17}); assert.equal(ofType('combat_damage')[0].damage,17);
""", combat=True)

    def test_each_added_signature_is_checked_before_any_install_or_isolation_ready(self):
        for rva in (0x223308, 0x209090, 0x2097D0, 0x0C2600, 0x22389C, 0x21E760, 0x21B540):
            with self.subTest(rva=hex(rva)):
                self.run_node("assert.throws(()=>vm.runInNewContext(SOURCE+\"\\nsend({type:'hook_ready'});\",runtime),/signature/); assert.equal(hooks.size,0); assert.equal(ofType('hook_ready').length,0);",
                              before=f"signatures.get(base+{rva})[0]^=1;\n", combat=True)

    def test_damage_records_register_damage_and_raw_state_only_for_current_actors(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime);
put(enemy+0x18EC,17); put(enemy+0x188C,8); put(enemy+0x1890,10);
put(enemy+0x18F0,0); put(enemy+0x18F4,2);
put(enemy+0x1910,12345); put(enemy+0x1900,2); put(enemy+0x1904,1); put(enemy+0x1928,0);
fire(0x223308,{esi:foreignActor,ecx:999});
fire(0x223308,{esi:enemy,ecx:100}); fire(0x223308,{esi:player,ecx:7});
const e=ofType('combat_damage'); assert.equal(e.length,2);
assert.equal(e[0].actor,ptr(enemy).toString()); assert.equal(e[0].role,'enemy');
assert.equal(e[0].damage,100); assert.equal(e[0].hpAfter,5900); assert.equal(e[0].maxHp,6000);
assert.equal(e[0].controlBeforeReaction,17); assert.equal(e[0].state,8); assert.equal(e[0].nextState,10);
assert.equal(e[0].pendingReaction,0); assert.equal(e[0].reaction,2);
assert.equal(e[0].heatRaw,12345); assert.equal(e[0].normalGate,2); assert.equal(e[0].sdGate,1); assert.equal(e[0].sdBudgetRaw,0);
assert.equal('frame' in e[0],false); assert.equal(e[0].lastReplayFrame,null); assert.equal(e[0].lastReplayConsumer,null); assert.equal(e[0].tick,100);
assert.equal(e[1].role,'player'); assert.equal('sdAvailable' in e[0],false);
""", combat=True)

    def test_combat_manager_must_match_both_owner_and_actor_current_manager(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime); put(stack+4,7);
fire(0x209090,{ecx:staleManager,esp:stack});
put(staleManager+8,foreignActor); fire(0x2097D0,{ecx:staleManager});
fire(0x209090,{ecx:manager,esp:stack}); fire(0x2097D0,{ecx:manager});
put(manager+0xCC,0); fire(0x2097D0,{ecx:manager});
assert.equal(ofType('combat_use').length,1); assert.equal(ofType('combat_reset').length,1);
const use=ofType('combat_use')[0]; assert.equal(use.manager,ptr(manager).toString());
assert.equal(use.actor,ptr(player).toString()); assert.equal(use.usedBefore,2); assert.equal(use.chainBefore,3); assert.equal(use.slotRaw,7);
assert.equal(ofType('combat_recovery_entry').length,0); assert.equal(ofType('sou_menu_ready').length,0);
""", combat=True)

    def test_replay_frame_comes_only_from_consumer_and_unchanged_input_does_not_accumulate(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime);
global(0x46F700,foreignActor); put(foreignActor+0x15A0,999);
for(let frame=0;frame<60;frame++){put(replay+0x15A0,frame); fire(0x0C2600,{ecx:replay});}
assert.equal(ofType('combat_replay_input').length,1);
fire(0x223308,{esi:enemy,ecx:100}); let hit=ofType('combat_damage').at(-1);
assert.equal('frame' in hit,false); assert.equal(hit.lastReplayFrame,59); assert.equal(hit.lastReplayConsumer,ptr(replay).toString());
put(input,0x100); put(input+4,0x100); put(replay+0x15A0,60); fire(0x0C2600,{ecx:replay});
put(input,0); put(replay+0x15A0,61); fire(0x0C2600,{ecx:replay});
put(replay+0x15A0,62); fire(0x0C2600,{ecx:replay});
assert.equal(ofType('combat_replay_input').length,3);
put(otherReplay+0x15A0,4); fire(0x0C2600,{ecx:otherReplay}); fire(0x223308,{esi:enemy,ecx:20});
assert.equal(ofType('combat_replay_input').length,4); hit=ofType('combat_damage').at(-1);
assert.equal('frame' in hit,false); assert.equal(hit.lastReplayFrame,4); assert.equal(hit.lastReplayConsumer,ptr(otherReplay).toString());
assert.equal(ofType('combat_replay_input')[1].edge,0x100); assert.equal(ofType('combat_replay_input')[2].held,0x100);
""", combat=True)

    def test_reaction_recovery_and_motion_keep_the_native_actor_and_entry_semantics(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime); put(enemy+0x188C,8); put(enemy+0x18EC,0);
fire(0x22389C,{esi:enemy}); fire(0x22389C,{esi:foreignActor});
fire(0x2097D0,{ecx:manager});
assert.equal(ofType('combat_recovery_entry').length,0);
fire(0x21E760,{ecx:foreignActor}); fire(0x21E760,{ecx:enemy});
put(stack+4,8); fire(0x21B540,{ecx:enemy,esp:stack});
put(stack+4,11); fire(0x21B540,{ecx:enemy,esp:stack}); fire(0x21B540,{ecx:foreignActor,esp:stack});
assert.equal(ofType('combat_reaction_applied').length,1); assert.equal(ofType('combat_recovery_entry').length,1);
assert.equal(ofType('combat_recovery_entry')[0].actor,ptr(enemy).toString());
const transition=ofType('combat_motion_transition'); assert.equal(transition.length,1);
assert.equal(transition[0].fromState,8); assert.equal(transition[0].toState,11);
assert.equal(ofType('sou_menu_ready').length,0); assert.equal(ofType('combat_damage').length,0);
""", combat=True)

    def test_replay_without_an_input_pointer_does_not_emit_per_frame_errors_or_break_sou(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime); request(); start(); put(replay+0x28,0);
for(let frame=0;frame<60;frame++){put(replay+0x15A0,frame); fire(0x0C2600,{ecx:replay});}
assert.equal(ofType('combat_replay_input').length,0); assert.equal(ofType('gameplay_observer_error').length,0);
fire(0x223308,{esi:enemy,ecx:100}); assert.equal(ofType('combat_damage').at(-1).lastReplayFrame,null);
ready(); assert.equal(ofType('sou_menu_ready').length,1);
""", combat=True)

    def test_combat_memory_gap_invalidates_pending_sou_and_cached_frame_without_escaping(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime); request(); start();
put(replay+0x15A0,77); fire(0x0C2600,{ecx:replay});
unreadable=enemy+0x161C; assert.doesNotThrow(()=>fire(0x223308,{esi:enemy,ecx:100}));
unreadable=null; ready(); fire(0x223308,{esi:enemy,ecx:20});
assert.equal(ofType('sou_menu_ready').length,0);
assert.equal(ofType('gameplay_observer_error').at(-1).invalidatedRequestSeq,1);
assert.equal(ofType('combat_damage').at(-1).lastReplayFrame,null);
sendFails=true; unreadable=enemy+0x161C; assert.doesNotThrow(()=>fire(0x223308,{esi:enemy,ecx:100}));
""", combat=True)

    def test_combat_source_remains_passive_and_never_declares_combo_acceptance(self):
        source = observer._render(self.profile(combat=True))
        for forbidden in ("NativeFunction", "NativeCallback", "Memory.", ".write", "replace(",
                          "setExceptionHandler", "setInterval", "setTimeout", "retval", "combo_passed"):
            self.assertNotIn(forbidden, source)

    def test_stopped_replay_cache_is_explicitly_historical_on_subsequent_menu_and_hit(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime); put(replay+0x15A0,539); fire(0x0C2600,{ecx:replay});
const consumed=ofType('combat_replay_input')[0]; assert.equal(consumed.frame,539);
global(0x479A5C,900); request(); fire(0x223308,{esi:enemy,ecx:10});
for(const e of [ofType('sou_menu_request').at(-1),ofType('combat_damage').at(-1)]) {
  assert.equal('frame' in e,false); assert.equal('replayConsumer' in e,false);
  assert.equal(e.lastReplayFrame,539); assert.equal(e.lastReplayConsumer,ptr(replay).toString());
  assert.equal(e.replayReadTick,100); assert.equal(e.tick,900);
}
""", combat=True)

    def test_inactive_replay_cannot_refresh_cache_even_with_a_nonempty_input_pointer(self):
        self.run_node("""
vm.runInNewContext(SOURCE,runtime); request(); start();
put(replay+0x15A0,4); fire(0x0C2600,{ecx:replay});
put(replay+0x159D,0); put(input,0x100); put(input+4,0x100);
for(let frame=5;frame<65;frame++){put(replay+0x15A0,frame); fire(0x0C2600,{ecx:replay});}
assert.equal(ofType('combat_replay_input').length,1); assert.equal(ofType('gameplay_observer_error').length,0);
fire(0x223308,{esi:enemy,ecx:10}); const hit=ofType('combat_damage').at(-1);
assert.equal(hit.lastReplayFrame,null); assert.equal(hit.lastReplayConsumer,null); assert.equal(hit.replayReadTick,null);
ready(); assert.equal(ofType('sou_menu_ready').length,1);
put(replay+0x159D,1); put(replay+0x15A0,0); fire(0x0C2600,{ecx:replay});
assert.equal(ofType('combat_replay_input').length,2); assert.equal(ofType('combat_replay_input').at(-1).frame,0);
""", combat=True)

    def test_replay_active_byte_instruction_is_part_of_the_pinned_profile(self):
        data = bytearray(self.image)
        offset = 0x0C2600 - 0x1000 + 0x200
        data[offset + 5] = 0x9E
        with self.assertRaisesRegex(ValueError, "Replay active"):
            self.profile(data, combat=True)

    def test_combat_launch_requires_gameplay_and_explicitly_forwards_both_options(self):
        self.assertIn("observe_combat", inspect.signature(harness.launch_case).parameters)
        with patch.object(harness, "launch_case", return_value=0) as launch:
            self.assertEqual(harness.main(["launch", "--case", "offline", "--observe-gameplay", "--observe-combat"]), 0)
            self.assertTrue(launch.call_args.kwargs["observe_gameplay"])
            self.assertTrue(launch.call_args.kwargs["observe_combat"])
        with patch.object(harness, "launch_case", return_value=0) as launch:
            self.assertEqual(harness.main(["launch", "--case", "offline", "--observe-combat"]), 1)
            launch.assert_not_called()
        case = Path(__file__).resolve().parents[1] / "target/bhe-observer-checks/not-created"
        with self.assertRaisesRegex(ValueError, "gameplay"):
            harness.launch_case(case, observe_combat=True)
        with self.assertRaisesRegex(ValueError, "gameplay"):
            harness._launch_case(case, prepared_manifest={}, observe_combat=True)

    def test_combat_profile_generation_precedes_process_creation_and_lock_file(self):
        self.assertIn("observe_combat", inspect.signature(harness._launch_case).parameters)
        case = Path(__file__).resolve().parents[1] / "target/bhe-observer-checks/not-created"
        with patch.object(observer, "observer_script", side_effect=ValueError("combat profile checked")) as source, \
                patch.object(harness, "OwnedProcess") as native:
            with self.assertRaisesRegex(ValueError, "combat profile checked"):
                harness._launch_case(case, prepared_manifest={"exe_name": "known.exe"},
                                     observe_gameplay=True, observe_combat=True)
            source.assert_called_once_with(case / "game/known.exe", observe_combat=True)
            native.assert_not_called()
        self.assertFalse(case.exists())


if __name__ == "__main__":
    unittest.main()
