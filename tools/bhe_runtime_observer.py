"""Passive, profile-pinned BSDX observations recovered from preserved case09 logs.

The hooks only read registers/memory and send discrete events. Menu completion
requires the native READY site and the same accepted request; release is not READY.
"""

import hashlib
import json
from pathlib import Path
import struct


KNOWN_EXE_SHA256 = "1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae"
IMAGE_BASE = 0x400000
HOOKS = (("request", 0x1FE030), ("start", 0x220A6C),
         ("ready", 0x1FF58F), ("release", 0x21CC30))
COMBAT_HOOKS = (("damage", 0x223308), ("use", 0x209090), ("reset", 0x2097D0),
                ("replay", 0x0C2600), ("reaction", 0x22389C),
                ("recovery", 0x21E760), ("motion", 0x21B540))
# Combat sources: combo-yuri-follow-20261029-04, decoded log lines 154..159;
# motion: motoki-flight-suffix-20261029-02, decoded line 266.
# Source: preserved case09 runtime.cdb.log.json, decoded text lines 176..179.
# START bytes also appear in input-resume combo.cdb.log.json, text line 179.


def profile_for_exe(exe_path, *, observe_combat=False):
    """Read the exact known image and map hook RVAs through its PE sections."""
    data = Path(exe_path).read_bytes()
    digest = hashlib.sha256(data).hexdigest()
    if digest != KNOWN_EXE_SHA256:
        raise ValueError(f"Unknown gameplay observer EXE SHA256: {digest}")

    def number(offset, fmt):
        if offset < 0 or offset + struct.calcsize(fmt) > len(data):
            raise ValueError("Truncated gameplay observer PE image")
        return struct.unpack_from(fmt, data, offset)[0]

    if data[:2] != b"MZ":
        raise ValueError("Gameplay observer requires a PE image")
    pe = number(0x3C, "<I")
    if pe < 0x40 or data[pe:pe + 4] != b"PE\0\0" or number(pe + 4, "<H") != 0x14C:
        raise ValueError("Gameplay observer requires the known x86 PE image")
    sections, optional_size = number(pe + 6, "<H"), number(pe + 20, "<H")
    optional = pe + 24
    if not 1 <= sections <= 96 or optional_size < 96:
        raise ValueError("Unsupported gameplay observer PE headers")
    if number(optional, "<H") != 0x10B or number(optional + 28, "<I") != IMAGE_BASE:
        raise ValueError("Gameplay observer PE32 image base mismatch")
    image_size = number(optional + 56, "<I")
    table = optional + optional_size
    if table + sections * 40 > len(data):
        raise ValueError("Truncated gameplay observer PE sections")
    hooks = []
    for name, rva in HOOKS + (COMBAT_HOOKS if observe_combat else ()):
        candidates = []
        for index in range(sections):
            entry = table + index * 40
            virtual_size, address, raw_size, raw = struct.unpack_from("<IIII", data, entry + 8)
            if address <= rva < address + max(virtual_size, raw_size):
                delta = rva - address
                if delta + 16 > raw_size or raw + delta + 16 > len(data):
                    raise ValueError(f"Unbacked gameplay observer hook: {name}")
                candidates.append(raw + delta)
        if len(candidates) != 1 or rva + 16 > image_size:
            raise ValueError(f"Ambiguous or unmapped gameplay observer hook: {name}")
        expected = data[candidates[0]:candidates[0] + 16]
        if name == "start" and expected[:6] != bytes.fromhex("89be38180000"):
            raise ValueError("Historical START instruction signature mismatch")
        if name == "replay" and expected[:10] != bytes.fromhex("568bf180be9d15000000"):
            raise ValueError("Replay active-byte instruction signature mismatch")
        hooks.append({"name": name, "rva": rva, "expected": expected.hex()})
    return {"exeSha256": digest, "imageBase": IMAGE_BASE, "hooks": hooks,
            "observeCombat": bool(observe_combat)}


def observer_script(exe_path, *, observe_combat=False):
    """Validate before creating a process, then emit its passive Frida script."""
    return _render(profile_for_exe(exe_path, observe_combat=observe_combat))


