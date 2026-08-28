package com.alezzi.invoicetracker

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class InvoiceAdapter(
    private val items: MutableList<InvoiceItem>,
    private val onItemChanged: () -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val etAmount: EditText = view.findViewById(R.id.etAmount)
        val etDetails: EditText = view.findViewById(R.id.etDetails)
        val tvBalance: TextView = view.findViewById(R.id.tvBalance)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteRow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // تجنب إطلاق المستمعين بشكل متكرر
        holder.etAmount.setText(if (item.amount > 0) item.amount.toString() else "")
        holder.etDetails.setText(item.details)
        holder.tvBalance.text = String.format(Locale.US, "%.2f", item.balance)

        holder.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val valStr = holder.etAmount.text.toString()
                item.amount = valStr.toDoubleOrNull() ?: 0.0
                onItemChanged()
            }
        }

        holder.etDetails.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                item.details = holder.etDetails.text.toString()
            }
        }

        holder.btnDelete.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION && items.size > 0) {
                items.removeAt(currentPos)
                notifyItemRemoved(currentPos)
                notifyItemRangeChanged(currentPos, items.size)
                onItemChanged()
            }
        }
    }

    override fun getItemCount() = items.size
}