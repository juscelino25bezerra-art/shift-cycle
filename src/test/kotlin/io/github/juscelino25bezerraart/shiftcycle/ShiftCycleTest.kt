// shift-cycle — Copyright (C) 2026 Juscelino Be
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.juscelino25bezerraart.shiftcycle

import io.github.juscelino25bezerraart.shiftcycle.ShiftCycle.Lang
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ported from the Kalendrium engine tests (EngineTest, AuditTest) plus library-specific cases. */
class ShiftCycleTest {
    private fun d(s: String) = ShiftCycle.parse(s)
    private fun sch(blocks: List<Block>, anchor: String = "2026-01-01", changes: List<Change> = emptyList()) =
        Schedule(anchor, blocks, changes)

    // ---------- status ----------
    @Test fun `composite 28-day cycle repeats across months and years`() {
        val blocks = listOf(Block(4, 2), Block(4, 1), Block(4, 2), Block(4, 1), Block(5, 1))
        assertEquals(28, ShiftCycle.cycleLen(blocks))
        val s = sch(blocks)
        val d0 = d("2026-01-01")
        for (i in 0L until 400L) {
            assertEquals(ShiftCycle.status(d0.plusDays(i), s).idx, ShiftCycle.status(d0.plusDays(i + 28), s).idx)
        }
        assertTrue(ShiftCycle.status(d0, s).work)
        assertFalse(ShiftCycle.status(d0.plusDays(4), s).work)
        assertEquals(27, ShiftCycle.status(d0.minusDays(1), s).idx) // projected before the anchor too
    }

    @Test fun `a change only affects days from its date on`() {
        val s = sch(listOf(Block(4, 1)), changes = listOf(Change("2026-01-10", 4)))
        assertEquals(ShiftCycle.status(d("2026-01-09"), s.copy(changes = emptyList())).idx, ShiftCycle.status(d("2026-01-09"), s).idx)
        assertEquals(4, ShiftCycle.status(d("2026-01-10"), s).idx)
        assertEquals(0, ShiftCycle.status(d("2026-01-11"), s).idx)
    }

    @Test fun `preserved era still applies before its limit`() {
        val s = Schedule(
            anchor = "2026-06-01", blocks = listOf(Block(4, 1)),
            history = listOf(Era("2026-09-01", "2026-01-01", listOf(Block(5, 2)), emptyList())),
        )
        assertEquals("2026-09-01", ShiftCycle.lastTo(s))
        assertEquals(5, ShiftCycle.status(d("2026-01-06"), s).block.w)
        assertEquals(4, ShiftCycle.status(d("2026-09-01"), s).block.w)
    }

    @Test fun `anchor on February 29 and cycles crossing the year`() {
        val s = sch(listOf(Block(4, 1)), anchor = "2028-02-29")
        assertTrue(ShiftCycle.status(d("2028-02-29"), s).work)
        assertFalse(ShiftCycle.status(d("2028-03-04"), s).work) // 5th day = off
        assertTrue(ShiftCycle.status(d("2028-03-05"), s).work)
        assertFalse(ShiftCycle.status(d("2028-02-28"), s).work) // before the anchor
        assertEquals(0, ShiftCycle.status(d("2027-01-01"), sch(listOf(Block(4, 1)))).idx) // 365 % 5 = 0
    }

