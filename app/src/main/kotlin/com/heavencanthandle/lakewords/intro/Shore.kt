package com.heavencanthandle.lakewords.intro

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The opening: a lake shore seen from above, the water drawing back down the screen, the title
 * coming up faintly once the water passes the middle, and salt growing out of the words and
 * over the lake bed behind the water. Pure black and white on a grid of cells.
 *
 * Drawn for e-ink. It moves a few times a second ([FPS]), never more. What changes from one
 * frame to the next is small: the stipple in the water is fixed to the ground, so only the
 * wave lines, the water's edge, and new salt move. Salt only ever adds black, which an e-ink
 * panel draws without ghosting. Shades are dot patterns ([BAYER]), not grey: on a fast panel
 * update grey can come out as black or white.
 *
 * [title] is the words, [width] x [height], true where a glyph is. [titleBottom] is its lowest
 * row: salt starts from the words once the water is below it. [bones] is a bird's skeleton lying
 * low on the bed: it shows only where the water has gone, and salt grows up to it, not over it.
 */
class Shore(
    val width: Int,
    val height: Int,
    private val title: BooleanArray,
    private val titleBottom: Int,
    private val bones: BooleanArray = BooleanArray(width * height),
    seed: Int = 1,
) {
    private val random = Random(seed)

    /** The water's edge for each column, this frame. Water is below it. */
    private val edge = FloatArray(width)

    private val keepOut = Salt.keepOutOf(BooleanArray(width * height) { title[it] || bones[it] }, width, height)

    /** Glyph edge cells salt can start from: the words', not the bones'. */
    private val titleKeepOut = Salt.keepOutOf(title, width, height)

    private val titleEdge = Salt.edgeOf(titleKeepOut, width, height).filter { !keepOut[it] }.toIntArray()

    /** Salt only on dry ground, a cell back from the water. */
    private val salt = Salt(width, height, keepOut, random) { x, y -> y < edge[x] - 1 }

    private var wordCrystals = 0

    var frame = 0
        private set

    /** The frame the salt stopped growing, or -1 while it still grows. */
    var finishedAt = -1
        private set

    val finished: Boolean get() = finishedAt >= 0

    val seconds: Float get() = frame / FPS.toFloat()

    /** 0 until the water passes the middle, then up to 1 when the salt is done. */
    val titleInk: Float
        get() {
            val start = startOfTitle
            if (start < 0 || frame < start) return 0f
            if (finished) return 1f
            val end = max(start + 1, (SALT_END_S * FPS).toInt())
            return min(1f, TITLE_FIRST + (1f - TITLE_FIRST) * (frame - start) / (end - start))
        }

    private var startOfTitle = -1

    init {
        computeEdge()
    }

    /** One frame on. */
    fun step() {
        if (finished) return
        frame++
        computeEdge()
        if (startOfTitle < 0 && shoreBase() >= height * 0.5f) startOfTitle = frame
        seed()
        salt.grow()
        // Done on time rather than on the salt's count, so the words darken evenly to the end.
        if (seconds >= SALT_END_S) finishedAt = frame
    }

    fun isWater(x: Int, y: Int): Boolean = y > edge[x]

    fun isSalt(x: Int, y: Int): Boolean = salt.isSalt(x, y)

    /** Whether the bones show at (x, y) now: only on dry ground, and where the sand lets them. */
    fun showsBone(x: Int, y: Int): Boolean =
        bones[y * width + x] && y < edge[x] - 1 &&
            BAYER[(y and 3) * 4 + (x and 3)] < boneInk(x, y) * 16f

    /**
     * How much of the bones shows through the sand at (x, y), 0 to [BONE_MOST]: a smooth drift
     * over the bed, so whole stretches of bone are buried, others faint, none fully dark.
     */
    private fun boneInk(x: Int, y: Int): Float {
        val sand = smoothNoise(x / SAND_CELLS, y / SAND_CELLS)
        return ((sand - SAND_BURIED) / (1f - SAND_BURIED)).coerceIn(0f, BONE_MOST)
    }

    /** Value noise: [hash] at whole points, blended smoothly between them. */
    private fun smoothNoise(fx: Float, fy: Float): Float {
        val x0 = kotlin.math.floor(fx).toInt()
        val y0 = kotlin.math.floor(fy).toInt()
        val tx = fx - x0
        val ty = fy - y0
        val sx = tx * tx * (3 - 2 * tx)
        val sy = ty * ty * (3 - 2 * ty)
        val top = hash(x0, y0 + 9001) * (1 - sx) + hash(x0 + 1, y0 + 9001) * sx
        val foot = hash(x0, y0 + 9002) * (1 - sx) + hash(x0 + 1, y0 + 9002) * sx
        return top * (1 - sy) + foot * sy
    }

    fun saltCount(): Int = salt.size

    /** The whole picture, true for black, row by row. */
    fun render(into: BooleanArray) {
        val ink = titleInk
        val wobble = (2 * PI * seconds / LAP_S).toFloat()
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val i = row + x
                into[i] = when {
                    salt.isSalt(i) -> true
                    title[i] && ink > 0f -> BAYER[(y and 3) * 4 + (x and 3)] < ink * 16f
                    bones[i] && y < edge[x] - 1 -> BAYER[(y and 3) * 4 + (x and 3)] < boneInk(x, y) * 16f
                    y > edge[x] -> hash(x, y) < STIPPLE
                    else -> false
                }
            }
        }
        // The water's edge, two cells thick, and the wave lines out on the water.
        for (x in 0 until width) {
            val e = edge[x].toInt()
            for (y in e..e + 1) if (y in 0 until height) into[y * width + x] = true
            for (k in 0 until WAVES) {
                // Distance out from the edge. It shrinks: the waves come in to the shore.
                val span = WAVES * SPACING
                val d = ((k * SPACING - seconds * SPACING / WAVE_S) % span + span) % span
                if (d < SPACING * 0.6f) continue
                if (hash(x / DASH, k * 7919) < GAPS) continue
                val y = (e + d + 1.5f * sin(x * 0.11f + k * 1.3f + wobble)).toInt()
                if (y in 0 until height && !salt.isSalt(x, y)) into[y * width + x] = true
            }
        }
    }

    /** The water's edge at its middle, from just under the top to past the foot. */
    private fun shoreBase(): Float {
        val t = seconds
        val start = height * 0.15f
        val end = height * 1.12f
        if (t <= CALM_S) return start
        val p = min(1f, (t - CALM_S) / (RECEDE_END_S - CALM_S))
        val eased = p * p * (3 - 2 * p)
        return start + (end - start) * eased
    }

    private fun computeEdge() {
        val base = shoreBase()
        val lap = height * 0.008f * sin((2 * PI * seconds / LAP_S).toFloat())
        for (x in 0 until width) {
            edge[x] = base + lap +
                height * 0.018f * sin(x * 0.021f + 0.3f) +
                height * 0.007f * sin(x * 0.067f + 1.7f)
        }
    }

    /**
     * New crystals: out of the words once the water has left them, and over the bed behind
     * the water, spread out so each has room, so the salt follows the edge down the screen.
     * About one in four on the bed is a hopper.
     */
    private fun seed() {
        val dryBelowWords = shoreBase() > titleBottom + height * 0.04f
        if (dryBelowWords && titleEdge.isNotEmpty() && wordCrystals < WORD_CRYSTALS) {
            repeat(2) {
                val i = titleEdge[random.nextInt(titleEdge.size)]
                if (salt.dendrite(i % width, i / width, WORD_MOST)) wordCrystals++
            }
        }
        val behind = shoreBase() - height * 0.05f
        if (behind <= 0f) return
        val wanted = (width * min(behind, height.toFloat()) / CELLS_PER_CRYSTAL).toInt() + wordCrystals
        var tries = 0
        while (salt.crystalCount < wanted && tries++ < 12) {
            val x = random.nextInt(width)
            val y = random.nextInt(max(1, min(behind.toInt(), height)))
            if (salt.crowded(x, y, SPREAD)) continue
            if (random.nextFloat() < HOPPER_SHARE) salt.hopper(x, y, HOPPER_HALF)
            else salt.dendrite(x, y, BED_MOST)
        }
    }

    companion object {
        /** Frames a second. E-ink: a few, never more. */
        const val FPS = 4

        /** Seconds of still water before it starts to draw back. */
        const val CALM_S = 4f

        /** When the water has gone off the foot of the screen. */
        const val RECEDE_END_S = 24f

        /** The salt is done, and the words fully dark, here. Each crystal may stop sooner. */
        const val SALT_END_S = 34f

        /** How dark the words are when they first show. */
        const val TITLE_FIRST = 0.12f

        private const val LAP_S = 5f
        private const val WAVE_S = 7f
        private const val WAVES = 6
        private const val SPACING = 22f
        private const val DASH = 9
        private const val GAPS = 0.35f
        private const val STIPPLE = 0.03f

        /** Cells across one drift of sand over the bones; where it is under this, they are buried. */
        private const val SAND_CELLS = 11f
        private const val SAND_BURIED = 0.27f

        /** The bones at their clearest: faint, never solid. */
        private const val BONE_MOST = 0.8f

        /** Crystals that start from the words. */
        private const val WORD_CRYSTALS = 14
        private val WORD_MOST = 80..220

        /** One crystal on the bed for this many cells of it, at least [SPREAD] apart. */
        private const val CELLS_PER_CRYSTAL = 2_600
        private const val SPREAD = 24
        private val BED_MOST = 140..420
        private const val HOPPER_SHARE = 0.25f
        private val HOPPER_HALF = 6..14

        /** A 4 x 4 ordered dither: a cell is inked when its number is under 16 x the shade. */
        val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

        /** A fixed value in 0..1 for a cell, so the stipple stays put on the ground. */
        fun hash(x: Int, y: Int): Float {
            var h = x * 374761393 + y * 668265263
            h = (h xor (h ushr 13)) * 1274126177
            h = h xor (h ushr 16)
            return (h and 0xFFFFFF) / 16777216f
        }
    }
}
