package com.heavencanthandle.lakewords

import com.heavencanthandle.lakewords.intro.TaglinePage
import com.heavencanthandle.lakewords.intro.Shore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoreTest {

    private val w = 120
    private val h = 200

    /** A bar standing in for the words, rows 80..90, columns 30..90. */
    private val title = BooleanArray(w * h) { i -> i / w in 80..90 && i % w in 30..90 }

    /** A bar standing in for the bird's bones, low down: rows 170..175, columns 20..100. */
    private val bones = BooleanArray(w * h) { i -> i / w in 170..175 && i % w in 20..100 }

    private fun shore(seed: Int = 7) = Shore(w, h, title, 90, bones, seed)

    private fun runToEnd(s: Shore, each: (Shore) -> Unit = {}) {
        var guard = 0
        while (!s.finished && guard++ < 10_000) {
            s.step()
            each(s)
        }
    }

    @Test
    fun theWaterStartsHighAndIsGoneByTheEnd() {
        val s = shore()
        assertTrue(s.isWater(w / 2, h - 1))
        assertFalse(s.isWater(w / 2, 0))
        runToEnd(s)
        for (x in 0 until w) assertFalse(s.isWater(x, h - 1))
    }

    @Test
    fun saltOnlyGrowsOnDryGroundAndKeepsOffTheWords() {
        val s = shore()
        runToEnd(s) { now ->
            for (y in 0 until h) for (x in 0 until w) {
                if (!now.isSalt(x, y)) continue
                assertFalse("salt in the water at $x,$y frame ${now.frame}", now.isWater(x, y))
                assertFalse("salt on the words at $x,$y", title[y * w + x])
            }
        }
        assertTrue("some salt grew", s.saltCount() > 0)
    }

    @Test
    fun theWordsDarkenOnlyOnceTheWaterIsPastTheMiddleAndEndFullyDark() {
        val s = shore()
        var last = 0f
        runToEnd(s) { now ->
            val ink = now.titleInk
            assertTrue("never lighter again", ink >= last)
            last = ink
        }
        assertEquals(1f, s.titleInk, 0f)
        // At the start, with the water near the top, nothing of the words shows.
        assertEquals(0f, shore().titleInk, 0f)
    }

    @Test
    fun itFinishesOnTime() {
        val s = shore()
        runToEnd(s)
        assertTrue(s.finished)
        assertEquals((Shore.SALT_END_S * Shore.FPS).toInt(), s.finishedAt)
    }

    @Test
    fun theSameSeedGrowsTheSameSalt() {
        val a = shore(seed = 3)
        val b = shore(seed = 3)
        runToEnd(a)
        runToEnd(b)
        assertEquals(a.saltCount(), b.saltCount())
        val pa = BooleanArray(w * h).also(a::render)
        val pb = BooleanArray(w * h).also(b::render)
        assertTrue(pa.contentEquals(pb))
    }

    @Test
    fun theBonesShowOnlyOnceDryAndThenFaintAndPartlyUnderSand() {
        val s = shore()
        assertFalse("under water at the start", s.showsBone(50, 172))
        runToEnd(s) { now ->
            for (y in 170..175) for (x in 20..100) {
                assertFalse("salt on the bones at $x,$y", now.isSalt(x, y))
                if (now.showsBone(x, y)) assertFalse(now.isWater(x, y))
            }
        }
        var showing = 0
        for (y in 170..175) for (x in 20..100) if (s.showsBone(x, y)) showing++
        val all = 6 * 81
        assertTrue("some of the bones show once dry", showing > 0)
        assertTrue("the sand keeps much of them, and the rest is faint", showing < all * 0.6)
    }

    @Test
    fun theTaglinePageAddsItsPiecesInOrderAndNeverTakesInkAway() {
        val tagline = BooleanArray(w * h) { it == 5 }
        val knot = BooleanArray(w * h) { it == 6 }
        val skull = BooleanArray(w * h) { it == 7 }
        val page = TaglinePage(w, h, listOf(tagline, knot, skull))
        val picture = BooleanArray(w * h)
        page.render(picture)
        assertTrue(picture[5])
        assertFalse(picture[6])
        page.showNext()
        page.render(picture)
        assertTrue(picture[5] && picture[6])
        assertFalse(page.finished)
        page.showNext()
        page.render(picture)
        assertTrue(picture[5] && picture[6] && picture[7])
        assertTrue(page.finished)
        assertEquals(3, picture.count { it })
    }
}