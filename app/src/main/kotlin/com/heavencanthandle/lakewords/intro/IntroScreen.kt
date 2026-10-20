package com.heavencanthandle.lakewords.intro

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.core.content.res.ResourcesCompat
import com.heavencanthandle.lakewords.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Screen pixels to a cell of the lake. Two keeps the salt's lines thick enough to see. */
private const val CELL_PX = 2

/** How long the finished tagline page stays before the book. */
private const val TAGLINE_HOLD_MS = 6_000L

/** Between the pieces of the tagline page coming in. */
private const val PIECE_MS = 2_000L

/** Presses that skip to the book, each within [SKIP_WINDOW_MS] of the one before. */
private const val SKIP_PRESSES = 5
private const val SKIP_WINDOW_MS = 1_500L

/**
 * The opening: the shore drying into salt under the title, with a bird's bones left on the bed,
 * a cleared screen, then the tagline and what comes under it, then [onDone]. Five quick presses
 * go straight to the book.
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val description = stringResource(R.string.intro_description)
    val titleText = stringResource(R.string.title)
    val taglineText = stringResource(R.string.tagline)
    var presses by remember { mutableIntStateOf(0) }
    var lastPress by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures {
                    val now = (SystemClock.uptimeMillis() and 0x3FFFFFFF).toInt()
                    presses = if (now - lastPress <= SKIP_WINDOW_MS) presses + 1 else 1
                    lastPress = now
                    if (presses >= SKIP_PRESSES) onDone()
                }
            },
    ) {
        val density = LocalDensity.current
        val pxW = with(density) { maxWidth.roundToPx() }
        val pxH = with(density) { maxHeight.roundToPx() }
        val w = max(1, pxW / CELL_PX)
        val h = max(1, pxH / CELL_PX)

        val stage = remember(pxW, pxH) {
            val italic = ResourcesCompat.getFont(context, R.font.eb_garamond_italic)
            Stage.make(w, h, pxW, pxH, titleText, taglineText, italic, context.resources)
        }
        var frame by remember(stage) { mutableIntStateOf(0) }

        // The whole opening runs under a minute: the screen stays on for it, and only for it.
        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }

        LaunchedEffect(stage) {
            // Paced by the clock: the work of a frame comes out of its wait, so the scene keeps
            // its timing on a slow phone instead of stretching out. The working out is off the
            // main thread; the picture is filled in on it, so it is never drawn half-changed.
            suspend fun run(finished: () -> Boolean, step: () -> Unit, draw: () -> Unit) {
                val tick = 1_000L / Shore.FPS
                var next = SystemClock.uptimeMillis() + tick
                while (!finished()) {
                    delay((next - SystemClock.uptimeMillis()).coerceAtLeast(0))
                    next += tick
                    withContext(Dispatchers.Default) { step() }
                    draw()
                    frame++
                }
            }

            stage.drawShore()
            frame++
            run({ stage.shore.finished }, { stage.shore.step() }, { stage.drawShore() })
            delay(1_500)

            // Clear to white before the tagline. Only white: no black flash, nothing to wipe the
            // panel clean. Whatever the e-ink keeps of the shore is meant to linger as a ghost.
            stage.clear()
            frame++
            delay(900)

            // The tagline, then what goes under it, one piece at a time.
            stage.drawPage()
            frame++
            while (!stage.page.finished) {
                delay(PIECE_MS)
                stage.page.showNext()
                stage.drawPage()
                frame++
            }
            delay(TAGLINE_HOLD_MS)
            onDone()
        }

        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION") frame
            drawImage(
                stage.image(),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

/**
 * The two scenes, for one screen size. The shore is drawn on cells of [CELL_PX] pixels, which
 * keeps the salt's lines thick enough to see and its working out quick. The tagline page does
 * not move, so it is drawn at the screen's own pixels: the knot band's fine line work needs them.
 */
