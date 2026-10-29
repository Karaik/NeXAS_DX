"""Create new BSDX training replays containing ordinary per-frame button input.

This tool preserves the template's header and first two compressed blocks exactly.
It never launches the game or writes a weapon/actor state. Runtime combo acceptance
still requires the game's normal replay UI, distinct weapon starts and hit chains.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import struct
import sys
import zlib


HEADER = struct.pack("<II", 104, 3852)
MAX_FRAMES = 216_000
MAX_FILE_BYTES = 8 * 1024 * 1024
MAX_JSON_BYTES = 1024 * 1024
MAX_RANGES = 4096
CONTROLS = {"up": 0x001, "right": 0x004, "down": 0x010, "left": 0x040,
            "A": 0x100, "B": 0x200, "C": 0x400, "D": 0x800}
KNOWN_MASK = sum(CONTROLS.values())
PROJECT_ROOT = Path(__file__).resolve().parents[1]


class InvalidReplay(ValueError):
    """A replay or input schedule cannot be used without guessing its meaning."""


@dataclass(frozen=True)
class Block:
    offset: int
    encoded: bytes
    decoded: bytes


@dataclass(frozen=True)
class Replay:
    data: bytes
    blocks: tuple[Block, Block, Block]

    @property
    def frames(self) -> int:
        return len(self.blocks[2].decoded) // 8


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def bounded_read(path: Path, maximum: int) -> bytes:
    with path.open("rb") as source:
        data = source.read(maximum + 1)
    if len(data) > maximum:
        raise InvalidReplay(f"Input exceeds {maximum} bytes: {path}")
    return data


def parse_replay(data: bytes) -> Replay:
    if len(data) > MAX_FILE_BYTES or data[:8] != HEADER:
        raise InvalidReplay("Expected BSDX training replay header DWORDs 104, 3852")
    offset = 8
    blocks = []
    for index, limit in enumerate((1112, 2732, MAX_FRAMES * 8)):
        start = offset
        if offset + 4 > len(data):
            raise InvalidReplay(f"Block {index}: truncated raw length")
        raw_length, = struct.unpack_from("<I", data, offset)
        offset += 4
        if raw_length > limit or (index < 2 and raw_length != limit):
            raise InvalidReplay(f"Block {index}: unsupported raw length {raw_length}")
        if index == 2 and raw_length % 8:
            raise InvalidReplay("Input block length must be exactly 8 * frame count")
        decoded = b""
        if raw_length:
            if offset + 4 > len(data):
                raise InvalidReplay(f"Block {index}: truncated compressed length")
            compressed_length, = struct.unpack_from("<I", data, offset)
            offset += 4
            if not compressed_length or compressed_length > MAX_FILE_BYTES or offset + compressed_length > len(data):
                raise InvalidReplay(f"Block {index}: invalid/truncated compressed payload")
            compressed = data[offset:offset + compressed_length]
            offset += compressed_length
            decoder = zlib.decompressobj()
            try:
                decoded = decoder.decompress(compressed, raw_length + 1)
            except zlib.error as error:
                raise InvalidReplay(f"Block {index}: invalid zlib stream") from error
            if (len(decoded) != raw_length or not decoder.eof
                    or decoder.unused_data or decoder.unconsumed_tail):
                raise InvalidReplay(f"Block {index}: decoded size or zlib EOF mismatch")
        blocks.append(Block(start, data[start:offset], decoded))
    if offset != len(data):
        raise InvalidReplay("Trailing bytes after the three replay blocks")
    return Replay(data, tuple(blocks))


def integer(value: object, label: str, minimum: int, maximum: int) -> int:
    if type(value) is not int or not minimum <= value <= maximum:
        raise InvalidReplay(f"{label} must be an integer in [{minimum}, {maximum}]")
    return value


def held_mask(value: object) -> int:
    if type(value) is int:
        if value < 0 or value & ~KNOWN_MASK:
            raise InvalidReplay(f"Unknown held input bits: {value!r}")
        return value
    if not isinstance(value, list) or any(type(name) is not str or name not in CONTROLS for name in value):
        raise InvalidReplay("held must be a known numeric mask or a list of control names")
    result = 0
    for name in value:
        result |= CONTROLS[name]
    return result


def compile_inputs(schedule: object) -> tuple[bytes, dict]:
    if not isinstance(schedule, dict) or set(schedule) - {"frames", "tail_frames", "ranges"}:
        raise InvalidReplay("Input JSON must contain only frames, tail_frames and ranges")
    frames = integer(schedule.get("frames"), "frames", 2, MAX_FRAMES)
    tail = integer(schedule.get("tail_frames", 60), "tail_frames", 1, frames - 1)
    ranges = schedule.get("ranges")
    if not isinstance(ranges, list) or len(ranges) > MAX_RANGES:
        raise InvalidReplay(f"ranges must be a list of at most {MAX_RANGES} intervals")
    events: dict[int, list[tuple[int, int]]] = {}
    for index, item in enumerate(ranges):
        if not isinstance(item, dict) or set(item) != {"start", "end", "held"}:
            raise InvalidReplay(f"ranges[{index}] must contain exactly start, end, held")
        start = integer(item["start"], f"ranges[{index}].start", 0, frames - tail - 1)
        end = integer(item["end"], f"ranges[{index}].end", start + 1, frames - tail)
        mask = held_mask(item["held"])
        events.setdefault(start, []).append((mask, 1))
        events.setdefault(end, []).append((mask, -1))
    # Half-open intervals overlap by OR. Edges are computed after combining them,
    # so continuing a held key through overlapping ranges never retriggers it.
    data = bytearray(frames * 8)
    previous = 0
    counts = {mask: 0 for mask in CONTROLS.values()}
    current = 0
    for frame in range(frames):
        if frame in events:
            for mask, delta in events[frame]:
                for bit in counts:
                    if mask & bit:
                        counts[bit] += delta
            current = sum(bit for bit, count in counts.items() if count)
        struct.pack_into("<II", data, frame * 8, current & ~previous, current)
        previous = current
    return bytes(data), {"frames": frames, "tail_frames": tail, "range_count": len(ranges),
                         "range_semantics": "start inclusive, end exclusive; overlap OR",
                         "edge_semantics": "held & ~previous_held; previous starts at zero"}


def mapping_evidence() -> dict:
    evidence = {
        "target/input-mapping-static-20261028/config-keyboard.json":
            "Verified Config table slots 0/2/4/6 are Up/Right/Down/Left; 8/9/10/11 are Z/X/C/V = A/B/C/D.",
        "src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/749580.c":
            "32 mapping slots produce bit 1<<slot from DirectInput scan codes.",
        "src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/61E310.c":
            "Lines 47-60: 0x100/0x200/0x400 choose ordinary A/B/C; line130 requires actor normal cancel gate.",
        "src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/728290.c":
            "Final branch maps Shift to the same 0x800 D/search-dash input bit.",
        "src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/4C2600.c":
            "Replay consumes each pair as edge then held; no weapon start is stored.",
        "src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6221E0.c":
            "Lines99-100 copy ordinary global input into actor+5548/+5552; normal action logic remains active.",
    }
    return {relative: {"meaning": meaning,
                       "sha256": digest(bounded_read(PROJECT_ROOT / relative, MAX_FILE_BYTES))
                       if (PROJECT_ROOT / relative).is_file() else None}
            for relative, meaning in evidence.items()}


def inspect_replay(path: Path, replay: Replay) -> dict:
    seed = struct.unpack("<683I", replay.blocks[1].decoded)
    unknown = 0
    edge_count = 0
    for edge, held in struct.iter_unpack("<II", replay.blocks[2].decoded):
        unknown |= (edge | held) & ~KNOWN_MASK
        edge_count += bool(edge)
    return {"path": str(path.resolve()), "size": len(replay.data), "sha256": digest(replay.data),
            "header": [104, 3852], "frames": replay.frames, "nonzero_edge_frames": edge_count,
            "input_bits_outside_generator_subset": f"0x{unknown:08X}",
            "blocks": [{"offset": block.offset, "encoded_bytes": len(block.encoded),
                        "decoded_bytes": len(block.decoded), "encoded_sha256": digest(block.encoded),
                        "decoded_sha256": digest(block.decoded)} for block in replay.blocks],
            "initial_state": {"player_meka": seed[1], "enemy_meka": seed[2], "map": seed[3],
                              "equipment_ids": list(struct.unpack("<14i", replay.blocks[1].decoded[1260:1316]))},
            "controls": {name: f"0x{mask:03X}" for name, mask in CONTROLS.items()},
            "native_evidence": mapping_evidence(),
            "runtime_validated": False}


def check_new_output(path: Path, template: Path) -> Path:
    # Reject existing targets (also broken symlinks) and ancestor reparse points.
    # Exclusive creation below closes the ordinary check/create overwrite race.
    path = path.absolute()
    if path.resolve() == template.resolve():
        raise InvalidReplay("Output must not be the template")
    if path.exists() or path.is_symlink():
        raise FileExistsError(f"Output already exists; nothing overwritten: {path}")
    for ancestor in path.parents:
        if ancestor.is_symlink() or (ancestor.exists() and getattr(ancestor.lstat(), "st_file_attributes", 0) & 0x400):
            raise InvalidReplay(f"Output ancestor is a symlink/reparse point: {ancestor}")
    if not path.parent.is_dir():
        raise InvalidReplay("Output parent must already exist")
    return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_subparsers(dest="mode", required=True)
    inspect = modes.add_parser("inspect", help="Read and validate a replay without writing")
    inspect.add_argument("--template", type=Path, required=True)
    create = modes.add_parser("create", help="Create a new replay from an explicit input schedule")
    create.add_argument("--template", type=Path, required=True)
    create.add_argument("--out", type=Path, required=True)
    create.add_argument("--input-json", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        template_bytes = bounded_read(args.template, MAX_FILE_BYTES)
        template = parse_replay(template_bytes)
        report = {"template": inspect_replay(args.template, template)}
        if args.mode == "create":
            output = check_new_output(args.out, args.template)
            schedule_bytes = bounded_read(args.input_json, MAX_JSON_BYTES)
            schedule = json.loads(schedule_bytes.decode("utf-8"))
            inputs, schedule_info = compile_inputs(schedule)
            compressed_inputs = zlib.compress(inputs)
            encoded_inputs = struct.pack("<II", len(inputs), len(compressed_inputs)) + compressed_inputs
            prefix = HEADER + template.blocks[0].encoded + template.blocks[1].encoded
            generated = prefix + encoded_inputs
            parsed = parse_replay(generated)
            if parsed.blocks[:2] != template.blocks[:2] or parsed.blocks[2].decoded != inputs:
                raise InvalidReplay("Generated replay failed preservation/readback verification")
            if bounded_read(args.template, MAX_FILE_BYTES) != template_bytes:
                raise InvalidReplay("Template changed during generation; no output created")
            with output.open("xb") as destination:
                destination.write(generated)
            written = parse_replay(bounded_read(output, MAX_FILE_BYTES))
            if written.data != generated:
                raise InvalidReplay("New output differs from prepared bytes; evidence retained")
            report.update(output=inspect_replay(output, written), schedule=schedule_info,
                          input_json={"path": str(args.input_json.resolve()), "sha256": digest(schedule_bytes)},
                          preserved_prefix_sha256=digest(prefix),
                          preserved_header_and_first_two_encoded_blocks=True)
        print(json.dumps(report, ensure_ascii=True, indent=2))
        return 0
    except (OSError, ValueError, struct.error) as error:
        print(f"Replay fixture rejected: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
