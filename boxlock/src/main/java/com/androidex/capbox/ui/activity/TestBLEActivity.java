package com.androidex.capbox.ui.activity;

/**
 * Created by cts on 17/3/31.
 */

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.androidex.boxlib.service.BleService;
import com.androidex.boxlib.utils.Byte2HexUtil;
import com.androidex.capbox.R;
import com.androidex.capbox.base.BaseActivity;
import com.androidex.capbox.service.MyBleService;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.RLog;

import java.util.List;

import butterknife.Bind;

import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_ALL_DATA;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_DIS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_FAIL;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS_ALLCONNECTED;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLUTOOTH_OFF;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLUTOOTH_ON;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_ADDRESS;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_DATA;

/**
 * @author benjaminwan
 *         串口助手
 *         程序载入时自动搜索串口设备
 *         n,8,1，没得选
 */
public class TestBLEActivity extends BaseActivity {
    @Bind(R.id.editTextCOMA)
    EditText editTextCOMA;
    @Bind(R.id.editTextLines)
    EditText editTextLines;
    @Bind(R.id.editTextRecDisp)
    EditText editTextRecDisp;
    @Bind(R.id.editTextTimeCOMA)
    EditText editTextTimeCOMA;
    @Bind(R.id.checkBoxAutoClear)
    CheckBox checkBoxAutoClear;
    @Bind(R.id.checkBoxAutoCOMA)
    CheckBox checkBoxAutoCOMA;
    @Bind(R.id.ButtonClear)
    Button ButtonClear;
    @Bind(R.id.ButtonSendCOMA)
    Button ButtonSendCOMA;
    @Bind(R.id.radioButtonTxt)
    RadioButton radioButtonTxt;
    @Bind(R.id.radioButtonHex)
    RadioButton radioButtonHex;

    int iRecLines = 0;//接收区行数
    private int delayTime = 500;//定时发送的间隔时间
    private String address = null;
    private boolean isHex;
    private List<BluetoothDevice> allConnectDevice;

    @Override
    public void initData(Bundle savedInstanceState) {
        allConnectDevice = MyBleService.get().getAllConnectDevice();
        Loge("connected device size=" + allConnectDevice.size());
        BluetoothDevice device = allConnectDevice.get(0);
        address = device.getAddress();
        initBleBroadCast();
    }

    @Override
    public void setListener() {
        editTextCOMA.setOnEditorActionListener(new EditorActionEvent());
        editTextTimeCOMA.setOnEditorActionListener(new EditorActionEvent());
        editTextCOMA.setOnFocusChangeListener(new FocusChangeEvent());
        editTextTimeCOMA.setOnFocusChangeListener(new FocusChangeEvent());

        radioButtonTxt.setOnClickListener(new radioButtonClickEvent());
        radioButtonHex.setOnClickListener(new radioButtonClickEvent());
        ButtonClear.setOnClickListener(new ButtonClickEvent());
        ButtonSendCOMA.setOnClickListener(new ButtonClickEvent());
        checkBoxAutoCOMA.setOnCheckedChangeListener(new CheckBoxChangeEvent());

        editTextCOMA.setKeyListener(hexkeyListener);
    }

