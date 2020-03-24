package com.isolarcloud.blufi

import android.os.Binder

class BluServiceBinder constructor(var bservice: BluetoothService) : Binder() {

    fun getService(): BluetoothService {
        return bservice
    }
}