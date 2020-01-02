package onion.w4v3xrmknycexlsd.app.hypercampus.data

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import onion.w4v3xrmknycexlsd.app.hypercampus.DIR_MEDIA
import onion.w4v3xrmknycexlsd.app.hypercampus.FILE_AUDIO
import onion.w4v3xrmknycexlsd.app.hypercampus.HyperActivity
import onion.w4v3xrmknycexlsd.app.hypercampus.HyperApp
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


// intent codes
const val CREATE_FILE = 1
const val OPEN_FILE = 2

class HyperDataConverter (private val activity: HyperActivity){
    @Inject lateinit var repository: HyperRepository
    private val contentResolver = activity.applicationContext.contentResolver

    init {
        (activity.applicationContext as HyperApp).hyperComponent.inject(this)
    }

    suspend fun collectionToFile(data: List<DeckData>, withMedia: Boolean = false) {
        val uri: Uri? = suspendCoroutine { cont ->
            activity.onActivityResultListener = object : HyperActivity.OnActivityResultListener {
                override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
                    if (requestCode == CREATE_FILE
                        && resultCode == Activity.RESULT_OK) {
                        resultData?.data?.also { uri ->
                            cont.resume(uri)
                        }
                    }
                }
            }
            if (withMedia) createFile("collection.zip") else createFile("collection.md")
        }

