package com.alezzi.invoicetracker;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private final List<InvoiceItem> items;
    private final Runnable onItemChanged;

    public InvoiceAdapter(List<InvoiceItem> items, Runnable onItemChanged) {
        this.items = items;
        this.onItemChanged = onItemChanged;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final EditText etAmount;
        final EditText etDetails;
        final TextView tvBalance;
        final ImageButton btnDelete;

        public ViewHolder(@NonNull View view) {
            super(view);
            etAmount = view.findViewById(R.id.etAmount);
            etDetails = view.findViewById(R.id.etDetails);
            tvBalance = view.findViewById(R.id.tvBalance);
            btnDelete = view.findViewById(R.id.btnDeleteRow);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvoiceItem item = items.get(position);

        holder.etAmount.setText(item.getAmount() > 0 ? String.valueOf(item.getAmount()) : "");
        holder.etDetails.setText(item.getDetails());
        holder.tvBalance.setText(String.format(Locale.US, "%.2f", item.getBalance()));

        holder.etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (holder.etAmount.hasFocus()) {
                    String str = s != null ? s.toString() : "";
                    try {
                        item.setAmount(str.isEmpty() ? 0.0 : Double.parseDouble(str));
                    } catch (Exception e) {
                        item.setAmount(0.0);
                    }
                    if (onItemChanged != null) onItemChanged.run();
                }
            }
        });

        holder.etDetails.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (holder.etDetails.hasFocus()) {
                    item.setDetails(s != null ? s.toString() : "");
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && items.size() > 0) {
                items.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, items.size());
                if (onItemChanged != null) onItemChanged.run();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}