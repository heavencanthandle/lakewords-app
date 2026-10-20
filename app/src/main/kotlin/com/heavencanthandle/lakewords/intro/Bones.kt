package com.heavencanthandle.lakewords.intro

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.random.Random

/**
 * A bird's bones, drawn as line work: the skull alone, and a whole skeleton lying on its side
 * on the dry lake bed, as the shore leaves them. Each fits a box [w] wide with its top left at
 * ([x], [y]); the skull faces left.
 */
object Bones {

    /** The skull's box is 100 x 50 units: cranium at the right, a long beak to the left. */
    fun skull(canvas: Canvas, x: Float, y: Float, w: Float, paint: Paint) {
        val u = w / 100f
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(u, u)
        val line = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = paint.strokeWidth / u
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(paint).apply { style = Paint.Style.FILL }
        // Cranium.
        canvas.drawOval(RectF(48f, 7f, 94f, 43f), line)
        // The orbit, large as a bird's is, and dark.
        canvas.drawCircle(66f, 22f, 8.5f, fill)
        // Upper beak: from the brow to a fine point, and back along its cutting edge.
        canvas.drawPath(Path().apply {
            moveTo(52f, 12f)
            cubicTo(36f, 15f, 16f, 24f, 3f, 31f)
            cubicTo(18f, 30f, 36f, 29f, 52f, 30f)
        }, line)
        // The nostril, a slit along the beak.
        canvas.drawLine(28f, 24.5f, 39f, 22f, line)
        // Lower jaw, a little open, hinged under the cranium.
        canvas.drawPath(Path().apply {
            moveTo(9f, 34f)
            cubicTo(26f, 36f, 46f, 37f, 62f, 38f)
            lineTo(78f, 41f)
        }, line)
        // The quadrate and the first of the neck.
        canvas.drawLine(84f, 37f, 92f, 45f, line)
        canvas.restore()
    }

