package com.heavencanthandle.lakewords.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.heavencanthandle.lakewords.R

/**
 * EB Garamond, as on the website. The files are the variable fonts from Google Fonts (SIL Open
 * Font License, `assets/OFL-EBGaramond.txt`), so each weight is a setting of the one file.
 */
@OptIn(ExperimentalTextApi::class)
val Garamond = FontFamily(
    Font(R.font.eb_garamond, FontWeight.Normal),
    Font(
        R.font.eb_garamond,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(
        R.font.eb_garamond_italic,
        FontWeight.SemiBold,
        FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

/** Black on white and nothing else, for e-ink. */
val monochrome = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = Color.Black,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
)
