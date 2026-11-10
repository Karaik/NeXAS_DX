# BIN Pseudo Editor

## Goal

This editor is for BSDX `.bin` stage / level scripts.

The core rule is:

- `ASM` mode must be strictly reversible.
- `Pseudo` mode must also compile back to the exact original binary when the displayed lines are not changed.

## Modes

### ASM

- Displays strict instruction IR.
- One line maps to one instruction.
- `Compile Back` uses `compileStrict(...)`.
- This is the primary source of truth when exact low-level editing is needed.

### Pseudo

- Displays single-line pseudo code with no visible IR trailer.
- Internally every rendered line still carries hidden low-level replay metadata.
- `Compile Back` uses `compile(...)`.
- Unchanged lines replay the original instruction slice exactly.
- Changed lines fall back to semantic compilation.

This means `Pseudo` is not just a preview anymore. It is a lossless pseudo representation.

## Why hidden metadata exists

Normal high-level pseudo code is not naturally byte-reversible.

Examples:

- Multiple low-level instructions may collapse into one `syscall ...`.
- Register / stack traffic may be normalized into one expression.
- Labels may be re-numbered.
- Constants / 68-byte table blocks may be normalized.

To preserve exact round-trip behavior, pseudo lines store:

- original instruction indices
- original opcode / operand slices
- a hash of the visible content

If the visible line text is unchanged, the compiler replays the original IR directly.

## Tooltip metadata

In `Pseudo` mode, hovering a syscall / operand name shows details loaded from:

- `/research/bin立即数.csv`

The tooltip includes:

- enum name
- decimal / hex code
- description
- parameter count
- `a ~ m` parameter notes when present

The loading entrypoint is:

- `OperandDocRegistry`

The enum is annotated with:

- `@OperandDocSource`

## Comparison with external Script.cs

Current parity with the external `Script.cs` baseline:

- label rendering
- entry / jump / direct jump rendering
- arithmetic / compare / assignment pseudo output
- syscall rendering
- recompilation from a text form

Added on top of that:

- strict reversible ASM mode
- lossless pseudo mode with hidden replay metadata
- full `Hell*.bin` exact byte round-trip verification
- tooltip docs from local CSV knowledge

## Verification

The following tests must pass:

- synthetic pseudo round-trip
- synthetic strict round-trip
- all `Hell*.bin` strict round-trip
- all `Hell*.bin` pseudo round-trip

Current regression test class:

- `com.giga.nexas.bsdx.BsdxBinRendererTest`

