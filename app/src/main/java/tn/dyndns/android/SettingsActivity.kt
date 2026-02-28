package tn.dyndns.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import tn.dyndns.android.database.AppDatabase
import tn.dyndns.android.models.*
import tn.dyndns.android.workers.DyndnsUpdateWorker

class SettingsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var name: EditText
    private lateinit var hostname: EditText
    private lateinit var fqdn: EditText

    private lateinit var duckdnsView: View
    private lateinit var dynuView: View
    private lateinit var freeformView: View

    // DuckDNS views
    private lateinit var duckdnsToken: EditText

    // Dynu views
    private lateinit var dynuUsername: EditText
    private lateinit var dynuPassword: EditText

    // Freeform views
    private lateinit var freeformUrl: EditText
    private lateinit var freeformMethodGet: RadioButton
    private lateinit var freeformMethodPost: RadioButton
    private lateinit var freeformAuthNone: RadioButton
    private lateinit var freeformAuthBasic: RadioButton
    private lateinit var freeformUsername: EditText
    private lateinit var freeformPassword: EditText
    private lateinit var freeformBody: EditText
    private lateinit var freeformHeadersContainer: LinearLayout
    private lateinit var freeformAddHeader: Button
    private val freeformHeaderViews = mutableListOf<View>()
    private lateinit var providerFlipper: ViewFlipper

    private var entryId = 0L
    private var providerType = ProviderType.DUCKDNS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll_view)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = AppDatabase.getDatabase(applicationContext)
        initViews()

        val mode = intent.getStringExtra("mode")
        if (mode == "edit") {
            entryId = intent.getLongExtra("entry_id", 0)
            loadEntry()
        } else if (mode == "add") {
            providerType = when (intent.getIntExtra("provider", 0)) {
                0 -> ProviderType.DUCKDNS
                1 -> ProviderType.DYNU
                else -> ProviderType.FREEFORM
            }
            updateProviderUI()
        }
    }

    private fun initViews() {
        name = findViewById(R.id.editTextName)
        hostname = findViewById(R.id.editTextHostname)
        fqdn = findViewById(R.id.editTextFqdn)

        providerFlipper = findViewById(R.id.providerFlipper)

        // Clear any existing views
        providerFlipper.removeAllViews()


        // Inflate and add provider layouts to ViewFlipper
        layoutInflater.inflate(R.layout.provider_duckdns, providerFlipper, true).also {
            duckdnsView = it
            duckdnsToken = it.findViewById(R.id.duckdnsToken)
        }

        layoutInflater.inflate(R.layout.provider_dynu, providerFlipper, true).also {
            dynuView = it
            dynuUsername = it.findViewById(R.id.dynuUsername)
            dynuPassword = it.findViewById(R.id.dynuPassword)
        }

        layoutInflater.inflate(R.layout.provider_freeform, providerFlipper, true).also {
            freeformView = it
            freeformUrl = it.findViewById(R.id.freeformUrl)
            freeformMethodGet = it.findViewById(R.id.freeformMethodGet)
            freeformMethodPost = it.findViewById(R.id.freeformMethodPost)
            freeformAuthNone = it.findViewById(R.id.freeformAuthNone)
            freeformAuthBasic = it.findViewById(R.id.freeformAuthBasic)
            freeformUsername = it.findViewById(R.id.freeformUsername)
            freeformPassword = it.findViewById(R.id.freeformPassword)
            freeformBody = it.findViewById(R.id.freeformBody)
            freeformHeadersContainer = it.findViewById(R.id.freeformHeadersContainer)
            freeformAddHeader = it.findViewById(R.id.freeformAddHeader)
            // ... initialize freeform views
            setupFreeformListeners(it)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveEntry() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }

    private fun setupFreeformListeners(view: View) {
        // Show/hide basic auth fields
        val authGroup = view.findViewById<RadioGroup>(R.id.freeformAuthGroup)
        authGroup.setOnCheckedChangeListener { _, checkedId ->
            val showBasic = checkedId == R.id.freeformAuthBasic
            view.findViewById<LinearLayout>(R.id.freeformBasicAuthLayout).visibility =
                if (showBasic) View.VISIBLE else View.GONE
        }
        val freeformMethodGroup = view.findViewById<RadioGroup>(R.id.freeformMethodGroup)

        freeformMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            val showPost = checkedId == R.id.freeformMethodPost
            view.findViewById<LinearLayout>(R.id.freeformPostBodyLayout).visibility =
                if (showPost) View.VISIBLE else View.GONE
        }

        // Add header button
        freeformAddHeader.setOnClickListener {
            addFreeformHeaderRow(null, null)
        }
    }

    private fun addFreeformHeaderRow(key: String?, value: String?) {
        val row = layoutInflater.inflate(R.layout.item_header, freeformHeadersContainer, false)

        val editKey = row.findViewById<EditText>(R.id.editHeaderKey)
        val editValue = row.findViewById<EditText>(R.id.editHeaderValue)
        val btnRemove = row.findViewById<ImageButton>(R.id.btnRemoveHeader)

        editKey.setText(key ?: "")
        editValue.setText(value ?: "")

        btnRemove.setOnClickListener {
            freeformHeadersContainer.removeView(row)
            freeformHeaderViews.remove(row)
        }

        freeformHeadersContainer.addView(row)
        freeformHeaderViews.add(row)
    }

    private fun getFreeformHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        freeformHeaderViews.forEach { view ->
            val key = view.findViewById<EditText>(R.id.editHeaderKey).text.toString()
            val value = view.findViewById<EditText>(R.id.editHeaderValue).text.toString()
            if (key.isNotBlank() && value.isNotBlank()) {
                headers[key] = value
            }
        }
        return headers
    }

    private fun updateProviderUI() {
        val index = when (providerType) {
            ProviderType.DUCKDNS -> 0
            ProviderType.DYNU -> 1
            ProviderType.FREEFORM -> 2
        }
        providerFlipper.displayedChild = index
    }

    private fun loadEntry() {
        lifecycleScope.launch {
            val entry = db.dyndnsDao().getEntryById(entryId)
            if (entry != null) {
                providerType = entry.providerType

                // Load common fields
                name.setText(entry.name)
                hostname.setText(entry.hostname)
                fqdn.setText(entry.fqdn)

                // Load provider-specific config based on type
                when (entry.providerType) {
                    ProviderType.DUCKDNS -> {
                        val config = ConfigConverter.fromJson<DuckDnsConfig>(entry.providerConfig)
                        duckdnsToken.setText(config.token)
                    }
                    ProviderType.DYNU -> {
                        val config = ConfigConverter.fromJson<DynuConfig>(entry.providerConfig)
                        dynuUsername.setText(config.username)
                        dynuPassword.setText(config.password)
                    }
                    ProviderType.FREEFORM -> {
                        val config = ConfigConverter.fromJson<FreeformConfig>(entry.providerConfig)
                        freeformUrl.setText(config.url)
                        if (config.method == "POST") freeformMethodPost.isChecked = true
                        if (config.authType == AuthType.BASIC) {
                            freeformAuthBasic.isChecked = true
                            freeformUsername.setText(config.username)
                            freeformPassword.setText(config.password)
                        }
                        freeformBody.setText(config.body ?: "")
                        config.headers.forEach { (key, value) ->
                            addFreeformHeaderRow(key, value)
                        }
                    }
                }

                updateProviderUI()
            }
        }
    }

    private fun saveEntry() {
        if (name.text.isBlank() || hostname.text.isBlank()) {
            Toast.makeText(this, "Name and hostname required", Toast.LENGTH_SHORT).show()
            return
        }

        val providerConfig = when (providerType) {
            ProviderType.DUCKDNS -> {
                if (duckdnsToken.text.isBlank()) {
                    Toast.makeText(this, "Token required for DuckDNS", Toast.LENGTH_SHORT).show()
                    return
                }
                val config = DuckDnsConfig(
                    token = duckdnsToken.text.toString()
                )
                ConfigConverter.toJson(config)
            }
            ProviderType.DYNU -> {
                if (dynuUsername.text.isBlank() || dynuPassword.text.isBlank()) {
                    Toast.makeText(this, "Username and password required for Dynu", Toast.LENGTH_SHORT).show()
                    return
                }
                val config = DynuConfig(
                    username = dynuUsername.text.toString(),
                    password = dynuPassword.text.toString()
                )
                ConfigConverter.toJson(config)
            }
            ProviderType.FREEFORM -> {
                if (freeformUrl.text.isBlank()) {
                    Toast.makeText(this, "URL required for freeform", Toast.LENGTH_SHORT).show()
                    return
                }
                val config = FreeformConfig(
                    url = freeformUrl.text.toString(),
                    method = if (freeformMethodPost.isChecked) "POST" else "GET",
                    authType = if (freeformAuthBasic.isChecked) AuthType.BASIC else AuthType.NONE,
                    username = if (freeformAuthBasic.isChecked) freeformUsername.text.toString() else null,
                    password = if (freeformAuthBasic.isChecked) freeformPassword.text.toString() else null,
                    body = if (freeformMethodPost.isChecked) freeformBody.text.toString() else null,
                    headers = getFreeformHeaders()
                )
                ConfigConverter.toJson(config)
            }
        }

        lifecycleScope.launch {
            val entry = DyndnsEntry(
                id = entryId,
                name = name.text.toString(),
                providerType = providerType,
                hostname = hostname.text.toString(),
                fqdn = fqdn.text.toString().takeIf { it.isNotBlank() },
                providerConfig = providerConfig
            )

            if (entryId == 0L) {
                db.dyndnsDao().insert(entry)
            } else {
                db.dyndnsDao().update(entry)
            }

            DyndnsUpdateWorker.triggerImmediate(applicationContext)
            finish()
        }
    }
}