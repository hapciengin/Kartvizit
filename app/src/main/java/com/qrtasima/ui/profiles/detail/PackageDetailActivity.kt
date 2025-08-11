package com.qrtasima.ui.profiles.detail

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.qrtasima.QrTasimaApplication
import com.qrtasima.R
import com.qrtasima.data.PackageContent
import com.qrtasima.data.ProfilePackage
import com.qrtasima.databinding.ActivityPackageDetailRevisedBinding
import com.qrtasima.databinding.DialogPackageSettingsBinding
import com.qrtasima.databinding.DialogShowQrBinding
import com.qrtasima.ui.profiles.detail.PreviewActivity
import com.qrtasima.viewmodel.ViewModelFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import androidx.core.graphics.toColorInt

sealed class QrResult {
    data class Success(val url: String) : QrResult()
    data class Error(val message: String) : QrResult()
}

class PackageDetailActivity : AppCompatActivity() {

    companion object {
        private const val TARGET_IMAGE_WIDTH = 400
        private const val TARGET_IMAGE_HEIGHT = 400
    }

    private lateinit var binding: ActivityPackageDetailRevisedBinding
    private var packageId: Int = -1
    private var currentPackage: ProfilePackage? = null
    private var imageBase64: String? = null
    private val contentList = mutableListOf<PackageContent>() // Keep this list updated by adapter

    private val detailViewModel: DetailViewModel by viewModels {
        ViewModelFactory((application as QrTasimaApplication).database.packageDao())
    }

