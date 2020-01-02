package onion.w4v3xrmknycexlsd.app.hypercampus

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.ActivityMainBinding

class HyperActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    lateinit var binding: ActivityMainBinding

    var onActivityResultListener: OnActivityResultListener? = null

    private var showMenu = true

    var showing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, dest, _ ->
            if (dest.id == R.id.lessonsList || dest.id == R.id.cardsList) binding.appBar.title = ""
            when (dest.id) {
                R.id.coursesList -> setMenuVisible(true)
                R.id.lessonsList -> setMenuVisible(true)
                R.id.cardsList -> setMenuVisible(true)
                else -> setMenuVisible(false)
            }
        }

        setSupportActionBar(binding.appBar)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        binding.floatingActionButton.setOnClickListener { navController.navigate(R.id.action_to_srs) }

        intent.data?.also { HyperDataConverter(this).fileToCollection(it) }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        onActivityResultListener?.onActivityResult(requestCode,resultCode,resultData)
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    interface OnActivityResultListener {
        fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?)
    }
}

