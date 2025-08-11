package com.qrtasima.ui.settings

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qrtasima.R
import com.qrtasima.databinding.DialogSettingsBinding
import com.qrtasima.util.AppPreferences
import com.qrtasima.util.SlugInputFilter
import com.qrtasima.util.Slugify
import com.qrtasima.util.ThemeManager

class SettingsDialogFragment : DialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextUserIdentifier.filters = arrayOf(SlugInputFilter(), InputFilter.LengthFilter(50))
        loadCurrentSettings()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ayarlar")
            .setView(onCreateView(layoutInflater, null, savedInstanceState))
            .setPositiveButton("Kaydet") { _, _ ->
                saveSettings()
            }
            .setNegativeButton("İptal", null)
            .create()
    }

    private fun loadCurrentSettings() {
        val currentUserIdentifier = AppPreferences.getUserIdentifier(requireContext())
        binding.editTextUserIdentifier.setText(currentUserIdentifier)

        when (ThemeManager.getCurrentTheme(requireContext())) {
            ThemeManager.Theme.LIGHT -> binding.radioGroupTheme.check(R.id.radio_light)
            ThemeManager.Theme.DARK -> binding.radioGroupTheme.check(R.id.radio_dark)
            ThemeManager.Theme.SYSTEM -> binding.radioGroupTheme.check(R.id.radio_system)
        }
    }

    private fun saveSettings() {
        val selectedTheme = when (binding.radioGroupTheme.checkedRadioButtonId) {
            R.id.radio_light -> ThemeManager.Theme.LIGHT
            R.id.radio_dark -> ThemeManager.Theme.DARK
            else -> ThemeManager.Theme.SYSTEM
        }
        ThemeManager.setTheme(requireContext(), selectedTheme)

        val oldIdentifier = AppPreferences.getUserIdentifier(requireContext())
        val newIdentifierRaw = binding.editTextUserIdentifier.text.toString()
        val newIdentifierSlug = Slugify.from(newIdentifierRaw)

        if (newIdentifierSlug.isBlank()) {
            Toast.makeText(context, "Kullanıcı alanı boş olamaz.", Toast.LENGTH_SHORT).show()
            return
        }

        if (oldIdentifier != newIdentifierSlug) {
            AppPreferences.setUserIdentifier(requireContext(), newIdentifierSlug)
            Toast.makeText(context, "Kullanıcı adı değiştirildi. Mevcut profillerin QR kodları geçersiz olabilir.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Ayarlar kaydedildi.", Toast.LENGTH_SHORT).show()
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}