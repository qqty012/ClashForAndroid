package com.github.kr328.clash.common.util

import android.app.Service
import android.os.Build

open class ServiceUtil {
    @Suppress("Deprecation")
    companion object {
        fun stopForeground(service: Service) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                service.stopForeground(true)
            }
        }
    }
}