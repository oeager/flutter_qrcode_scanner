package com.qr.scan.Scanner;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * ScannerPlugin
 */
public class ScannerPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    static final int status_recognize_failed = 0;
    static final int status_success = 1;
    static final int status_file_not_exist = 2;
    static final int status_platform_exception = 3;
    static final int status_permission_denied = 4;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "com.qr.scanner");
        channel.setMethodCallHandler(this);
        QRScannerViewFactory scannerViewFactory = new QRScannerViewFactory(flutterPluginBinding.getBinaryMessenger());
        flutterPluginBinding.getPlatformViewRegistry().registerViewFactory("com.qr.scanner.view", scannerViewFactory);
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.qr.scanner");
        ScannerPlugin handler = new ScannerPlugin();
        QRScannerViewFactory factory = new QRScannerViewFactory(registrar.messenger());
        registrar.platformViewRegistry().registerViewFactory("com.qr.scanner.view", factory);
        channel.setMethodCallHandler(handler);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("fromImage")) {
            String filePath = call.argument("file");
            if (TextUtils.isEmpty(filePath)) {
                result.error(String.valueOf(status_file_not_exist), "file path can not be null", null);
                return;
            }
            String content = QRCodeDecoder.syncDecodeQRCode(filePath);
            final Map<String, Object> map = new HashMap<>();
            map.put("status", status_success);
            map.put("result", content);
            result.success(map);
        } else {
            result.notImplemented();
        }
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}
