package com.qr.scan.Scanner;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

/**
 * Created on 09.09 - 2020
 *
 * @author wenbosheng
 * Copyright (c) 2020 android Amber
 */
public class QRScannerView implements PlatformView, MethodChannel.MethodCallHandler, PreView.Delegate {

    private final Context context;
    private final PreView mView;
    private final MethodChannel channel;
    private boolean startPreView = false;
    private boolean startScan = false;

    QRScannerView(BinaryMessenger messenger, int viewId, Context context, Map<String, Object> params) {
        this.context = context;
        channel = new MethodChannel(messenger, "com.qr.scanner.view_" + viewId);
        PreView.Builder builder = PreView.newBuilder();
        if (params.containsKey("borderColor")){
            long borderColor = (long) params.get("borderColor");
            builder.borderColor((int) borderColor);
        }
        if (params.containsKey("scanColor")){
            long scanColor = (long) params.get("scanColor");
            builder.scanColor((int) scanColor);
        }
        if (params.containsKey("cornerColor")){
            long cornerColor = (long) params.get("cornerColor");
            builder.cornerLineColor((int) cornerColor);
        }
        if (params.containsKey("scanBoxSize")){
            double size = (double) params.get("scanBoxSize");
            int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) size, Resources.getSystem().getDisplayMetrics());
            builder.scanBoxSize(value);
        }
        if (params.containsKey("scanBoxHeightWhenBarCode")){
            double size = (double)params.get("scanBoxHeightWhenBarCode");
            int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) size, Resources.getSystem().getDisplayMetrics());
            builder.scanBarCodeBoxHeight(value);
        }
        if (params.containsKey("defaultQRCodeMode")){
            Boolean square = (Boolean) params.get("defaultQRCodeMode");
            boolean squareValue = square==null?true:square;
            builder.square(squareValue);
        }
        if (params.containsKey("scanBoxVerticalBias")){
            double size = (double)params.get("scanBoxVerticalBias");
            builder.scanBoxVerticalBias((float) size);
        }

        this.mView = builder.build(context);
        channel.setMethodCallHandler(this);
        mView.setDelegate(this);
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public void onFlutterViewAttached(@NonNull View flutterView) {
        if (!hasCameraPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionHandler.requestPermission(context, Manifest.permission.CAMERA, new OnPermissionResult() {
                    @Override
                    public void onResult(boolean success) {
                        if (success) {
                            if (startScan) {
                                mView.startSpotAndShowRect();
                            } else if (startPreView) {
                                mView.startCamera();
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onFlutterViewDetached() {

    }

    @Override
    public void dispose() {
        startScan = false;
        startPreView = false;
        channel.setMethodCallHandler(null);
        mView.stopSpotAndHiddenRect();
        mView.stopCamera();
    }

    @Override
    public void onInputConnectionLocked() {

    }

    @Override
    public void onInputConnectionUnlocked() {

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        String method = call.method;
        switch (method) {
            case "startPreView":
                startPreView = true;
                if (!hasCameraPermission()) {
                    result.success(false);
                    return;
                }
                mView.startCamera();
                result.success(true);
                break;
            case "stopPreView":
                startPreView = false;
                mView.stopCamera();
                result.success(true);
                break;
            case "startScan":
                startScan = true;
                if (!hasCameraPermission()) {
                    result.success(false);
                    return;
                }
                mView.startSpot();
                result.success(true);
                break;
            case "stopScan":
                startScan = false;
                mView.stopSpot();
                result.success(true);
                break;
            case "toggleFlashlight":
                Boolean on = call.argument("status");
                if (on == null) {
                    result.success(false);
                    break;
                }
                if (!hasCameraPermission()) {
                    result.success(false);
                    return;
                }
                if (on) {
                    mView.openFlashlight();
                    result.success(true);
                } else {
                    mView.closeFlashlight();
                    result.success(true);
                }
                break;
            case "toggleQRMode":
                Boolean qrMode = call.argument("status");
                if (qrMode == null) {
                    result.success(false);
                    break;
                }
                if (qrMode) {
                    mView.setSquare(true);
                    result.success(true);
                } else {
                    mView.setSquare(false);
                    result.success(true);
                }
                break;
            default:
                result.notImplemented();


        }
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", ScannerPlugin.status_success);
        map.put("text", result);
        channel.invokeMethod("onResult", map);
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Map<String, Object> map = new HashMap<>();
        boolean granted = hasCameraPermission();
        int status = granted ? ScannerPlugin.status_recognize_failed : ScannerPlugin.status_permission_denied;
        String message = granted ? "Scan QRCode Open CameraError" : "no camera permission";
        map.put("status", status);
        map.put("message", message);
        channel.invokeMethod("onResult", map);
    }

    private boolean hasCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PermissionHandler.hasPermission(context, Manifest.permission.CAMERA);
        }
        return true;
    }
}
