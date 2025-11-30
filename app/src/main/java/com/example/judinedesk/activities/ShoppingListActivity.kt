package com.example.judinedesk.activities

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.adapters.ShoppingListAdapter
import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.ShoppingListItem
import com.example.judinedesk.models.Staff
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ShoppingListActivity : AppCompatActivity() {

    private lateinit var rvShoppingList: RecyclerView
    private lateinit var btnAddItem: Button
    private lateinit var btnAddFirstItem: Button
    private lateinit var tvHallName: TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalCost: TextView
    private lateinit var emptyState: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private var currentHall: String = ""
    private var shoppingListener: ListenerRegistration? = null
    private val shoppingItems = mutableListOf<ShoppingListItem>()
    private lateinit var adapter: ShoppingListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        // Safe way to get current user data
        val currentUser = AuthHelper.currentUserData
        currentHall = when {
            currentUser is Staff -> currentUser.hall
            currentUser is Manager -> currentUser.hall
            else -> "Unknown Hall"
        }

        setupViews()
        setupRecyclerView()
        loadShoppingList()
    }

    private fun setupViews() {
        rvShoppingList = findViewById(R.id.rvShoppingList)
        btnAddItem = findViewById(R.id.btnAddItem)
        btnAddFirstItem = findViewById(R.id.btnAddFirstItem)
        tvHallName = findViewById(R.id.tvHallName)
        tvTotalItems = findViewById(R.id.tvTotalItems) // FIXED: Changed from tvPendingCount
        tvTotalCost = findViewById(R.id.tvTotalCost)
        emptyState = findViewById(R.id.emptyState)

        tvHallName.text = currentHall

        btnAddItem.setOnClickListener {
            showAddItemDialog()
        }

        btnAddFirstItem.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter(shoppingItems) { item, action ->
            when (action) {
                "edit" -> showEditItemDialog(item)
                "delete" -> deleteItem(item)
            }
        }
        rvShoppingList.layoutManager = LinearLayoutManager(this)
        rvShoppingList.adapter = adapter
    }

    private fun loadShoppingList() {
        shoppingListener = db.collection("shopping_lists")
            .whereEqualTo("hall", currentHall)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ShoppingList", "Error loading shopping list: ${error.message}")
                    Toast.makeText(this, "Error loading shopping list", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                shoppingItems.clear()
                snapshot?.documents?.forEach { document ->
                    val item = document.toObject(ShoppingListItem::class.java)
                    item?.let { shoppingItems.add(it) }
                }

                updateStats()
                adapter.updateItems(shoppingItems)
                emptyState.visibility = if (shoppingItems.isEmpty()) LinearLayout.VISIBLE else LinearLayout.GONE
            }
    }

    private fun updateStats() {
        val totalItems = shoppingItems.size
        val totalCost = shoppingItems.sumOf { it.cost }

        tvTotalItems.text = totalItems.toString() // FIXED: Now uses tvTotalItems
        tvTotalCost.text = "৳${totalCost.toInt()}"
    }

    private fun showAddItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_shopping_item, null)

        // Initialize all views
        val etItemName = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val etUnit = dialogView.findViewById<EditText>(R.id.etUnit)
        val etCost = dialogView.findViewById<EditText>(R.id.etCost)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        // Setup category spinner only
        val categories = arrayOf("Vegetables", "Meat", "Fish", "Grocery", "Spices", "Beverages", "Others")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        // Set default values
        etQuantity.setText("1.0")
        etUnit.setText("kg")
        spCategory.setSelection(0) // First item

        val dialog = AlertDialog.Builder(this)
            .setTitle("➕ Add Purchased Item")
            .setView(dialogView)
            .setPositiveButton("Add Item") { _, _ ->
                val itemName = etItemName.text.toString().trim()
                val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                val unit = etUnit.text.toString().trim()
                val cost = etCost.text.toString().toDoubleOrNull() ?: 0.0
                val category = spCategory.selectedItem.toString()
                val notes = etNotes.text.toString().trim()

                // Validation
                if (itemName.isEmpty()) {
                    Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (quantity <= 0) {
                    Toast.makeText(this, "Please enter valid quantity", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (unit.isEmpty()) {
                    Toast.makeText(this, "Please enter unit", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (cost <= 0) {
                    Toast.makeText(this, "Please enter valid cost", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addShoppingItem(itemName, quantity, unit, cost, category, notes)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addShoppingItem(
        itemName: String,
        quantity: Double,
        unit: String,
        cost: Double,
        category: String,
        notes: String
    ) {
        // Safe way to get user data
        val currentUser = AuthHelper.currentUserData
        val addedByName = when {
            currentUser is Staff -> currentUser.name
            currentUser is Manager -> "Manager"
            else -> "Unknown"
        }

        val addedByUid = AuthHelper.getCurrentUid() ?: ""

        val item = ShoppingListItem(
            id = db.collection("shopping_lists").document().id,
            itemName = itemName,
            quantity = quantity,
            unit = unit,
            cost = cost, // Real cost
            category = category,
            addedBy = addedByUid,
            addedByName = addedByName,
            hall = currentHall,
            notes = notes
        )

        db.collection("shopping_lists").document(item.id)
            .set(item)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Item added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Failed to add item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditItemDialog(item: ShoppingListItem) {
        Toast.makeText(this, "Edit functionality to be implemented", Toast.LENGTH_SHORT).show()
    }

    private fun deleteItem(item: ShoppingListItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${item.itemName}'?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("shopping_lists").document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        shoppingListener?.remove()
    }
}