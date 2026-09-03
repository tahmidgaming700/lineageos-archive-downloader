package com.tahmidgaming.lineagearchive

import android.os.Build

object DeviceDetector {
    data class Info(
        val manufacturer: String = Build.MANUFACTURER,
        val model: String = Build.MODEL,
        val device: String = Build.DEVICE,
        val product: String = Build.PRODUCT
    )

    fun current(): Info = Info()

    // Prefer exact codenames first, then product identifiers, then a safe OEM/model fallback.
    fun match(info: Info, devices: List<LineageDevice>): LineageDevice? {
        val device = devices.firstOrNull { it.model.equals(info.device, ignoreCase = true) }
        if (device != null) return device
        val product = devices.firstOrNull { it.model.equals(info.product, ignoreCase = true) }
        if (product != null) return product
        return devices.firstOrNull {
            it.oem.equals(info.manufacturer, ignoreCase = true) &&
                it.name.contains(info.model, ignoreCase = true)
        }
    }
}
