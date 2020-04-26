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
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import onion.w4v3xrmknycexlsd.app.hypercampus.MODE_LEARNT
import onion.w4v3xrmknycexlsd.app.hypercampus.STATUS_ENABLED

sealed class DeckData

@Entity(tableName = "courses")
data class Course(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0
                  , @ColumnInfo(name = "symbol") var symbol: String = ""
                  , @ColumnInfo(name = "name") var name: String = ""
                  , @ColumnInfo(name = "new_per_day") var new_per_day: Int = 10
                  , @ColumnInfo(name = "new_studied_today") var new_studied_today: Int = 0
): DeckData()

@Entity(tableName = "lessons")
data class Lesson(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0
                  , @ColumnInfo(name = "course_id") val course_id: Int
                  , @ColumnInfo(name = "symbol") var symbol: String = ""
                  , @ColumnInfo(name = "name") var name: String = ""
): DeckData()

@Entity(tableName = "cards")
data class Card(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0
                , @ColumnInfo(name = "course_id") val course_id: Int
                , @ColumnInfo(name = "lesson_id") val lesson_id: Int
                , @ColumnInfo(name = "question") var question: String = ""
                , @ColumnInfo(name = "answer") var answer: String = ""
                , @ColumnInfo(name = "question_column_name") var q_col_name: String = ""
                , @ColumnInfo(name = "answer_column_name") var a_col_name: String = ""
                , @ColumnInfo(name = "within_course_id") var within_course_id: Int = 0
                , @ColumnInfo(name = "due") var due: Int? = null
                , @ColumnInfo(name = "status") var status: Int = STATUS_ENABLED
                , @ColumnInfo(name = "last_interval") var last_interval: Int = 0
                , @ColumnInfo(name = "sm2_e_factor") var eFactor: Float = 2.5f
                , @ColumnInfo(name = "hc1_last_stability") var former_stability: Float = 3.6f
                , @ColumnInfo(name = "hc1_last_retrievability") var former_retrievability: Float = 0.9f
                , @ColumnInfo(name = "hc1_params") var params_r: List<Float> = listOf(0.7f, 0f, 0f, 0f)
                , @ColumnInfo(name = "hc1_sigma_params") var sigma_params_r: List<Float>
                                                                           = listOf(1f, 0f, 0f, 0f,
                                                                                    0f, 1f, 0f, 0f,
                                                                                    0f, 0f, 1f, 0f,
                                                                                    0f, 0f, 0f, 1f)
                , @ColumnInfo(name = "hc1_params_incorrect") var params_w: List<Float> = listOf(0.7f, 0f, 0f, 0f)
                , @ColumnInfo(name = "hc1_sigma_params_incorrect") var sigma_params_w: List<Float>
                                                                           = listOf(1f, 0f, 0f, 0f,
                                                                                    0f, 1f, 0f, 0f,
                                                                                    0f, 0f, 1f, 0f,
                                                                                    0f, 0f, 0f, 1f)
                , @ColumnInfo(name = "info_file") var info_file: String? = null
): DeckData()

data class CardContent(val id: Int, val lesson_id: Int, val question: String, val answer: String, val info_file: String?) // for content only updates

@Database(entities = [Course::class, Lesson::class, Card::class], version = 9)
@TypeConverters(Converters::class)
abstract class HyperRoom : RoomDatabase() {
    abstract fun courseDao(): CourseDAO
    abstract fun lessonDao(): LessonDAO
    abstract fun cardDao(): CardDAO
}

@Dao
interface CourseDAO {

    @Query("SELECT * FROM courses")
    fun getAll(): LiveData<List<Course>>

    @Query("SELECT * FROM courses")
    suspend fun getAllAsync(): List<Course>

    @Query("SELECT * FROM courses WHERE id=:courseId")
    suspend fun getCourse(courseId: Int): Course

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(course: Course): Long

    @Query("DELETE FROM courses WHERE id=:id")
    suspend fun delete(id: Int)

    @Update
    suspend fun update(course: Course)

    @Update
    suspend fun updateAll(course: List<Course>)
}

@Dao
interface LessonDAO {

