package onion.w4v3xrmknycexlsd.app.hypercampus.data

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DbModule {
    @Provides @Singleton
    internal fun provideDatabase(application: Application): HyperRoom {
        return Room.databaseBuilder(application, HyperRoom::class.java, "hypercampus_database.db")
            .fallbackToDestructiveMigrationFrom(1)
            .build()
    }

    @Provides @Singleton internal fun provideCourseDao(hyperRoom: HyperRoom): CourseDAO { return hyperRoom.courseDao() }
    @Provides @Singleton internal fun provideLessonDao(hyperRoom: HyperRoom): LessonDAO { return hyperRoom.lessonDao() }
    @Provides @Singleton internal fun provideCardDao(hyperRoom: HyperRoom): CardDAO { return hyperRoom.cardDao() }
}
