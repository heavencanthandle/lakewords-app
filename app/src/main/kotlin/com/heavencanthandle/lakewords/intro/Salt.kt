package com.heavencanthandle.lakewords.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Salt crystals growing on a grid of cells, [width] x [height], row by row. Two kinds:
 *
 * - **Dendrites**, by diffusion-limited aggregation, the way salt comes out of drying brine:
 *   wanderers start on a ring just outside a crystal and step at random until they come to
 *   rest beside it. A cell joins only after [STICK] wanderers have rested there, which smooths
 *   the noise into long arms, four of them, square to each other, with side branches.
 * - **Hoppers**, the hollow, stepped squares halite makes when its edges grow faster than its
 *   faces: from above, square frames one inside another, joined at the corners. Each grows
 *   outward a frame at a time.
 *
 * Salt only ever adds cells, never takes them away. [keepOut] is never salted; [allowed] says
 * where else salt may go this frame (dry ground, say).
 */
class Salt(
    val width: Int,
    val height: Int,
    private val keepOut: BooleanArray,
    private val random: Random,
    private val allowed: (x: Int, y: Int) -> Boolean,
) {
    private val salt = BooleanArray(width * height)
    private var count = 0

    /** How often a wanderer has come to rest beside each free cell. */
    private val hits = ByteArray(width * height)

    private sealed class Crystal(val x: Int, val y: Int)

    private class Dendrite(x: Int, y: Int, val most: Int) : Crystal(x, y) {
        var reach = 1f
        var size = 1
    }

    private class Hopper(x: Int, y: Int, val half: Int) : Crystal(x, y) {
        var grown = 0
        var wait = 0
    }

    private val crystals = ArrayList<Crystal>()

    val size: Int get() = count

    val crystalCount: Int get() = crystals.size

    fun isSalt(x: Int, y: Int): Boolean = salt[y * width + x]

    fun isSalt(i: Int): Boolean = salt[i]

    /** Whether a crystal already starts within [distance] cells of (x, y). */
    fun crowded(x: Int, y: Int, distance: Int): Boolean =
        crystals.any { sq(it.x - x) + sq(it.y - y) < distance * distance }

    fun free(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        val i = y * width + x
        return !salt[i] && !keepOut[i] && allowed(x, y)
    }

    /** A branching crystal at (x, y), to grow to about [most] cells. False if there is no room. */
    fun dendrite(x: Int, y: Int, most: IntRange): Boolean {
        if (!free(x, y)) return false
        add(x, y)
        crystals += Dendrite(x, y, pick(most))
        return true
    }

    /** A hopper square centred on (x, y), to grow to [half] cells either side. */
    fun hopper(x: Int, y: Int, half: IntRange): Boolean {
        if (!free(x, y)) return false
        add(x, y)
        crystals += Hopper(x, y, pick(half))
        return true
    }

    /** One frame of growth for every crystal still growing. */
    fun grow() {
        for (c in crystals) {
            when (c) {
                is Dendrite -> if (c.size < c.most) repeat(WANDERERS) { wander(c) }
                is Hopper -> stepHopper(c)
            }
        }
    }

    private fun wander(c: Dendrite) {
        val launch = c.reach + 3f
        val lost = sq(c.reach + 10f)
        val a = random.nextFloat() * 2f * PI.toFloat()
        var x = (c.x + launch * cos(a)).toInt()
        var y = (c.y + launch * sin(a)).toInt()
        repeat(STEPS) {
            if (!free(x, y)) return
            if (touchesSalt(x, y)) {
                val i = y * width + x
                hits[i] = (hits[i] + 1).toByte()
                if (hits[i] >= STICK) {
                    add(x, y)
                    c.size++
                    c.reach = max(c.reach, sqrt((sq(x - c.x) + sq(y - c.y)).toFloat()))
                }
                return
            }
            val d = random.nextInt(4)
            x += DX[d]
            y += DY[d]
            if (sq(x - c.x) + sq(y - c.y) > lost) return
        }
    }

    /**
     * A hopper adds one ring of its square every few frames. Rings [HOPPER_STEP] apart are
     * drawn whole, the ones between only at the corners' diagonals, so the square reads as
     * stepped frames sinking to a hollow middle.
     */
    private fun stepHopper(c: Hopper) {
        if (c.grown >= c.half) return
        if (c.wait-- > 0) return
        c.wait = HOPPER_WAIT
        c.grown++
        val r = c.grown
        if (r % HOPPER_STEP == 0 || r == c.half) {
            for (t in -r..r) {
                // Now and then a cell of a frame stays open, as real hoppers are never perfect.
                if (random.nextFloat() < 0.04f) continue
                tryAdd(c.x + t, c.y - r)
                tryAdd(c.x + t, c.y + r)
                tryAdd(c.x - r, c.y + t)
                tryAdd(c.x + r, c.y + t)
            }
        } else {
            tryAdd(c.x - r, c.y - r)
            tryAdd(c.x + r, c.y - r)
            tryAdd(c.x - r, c.y + r)
            tryAdd(c.x + r, c.y + r)
        }
    }

    private fun tryAdd(x: Int, y: Int) {
        if (free(x, y)) add(x, y)
    }

    private fun add(x: Int, y: Int) {
        salt[y * width + x] = true
        count++
    }

    private fun touchesSalt(x: Int, y: Int): Boolean =
        (x > 0 && salt[y * width + x - 1]) ||
            (x < width - 1 && salt[y * width + x + 1]) ||
            (y > 0 && salt[(y - 1) * width + x]) ||
            (y < height - 1 && salt[(y + 1) * width + x])

    private fun pick(range: IntRange) = range.first + random.nextInt(range.last - range.first + 1)

    companion object {
        /** Wanderers to rest beside a cell before it joins. More makes longer, cleaner arms. */
        private const val STICK = 3
        private const val WANDERERS = 60
        private const val STEPS = 220

        /** Frames between a hopper's rings, and the rings between its whole frames. */
        private const val HOPPER_WAIT = 2
        private const val HOPPER_STEP = 3

        private val DX = intArrayOf(1, -1, 0, 0)
        private val DY = intArrayOf(0, 0, 1, -1)

        private fun sq(v: Int) = v * v
        private fun sq(v: Float) = v * v

        /** The glyphs and the cell around each: salt grows up to words, never onto them. */
        fun keepOutOf(mask: BooleanArray, width: Int, height: Int): BooleanArray {
            val out = BooleanArray(width * height)
            for (y in 0 until height) for (x in 0 until width) {
                if (!mask[y * width + x]) continue
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height) out[ny * width + nx] = true
                }
            }
            return out
        }

        /** Free cells touching the keep-out on the side [below] says: where salt starts from words. */
        fun edgeOf(keepOut: BooleanArray, width: Int, height: Int, below: Boolean = false): IntArray {
            val edge = ArrayList<Int>()
            for (y in 1 until height - 1) for (x in 1 until width - 1) {
                val i = y * width + x
                if (keepOut[i]) continue
                val touches = if (below) keepOut[i - width]
                else keepOut[i - 1] || keepOut[i + 1] || keepOut[i - width] || keepOut[i + width]
                if (touches) edge += i
            }
            return edge.toIntArray()
        }
    }
}
