package tn.dyndns.android.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dyndns_entries")
data class DyndnsEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val providerType: ProviderType,

    // Common fields
    val hostname: String,
    val fqdn: String? = null,

    // Status fields
    var lastUpdateTime: Long = 0,
    var lastStatus: String = "Unknown",
    var resolvedIp: String = "Unknown",
    var enabled: Boolean = true,

    // Provider-specific configuration (JSON string)
    val providerConfig: String
)

enum class ProviderType {
    DUCKDNS,
    DYNU,
    FREEFORM
}