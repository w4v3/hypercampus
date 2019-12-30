package onion.w4v3xrmknycexlsd.app.hypercampus.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import onion.w4v3xrmknycexlsd.app.hypercampus.currentDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HyperRepository @Inject constructor(private val courseDao: CourseDAO, private val lessonDao: LessonDAO, private val cardDao: CardDAO) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val allCourses: LiveData<List<Course>> = courseDao.getAll()
    fun getLessons(courseId: Int): LiveData<List<Lesson>> = lessonDao.getFrom(courseId)
    fun getCards(lessonId: Int): LiveData<List<Card>> = cardDao.getFrom(lessonId)
    fun getCard(cardId: Int): LiveData<Card> = cardDao.getCard(cardId)

    suspend fun getCourse(courseId: Int): Course = courseDao.getCourse(courseId)
    suspend fun getLesson(lessonId: Int): Lesson = lessonDao.getLesson(lessonId)

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
            for (course in courses) result.addAll(cardDao.getAllFromCourseDueBy(course,
                currentDate()
            ))
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
            for (lesson in lessons) result.addAll(cardDao.getAllFromLessonDueBy(lesson,
                currentDate()
            ))
            result.toList()
        }

    private suspend fun getNewCardsCourse(course: Course): List<Card> {
        return cardDao.getNewCardsAsync(course.id,course.new_per_day - course.new_studied_today)
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

    suspend fun countDueCourses(): List<Int> = cardDao.countDueInCourses(courseDao.getAllAsync().map { it.id }, currentDate())

    suspend fun countNewCards(): List<Int> = courseDao.getAllAsync()
        .map { cardDao.getNewCardsAsync(it.id, (it.new_per_day - it.new_studied_today).coerceAtLeast(0)).size }

    suspend fun countDueLessons(courseId: Int): List<Int> = cardDao.countDueInLessons(lessonDao.getFromAsync(courseId).map { it.id }, currentDate())

    fun add(data: DeckData) = repositoryScope.launch {
        when (data) {
            is Course -> courseDao.add(data)
            is Lesson -> lessonDao.add(data)
            is Card -> cardDao.add(data)
        }
    }

    fun delete(data: DeckData) = repositoryScope.launch {
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

    fun update(data: DeckData) = repositoryScope.launch {
        when (data) {
            is Course -> courseDao.update(data)
            is Lesson -> lessonDao.update(data)
            is Card -> cardDao.update(data)
        }
    }
}
