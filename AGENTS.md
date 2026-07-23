# AGENTS.md — java-life

Operating instructions for AI coding agents working in this repository.
Tool-agnostic — `CLAUDE.md` is a pointer to this file.

> This repo sits inside the `C:\Users\mike_\git` workspace. The workspace-level
> `AGENTS.md` still applies (agent identity on GitHub, British English); this
> file overrides it where they differ.

---

## What this is

**Java Life** is a flexible cellular automata engine inspired by Conway's Game
of Life. It ships a small AWT/Swing GUI, but the interesting part is the engine:
arbitrary rulesets over up to 256 cell states, driven entirely by lookup tables.

Licence: GPL. Upstream: https://github.com/mikera/java-life

## Tech stack

| Item | Value |
|------|-------|
| Language | Java 17 (`maven-compiler-plugin` source/target 17) |
| Build | Maven (`pom.xml`, parent `net.mikera:mikera-pom:0.0.4`) |
| Dependency | `net.mikera:mikera:1.6.1` (from Clojars) — `Rand`, `TextUtils`, `Gradient` |
| GUI | AWT + Swing (`JFrame`, `JComponent`, AWT `MenuBar`) |
| Tests | JUnit 6 (`org.junit.jupiter:junit-jupiter`, pinned via `${junit.version}`) |
| CI | GitHub Actions — `.github/workflows/ci.yml`, builds on JDK 17 and 21 |
| Branch | `master` |

## Project structure

```
src/main/java/mikera/life/
  Engine.java      # Simulation core: grid state, neighbour totals, step function
  Rules.java       # Ruleset data: transition table, effect values, colour palette
  RuleSets.java    # Named built-in rulesets
  LifePanel.java   # Swing component rendering the grid via a BufferedImage
  LifeApp.java     # Entry point: menu bar, keyboard/mouse input, simulation loop
src/test/java/mikera/life/
  TestEngine.java  # Engine + ruleset tests (JUnit 6)
```

Surefire picks up `Test*.java`, so keep the `TestXxx` naming.

## Architecture — read this before touching `Engine`

The engine is fast because it **never scans neighbours**. Two parallel
256×256 `byte[]` arrays are kept in lockstep:

- `values[i]` — the cell's state (0–255), an index into the ruleset
- `totals[i]` — the running sum of the *effect values* of that cell's 8 neighbours

A step is then a flat table lookup per cell, with no inner loop:

```java
nextState(value, total) == rules.getTransitions()[((value & 255) << 8) + (total & 255)]
```

`totals` is maintained **incrementally**: `changeColour(i, old, new)` writes the
new value and adds the effect-value delta into the eight neighbouring `totals`
entries. Nothing recomputes totals from scratch.

Writes go to the shadow buffers `nvalues` / `ntotals`; `flip()` swaps them with
`values` / `totals` and copies the result back over the shadows.

### Invariants — load-bearing, do not break

- **The grid is fixed at 256×256.** Index is `x + (y << 8)`.
  `Engine.GRIDSIZE` (`Engine.java:11`) is decorative: changing it will *not*
  resize anything, because the masks and offsets are hardcoded throughout
  `Engine` and `LifePanel`. Resizing means reworking all of them together.
- **The wrap is helical, not a true torus.** Neighbour offsets (±1, ±255, ±256,
  ±257) are applied *flat* with `& 65535`, so the left neighbour of column 0 is
  column 255 of the **previous row**, not of the same row. Vertical wrap is
  clean; horizontal wrap shears by one row. Interior cells are unaffected, so
  put test patterns well away from columns 0 and 255. Note `setCell` masks
  per-axis (`(x & 255) + ((y & 255) << 8)`), which disagrees with the flat
  offsets at those edges — a pre-existing quirk, not something to "tidy" without
  deciding what the boundary should actually be.
- **`totals[i]` must always equal the sum of the neighbours' effect values.**
  Every state mutation goes through `changeColour` (or `setCell` / `clear` /
  the `fill*` helpers, which call it). Never assign into `values[]` or
  `nvalues[]` directly — it silently desynchronises `totals` and the automaton
  quietly produces garbage.
