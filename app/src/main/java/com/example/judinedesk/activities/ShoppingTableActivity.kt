package com.example.judinedesk.activities

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.adapters.ShoppingTableAdapter
import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.ShoppingListItem
import com.example.judinedesk.utils.AuthHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ShoppingTableActivity : AppCompatActivity() {

    private lateinit var rvShoppingTable: RecyclerView
    private lateinit var tvTotalItems: TextView
    private lateinit var tvTotalCost: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var ivBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentManager: Manager
    private var currentHall: String = ""
    private val shoppingItems = mutableListOf<ShoppingListItem>()
    private lateinit var adapter: ShoppingTableAdapter
    private var shoppingListener: ListenerRegistration? = null

    private val TAG = "ShoppingTable"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_table)

        Log.d(TAG, "Activity created")

        // Get manager data
        currentManager = getManagerData()
        currentHall = currentManager.hall

        Log.d(TAG, "Manager hall: $currentHall")

        setupViews()
        setupRecyclerView()
        loadShoppingList()
    }

    private fun getManagerData(): Manager {
        return (AuthHelper.currentUserData as? Manager) ?: Manager(
            uid = AuthHelper.getCurrentUid() ?: "",
            email = AuthHelper.getCurrentUid() ?: "Unknown",
            hall = "Unknown Hall",
            role = "manager"
        )
    }

    private fun setupViews() {
        try {
            rvShoppingTable = findViewById(R.id.rvShoppingTable)
            tvTotalItems = findViewById(R.id.tvTotalItems)
            tvTotalCost = findViewById(R.id.tvTotalCost)
            emptyState = findViewById(R.id.emptyState)
            tvTitle = findViewById(R.id.tvTitle)
            ivBack = findViewById(R.id.ivBack)

            tvTitle.text = "🛒 Shopping Items - $currentHall"

            ivBack.setOnClickListener {
                finish()
            }

            Log.d(TAG, "Views setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}")
            Toast.makeText(this, "Error setting up UI", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        try {
            adapter = ShoppingTableAdapter(shoppingItems)
            rvShoppingTable.layoutManager = LinearLayoutManager(this)
            rvShoppingTable.adapter = adapter
            Log.d(TAG, "RecyclerView setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView: ${e.message}")
        }
    }

    private fun loadShoppingList() {
        Log.d(TAG, "Loading shopping list for hall: $currentHall")

        shoppingListener = db.collection("shopping_lists")
            .whereEqualTo("hall", currentHall)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading shopping list: ${error.message}")
                    Toast.makeText(this, "Error loading shopping list", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                    return@addSnapshotListener
                }

                Log.d(TAG, "Snapshot received, documents: ${snapshot?.size()}")

                shoppingItems.clear()
                snapshot?.documents?.forEach { document ->
                    try {
                        val item = document.toObject(ShoppingListItem::class.java)
                        item?.let {
                            shoppingItems.add(it)
                            Log.d(TAG, "Loaded item: ${item.itemName} - ${item.cost}৳ - Hall: ${item.hall}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document: ${e.message}")
                    }
                }

                Log.d(TAG, "Total items loaded: ${shoppingItems.size}")

                runOnUiThread {
                    updateStats()
                    adapter.updateItems(shoppingItems)

                    if (shoppingItems.isEmpty()) {
                        showEmptyState()
                        Toast.makeText(this, "No shopping items found for $currentHall", Toast.LENGTH_LONG).show()
                    } else {
                        hideEmptyState()
                        Toast.makeText(this, "Loaded ${shoppingItems.size} items", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updateStats() {
        try {
            val totalItems = shoppingItems.size
            val totalCost = shoppingItems.sumOf { it.cost }

            tvTotalItems.text = totalItems.toString()
            tvTotalCost.text = "৳${totalCost.toInt()}"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating stats: ${e.message}")
        }
    }

    private fun showEmptyState() {
        try {
            emptyState.visibility = LinearLayout.VISIBLE
            rvShoppingTable.visibility = LinearLayout.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error showing empty state: ${e.message}")
        }
    }

    private fun hideEmptyState() {
        try {
            emptyState.visibility = LinearLayout.GONE
            rvShoppingTable.visibility = LinearLayout.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding empty state: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shoppingListener?.remove()
        Log.d(TAG, "Activity destroyed")
    }
}