private class Stage(
    val shore: Shore,
    val page: TaglinePage,
) {
    private val shoreFrame = Frame(shore.width, shore.height)
    private val pageFrame = Frame(page.width, page.height)
    private var current = shoreFrame

    fun image() = current.image

    fun drawShore() {
        current = shoreFrame
        shore.render(shoreFrame.ink)
        shoreFrame.show()
    }

    /** Clears to white, on the page's frame, ready for the tagline. */
    fun clear() {
        current = pageFrame
        pageFrame.ink.fill(false)
        pageFrame.show()
    }

    fun drawPage() {
        current = pageFrame
        page.render(pageFrame.ink)
        pageFrame.show()
    }

    private class Frame(val w: Int, val h: Int) {
        val ink = BooleanArray(w * h)
        private val pixels = IntArray(w * h)
        private val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val image = bitmap.asImageBitmap()

        fun show() {
            for (i in ink.indices) pixels[i] = if (ink[i]) BLACK else WHITE
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    companion object {
        private const val BLACK = 0xFF000000.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()

        fun make(
            w: Int,
            h: Int,
            pw: Int,
            ph: Int,
            title: String,
            tagline: String,
            italic: Typeface?,
            resources: Resources,
        ): Stage {
            val seed = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
            // The title, a little above the middle, across most of the width.
            val titleMask = Mask(w, h, italic)
            val titleBottom = titleMask.lines(listOf(title), widthShare = 0.78f, firstBaseline = h * 0.44f)
            // A bird's skeleton lying low on the bed, a little left of centre.
            val bones = Mask(w, h, italic)
            // Drawn a little heavy: the sand over it thins it to a faint dot pattern on the shore.
            bones.draw(stroke = 1.9f) { canvas, paint -> Bones.skeleton(canvas, w * 0.2f, h * 0.78f, w * 0.6f, paint, seed) }
            // The tagline page, at the screen's own pixels: the tagline as big as its longest
            // line allows, in the upper part of the screen.
            val lines = tagline.split('\n')
            val taglineMask = Mask(pw, ph, italic)
            val taglineBottom = taglineMask.lines(lines, widthShare = 0.86f, firstBaseline = null)
            // Under it: the knot band and the vigil keepers from the zine-launch handbill on the
            // website, and a little bird skull.
            val knot = Mask(pw, ph, italic)
            val knotBottom = knot.image(resources, R.drawable.knot_band, taglineBottom + ph * 0.05f, widthShare = 0.9f)
            val keepers = Mask(pw, ph, italic)
            val keepersBottom = keepers.image(resources, R.drawable.vigil_keepers, knotBottom + ph * 0.06f, widthShare = 0.62f)
            val skull = Mask(pw, ph, italic)
            skull.draw(stroke = 2.6f) { canvas, paint ->
                val sw = pw * 0.3f
                Bones.skull(canvas, (pw - sw) / 2f, keepersBottom + ph * 0.07f, sw, paint)
            }
            return Stage(
                shore = Shore(w, h, titleMask.cells, titleBottom, bones.cells, seed),
                page = TaglinePage(pw, ph, listOf(taglineMask.cells, knot.cells, keepers.cells, skull.cells)),
            )
        }
    }
}

/** Words, line drawings and pictures drawn once into cells: true where ink is. */
private class Mask(private val w: Int, private val h: Int, private val typeface: Typeface?) {
    val cells = BooleanArray(w * h)

    /** Line work drawn with [block], a stroke [stroke] cells wide. */
    fun draw(stroke: Float = 1.3f, block: (AndroidCanvas, Paint) -> Unit) {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            strokeWidth = stroke
        }
        block(AndroidCanvas(bitmap), paint)
        take(bitmap)
    }

    /**
     * The black-on-clear picture [res], centred, [widthShare] of the width, its top at [top].
     * Returns the lowest row inked.
     */
    fun image(resources: Resources, res: Int, top: Float, widthShare: Float): Float {
        val source = BitmapFactory.decodeResource(resources, res) ?: return top
        val width = (w * widthShare).toInt().coerceAtLeast(1)
        val height = (source.height.toFloat() * width / source.width).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        AndroidCanvas(bitmap).drawBitmap(scaled, (w - width) / 2f, top, null)
        take(bitmap)
        return top + height
    }

    private fun take(bitmap: Bitmap) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        for (i in cells.indices) cells[i] = (pixels[i] ushr 24) > 110
    }

    /**
     * Draws [lines] centred, the longest at [widthShare] of the width. [firstBaseline] null
     * puts the block's top a seventh of the way down. Returns the lowest row inked.
     */
    fun lines(lines: List<String>, widthShare: Float, firstBaseline: Float?): Int {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = this@Mask.typeface
            color = android.graphics.Color.BLACK
            textSize = 100f
        }
        val longest = lines.maxOf { paint.measureText(it) }
        paint.textSize = 100f * (w * widthShare) / longest
        val lineGap = paint.textSize * 1.12f
        val bounds = Rect()
        paint.getTextBounds(lines.first(), 0, lines.first().length, bounds)
        val baseline0 = firstBaseline ?: (h * 0.14f - bounds.top)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        var bottom = 0
        lines.forEachIndexed { k, line ->
            val baseline = baseline0 + k * lineGap
            val x = (w - paint.measureText(line)) / 2f
            canvas.drawText(line, x, baseline, paint)
            paint.getTextBounds(line, 0, line.length, bounds)
            bottom = max(bottom, (baseline + bounds.bottom).toInt())
        }
        take(bitmap)
        return bottom.coerceIn(0, h - 1)
    }
}
