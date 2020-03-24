package com.isolarcloud.blufi

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.sungrowpower.widget.ExDialog
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), Observer {

    private val TAG = MainActivity::class.simpleName
    private val REQUEST_PERMISSION_LOCATION = 100
    private val REQUEST_OPEN_BLUETOOTH = 101

    private var mService: BluetoothService? = null
    private lateinit var mServiceConnect: BlueConnect

    private var mBleDevice : BleDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFastBle()
        setContentView(R.layout.activity_main)
        bindBlueService()
        if (BleManager.getInstance().isBlueEnable) {
            btn_open_blu.visibility = View.GONE
            tv_content.text = "蓝牙待机"
        }  else {
            btn_open_blu.visibility = View.VISIBLE
        }

        btn_connect.setOnClickListener {
            if (btn_connect.text == "连接ESP") {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        || ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    ) {
                        ExDialog.Builder(it.context).content("蓝牙需要定位权限")
                            .onAction { dialog, isPositive ->
                                if (isPositive) {
                                    ActivityCompat.requestPermissions(
                                        this, arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ), REQUEST_PERMISSION_LOCATION
                                    )
                                }
                            }
                            .show()
                    } else {
                        ActivityCompat.requestPermissions(
                            this, arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ), REQUEST_PERMISSION_LOCATION
                        )
                    }
                    return@setOnClickListener
                }
                connectDevice("BLUFI_DEVICE")
            } else {
                BleManager.getInstance().disconnect(mBleDevice)
            }
        }


        btn_open_blu.setOnClickListener {
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(btIntent, REQUEST_OPEN_BLUETOOTH)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (REQUEST_PERMISSION_LOCATION == requestCode) {

            if (verifyPermissions(grantResults)) {

            } else {

            }
        }
    }

    private fun bindBlueService() {
        mServiceConnect = BlueConnect()
        val serviceIntent = Intent(this, BluetoothService::class.java)
        bindService(serviceIntent, mServiceConnect, Context.BIND_AUTO_CREATE)
    }

    private fun initFastBle() {
        BleManager.getInstance().init(application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000)
            .setOperateTimeout(5000)
        val scanRuleConfig = BleScanRuleConfig.Builder()
            .setScanTimeOut(5000) // 扫描超时时间，可选，默认10秒
            .setAutoConnect(true)
            .build()
        BleManager.getInstance().initScanRule(scanRuleConfig)
    }

    /**
     * 连接设备
     */
    private fun connectDevice(name: String) {
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                tv_content.text = "正在搜索......"
                btn_connect.isEnabled = false
            }
            override fun onScanning(bleDevice: BleDevice) {
                if (name == bleDevice.name) {
                    BleManager.getInstance().cancelScan()
                }
                BleManager.getInstance().connect(mBleDevice, object : BleGattCallback() {
                    override fun onStartConnect() {}
                    override fun onConnectFail(
                        bleDevice: BleDevice,
                        exception: BleException
                    ) {
                        Log.d(TAG, "连接失败：${exception.description}")
                    }

                    override fun onConnectSuccess(
                        bleDevice: BleDevice,
                        gatt: BluetoothGatt,
                        status: Int
                    ) {
                        mBleDevice = bleDevice
                        Log.d(TAG, "连接上：${bleDevice.name}")
                        btn_connect.text = "断开ESP蓝牙"

                    }

                    override fun onDisConnected(
                        isActiveDisConnected: Boolean,
                        bleDevice: BleDevice,
                        gatt: BluetoothGatt,
                        status: Int
                    ) {
                        Log.d(TAG, "断开：${bleDevice.name}")

                    }
                })
            }
            override fun onScanFinished(scanResultList: List<BleDevice>) {
                btn_connect.isEnabled = true
                if (scanResultList?.isNotEmpty()) {

                    mBleDevice = scanResultList.find { it.name == name }
                    if (mBleDevice == null) {
                        tv_content.text = "未找到ESP设备"
                        return
                    }

                } else {
                    tv_content.text = "未找到ESP设备"
                }
            }
        })
    }

    inner class BlueConnect : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = (service as BluServiceBinder)?.getService()
            mService?.addObserver(this@MainActivity)
        }

    }

    override fun update(o: Observable?, arg: Any?) {
        val state = (arg as BluetoothHostStatus)
        state?.let {
            var btStatus = ""
            if (it.status == BluetoothHostStatus.OFF) btn_open_blu.visibility = View.VISIBLE else btn_open_blu.visibility = View.GONE
            if (it.status == BluetoothHostStatus.STANDBY) btn_connect.text ="连接ESP" else btn_connect.text = "断开ESP蓝牙"
            when (it.status) {
                BluetoothHostStatus.OFF -> {
                    btStatus = "蓝牙关闭"
                    tv_content.visibility = View.VISIBLE
                }
                BluetoothHostStatus.STANDBY -> {
                    btStatus = "蓝牙待机"
                }
                BluetoothHostStatus.SCANNING -> {
                    btStatus = "搜索蓝牙"
                }
                BluetoothHostStatus.INITIATOR -> {
                    btStatus = "发起连接"
                }
                BluetoothHostStatus.CONNECTED -> {
                    btStatus = "连接上蓝牙${state.deviceName}"
                }
                else -> ""
            }
            tv_content.text = btStatus
        }

    }

    /**
     * 检查所有给定的权限是否通过验证给定数组中的每个条目都具有该值 */
    private fun verifyPermissions(grantResults: IntArray): Boolean {
        // 至少检查一个结果
        if (grantResults.isEmpty()) {
            return false
        }

        // 验证是否已授予每个必需的权限，否则返回false
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OPEN_BLUETOOTH -> {
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnect)
    }
}
