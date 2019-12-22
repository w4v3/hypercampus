package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.*
import java.util.*

@Database(entities = [Course::class, Lesson::class, Card::class], version = 1, exportSchema = false)
abstract class HyperRoom : RoomDatabase() {

    abstract fun courseDao(): CourseDAO
    abstract fun lessonDao(): LessonDAO
    abstract fun cardDao(): CardDAO

    companion object {
        @Volatile
        private var INSTANCE: HyperRoom? = null

        private fun getDatabase(context: Context): HyperRoom {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        HyperRoom::class.java,
                        "hypercampus_database"
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        fun getRepository(context: Context): HyperRepository {
            val instance = getDatabase(context)
            return HyperRepository(instance.courseDao(),instance.lessonDao(),instance.cardDao())
        }
    }
}

class HyperRepository(private val courseDao: CourseDAO, private val lessonDao: LessonDAO, private val cardDao: CardDAO) {
    val allCourses: LiveData<List<Course>> = courseDao.getAll()
    fun getLessons(courseId: Int) = lessonDao.getFrom(courseId)
    fun getCards(lessonId: Int) = cardDao.getFrom(lessonId)

    suspend fun getCourse(courseId: Int) = courseDao.getCourse(courseId)
    suspend fun getLesson(lessonId: Int) = lessonDao.getLesson(lessonId)
    fun getCard(cardId: Int) = cardDao.getCard(cardId)

    suspend fun getAllCardsFromCourses(courses: IntArray?): List<Card> =
        if (courses==null) cardDao.getAllAsync()
        else {
            val result = mutableListOf<Card>()
            for (course in courses) result.addAll(cardDao.getAllFromCourse(course))
            result.toList()
        }

    suspend fun getAllDueCardsFromCourses(courses: IntArray?): List<Card> =
        if (courses==null) cardDao.getAllDueBy(currentDate())
        else {
            val result = mutableListOf<Card>()
            for (course in courses) result.addAll(cardDao.getAllFromCourseDueBy(course, currentDate()))
            result.toList()
        }

    suspend fun getAllCardsFromLessons(lessons: IntArray?): List<Card> =
        if (lessons==null) cardDao.getAllAsync()
        else {
            val result = mutableListOf<Card>()
            for (lesson in lessons) result.addAll(cardDao.getAllFromLesson(lesson))
            result.toList()
        }

    suspend fun getAllDueCardsFromLessons(lessons: IntArray?): List<Card> =
        if (lessons==null) cardDao.getAllDueBy(currentDate())
        else {
            val result = mutableListOf<Card>()
            for (lesson in lessons) result.addAll(cardDao.getAllFromLessonDueBy(lesson, currentDate()))
            result.toList()
        }

    suspend fun getNewCards(courses: IntArray?): List<Card> {
        val courseList = mutableListOf<Course>()
        if (courses==null) {
            courseList.addAll(courseDao.getAllAsync())
        }
        else {
            for (course in courses) courseList.add(courseDao.getCourse(course))
        }
        val result = mutableListOf<Card>()
        for (course in courseList) result.addAll(getNewCardsCourse(course))
        return result.toList()
    }

    private suspend fun getNewCardsCourse(course: Course): List<Card> {
        return cardDao.getNewCards(course.id,course.new_per_day - course.new_studied_today)
    }

    suspend fun countNewCards(): List<Int> {
        return courseDao.getAllAsync().map { getNewCardsCourse(it).size }
    }

    suspend fun countDueCourses(): List<Int> {
        val result = mutableListOf<Int>()
        for (c in courseDao.getAllAsync()) {
            result.add(cardDao.countDueInCourse(c.id, currentDate()))
        }
        return result.toList()
    }

    suspend fun countDueLessons(courseId: Int): List<Int> {
        val result = mutableListOf<Int>()
        for (l in lessonDao.getFromAsync(courseId)) {
            result.add(cardDao.countDueInLesson(l.id, currentDate()))
        }
        return result.toList()
    }

    suspend fun add(data: DeckData) {
        when (data) {
            is Course -> courseDao.add(data)
            is Lesson -> lessonDao.add(data)
            is Card -> cardDao.add(data)
        }
    }

    suspend fun delete(data: DeckData){
        when (data) {
            is Course -> {
                courseDao.delete(data.id)
                lessonDao.deleteCourse(data.id)
                cardDao.deleteCourse(data.id)
            }
            is Lesson -> {
                lessonDao.delete(data.id)
                cardDao.deleteLesson(data.id)
            }
            is Card -> cardDao.delete(data.id)
        }
    }

    suspend fun update(data: DeckData){
        when (data) {
            is Course -> courseDao.update(data)
            is Lesson -> lessonDao.update(data)
            is Card -> cardDao.update(data)
        }
    }

    private fun currentDate(): Int {
        val date = Calendar.getInstance(Locale.US)
        return date.get(Calendar.YEAR) * 10000 + (date.get(Calendar.MONTH) + 1) * 100 + date.get(Calendar.DAY_OF_MONTH)
    }
}

class HyperViewModel(application: Application) : AndroidViewModel(application) {
    private var repository: HyperRepository = HyperRoom.getRepository(application)

    val allCourses: LiveData<List<Course>> = repository.allCourses
    fun getCourseAsync(courseId: Int) = viewModelScope.async { repository.getCourse(courseId) }
    fun getCourseLessons(courseId: Int): LiveData<List<Lesson>> = repository.getLessons(courseId)
    fun getLessonAsync(lessonId: Int) = viewModelScope.async { repository.getLesson(lessonId) }
    fun getLessonCards(lessonId: Int): LiveData<List<Card>> = repository.getCards(lessonId)
    fun getCard(cardId: Int) =  repository.getCard(cardId)

    fun getDueAsync() = viewModelScope.async { repository.countDueCourses() }
    fun getNewAsync() = viewModelScope.async { repository.countNewCards() }
    fun getCourseDueAsync(courseId: Int) = viewModelScope.async { repository.countDueLessons(courseId) }

    fun getDueFromCoursesAsync(premature: Boolean, courses: IntArray?) = viewModelScope.async {
        if (premature) repository.getAllCardsFromCourses(courses) else repository.getAllDueCardsFromCourses(courses)
    }

    fun getDueFromLessonsAsync(premature: Boolean, lessons: IntArray?) = viewModelScope.async {
        if (premature) repository.getAllCardsFromLessons(lessons) else repository.getAllDueCardsFromLessons(lessons)
    }

    fun getNewCardsAsync(courses: IntArray?) = viewModelScope.async { repository.getNewCards(courses) }

    fun add(data: DeckData) = viewModelScope.launch {
        repository.add(data)
    }

    fun delete(data: DeckData) = viewModelScope.launch {
        repository.delete(data)
    }

    fun update(data: DeckData) = viewModelScope.launch {
        repository.update(data)
    }

    fun runAsync(task: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch {
        task(viewModelScope)
    }
}
