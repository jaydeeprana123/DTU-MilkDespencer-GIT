package com.imdc.milkdespencer

import com.ftdi.j2xx.FT_Device
import com.imdc.milkdespencer.CashCollectorActivity.Companion.SetConfig
import device.itl.sspcoms.BarCodeReader
import device.itl.sspcoms.DeviceEvent
import device.itl.sspcoms.DeviceEventListener
import device.itl.sspcoms.DeviceFileUpdateListener
import device.itl.sspcoms.DeviceSetupListener
import device.itl.sspcoms.SSPComsConfig
import device.itl.sspcoms.SSPDevice
import device.itl.sspcoms.SSPSystem
import device.itl.sspcoms.SSPSystem.BillAction
import device.itl.sspcoms.SSPUpdate

/**
 * Created by tbeswick on 05/04/2017.
 */
class ITLDeviceCom : Thread(), DeviceSetupListener, DeviceEventListener, DeviceFileUpdateListener {
    var rbuf = ByteArray(READBUF_SIZE)
    var wbuf = ByteArray(WRITEBUF_SIZE)
    var mReadSize = 0
    private var ftDev: FT_Device? = null
    private var sspDevice: SSPDevice? = null
    private var deviceSetupListener: DeviceSetupListener? = null
    private var deviceEventListener: DeviceEventListener? = null
    private var deviceFileUpdateListener: DeviceFileUpdateListener? = null

    init {
        ssp = SSPSystem()
        ssp!!.setOnDeviceSetupListener(this)
        ssp!!.setOnEventUpdateListener(this)
        ssp!!.setOnDeviceFileUpdateListener(this)
    }

    fun setDeviceSetupListener(listener: DeviceSetupListener?) {
        deviceSetupListener = listener
    }

    fun setDeviceEventListener(listener: DeviceEventListener?) {
        deviceEventListener = listener
    }

    fun setDeviceFileUpdateListener(listener: DeviceFileUpdateListener?) {
        deviceFileUpdateListener = listener
    }

    fun setup(ftdev: FT_Device?, address: Int, escrow: Boolean, essp: Boolean, key: Long) {
        ftDev = ftdev
        ssp!!.SetAddress(address)
        ssp!!.EscrowMode(escrow)
        ssp!!.SetESSPMode(essp, key)
    }

    override fun run() {
        var readSize = 0
        ssp!!.Run()
        isrunning = true
        while (isrunning) {


            // poll for transmit data
            synchronized(ftDev!!) {
                val newdatalen = ssp!!.GetNewData(wbuf)
                if (newdatalen > 0) {
                    if (ssp!!.GetDownloadState() != SSPSystem.DownloadSetupState.active) {
                        ftDev!!.purge(1.toByte())
                    }
                    ftDev!!.write(wbuf, newdatalen)
                    ssp!!.SetComsBufferWritten(true)
                }
            }

            // poll for received
            synchronized(ftDev!!) {
                readSize = ftDev!!.queueStatus
                if (readSize > 0) {
                    mReadSize = readSize
                    if (mReadSize > READBUF_SIZE) {
                        mReadSize = READBUF_SIZE
                    }
                    readSize = ftDev!!.read(rbuf, mReadSize)
                    ssp!!.ProcessResponse(rbuf, readSize)
                }
            } // end of synchronized


            // coms config changes
            val cfg = ssp!!.GetComsConfig()
            if (cfg.configUpdate == SSPComsConfig.ComsConfigChangeState.ccNewConfig) {
                cfg.configUpdate = SSPComsConfig.ComsConfigChangeState.ccUpdating
                CashCollectorActivity.cashCollectorActivity!!.runOnUiThread {
                    SetConfig(
                        cfg.baud,
                        cfg.dataBits,
                        cfg.stopBits,
                        cfg.parity,
                        cfg.flowControl
                    )
                }
                cfg.configUpdate = SSPComsConfig.ComsConfigChangeState.ccUpdated
            }
            try {
                sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun OnNewDeviceSetup(dev: SSPDevice) {

        // set local device object
        sspDevice = dev
        // call to Main UI
        deviceSetupListener!!.OnNewDeviceSetup(dev)
        /*CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.DisplaySetUp(dev);
            }
        });*/
    }

    override fun OnDeviceDisconnect(dev: SSPDevice) {
        deviceSetupListener!!.OnDeviceDisconnect(dev)
        /*CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.DeviceDisconnected(dev);
            }
        });*/
    }

    override fun OnDeviceEvent(ev: DeviceEvent) {
        deviceEventListener!!.OnDeviceEvent(ev)
        /*   CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.DisplayEvents(ev);
            }
        });*/
    }

    override fun OnFileUpdateStatus(sspUpdate: SSPUpdate) {
        deviceFileUpdateListener!!.OnFileUpdateStatus(sspUpdate)
        /*CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.UpdateFileDownload(sspUpdate);
            }
        });*/
    }

    fun Stop() {
        ssp!!.Close()
        isrunning = false
    }

    fun SetSSPDownload(update: SSPUpdate?): Boolean {
        return ssp!!.SetDownload(update)
    }

    fun SetEscrowMode(mode: Boolean) {
        if (ssp != null) {
            ssp!!.EscrowMode(mode)
        }
    }

    fun SetDeviceEnable(en: Boolean) {
        if (ssp != null) {
            if (en) {
                ssp!!.EnableDevice()
            } else {
                ssp!!.DisableDevice()
            }
        }
    }

    fun SetEscrowAction(action: BillAction?) {
        if (ssp != null) {
            ssp!!.SetBillEscrowAction(action)
        }
    }

    fun SetBarcocdeConfig(cfg: BarCodeReader?) {
        if (ssp != null) {
            ssp!!.SetBarCodeConfiguration(cfg)
        }
    }

    fun GetDeviceCode(): Int {
        return if (ssp != null) {
            sspDevice!!.headerType.value
        } else {
            -1
        }
    }

    companion object {
        const val READBUF_SIZE = 256
        const val WRITEBUF_SIZE = 4096
        private var isrunning = false
        private var ssp: SSPSystem? = null
    }
}
