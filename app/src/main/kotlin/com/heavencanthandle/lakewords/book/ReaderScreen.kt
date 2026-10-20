package com.heavencanthandle.lakewords.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heavencanthandle.lakewords.R
import com.heavencanthandle.lakewords.ui.Garamond
import kotlinx.coroutines.launch

/**
 * The book. On e-ink it turns by pages, not by sliding: a press on the lower part of the screen
 * goes a page on, on the upper part a page back. A drag still scrolls. [onReplay] plays the shore
 * again.
 */
@Composable
fun ReaderScreen(onReplay: () -> Unit) {
    val context = LocalContext.current
    val blocks = remember { Book.load(context) }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        val pageHeight = with(LocalDensity.current) { maxHeight.toPx() } * PAGE_SHARE
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { at ->
                        val forward = at.y > size.height * 0.33f
                        scope.launch { state.scrollBy(if (forward) pageHeight else -pageHeight) }
                    }
                },
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 28.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text(
                        text = stringResource(R.string.title),
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = Garamond,
                        fontStyle = FontStyle.Italic,
                        fontSize = 40.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                    )
                    // Small, beside the title: the shore again from the start.
                    Text(
                        text = stringResource(R.string.replay),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable(onClick = onReplay)
                            .padding(start = 10.dp, top = 4.dp, bottom = 10.dp),
                        fontFamily = Garamond,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
            itemsIndexed(blocks) { _, block -> BlockView(block) }
            item {
                Text(
                    text = "anti-copyright, reproduce freely",
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 40.dp),
                    fontFamily = Garamond,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BlockView(block: Block) {
    when (block) {
        is Block.Heading -> Text(
            text = block.text,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            fontFamily = Garamond,
            fontWeight = FontWeight.SemiBold,
            fontSize = 25.sp,
            lineHeight = 31.sp,
            color = Color.Black,
        )
        is Block.Italic -> Text(
            text = block.text,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            fontFamily = Garamond,
            fontStyle = FontStyle.Italic,
            fontSize = BODY,
            lineHeight = BODY_LINE,
            color = Color.Black,
        )
        is Block.Paragraph -> Text(
            text = block.text,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            fontFamily = Garamond,
            fontSize = BODY,
            lineHeight = BODY_LINE,
            color = Color.Black,
        )
        Block.Rule -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp)
                .height(1.dp)
                .background(Color.Black),
        )
    }
}

/** A page is most of the screen, so the last line or two of one is the top of the next. */
private const val PAGE_SHARE = 0.88f

private val BODY = 20.sp
private val BODY_LINE = 28.sp
