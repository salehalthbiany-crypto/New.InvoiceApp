package com.alezzi.invoicetracker

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.print.PrintHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.alezzi.invoicetracker.databinding.ActivityMainBinding
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val itemList = mutableListOf<InvoiceItem>()
    private lateinit var adapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDate()
        setupRecyclerView()
        setupListeners()
        calculateTotals()
        loadHistoryCount()
    }

    private fun setupDate() {
        val sdf = SimpleDateFormat("yyyy/M/d", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date())
    }

    private fun setupRecyclerView() {
        itemList.clear()
        itemList.add(InvoiceItem())
        itemList.add(InvoiceItem())

        adapter = InvoiceAdapter(itemList) {
            calculateTotals()
        }

        binding.recyclerViewRows.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRows.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddRow.setOnClickListener {
            itemList.add(InvoiceItem())
            adapter.notifyItemInserted(itemList.size - 1)
            calculateTotals()
        }

        binding.etPaid.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateTotals()
            }
        })

        binding.btnNew.setOnClickListener { resetForm() }
        binding.btnSave.setOnClickListener { saveInvoiceToHistory() }
        binding.btnImage.setOnClickListener { saveCardAsImage(binding.cardInvoice) }
        binding.btnPrint.setOnClickListener { printInvoiceCard(binding.cardInvoice) }

        binding.tabRecord.setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun calculateTotals() {
        var totalAmount = 0.0
        for (item in itemList) {
            totalAmount += item.amount
            item.balance = totalAmount
        }

        val paidStr = binding.etPaid.text.toString()
        val paidAmount = paidStr.toDoubleOrNull() ?: 0.0
        val remainingNet = totalAmount - paidAmount

        binding.tvTotalAmount.text = String.format(Locale.US, "%.2f", totalAmount)
        binding.tvNetRemaining.text = String.format(Locale.US, "%.2f", remainingNet)
    }

    private fun resetForm() {
        binding.etCustomerName.setText("")
        binding.etPaid.setText("")
        itemList.clear()
        itemList.add(InvoiceItem())
        itemList.add(InvoiceItem())
        adapter.notifyDataSetChanged()
        calculateTotals()
        Toast.makeText(this, "تم بدء فاتورة جديدة لبقالة العزي", Toast.LENGTH_SHORT).show()
    }

    private fun saveInvoiceToHistory() {
        val prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("history_count", 0) + 1
        val customerName = binding.etCustomerName.text.toString().ifEmpty { "عميل عام" }
        val total = binding.tvTotalAmount.text.toString()

        prefs.edit()
            .putInt("history_count", currentCount)
            .putString("invoice_$currentCount", "فاتورة #$currentCount - $customerName - إجمالي: $total")
            .apply()

        loadHistoryCount()
        Toast.makeText(this, "تم حفظ الفاتورة في السجل بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun loadHistoryCount() {
        val prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("history_count", 0)
        binding.tabRecord.text = "السجل ($count) 📁"
    }

    private fun showHistoryDialog() {
        val prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("history_count", 0)

        if (count == 0) {
            Toast.makeText(this, "لا توجد فواتير محفوظة بالسجل حالياً", Toast.LENGTH_SHORT).show()
            return
        }

        val historyItems = Array(count) { i ->
            prefs.getString("invoice_${i + 1}", "فاتورة فارغة") ?: ""
        }

        AlertDialog.Builder(this)
            .setTitle("سجل فواتير بقالة العزي")
            .setItems(historyItems, null)
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun saveCardAsImage(view: View) {
        val bitmap = getBitmapFromView(view)
        val filename = "AlEzzi_Invoice_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AlEzziInvoices")
                }
            }
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = contentResolver.openOutputStream(imageUri)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(this, "تم حفظ الفاتورة كصورة بالاستوديو", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "فشل الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printInvoiceCard(view: View) {
        val bitmap = getBitmapFromView(view)
        val printHelper = PrintHelper(this)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap("AlEzzi_Invoice_Print", bitmap)
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الخروج")
            .setMessage("هل تريد الخروج من برنامج فواتير بقالة العزي؟")
            .setPositiveButton("نعم") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("لا", null)
            .show()
    }
}