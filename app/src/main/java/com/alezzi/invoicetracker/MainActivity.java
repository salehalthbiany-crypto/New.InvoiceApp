package com.alezzi.invoicetracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private LinearLayout layoutTabInvoice, layoutTabHistory, layoutTabCustomers;
    private Button tabInvoiceBtn, tabRecordBtn, tabCustomersBtn;

    // عناصر الفاتورة
    private TextView tvDate;
    private EditText etCustomerName;
    private EditText etPaid;
    private TextView tvTotalAmount;
    private TextView tvNetRemaining;
    private Button btnAddRow, btnNew, btnSave, btnImage, btnPrint, btnShareWhatsappText, btnShareWhatsappImage;
    private RecyclerView recyclerViewRows;
    private CardView cardInvoice;

    private final List<InvoiceItem> itemList = new ArrayList<>();
    private final List<String> suggestionsList = new ArrayList<>();
    private InvoiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDefaultSuggestions();
        setupBackHandler();
        setupDate();
        setupRecyclerView();
        setupListeners();
        calculateTotals();
        loadHistoryCount();
        switchTab(1);
    }

    private void initViews() {
        layoutTabInvoice = findViewById(R.id.layoutTabInvoice);
        layoutTabHistory = findViewById(R.id.layoutTabHistory);
        layoutTabCustomers = findViewById(R.id.layoutTabCustomers);

        tabInvoiceBtn = findViewById(R.id.tabInvoiceBtn);
        tabRecordBtn = findViewById(R.id.tabRecordBtn);
        tabCustomersBtn = findViewById(R.id.tabCustomersBtn);

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
        btnShareWhatsappText = findViewById(R.id.btnShareWhatsappText);
        btnShareWhatsappImage = findViewById(R.id.btnShareWhatsappImage);

        recyclerViewRows = findViewById(R.id.recyclerViewRows);
        cardInvoice = findViewById(R.id.cardInvoice);
    }

    private void setupDefaultSuggestions() {
        suggestionsList.add("سكر 5 كيلو");
        suggestionsList.add("رز الشعلان 10 كيلو");
        suggestionsList.add("زيت طبخ 1.8 لتر");
        suggestionsList.add("حليب مدهش 1800 جرام");
        suggestionsList.add("كرتون دجاج");
        suggestionsList.add("معجون طماطم");
        suggestionsList.add("شاي كبوس");
    }

    private void switchTab(int tabIndex) {
        layoutTabInvoice.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutTabHistory.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        layoutTabCustomers.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        tabInvoiceBtn.setBackgroundColor(Color.parseColor(tabIndex == 1 ? "#059669" : "#2D3748"));
        tabRecordBtn.setBackgroundColor(Color.parseColor(tabIndex == 2 ? "#059669" : "#2D3748"));
        tabCustomersBtn.setBackgroundColor(Color.parseColor(tabIndex == 3 ? "#059669" : "#2D3748"));
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

        adapter = new InvoiceAdapter(itemList, suggestionsList, this::calculateTotals);
        recyclerViewRows.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRows.setAdapter(adapter);
    }

    private void setupListeners() {
        tabInvoiceBtn.setOnClickListener(v -> switchTab(1));
        tabRecordBtn.setOnClickListener(v -> switchTab(2));
        tabCustomersBtn.setOnClickListener(v -> switchTab(3));

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

        btnShareWhatsappText.setOnClickListener(v -> shareInvoiceTextWhatsapp());
        btnShareWhatsappImage.setOnClickListener(v -> shareInvoiceImageWhatsapp(cardInvoice));
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
        Toast.makeText(this, "تم تفريغ الفاتورة لبدء إدخال جديد", Toast.LENGTH_SHORT).show();
    }

    private void saveInvoiceToHistory() {
        SharedPreferences prefs = getSharedPreferences("AlEzziPrefs", Context.MODE_PRIVATE);
        int currentCount = prefs.getInt("history_count", 0) + 1;
        String customerName = etCustomerName.getText().toString().trim();
        if (customerName.isEmpty()) customerName = "عميل عام";
        String total = tvTotalAmount.getText().toString();
        String remaining = tvNetRemaining.getText().toString();
        String date = tvDate.getText().toString();

        try {
            JSONObject invoiceObj = new JSONObject();
            invoiceObj.put("id", currentCount);
            invoiceObj.put("customer", customerName);
            invoiceObj.put("total", total);
            invoiceObj.put("remaining", remaining);
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
        tabRecordBtn.setText("السجل (" + count + ") 📁");
    }

    private void shareInvoiceTextWhatsapp() {
        String customer = etCustomerName.getText().toString().trim();
        if (customer.isEmpty()) customer = "عميل عام";
        String total = tvTotalAmount.getText().toString();
        String net = tvNetRemaining.getText().toString();
        String date = tvDate.getText().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *بقالة العزي - فاتورة مبيعات*
");
        sb.append("📅 التاريخ: ").append(date).append("
");
        sb.append("👤 العميل: ").append(customer).append("
");
        sb.append("----------------------------
");
        for (InvoiceItem item : itemList) {
            if (item.getAmount() > 0 || !item.getDetails().isEmpty()) {
                sb.append("• ").append(item.getDetails()).append(" | ").append(item.getAmount()).append(" ر.ي
");
            }
        }
        sb.append("----------------------------
");
        sb.append("💰 الإجمالي: ").append(total).append(" ر.ي
");
        sb.append("📌 الصافي المتبقي: ").append(net).append(" ر.ي
");
        sb.append("شكراً لتسوقكم من بقالة العزي!");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (Exception e) {
            intent.setPackage(null);
            startActivity(Intent.createChooser(intent, "مشاركة الفاتورة عبر:"));
        }
    }

    private void shareInvoiceImageWhatsapp(View view) {
        Bitmap bitmap = getBitmapFromView(view);
        try {
            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "AlEzzi_Invoice", null);
            Uri uri = Uri.parse(path);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر مشاركة الصورة المباشرة", Toast.LENGTH_SHORT).show();
        }
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
            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
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