    private lateinit var adapter: DetailAdapter

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val scaledBitmap = decodeSampledBitmapFromUri(contentResolver, it, TARGET_IMAGE_WIDTH, TARGET_IMAGE_HEIGHT)
                if (scaledBitmap != null) {
                    binding.profileImage.setImageBitmap(scaledBitmap)
                    imageBase64 = bitmapToBase64(scaledBitmap)
                    savePackageChanges()
                } else {
                    Toast.makeText(this, "Resim işlenemedi.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Resim yüklenirken bir hata oluştu.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackageDetailRevisedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        packageId = intent.getIntExtra("PACKAGE_ID", -1)
        if (packageId == -1) {
            finish()
            return
        }

        setupRecyclerView()
        setupObservers()
        detailViewModel.loadPackage(packageId)

        binding.fabAddModule.setOnClickListener { showAddModuleDialog() }
        binding.buttonSaveAndQr.setOnClickListener { saveAndRequestQr() }
        binding.profileImage.setOnClickListener { pickImage.launch("image/*") }
        binding.buttonSettings.setOnClickListener { showPackageSettingsDialog() }
        binding.buttonPreview.setOnClickListener { showPreview() }
        binding.buttonSetBackground.setOnClickListener { showBackgroundColorPicker() }
    }

    override fun onPause() {
        super.onPause()
        savePackageChanges()
    }

    private fun setupRecyclerView() {
        adapter = DetailAdapter(
            contentList, // Pass the mutable list
            onContentChanged = { position, newContent ->
                if (position < contentList.size) {
                    contentList[position] = newContent
                }
            },
            onDeleteClicked = { contentToDelete ->
                val position = contentList.indexOf(contentToDelete)
                if (position != -1) {
                    contentList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
                detailViewModel.deleteContent(contentToDelete)
            },
            onColorPickerClicked = { item, position ->
                showColorPickerForItem(position)
            }
        )
        binding.detailRecyclerView.adapter = adapter
        binding.detailRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        detailViewModel.packageWithContents.observe(this) { packageWithContents ->
            packageWithContents?.let {
                this.currentPackage = it.profilePackage
                supportActionBar?.title = it.profilePackage.name

                // Only update adapter data if it's the initial load or a significant change
                // to prevent loop with onContentChanged.
                if (contentList.isEmpty() || !contentList.containsAll(it.contents) || !it.contents.containsAll(contentList)) {
                    contentList.clear()
                    contentList.addAll(it.contents)
                    adapter.updateData(it.contents)
                }

                this.imageBase64 = it.profilePackage.profileImageBase64
                if (it.profilePackage.profileImageBase64 != null) {
                    val bitmap = base64ToBitmap(it.profilePackage.profileImageBase64)
                    binding.profileImage.setImageBitmap(bitmap)
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_person)
                }

                // Update activity background color dynamically
                it.profilePackage.backgroundColor?.let { colorHex ->
                    try {
                        window.decorView.setBackgroundColor(colorHex.toColorInt())
                    } catch (e: Exception) {
                        // Default to a light background if color is invalid
                        window.decorView.setBackgroundColor(Color.parseColor("#f0f2f5"))
                    }
                } ?: run {
                    // Default background if no color is set
                    window.decorView.setBackgroundColor(Color.parseColor("#f0f2f5"))
                }
            }
        }

        detailViewModel.qrResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.buttonSaveAndQr.isEnabled = true
            when (result) {
                is QrResult.Success -> {
                    val bitmap = generateQrCodeBitmap(result.url)
                    showQrDialog(bitmap)
                }
                is QrResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveAndRequestQr() {
        savePackageChanges(generateQr = true)
    }

    private fun showPreview() {
        if (packageId != -1) {
            val intent = Intent(this, PreviewActivity::class.java).apply {
                putExtra("PACKAGE_ID", packageId)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Lütfen önce paketi kaydedin.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePackageChanges(generateQr: Boolean = false) {
        val updatedPackage = currentPackage?.copy(
            profileImageBase64 = this.imageBase64,
            backgroundColor = currentPackage?.backgroundColor
        )
        if (updatedPackage != null) {
            if (generateQr) {
                Toast.makeText(this, "Profil sayfası oluşturuluyor, lütfen bekleyin...", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.VISIBLE
                binding.buttonSaveAndQr.isEnabled = false
                detailViewModel.saveAndGenerateGist(updatedPackage, contentList.toList())
            } else {
                detailViewModel.updatePackageAndContents(updatedPackage, contentList.toList())
            }
        }
    }

    private fun showPackageSettingsDialog() {
        currentPackage?.let { pkg ->
            val dialogBinding = DialogPackageSettingsBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Paket Ayarları")
                .setView(dialogBinding.root)
                .setPositiveButton("Kaydet", null)
                .setNegativeButton("İptal", null)
                .show()

            dialogBinding.editTextPackageName.setText(pkg.name)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = dialogBinding.editTextPackageName.text.toString()
                if (newName.isNotBlank()) {
                    currentPackage = currentPackage?.copy(name = newName)
                    supportActionBar?.title = newName
                    savePackageChanges()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Paket adı boş olamaz.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBackgroundColorPicker() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val hexagonPicker = dialogView.findViewById<HexagonColorPickerView>(R.id.hexagon_color_picker)
        val resetButton = dialogView.findViewById<Button>(R.id.button_reset_color)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        hexagonPicker.onColorSelected = { colorHex ->
            currentPackage = currentPackage?.copy(backgroundColor = colorHex)
            window.decorView.setBackgroundColor(colorHex.toColorInt())
            savePackageChanges()
            dialog.dismiss()
        }

        resetButton.setOnClickListener {
            currentPackage = currentPackage?.copy(backgroundColor = "#f0f2f5") // Default light gray
            window.decorView.setBackgroundColor(Color.parseColor("#f0f2f5"))
            savePackageChanges()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showColorPickerForItem(position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val hexagonPicker = dialogView.findViewById<HexagonColorPickerView>(R.id.hexagon_color_picker)
        val resetButton = dialogView.findViewById<Button>(R.id.button_reset_color)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        hexagonPicker.onColorSelected = { colorHex ->
            val updatedItem = contentList[position].copy(customColor = colorHex)
            contentList[position] = updatedItem
            adapter.notifyItemChanged(position)
            dialog.dismiss()
        }

        resetButton.setOnClickListener {
            val updatedItem = contentList[position].copy(customColor = null)
            contentList[position] = updatedItem
            adapter.notifyItemChanged(position)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddModuleDialog() {
        val allModules = arrayOf("İsim", "Telefon", "WhatsApp", "E-posta", "Instagram", "Facebook", "LinkedIn", "Konum", "Açıklama")

        val availableModules = allModules.filter { moduleName ->
            val type = when(moduleName) {
                "Açıklama" -> "CUSTOM_TEXT"
                else -> moduleName.uppercase()
            }
            if (type == "CUSTOM_TEXT") true else contentList.none { it.type == type }
        }.toTypedArray()

        if (availableModules.isEmpty()) {
            Toast.makeText(this, "Tüm temel modüller eklendi.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedItems = ArrayList<Int>()
        AlertDialog.Builder(this)
            .setTitle("Yeni İçerik Ekle")
            .setMultiChoiceItems(availableModules, null) { _, which, isChecked ->
                if (isChecked) selectedItems.add(which) else selectedItems.remove(Integer.valueOf(which))
            }
            .setPositiveButton("Ekle") { _, _ ->
                selectedItems.forEach { index ->
                    val selectedModuleName = availableModules[index]
                    val type = when (selectedModuleName) {
                        "Açıklama" -> "CUSTOM_TEXT"
                        else -> selectedModuleName.uppercase()
                    }
                    val newContent = PackageContent(
                        packageId = packageId,
                        type = type,
                        label = selectedModuleName,
                        value = ""
                    )
                    contentList.add(newContent)
                }
                adapter.notifyDataSetChanged() // Notify the adapter that the underlying data has changed
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showQrDialog(bitmap: Bitmap?) {
        if (bitmap == null) return
        val dialogBinding = DialogShowQrBinding.inflate(layoutInflater)
        dialogBinding.qrImageView.setImageBitmap(bitmap)
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Kapat", null)
            .setCancelable(true)
            .show()
    }

    private fun generateQrCodeBitmap(text: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(resolver: ContentResolver, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = resolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            options.inJustDecodeBounds = false
            inputStream = resolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }
}