    /**
     * 初始化蓝牙广播
     */
    private void initBleBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_CONN_SUCCESS);
        intentFilter.addAction(BLE_CONN_SUCCESS_ALLCONNECTED);
        intentFilter.addAction(BLE_CONN_FAIL);
        intentFilter.addAction(BLE_CONN_DIS);
        intentFilter.addAction(BLUTOOTH_OFF);//手机蓝牙关闭
        intentFilter.addAction(BLUTOOTH_ON);//手机蓝牙打开
        intentFilter.addAction(ACTION_ALL_DATA);//读取调试数据
        context.registerReceiver(dataUpdateRecevice, intentFilter);
    }


    public void startSend() {
        Runnable sendRunnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(getDelayTime());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    sendData();
                }
            }
        };
        new Thread(sendRunnable).start();
    }

    @Override
    public void onClick(View view) {

    }

    KeyListener hexkeyListener = new NumberKeyListener() {
        public int getInputType() {
            return InputType.TYPE_CLASS_TEXT;
        }

        @Override
        protected char[] getAcceptedChars() {
            return new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};
        }
    };

    //编辑框焦点转移事件
    class FocusChangeEvent implements EditText.OnFocusChangeListener {
        public void onFocusChange(View v, boolean hasFocus) {
            if (v == editTextCOMA) {
                setSendData(editTextCOMA);
            } else if (v == editTextTimeCOMA) {
                setDelayTime(editTextTimeCOMA);
            }
        }
    }

    //编辑框完成事件
    class EditorActionEvent implements EditText.OnEditorActionListener {
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (v == editTextCOMA) {
                setSendData(editTextCOMA);
            } else if (v == editTextTimeCOMA) {
                setDelayTime(editTextTimeCOMA);
            }
            return false;
        }
    }

    //----------------------------------------------------Txt、Hex模式选择
    class radioButtonClickEvent implements RadioButton.OnClickListener {
        public void onClick(View v) {
            if (v == radioButtonTxt) {
                isHex = false;
            } else if (v == radioButtonHex) {
                isHex = true;
            }
            setSendData(editTextCOMA);
        }
    }

    //自动发送
    class CheckBoxChangeEvent implements CheckBox.OnCheckedChangeListener {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == checkBoxAutoCOMA) {
                if (isChecked) {
                    buttonView.setChecked(false);
                    return;
                }
                startSend();
            }
        }
    }

    //清除按钮、发送按钮
    class ButtonClickEvent implements View.OnClickListener {
        public void onClick(View v) {
            if (v == ButtonClear) {
                editTextRecDisp.setText("");
            } else if (v == ButtonSendCOMA) {
                sendData();
            }
        }
    }

    private void sendData() {
        if (MyBleService.getInstance().getConnectDevice(address) == null) {
            CommonKit.showErrorShort(context, "设备未连接");
            return;
        }
        MyBleService.get().sendData(address, Byte2HexUtil.decodeHex(getSendData()));
    }

    @NonNull
    private char[] getSendData() {
        String str = editTextCOMA.getText().toString().trim();
        if (TextUtils.isEmpty(str)) {
            CommonKit.showErrorShort(context, "请输入正确的指令");
        }
        return str.toCharArray();
    }

    //设置自动发送延时
    private void setDelayTime(TextView v) {
        String str = v.getText().toString().trim();
        if (TextUtils.isEmpty(str)) return;
        delayTime = Integer.parseInt(str);
    }

    private int getDelayTime() {
        return delayTime;
    }

    //设置自动发送数据
    private void setSendData(TextView v) {
        if (v == editTextCOMA) {

        }
    }

    private void updateText(String msg) {
        editTextRecDisp.append(String.format("%s\r\n", msg));
        //Log.i("TS",msg);
    }


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
                    BleService.get().enableNotify(address);
                    disProgress();
                    CommonKit.showOkShort(context, getResources().getString(R.string.bledevice_toast3));
                    break;

                case BLE_CONN_DIS://断开连接
                    RLog.d("断开连接");
                    CommonKit.showOkShort(context, getResources().getString(R.string.bledevice_toast4));
                    break;
                case BLE_CONN_FAIL://连接失败
                    disProgress();
                    CommonKit.showOkShort(context, getResources().getString(R.string.bledevice_toast8));
                    break;
                case BLUTOOTH_OFF:
                    RLog.d("手机蓝牙断开");
                    CommonKit.showOkShort(context, "手机蓝牙关闭");
                    break;
                case BLUTOOTH_ON:
                    RLog.d("手机蓝牙开启");
                    CommonKit.showOkShort(context, "手机蓝牙开启");
                    break;
                case ACTION_ALL_DATA:
                    RLog.d("读取到的数据=" + Byte2HexUtil.byte2Hex(b));
                    updateText(Byte2HexUtil.byte2Hex(b));
                    break;
                default:
                    break;
            }
        }
    };

    public static void lauch(Activity activity) {
        CommonKit.startActivity(activity, TestBLEActivity.class, null, false);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_test_ble;
    }
}
