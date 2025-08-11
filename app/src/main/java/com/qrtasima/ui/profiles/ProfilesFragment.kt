package com.qrtasima.ui.profiles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.qrtasima.QrTasimaApplication
import com.qrtasima.R
import com.qrtasima.data.ProfilePackage
import com.qrtasima.databinding.DialogShowQrBinding
import com.qrtasima.databinding.FragmentProfilesBinding
import com.qrtasima.ui.profiles.detail.PackageDetailActivity
import com.qrtasima.util.ThemeManager
import com.qrtasima.viewmodel.MainViewModel
import com.qrtasima.viewmodel.ViewModelFactory

class ProfilesFragment : Fragment() {
    private var _binding: FragmentProfilesBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as QrTasimaApplication).database.packageDao())
    }
    private lateinit var adapter: ProfilePackageAdapter

    private val requiredPermissions by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.all { it.value }) {
            quickSendFileLauncher.launch("*/*")
        } else {
            Toast.makeText(context, "Dosya erişim izni verilmedi.", Toast.LENGTH_LONG).show()
        }
    }

    private val quickSendFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Dosya işleniyor...", Toast.LENGTH_SHORT).show()
            mainViewModel.createQuickSendGistForFile(uri, requireActivity().contentResolver)
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfilesBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()

        mainViewModel.allPackages.observe(viewLifecycleOwner) { packages ->
            binding.emptyStateLayout.visibility = if (packages.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewPackages.visibility = if (packages.isNullOrEmpty()) View.GONE else View.VISIBLE
            adapter.submitList(packages)
        }

        mainViewModel.quickSendQrUrl.observe(viewLifecycleOwner) { url ->
            url?.let {
                val bitmap = generateQrCodeBitmap(it)
                showQrDialog(bitmap)
                mainViewModel.resetQuickSendQrUrl()
            }
        }

        mainViewModel.qrCodeUrl.observe(viewLifecycleOwner) { url ->
            url?.let {
                if (it != "UNAVAILABLE") {
                    val bitmap = generateQrCodeBitmap(it)
                    showQrDialog(bitmap)
                } else {
                    Toast.makeText(context, "Bu paketin QR kodu şu anda gösterilemiyor.", Toast.LENGTH_SHORT).show()
                }
                mainViewModel.resetQrCodeUrl()
            }
        }

        // FAB'ı menüden kontrol ettiğimiz için görünürlüğünü kaldırıyoruz
        // binding.fabAddPackage.visibility = View.GONE; // Bu satırı yorumdan çıkarın veya kaldırın.
        // Eğer FAB'ın görünür olmasını istiyorsanız, bu satırı silin.
        // Mevcut XML'de fabAddPackage zaten var ve görünürlüğü `gone` değil.
        // Bu nedenle, aslında buradaki sorun `fabAddPackage`'ın kendisinin menü ile çakışması değil,
        // menü öğelerinin ID'lerinin `R` sınıfında bulunamamasıdır.
        // Ancak bu dosyadaki `binding.fabAddPackage.visibility = View.GONE` satırını kaldırdım.
        // Eğer FAB'ı kullanmak isterseniz tekrar ekleyebilirsiniz.
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.profiles_menu, menu)
                val currentTheme = ThemeManager.getCurrentTheme(requireContext())
                val itemToCheck = when (currentTheme) {
                    ThemeManager.Theme.LIGHT -> R.id.theme_light
                    ThemeManager.Theme.DARK -> R.id.theme_dark
                    ThemeManager.Theme.SYSTEM -> R.id.theme_system
                }
                menu.findItem(itemToCheck)?.isChecked = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_package -> {
                        showAddPackageDialog()
                        true
                    }
                    R.id.action_quick_send -> {
                        showQuickSendDialog()
                        true
                    }
                    R.id.theme_light -> {
                        ThemeManager.setTheme(requireContext(), ThemeManager.Theme.LIGHT)
                        true
                    }
                    R.id.theme_dark -> {
                        ThemeManager.setTheme(requireContext(), ThemeManager.Theme.DARK)
                        true
                    }
                    R.id.theme_system -> {
                        ThemeManager.setTheme(requireContext(), ThemeManager.Theme.SYSTEM)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showQuickSendDialog() {
        val items = arrayOf("Yazı Gönder", "Medya/Dosya Gönder")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hızlı Gönder")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> showTextInputDialog()
                    1 -> checkPermissionsAndLaunchPicker()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun checkPermissionsAndLaunchPicker() {
        if (arePermissionsGranted()) {
            quickSendFileLauncher.launch("*/*")
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showTextInputDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Paylaşmak istediğiniz metni girin"
            minLines = 3
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yazı Gönder")
            .setView(editText)
            .setPositiveButton("QR Oluştur") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) {
                    mainViewModel.createQuickSendGistForText(text)
                } else {
                    Toast.makeText(context, "Metin boş olamaz.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = ProfilePackageAdapter(
            onPackageClicked = { pkg ->
                val intent = Intent(requireContext(), PackageDetailActivity::class.java).apply {
                    putExtra("PACKAGE_ID", pkg.id)
                }
                startActivity(intent)
            },
            onQrClicked = { pkg ->
                mainViewModel.generateQrForPackage(pkg.id)
            },
            onDeleteClicked = { pkg ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("${pkg.name} Silinsin mi?")
                    .setMessage("Bu işlem geri alınamaz.")
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("Sil") { _, _ ->
                        mainViewModel.delete(pkg)
                    }
                    .show()
            },
            onEditClicked = { pkg ->
                showEditPackageNameDialog(pkg)
            }
        )
        binding.recyclerViewPackages.adapter = adapter
    }

    private fun showAddPackageDialog() {
        val editText = EditText(requireContext()).apply { hint = "Paket adı (örn: İş, Sosyal...)" }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Yeni Profil Paketi")
            .setView(editText)
            .setPositiveButton("Oluştur") { _, _ ->
                val packageName = editText.text.toString().trim()
                if (packageName.isNotBlank()) {
                    mainViewModel.insert(ProfilePackage(name = packageName))
                } else {
                    Toast.makeText(requireContext(), "Paket adı boş olamaz.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showEditPackageNameDialog(profilePackage: ProfilePackage) {
        val editText = EditText(requireContext()).apply { setText(profilePackage.name) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Paket Adını Düzenle")
            .setView(editText)
            .setPositiveButton("Kaydet") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotBlank()) {
                    mainViewModel.update(profilePackage.copy(name = newName))
                } else {
                    Toast.makeText(requireContext(), "Paket adı boş olamaz.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showQrDialog(bitmap: Bitmap?) {
        if (bitmap == null) return
        val dialogBinding = DialogShowQrBinding.inflate(layoutInflater)
        dialogBinding.qrImageView.setImageBitmap(bitmap)
        AlertDialog.Builder(requireContext()).setView(dialogBinding.root).setPositiveButton("Kapat", null).show()
    }

    private fun generateQrCodeBitmap(text: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}