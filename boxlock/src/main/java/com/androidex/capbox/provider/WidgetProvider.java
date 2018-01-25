package com.androidex.capbox.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.androidex.boxlib.modules.ServiceBean;
import com.androidex.boxlib.service.BleService;
import com.androidex.capbox.MainActivity;
import com.androidex.capbox.R;
import com.androidex.capbox.data.Event;
import com.androidex.capbox.service.MyBleService;
import com.androidex.capbox.service.WidgetService;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.RLog;

import de.greenrobot.event.EventBus;

import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_LOCK_OPEN_SUCCED;
import static com.androidex.boxlib.utils.BleConstants.BLE.ACTION_LOCK_STARTS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_DIS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_FAIL;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_SUCCESS_ALLCONNECTED;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLUTOOTH_OFF;
import static com.androidex.boxlib.utils.BleConstants.BLE.BLUTOOTH_ON;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_DATA;

/**
 * Created by daiyiming on 2016/11/28.
 */

public class WidgetProvider extends AppWidgetProvider {
    public static final String ACTION_UPDATE_ALL = "com.androidex.capbox.provider.ACTION_UPDATE_ALL";
    public static final String ACTION_CLICK = "com.androidex.capbox.provider.ACTION_CLICK";

    public static final String CLICK_BLE_CONNECTED = "com.androidex.capbox.provider.CLICK_BLE_CONNECTED";
    public static final String CLICK_LOCK_OPEN = "com.androidex.capbox.provider.CLICK_LOCK_OPEN";

    public static final String EXTRA_ITEM_POSITION = "position";
    public static final String EXTRA_ITEM_ADDRESS = "address";
    public static final String EXTRA_ITEM_CLICK = "com.androidex.capbox.provider.EXTRA_ITEM_CLICK";

