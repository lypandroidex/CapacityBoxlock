package com.androidex.capbox.ui.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidex.capbox.R;
import com.androidex.capbox.base.UserBaseActivity;
import com.androidex.capbox.base.imageloader.UILKit;
import com.androidex.capbox.data.Event;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.data.net.NetApi;
import com.androidex.capbox.data.net.base.ResultCallBack;
import com.androidex.capbox.module.BaseModel;
import com.androidex.capbox.module.CheckVersionModel;
import com.androidex.capbox.ui.widget.SecondTitleBar;
import com.androidex.capbox.utils.CommonKit;
import com.androidex.capbox.utils.Constants;
import com.androidex.capbox.utils.RLog;
import com.androidex.capbox.utils.SystemUtil;

import java.io.File;

import butterknife.Bind;
import butterknife.OnClick;
import okhttp3.Headers;
import okhttp3.Request;

import static com.androidex.capbox.data.cache.SharedPreTool.LOGIN_STATUS;

/**
 * @title 设置界面
 */
public class SettingActivity extends UserBaseActivity {
    @Bind(R.id.title)
    SecondTitleBar title;
    @Bind(R.id.ll_notification)
    LinearLayout ll_notification;
    @Bind(R.id.ll_changePassword)
    LinearLayout ll_changePassword;
    @Bind(R.id.ll_clearCache)
    LinearLayout ll_clearCache;
    @Bind(R.id.ll_about)
    LinearLayout ll_about;
    @Bind(R.id.ll_searchVersion)
    LinearLayout ll_searchVersion;
    @Bind(R.id.tv_versionNum)
    TextView tv_versionNum;
    @Bind(R.id.tv_cacheSize)
    TextView tv_cacheSize;

    @Override
    public void initData(Bundle savedInstanceState) {
        RLog.e("SettingActivity is onCreat");
        tv_versionNum.setText(getResources().getString(R.string.about_tv_versionNum) + CommonKit.getVersionName(context));
        updateCache();
        initView();
    }

    private void initView() {
        if (SystemUtil.isApkInDebug(context)) {
            title.getRightTv().setVisibility(View.VISIBLE);
        } else {
            title.getRightTv().setVisibility(View.GONE);
        }
    }

