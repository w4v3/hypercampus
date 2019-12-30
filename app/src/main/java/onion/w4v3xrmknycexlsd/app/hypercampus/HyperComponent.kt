package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import onion.w4v3xrmknycexlsd.app.hypercampus.browse.DeckDataListFragment
import onion.w4v3xrmknycexlsd.app.hypercampus.browse.EditCardFragment
import onion.w4v3xrmknycexlsd.app.hypercampus.data.DbModule
import onion.w4v3xrmknycexlsd.app.hypercampus.data.ViewModelModule
import onion.w4v3xrmknycexlsd.app.hypercampus.review.SrsFragment
import javax.inject.Singleton


@Singleton
@Component(modules = [DbModule::class, ViewModelModule::class])
interface HyperComponent {
    fun inject(fragment: DeckDataListFragment)
    fun inject(fragment: EditCardFragment)
    fun inject(fragment: SrsFragment)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): HyperComponent
    }
}
