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

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected
import androidx.preference.PreferenceManager
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.image.picasso.PicassoImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.recycler.table.TableEntryPlugin
import io.noties.markwon.urlprocessor.UrlProcessorRelativeToAbsolute
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.ActivityMainBinding
import onion.w4v3xrmknycexlsd.lib.sgfcharm.SgfController
import java.util.*


class HyperActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    lateinit var binding: ActivityMainBinding

    var mediaPlayer: MediaPlayer? = null
    lateinit var markwon: Markwon
    lateinit var sgfController: SgfController

    var onActivityResultListener: OnActivityResultListener? = null

    private var showMenu = true

    var showing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)

        markwon = Markwon.builder(this)
            .usePlugin(TableEntryPlugin.create(this))
            .usePlugin(PicassoImagesPlugin.create(this))
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(resources.configuration.fontScale * 64) { builder ->
                builder.inlinesEnabled(
                    true
                )
            })
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.urlProcessor(
                        UrlProcessorRelativeToAbsolute(
                            "file://${applicationContext.getExternalFilesDir(
                                null
                            )?.absolutePath}/media/"
                        )
                    )
                }

                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder.headingBreakHeight(0)
                }
            })
            .build()

        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            sgfController = SgfController().apply {
                showVariations = when (getString("sgf_showvariations", "0")) {
                    "1" -> false
                    "2" -> true
                    else -> null
                }

                interactionMode = when (getString("sgf_interactionmode", "0")) {
                    "1" -> SgfController.InteractionMode.COUNTERMOVE
                    "2" -> SgfController.InteractionMode.DISABLE
                    else -> SgfController.InteractionMode.FREE_PLAY
                }
            }

            HyperDataConverter.readPrefs(this)
        }

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
            mediaPlayer?.reset()
        }

        setSupportActionBar(binding.appBar)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        binding.floatingActionButton.setOnClickListener { navController.navigate(R.id.action_to_srs) }

        intent.data?.also { HyperDataConverter(this).fileToCollection(it); intent.data = null }
    }

    override fun onStart() {
        super.onStart()

        mediaPlayer = MediaPlayer().apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_MUSIC)
            } else {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(attributes)
            }
        }
    }

    override fun onStop() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onStop()
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
        menu?.findItem(R.id.app_bar_search)?.isVisible = showMenu
        menu?.findItem(R.id.app_bar_add)?.isVisible = showMenu
        menu?.findItem(R.id.app_bar_stats)?.isVisible = showMenu
        menu?.findItem(R.id.settings)?.isVisible = showMenu
        menu?.findItem(R.id.app_bar_info)?.isVisible = false
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.app_bar_help) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/w4v3/hypercampus")
                )
            )
        }
        return item.onNavDestinationSelected(navController)
                || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    interface OnBackPressedListener {
        fun doOnBackPressed(): Boolean
    }

    var onBackPressedListener = object : OnBackPressedListener {
        override fun doOnBackPressed(): Boolean {
            return !showing
        }
    }

    override fun onBackPressed() {
        if (onBackPressedListener.doOnBackPressed()) super.onBackPressed()
    }

    override fun attachBaseContext(newBase: Context?) {
        val locale = when {
            PreferenceManager.getDefaultSharedPreferences(newBase)
                .getBoolean("set_lang", false) -> "en"
            Locale.getDefault().language in SUPPORTED_LANG -> Locale.getDefault().language
            else -> "en"
        }
        super.attachBaseContext(ApplicationLanguageHelper.wrap(newBase!!, locale))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        onActivityResultListener?.onActivityResult(requestCode, resultCode, resultData)
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    interface OnActivityResultListener {
        fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?)
    }
}

