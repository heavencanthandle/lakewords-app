package com.heavencanthandle.lakewords

import com.heavencanthandle.lakewords.book.Block
import com.heavencanthandle.lakewords.book.Book
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BookTest {

    @Test
    fun eachLineIsOneBlock() {
        val blocks = Book.parse("# dedication\nto those\n\n~ caging our desires\n---\n")
        assertEquals(
            listOf(
                Block.Heading("dedication"),
                Block.Paragraph("to those"),
                Block.Italic("caging our desires"),
                Block.Rule,
            ),
            blocks,
        )
    }

    @Test
    fun theBookStartsWithTheTaglineAndEndsWithTheLake() {
        val text = File("src/main/assets/${Book.ASSET}").readText()
        val blocks = Book.parse(text)
        val first = blocks.first() as Block.Paragraph
        assert(first.text.contains("a love so robust it refuses to look away"))
        assertEquals(Block.Paragraph("to the lake – the center. thank you for holding all this."), blocks.last())
        assert(blocks.contains(Block.Heading("lake words")))
    }
}