    /**
     * 每次窗口小部件被点击更新都调用一次该方法// 每次 widget 被创建时，对应的将widget的id添加到set中
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        RLog.e("onUpdate");
        for (int i = 0; i < appWidgetIds.length; i++) {
            // 把这个Widget绑定到RemoteViewsService
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.provider_widget);
            // 设置适配器
            remoteViews.setRemoteAdapter(R.id.myListView, intent);

            // 设置当显示的widget_list为空显示的View
            remoteViews.setEmptyView(R.id.myListView, R.layout.list_item_provider_widget);
            remoteViews.setOnClickPendingIntent(R.id.ll_layout, getPendingIntentStartActivity(context));

            // 点击列表触发事件
            Intent clickIntent = new Intent(context, WidgetProvider.class);
            // 设置Action，方便在onReceive中区别点击事件
            clickIntent.setAction(ACTION_CLICK);
            clickIntent.setData(Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent pendingIntentTemplate = PendingIntent.getBroadcast(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setPendingIntentTemplate(R.id.myListView, pendingIntentTemplate);

            // 刷新按钮
            final Intent refreshIntent = new Intent(context, WidgetProvider.class);
            refreshIntent.setAction(ACTION_UPDATE_ALL);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.tv_title,
                    refreshPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        updateAllAppWidgets(context, AppWidgetManager.getInstance(context), intent);
    }

    // 更新所有的 widget
    private void updateAllAppWidgets(Context context, AppWidgetManager appWidgetManager, Intent intent) {
        // TODO:可以在这里做更多的逻辑操作，比如：数据处理、网络请求等。然后去显示数据
        RLog.e("action=" + intent.getAction());
        switch (intent.getAction()) {
            case ACTION_UPDATE_ALL:
                RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.provider_widget);
                remoteView.setOnClickPendingIntent(R.id.ll_layout, getPendingIntentStartActivity(context));

                int[] appIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
                WidgetService.boxlist();
                //appWidgetManager.notifyAppWidgetViewDataChanged(appIds, R.id.myListView);
                RLog.e("发送更新数据指令");
                appWidgetManager.updateAppWidget(appIds, remoteView);
                break;
            case ACTION_CLICK:
                RLog.e("listview被点击了");
                break;
            case CLICK_BLE_CONNECTED:
                RLog.e("接收到蓝牙点击广播");
                String address = intent.getStringExtra(EXTRA_ITEM_ADDRESS);
                if (address == null) return;
                ServiceBean device = BleService.get().getConnectDevice(address);
                if (device == null) {
                    BleService.get().connectionDevice(context, address);
                    EventBus.getDefault().postSticky(new Event.BleConnected(address));
                } else {
                    device.setActiveDisConnect(true);
                    MyBleService.get().disConnectDevice(address);
                }
                break;
            case CLICK_LOCK_OPEN:
                String address1 = intent.getStringExtra(EXTRA_ITEM_ADDRESS);
                if (address1 == null) return;
                if (BleService.get().getConnectDevice(address1) == null) {
                    CommonKit.showErrorShort(context, context.getResources().getString(R.string.bledevice_toast7));
                } else {
                    BleService.get().openLock(address1);
                }
                break;
            case BLE_CONN_SUCCESS://重复连接
            case BLE_CONN_SUCCESS_ALLCONNECTED://重复连接
                BleService.get().enableNotify(intent.getStringExtra(EXTRA_ITEM_ADDRESS));
                CommonKit.showOkShort(context, context.getResources().getString(R.string.bledevice_toast3));
                context.sendBroadcast(new Intent(ACTION_UPDATE_ALL));
                break;
            case BLE_CONN_DIS://蓝牙断开
                CommonKit.showOkShort(context, context.getResources().getString(R.string.bledevice_toast4));
                context.sendBroadcast(new Intent(ACTION_UPDATE_ALL));
                break;
            case BLE_CONN_FAIL://连接失败
                CommonKit.showOkShort(context, context.getResources().getString(R.string.bledevice_toast8));
                break;
            case BLUTOOTH_OFF:
                CommonKit.showOkShort(context, context.getResources().getString(R.string.bledevice_toast9));
                MyBleService.get().disConnectDeviceALL();
                context.sendBroadcast(new Intent(ACTION_UPDATE_ALL));
                break;
            case BLUTOOTH_ON:
                CommonKit.showOkShort(context, "手机蓝牙开启");
                break;
            case ACTION_LOCK_OPEN_SUCCED:
                String address2 = intent.getStringExtra(EXTRA_ITEM_ADDRESS);
                CommonKit.showOkShort(context, "开锁成功");
                MyBleService.get().getLockStatus(address2);
                break;
            case ACTION_LOCK_STARTS://锁状态FB 32 00 01 00 00 FE
                byte[] b = intent.getByteArrayExtra(BLECONSTANTS_DATA);
                if (b[2] == (byte) 0x01) {
                    //tv_status.setText("已打开");
                } else {
                    //tv_status.setText("已关闭");
                }
                break;
            default:
                break;
        }

    }

    /**
     * 发送广播
     *
     * @param context
     * @param buttonId
     * @return
     */

    private PendingIntent getPendingIntentBroadcast(Context context, int buttonId) {
        Intent intent = new Intent(ACTION_UPDATE_ALL);
        intent.setClass(context, WidgetProvider.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        RLog.e("发送广播=" + buttonId);
        return pi;
    }

    /**
     * 发送广播
     *
     * @param context
     * @return
     */
    private PendingIntent getPendingIntentStartActivity(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, MainActivity.class);
        intent.putExtra("widget", "widget");
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
        return pi;
    }

    /**
     * 当该窗口小部件第一次添加到桌面时调用该方法，可添加多次但只第一次调用
     */
    @Override
    public void onEnabled(Context context) {
        // 在第一个 widget 被创建时，开启服务
        RLog.e("onEnabled");
        super.onEnabled(context);
    }

    /**
     * 当 widget 被初次添加 或者 当 widget 的大小被改变时，被调用
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle
            newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    /**
     * 当小部件从备份恢复时调用该方法
     */
    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }

    /**
     * 每删除一次窗口小部件就调用一次// 当 widget 被删除时，对应的删除set中保存的widget的id
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    /**
     * 当最后一个该窗口小部件删除时调用该方法，注意是最后一个
     */
    @Override
    public void onDisabled(Context context) {
        // 在最后一个 widget 被删除时，终止服务
        Intent intent = new Intent(context, WidgetService.class);
        context.stopService(intent);
        super.onDisabled(context);
    }
}
