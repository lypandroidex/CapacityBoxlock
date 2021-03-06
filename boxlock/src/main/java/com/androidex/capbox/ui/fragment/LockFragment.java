package com.androidex.capbox.ui.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidex.boxlib.modules.ServiceBean;
import com.androidex.capbox.R;
import com.androidex.capbox.base.BaseFragment;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.db.DbUtil;
import com.androidex.capbox.module.ActionItem;
import com.androidex.capbox.module.BaiduModel;
import com.androidex.capbox.module.BaseModel;
import com.androidex.capbox.module.BoxDeviceModel;
import com.androidex.capbox.module.CheckVersionModel;
import com.androidex.capbox.module.DeviceModel;
import com.androidex.capbox.module.LocationModel;
import com.androidex.capbox.service.DfuService;
import com.androidex.capbox.service.MyBleService;
import com.androidex.capbox.ui.activity.BoxDetailActivity;
import com.androidex.capbox.ui.activity.ConnectDeviceListActivity;
import com.androidex.capbox.ui.activity.DebugBLEActivity;
import com.androidex.capbox.ui.view.TitlePopup;
import com.androidex.capbox.utils.BleUpdateUtil;
import com.androidex.capbox.utils.CalendarUtil;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.Dialog;
import com.androidex.capbox.utils.RLog;
import com.androidex.capbox.utils.SystemUtil;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.e.ble.bean.BLEDevice;
import com.e.ble.scan.BLEScanCfg;
import com.e.ble.scan.BLEScanListener;
import com.e.ble.util.BLEError;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;
import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.boxlib.cache.SharedPreTool.HIGHEST_TEMP;
import static com.androidex.boxlib.cache.SharedPreTool.IS_BIND_NUM;
import static com.androidex.boxlib.cache.SharedPreTool.LOWEST_TEMP;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_BOX_FIRMWARE_VER;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_BOX_HARDWARE_VER;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_BOX_POLICE_CHANGE;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_BOX_STARTS;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_BOX_STARTS_CHANGE;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_END_TAST;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_HEART;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_LOCK_OPEN_SUCCED;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_LOCK_STARTS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_DIS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_FAIL;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_RSSI_FAIL;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_RSSI_SUCCED;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS_ALLCONNECTED;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLUTOOTH_OFF;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLUTOOTH_ON;
import static com.androidex.boxlib.utils.BleConstants.BLE.SCAN_PERIOD;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_ADDRESS;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_DATA;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_ELECTRIC_QUANTITY;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_HUM;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_TEMP;
import static com.androidex.capbox.utils.Constants.CONFIG.OPEN_DFU_UPDATE;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_NAME;
import static com.androidex.capbox.utils.Constants.EXTRA_BOX_UUID;
import static com.androidex.capbox.utils.Constants.EXTRA_DEVICE;
import static com.androidex.capbox.utils.Constants.EXTRA_ITEM_ADDRESS;
import static com.androidex.capbox.utils.Constants.EXTRA_PAGER_SIGN;

