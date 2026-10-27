# ShieldScan Architecture

## Overview
ShieldScan is 100% on-device. No backend required.

## Modules

### 1. InventoryScanner
- Uses `PackageManager.getInstalledPackages(GET_PERMISSIONS | GET_SIGNING_CERTIFICATES)`
- Extracts APK path, size, SHA-256, signer cert, installer label, hidden detection

### 2. RiskEngine
- `CategoryExpectations` maps app category → expected permissions
- Weighted scoring for special access (Accessibility +40, Device Admin +35, etc.)
- `ComboRules` for trojan/RAT patterns
- Whitelist dampening for trusted packages

### 3. SpecialAccessAuditor
- `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
- `DevicePolicyManager.activeAdmins`
- `NotificationManagerCompat.getEnabledListenerPackages()`
- `AppOpsManager` for overlay & install-unknown-apps

### 4. BehaviorMonitor
- `NetworkStatsManager.queryDetailsForUid()` per-app data
- `UsageStatsManager.queryEvents()` for background/foreground

### 5. ApkInspector
- Lists exported components
- Extracts embedded URLs/IPs via string scan

### 6. Workers
- `MorningScanWorker` every 24h at 7 AM
- `DiffWorker` every 15 min to catch installs missed by receiver
- `InstallWatchService` foreground service with dynamic receiver for instant alerts

### 7. Data Layer
Room DB with `apps`, `scan_history`, `alerts`
