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

package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import onion.w4v3xrmknycexlsd.app.hypercampus.browse.DeckDataListFragment
import onion.w4v3xrmknycexlsd.app.hypercampus.browse.EditCardFragment
import onion.w4v3xrmknycexlsd.app.hypercampus.data.DbModule
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import onion.w4v3xrmknycexlsd.app.hypercampus.data.ViewModelModule
import onion.w4v3xrmknycexlsd.app.hypercampus.review.SrsFragment
import javax.inject.Singleton


@Singleton
@Component(modules = [DbModule::class, ViewModelModule::class])
interface HyperComponent {
    fun inject(fragment: DeckDataListFragment)
    fun inject(fragment: EditCardFragment)
    fun inject(fragment: SrsFragment)
    fun inject(converter: HyperDataConverter)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): HyperComponent
    }
}