    @Test fun `change on the anchor day and consecutive eras`() {
        val s = sch(listOf(Block(4, 1)), changes = listOf(Change("2026-01-01", 4)))
        assertFalse(ShiftCycle.status(d("2026-01-01"), s).work)
        val eras = s.copy(
            anchor = "2026-03-01",
            history = listOf(
                Era("2026-02-01", "2026-01-01", listOf(Block(1, 1)), emptyList()),
                Era("2026-03-01", "2026-02-01", listOf(Block(2, 2)), emptyList()),
            ),
            changes = emptyList(),
        )
        assertTrue(ShiftCycle.status(d("2026-01-01"), eras).work)
        assertFalse(ShiftCycle.status(d("2026-01-02"), eras).work)  // 1×1
        assertTrue(ShiftCycle.status(d("2026-02-02"), eras).work)   // 2×2: 2nd work day
        assertFalse(ShiftCycle.status(d("2026-02-03"), eras).work)
        assertTrue(ShiftCycle.status(d("2026-03-01"), eras).work)   // 4×1 starts at the anchor
        assertEquals(listOf("1×1", "2×2", "4×1"), ShiftCycle.cyclesIn("2026-01-15", "2026-03-15", eras).map { ShiftCycle.cycleText(it) })
        assertEquals(listOf("4×1"), ShiftCycle.cyclesIn("2026-04-01", "2026-12-31", eras).map { ShiftCycle.cycleText(it) })
        assertEquals("1×1 → 2×2", ShiftCycle.cyclesText("2026-01-15", "2026-02-15", eras))
    }

    // ---------- remapChanges ----------
    @Test fun `removing a middle block does not reassign its change to another block`() {
        val ob = listOf(Block(4, 2), Block(4, 1), Block(5, 1))
        val nb = listOf(Block(4, 2), Block(5, 1))
        val ps = ShiftCycle.positions(ob)
        val inB1 = ps.first { it.bi == 1 }.idx
        val inB2 = ps.first { it.bi == 2 }.idx
        val r = ShiftCycle.remapChanges(ob, nb, listOf(Change("2026-10-10", inB1), Change("2026-10-11", inB2)), listOf(0, 2))
        assertEquals(1, r.dropped)
        assertEquals(1, r.changes.size)
        assertEquals(1, ShiftCycle.positions(nb)[r.changes[0].pos].bi)
    }

    @Test fun `without origs the index is kept`() {
        val b = listOf(Block(4, 1))
        assertEquals(listOf(Change("2026-01-01", 2)), ShiftCycle.remapChanges(b, b, listOf(Change("2026-01-01", 2))).changes)
    }

    @Test fun `shrinking work keeps the change on a work day`() {
        val r = ShiftCycle.remapChanges(listOf(Block(4, 1)), listOf(Block(2, 1)), listOf(Change("2026-03-01", 2)), listOf(0))
        val p = ShiftCycle.positions(listOf(Block(2, 1)))[r.changes.single().pos]
        assertTrue(p.work)
        assertEquals(2, p.n)
        assertEquals(1, r.moved)
    }

    @Test fun `shrinking days off keeps a day off`() {
        val r = ShiftCycle.remapChanges(listOf(Block(4, 3)), listOf(Block(4, 1)), listOf(Change("2026-03-01", 6)), listOf(0))
        val p = ShiftCycle.positions(listOf(Block(4, 1)))[r.changes.single().pos]
        assertFalse(p.work)
        assertEquals(1, p.n)
    }

    @Test fun `growing work does not turn a day off into work`() {
        val nb = listOf(Block(5, 2))
        val r = ShiftCycle.remapChanges(listOf(Block(2, 2)), nb, listOf(Change("2026-03-01", 2), Change("2026-03-05", 1)), listOf(0))
        val ps = ShiftCycle.positions(nb)
        assertEquals(listOf(false to 1, true to 2), r.changes.map { ps[it.pos].work to ps[it.pos].n })
    }

    @Test fun `reordered blocks and a new block`() {
        val ob = listOf(Block(4, 1), Block(2, 2))
        val nb = listOf(Block(3, 3), Block(2, 2), Block(4, 1))
        val c = listOf(Change("2026-03-01", ShiftCycle.positions(ob).indexOfFirst { it.bi == 1 && !it.work }))
        val r = ShiftCycle.remapChanges(ob, nb, c, listOf(-1, 1, 0))
        val p = ShiftCycle.positions(nb)[r.changes.single().pos]
        assertEquals(1, p.bi)
        assertFalse(p.work)
        assertEquals(1, p.n)
        assertEquals(0, r.dropped)
    }

