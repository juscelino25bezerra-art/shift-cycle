// shift-cycle — Copyright (C) 2026 Juscelino Be
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.juscelino25bezerraart.shiftcycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

/**
 * Cycle engine for rotating work schedules. Dates are ISO strings ("YYYY-MM-DD"); functions that
 * walk the calendar take [LocalDate]. Same algorithm as the Kalendrium app and the Minha Escala PWA.
 */
object ShiftCycle {
    /** Total length of the cycle in days. */
    fun cycleLen(blocks: List<Block>): Int = blocks.sumOf { it.w + it.o }

    private val posCache = LinkedHashMap<List<Block>, List<Pos>>()

    /** Every position of the cycle, in order: the work days of block 0, its days off, then block 1… */
    fun positions(blocks: List<Block>): List<Pos> {
        synchronized(posCache) { posCache[blocks]?.let { return it } }
        val a = ArrayList<Pos>(cycleLen(blocks))
        var i = 0
        blocks.forEachIndexed { bi, b ->
            for (x in 0 until b.w) a.add(Pos(i++, bi, true, x + 1, b))
            for (x in 0 until b.o) a.add(Pos(i++, bi, false, x + 1, b))
        }
        synchronized(posCache) {
            if (posCache.size > 20) posCache.clear()
            posCache[blocks.toList()] = a
        }
        return a
    }

    /** Configuration that applies on date [s]: the preserved era that covers it, or the current one. */
    fun cfgFor(s: String, sch: Schedule): Cfg {
        for (e in sch.history) if (s < e.to) return e
        return sch
    }

    private data class Origin(val date: String, val pos: Int)

    private fun originFor(s: String, cfg: Cfg): Origin {
        var origin = Origin(cfg.anchor, 0)
        for (c in cfg.changes.sortedBy { it.date }) {
            if (c.date <= s) origin = Origin(c.date, c.pos) else break
        }
        return origin
    }

    /** Status of [date]: work or off, block, cycle index and day number. Works before the anchor too. */
    fun status(date: LocalDate, sch: Schedule): DayStatus {
        val s = iso(date)
        val cfg = cfgFor(s, sch)
        val len = cycleLen(cfg.blocks)
        require(len > 0) { "the cycle has no blocks" }
        val origin = originFor(s, cfg)
        val idx = mod(origin.pos + ChronoUnit.DAYS.between(parse(origin.date), date), len)
        val p = positions(cfg.blocks)[idx]
        return DayStatus(p.work, p.bi, cfg.blocks[p.bi], idx, p.n)
    }

    /** "4×1 · 5×2 (Night)" */
    fun cycleText(blocks: List<Block>): String =
        blocks.joinToString(" · ") { if (it.name.isNotEmpty()) "${it.w}×${it.o} (${it.name})" else "${it.w}×${it.o}" }

    /** End date (exclusive) of the last preserved era, or "" when there is none. */
    fun lastTo(sch: Schedule): String = sch.history.lastOrNull()?.to ?: ""

    /**
     * Repositions changes by (block, day inside the block) when the blocks are edited.
     * `origs[i]` = index of the old block that the new block i came from (-1 = new block).
     * Work stays work and off stays off: a work day that no longer exists becomes the last work day
     * of the new block (same for days off). Every change whose day label changed counts as "moved"
     * ("3rd work day" → "2nd work day"), even if the position number is the same.
     */
    fun remapChanges(ob: List<Block>, nb: List<Block>, changes: List<Change>, origs: List<Int>? = null): RemapResult {
        fun starts(b: List<Block>): List<Int> {
            var n = 0
            return b.map { x -> val s = n; n += x.w + x.o; s }
        }
        val op = positions(ob)
        val np = positions(nb)
        val os = starts(ob)
        val ns = starts(nb)
        val map = HashMap<Int, Int>()
        (origs ?: nb.indices.toList()).forEachIndexed { ni, o -> if (o >= 0 && o < ob.size) map[o] = ni }
        var dropped = 0
        var moved = 0
        val out = ArrayList<Change>()
        for (c in changes) {
            val p = op.getOrNull(c.pos)
            val ni = p?.let { map[it.bi] }
            if (p == null || ni == null) { dropped++; continue }
            val nbk = nb[ni]
            val k = c.pos - os[p.bi]           // day inside the old block (0-based)
            val inBlock = if (p.work) min(k, nbk.w - 1) else nbk.w + min(k - ob[p.bi].w, nbk.o - 1)
            val pos = ns[ni] + inBlock
            if (pos != c.pos || np[pos].work != p.work || np[pos].n != p.n) moved++
            out.add(Change(c.date, pos))
        }
        return RemapResult(out, dropped, moved)
    }

