package com.alezzi.invoicetracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.print.PrintHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvDate;
    private EditText etCustomerName;
    private EditText etPaid;
    private TextView tvTotalAmount;
    private TextView tvNetRemaining;
    private Button btnAddRow;
    private Button btnNew;
    private Button btnSave;
    private Button btnImage;
    private Button btnPrint;
    private Button tabRecord;
    private Button tabInvoice;
    private RecyclerView recyclerViewRows;
    private CardView cardInvoice;

    private final List<InvoiceItem> itemList = new ArrayList<>();
    private InvoiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupBackHandler();
        setupDate();
        setupRecyclerView();
        setupListeners();
        calculateTotals();
        loadHistoryCount();
    }

    private void initViews() {
        tvDate = findViewById(R.id.tvDate);
        etCustomerName = findViewById(R.id.etCustomerName);
        etPaid = findViewById(R.id.etPaid);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvNetRemaining = findViewById(R.id.tvNetRemaining);
        btnAddRow = findViewById(R.id.btnAddRow);
        btnNew = findViewById(R.id.btnNew);
        btnSave = findViewById(R.id.btnSave);
        btnImage = findViewById(R.id.btnImage);
        btnPrint = findViewById(R.id.btnPrint);
        tabRecord = findViewById(R.id.tabRecord);
        tabInvoice = findViewById(R.id.tabInvoice);
        recyclerViewRows = findViewById(R.id.recyclerViewRows);
        cardInvoice = findViewById(R.id.cardInvoice);
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("تأكيد الخروج")
                        .setMessage("هل تريد الخروج من برنامج فواتير بقالة العزي؟")
                        .setPositiveButton("نعم", (dialog, which) -> finish())
                        .setNegativeButton("لا", null)
                        .show();
            }
        });
    }

    private void setupDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/d", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
    }

    private void setupRecyclerView() {
        itemList.clear();
        itemList.add(new InvoiceItem());
        itemList.add(new InvoiceItem());

        adapter = new InvoiceAdapter(itemList, this::calculateTotals);
        recyclerViewRows.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRows.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddRow.setOnClickListener(v -> {
            itemList.add(new InvoiceItem());
            adapter.notifyItemInserted(itemList.size() - 1);
            recyclerViewRows.smoothScrollToPosition(itemList.size() - 1);
            calculateTotals();
        });

        etPaid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calculateTotals();
            }
        });

        btnNew.setOnClickListener(v -> resetForm());
        btnSave.setOnClickListener(v -> saveInvoiceToHistory());
        btnImage.setOnClickListener(v -> saveCardAsImage(cardInvoice));
        btnPrint.setOnClickListener(v -> printInvoiceCard(cardInvoice));

        tabRecord.setOnClickListener(v -> showHistoryDialog());
    }

    private void calculateTotals() {
        double totalAmount = 0.0;
        for (InvoiceItem item : itemList) {
            totalAmount += item.getAmount();
            item.setBalance(totalAmount);
        }

        String paidStr = etPaid.getText().toString();
        double paidAmount = 0.0;
        try {
            if (!paidStr.isEmpty()) paidAmount = Double.parseDouble(paidStr);
        } catch (Exception ignored) {}

        double remainingNet = totalAmount - paidAmount;

        tvTotalAmount.setText(String.format(Locale.US, "%.2f", totalAmount));
        tvNetRemaining.setText(String.format(Locale.US, "%.2f", remainingNet));

        if (remainingNet > 0) {
            tvNetRemaining.setTextColor(Color.parseColor("#E53E3E"));
        } else {
            tvNetRemaining.setTextColor(Color.parseColor("#059669"));
        }
    }

    private void resetForm() {
        etCustomerName.setText("");
        etPaid.setText("");
        itemList.clear();
        itemList.add(new InvoiceItem());
        itemList.add(new InvoiceItem());
        adapter.notifyDataSetChanged();
        calculateTotals();
        Toast.makeText(this, "تم بدء فاتورة جديدة لبقالة العزي", Toast.LENGTH_SHORT).show();
    }

    private void saveInvoiceToHistory() {
        SharedPreferences prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE);
        int currentCount = prefs.getInt("history_count", 0) + 1;
        String customerName = etCustomerName.getText().toString().trim();
        if (customerName.isEmpty()) customerName = "عميل عام";
        String total = tvTotalAmount.getText().toString();
        String date = tvDate.getText().toString();

        try {
            JSONObject invoiceObj = new JSONObject();
            invoiceObj.put("id", currentCount);
            invoiceObj.put("customer", customerName);
            invoiceObj.put("total", total);
            invoiceObj.put("date", date);

            JSONArray itemsArr = new JSONArray();
            for (InvoiceItem item : itemList) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("amount", item.getAmount());
                itemObj.put("details", item.getDetails());
                itemObj.put("balance", item.getBalance());
                itemsArr.put(itemObj);
            }
            invoiceObj.put("items", itemsArr);

            prefs.edit()
                    .putInt("history_count", currentCount)
                    .putString("invoice_" + currentCount, invoiceObj.toString())
                    .apply();

            loadHistoryCount();
            Toast.makeText(this, "تم حفظ الفاتورة بالسجل بنجاح", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في حفظ الفاتورة", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHistoryCount() {
        SharedPreferences prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE);
        int count = prefs.getInt("history_count", 0);
        tabRecord.setText("السجل (" + count + ") 📁");
    }

    private void showHistoryDialog() {
        SharedPreferences prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE);
        int count = prefs.getInt("history_count", 0);

        if (count == 0) {
            Toast.makeText(this, "لا توجد فواتير محفوظة بالسجل حالياً", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> historyList = new ArrayList<>();
        List<JSONObject> jsonList = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String jsonStr = prefs.getString("invoice_" + i, null);
            if (jsonStr != null) {
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    jsonList.add(obj);
                    String label = "فاتورة #" + obj.optInt("id") + " - " + obj.optString("customer") + " (" + obj.optString("date") + ") - " + obj.optString("total") + " ر.ي";
                    historyList.add(label);
                } catch (Exception e) {
                    historyList.add("فاتورة #" + i);
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("سجل فواتير بقالة العزي")
                .setItems(historyList.toArray(new String[0]), (dialog, which) -> {
                    if (which < jsonList.size()) {
                        loadInvoiceFromJSON(jsonList.get(which));
                    }
                })
                .setPositiveButton("إغلاق", null)
                .show();
    }

    private void loadInvoiceFromJSON(JSONObject obj) {
        etCustomerName.setText(obj.optString("customer", ""));
        JSONArray itemsArr = obj.optJSONArray("items");
        itemList.clear();
        if (itemsArr != null) {
            for (int i = 0; i < itemsArr.length(); i++) {
                try {
                    JSONObject itemObj = itemsArr.getJSONObject(i);
                    InvoiceItem item = new InvoiceItem(
                            itemObj.optDouble("amount", 0.0),
                            itemObj.optString("details", ""),
                            itemObj.optDouble("balance", 0.0)
                    );
                    itemList.add(item);
                } catch (Exception ignored) {}
            }
        }
        if (itemList.isEmpty()) {
            itemList.add(new InvoiceItem());
        }
        adapter.notifyDataSetChanged();
        calculateTotals();
        Toast.makeText(this, "تم تحميل الفاتورة من السجل", Toast.LENGTH_SHORT).show();
    }

    private void saveCardAsImage(View view) {
        Bitmap bitmap = getBitmapFromView(view);
        String filename = "AlEzzi_Invoice_" + System.currentTimeMillis() + ".png";
        OutputStream fos = null;

        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/AlEzziInvoices");
            }
            android.net.Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri != null) {
                fos = getContentResolver().openOutputStream(imageUri);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "تم حفظ الفاتورة كصورة بالاستوديو", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "فشل الحفظ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void printInvoiceCard(View view) {
        Bitmap bitmap = getBitmapFromView(view);
        PrintHelper printHelper = new PrintHelper(this);
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        printHelper.printBitmap("AlEzzi_Invoice_Print", bitmap);
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        android.graphics.drawable.Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return bitmap;
    }
}