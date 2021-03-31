package com.nrw.readtext

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import android.content.Intent
import android.content.Context
import android.content.ContentResolver
import android.provider.OpenableColumns
import android.net.Uri

import org.mozilla.universalchardet.UniversalDetector


class TextHandler(context: Context) {
    var total_viewline_count: Int;
    var current_viewline_content: String;
    var current_viewline_index: Int;

    private val context: Context = context;
    private val contentResolver: ContentResolver = context.contentResolver
    private var encoding: String;
    private var uri: Uri;
    private var textName: String;
    private var textLength: Long;

    init {
        this.uri = Uri.EMPTY;
        this.encoding = "UTF-8"
        this.textName = ""
        this.textLength = 0;
        this.current_viewline_content = "";
        this.current_viewline_index = 0;
        this.total_viewline_count = 0;
    }

    companion object {
        val intentForTextFileUri: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Provide read access to files and sub-directories in the user-selected directory.
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
    }

    fun getUriString(): String {
        return uri.toString()
    }

    fun initialize(uri: Uri) {
        this.uri = uri
        contentResolver.openInputStream(uri).use { inputStream -> // use는 close() 호출 보장. closeable?을 상속받고 있어야 함.
            this.encoding = UniversalDetector.detectCharset(inputStream)
        }
        contentResolver.query(uri, null, null, null, null).use { cursor ->
            cursor!!.moveToFirst() // cursor object의 초기화와 같다
            textName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            textLength = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
        }
    }

    fun getTextName(): String {
        return textName;
    }
    fun getTextLength(): Long {
        return textLength;
    }

    @Throws(IOException::class)
    fun readText(): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, encoding)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    line = line + "\n"
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}