- **`changeColour`'s old-colour argument must come from `nvalues`, not `values`.**
  It applies its delta to the *shadow* totals, so the delta has to be relative to
  what the shadow buffer already records. Passing `values[i]` double-counts any
  repeated write to the same cell between flips. `calculate` is the one exception
  and is safe: it visits each index once, before anything else has touched
  `nvalues[i]` that pass.
- **`values`, `totals` and `effectValues` are `byte[]` — signed, with deliberate
  wraparound.** Always index with `& 255`. Negative effect values are a feature
  (see the `warfare` ruleset).
- **`transitions` is a flat `byte[256 * 256]`**, indexed
  `fromValue * 256 + (total & 255)`.
- **Threading:** the simulation loop, AWT event thread and repaint all touch the
  engine. `calculate`, `setCell` and `clear` are `synchronized` on the `Engine`.
  Any new mutator must be too.

### Rulesets

`RuleSets.getRules(name)` returns the built-ins: `life`, `mikera-1`,
`brians-brain`, `warfare`. A `Rules` object is three tables:

- `transitions[from * 256 + total]` → next state
- `effectValues[state]` → what this state contributes to its neighbours' totals
- `colours[state]` → ARGB colour for rendering

Adding a ruleset means adding a `setupXxxRules()` method and a `case` in
`getRules`, plus a menu entry in `LifeApp.createMenuBar()` (`"rules:<name>"`).

## Build and test

```bash
mvn clean install     # Full build and test
mvn test              # Run tests
mvn test -Dtest=TestEngine#blinkerOscillatesWithPeriodTwo   # Single test
mvn compile           # Compile only

# Run the GUI (resolves exec-maven-plugin from central)
mvn compile exec:java -Dexec.mainClass=mikera.life.LifeApp
```

The GUI is interactive and blocking — **do not launch it from an agent tool
call** unless the user explicitly asks. Verify engine changes with code, not by
running the app.

Controls: `space` pause, `c` clear, `f` random fill, `p` scatter points,
`0`–`9` select draw colour, left-drag to paint.

### Writing tests

`TestEngine` sits in the `mikera.life` package, so package-private members
(`setupRandomRules`, `clear`, the `RuleSets.setupXxxRules` factories) are
testable directly — prefer that over widening visibility.

The suite's backbone is `assertTotalsConsistent`, which recomputes `totals` by
brute force and compares. **Assert it after any test that mutates state** — it
is what catches a broken incremental update, which is otherwise invisible until
the automaton drifts several generations later.

Mutating helper: `setCells(engine, colour, cells...)`. `setCell` publishes each
write, so repeats and same-cell overwrites are safe.

## Known issues

Confirmed by review, still unfixed. Do not treat these as intentional.

| Where | Issue |
|-------|-------|
| `Engine.setupRandomRules` | Replaces `rules` without recomputing `totals`, so totals are stale against the new effect values until the grid is refilled. |
| `LifeApp.java:178` | `new Thread(app).run()` calls `run()` directly on the calling thread instead of `start()`. The `Thread` is pointless and the simulation loop blocks `main`. |
| `LifeApp.java:47-50` | File → "Open..." / "Save As..." have no action listeners. `Engine.saveRules` is unwired and there is no loader — the README's `TODO: save / load rulesets`. |
| `RuleSets.java:13` | Throws raw `Error` for an unknown ruleset name; should be `IllegalArgumentException`. Tests currently assert `Error`, so change both together. |
| `pom.xml` | Targets Java 17 while the workspace standard is Java 21+. CI builds both. |

## Conventions

- **British English** throughout, in code and prose. The codebase already uses
  `colour` / `colours` — match it; never introduce `color`.
- **Tabs** for indentation, matching every existing source file.
- Package is `mikera.life`; keep new classes there unless there is a reason not to.
- Match the surrounding style: this is terse, allocation-light, array-oriented
  code. Do not "modernise" the hot path in `Engine` into streams or boxed types —
  the lookup-table design is the whole point.
- Prefer editing existing files over adding new ones.

## Workflow

1. Read the affected source before changing it — the engine's invariants are not
   obvious from any single method.
2. Run `mvn clean install` after changes; CI runs the same on JDK 17 and 21.
3. Check the repo's git identity before committing —
   `git config user.name` / `user.email`. It is currently the global `mikera`
   identity, so agent commits must be attributed deliberately (see the workspace
   `AGENTS.md`).
4. Use `gh` as `brittleboye` for GitHub operations unless told otherwise.
