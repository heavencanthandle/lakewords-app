package com.heavencanthandle.lakewords.book

import android.content.Context

/**
 * The text of lake words, from `assets/lakewords.txt`: the website's read page, which says
 * "anti-copyright, reproduce freely". One block a line: `# ` a heading, `~ ` an italic line,
 * `---` a rule, anything else a paragraph. Blank lines are nothing.
 */
sealed interface Block {
    data class Heading(val text: String) : Block
    data class Italic(val text: String) : Block
    data class Paragraph(val text: String) : Block
    data object Rule : Block
}

object Book {
    const val ASSET = "lakewords.txt"

    fun load(context: Context): List<Block> =
        context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }

    fun parse(text: String): List<Block> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            when {
                line == "---" -> Block.Rule
                line.startsWith("# ") -> Block.Heading(line.removePrefix("# ").trim())
                line.startsWith("~ ") -> Block.Italic(line.removePrefix("~ ").trim())
                else -> Block.Paragraph(line)
            }
        }
        .toList()
}
