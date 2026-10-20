package com.heavencanthandle.lakewords

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.heavencanthandle.lakewords.book.ReaderScreen
import com.heavencanthandle.lakewords.intro.IntroScreen
import com.heavencanthandle.lakewords.ui.monochrome
import com.mudita.mmd.ThemeMMD

/**
 * The lake on each start, then the book. Turning or returning to the app keeps the book; its
 * replay button shows the lake again.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(colorScheme = monochrome) {
                var reading by rememberSaveable { mutableStateOf(false) }
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    if (reading) {
                        ReaderScreen(onReplay = { reading = false })
                    } else {
                        IntroScreen(onDone = { reading = true })
                    }
                }
            }
        }
    }
}
