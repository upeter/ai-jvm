package dev.example.utils

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser
import org.apache.pdfbox.io.RandomAccessFile
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths


fun getText(
    file: File, vararg skipPages: Int, headerRegion: Rectangle2D.Double? = null,
    footerRegion: Rectangle2D.Double? = null,
): String {
    try {
        val parser = PDFParser(RandomAccessFile(file, "r"))
        parser.parse()
        val pdDoc = parser.pdDocument
        val stripper = CustomPDFTextStripper(* skipPages, headerRegion = headerRegion, footerRegion = footerRegion)
        stripper.lineSeparator = "\n"
        return stripper.getText(pdDoc)
    } catch (e: Exception) {
        println(
            ("Extracting text from the .pdf  file " + file.getName()).toString() + " failed with " + e.message
        )
        throw e
    }
}


class CustomPDFTextStripper(
    private vararg val skipPages: Int,
    private val headerRegion: Rectangle2D.Double? = null,
    private val footerRegion: Rectangle2D.Double? = null,
) : PDFTextStripper() {

    override fun processTextPosition(text: TextPosition) {
        val pageNumber = currentPageNo
        if (!shouldSkipPage(pageNumber) && !isInHeaderOrFooter(text)) {
            super.processTextPosition(text)
        }
    }


    private fun shouldSkipPage(pageNumber: Int): Boolean = pageNumber in skipPages

    private fun isInHeaderOrFooter(text: TextPosition): Boolean {
        val x = text.xDirAdj
        val y = text.yDirAdj
        val width = text.widthDirAdj
        val height = text.heightDir
        val textBounds = Rectangle2D.Float(x, y, width, height)
        //println("===> bound: $textBounds")

        if (headerRegion != null && headerRegion.intersects(textBounds)) {
            //println("=====================> is header: $text")
            return true
        }

        if (footerRegion != null && footerRegion.intersects(textBounds)) {
            //println("=====================> is footer: $text")
            return true
        }

        return false
    }

}

class CustomApachePdfBoxDocumentParser( vararg val  skipPages: Int, val headerRegion: Rectangle2D.Double? = null,
                                        val footerRegion: Rectangle2D.Double? = null, val skipFromRegx:Regex? = null) : DocumentParser {
    override fun parse(inputStream: InputStream): Document {
        try {
            val pdfDocument = PDDocument.load(inputStream)
            val stripper = CustomPDFTextStripper(* skipPages, headerRegion = headerRegion, footerRegion = footerRegion).apply { lineSeparator = "\n" }
            val text = stripper.getText(pdfDocument).let{if(skipFromRegx != null) it.lines().takeWhile { !it.contains(skipFromRegx) }.joinToString("\n") else it}
            pdfDocument.close()
            return Document.from(text)
        } catch (var5: IOException) {
            throw RuntimeException(var5)
        }
    }
}

fun main() {
    val file = Files.list(Paths.get("/Users/urs/development/github/ai/langchain4j/src/main/resources/pdfs")).toList().first().toFile()
    println(ApachePdfBoxDocumentParser().parse(file.inputStream()).text())
    //println(getText(file, 2,3,4,5, headerRegion = Rectangle2D.Double(0.0, 750.0, 612.0, 40.0), footerRegion = Rectangle2D.Double(0.0, 0.0, 612.9, 20.0)).lines().takeWhile { !it.contains("""^REFERENCES$""".toRegex()) }.joinToString("\n"))
    //println(getText(file, 2,3,4,5).lines().takeWhile { !it.contains("""^REFERENCES$""".toRegex()) }.joinToString("\n"))
}