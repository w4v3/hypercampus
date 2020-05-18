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

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.preference.*
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import onion.w4v3xrmknycexlsd.app.hypercampus.data.HyperDataConverter
import onion.w4v3xrmknycexlsd.lib.sgfcharm.SgfController

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<ListPreference>("sgf_showvariations")?.setOnPreferenceChangeListener { _, newValue ->
            (activity as? HyperActivity)?.sgfController?.showVariations =
                when (newValue) {
                    "1" -> false
                    "2" -> true
                    else -> null
                }
            true
        }
        findPreference<ListPreference>("sgf_interactionmode")?.setOnPreferenceChangeListener { _, newValue ->
            (activity as? HyperActivity)?.sgfController?.interactionMode =
                when (newValue) {
                    "1" -> SgfController.InteractionMode.COUNTERMOVE
                    "2" -> SgfController.InteractionMode.DISABLE
                    else -> SgfController.InteractionMode.FREE_PLAY
                }
            true
        }
        findPreference<SwitchPreferenceCompat>("sgf_showinfo")?.setOnPreferenceChangeListener { _, newValue ->
            HyperDataConverter.sgfShowText = newValue as Boolean
            true
        }
        findPreference<SwitchPreferenceCompat>("sgf_showbuttons")?.setOnPreferenceChangeListener { _, newValue ->
            HyperDataConverter.sgfShowButtons = newValue as Boolean
            true
        }
        findPreference<ListPreference>("sgf_colortheme")?.setOnPreferenceChangeListener { _, newValue ->
            HyperDataConverter.sgfColors = newValue as String
            true
        }
        with(findPreference<SeekBarPreference>("font_size")) {
            this?.setOnPreferenceChangeListener { preference, newValue ->
                when (newValue) {
                    -2 -> preference.summary = getString(R.string.font_size_tiny)
                    -1 -> preference.summary = getString(R.string.font_size_small)
                    0 -> preference.summary = getString(R.string.font_size_medium)
                    1 -> preference.summary = getString(R.string.font_size_large)
                    2 -> preference.summary = getString(R.string.font_size_huge)
                }
                HyperDataConverter.textSizeFactor = (newValue as Int).toFloat()
                true
            }
            this?.summary =
                when (this?.value) {
                    -2 -> getString(R.string.font_size_tiny)
                    -1 -> getString(R.string.font_size_small)
                    0 -> getString(R.string.font_size_medium)
                    1 -> getString(R.string.font_size_large)
                    2 -> getString(R.string.font_size_huge)
                    else -> ""
                }
        }
        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            val builder = activity?.let { MaterialAlertDialogBuilder(it) }
            builder?.setTitle(R.string.app_name)
                ?.setMessage(R.string.about)
                ?.setIcon(R.drawable.ic_logo)

            val dialog: Dialog? = builder?.create()
            dialog?.show()
            dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod =
                LinkMovementMethod.getInstance()
            true
        }
        findPreference<Preference>("remove_media")?.setOnPreferenceClickListener {
            HyperDataConverter(activity as HyperActivity).deleteUnusedMediaFiles()
            true
        }
        findPreference<SwitchPreferenceCompat>("set_lang")?.setOnPreferenceChangeListener { _, _ ->
            startActivity(Intent(activity, HyperActivity::class.java))
            true
        }
        findPreference<Preference>("licence")?.setOnPreferenceClickListener {
            startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
            true
        }
    }
}