package onion.w4v3xrmknycexlsd.app.hypercampus

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Settings : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            val builder = activity?.let { MaterialAlertDialogBuilder(it) }
            builder?.setTitle(R.string.app_name)
                ?.setMessage(R.string.about)
                ?.setIcon(R.drawable.ic_logo)

            val dialog: Dialog? = builder?.create()
            dialog?.show()
            dialog?.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
            true
        }
    }
}