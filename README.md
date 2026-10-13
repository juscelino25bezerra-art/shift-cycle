# shift-cycle

Kotlin engine for rotating work schedules (4×1, 12×36, 5×2…). Combine named blocks into one cycle, shift the cycle on specific dates, and edit it later while keeping past days intact. No dependencies besides the Kotlin standard library; works offline.

It is the schedule engine of the [Kalendrium](https://github.com/juscelino25bezerra-art) Android app, extracted as a standalone library.

*Em português: [veja abaixo](#em-português).*

## What it does

- **Cycles made of blocks.** A block is *w* work days followed by *o* days off. A cycle can mix several blocks, each with an optional name: `4×2 · 4×1 (Night) · 5×1` is a 28-day cycle.
- **Status of any day**, before or after the reference date: work or off, which block, the position in the cycle and the day number ("2nd work day · block 4×1 · Night").
- **Changes.** From a given date on, the cycle restarts at a chosen position (a swapped shift, a return from leave…). Days before that date stay as they were.
- **Editing the cycle without rewriting the past.** When the blocks change, `applyCycleEdit` can keep what applied before a date as a preserved *era*, and moves existing changes to the matching day in the new blocks: work stays work, off stays off, and a day that no longer exists becomes the last day of the same kind.
- **Periods** (vacation, leave): which period covers a date and which one starts soon.
- **Ranges**: which cycles apply in a date range ("1×1 → 2×2") and the next switch between work and off.

## Example

```kotlin
import io.github.juscelino25bezerraart.shiftcycle.*
import java.time.LocalDate

// 4 work days, 1 off, starting on 1 Jan 2026
val schedule = Schedule(anchor = "2026-01-01", blocks = listOf(Block(4, 1)))

val day = ShiftCycle.status(LocalDate.of(2026, 1, 5), schedule)
day.work                                              // false: 5th day of the cycle is off
ShiftCycle.posLabel(day.work, day.n, day.block)       // "1st day off · block 4×1"
ShiftCycle.nextSwitch(LocalDate.of(2026, 1, 1), schedule) // (2026-01-05, 4)

// From 10 Jan, restart the cycle at position 4 (a day off)
val swapped = schedule.copy(changes = listOf(Change("2026-01-10", 4)))

// From 1 Nov, switch to 5×2 but keep every earlier day exactly as it was
val edit = ShiftCycle.applyCycleEdit(
    swapped, blocks = listOf(Block(5, 2)), anchor = "2026-11-01",
    keep = true, from = "2026-11-01", origs = listOf(0),
)
edit.schedule.history                                 // one preserved era up to 2026-11-01
ShiftCycle.cyclesText("2026-10-01", "2026-11-30", edit.schedule) // "4×1 → 5×2"
```

Dates in the model are ISO strings (`"YYYY-MM-DD"`), the same format the apps store; functions that walk the calendar take `java.time.LocalDate`.

## API

| Function | What it returns |
|---|---|
| `status(date, schedule)` | `DayStatus`: work or off, block, cycle index, day number |
| `positions(blocks)`, `cycleLen(blocks)` | Every position of the cycle; its length in days |
| `remapChanges(oldBlocks, newBlocks, changes, origs)` | Changes moved to the new blocks, with counts of moved and dropped |
| `applyCycleEdit(schedule, blocks, anchor, keep, from, origs)` | Edited schedule; with `keep`, a preserved era before `from` |
| `cfgFor(date, schedule)`, `lastTo(schedule)` | Configuration that applies on a date; end of the last era |
| `cyclesIn(start, end, schedule)`, `cyclesText(...)`, `cycleText(blocks)` | Cycles in a range and their text ("4×1 → 5×2") |
| `nextSwitch(today, schedule)`, `nextWorkAfter(date, schedule)` | Next work/off switch; next work day |
| `periodOn(date, schedule)`, `upcomingPeriod(date, schedule, days)` | Period on a date; next period within `days` |
| `posLabel(work, n, block, lang)` | Label in English, Portuguese or Spanish |
| `iso(date)`, `parse(text)`, `isDate(text)` | ISO date helpers |

A `Block` needs at least 1 work day and 1 day off. `status` and the other functions are pure and thread-safe.

## Installation

Not yet published to a Maven repository. Two options for now:

- **Gradle included build:** add this repository as a Git submodule (for example in `libs/shift-cycle`), put `includeBuild("libs/shift-cycle")` in your `settings.gradle.kts` and depend on `io.github.juscelino25bezerra-art:shift-cycle:1.0.0`.
- **Copy the source:** the library is two files in `src/main/kotlin`. If you copy them, keep the copyright and license headers (see *License*).

Requirements: JVM 11+ (built with JDK 17). On Android, `java.time` needs API 26+, or core library desugaring on older versions.

## Build and test

```sh
./gradlew build        # compiles and runs the tests
```

The tests are ported from the Kalendrium app and cover composite cycles, changes, preserved eras, cycle edits, leap days and year boundaries.

## License

Copyright (C) 2026 Juscelino Be.

shift-cycle is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version** (`GPL-3.0-or-later`). Full text in [LICENSE](LICENSE).

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

What this means for you: you can use, study, change and share it. If you distribute an app that includes shift-cycle, the app as a whole must be distributed under the GPL-3.0-or-later too, with its source code available to the people who receive it.

## Credits

- **Author:** Juscelino Be ([@juscelino25bezerra-art](https://github.com/juscelino25bezerra-art)). The algorithm comes from the author's own apps: the Minha Escala PWA and its native successor, Kalendrium.
- **Third-party components** (Kotlin standard library, test and build tools): see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Contributing

Issues and pull requests are welcome. By contributing, you agree to license your contribution under the GPL-3.0-or-later. Please don't add runtime dependencies; if one is truly needed, record its license in `THIRD_PARTY_NOTICES.md` (the CI fails on new runtime dependencies).

---

## Em português

Motor em Kotlin para escalas de trabalho (4×1, 12×36, 5×2…). Combina blocos num ciclo, aceita mudanças por data e permite editar a escala mantendo os dias anteriores. É o motor de escala do app Kalendrium, separado como biblioteca. Sem dependências além da biblioteca padrão do Kotlin; funciona offline.

- **Blocos:** *w* dias de trabalho seguidos de *o* folgas; um ciclo pode combinar vários, com nome.
- **Situação de qualquer dia:** trabalho ou folga, bloco, posição e rótulo ("2º trabalho · bloco 4×1 · Noite"), também antes da data de referência.
- **Mudanças:** a partir de uma data, o ciclo recomeça numa posição escolhida (troca de plantão, volta de afastamento).
- **Editar a escala sem reescrever o passado:** `applyCycleEdit` guarda o que valia antes como uma era preservada e reposiciona as mudanças já marcadas (trabalho continua trabalho, folga continua folga).
- **Períodos** (férias, licenças) e **intervalos** ("1×1 → 2×2", próxima troca trabalho↔folga).

Licença: **GPL-3.0-or-later**, Copyright (C) 2026 Juscelino Be. Quem distribuir um app que use o shift-cycle precisa distribuí-lo também sob a GPL-3.0-or-later, com o código-fonte. Componentes de terceiros: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
