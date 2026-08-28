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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.print.PrintHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvDate: TextView
    private lateinit var etCustomerName: EditText
    private lateinit var etPaid: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvNetRemaining: TextView
    private lateinit var btnAddRow: Button
    private lateinit var btnNew: Button
    private lateinit var btnSave: Button
    private lateinit var btnImage: Button
    private lateinit var btnPrint: Button
    private lateinit var tabRecord: Button
    private lateinit var tabInvoice: Button
    private lateinit var recyclerViewRows: RecyclerView
    private lateinit var cardInvoice: CardView

    private val itemList = mutableListOf<InvoiceItem>()
    private lateinit var adapter: InvoiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBackHandler()
        setupDate()
        setupRecyclerView()
        setupListeners()
        calculateTotals()
        loadHistoryCount()
    }

    private fun initViews() {
        tvDate = findViewById(R.id.tvDate)
        etCustomerName = findViewById(R.id.etCustomerName)
        etPaid = findViewById(R.id.etPaid)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvNetRemaining = findViewById(R.id.tvNetRemaining)
        btnAddRow = findViewById(R.id.btnAddRow)
        btnNew = findViewById(R.id.btnNew)
        btnSave = findViewById(R.id.btnSave)
        btnImage = findViewById(R.id.btnImage)
        btnPrint = findViewById(R.id.btnPrint)
        tabRecord = findViewById(R.id.tabRecord)
        tabInvoice = findViewById(R.id.tabInvoice)
        recyclerViewRows = findViewById(R.id.recyclerViewRows)
        cardInvoice = findViewById(R.id.cardInvoice)
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("تأكيد الخروج")
                    .setMessage("هل تريد الخروج من برنامج فواتير بقالة العزي؟")
                    .setPositiveButton("نعم") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("لا", null)
                    .show()
            }
        })
    }

    private fun setupDate() {
        val sdf = SimpleDateFormat("yyyy/M/d", Locale.getDefault())
        tvDate.text = sdf.format(Date())
    }

    private fun setupRecyclerView() {
        itemList.clear()
        itemList.add(InvoiceItem())
        itemList.add(InvoiceItem())

        adapter = InvoiceAdapter(itemList) {
            calculateTotals()
        }

        recyclerViewRows.layoutManager = LinearLayoutManager(this)
        recyclerViewRows.adapter = adapter
    }

    private fun setupListeners() {
        btnAddRow.setOnClickListener {
            itemList.add(InvoiceItem())
            adapter.notifyItemInserted(itemList.size - 1)
            recyclerViewRows.smoothScrollToPosition(itemList.size - 1)
            calculateTotals()
        }

        etPaid.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateTotals()
            }
        })

        btnNew.setOnClickListener { resetForm() }
        btnSave.setOnClickListener { saveInvoiceToHistory() }
        btnImage.setOnClickListener { saveCardAsImage(cardInvoice) }
        btnPrint.setOnClickListener { printInvoiceCard(cardInvoice) }

        tabRecord.setOnClickListener { showHistoryDialog() }
    }

    private fun calculateTotals() {
        var totalAmount = 0.0
        for (item in itemList) {
            totalAmount += item.amount
            item.balance = totalAmount
        }

        val paidStr = etPaid.text.toString()
        val paidAmount = paidStr.toDoubleOrNull() ?: 0.0
        val remainingNet = totalAmount - paidAmount

        tvTotalAmount.text = String.format(Locale.US, "%.2f", totalAmount)
        tvNetRemaining.text = String.format(Locale.US, "%.2f", remainingNet)

        if (remainingNet > 0) {
            tvNetRemaining.setTextColor(Color.parseColor("#E53E3E"))
        } else {
            tvNetRemaining.setTextColor(Color.parseColor("#059669"))
        }
    }

    private fun resetForm() {
        etCustomerName.setText("")
        etPaid.setText("")
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
        val customerName = etCustomerName.text.toString().ifEmpty { "عميل عام" }
        val total = tvTotalAmount.text.toString()
        val date = tvDate.text.toString()

        val invoiceObj = JSONObject()
        invoiceObj.put("id", currentCount)
        invoiceObj.put("customer", customerName)
        invoiceObj.put("total", total)
        invoiceObj.put("date", date)

        val itemsArr = JSONArray()
        for (item in itemList) {
            val itemObj = JSONObject()
            itemObj.put("amount", item.amount)
            itemObj.put("details", item.details)
            itemObj.put("balance", item.balance)
            itemsArr.put(itemObj)
        }
        invoiceObj.put("items", itemsArr)

        prefs.edit()
            .putInt("history_count", currentCount)
            .putString("invoice_" + currentCount, invoiceObj.toString())
            .apply()

        loadHistoryCount()
        Toast.makeText(this, "تم حفظ الفاتورة بالسجل بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun loadHistoryCount() {
        val prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("history_count", 0)
        tabRecord.text = "السجل (" + count + ") 📁"
    }

    private fun showHistoryDialog() {
        val prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("history_count", 0)

        if (count == 0) {
            Toast.makeText(this, "لا توجد فواتير محفوظة بالسجل حالياً", Toast.LENGTH_SHORT).show()
            return
        }

        val historyList = ArrayList<String>()
        val jsonList = ArrayList<JSONObject>()

        for (i in 1..count) {
            val jsonStr = prefs.getString("invoice_" + i, null)
            if (jsonStr != null) {
                try {
                    val obj = JSONObject(jsonStr)
                    jsonList.add(obj)
                    val label = "فاتورة #" + obj.optInt("id") + " - " + obj.optString("customer") + " (" + obj.optString("date") + ") - " + obj.optString("total") + " ر.ي"
                    historyList.add(label)
                } catch (e: Exception) {
                    historyList.add("فاتورة #" + i)
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("سجل فواتير بقالة العزي")
            .setItems(historyList.toTypedArray()) { _, which ->
                if (which < jsonList.size) {
                    loadInvoiceFromJSON(jsonList[which])
                }
            }
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun loadInvoiceFromJSON(obj: JSONObject) {
        etCustomerName.setText(obj.optString("customer", ""))
        val itemsArr = obj.optJSONArray("items")
        itemList.clear()
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val itemObj = itemsArr.getJSONObject(i)
                val item = InvoiceItem(
                    amount = itemObj.optDouble("amount", 0.0),
                    details = itemObj.optString("details", ""),
                    balance = itemObj.optDouble("balance", 0.0)
                )
                itemList.add(item)
            }
        }
        if (itemList.isEmpty()) {
            itemList.add(InvoiceItem())
        }
        adapter.notifyDataSetChanged()
        calculateTotals()
        Toast.makeText(this, "تم تحميل الفاتورة من السجل", Toast.LENGTH_SHORT).show()
    }

    private fun saveCardAsImage(view: View) {
        val bitmap = getBitmapFromView(view)
        val filename = "AlEzzi_Invoice_" + System.currentTimeMillis() + ".png"
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
            Toast.makeText(this, "فشل الحفظ: " + e.message, Toast.LENGTH_SHORT).show()
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
}