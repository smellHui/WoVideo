package com.lt.hm.wovideo.base;

import android.support.v4.app.FragmentActivity;

import com.lt.hm.wovideo.http.HttpUtilBack;
import com.lt.hm.wovideo.utils.TLog;

/**
 * Created by xuchunhui on 16/8/12.
 */
public class BaseRequestActivity extends FragmentActivity implements HttpUtilBack {
    @Override
    public void onBefore(String dialog) {

    }

    @Override
    public <T> void onSuccess(T value, int flag) {

    }

    @Override
    public void onFail(String error,int flag) {
        TLog.error(error);
    }

    @Override
    public void onAfter(int flag) {

    }
}
