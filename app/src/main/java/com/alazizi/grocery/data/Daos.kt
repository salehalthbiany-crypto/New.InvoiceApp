package com.alazizi.grocery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name") fun observeAll(): Flow<List<Product>>
    @Query("SELECT name FROM products WHERE name LIKE :q ORDER BY name LIMIT 20") suspend fun suggest(q: String): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(product: Product)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name") fun observeAll(): Flow<List<Customer>>
    @Insert suspend fun insert(customer: Customer): Long
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC") fun observeAll(): Flow<List<Invoice>>
    @Insert suspend fun insert(invoice: Invoice): Long
    @Insert suspend fun insertItems(items: List<InvoiceItem>)
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY id") suspend fun items(invoiceId: Long): List<InvoiceItem>
}
