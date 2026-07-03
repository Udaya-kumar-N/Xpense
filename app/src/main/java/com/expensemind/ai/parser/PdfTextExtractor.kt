package com.expensemind.ai.parser

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfTextExtractor {

    private var initialized = false

    /** Call once, e.g. in Application.onCreate(), before extracting any PDF. */
    fun init(context: Context) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
    }

    /**
     * Extracts raw text from a PDF picked via the system file picker.
     * password: pass the statement password if the bank PDF is protected
     * (most Indian bank statements are password-protected, commonly PAN-based).
     */
    fun extractText(context: Context, uri: Uri, password: String? = null): String {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open PDF at $uri" }
            PDDocument.load(input, password).use { document ->
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                return stripper.getText(document)
            }
        }
    }
}
