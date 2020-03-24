package com.isolarcloud.blufi

/**
 * @Description: 蓝牙主机状态
 * @Author:         chenjunyong
 * @CreateDate: 2020/3/20 16:04
 */
data class BluetoothHostStatus(var status: Int = OFF,
                               var deviceName: String = "") {

    companion object {
        // 关闭
        val OFF: Int = 0x00
        // 待机
        val STANDBY: Int = 0x01
        // 扫描
        val SCANNING: Int = 0x02
        // 发起连接
        val INITIATOR: Int = 0x03
        // 连接
        val CONNECTED: Int = 0x04
    }
}