    /**
     * The skeleton's box is 200 x 100 units: skull at the left, a wing open above the body.
     * It has lain a while: the bones have come a little apart, each nudged and turned a little
     * from where it grew ([seed] chooses how), a rib or two is gone, and the wing still holds
     * some ragged feathers, hanging off the bone.
     */
    fun skeleton(canvas: Canvas, x: Float, y: Float, w: Float, paint: Paint, seed: Int = 1) {
        val random = Random(seed)
        fun jitter(by: Float) = (random.nextFloat() * 2f - 1f) * by
        val u = w / 200f
        skull(canvas, x + jitter(3f) * u - 4f * u, y + (20f + jitter(3f)) * u, 52f * u, paint)
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(u, u)
        val line = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = paint.strokeWidth / u
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val thin = Paint(line).apply { strokeWidth = line.strokeWidth * 0.7f }

        /** One bone or group of bones, moved by up to [move] and turned by up to [turn] degrees. */
        fun part(cx: Float, cy: Float, move: Float, turn: Float, draw: () -> Unit) {
            canvas.save()
            canvas.translate(jitter(move), jitter(move))
            canvas.rotate(jitter(turn), cx, cy)
            draw()
            canvas.restore()
        }

        // Neck: small vertebrae from the back of the skull to the shoulders, strung loosely.
        val neck = listOf(50f to 42f, 56f to 46f, 62f to 49f, 68f to 51f, 74f to 52f, 80f to 52f, 86f to 51f, 92f to 50f)
        neck.forEach { (cx, cy) ->
            if (random.nextFloat() < 0.15f) return@forEach
            canvas.drawCircle(cx + jitter(1.6f), cy + jitter(1.6f), 2.4f, thin)
        }
        // Spine along the back to the pelvis, in two pieces.
        part(120f, 46f, 2f, 3f) {
            canvas.drawPath(Path().apply { moveTo(95f, 49f); cubicTo(105f, 46f, 118f, 45f, 128f, 45f) }, line)
        }
        part(150f, 47f, 2f, 4f) {
            canvas.drawPath(Path().apply { moveTo(133f, 45f); cubicTo(145f, 45f, 157f, 47f, 167f, 50f) }, line)
        }
        // Ribs, a few gone, the rest come loose from the spine.
        for (k in 0 until 6) {
            if (random.nextFloat() < 0.25f) continue
            val sx = 102f + k * 10f
            part(sx, 60f, 1.8f, 9f) {
                canvas.drawPath(Path().apply {
                    moveTo(sx, 49f + jitter(1.5f))
                    cubicTo(sx - 4f, 57f, sx - 2f, 66f, sx + 4f, 72f + jitter(2f))
                }, thin)
            }
        }
        // Breastbone and keel, slid a little off the ribs.
        part(130f, 75f, 4f, 5f) {
            canvas.drawPath(Path().apply { moveTo(98f, 71f); cubicTo(115f, 81f, 140f, 81f, 160f, 71f) }, line)
        }
        // Pelvis, the tail, and the pygostyle.
        part(163f, 52f, 3f, 10f) { canvas.drawOval(RectF(150f, 44f, 176f, 60f), line) }
        part(188f, 52f, 3f, 12f) {
            listOf(181f to 51f, 187f to 52f).forEach { (cx, cy) -> canvas.drawCircle(cx, cy, 2.0f, thin) }
            canvas.drawPath(Path().apply { moveTo(191f, 49f); lineTo(198f, 52f); lineTo(191f, 56f); close() }, thin)
        }
        // Wing, open above the body: humerus, forearm, hand, each come a little apart at the joint.
        part(98f, 33f, 2.5f, 5f) { canvas.drawLine(104f, 45f, 93f, 21f, line) }
        part(116f, 16f, 2.5f, 3f) {
            canvas.drawLine(95f, 19f, 140f, 8f, line)
            canvas.drawLine(96f, 24f, 140f, 13f, thin)
        }
        part(163f, 7f, 3f, 5f) {
            canvas.drawLine(143f, 9f, 172f, 6f, line)
            canvas.drawLine(173f, 6f, 186f, 9f, thin)
        }
        // What is left of the flight feathers: quills hanging off the trailing edge, ragged,
        // some short or bent, some fallen a little away from the bone, a few vanes still on.
        var fx = 100f
        while (fx < 184f) {
            fx += 5f + random.nextFloat() * 6f
            if (random.nextFloat() < 0.2f) continue
            // Where the bone is at this point along the wing.
            val boneY = if (fx < 140f) 13f + (140f - fx) * 0.25f else 9f + (fx - 140f) * 0.02f
            val gap = if (random.nextFloat() < 0.3f) 2f + random.nextFloat() * 4f else 0.5f
            val length = 12f + random.nextFloat() * 20f
            val lean = 0.35f + random.nextFloat() * 0.5f
            val sag = jitter(4f)
            val x0 = fx
            val y0 = boneY + gap
            val x1 = x0 + length * lean
            val y1 = y0 + length
            canvas.drawPath(Path().apply {
                moveTo(x0, y0)
                quadTo(x0 + length * lean * 0.3f + sag, y0 + length * 0.6f, x1, y1)
            }, thin)
            // A few barbs still on, on the side away from the body, and never many.
            val barbs = random.nextInt(4)
            for (b in 0 until barbs) {
                val t = 0.25f + random.nextFloat() * 0.6f
                val bx = x0 + (x1 - x0) * t
                val by = y0 + (y1 - y0) * t
                val len = 3f + random.nextFloat() * 4f
                canvas.drawLine(bx, by, bx + len, by - len * 0.5f + jitter(1.5f), thin)
            }
        }
        // Legs, folded, come away from the hips; toes splayed.
        part(162f, 80f, 3f, 8f) {
            canvas.drawLine(160f, 60f, 166f, 72f, line)
            canvas.drawLine(166f, 73f, 151f, 88f, line)
        }
        part(160f, 95f, 3f, 14f) {
            canvas.drawLine(150f, 89f, 160f, 95f, thin)
            canvas.drawLine(160f, 95f, 172f, 92f, thin)
            canvas.drawLine(160f, 95f, 171f, 99f, thin)
            canvas.drawLine(160f, 95f, 166f, 100f, thin)
        }
        canvas.restore()
    }
}
