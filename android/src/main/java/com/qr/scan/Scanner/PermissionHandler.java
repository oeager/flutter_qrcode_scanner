package com.qr.scan.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created on 09.09 - 2020
 *
 * @author wenbosheng
 * Copyright (c) 2020 android Amber
 */
public class PermissionHandler extends Activity {

    private final static int CODE_REQUEST = 1;

    private volatile static OnPermissionResult onResult;

    public static void requestPermission(Context context, String permission, OnPermissionResult result) {
        if (onResult != null) {
            result.onResult(false);
            return;
        }
        Intent i = new Intent(context, PermissionHandler.class);
        i.putExtra("permission", permission);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        onResult = result;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String permissions = intent == null ? null : intent.getStringExtra("permission");
        if (TextUtils.isEmpty(permissions)) {
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        if (hasPermission(this, permissions)) {
            if (onResult != null) {
                onResult.onResult(true);
            }
            onResult = null;
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{permissions}, CODE_REQUEST);
        }
    }

    @Override
    protected void onDestroy() {
        onResult = null;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (onResult != null) {
            onResult.onResult(grantResults[0] == PackageManager.PERMISSION_GRANTED);
            onResult = null;
        }
        finish();
        overridePendingTransition(0,0);
    }

    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
