package onion.w4v3xrmknycexlsd.app.hypercampus.data

import androidx.lifecycle.LiveData
import androidx.room.*
import onion.w4v3xrmknycexlsd.app.hypercampus.MODE_LEARNT
import onion.w4v3xrmknycexlsd.app.hypercampus.STATUS_ENABLED

sealed class DeckData

@Entity(tableName = "courses")
data class Course(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0
                  , @ColumnInfo(name = "symbol") var symbol: String = ""
                  , @ColumnInfo(name = "name") var name: String = ""
                  , @ColumnInfo(name = "new_per_day") var new_per_day: Int = 10
                  , @ColumnInfo(name = "new_studied_today") var new_studied_today: Int = 0
                  , @ColumnInfo(name = "info_file") var info_file: String = ""
                  , @ColumnInfo(name = "user_order") var user_order: Int = id
): DeckData()

@Entity(tableName = "lessons")
data class Lesson(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0
                  , @ColumnInfo(name = "course_id") val course_id: Int
                  , @ColumnInfo(name = "symbol") var symbol: String = ""
                  , @ColumnInfo(name = "name") var name: String = ""
                  , @ColumnInfo(name = "user_order") var user_order: Int = id
): DeckData()

@Entity(tableName = "cards")
data class Card(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0
                , @ColumnInfo(name = "course_id") val course_id: Int
                , @ColumnInfo(name = "lesson_id") val lesson_id: Int
                , @ColumnInfo(name = "question") var question: String = ""
                , @ColumnInfo(name = "answer") var answer: String = ""
                , @ColumnInfo(name = "due") var due: Int? = null
                , @ColumnInfo(name = "learning_mode") var learning_mode: Int = MODE_LEARNT
                , @ColumnInfo(name = "status") var status: Int = STATUS_ENABLED
                , @ColumnInfo(name = "last_interval") var last_interval: Int = 0
                , @ColumnInfo(name = "sm2_e_factor") var eFactor: Float = 2.5f
                , @ColumnInfo(name = "hc1_stability") var stability: Float = 1f
                , @ColumnInfo(name = "hc1_params") var params: List<Float> = listOf()
                , @ColumnInfo(name = "hc1_sigma_params") var sigma_params: List<List<Float>> = listOf()
                , @ColumnInfo(name = "user_order") var user_order: Int = id
): DeckData()

@Database(entities = [Course::class, Lesson::class, Card::class], version = 2)
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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

    @Query("SELECT * FROM cards WHERE due<=:by")
    suspend fun getAllDueBy(by: Int): List<Card>

    @Query("SELECT * FROM cards WHERE course_id=:courseId AND due<=:by")
    suspend fun getAllFromCourseDueBy(courseId: Int, by: Int): List<Card>

    @Query("SELECT * FROM cards WHERE lesson_id=:lessonId AND due<=:by")
    suspend fun getAllFromLessonDueBy(lessonId: Int, by: Int): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE course_id=:courseId AND due<=:by")
    suspend fun countDueInCourse(courseId: Int, by: Int): Int

    @Query("SELECT COUNT(*) FROM cards WHERE lesson_id=:lessonId AND due<=:by")
    suspend fun countDueInLessons(lessonId: Int, by: Int): Int

    @Query("SELECT * FROM cards WHERE due IS NULL AND course_id=:courseId LIMIT :n")
    suspend fun getNewCardsAsync(courseId: Int, n: Int): List<Card>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(card: Card): Long

    @Query("DELETE FROM cards WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM cards WHERE lesson_id=:lessonId")
    suspend fun deleteLesson(lessonId: Int)

    @Query("DELETE FROM cards WHERE course_id=:courseId")
    suspend fun deleteCourse(courseId: Int)

    @Update
    suspend fun update(vararg card: Card)
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

    @TypeConverter
    fun fromStringToMatrix(value: String?): List<List<Float?>?>? {
        return value?.split(";")?.map { it.split(",").map { v -> v.toFloatOrNull() }.toList() }?.toList()
    }

    @TypeConverter
    fun fromMatrixToList(list: List<List<Float?>?>?): String? {
        return list?.map { it?.joinToString(",") }?.joinToString(";")
    }
}
