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

import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import onion.w4v3xrmknycexlsd.app.hypercampus.STATUS_DISABLED
import onion.w4v3xrmknycexlsd.app.hypercampus.STATUS_ENABLED
import onion.w4v3xrmknycexlsd.app.hypercampus.browse.Level
import onion.w4v3xrmknycexlsd.app.hypercampus.currentDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HyperRepository @Inject constructor(private val courseDao: CourseDAO, private val lessonDao: LessonDAO, private val cardDao: CardDAO) {
    val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allCourses: LiveData<List<Course>> = courseDao.getAll()
    fun getLessons(courseId: Int): LiveData<List<Lesson>> = lessonDao.getFrom(courseId)
    fun getCards(lessonId: Int): LiveData<List<Card>> = cardDao.getFrom(lessonId)
    fun getCard(cardId: Int): LiveData<Card> = cardDao.getCard(cardId)

    suspend fun getCourse(courseId: Int): Course = courseDao.getCourse(courseId)
    suspend fun getLesson(lessonId: Int): Lesson = lessonDao.getLesson(lessonId)

    suspend fun getLessonsFromCourseAsync(courseId: Int): List<Lesson> = lessonDao.getFromAsync(courseId)
    suspend fun getCardsFromLessonAsync(lessonId: Int): List<Card> = cardDao.getAllFromLesson(lessonId)

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

    suspend fun getAllCards() = cardDao.getAllAsync()

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

    suspend fun countDueCourses(): List<Int> = courseDao.getAllAsync()
        .map { cardDao.countDueInCourse(it.id, currentDate()) }
    suspend fun countNewCards(): List<Int> = courseDao.getAllAsync()
        .map { cardDao.getNewCardsAsync(it.id, (it.new_per_day - it.new_studied_today).coerceAtLeast(0)).size }
    suspend fun countDueLessons(courseId: Int): List<Int> = lessonDao.getFromAsync(courseId)
        .map { cardDao.countDueInLessons(it.id, currentDate()) }

    fun add(data: DeckData) = repositoryScope.launch {
        when (data) {
            is Course -> courseDao.add(data)
            is Lesson -> lessonDao.add(data)
            is Card -> {
                val withinCourse = ((cardDao.getWithinCourseIndex(data.course_id) ?: 0).div(10) + 1)*10
                cardDao.add(data.apply { within_course_id = withinCourse })
            }
        }
    }

    fun addAsync(data: DeckData, onlyIfNew: Boolean = false) = repositoryScope.async {
        if (onlyIfNew) {
            when (data) {
                is Course -> {
                    val coursesWithSameName = courseDao.getCoursesFromName(data.name)
                    if (coursesWithSameName.isEmpty()) courseDao.add(data) else coursesWithSameName[0].id.toLong()
                }
                is Lesson -> {
                    val lessonsWithSameName = lessonDao.getLessonsFromName(data.course_id,data.name)
                    if (lessonsWithSameName.isEmpty()) lessonDao.add(data) else lessonsWithSameName[0].id.toLong()
                }
                is Card -> {
                    val existingCard = cardDao.getCardsFromWithinCourseId(data.course_id,data.within_course_id)
                    if (existingCard.isEmpty()) cardDao.add(data) else {
                        cardDao.updateContent(CardContent(existingCard[0].id, data.lesson_id, data.question, data.answer, data.q_col_name, data.a_col_name, data.info_file))
                        existingCard[0].id.toLong()
                    }
                }
            }
        } else {
            when (data) {
                is Course -> courseDao.add(data)
                is Lesson -> lessonDao.add(data)
                is Card -> cardDao.add(data)
            }
        }
    }

    fun addAllCardsAsync(cards: List<Card>, onlyIfNew: Boolean = false) = repositoryScope.async {
        if (onlyIfNew) {
            val result = mutableListOf<Long>()
            for (card in cards) {
                val existingCard = cardDao.getCardsFromWithinCourseId(card.course_id,card.within_course_id)
                if (existingCard.isEmpty()) result.add(cardDao.add(card)) else {
                    cardDao.updateContent(CardContent(id = existingCard[0].id,
                        lesson_id = card.lesson_id,
                        question = card.question,
                        answer = card.answer,
                        question_column_name = card.q_col_name,
                        answer_column_name = card.a_col_name,
                        info_file = card.info_file))
                    result.add(existingCard[0].id.toLong())
                }
            }
            result
        } else cardDao.addAll(cards)
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

    fun updateAll(cardContents: List<CardContent>) = repositoryScope.launch {
        cardDao.updateAllContents(cardContents)
    }

    fun resetStudied() = repositoryScope.launch {
        courseDao.updateAll(courseDao.getAllAsync().map { it.apply { new_studied_today = 0 } })
    }

    suspend fun getStats(level: Level, dataId: Int): List<IntArray> {
        val result = mutableListOf<IntArray>()
        val totraverse = if (level == Level.COURSES) courseDao.getAllAsync() else lessonDao.getFromAsync(dataId)
        for (unit in totraverse) {
            val unitcards = cardDao.getAllFromCourse(if (unit is Course) unit.id else (unit as Lesson).id)
            val totalnum = unitcards.size
            val newnum = unitcards.filter { card -> card.due == null && card.status == STATUS_ENABLED }.size
            val disablenum = unitcards.filter { card -> card.status == STATUS_DISABLED }.size
            result.add(intArrayOf(totalnum,newnum,disablenum))
        }
        return result
    }
}
