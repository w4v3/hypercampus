/*
 *     Copyright (c) 2019, 2020 by w4v3 <support.w4v3+hypercampus@protonmail.com>
 *
 *     This file is part of HyperCampus.
 *
 *     HyperCampus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     HyperCampus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with HyperCampus.  If not, see <https://www.gnu.org/licenses/>.
 */

package onion.w4v3xrmknycexlsd.app.hypercampus.data

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import onion.w4v3xrmknycexlsd.app.hypercampus.*
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// private file directories
const val DIR_MEDIA = "media"

// intent codes
const val CREATE_FILE = 1
const val OPEN_FILE = 2

@Suppress("RegExpRedundantEscape")
class HyperDataConverter (private val activity: HyperActivity){
    @Inject lateinit var repository: HyperRepository
    private val contentResolver = activity.applicationContext.contentResolver

    private val newlyAdded = mutableListOf<Int>()
    private val renamedFiles = mutableMapOf<String, String>()

    init {
        (activity.applicationContext as HyperApp).hyperComponent.inject(this)
    }

    fun collectionToFile(data: List<DeckData>, withMedia: Boolean = false) = repository.repositoryScope.launch(Dispatchers.IO) {
        try {
            val uri: Uri? = suspendCancellableCoroutine { cont ->
                linkActivityIntent(CREATE_FILE, cont)
                if (withMedia) createFile("collection.hczip") else createFile("collection.hcmd")
            }

            if (withMedia) {
                val files = prepareZip(data)
                uri?.let { makeZipFile(files,it) }
                activity.goodSnack(activity.getString(R.string.exp_success))
            } else {
                uri?.let {
                    contentResolver.openOutputStream(it)?.let { out ->
                        writeStringTo(collectionToMd(data), out)
                    }
                }
                activity.goodSnack(activity.getString(R.string.exp_success))
            }
        } catch (e: FileNotFoundException) {
            activity.badSnack(activity.getString(R.string.fnf_exception))
        } catch (e: IOException) {
            activity.badSnack(activity.getString(R.string.io_exception))
        }
}

    fun fileToCollection(uri: Uri) = repository.repositoryScope.launch(Dispatchers.IO) {
        try {
            val proj = arrayOf(OpenableColumns.DISPLAY_NAME)
            val cursor = contentResolver.query(uri,proj,null,null,null)
            cursor?.moveToFirst()
            val filename = cursor?.getString(0)
            cursor?.close()
            when (filename?.substringAfterLast(".")) {
                "md" -> {
                    val content = contentResolver.openInputStream(uri)?.let { readStringFrom(it) }
                    content?.let { mdToCollection(it) }
                    activity.goodSnack(activity.getString(R.string.imp_success))
                }
                "hcmd" -> withContext(Dispatchers.Main){
                    val builder =  MaterialAlertDialogBuilder(activity)
                    builder.setTitle(activity.getString(R.string.title_import))
                        .setMessage(activity.getString(R.string.import_dialog))
                        .setPositiveButton(activity.getString(R.string.import_overwrite)) { _, _ ->
                            repository.repositoryScope.launch(Dispatchers.IO) {
                                val content = contentResolver.openInputStream(uri)?.let { readStringFrom(it) }
                                content?.let { mdToCollection(it, withOrder = true, overwrite = true) }
                                activity.goodSnack(activity.getString(R.string.imp_success))
                            }
                        }
                        .setNegativeButton(activity.getString(R.string.import_addnew)) { _, _ ->
                            repository.repositoryScope.launch(Dispatchers.IO) {
                                val content = contentResolver.openInputStream(uri)?.let { readStringFrom(it) }
                                content?.let { mdToCollection(it, withOrder = true, overwrite = false) }
                                activity.goodSnack(activity.getString(R.string.imp_success))
                            }
                        }

                    val dialog: Dialog? = builder.create()
                    dialog?.show()
                }
                "zip" -> {
                    unZipFile(uri)
                    activity.goodSnack(activity.getString(R.string.imp_success))
                }
                "hczip" -> withContext(Dispatchers.Main){
                    val builder =  MaterialAlertDialogBuilder(activity)
                    builder.setTitle(activity.getString(R.string.title_import))
                        .setMessage(activity.getString(R.string.import_dialog))
                        .setPositiveButton(activity.getString(R.string.import_overwrite)) { _, _ ->
                            repository.repositoryScope.launch(Dispatchers.IO) {
                                unZipFile(uri, withOrder = true, overwrite = true)
                                activity.goodSnack(activity.getString(R.string.imp_success))
                            }
                        }
                        .setNegativeButton(activity.getString(R.string.import_addnew)) { _, _ ->
                            repository.repositoryScope.launch(Dispatchers.IO) {
                                unZipFile(uri, withOrder = true, overwrite = false)
                                activity.goodSnack(activity.getString(R.string.imp_success))
                            }
                        }

                    val dialog: Dialog? = builder.create()
                    dialog?.show()
                }
                else -> {
                    throw UnsupportedOperationException()
                }
            }
        } catch (e: FileNotFoundException) {
            activity.badSnack(activity.getString(R.string.fnf_exception))
        } catch (e: IOException) {
            activity.badSnack(activity.getString(R.string.io_exception))
        } catch (e: UnsupportedOperationException) {
            activity.badSnack(activity.getString(R.string.not_recognized_exception))
        }
    }

