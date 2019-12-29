package onion.w4v3xrmknycexlsd.app.hypercampus.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

class HyperViewModel @Inject constructor(private val repository: HyperRepository): ViewModel() {

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

