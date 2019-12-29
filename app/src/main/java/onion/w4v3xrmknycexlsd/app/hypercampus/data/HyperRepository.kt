package onion.w4v3xrmknycexlsd.app.hypercampus.data

import androidx.lifecycle.LiveData
import onion.w4v3xrmknycexlsd.app.hypercampus.currentDate
import javax.inject.Inject

class HyperRepository @Inject constructor(private val courseDao: CourseDAO, private val lessonDao: LessonDAO, private val cardDao: CardDAO) {
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
            result.add(cardDao.countDueInCourse(c.id,
                currentDate()
            ))
        }
        return result.toList()
    }

    suspend fun countDueLessons(courseId: Int): List<Int> {
        val result = mutableListOf<Int>()
        for (l in lessonDao.getFromAsync(courseId)) {
            result.add(cardDao.countDueInLesson(l.id,
                currentDate()
            ))
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
}