    suspend fun addMedia(requestName: String, type: String): String {
        try {
            val uri: Uri? = suspendCoroutine { cont ->
                linkActivityIntent(OPEN_FILE,cont)
                openFile(type)
            }

            val toParent = getMediaAccess()

            val from = uri?.let { contentResolver.openInputStream(it) }
            val to = getUniqueFile(toParent,requestName)
            from?.let { writeFromTo(from, to.outputStream()) }
            return "![${if (type == FILE_AUDIO) "audio" else "image"}]{${to.name}}"
        } catch (e: FileNotFoundException) {
            activity.badSnack(activity.getString(R.string.fnf_exception))
            return ""
        } catch (e: IOException) {
            activity.badSnack(activity.getString(R.string.io_exception))
            return ""
        }
    }

    fun convertToViews(toConvert: String, root: LinearLayout) {
        val mediaRegex = Regex("""!\[(.*)\]\{(.*)\}""")
        val groups: MutableList<String> = toConvert.split(mediaRegex).toMutableList()
        for ((i,s) in mediaRegex.findAll(toConvert).withIndex()) {
            groups.add(2*i+1,s.value)
        }

        groups.map { it.removeSuffix("\n") }

        for (g in groups) {
            if (g == "") continue
            val parse = mediaRegex.find(g)?.groups
            when (parse?.get(1)?.value) {
                "image" -> {
                    val imgView = ImageView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                    val uri = "${getMediaAccess().absolutePath}/${parse[2]?.value}"
                    root.addView(imgView)
                    Picasso.get().load(File(uri)).resizeDimen(R.dimen.card_width,R.dimen.card_height).centerInside().into(imgView)
                }
                "audio" -> {
                    root.addView(ImageButton(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        setBackgroundColor(activity.getThemeColor(R.attr.colorSecondaryVariant))
                        setPadding(8,8,8,8)
                        minimumHeight = 48
                        minimumWidth = 48
                        setImageResource(R.drawable.ic_volume_up_white_24dp)
                        fun play(uri: String) = try {
                            activity.mediaPlayer?.reset()
                            activity.mediaPlayer?.setDataSource(uri)
                        } catch (e: IOException) {
                            activity.badSnack(activity.getString(R.string.io_exception))
                        } finally {
                            activity.mediaPlayer?.prepare()
                            activity.mediaPlayer?.start()
                        }
                        val uri = "${getMediaAccess().absolutePath}/${parse[2]?.value}"
                        play(uri)
                        setOnClickListener {
                            play(uri)
                        }
                    })
                }
                null -> {
                    root.addView(TextView(activity).apply {
                        text = g
                        gravity = Gravity.CENTER
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            @Suppress("DEPRECATION")
                            setTextAppearance(activity, R.style.TextAppearance_MaterialComponents_Body1)
                        } else {
                            setTextAppearance(R.style.TextAppearance_MaterialComponents_Body1)
                        }
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    })
                }
            }
        }
    }

    fun deleteUnusedMediaFiles() = repository.repositoryScope.launch {
        try {
            val parent = getMediaAccess()
            val mediaRegex = Regex("!\\[.*\\]\\{(.*)\\}")
            val usedMedia = repository.getAllCards().joinToString(";") { card ->
                mediaRegex.findAll(card.question).map { it.groups[1] }.joinToString(";") +
                mediaRegex.findAll(card.answer).map { it.groups[1] }.joinToString(";")
            }
            parent.listFiles()?.let { for (f in it) if (f.name !in usedMedia) f.delete() }
            activity.goodSnack(activity.getString(R.string.delete_unused_success))
        } catch (e: FileNotFoundException) {
            activity.badSnack(activity.getString(R.string.fnf_exception))
        } catch (e: IOException) {
            activity.badSnack(activity.getString(R.string.io_exception))
        }
    }

    private suspend fun collectionToMd(data: List<DeckData>): String {
        var result = ""

        for (d in data) {
            result += when (d) {
                is Course -> "# [${d.symbol}] ${d.name}\n" +
                        collectionToMd(repository.getLessonsFromCourseAsync(d.id))
                is Lesson -> "## [${d.symbol}] ${d.name}\n" +
                        "n|Q|A\n" +
                        "---|---|---\n" +
                        collectionToMd(repository.getCardsFromLessonAsync(d.id))
                is Card -> "${d.within_course_id}|${d.question.escape()}|${d.answer.escape()}\n"
            }
        }

        return result
    }

    private suspend fun mdToCollection(content: String, withOrder: Boolean = false, overwrite: Boolean = false) {
        val lines = content.split("\n")

        var currentCourse = 0
        var currentLesson = 0
        var tableLineCount = 0 // for skipping two first lines in markdown table
        for (l in lines) {
            when {
                l.matches(Regex("^# \\[(.*)] (.*)$")) -> Regex("^# \\[(.*)] (.*)$").find(l)?.groups?.let {
                    currentCourse = repository.addAsync(Course(symbol = it[1]!!.value, name = it[2]!!.value), overwrite).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^# (.*)$")) -> Regex("^# (.*)$").find(l)?.groups?.let {
                    currentCourse = repository.addAsync(Course(symbol = "", name = it[1]!!.value), overwrite).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^## \\[(.*)] (.*)$")) -> Regex("^## \\[(.*)] (.*)$").find(l)?.groups?.let {
                    currentLesson = repository.addAsync(Lesson(course_id = currentCourse, symbol = it[1]!!.value, name = it[2]!!.value), overwrite).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^## (.*)$")) -> Regex("^## (.*)$").find(l)?.groups?.let {
                    currentLesson = repository.addAsync(Lesson(course_id = currentCourse, symbol = "", name = it[1]!!.value), overwrite).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^(.*)\\|(.*)$")) ->
                    if (tableLineCount >= 2) {
                        if (withOrder) {
                            Regex("^(\\d+)\\|(.*)\\|(.*)$").find(l)?.groups?.let {
                                newlyAdded.add(repository.addAsync(Card(course_id = currentCourse, lesson_id = currentLesson, within_course_id = Integer.parseInt(it[1]!!.value), question = it[2]!!.value.unescape(), answer = it[3]!!.value.unescape()), overwrite).await().toInt())
                            }
                        } else {
                            Regex("^(.*)\\|(.*)$").find(l)?.groups?.let {
                                newlyAdded.add(repository.addAsync(Card(course_id = currentCourse, lesson_id = currentLesson, question = it[1]!!.value.unescape(), answer = it[2]!!.value.unescape()), onlyIfNew = false).await().toInt())
                            }
                        }
                    } else tableLineCount++
                else -> tableLineCount = 0
            }
        }
    }

    private fun String.escape(): String = this
        .replace("""\""","""\back;""")
        .replace("""|""","""\pipe;""")
        .replace("\n","""\n;""")
    private fun String.unescape(): String = this
        .replace("""\back;""","""\""")
        .replace("""\pipe;""","""|""")
        .replace("""\n;""","\n")

    private fun linkActivityIntent(code: Int, cont: Continuation<Uri>) {
        activity.onActivityResultListener = object : HyperActivity.OnActivityResultListener {
            override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
                if (requestCode == code
                    && resultCode == Activity.RESULT_OK) {
                    resultData?.data?.also { uri ->
                        cont.resume(uri)
                    }
                }
            }
        }
    }

    private fun getMediaAccess(dir: String = DIR_MEDIA): File {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val parent = File(activity.applicationContext.getExternalFilesDir(null)?.absolutePath + "/$dir/")
            if (parent.exists() || parent.mkdirs()) {
                return parent
            } else throw FileNotFoundException()
        } else throw IOException()
    }

    private fun getUniqueFile(parent: File, name: String): File {
        var file = File(parent, name)
        var count = 0
        while (file.exists()) {
            val ext = name.substringAfterLast(".","")
            val dot = if (ext != "") "." else ""
            file = File(parent, "${name}_$count$dot$ext")
            count++
        }
        if (file.name != name) renamedFiles[name] = file.name
        return file
    }

    private fun getMediaFromCard(card: Card, inParent: File): Array<File>? {
        val mediaRegex = Regex("!\\[.*\\]\\{(.*)\\}")
        val usedMedia = mutableListOf<String>()
        usedMedia.addAll(mediaRegex.findAll(card.question).map { it.groups[1]?.value ?: "" })
        usedMedia.addAll(mediaRegex.findAll(card.answer).map { it.groups[1]?.value ?: "" })
        return inParent.listFiles { _, name: String? -> name in usedMedia}
    }

    private fun writeFromTo(from: InputStream, to: OutputStream) {
        from.use { input ->
            to.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun readStringFrom(from: InputStream): String {
        from.use { return it.bufferedReader().use { r -> r.readText() } }
    }

    private fun writeStringTo(from: String, to: OutputStream) {
        to.use { it.bufferedWriter().use { w -> w.write(from) } }
    }

    private fun createFile(name: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, name)
            type="*/*"
        }
        activity.startActivityForResult(intent, CREATE_FILE)
    }

    private fun openFile(type_: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = type_
        }
        activity.startActivityForResult(intent, OPEN_FILE)
    }

    private suspend fun prepareZip(data: List<DeckData>): List<File> {
        val result = mutableListOf<File>()
        val toParent = getMediaAccess()

        if (data.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val toTraverse: List<Card> = when (data[0]) {
                is Course -> repository.getAllCardsFromCourses(data.map { (it as Course).id }.toIntArray())
                is Lesson -> repository.getAllCardsFromLessons(data.map { (it as Lesson).id }.toIntArray())
                is Card -> data as List<Card>
            }

            for (t in toTraverse) { getMediaFromCard(t,toParent)?.let { result.addAll(it) } }

            val md = File(toParent.absolutePath,"collection.md")
            val mdData = collectionToMd(data)
            writeStringTo(mdData, md.outputStream())
            result.add(md)
        }

        return result.toList()
    }

    private fun makeZipFile(files: List<File>, uri: Uri) {
        contentResolver.openOutputStream(uri)?.let { ZipOutputStream(BufferedOutputStream(it)) }
            .use { output ->
            for (file in files) {
                BufferedInputStream(FileInputStream(file)).use { input ->
                    val entry = ZipEntry(file.name)
                    output?.putNextEntry(entry)
                    output?.let { input.copyTo(it) }
                }
            }
        }
    }

    private suspend fun unZipFile(uri: Uri, withOrder: Boolean = false, overwrite: Boolean = false) {
        val parent = getMediaAccess()
        val path = parent.absolutePath + "/"

        newlyAdded.clear()
        renamedFiles.clear()

        var ze: ZipEntry?
        contentResolver.openInputStream(uri)?.let { ZipInputStream(BufferedInputStream(it)) }
            .use { input ->
                while (input?.nextEntry.also { ze = it } != null) {
                    val fo = if (overwrite) FileOutputStream(path + ze!!.name, false) else getUniqueFile(parent, ze!!.name).outputStream()
                    BufferedOutputStream(fo).use { output ->
                        if (ze!!.name.substringAfterLast(".") == "md" || ze!!.name.substringAfterLast(".") == "hcmd") {
                            input?.bufferedReader()?.readText()?.let { mdToCollection(it, withOrder, overwrite) }
                        } else {
                            input?.copyTo(output)
                        }
                        input?.closeEntry()
                    }
                }
            }

        // if files were renamed, we have to change the corresponding cards too
        val newlyAddedCards = repository.getCardsFromIdsAsync(newlyAdded)
        for (f in renamedFiles) {
            newlyAddedCards.map { card ->
                card.apply {
                    question = Regex("(!\\[.*\\]\\{)${f.key}\\}").replace(question) { it.groups[1]?.value + f.value + "}" }
                    answer = Regex("(!\\[.*\\]\\{)${f.key}\\}").replace(answer) { it.groups[1]?.value + f.value + "}" }
                }
            }
        }

        repository.updateAll(newlyAddedCards)

        newlyAdded.clear()
        renamedFiles.clear()
    }
}