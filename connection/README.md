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

`BleNearbyDevice` adapts a scanned advertisement and a pair of Kable characteristics to the shared `NearbyDevice` sensor interface. Subscribing connects, each notification is recorded as one message, and sending writes to the specified characteristic. It does not add stream framing or split oversized writes; use characteristics and a protocol appropriate for the peripheral's negotiated write size. Inspect its `failure` flow for connection or notification errors.

`BleNearbyDeviceScanner` adapts BLE discovery to the shared `NearbyDeviceScanner` interface. Provide the receive and send characteristics and a coroutine scope; the scanner deduplicates advertisements by their platform identifier and creates `BleNearbyDevice` instances. Discovery runs while subscribed, and stopping the scanner does not disconnect devices that callers have separately subscribed to. Its `failure` flow reports scan errors.

For Bluetooth Classic serial services, `connectRfcomm(address, service, security)` opens a byte-stream connection on Android. Use `read` and `write` from coroutines and close the connection when finished; cancelling an operation closes it. Android requires the Bluetooth permissions appropriate to the OS version. `rfcommAvailable` reports whether the platform has a Bluetooth adapter; it does not guarantee permissions or that Bluetooth is enabled. iOS and desktop currently report RFCOMM as unavailable.

`pairedBluetoothDevices()` lists bonded devices, and `acceptRfcomm(name, service, security)` accepts one incoming connection on Android. `SerialPortService` is the standard serial-port UUID. Listening stops after one connection or cancellation; the caller owns and closes the accepted connection. These APIs are unavailable on iOS and desktop, which return no paired devices and reject RFCOMM listening.
