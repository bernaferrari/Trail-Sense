# Bluetooth Low Energy

`scanBluetooth()` returns a flow of Kable advertisements. The `connection` module exports [Kable](https://github.com/JuulLabs/kable/tree/0.45.0), so its filtering, connection state, service discovery, characteristic and descriptor APIs are available directly in shared code.

```kotlin
val advertisement = scanBluetooth().first()
advertisement.withBluetoothConnection {
    val characteristic = characteristicOf(serviceUuid, characteristicUuid)
    val value = read(characteristic)
    write(characteristic, value, WriteType.WithResponse)
    // collect observe(characteristic) for notifications or indications
}
```

Import `kotlinx.coroutines.flow.first`, `com.juul.kable.characteristicOf` and `com.juul.kable.WriteType`; service and characteristic identifiers are `kotlin.uuid.Uuid` values. `withBluetoothConnection` closes the peripheral when the block exits. For a connection that outlives a block, use `advertisement.connectBluetooth()` and close the returned peripheral when finished.

Android apps must declare and request the Bluetooth permissions appropriate for their Android version. iOS apps must provide `NSBluetoothAlwaysUsageDescription` in their Info.plist. Bluetooth access is subject to platform and device availability. Desktop support uses Kable's native Bluetooth backend.
