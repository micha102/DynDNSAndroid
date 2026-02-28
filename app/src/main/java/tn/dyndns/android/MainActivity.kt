package tn.dyndns.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tn.dyndns.android.database.AppDatabase
import tn.dyndns.android.models.DyndnsEntry

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: EntryAdapter
    private lateinit var viewModel: UpdateViewModel
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        // Handle insets for the scroll view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(this)
        viewModel = ViewModelProvider(this)[UpdateViewModel::class.java]

        fabAdd = findViewById(R.id.fabAdd)
        // Verify if battery optimizations are disabled
        checkBatteryOptimization()
        setupTabs()
        setupToolbar()
        setupObservers()

        fabAdd.setOnClickListener {
            showProviderDialog()
        }

    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_clear_logs -> {
                    viewModel.clearLogs()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTabs() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        setupRecyclerView()

        val pagerAdapter = ViewPagerAdapter(this)
        pagerAdapter.addFragment(EntriesFragment(), "Entries")
        pagerAdapter.addFragment(LogsFragment(), "Logs")
        viewPager.adapter = pagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) fabAdd.show() else fabAdd.hide()
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Entries" else "Logs"
        }.attach()

    }

    private fun setupRecyclerView() {
        adapter = EntryAdapter(
            onItemClick = { entry -> editEntry(entry) },
            onItemLongClick = { entry -> deleteEntry(entry) },
            onToggleEnabled = { entry -> toggleEnabled(entry) }
        )
    }

    private fun setupObservers() {
        viewModel.updateSummary.observe(this) { summary ->
            Toast.makeText(this, summary.summary, Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            db.dyndnsDao().getAllEntries().collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    fun getEntryAdapter() = adapter

    private fun showProviderDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Provider")
            .setItems(arrayOf("DuckDNS", "Dynu", "Freeform")) { _, which ->
                startActivity(Intent(this, SettingsActivity::class.java).apply {
                    putExtra("mode", "add")
                    putExtra("provider", which)
                })
            }
            .show()
    }

    private fun editEntry(entry: DyndnsEntry) {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            putExtra("mode", "edit")
            putExtra("entry_id", entry.id)
        })
    }

    private fun deleteEntry(entry: DyndnsEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete ${entry.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch { db.dyndnsDao().delete(entry) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleEnabled(entry: DyndnsEntry) {
        lifecycleScope.launch {
            db.dyndnsDao().update(entry.copy(enabled = !entry.enabled))
        }
    }
    @SuppressLint("ServiceCast")
    private fun checkBatteryOptimization() {
        // Check if the device is running at least Android 6.0 (API 23) because Doze mode is introduced in API 23.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            // Check if the app is ignoring battery optimizations
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "App is not battery optimized.")
            } else {
                Log.d(TAG, "App is battery optimized.")
                // Optionally, request the user to disable battery optimizations for your app
                showBatteryOptimizationDialog()
            }
        }
    }

    fun showBatteryOptimizationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Ignore Battery Optimization")
            .setMessage("To ensure the periodic task runs smoothly, please allow this app to run without battery optimizations. Would you like to go to the settings?")
            .setPositiveButton("OK") { dialog, _ ->
                // Navigate to the battery optimization settings
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                this.startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Nevermind") { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
            .setCancelable(false) // Optionally, make the dialog not cancellable
            .show()
    }
    companion object {
        val TAG = "MainActivity"
    }

}