    @Override
    public void setListener() {
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DebugBLEActivity.lauch(context);
            }
        });
    }

    @OnClick({
            R.id.tv_logout,
            R.id.tv_logoff,
            R.id.ll_about,
            R.id.ll_clearCache,
            R.id.ll_searchVersion,
            R.id.ll_changePassword,
    })
    public void clickEvent(View view) {
        String username = SharedPreTool.getInstance(context).getStringData(SharedPreTool.PHONE, null);
        switch (view.getId()) {
            case R.id.ll_about:
                String url = "http://www.lockaxial.com/about/aboutandroidex.html";
                if (!TextUtils.isEmpty(url)) {
                    if (!CommonKit.isNetworkAvailable(context)) {
                        CommonKit.showErrorShort(context, "请检查网络");
                        return;
                    }
                    WebViewActivity.lauch(context, url);
                }
                break;
            case R.id.tv_logout:
                if (username != null) {
                    if (!CommonKit.isNetworkAvailable(context)) {
                        CommonKit.showErrorShort(context, "设备未连接网络");
                        return;
                    }
                    NetApi.userLogout(getToken(), username, new ResultCallBack<BaseModel>() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, BaseModel model) {
                            super.onSuccess(statusCode, headers, model);
                            if (model != null) {
                                switch (model.code) {
                                    case Constants.API.API_OK:
                                        CommonKit.showOkShort(context, getString(R.string.hint_logout_ok));
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
                removeCacheForSp();//删除缓存
                CommonKit.finishActivity(context);
                postSticky(new Event.UserLoginEvent());//登录状态发生改变
                break;
            case R.id.tv_logoff:
                if (username != null) {
                    if (!CommonKit.isNetworkAvailable(context)) {
                        CommonKit.showErrorShort(context, "设备未连接网络");
                        return;
                    }
                    NetApi.userLogoff(getToken(), username, new ResultCallBack<BaseModel>() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, BaseModel model) {
                            super.onSuccess(statusCode, headers, model);
                            if (model != null) {
                                switch (model.code) {
                                    case Constants.API.API_OK:
                                        CommonKit.showOkShort(context, getString(R.string.hint_logoff_ok));
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
                removeCacheForSp();//删除缓存
                CommonKit.finishActivity(context);
                postSticky(new Event.UserLoginEvent());//登录状态发生改变
                break;

            case R.id.ll_searchVersion:
                checkVersion();
                break;
            case R.id.ll_changePassword:
                ModifiActivtiy.lauch(context);
                break;
            case R.id.ll_clearCache:
                clearCache();
                break;
            default:
                break;
        }
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
                            RLog.d(model.toString());
                            if (model.appVersion > CommonKit.getAppVersionCode(context)) {
                                RLog.d("发现新版本");
                                downloadAppApk(model.appFileName);
                            } else {
                                CommonKit.showOkShort(context, "已经是最新版本");
                            }
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
     * 下载Apk
     *
     * @param appFireName
     */
    public void downloadAppApk(final String appFireName) {
        if (!CommonKit.isNetworkAvailable(context)) {
            CommonKit.showErrorShort(context, "设备未连接网络");
            return;
        }
        final String SDCard = Environment.getExternalStorageDirectory() + "/androidex";
        Log.v("downloadFile", "File path: " + SDCard);
        NetApi.downloadAppApk(getToken(), SDCard, appFireName, new ResultCallBack() {
            @Override
            public void onStart() {
                super.onStart();
                RLog.d("开始下载新版本");
                CommonKit.showOkShort(context, "开始下载新版本");
            }

            @Override
            public void onSuccess(int statusCode, Headers headers, Object model) {
                super.onSuccess(statusCode, headers, model);
                RLog.d("下载完成");
                CommonKit.installNormal(context, SDCard + "/" + appFireName);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                RLog.d("bytesWritten=" + bytesWritten + "\ntotalSize=" + totalSize);
            }

            @Override
            public void onFailure(int statusCode, Request request, Exception e) {
                super.onFailure(statusCode, request, e);
                RLog.d("下载失败" + e.getMessage());
            }
        });
    }

    /**
     * 清除缓存
     */

    private void clearCache() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                UILKit.getLoader().clearDiskCache();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                CommonKit.showOkShort(context, "成功清除缓存");
                updateCache();
            }
        }.execute();
    }

    /**
     * 更新缓存
     */
    private void updateCache() {
        getSpaceSize(UILKit.getLoader().getDiskCache().getDirectory());
    }

    private void getSpaceSize(File file) {
        new AsyncTask<File, Void, Float>() {

            @Override
            protected Float doInBackground(File... params) {
                File file = params[0];
                if (file.exists() && file.isDirectory()) {
                    return CommonKit.getFolderSize(file.getAbsolutePath()) / 1024.0f;
                }
                return 0f;
            }

            @Override
            protected void onPostExecute(Float result) {
                if (result != 0) {
                    tv_cacheSize.setText(String.format("%.2f", result) + "MB");
                } else {
                    tv_cacheSize.setText("无缓存");
                }
                super.onPostExecute(result);
            }
        }.execute(file);
    }

    /**
     * 删除缓存本地的账号和密码
     */
    private void removeCacheForSp() {
        SharedPreTool.getInstance(context).setBoolData(LOGIN_STATUS, false);//设置登录标识为false
        //SharedPreTool.getInstance(context).remove(SharedPreTool.TOKEN);
        SharedPreTool.getInstance(context).remove(SharedPreTool.PHONE);
        SharedPreTool.getInstance(context).remove(SharedPreTool.PASSWORD);
        SharedPreTool.getInstance(context).remove(SharedPreTool.AUTOMATIC_LOGIN);
        SharedPreTool.getInstance(context).remove(SharedPreTool.IS_REGISTED);
    }

    @Override
    public void onClick(View v) {

    }

    public static void lauch(Activity activity) {
        CommonKit.startActivity(activity, SettingActivity.class, null, false);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_setting;
    }

}
