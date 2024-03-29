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

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DbModule {
    @Provides
    @Singleton
    internal fun provideDatabase(application: Application): HyperRoom {
        return Room.databaseBuilder(application, HyperRoom::class.java, "hypercampus_database.db")
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9)
            .build()
    }

    @Provides
    @Singleton
    internal fun provideCourseDao(hyperRoom: HyperRoom): CourseDAO {
        return hyperRoom.courseDao()
    }

    @Provides
    @Singleton
    internal fun provideLessonDao(hyperRoom: HyperRoom): LessonDAO {
        return hyperRoom.lessonDao()
    }

    @Provides
    @Singleton
    internal fun provideCardDao(hyperRoom: HyperRoom): CardDAO {
        return hyperRoom.cardDao()
    }
}
