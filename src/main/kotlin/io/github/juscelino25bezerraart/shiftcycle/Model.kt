// shift-cycle — Copyright (C) 2026 Juscelino Be
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.juscelino25bezerraart.shiftcycle

/**
 * A block of [w] work days followed by [o] days off ("4×1", "12×36" as 1×1…).
 * A cycle is one or more blocks in order; [name] is optional ("Night", "Day").
 */
data class Block(val w: Int, val o: Int, val name: String = "") {
    init {
        // same rule as the Kalendrium app: every block has at least 1 work day and 1 day off
        require(w >= 1 && o >= 1) { "a block needs at least 1 work day and 1 day off (got ${w}×$o)" }
    }
}

/** From [date] (ISO "YYYY-MM-DD") on, the cycle restarts at position [pos] (0-based index in the cycle). */
data class Change(val date: String, val pos: Int)

/** A cycle configuration: reference date ([anchor] = position 0), blocks and changes. */
interface Cfg {
    val anchor: String
    val blocks: List<Block>
    val changes: List<Change>
}

/**
 * A preserved era: the configuration that applied before [to] (exclusive).
 * Created by [ShiftCycle.applyCycleEdit] with `keep = true`, so past days keep their old status.
 */
data class Era(
    val to: String,
    override val anchor: String,
    override val blocks: List<Block>,
    override val changes: List<Change>,
) : Cfg

/** A period such as vacation or leave, from [start] to [end] (inclusive, ISO dates). */
data class Period(
    val id: String,
    val start: String,
    val end: String,
    val title: String = "",
)

/**
 * A work schedule: the current cycle ([anchor], [blocks], [changes]), the preserved eras in [history]
 * (oldest first) and optional [periods].
 */
data class Schedule(
    override val anchor: String,
    override val blocks: List<Block>,
    override val changes: List<Change> = emptyList(),
    val history: List<Era> = emptyList(),
    val periods: List<Period> = emptyList(),
) : Cfg

/** Position of a day inside the cycle. [n] is 1-based within the work or off part of block [bi]. */
data class Pos(val idx: Int, val bi: Int, val work: Boolean, val n: Int, val block: Block)

/** Status of a calendar day: work or off, which block, cycle index and day number inside the block. */
data class DayStatus(val work: Boolean, val bi: Int, val block: Block, val idx: Int, val n: Int)

/** Result of [ShiftCycle.remapChanges]: repositioned changes and how many were dropped or moved. */
data class RemapResult(val changes: List<Change>, val dropped: Int, val moved: Int)

/** Schedule after editing its blocks, with how many changes were moved or dropped. */
data class CycleEdit(val schedule: Schedule, val moved: Int, val dropped: Int)
