# DynDNSAndroid

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

A lightweight, battery-efficient Android application that automatically keeps your Dynamic DNS (DynDNS) entries up to date. It monitors network changes and triggers updates when your IP address changes, ensuring your domains always point to your current public IP.

## ✨ Features

- **Automatic IP Detection**: Monitors network changes and updates your DynDNS entries in real-time
- **Multiple Provider Support**:
  - 🦆 **DuckDNS** - Using token-based authentication
  - 🔷 **Dynu** - Using username/password authentication  
  - ⚙️ **Freeform** - Custom HTTP requests with full control over method, headers, body, and authentication
- **Background Operation**: Uses Android's WorkManager for reliable, battery-efficient background updates
- **Per-Entry Configuration**: Each entry can be individually enabled/disabled
- **Comprehensive Logging**: Built-in log viewer to monitor all update activities
- **Modern Architecture**: Built with Kotlin, Coroutines, Room Database, and Material Design 3
- **No Foreground Service**: Designed to comply with Android's latest background execution limits

## 📸 Screenshots

Here are some screenshots of DynDNSAndroid in action:

| Main Screen | Select Provider | DuckDNS Config |
|:---:|:---:|:---:|
| ![MainActivity](/screenshots/MainActivity.png) | ![Select Provider](/screenshots/SelectProvider.png) | ![DuckDNS Config](/screenshots/DuckDNSSettings.png) |

| Main Screen | Logs Tab | Freeform Config |
|:---:|:---:|:---:|
| ![DuckDNS Configuration](/screenshots/MainActivity2.png) | ![Logs Tab](/screenshots/LogsTab.png) | ![Freeform Configuration](/screenshots/FreeformSettings.png) |

## 🚀 How It Works

1. **Network Monitoring**: The app registers a `BroadcastReceiver` for `CONNECTIVITY_CHANGE` events
2. **Boot Monitoring**: The app registers a `BroadcastReceiver` for `ACTION_BOOT_COMPLETED` events
3. **Smart Updates**: When a network change is detected, it triggers a `WorkManager` task with network constraints
4. **IP Resolution**: For each enabled entry, the app:
   - Resolves the current DNS IP for your domain
   - Sends an update request to your DynDNS provider
   - Logs the result and updates the database
5. **Periodic Fallback**: A 15-minute periodic WorkManager task ensures updates even if network events are missed
6. **Boot Handling**: After device reboot, a `JobScheduler` task restarts the monitoring system

## 📋 Requirements

- Android 7.0 (API 24) or higher
- Internet permission
- Access network state permission

## 🛠️ Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Database**: Room for persistent storage
- **Background Processing**: WorkManager + JobScheduler
- **Networking**: OkHttp
- **UI**: Material Design 3, ViewPager2, RecyclerView
- **Concurrency**: Coroutines + Flow

## 📦 Installation

Go to [Release](https://github.com/micha102/DynDNSAndroid/releases/) section.

### From Source
1. Clone the repository
   ```bash
   git clone https://github.com/micha102/DynDNSAndroid.git
   
2. Open the project in Android Studio
3. Build and run on your device/emulator

## 📦 Direct download

## 📱 Usage

### Adding a DynDNS Entry

1. **Open the app** and tap the **+** button
2. **Select your provider**:
   - **DuckDNS**: Enter your token
   - **Dynu**: Enter your username and password
   - **Freeform**: Configure custom HTTP request with URL, method, headers, and body
3. **Configure common fields**:
   - **Entry Name**: A friendly name for identification
   - **Hostname**: Your subdomain (e.g., `mydomain`)
   - **Full Domain**: Optional, complete FQDN (e.g., `mydomain.duckdns.org`)
4. **Save** the entry

### Managing Entries

- **Toggle** entries on/off using the switch on each card
- **Tap** an entry to edit its configuration
- **Long-press** an entry to delete it

### Viewing Logs

Switch to the **Logs** tab to see:
- Network change events
- Update attempts and results
- DNS resolution status
- Error messages for troubleshooting

## ⚙️ Configuration

Each entry has its own configuration stored in the Room database:

| Field | Description |
|-------|-------------|
| `name` | User-friendly identifier |
| `hostname` | Subdomain part of your DynDNS |
| `fqdn` | Full domain name (optional) |
| `providerConfig` | JSON string with provider-specific settings |

### Provider Configurations

**DuckDNS**:
```json
{"token": "your-token-here"}
```


**Dynu**:
```json
{"username": "your-username", "password": "your-password"}
```

**Freeform**:
```json
{
  "url": "https://api.example.com/update",
  "method": "POST",
  "authType": "BASIC",
  "username": "user",
  "password": "pass",
  "body": "hostname={hostname}&ip={ip}",
  "headers": {"X-Custom": "value"}
}
```

## 🔒 Permissions

The app requires the following permissions:
- `INTERNET` - To send update requests
- `ACCESS_NETWORK_STATE` - To monitor network changes
- `RECEIVE_BOOT_COMPLETED` - To restart after device reboot

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📬 Contact

Project Link: [https://github.com/micha102/DynDNSAndroid](https://github.com/micha102/DynDNSAndroid)

## 🙏 Acknowledgments
- [Deepseek](https://chat.deepseek.com/) for the code
- [DuckDNS](https://www.duckdns.org) for their free dynamic DNS service
- [Dynu](https://www.dynu.com) for their dynamic DNS API
- Android Jetpack libraries for making modern Android development possible
