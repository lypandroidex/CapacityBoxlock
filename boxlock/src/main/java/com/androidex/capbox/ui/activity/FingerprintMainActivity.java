package com.androidex.capbox.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidex.capbox.R;
import com.androidex.capbox.base.BaseActivity;
import com.androidex.capbox.core.FingerprintCore;
import com.androidex.capbox.core.FingerprintUtil;
import com.androidex.capbox.core.KeyguardLockScreenManager;
import com.androidex.capbox.utils.CommonKit;

public class FingerprintMainActivity extends BaseActivity {
    private FingerprintCore mFingerprintCore;
    private KeyguardLockScreenManager mKeyguardLockScreenManager;
    private ImageView mFingerGuideImg;
    private TextView mFingerGuideTxt;

    @Override
    public int getLayoutId() {
        return R.layout.activity_fingerprint_main;
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            //结束你的activity
            CommonKit.finishActivity(this);
            return;
        }
        initViews();
        initFingerprintCore();
        startFingerprintRecognition();
    }

    @Override
    public void setListener() {

    }

    /**
     * 初始化指纹
     */
    private void initFingerprintCore() {
        mFingerprintCore = new FingerprintCore(this);
        mFingerprintCore.setFingerprintManager(mResultListener);
        mKeyguardLockScreenManager = new KeyguardLockScreenManager(this);
    }

    private void initViews() {
        mFingerGuideImg = (ImageView) findViewById(R.id.fingerprint_guide);
        mFingerGuideTxt = (TextView) findViewById(R.id.fingerprint_guide_tip);
    }

    /**
     * 取消指纹识别
     */
    private void cancelFingerprintRecognition() {
        if (mFingerprintCore.isAuthenticating()) {
            mFingerprintCore.cancelAuthenticate();
            resetGuideViewState();
        }
    }

    /**
     * 开始锁屏密码
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startFingerprintRecognitionUnlockScreen() {
        if (mKeyguardLockScreenManager == null) {
            return;
        }
        //判断是否设置锁屏密码或指纹，没有设置直接进入欢迎界面
        if (!mKeyguardLockScreenManager.isOpenLockScreenPwd()) {
            CommonKit.showMsgShort(context, getString(R.string.fingerprint_not_set_unlock_screen_pws));
            //FingerprintUtil.openFingerPrintSettingPage(this);//开启系统设置界面
            WelcomeActivity.lauch(context);
            return;
        }
        mKeyguardLockScreenManager.showAuthenticationScreen(this);
    }

    /**
     * 开始指纹识别
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startFingerprintRecognition() {
        if (mFingerprintCore.isSupport()) {
            if (!mFingerprintCore.isHasEnrolledFingerprints()) {//未录入
                CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_not_enrolled));
                FingerprintUtil.openFingerPrintSettingPage(this);
                return;
            }
            CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_tip));
            mFingerGuideTxt.setText(R.string.fingerprint_recognition_tip);
            mFingerGuideImg.setImageResource(R.mipmap.fingerprint_guide);
            if (mFingerprintCore.isAuthenticating()) {
                CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_authenticating));
            } else {
                mFingerprintCore.startAuthenticate();
            }
        } else {
            CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_not_support));
            mFingerGuideTxt.setText(R.string.fingerprint_recognition_tip);
            startFingerprintRecognitionUnlockScreen();
        }
    }

    private void resetGuideViewState() {
        mFingerGuideTxt.setText(R.string.fingerprint_recognition_start);
        mFingerGuideImg.setImageResource(R.mipmap.fingerprint_normal);
    }

    int error = 0;

    private FingerprintCore.IFingerprintResultListener mResultListener = new FingerprintCore.IFingerprintResultListener() {
        @Override
        public void onAuthenticateSuccess() {
            CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_success));
            WelcomeActivity.lauch(context);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onAuthenticateFailed(int helpId) {
            CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_failed));
            mFingerGuideTxt.setText(R.string.fingerprint_recognition_failed);
            error++;
            if (error == 3) {
                startFingerprintRecognitionUnlockScreen();
                error = 0;  
            }
        }

        @Override
        public void onAuthenticateError(int errMsgId) {
            resetGuideViewState();
            CommonKit.showMsgShort(context, getString(R.string.fingerprint_recognition_error));
        }

        @Override
        public void onStartAuthenticateResult(boolean isSuccess) {

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == KeyguardLockScreenManager.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                WelcomeActivity.lauch(context);
                CommonKit.showMsgShort(context, getString(R.string.sys_pwd_recognition_success));
            } else {
                CommonKit.showMsgShort(context, getString(R.string.sys_pwd_recognition_failed));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFingerprintCore != null) {
            mFingerprintCore.onDestroy();
            mFingerprintCore = null;
        }
        if (mKeyguardLockScreenManager != null) {
            mKeyguardLockScreenManager.onDestroy();
            mKeyguardLockScreenManager = null;
        }
        mResultListener = null;
    }

    @Override
    public void onClick(View view) {

    }
}
