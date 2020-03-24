package com.isolarcloud.blufi

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.widget.ViewUtils
import java.util.*

/**
 * @Description: 蓝牙是否开启监听服务
 * @Author:         chenjunyong
 * @CreateDate: 2020/3/20 15:41
 */
class BluetoothService constructor(): Service() {

    companion object {
        const val TAG = "BluetoothLeService"
    }

    private lateinit var mObservable: BluObservable

    private var mBroadcastStateReceiver: BluetoothState? = null
    private var mBluetoothHostStatus: BluetoothHostStatus = BluetoothHostStatus()

    override fun onBind(intent: Intent?): IBinder? {
        return BluServiceBinder(this)
    }

    override fun onCreate() {
        super.onCreate()
        mObservable = BluObservable()
        if (mBroadcastStateReceiver == null) {
            mBroadcastStateReceiver = this.BluetoothState()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothDevice.ACTION_UUID)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(mBroadcastStateReceiver, intentFilter)
    }


    /**
     * 系统蓝牙开关状态 on off
     */
    inner class BluetoothOnOffState : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val blueState = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
            var state: Int = when (blueState) {
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF ->
                    BluetoothHostStatus.OFF

                BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_TURNING_ON ->
                    BluetoothHostStatus.STANDBY

                else -> BluetoothHostStatus.OFF
            }
            mObservable.notifyObservers(state)
        }
    }

    /**
     * 蓝牙状态 on off
     */
    inner class BluetoothState : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val bluetoothHostStatus = BluetoothHostStatus(BluetoothHostStatus.OFF, "")
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                //连接上了
                val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val name = device?.name ?: ""
                Toast.makeText(applicationContext,"连接上蓝牙：$name", Toast.LENGTH_LONG).show()
                bluetoothHostStatus.status = BluetoothHostStatus.CONNECTED
                bluetoothHostStatus.deviceName = name
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                //蓝牙连接被切断
                val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val name = device?.name ?: ""
                Toast.makeText(applicationContext, name + "的连接被断开", Toast.LENGTH_LONG).show()
                bluetoothHostStatus.status = BluetoothHostStatus.STANDBY
                bluetoothHostStatus.deviceName = name
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                val blueState = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                bluetoothHostStatus.status = when (blueState) {
                    BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF ->
                        BluetoothHostStatus.OFF

                    BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_TURNING_ON ->
                        BluetoothHostStatus.STANDBY

                    else -> BluetoothHostStatus.OFF
                }
            } else if (action == BluetoothDevice.ACTION_FOUND) {
                //蓝牙连接被切断
                val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                bluetoothHostStatus.status = BluetoothHostStatus.SCANNING
            }
            if (mBluetoothHostStatus.status != bluetoothHostStatus.status) {
                mBluetoothHostStatus = bluetoothHostStatus
                mObservable.notifyObservers(mBluetoothHostStatus)
            }
        }
    }

    /**
     * 添加监听者
     */
    fun addObserver(observer: Observer) {
        mObservable?.addObserver(observer)
    }

    inner class BluObservable : Observable() {
        override fun notifyObservers(arg: Any?) {
            setChanged()
            super.notifyObservers(arg)
        }
    }
}