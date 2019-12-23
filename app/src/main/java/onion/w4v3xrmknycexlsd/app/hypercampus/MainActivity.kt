package onion.w4v3xrmknycexlsd.app.hypercampus

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    lateinit var binding: ActivityMainBinding

    private var showMenu = true

    var showing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.appBar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        binding.floatingActionButton.setOnClickListener { navController.navigate(R.id.action_to_srs) }

        navController.addOnDestinationChangedListener { _, dest, _ ->
            if (dest.id == R.id.lessonsList || dest.id == R.id.cardsList) binding.appBar.title = ""
            when (dest.id) {
                R.id.coursesList -> setMenuVisible(true)
                R.id.lessonsList -> setMenuVisible(true)
                R.id.cardsList -> setMenuVisible(true)
                else -> setMenuVisible(false)
            }
        }
    }

    private fun setMenuVisible(visible: Boolean) {
        binding.floatingActionButton.isVisible = visible
        showMenu = visible
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.standard_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.setGroupVisible(R.id.standard_menu_group,showMenu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController)
            || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onBackPressed() {
        if (!showing) {
            super.onBackPressed()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ApplicationLanguageHelper.wrap(newBase!!, "en"))
    }
}

class ApplicationLanguageHelper(base: Context) : ContextThemeWrapper(base, R.style.AppTheme) {
    companion object {

        fun wrap(context: Context, language: String): ContextThemeWrapper {
            var mContext = context
            val config = mContext.resources.configuration
            if (language != "") {
                val locale = Locale(language)
                Locale.setDefault(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setSystemLocale(config, locale)
                } else {
                    setSystemLocaleLegacy(config, locale)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    config.setLayoutDirection(locale)
                    mContext = mContext.createConfigurationContext(config)
                } else {
                    @Suppress("DEPRECATION")
                    mContext.resources.updateConfiguration(config, mContext.resources.displayMetrics)
                }
            }
            return ApplicationLanguageHelper(mContext)
        }

        @Suppress("DEPRECATION")
        fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
            config.locale = locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        fun setSystemLocale(config: Configuration, locale: Locale) {
            config.setLocale(locale)
        }
    }
}