package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import onion.w4v3xrmknycexlsd.app.hypercampus.databinding.DialogHypercampusBinding

class Settings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            val dialogBinding = DialogHypercampusBinding.inflate(layoutInflater)
            dialogBinding.dialogText.text = getString(R.string.about)
            val builder: AlertDialog.Builder? = activity?.let { AlertDialog.Builder(it) }
            builder?.setTitle(R.string.app_name)
                ?.setView(dialogBinding.root)
            val dialog: Dialog? = builder?.create()
            dialog?.show()
            true
        }
    }
}