        if (withMedia) {
            val files = prepareZip(data)
            uri?.let { makeZipFile(files,it) }
        } else {
            uri?.let { writeToFile(collectionToMd(data),it) }
        }
    }

    suspend fun addMedia(id: Int, type: String): String {
        val uri: Uri? = suspendCoroutine { cont ->
            activity.onActivityResultListener = object : HyperActivity.OnActivityResultListener {
                override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
                    if (requestCode == OPEN_FILE
                        && resultCode == Activity.RESULT_OK) {
                        resultData?.data?.also { uri ->
                            cont.resume(uri)
                        }
                    } else {
                        cont.resume(null)
                    }
                }
            }
            openFile(type)
        }

        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val toParent = File(activity.applicationContext.getExternalFilesDir(null)?.absolutePath + "/$DIR_MEDIA/")

            if (!toParent.exists() && !toParent.mkdirs()) {
                Log.e("e...to", "Directory not created")
            } else {
                val ind = toParent.listFiles { _: File?, name: String? -> name?.matches(Regex("^${id}_.*$")) ?: false }?.size?.plus(1)
                val to = File(toParent.absolutePath,"${id}_$ind")
                uri?.let {
                    contentResolver.openInputStream(it).use { input ->
                        to.outputStream().use { output ->
                            input?.copyTo(output)
                        }
                    }
                }
                return "![${if (type == FILE_AUDIO) "audio" else "image"}]{${id}_$ind}"
            }
        }

        return "![${if (type == FILE_AUDIO) "audio" else "image"}]{${uri.toString()}}"
    }

    fun fileToCollection(uri: Uri) = repository.repositoryScope.launch(Dispatchers.IO) {
        val cursor = contentResolver.query(uri,null,null,null,null)
        cursor?.moveToFirst()
        val filename = cursor?.getString(2)
        cursor?.close()
        when (filename?.substringAfterLast(".")) {
            "md" -> {
                val content = readTextFromUri(uri)
                mdToCollection(content)
            }
            "zip" -> {
                unZipFile(uri)
            }
        }
    }

    private suspend fun collectionToMd(data: List<DeckData>): String {
        var result = ""

        for (d in data) {
            result += when (d) {
                is Course -> "# [${d.symbol}] ${d.name}\n" +
                        collectionToMd(repository.getLessonsFromCourseAsync(d.id))
                is Lesson -> "## [${d.symbol}] ${d.name}\n" +
                        "Q|A\n" +
                        "---|---\n" +
                        collectionToMd(repository.getCardsFromLessonAsync(d.id))
                is Card -> "${d.question}|${d.answer}\n"
            }
        }

        return result
    }

    private suspend fun mdToCollection(content: String) {
        val lines = content.split("\n")

        var currentCourse = 0
        var currentLesson = 0
        var tableLineCount = 0 // for skipping two first lines in markdown table
        for (l in lines) {
            when {
                l.matches(Regex("^# \\[(.*)] (.*)$")) -> Regex("^# \\[(.*)] (.*)$").find(l)?.groups?.let {
                    currentCourse = repository.addAsync(Course(symbol = it[1]!!.value, name = it[2]!!.value)).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^# (.*)$")) -> Regex("^# (.*)$").find(l)?.groups?.let {
                    currentCourse = repository.addAsync(Course(symbol = "", name = it[1]!!.value)).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^## \\[(.*)] (.*)$")) -> Regex("^## \\[(.*)] (.*)$").find(l)?.groups?.let {
                    currentLesson = repository.addAsync(Lesson(course_id = currentCourse, symbol = it[1]!!.value, name = it[2]!!.value)).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^## (.*)$")) -> Regex("^## (.*)$").find(l)?.groups?.let {
                    currentLesson = repository.addAsync(Lesson(course_id = currentCourse, symbol = "", name = it[1]!!.value)).await().toInt()
                    tableLineCount = 0
                }
                l.matches(Regex("^(.*)\\|(.*)$")) ->
                    if (tableLineCount >= 2) {
                        Regex("^(.*)\\|(.*)$").find(l)?.groups?.let {
                            repository.add(Card(course_id = currentCourse, lesson_id = currentLesson, question = it[1]!!.value, answer = it[2]!!.value))
                        }
                    } else tableLineCount++
                else -> tableLineCount = 0
            }
        }
    }

    private fun convertToViews(toConvert: String, root: ViewGroup): ViewGroup {
        val mediaRegex = Regex(""""!\[]\{.*\..*}]""")
        val groups: List<String> = toConvert.split(mediaRegex)

        for (g in groups) {
            if (g.matches(mediaRegex)) {
                root.addView(ImageView(activity).apply { setImageURI(g.toUri()) })
            } else {
                root.addView(TextView(activity).apply { text = g })
            }
        }
        return root
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

    private fun writeToFile(content: String, uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { itt ->
                    itt.write(content.toByteArray())
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private suspend fun prepareZip(data: List<DeckData>): List<File> {
        val result = mutableListOf<File>()

        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && data.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val toTraverse: List<Card> = when (data[0]) {
                is Course -> repository.getAllCardsFromCourses(data.map { (it as Course).id }.toIntArray())
                is Lesson -> repository.getAllCardsFromLessons(data.map { (it as Lesson).id }.toIntArray())
                is Card -> data as List<Card>
            }

            val toParent = File(activity.applicationContext.getExternalFilesDir(null)?.absolutePath + "/$DIR_MEDIA/")

            if (!toParent.exists()) {
                Log.e("e...to", "Directory not existing")
            } else {
                for (t in toTraverse) {
                    toParent.listFiles { _: File?, name: String? -> name?.matches(Regex("^${t.id}_.*$")) ?: false }?.let {
                        result.addAll(it)
                    }
                }

                val md = File(toParent.absolutePath,"collection.md")
                md.outputStream().use { output -> output.write(collectionToMd(data).toByteArray()) }
                result.add(md)
            }
        }
        return result.toList()
    }

    private fun makeZipFile(files: List<File>, uri: Uri) {
        val out = contentResolver.openOutputStream(uri)?.let { ZipOutputStream(BufferedOutputStream(it)) }

        for (file in files) {
            val fi = FileInputStream(file)
            val origin = BufferedInputStream(fi)
            val entry = ZipEntry(file.name)
            out?.putNextEntry(entry)
            out?.let { origin.copyTo(it) }
            origin.close()
        }

        out?.close()
    }

    private suspend fun unZipFile(uri: Uri) {
        val path = activity.applicationContext.getExternalFilesDir(null)?.absolutePath + "/$DIR_MEDIA/"
        if (!File(path).mkdirs()) Log.e("uhoh","smth went wrong")
        val inn = contentResolver.openInputStream(uri)?.let { ZipInputStream(BufferedInputStream(it)) }
        var ze: ZipEntry?
        inn.use { reader ->
            while (inn?.nextEntry.also { ze = it } != null) {
                val fo = FileOutputStream(path + ze!!.name)
                val target = BufferedOutputStream(fo)
                if (ze!!.name.substringAfterLast(".") == "md") {
                    reader?.bufferedReader()?.readText()?.let { mdToCollection(it) }
                } else {
                    reader?.copyTo(target)
                    inn?.closeEntry()
                }
            }
        }
    }


    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.appendln(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}