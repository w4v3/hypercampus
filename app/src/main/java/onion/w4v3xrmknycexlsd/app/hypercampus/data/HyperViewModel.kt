package onion.w4v3xrmknycexlsd.app.hypercampus.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

class HyperViewModel @Inject constructor(private val repository: HyperRepository): ViewModel() {

    val allCourses: LiveData<List<Course>> = repository.allCourses
    fun getCourseLessons(courseId: Int): LiveData<List<Lesson>> = repository.getLessons(courseId)
    fun getLessonCards(lessonId: Int): LiveData<List<Card>> = repository.getCards(lessonId)
    fun getCard(cardId: Int): LiveData<Card> =  repository.getCard(cardId)

    suspend fun getCourseAsync(courseId: Int): Course = repository.getCourse(courseId)
    suspend fun getLessonAsync(lessonId: Int): Lesson = repository.getLesson(lessonId)

    suspend fun getDueFromCoursesAsync(premature: Boolean, courses: IntArray?): List<Card> =
        if (premature) repository.getAllCardsFromCourses(courses) else repository.getAllDueCardsFromCourses(courses)
    suspend fun getDueFromLessonsAsync(premature: Boolean, lessons: IntArray?): List<Card> =
        if (premature) repository.getAllCardsFromLessons(lessons) else repository.getAllDueCardsFromLessons(lessons)
    suspend fun getNewCardsFromCoursesAsync(courses: IntArray?): List<Card> =
        repository.getNewCards(courses)

    suspend fun countDuePerCourseAsync(): List<Int> = repository.countDueCourses()
    suspend fun countNewPerCourseAsync(): List<Int> = repository.countNewCards()
    suspend fun countDuePerLessonAsync(courseId: Int): List<Int> = repository.countDueLessons(courseId)

    fun add(data: DeckData) = repository.add(data)
    fun delete(data: DeckData) = repository.delete(data)
    fun update(data: DeckData) = repository.update(data)

    fun resetStudied() = repository.resetStudied()
}

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(
        factory: HyperViewModelFactory
    ): ViewModelProvider.Factory
    @Binds
    @IntoMap
    @ViewModelKey(HyperViewModel::class)
    abstract fun bindHyperViewModel(
        hyperViewModel: HyperViewModel
    ): ViewModel
}

@Singleton
class HyperViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = creators[modelClass] ?: creators.entries.firstOrNull {
            modelClass.isAssignableFrom(it.key)
        }?.value ?: throw IllegalArgumentException("unknown model class $modelClass")
        try {
            @Suppress("UNCHECKED_CAST")
            return creator.get() as T
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }
}