    @Test fun `a change with no real difference is not counted as moved`() {
        val r = ShiftCycle.remapChanges(listOf(Block(4, 1)), listOf(Block(4, 1, "Day")), listOf(Change("2026-03-01", 3)), listOf(0))
        assertEquals(0, r.moved)
        assertEquals(3, r.changes.single().pos)
    }

    // ---------- applyCycleEdit ----------
    @Test fun `keeping previous days leaves the past identical and the new cycle starts on day 1`() {
        val old = sch(listOf(Block(4, 1)), changes = listOf(Change("2026-06-10", 2), Change("2026-12-01", 0)))
        val from = "2026-11-01"
        val s = ShiftCycle.applyCycleEdit(old, listOf(Block(5, 2)), anchor = from, keep = true, from = from, origs = listOf(0)).schedule
        var day = d(from).minusYears(2)
        while (day < d(from)) {
            assertEquals(ShiftCycle.status(day, old).work, ShiftCycle.status(day, s).work)
            assertEquals(ShiftCycle.status(day, old).idx, ShiftCycle.status(day, s).idx)
            day = day.plusDays(1)
        }
        val first = ShiftCycle.status(d(from), s)
        assertTrue(first.work)
        assertEquals(1, first.n)
        assertEquals(listOf(Change("2026-06-10", 2)), s.history.single().changes)
        assertEquals(listOf("2026-12-01"), s.changes.map { it.date })
    }

    @Test fun `without keep everything is recalculated and history cleared`() {
        val old = sch(listOf(Block(4, 1))).copy(history = listOf(Era("2026-01-01", "2025-01-01", listOf(Block(2, 2)), emptyList())))
        assertTrue(ShiftCycle.applyCycleEdit(old, listOf(Block(5, 2)), "2026-01-01", keep = false, from = "", origs = listOf(0)).schedule.history.isEmpty())
    }

    // ---------- helpers ----------
    @Test fun `next switch and next work day`() {
        val s = sch(listOf(Block(4, 1)))
        assertEquals(d("2026-01-05") to 4, ShiftCycle.nextSwitch(d("2026-01-01"), s))
        assertEquals(d("2026-01-06"), ShiftCycle.nextWorkAfter(d("2026-01-04"), s))
    }

    @Test fun `periods on a date and upcoming`() {
        val s = sch(listOf(Block(4, 1))).copy(
            periods = listOf(Period("a", "2026-07-01", "2026-07-30", "Vacation"), Period("b", "2026-07-10", "2026-08-05")),
        )
        assertEquals("b", ShiftCycle.periodOn("2026-07-15", s)?.id)
        assertNull(ShiftCycle.periodOn("2026-06-30", s))
        assertEquals("a", ShiftCycle.upcomingPeriod(d("2026-06-20"), s)?.id)
        assertNull(ShiftCycle.upcomingPeriod(d("2026-06-01"), s))
    }

    @Test fun `position labels in three languages`() {
        val b = Block(4, 2, "Night")
        assertEquals("2nd work day · block 4×2 · Night", ShiftCycle.posLabel(true, 2, b))
        assertEquals("2º trabalho · bloco 4×2 · Night", ShiftCycle.posLabel(true, 2, b, Lang.PT))
        assertEquals("1º libre · bloque 4×2 · Night", ShiftCycle.posLabel(false, 1, b, Lang.ES))
        assertEquals("11th day off · block 4×2 · Night", ShiftCycle.posLabel(false, 11, b))
    }

    @Test fun `dates and invalid blocks`() {
        assertFalse(ShiftCycle.isDate("0050-01-01"))
        assertFalse(ShiftCycle.isDate("2026-02-30"))
        assertTrue(ShiftCycle.isDate("2028-02-29"))
        assertEquals("2026-01-05", ShiftCycle.iso(LocalDate.of(2026, 1, 5)))
        assertFailsWith<IllegalArgumentException> { Block(0, 1) }
        assertFailsWith<IllegalArgumentException> { ShiftCycle.status(d("2026-01-01"), sch(emptyList())) }
    }
}
