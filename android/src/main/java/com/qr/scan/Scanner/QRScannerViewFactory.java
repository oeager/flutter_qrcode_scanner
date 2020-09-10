package com.qr.scan.Scanner;

import android.app.Activity;
import android.content.Context;

import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

/**
 * Created on 09.09 - 2020
 *
 * @author wenbosheng
 * Copyright (c) 2020 android Amber
 */
public class QRScannerViewFactory extends PlatformViewFactory {

    private final BinaryMessenger binaryMessenger;

    QRScannerViewFactory(BinaryMessenger binaryMessenger) {
        super(StandardMessageCodec.INSTANCE);
        this.binaryMessenger = binaryMessenger;
    }


    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        //noinspection unchecked
        Map<String, Object> params = (Map<String, Object>) args;
        return new QRScannerView(binaryMessenger,viewId,context, params);
    }
}