def _render(profile):
    return "(function () {\n'use strict';\nconst profile = " + json.dumps(profile) + r''';
if (Process.arch !== 'ia32' || Process.pointerSize !== 4)
  throw new Error('Gameplay observer requires the known x86 runtime');
const base = Process.mainModule.base;
const installed = [];
let sequence = 0, startSequence = 0, request = null;
let replayFrame = null, replayConsumer = null, replayReadTick = null, previousInput = null;
function globalPointer(rva) { return base.add(rva).readPointer(); }
function mode() { return base.add(0x46cd23).readU8(); }
function tick() { return base.add(0x479a5c).readU32(); }
function counts() {
  return {tama: base.add(0x46ce8c).readU32(), script: base.add(0x46cea8).readU32()};
}
function requireReadable(address, size) {
  if (address.isNull()) throw new Error('Null gameplay observation pointer');
  const range = Process.findRangeByAddress(address);
  if (range === null || !range.protection.includes('r') ||
      address.add(size).sub(range.base).toUInt32() > range.size)
    throw new Error('Unreadable gameplay observation span: ' + address);
  return address;
}
function actor(address) {
  requireReadable(address, 0x1940);
  const unit = requireReadable(address.add(0x158).readPointer(), 0x30);
  const result = {actor: address.toString(), meka: unit.add(0x2c).readU32(),
    active: address.add(0x18dc).readS32(), group: address.add(0x15b8).readU32(),
    skill: address.add(0x15bc).readU32(), entry: address.add(0x15c0).readU32(),
    block: address.add(0x15c4).readU32(), slot71Clock: address.add(0x1480).readU32(),
    state: address.add(0x188c).readU32(), zRaw: address.add(0xa8).readS32(),
    hp: address.add(0x161c).readS32(), maxHp: address.add(0x1618).readS32()};
  if (profile.observeCombat) Object.assign(result, {
    control: address.add(0x18ec).readS32(), nextState: address.add(0x1890).readS32(),
    pendingReaction: address.add(0x18f0).readS32(), reaction: address.add(0x18f4).readS32(),
    heatRaw: address.add(0x1910).readS32(), normalGate: address.add(0x1900).readS32(),
    sdGate: address.add(0x1904).readS32(), sdBudgetRaw: address.add(0x1928).readS32()
  }); // Raw gates/budget are observations, not an assertion that SD is available.
  return result;
}
function emit(type, fields) {
  const metadata = {type: type, tick: tick()};
  if (profile.observeCombat) Object.assign(metadata, {lastReplayFrame: replayFrame,
    lastReplayConsumer: replayConsumer, replayReadTick: replayReadTick});
  send(Object.assign(metadata, fields));
}
function observe(name, callback) {
  const onHit = function () {
    try { callback(this.context); }
    catch (error) {
      const invalidated = request === null ? null : request.sequence;
      request = null; // An observation gap must never close a stale request later.
      replayFrame = null; replayConsumer = null; replayReadTick = null; previousInput = null;
      try { send({type: 'gameplay_observer_error', hook: name,
        invalidatedRequestSeq: invalidated, error: String(error)}); }
      catch (_) { /* Diagnostics must not escape into the observed game. */ }
    }
  };
  // Frida's instruction-probe form avoids function return-address/leave handling
  // at these four sites, where the native prologue has already changed the stack.
  return ['start', 'ready', 'damage', 'reaction'].includes(name) ? onHit : {onEnter: onHit};
}
function descriptorMatches(current) {
  requireReadable(current, 8);
  return current.equals(request.descriptor) && current.readS32() === request.weapon &&
    current.add(4).readS32() === request.variant;
}
function menuRequest(context) {
  if (mode() !== 1 || base.add(0x46cd54).readU32() !== 113 ||
      !context.ecx.equals(globalPointer(0x46ce14))) return;
  const preview = requireReadable(context.ecx, 0x154);
  const descriptor = requireReadable(requireReadable(context.esp, 8).add(4).readPointer(), 8);
  const weapon = descriptor.readS32(), variant = descriptor.add(4).readS32();
  if (weapon < 0 || weapon > 152) {
    const invalidated = request === null ? null : request.sequence;
    request = null;
    emit('sou_menu_range_alert', {weapon: weapon, invalidatedRequestSeq: invalidated}); return;
  }
  if (request !== null && !request.ready)
    emit('sou_menu_superseded', {requestSeq: request.sequence, weapon: request.weapon,
      acceptedStarts: request.acceptedStarts});
  request = {sequence: ++sequence, preview: preview, descriptor: descriptor,
    weapon: weapon, variant: variant, acceptedActor: null, acceptedStarts: 0,
    acceptedStartSeq: null, ready: false};
  emit('sou_menu_request', Object.assign({requestSeq: request.sequence, weapon: weapon,
    variant: variant, preview: preview.toString(), descriptor: descriptor.toString(),
    oldPlayer: globalPointer(0x46cf88).toString()}, counts()));
}
function weaponStart(context) {
  if (!context.esi.equals(globalPointer(0x46cf88))) return;
  const current = actor(context.esi), weapon = context.ebx.toInt32(), currentMode = mode();
  const startSeq = ++startSequence;
  emit('gameplay_start', Object.assign({startSeq: startSeq, weapon: weapon,
    previewMode: currentMode, requestSeq: request === null ? null : request.sequence}, current, counts()));
  if (current.meka !== 113 || currentMode !== 1 || request === null || request.ready ||
      weapon !== request.weapon || !request.preview.equals(globalPointer(0x46ce14))) return;
  requireReadable(request.preview, 0x154);
  if (!descriptorMatches(request.preview.add(0x150).readPointer())) return;
  request.acceptedActor = context.esi;
  request.acceptedStarts++;
  request.acceptedStartSeq = startSeq;
  emit('sou_menu_matched_start', {requestSeq: request.sequence, startSeq: startSeq,
    weapon: weapon, variant: request.variant, actor: current.actor});
}
function menuReady(context) {
  if (mode() !== 1 || !context.esi.equals(globalPointer(0x46ce14))) return;
  const preview = requireReadable(context.esi, 0x154);
  const player = globalPointer(0x46cf88), enemy = preview.add(0x144).readPointer();
  const descriptor = requireReadable(preview.add(0x150).readPointer(), 8);
  const current = actor(player), other = actor(enemy), objects = counts();
  if (current.meka !== 113) return;
  emit('sou_ready_candidate', Object.assign({requestSeq: request === null ? null : request.sequence,
    requestedWeapon: request === null ? null : request.weapon, actualWeapon: descriptor.readS32(),
    actor: current.actor, enemy: other.actor, playerActive: current.active,
    enemyActive: other.active}, objects));
  if (request === null || request.ready || request.acceptedStarts < 1 ||
      !player.equals(request.acceptedActor) || !preview.equals(request.preview) ||
      !descriptorMatches(descriptor) || preview.add(0x108).readU32() !== 1 ||
      current.active !== -1 || other.active !== -1 || objects.tama !== 0 || objects.script !== 0) return;
  request.ready = true;
  emit('sou_menu_ready', Object.assign({requestSeq: request.sequence, startSeq: request.acceptedStartSeq,
    weapon: request.weapon, variant: request.variant, enemy: other.actor,
    enemyActive: other.active, enemyHp: other.hp, enemyMaxHp: other.maxHp}, current, objects));
}
function weaponRelease(context) {
  if (!context.ecx.equals(globalPointer(0x46cf88))) return;
  const current = actor(context.ecx);
  if (current.active < 0 || current.active > 152) return;
  emit('gameplay_release_or_cancel', Object.assign({previewMode: mode(),
    requestSeq: request === null ? null : request.sequence}, current, counts()));
}
function combatActor(address) {
  if (address.equals(globalPointer(0x46cf88)))
    return Object.assign({role: 'player'}, actor(address));
  const preview = globalPointer(0x46ce14);
  if (preview.isNull()) return null;
  const enemy = requireReadable(preview, 0x148).add(0x144).readPointer();
  if (!address.equals(enemy)) return null;
  return Object.assign({role: 'enemy'}, actor(address));
}
function combatDamage(context) {
  const current = combatActor(context.esi);
  if (current === null) return;
  emit('combat_damage', Object.assign({damage: context.ecx.toInt32(), hpAfter: current.hp,
    controlBeforeReaction: current.control}, current));
}
function combatManager(context, resetting) {
  const manager = requireReadable(context.ecx, 0xdc);
  const owner = manager.add(8).readPointer(), current = combatActor(owner);
  if (current === null || !owner.add(0x154).readPointer().equals(manager)) return;
  const used = manager.add(0xcc).readU32(), chain = manager.add(0xd8).readU32();
  if (resetting && used === 0) return; // The native check also runs on empty chains.
  const fields = Object.assign({manager: manager.toString(), usedBefore: used, chainBefore: chain}, current);
  if (!resetting) fields.slotRaw = requireReadable(context.esp, 8).add(4).readU32();
  emit(resetting ? 'combat_reset' : 'combat_use', fields);
}
function combatReplay(context) {
  const consumer = requireReadable(context.ecx, 0x15a8);
  if (consumer.add(0x159d).readU8() === 0) {
    replayFrame = null; replayConsumer = null; replayReadTick = null; previousInput = null;
    return; // 4C2600 consumes input only under this exact native active-byte guard.
  }
  const inputAddress = consumer.add(0x28).readPointer();
  if (inputAddress.isNull()) {
    replayFrame = null; replayConsumer = null; replayReadTick = null; previousInput = null;
    return; // The preserved CDB observer also skipped an absent input pointer.
  }
  const input = requireReadable(inputAddress, 8);
  const frame = consumer.add(0x15a0).readU32(), identity = consumer.toString();
  const edge = input.readU32(), held = input.add(4).readU32();
  replayFrame = frame; replayConsumer = identity; replayReadTick = tick();
  if (previousInput !== null && previousInput.consumer === identity &&
      previousInput.edge === edge && previousInput.held === held) return;
  previousInput = {consumer: identity, edge: edge, held: held};
  emit('combat_replay_input', {frame: frame, edge: edge, held: held});
}
function combatReaction(context) {
  const current = combatActor(context.esi);
  if (current !== null) emit('combat_reaction_applied', current);
}
function combatRecovery(context) {
  const current = combatActor(context.ecx);
  if (current !== null) emit('combat_recovery_entry', current);
}
function combatMotion(context) {
  const current = combatActor(context.ecx);
  if (current === null) return;
  const next = requireReadable(context.esp, 8).add(4).readS32();
  if (current.state !== next)
    emit('combat_motion_transition', Object.assign({fromState: current.state, toState: next}, current));
}
const callbacks = {request: menuRequest, start: weaponStart, ready: menuReady, release: weaponRelease,
  damage: combatDamage, use: context => combatManager(context, false),
  reset: context => combatManager(context, true), replay: combatReplay,
  reaction: combatReaction, recovery: combatRecovery, motion: combatMotion};
// Check every original instruction span before attaching the first hook.
for (const hook of profile.hooks) {
  const bytes = new Uint8Array(base.add(hook.rva).readByteArray(hook.expected.length / 2));
  const actual = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
  if (actual !== hook.expected) throw new Error('Gameplay observer runtime signature mismatch: ' + hook.name);
}
try {
  for (const hook of profile.hooks)
    installed.push(Interceptor.attach(base.add(hook.rva), observe(hook.name, callbacks[hook.name])));
  send({type: 'gameplay_observer_ready', exeSha256: profile.exeSha256,
    base: base.toString(), hooks: profile.hooks.map(hook => ({name: hook.name, rva: hook.rva})),
    policy: 'passive discrete observations only; no gameplay acceptance asserted'});
} catch (error) {
  for (const listener of installed.reverse()) { try { listener.detach(); } catch (_) {} }
  throw error;
}
})();
'''
