package tn.dyndns.android.models

// DuckDNS config
data class DuckDnsConfig(
    val token: String
)

// Dynu config
data class DynuConfig(
    val username: String,
    val password: String
)

// Freeform config - supports any HTTP request
data class FreeformConfig(
    val url: String,
    val method: String = "GET",
    val authType: AuthType = AuthType.NONE,
    val username: String? = null,
    val password: String? = null,
    val body: String? = null,
    val headers: Map<String, String> = emptyMap()
)

enum class AuthType {
    NONE,
    BASIC
}