    @Query("SELECT * FROM lessons")
    fun getAll(): LiveData<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE id=:lessonId")
    suspend fun getLesson(lessonId: Int): Lesson

    @Query("SELECT * FROM lessons WHERE course_id=:courseId")
    fun getFrom(courseId: Int): LiveData<List<Lesson>>

    @Query("SELECT * FROM lessons WHERE course_id=:courseId")
    suspend fun getFromAsync(courseId: Int): List<Lesson>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(lesson: Lesson): Long

    @Query("DELETE FROM lessons WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM lessons WHERE course_id=:courseId")
    suspend fun deleteCourse(courseId: Int)

    @Update
    suspend fun update(vararg lesson: Lesson)
}

@Dao
interface CardDAO {

    @Query("SELECT * FROM cards")
    fun getAll(): LiveData<List<Card>>

    @Query("SELECT * FROM cards WHERE id=:cardId")
    fun getCard(cardId: Int): LiveData<Card>

    @Query("SELECT * FROM cards WHERE lesson_id=:lessonId")
    fun getFrom(lessonId: Int): LiveData<List<Card>>

    @Query("SELECT * FROM cards")
    suspend fun getAllAsync(): List<Card>

    @Query("SELECT * FROM cards WHERE course_id=:courseId")
    suspend fun getAllFromCourse(courseId: Int): List<Card>

    @Query("SELECT * FROM cards WHERE lesson_id=:lessonId")
    suspend fun getAllFromLesson(lessonId: Int): List<Card>

    @Query("SELECT * FROM cards WHERE due<=:by AND status=$STATUS_ENABLED")
    suspend fun getAllDueBy(by: Int): List<Card>

    @Query("SELECT * FROM cards WHERE course_id=:courseId AND due<=:by AND status=$STATUS_ENABLED")
    suspend fun getAllFromCourseDueBy(courseId: Int, by: Int): List<Card>

    @Query("SELECT * FROM cards WHERE lesson_id=:lessonId AND due<=:by AND status=$STATUS_ENABLED")
    suspend fun getAllFromLessonDueBy(lessonId: Int, by: Int): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE course_id=:courseId AND due<=:by")
    suspend fun countDueInCourse(courseId: Int, by: Int): Int

    @Query("SELECT COUNT(*) FROM cards WHERE lesson_id=:lessonId AND due<=:by")
    suspend fun countDueInLessons(lessonId: Int, by: Int): Int

    @Query("SELECT * FROM cards WHERE due IS NULL AND course_id=:courseId AND status=$STATUS_ENABLED LIMIT :n")
    suspend fun getNewCardsAsync(courseId: Int, n: Int): List<Card>

    @Query("SELECT within_course_id FROM cards WHERE course_id=:courseId ORDER BY within_course_id DESC LIMIT 1")
    suspend fun getWithinCourseIndex(courseId: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(card: Card): Long

    @Query("DELETE FROM cards WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM cards WHERE lesson_id=:lessonId")
    suspend fun deleteLesson(lessonId: Int)

    @Query("DELETE FROM cards WHERE course_id=:courseId")
    suspend fun deleteCourse(courseId: Int)

    @Update
    suspend fun update(vararg card: Card)

    @Update
    suspend fun updateAll(cards: List<Card>)

    @Update(entity = Card::class)
    suspend fun updateContent(cardContent: CardContent)
}

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<Float?>? {
        return value?.split(",")?.map { it.toFloatOrNull() }?.toList()
    }

    @TypeConverter
    fun fromList(list: List<Float?>?): String? {
        return list?.joinToString(",")
    }
}

// MIGRATIONS
//val MIGRATION_4_5 = object : Migration(4,5) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        database.execSQL("ALTER TABLE cards ADD COLUMN hc1_params_incorrect TEXT")
//        database.execSQL("ALTER TABLE cards ADD COLUMN hc1_sigma_params_incorrect TEXT")
//    }
//}
//val MIGRATION_5_6 = object : Migration(5,6) {
//    override fun migrate(database: SupportSQLiteDatabase) {
//        database.execSQL("ALTER TABLE cards ADD COLUMN info_file TEXT")
//    }
//}
