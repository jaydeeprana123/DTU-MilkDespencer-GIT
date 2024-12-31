package com.imdc.milkdespencer;

import com.ftdi.j2xx.FT_Device;

import device.itl.sspcoms.BarCodeReader;
import device.itl.sspcoms.DeviceEvent;
import device.itl.sspcoms.DeviceEventListener;
import device.itl.sspcoms.DeviceFileUpdateListener;
import device.itl.sspcoms.DeviceSetupListener;
import device.itl.sspcoms.SSPComsConfig;
import device.itl.sspcoms.SSPDevice;
import device.itl.sspcoms.SSPSystem;
import device.itl.sspcoms.SSPUpdate;

/**
 * Created by tbeswick on 05/04/2017.
 */

public class ITLDeviceCom extends Thread implements DeviceSetupListener, DeviceEventListener, DeviceFileUpdateListener {


    static final int READBUF_SIZE = 256;
    static final int WRITEBUF_SIZE = 4096;
    private static boolean isrunning = false;
    private static SSPSystem ssp;
    byte[] rbuf = new byte[READBUF_SIZE];
    byte[] wbuf = new byte[WRITEBUF_SIZE];
    int mReadSize = 0;
    private FT_Device ftDev = null;
    private SSPDevice sspDevice = null;
    private DeviceSetupListener deviceSetupListener;
    private DeviceEventListener deviceEventListener;
    private DeviceFileUpdateListener deviceFileUpdateListener;

    public ITLDeviceCom() {

        ssp = new SSPSystem();

        ssp.setOnDeviceSetupListener(this);
        ssp.setOnEventUpdateListener(this);
        ssp.setOnDeviceFileUpdateListener(this);

    }

    public void setDeviceSetupListener(DeviceSetupListener listener) {
        this.deviceSetupListener = listener;
    }

    public void setDeviceEventListener(DeviceEventListener listener) {
        this.deviceEventListener = listener;
    }

    public void setDeviceFileUpdateListener(DeviceFileUpdateListener listener) {
        this.deviceFileUpdateListener = listener;
    }

    public void setup(FT_Device ftdev, int address, boolean escrow, boolean essp, long key) {

        ftDev = ftdev;
        ssp.SetAddress(address);
        ssp.EscrowMode(escrow);
        ssp.SetESSPMode(essp, key);

    }

    @Override
    public void run() {

        int readSize = 0;
        ssp.Run();

        isrunning = true;
        while (isrunning) {


            // poll for transmit data
            synchronized (ftDev) {
                int newdatalen = ssp.GetNewData(wbuf);
                if (newdatalen > 0) {
                    if (ssp.GetDownloadState() != SSPSystem.DownloadSetupState.active) {
                        ftDev.purge((byte) 1);
                    }
                    ftDev.write(wbuf, newdatalen);
                    ssp.SetComsBufferWritten(true);
                }
            }

            // poll for received
            synchronized (ftDev) {
                readSize = ftDev.getQueueStatus();
                if (readSize > 0) {
                    mReadSize = readSize;
                    if (mReadSize > READBUF_SIZE) {
                        mReadSize = READBUF_SIZE;
                    }
                    readSize = ftDev.read(rbuf, mReadSize);
                    ssp.ProcessResponse(rbuf, readSize);
                }
                //    } // end of if(readSize>0)
            }  // end of synchronized


            // coms config changes
            final SSPComsConfig cfg = ssp.GetComsConfig();
            if (cfg.configUpdate == SSPComsConfig.ComsConfigChangeState.ccNewConfig) {
                cfg.configUpdate = SSPComsConfig.ComsConfigChangeState.ccUpdating;
                CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CashCollectorActivity.SetConfig(cfg.baud, cfg.dataBits, cfg.stopBits, cfg.parity, cfg.flowControl);
                    }
                });
                cfg.configUpdate = SSPComsConfig.ComsConfigChangeState.ccUpdated;
            }
            try {
                sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void OnNewDeviceSetup(final SSPDevice dev) {

        // set local device object
        sspDevice = dev;
        // call to Main UI
        deviceSetupListener.OnNewDeviceSetup(dev);
        /*CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.DisplaySetUp(dev);
            }
        });*/

    }

    @Override
    public void OnDeviceDisconnect(final SSPDevice dev) {

        deviceSetupListener.OnDeviceDisconnect(dev);
        /*CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.DeviceDisconnected(dev);
            }
        });*/
    }


    @Override
    public void OnDeviceEvent(final DeviceEvent ev) {
        deviceEventListener.OnDeviceEvent(ev);
        /*   CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.DisplayEvents(ev);
            }
        });*/
    }


    @Override
    public void OnFileUpdateStatus(final SSPUpdate sspUpdate) {

        deviceFileUpdateListener.OnFileUpdateStatus(sspUpdate);
        /*CashCollectorActivity.cashCollectorActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CashCollectorActivity.UpdateFileDownload(sspUpdate);
            }
        });*/

    }


    void Stop() {
        ssp.Close();
        isrunning = false;
    }


    boolean SetSSPDownload(final SSPUpdate update) {
        return ssp.SetDownload(update);

    }


    void SetEscrowMode(boolean mode) {
        if (ssp != null) {
            ssp.EscrowMode(mode);
        }

    }

    void SetDeviceEnable(boolean en) {
        if (ssp != null) {
            if (en) {
                ssp.EnableDevice();
            } else {
                ssp.DisableDevice();
            }
        }
    }

    void SetEscrowAction(SSPSystem.BillAction action) {
        if (ssp != null) {
            ssp.SetBillEscrowAction(action);
        }
    }


    void SetBarcocdeConfig(BarCodeReader cfg) {
        if (ssp != null) {
            ssp.SetBarCodeConfiguration(cfg);
        }
    }


    int GetDeviceCode() {
        if (ssp != null) {
            return sspDevice.headerType.getValue();
        } else {
            return -1;
        }
    }


}
