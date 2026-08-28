package com.alazizi.grocery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val lastPrice: Double = 0.0)

@Entity(tableName = "customers")
data class Customer(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val phone: String = "", val balance: Double = 0.0)

@Entity(tableName = "invoices")
data class Invoice(@PrimaryKey(autoGenerate = true) val id: Long = 0, val customerName: String, val createdAt: Long, val total: Double)

@Entity(tableName = "invoice_items")
data class InvoiceItem(@PrimaryKey(autoGenerate = true) val id: Long = 0, val invoiceId: Long, val productName: String, val quantity: Double, val unitPrice: Double, val lineTotal: Double)
