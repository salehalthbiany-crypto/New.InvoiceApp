package com.example.invoicetracker

import android.content.ContentValues
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
import androidx.appcompat.app.AppCompatActivity
import androidx.print.PrintHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.invoicetracker.databinding.ActivityMainBinding
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
    }

    private fun setupDate() {
        val sdf = SimpleDateFormat("yyyy/M/d", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date())
    }

    private fun setupRecyclerView() {
        itemList.add(InvoiceItem())
        itemList.add(InvoiceItem())

        adapter = InvoiceAdapter(itemList) { calculateTotals() }

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
            override fun afterTextChanged(s: Editable?) { calculateTotals() }
        })

        binding.btnNew.setOnClickListener { resetForm() }
        binding.btnSave.setOnClickListener { Toast.makeText(this, "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show() }
        binding.btnImage.setOnClickListener { saveCardAsImage(binding.cardInvoice) }
        binding.btnPrint.setOnClickListener { printInvoiceCard(binding.cardInvoice) }
    }

    private fun calculateTotals() {
        var totalAmount = 0.0
        for (item in itemList) {
            totalAmount += item.amount
            item.balance = totalAmount
        }
        adapter.notifyDataSetChanged()

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
        Toast.makeText(this, "تم بدء فاتورة جديدة", Toast.LENGTH_SHORT).show()
    }

    private fun saveCardAsImage(view: View) {
        val bitmap = getBitmapFromView(view)
        val filename = "Invoice_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Invoices")
                }
            }
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = contentResolver.openOutputStream(imageUri)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(this, "تم حفظ الفاتورة كصورة في الاستوديو", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "فشل حفظ الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printInvoiceCard(view: View) {
        val bitmap = getBitmapFromView(view)
        val printHelper = PrintHelper(this)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap("Invoice_Print", bitmap)
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
}