public class LockFragment extends BaseFragment implements OnClickListener {
    private final static String TAG = "LockFragment";
    @Bind(R.id.main_bt_ble)
    ImageView ble;
    @Bind(R.id.iv_menu)
    ImageView iv_menu;
    @Bind(R.id.main_bt_ble_close)
    ImageView close;
    @Bind(R.id.iv_lock)
    ImageView iv_lock;
    @Bind(R.id.main_tv_title)
    TextView main_tv_title;
    @Bind(R.id.tv_boxConfig)
    TextView tv_boxConfig;
    @Bind(R.id.tv_latitude)
    TextView tv_latitude;
    @Bind(R.id.tv_longitude)
    TextView tv_longitude;
    @Bind(R.id.tv_elevation)
    TextView tv_elevation;
    @Bind(R.id.tv_locationTime)
    TextView tv_locationTime;
    @Bind(R.id.tv_lastTime)
    TextView tv_lastTime;
    @Bind(R.id.tv_address)
    TextView tv_address;
    @Bind(R.id.main_tv_name)
    TextView name;
    @Bind(R.id.main_tv_device)
    TextView tv_deviceMac;
    @Bind(R.id.tv_status)
    TextView tv_status;
    @Bind(R.id.tv_boxStarts)
    TextView tv_boxStarts;
    @Bind(R.id.current_temp)
    TextView current_temp;
    @Bind(R.id.main_tv_maxtemp)
    TextView main_tv_maxtemp;
    @Bind(R.id.main_tv_mintemp)
    TextView main_tv_mintemp;
    @Bind(R.id.current_hum)
    TextView current_hum;
    @Bind(R.id.tv_electric_quantity)
    TextView tv_electric_quantity;
    @Bind(R.id.main_tv_maxhum)
    TextView maxhum;
    @Bind(R.id.main_tv_minhum)
    TextView minhum;
    @Bind(R.id.tv_chargingState)
    TextView tv_chargingState;
    @Bind(R.id.progressBar_dfu)
    ProgressBar mProgressBarOtaUpload;
    @Bind(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @Bind(R.id.rl_police)
    RelativeLayout rl_police;
    @Bind(R.id.tv_police)
    TextView tv_police;

    private Timer timer_location = new Timer();// 设计定时器
    private TimerTask timer_getlocation;
    private String address = null;
    private String uuid = null;
    private String deviceName = "Box";
    private String latitude;
    private String longitude;
    private String elevation;
    private GeoCoder mSearch;
    private TitlePopup titlePopup;
    private boolean isConnect = false;
    private boolean mReceiverTag = false;   //广播接受者标识
    BluetoothDevice bluetoothDevice;

    @Override
    public void initData() {
        isConnect = false;
        Bundle bundle = getArguments();
        DeviceModel deviceModel = bundle.getParcelable(EXTRA_DEVICE);
        if (deviceModel != null) {
            address = deviceModel.getAddress();
            uuid = deviceModel.getUuid();
            deviceName = deviceModel.getName();
        } else {
            address = bundle.getString(EXTRA_ITEM_ADDRESS);
            uuid = bundle.getString(EXTRA_BOX_UUID);
            deviceName = bundle.getString(EXTRA_BOX_NAME);
        }
        if (uuid != null) getLocation(true);
        main_tv_title.setText(CalendarUtil.getName(address));
        initView();
        iniRefreshView();
        initMap();
        initBleBroadCast();
        if (MyBleService.getInstance().getConnectDevice(address) != null) {
            initTemp();
        }
    }

    /**
     * 获取温湿度和定位信息
     */
    private void initTemp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 4; i++) {
                        Thread.sleep(300);
                        switch (i) {
                            case 0:
                                MyBleService.getInstance().getTempEnergy(address);//获取温度信息
                                break;
                            case 1:
                                MyBleService.getInstance().getLocation(address);//获取定位信息
                                break;
                            case 2:
                                MyBleService.getInstance().getLockStatus(address);//获取锁状态
                                break;
                            case 3:
                                MyBleService.getInstance().getBoxStatus(address);//获取箱状态
                                break;
                            default:
                                break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 初始化蓝牙广播
     */
    private void initBleBroadCast() {
        if (!mReceiverTag) {     //在注册广播接受者的时候 判断是否已被注册,避免重复多次注册广播
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BLE_CONN_SUCCESS);
            intentFilter.addAction(BLE_CONN_SUCCESS_ALLCONNECTED);
            intentFilter.addAction(BLE_CONN_FAIL);
            intentFilter.addAction(BLE_CONN_DIS);
            intentFilter.addAction(ACTION_LOCK_STARTS);//锁状态
            intentFilter.addAction(BLE_CONN_RSSI_SUCCED);
            intentFilter.addAction(BLE_CONN_RSSI_FAIL);
            intentFilter.addAction(ACTION_HEART);//收到心跳返回
            intentFilter.addAction(ACTION_END_TAST);//结束携行押运
            intentFilter.addAction(ACTION_LOCK_OPEN_SUCCED);//开锁成功
            intentFilter.addAction(BLUTOOTH_OFF);//手机蓝牙关闭
            intentFilter.addAction(BLUTOOTH_ON);//手机蓝牙打开
            intentFilter.addAction(ACTION_BOX_HARDWARE_VER);//获取箱体的硬件版本号
            intentFilter.addAction(ACTION_BOX_FIRMWARE_VER);//获取箱体的固件版本号
            intentFilter.addAction(ACTION_BOX_STARTS_CHANGE);//箱体状态变更
            intentFilter.addAction(ACTION_BOX_POLICE_CHANGE);//报警信息变更
            mReceiverTag = true;    //标识值 赋值为 true 表示广播已被注册
            context.registerReceiver(dataUpdateRecevice, intentFilter);
        }
    }

    private void iniRefreshView() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 模拟刷新完成
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        boxlocation(uuid);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });
    }


    @Override
    public void setListener() {
        ble.setOnClickListener(this);
        close.setOnClickListener(this);
        tv_boxConfig.setOnClickListener(this);
        iv_lock.setOnClickListener(this);
        iv_menu.setOnClickListener(this);
        rl_police.setOnClickListener(this);
    }

    public void initMap() {
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(listener);
    }

    public void getAddress(String latitudeStr, String longitudeStr) {
        LatLng latLng = new LatLng(Double.valueOf(latitudeStr).doubleValue(), Double.valueOf(longitudeStr).doubleValue());
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
    }

    private void initView() {
        tv_deviceMac.setText(address);
        deviceName=CalendarUtil.getName(address);
        name.setText(deviceName);
        if (address == null || address.equals("")) {
            tv_deviceMac.setText("FF:FF:FF:FF:FF:FF");
        } else {
            tv_deviceMac.setText(address);
        }
        String lowestTemp = SharedPreTool.getInstance(context).getStringData(LOWEST_TEMP, "0");
        String highestTemp = SharedPreTool.getInstance(context).getStringData(HIGHEST_TEMP, "80");
        main_tv_mintemp.setText(String.format("%s℃", lowestTemp));
        main_tv_maxtemp.setText(String.format("%s℃", highestTemp));
        // 实例化标题栏弹窗
        titlePopup = new TitlePopup(context, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        // 给标题栏弹窗添加子类
        titlePopup.addAction(new ActionItem(context, "结束携行", R.mipmap.finish_carry));
        titlePopup.addAction(new ActionItem(context, "连接列表", R.mipmap.connectlist));
        titlePopup.addAction(new ActionItem(context, "箱体升级", R.mipmap.setting));
        if (SystemUtil.isApkInDebug(context)) {
            titlePopup.addAction(new ActionItem(context, "箱体调试", R.mipmap.setting));
        }

        titlePopup.setItemOnClickListener(new TitlePopup.OnItemOnClickListener() {
            @Override
            public void onItemClick(ActionItem item, int position) {
                switch (position) {
                    case 0:
                        showEndCarryAffirm();//显示确认结束弹窗
                        break;
                    case 1:
                        ConnectDeviceListActivity.lauch(context);
                        break;
                    case 2:
                        if (!OPEN_DFU_UPDATE) {
                            CommonKit.showOkShort(context, "该功能尚未开启");
                        } else {
                            if (MyBleService.getInstance().getConnectDevice(address) == null) {
                                CommonKit.showErrorShort(context, "请先连接设备");
                                return;
                            }
                            //MyBleService.getInstance().getFirmWareVer(address);//获取箱体的软件版本号
                            startDFU();
                        }
                        break;
                    case 3:
                        DebugBLEActivity.lauch(context);
                        break;

                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_bt_ble_close://连接状态，点击关闭
                ServiceBean device = MyBleService.getInstance().getConnectDevice(address);
                if (device != null) {
                    device.setActiveDisConnect(true);
                }
                MyBleService.getInstance().disConnectDevice(address);
                updateBleView(View.VISIBLE, View.GONE);
                break;

            case R.id.main_bt_ble://蓝牙未连接，点击连接
                if (MyBleService.getInstance().getConnectDevice(address) == null) {
                    scanLeDevice();
                } else {
                    CommonKit.showMsgShort(context, "设备已连接");
                    updateBleView(View.GONE, View.VISIBLE);
                }
                break;

            case R.id.iv_lock:
                openLock();
                break;

            case R.id.tv_boxConfig:
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_BOX_NAME, deviceName);
                bundle.putString(EXTRA_ITEM_ADDRESS, address);
                bundle.putString(EXTRA_BOX_UUID, uuid);
                bundle.putInt(EXTRA_PAGER_SIGN, 1);//0表示从设备列表跳转过去的1表示从监控页跳转
                BoxDetailActivity.lauch(getActivity(), bundle);
                break;

            case R.id.iv_menu:
                titlePopup.show(v);
                break;
            case R.id.rl_police:
                rl_police.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    /**
     * 显示确认结束弹窗
     */
    private void showEndCarryAffirm() {
        Dialog.showAlertDialog(context, "确定结束携行押运吗？", new Dialog.DialogFingerListener() {
            @Override
            public void confirm() {
                if (MyBleService.getInstance().getConnectDevice(address) != null) {
                    MyBleService.getInstance().endTask(address);
                } else {
                    CommonKit.showErrorShort(context, "请先连接蓝牙");
                    return;
                }
            }

            @Override
            public void cancel() {
            }
        });
    }

    /**
     * 结束携行
     */
    private void endTask() {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.endTask(getToken(), getUserName(), uuid, new ResultCallBack<BaseModel>() {
            @Override
            public void onSuccess(int statusCode, Headers headers, BaseModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    switch (model.code) {
                        case Constants.API.API_OK:
                            CommonKit.showOkShort(context, "结束成功");
                            setDeviceCaryyStarts(false);
                            break;
                        default:
                            if (model.info != null) {
                                CommonKit.showErrorShort(context, model.info);
                            } else {
                                CommonKit.showOkShort(context, "结束失败");
                            }
                            break;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                CommonKit.showErrorShort(context, "网络异常");
            }
        });
    }

    /**
     * 设置是否携行状态
     *
     * @param isflag
     */
    private void setDeviceCaryyStarts(boolean isflag) {
        boxlist();
        ServiceBean obj = SharedPreTool.getInstance(context).getObj(ServiceBean.class, address);
        if (obj != null) {
            obj.setStartCarry(isflag);
            SharedPreTool.getInstance(context).saveObj(obj, address);
        } else {
            ServiceBean device = MyBleService.getInstance().getConnectDevice(address);
            if (device != null) {
                device.setStartCarry(isflag);
                SharedPreTool.getInstance(context).saveObj(device, address);
            }
        }
        Object obj1 = SharedPreTool.getInstance(context).getObj(ServiceBean.class, address);
        RLog.e("存储携行状态" + obj1.toString());
    }

    /**
     * 获取设备列表
     */
    public void boxlist() {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.boxlist(getToken(), getUserName(), new ResultCallBack<BoxDeviceModel>() {
            @Override
            public void onSuccess(int statusCode, Headers headers, BoxDeviceModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    switch (model.code) {
                        case Constants.API.API_OK:
                            int carryNum = 0;
                            for (BoxDeviceModel.device device : model.devicelist) {
                                if (device.deviceStatus == 2) {
                                    carryNum++;
                                }
                            }
                            //SharedPreTool.getInstance(context).setIntData(IS_BIND_NUM, carryNum++);
                            SharedPreTool.getInstance(context).setIntData(IS_BIND_NUM, model.devicelist.size());
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
            }

            @Override
            public void onStart() {
                super.onStart();
            }
        });
    }


    /**
     * 开始和停止获取定位信息
     *
     * @param flag
     */
    private void getLocation(boolean flag) {
        if (flag) {
            timer_getlocation = new TimerTask() {
                @Override
                public void run() {
                    Log.i("LockFragment", "开始定位");
                    boxlocation(uuid);
                }
            };
            if (timer_location == null) timer_location = new Timer();
            timer_location.schedule(timer_getlocation, 100, 1 * 30 * 1000);
        } else {
            if (timer_getlocation != null) {
                timer_getlocation.cancel();
                timer_location.cancel();
                timer_getlocation = null;
                timer_location = null;
                Log.i("LockFragment", "停止定位");
            }
        }
    }

    /**
     * 开锁
     */
    private void openLock() {
        if (MyBleService.getInstance().getConnectDevice(address) != null) {
            MyBleService.getInstance().openLock(address);
        } else {
            CommonKit.showErrorShort(context, getResources().getString(R.string.setting_tv_ble_disconnect));
        }
    }

    /**
     * 开始扫描
     */
    private void scanLeDevice() {
        showProgress("搜索设备中...");
        BLEScanCfg scanCfg = new BLEScanCfg.ScanCfgBuilder(SCAN_PERIOD).builder();
        MyBleService.getInstance().startScanner(scanCfg, new BLEScanListener() {
            boolean isScanDevice;//是否扫描到设备

            @Override
            public void onScannerStart() {
                isScanDevice = false;
            }

            @Override
            public void onScanning(BLEDevice device) {
                if (device.getMac().equals(address)) {
                    isScanDevice = true;
                    Log.e(TAG, "搜索到设备...");
                    stopScanLe();
                    showProgress("正在连接设备");
                    MyBleService.getInstance().connectionDevice(context, address);
                    detectionIsConnect();
                }
            }

            @Override
            public void onScannerStop() {
                showProgress("扫描结束...");
                Loge(TAG, "扫描结束");
                disProgress();
                if (!isScanDevice) {
                    CommonKit.showErrorShort(context, "未搜索到设备");
                }
            }

            @Override
            public void onScannerError(int errorCode) {
                stopScanLe();
                if (errorCode == BLEError.BLE_CLOSE) {
                    Log.e(TAG, "蓝牙未打开，请打开蓝牙后重试");
                    CommonKit.showErrorShort(context, "蓝牙未打开，请打开蓝牙后重试");
                } else {
                    CommonKit.showErrorShort(context, "扫描出现异常");
                }
            }
        });
    }

    /**
     * 检测是否连接成功，不成功再连接一次
     */
    private void detectionIsConnect() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Loge(TAG, "检测是否连接");
                if (isConnect) return;
                if (MyBleService.getInstance().getConnectDevice(address) == null) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress("正在重连设备");
                        }
                    });
                    MyBleService.getInstance().connectionDevice(context, address);
                }
            }
        }, 3000);
    }

    private void stopScanLe() {
        disProgress();
        MyBleService.getInstance().stopScan();
    }

    /**
     * 检测版本号，包括APP的，箱体的，腕表的
     * {"appFileName":"boxlock-3.apk",
     * "appVersion":"3","boxFileName":"20171129.hex",
     * "boxVersion":"0.0.1","code":0,"watchFileName":"20171129.hex",
     * "watchVersion":"0.0.2"}
     */
    public void checkVersion() {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.checkVersion(getToken(), getUserName(), new ResultCallBack<CheckVersionModel>() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onSuccess(int statusCode, Headers headers, CheckVersionModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    switch (model.code) {
                        case Constants.API.API_OK:
                            //startDFU();
                            break;
                        case Constants.API.API_FAIL:
                            RLog.d("网络连接失败");
                            CommonKit.showErrorShort(context, "网络连接失败");
                            break;
                        default:
                            if (model.info != null) {
                                RLog.d(model.info);
                                CommonKit.showErrorShort(context, model.info);
                            } else {
                                RLog.d("网络连接失败");
                                CommonKit.showErrorShort(context, "网络连接失败");
                            }
                            break;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                RLog.d("网络连接失败");
                CommonKit.showErrorShort(context, "网络连接失败");
            }
        });
    }

    /**
     * 启动蓝牙升级服务
     */
    private void startDFU() {
        DfuServiceListenerHelper.registerProgressListener(context, mDfuProgressListener);
        bluetoothDevice = MyBleService.getInstance().getBluetoothDevice(address);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //大于等于6.0的时候，禁用PEN
            BleUpdateUtil.startDFU(context, bluetoothDevice, true, false, false, 0, "");
        } else {
            BleUpdateUtil.startDFU(context, bluetoothDevice, true, false, true, 0, "");
        }
    }

    /**
     * 获取设备信息
     *
     * @param uuid
     */
    private void boxlocation(String uuid) {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.getboxLocation(getToken(), getUserName(), uuid,
                new ResultCallBack<LocationModel>() {
                    @Override
                    public void onSuccess(int statusCode, Headers headers, LocationModel model) {
                        super.onSuccess(statusCode, headers, model);
                        if (model != null) {
                            switch (model.code) {
                                case Constants.API.API_OK:
                                    LocationModel.Data data = model.data;
                                    latitude = data.getLatitude();//纬度
                                    longitude = data.getLongitude();//经度
                                    elevation = data.getElevation();//高程
                                    tv_latitude.setText(latitude);
                                    tv_longitude.setText(longitude);
                                    tv_elevation.setText(elevation);
                                    if (latitude.equals("0") && longitude.equals("0")) {
                                        tv_address.setText("携行箱位置未知");
                                    } else {
                                        getBaiduLocation(latitude, longitude);
                                    }
                                    if (data.locationTime != null) {
                                        tv_locationTime.setText(data.locationTime);
                                    } else {
                                        tv_locationTime.setText("0");
                                    }
                                    if (data.lastTime != null) {
                                        tv_lastTime.setText(data.lastTime);
                                    } else {
                                        tv_lastTime.setText("0");
                                    }
                                    break;
                                case Constants.API.API_FAIL:
                                    if (model.info != null) {
                                        CommonKit.showErrorShort(getContext(), model.info);
                                    }
                                    break;
                                default:
                                    break;
                            }
                            RLog.e(model.toString());
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Request request, Exception e) {
                        super.onFailure(statusCode, request, e);
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                    }
                });
    }

    /**
     * 经纬度纠偏
     *
     * @param latitude
     * @param longitude
     */
    public void getBaiduLocation(String latitude, String longitude) {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        NetApi.getLocation(latitude, longitude, new ResultCallBack<BaiduModel>() {
            @Override
            public void onSuccess(int statusCode, Headers headers, BaiduModel model) {
                super.onSuccess(statusCode, headers, model);
                if (model != null) {
                    if (model.error == 0) {
                        String str_lat = new String(Base64.decode(model.y, Base64.DEFAULT));
                        String str_lon = new String(Base64.decode(model.x, Base64.DEFAULT));
                        if (!str_lat.equals("0") && !str_lat.equals("") && !str_lon.equals("0") && !str_lon.equals("")) {
                            getAddress(str_lat, str_lon);
                        } else {
                            tv_address.setText("位置未知");
                        }
                    } else {
                        RLog.e("经纬度转换出错 error=" + model.error);
                    }
                } else {
                    RLog.e("经纬度转换出错 model=null");
                }
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                RLog.e("经纬度转换出错");
                CommonKit.showErrorShort(getContext(), "网络连接失败");
            }
        });
    }


    DfuProgressListener mDfuProgressListener = new DfuProgressListener() {

        @Override
        public void onDeviceConnecting(String deviceAddress) {
            //当DFU服务开始与DFU目标连接时调用的方法//升级服务开始与硬件设备连接
            Log.d("debug", "DFU服务开始与DFU目标连接," + deviceAddress);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            //方法在服务成功连接时调用，发现服务并在DFU目标上找到DFU服务。//升级服务连接成功
            Log.d("debug", "服务成功连接,发现服务并在DFU目标上找到DFU服务." + deviceAddress);

        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            //当DFU进程启动时调用的方法。 这包括读取DFU版本特性，发送DFU START命令以及Init数据包（如果设置）。
            Log.d("debug", "DFU进程启动," + deviceAddress);//升级进程启动
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            //当DFU进程启动和要发送的字节时调用的方法。
            Log.d("debug", "DFU进程启动和要发送的字节," + deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            //当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方法。 将发送开关命令，并且DFU过程应该再次开始。 此调用后不会有onDeviceDisconnected（String）事件。
            Log.d("debug", "当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方");
            //硬件设备切换到升级模式
        }

        /**
         * @param deviceAddress
         * @param percent  进度
         * @param speed
         * @param avgSpeed
         * @param currentPart
         * @param partsTotal
         */
        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            //在上传固件期间调用的方法。 它不会使用相同的百分比值调用两次，但是在小型固件文件的情况下，可能会省略一些值。\
            mProgressBarOtaUpload.setVisibility(View.VISIBLE);
            mProgressBarOtaUpload.setProgress(percent);//状态：升级中...
            RLog.d("dfu  percent:" + percent + " partsTotal:" + partsTotal + " speed=" + speed);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            //在目标设备上验证新固件时调用的方法。
            RLog.d("dfu  目标设备上验证新固件时调用的方法");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            //服务开始断开与目标设备的连接时调用的方法。
            RLog.d("dfu  服务开始断开与目标设备的连接时调用的方法");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            //当服务从设备断开连接时调用的方法。 设备已重置。
            RLog.d("dfu  当服务从设备断开连接时调用的方法。 设备已重置。");
            DfuServiceListenerHelper.unregisterProgressListener(context, mDfuProgressListener);
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            //Method called when the DFU process succeeded.
            RLog.d("dfu  DFU已完成");//升级成功！
            mProgressBarOtaUpload.setVisibility(View.GONE);
            DfuServiceListenerHelper.unregisterProgressListener(context, mDfuProgressListener);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            //当DFU进程已中止时调用的方法。
            RLog.d("dfu  当DFU进程已中止时调用的方法。");//升级进程已中止.
            DfuServiceListenerHelper.unregisterProgressListener(context, mDfuProgressListener);
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            //发生错误时调用的方法。
            RLog.d("dfu  发生错误时调用的方法onError");//升级发生错误.
            DfuServiceListenerHelper.unregisterProgressListener(context, mDfuProgressListener);
        }
    };

    BroadcastReceiver dataUpdateRecevice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceMac = intent.getStringExtra(BLECONSTANTS_ADDRESS);
            byte[] b = intent.getByteArrayExtra(BLECONSTANTS_DATA);
            if (deviceMac == null) return;
            if (!address.equals(deviceMac)) return;
            switch (intent.getAction()) {
                case BLE_CONN_SUCCESS:
                case BLE_CONN_SUCCESS_ALLCONNECTED:
                    isConnect = true;
                    disProgress();
                    updateBleView(View.GONE, View.VISIBLE);
                    initTemp();//获取温湿度信息
                    break;
                case BLE_CONN_DIS://断开连接
                    Loge(TAG, "断开连接");
                    updateBleView(View.VISIBLE, View.GONE);
                    break;
                case BLE_CONN_FAIL://连接失败
                    disProgress();
                    CommonKit.showOkShort(context, getResources().getString(R.string.bledevice_toast8));
                    break;
                case BLUTOOTH_OFF://手机蓝牙断开
                    ServiceBean device = MyBleService.getInstance().getConnectDevice(address);
                    if (device != null) {
                        device.setActiveDisConnect(true);
                    }
                    MyBleService.getInstance().disConnectDevice(address);
                    updateBleView(View.VISIBLE, View.GONE);
                    break;
                case BLUTOOTH_ON://手机蓝牙开启
                    scanLeDevice();
                    break;
                case ACTION_LOCK_OPEN_SUCCED:
                    if (b.length >= 2 && b[1] == (byte) 0x01) {
                        CommonKit.showOkShort(context, "开锁失败");
                        MyBleService.getInstance().insertReceiveData(address, "开锁失败");
                    } else {
                        CommonKit.showOkShort(context, "开锁成功");
                        MyBleService.getInstance().insertReceiveData(address, "开锁成功");
                    }
                    break;
                case ACTION_LOCK_STARTS:
                    if (b.length >= 2) {
                        tv_status.setText(b[1] == (byte) 0x00 ? "已打开" : "已关闭");
                        MyBleService.getInstance().insertReceiveData(address, b[1] == (byte) 0x00 ? "锁已打开" : "锁已关闭");
                    } else if (b.length > 0) {
                        tv_status.setText(b[0] == (byte) 0x00 ? "已关闭" : "已打开");
                        MyBleService.getInstance().insertReceiveData(address, b[0] == (byte) 0x00 ? "锁是关着的" : "锁是开着的");
                    } else {
                        tv_status.setText("未知");
                    }
                    break;
                case ACTION_BOX_STARTS:
                    if (b.length >= 2) {
                        tv_boxStarts.setText(b[1] == (byte) 0x00 ? "已打开" : "已关闭");
                        MyBleService.getInstance().insertReceiveData(address, b[1] == (byte) 0x00 ? "箱子打开" : "箱子关闭");
                    } else {
                        tv_boxStarts.setText("未知");
                    }
                    break;
                case BLE_CONN_RSSI_SUCCED://获取到信号强度值
                    break;
                case BLE_CONN_RSSI_FAIL://获取信号强度失败
                    break;
                case ACTION_HEART://温度、湿度、电量
                    current_temp.setText(intent.getStringExtra(BLECONSTANTS_TEMP) != null ? intent.getStringExtra(BLECONSTANTS_TEMP) : "");
                    current_hum.setText(intent.getStringExtra(BLECONSTANTS_HUM) != null ? intent.getStringExtra(BLECONSTANTS_HUM) : "");
                    tv_electric_quantity.setText(intent.getStringExtra(BLECONSTANTS_ELECTRIC_QUANTITY) != null ? intent.getStringExtra(BLECONSTANTS_ELECTRIC_QUANTITY) : "");
                    break;
                case ACTION_END_TAST://结束携行押运
                    Log.e(TAG, "结束携行押运");
                    if (b.length < 2) return;
                    switch (b[2]) {
                        case (byte) 0x00://成功
                            endTask();
                            break;
                        case (byte) 0x01://失败
                            CommonKit.showErrorShort(context, "结束失败");
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_BOX_HARDWARE_VER://获取箱体的硬件版本号
                    checkVersion();
                    break;
                case ACTION_BOX_FIRMWARE_VER://获取箱体的固件版本号
                    checkVersion();
                    break;
                case ACTION_BOX_STARTS_CHANGE://箱体状态变更
                    //开/关箱：0x01/0x02
                    //开/关锁：0x03/0x04
                    //APP开锁：0x05
                    //指纹开锁：0x06
                    //远程开锁：0x07
                    //进入静默/退出静默：0x08/0x09
                    //开始充电/断开充电/充满： 0x0a/0x0b /0x0c
                    switch (b[1]) {
                        case (byte) 0x01:
                            tv_boxStarts.setText("打开");
                            //CommonKit.showMsgShort(context, "箱子已打开");
                            break;
                        case (byte) 0x02:
                            tv_boxStarts.setText("关闭");
                            // CommonKit.showMsgShort(context, "箱子已关闭");
                            break;
                        case (byte) 0x03:
                            tv_status.setText("已打开");
                            //CommonKit.showOkShort(context, "锁已打开");
                            break;
                        case (byte) 0x04:
                            tv_status.setText("关闭");
                            //CommonKit.showOkShort(context, "锁已关闭");
                            break;
                        case (byte) 0x05:

                            break;
                        case (byte) 0x06:

                            break;
                        case (byte) 0x07:

                            break;
                        case (byte) 0x08:

                            break;
                        case (byte) 0x09:

                            break;
                        case (byte) 0x0a:
                            tv_chargingState.setText("正在充电");
                            break;
                        case (byte) 0x0b:
                            tv_chargingState.setText("未充电");
                            break;
                        case (byte) 0x0c:
                            tv_chargingState.setText("已充满");
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_BOX_POLICE_CHANGE://报警信息变更
                    //锁异常报警：0x01
                    //异动报警：0x02
                    //破拆：0x03
                    switch (b[1]) {
                        case (byte) 0x01:
                            rl_police.setVisibility(View.VISIBLE);
                            tv_police.setText("锁异常报警");
                            break;
                        case (byte) 0x02:
                            rl_police.setVisibility(View.VISIBLE);
                            tv_police.setText("异动报警");
                            break;
                        case (byte) 0x03:
                            rl_police.setVisibility(View.VISIBLE);
                            tv_police.setText("破拆报警");
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
    };

    private void updateBleView(int visible, int gone) {
        tv_status.setText("关闭");
        ble.setVisibility(visible);
        close.setVisibility(gone);
    }

    OnGetGeoCoderResultListener listener = new OnGetGeoCoderResultListener() {

        public void onGetGeoCodeResult(GeoCodeResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                //没有检索到结果
                return;
            }
            //获取地理编码结果
        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                //没有找到检索结果
                tv_address.setText("");
                return;
            }
            tv_address.setText(result.getAddress() + result.getSematicDescription());
            //获取反向地理编码结果
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (address == null) return;
        if (MyBleService.getInstance().getConnectDevice(address) == null) {
            scanLeDevice();
            isConnect = false;
        } else {
            CommonKit.showMsgShort(context, "设备已连接");
            updateBleView(View.GONE, View.VISIBLE);
            initTemp();
        }
    }

    //注销广播
    private void unregisterReceiver() {
        if (mReceiverTag && dataUpdateRecevice != null) {   //判断广播是否注册
            mReceiverTag = false;   //Tag值 赋值为false 表示该广播已被注销
            context.unregisterReceiver(dataUpdateRecevice);   //注销广播
        }
    }

    @Override
    public void onPause() {
        stopScanLe();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getLocation(false);
        if (mSearch != null)
            mSearch.destroy();
        unregisterReceiver();
    }


    @Override
    public int getLayoutId() {
        return R.layout.fragment_lock;
    }
}