    /** Next switch between work and off after [today]: the date and how many days away. */
    fun nextSwitch(today: LocalDate, sch: Schedule): Pair<LocalDate, Int>? {
        val st = status(today, sch)
        val limit = maxOf(62, cycleLen(sch.blocks) * 2)
        for (i in 1..limit) {
            val d = today.plusDays(i.toLong())
            if (status(d, sch).work != st.work) return d to i
        }
        return null
    }

    /** First work day after [date] (null if there is none within two cycles). */
    fun nextWorkAfter(date: LocalDate, sch: Schedule): LocalDate? {
        val limit = maxOf(62, cycleLen(sch.blocks) * 2)
        for (i in 1..limit) {
            val d = date.plusDays(i.toLong())
            if (status(d, sch).work) return d
        }
        return null
    }

    /** Period that includes [date]; if more than one, the one that ends last. */
    fun periodOn(date: String, sch: Schedule): Period? =
        sch.periods.filter { date >= it.start && date <= it.end }.maxByOrNull { it.end }

    /** Next period that starts after [date] and within [days] days. */
    fun upcomingPeriod(date: LocalDate, sch: Schedule, days: Long = 14): Period? {
        val from = iso(date)
        val to = iso(date.plusDays(days))
        return sch.periods.filter { it.start > from && it.start <= to }.minByOrNull { it.start }
    }

    /**
     * Cycles that apply on some day of [start, end], oldest first
     * (a preserved era covers from the previous era's `to` up to its own `to`, exclusive).
     */
    fun cyclesIn(start: String, end: String, sch: Schedule): List<List<Block>> {
        val out = ArrayList<List<Block>>()
        var from = ""
        for (e in sch.history) {
            if (start < e.to && end >= from) out.add(e.blocks)
            from = e.to
        }
        if (end >= from) out.add(sch.blocks)
        return out.distinct()
    }

    /** "4×1", or "4×1 → 5×2" when the schedule changed inside the range. */
    fun cyclesText(start: String, end: String, sch: Schedule): String =
        cyclesIn(start, end, sch).joinToString(" → ") { cycleText(it) }.ifEmpty { cycleText(sch.blocks) }

    /**
     * Edits the cycle: replaces blocks and anchor and repositions the changes.
     * keep = true ("keep previous days"): what applied before [from] becomes a preserved era (with only
     * the changes before [from]) and the changes from [from] on are repositioned in the new blocks.
     * keep = false: every change is repositioned and the history is discarded.
     */
    fun applyCycleEdit(sc: Schedule, blocks: List<Block>, anchor: String, keep: Boolean, from: String, origs: List<Int>?): CycleEdit {
        val r = remapChanges(sc.blocks, blocks, if (keep) sc.changes.filter { it.date >= from } else sc.changes, origs)
        val history = if (keep) sc.history + Era(from, sc.anchor, sc.blocks, sc.changes.filter { it.date < from }) else emptyList()
        return CycleEdit(sc.copy(blocks = blocks, anchor = anchor, changes = r.changes, history = history), r.moved, r.dropped)
    }

    /** Label of a position: "2º trabalho · bloco 4×2 · Noite", "2nd work day · block 4×2 · Night"… */
    fun posLabel(work: Boolean, n: Int, b: Block, lang: Lang = Lang.EN): String {
        val nm = if (b.name.isNotEmpty()) " · ${b.name}" else ""
        val day = when (lang) {
            Lang.PT -> if (work) "${n}º trabalho" else "${n}º folga"
            Lang.ES -> if (work) "${n}º trabajo" else "${n}º libre"
            Lang.EN -> if (work) "${ordinal(n)} work day" else "${ordinal(n)} day off"
        }
        val block = when (lang) { Lang.PT -> "bloco"; Lang.ES -> "bloque"; Lang.EN -> "block" }
        return "$day · $block ${b.w}×${b.o}$nm"
    }

    /** Label languages for [posLabel]. */
    enum class Lang { PT, EN, ES }

    private fun ordinal(n: Int): String = n.toString() + when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }

    // ---------- dates ----------
    private val ISO_RE = Regex("""^\d{4}-\d{2}-\d{2}$""")

    /** ISO date "YYYY-MM-DD". */
    fun iso(d: LocalDate): String =
        d.year.toString().padStart(4, '0') + "-" + d.monthValue.toString().padStart(2, '0') + "-" + d.dayOfMonth.toString().padStart(2, '0')

    fun parse(s: String): LocalDate {
        val (y, m, d) = s.split("-").map { it.toInt() }
        return LocalDate.of(y, m, d)
    }

    /** True for a real date in "YYYY-MM-DD" with year ≥ 100. */
    fun isDate(s: String?): Boolean {
        if (s == null || !ISO_RE.matches(s)) return false
        return try {
            val d = parse(s)
            d.year >= 100 && iso(d) == s
        } catch (_: Exception) {
            false
        }
    }

    private fun mod(n: Long, m: Int): Int = (((n % m) + m) % m).toInt()
}
