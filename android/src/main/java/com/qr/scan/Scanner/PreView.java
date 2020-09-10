package com.qr.scan.Scanner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import java.util.Map;

/**
 * Created on 09.09 - 2020
 *
 * @author wenbosheng
 * Copyright (c) 2020 android Amber
 */
public class PreView extends FrameLayout implements Camera.PreviewCallback {

    private static final int NO_CAMERA_ID = -1;
    protected Camera mCamera;
    protected final CameraPreview mCameraPreview;
    protected final PreViewBox mScanBoxView;
    protected Delegate mDelegate;
    protected boolean mSpotAble = false;
    protected ProcessDataTask mProcessDataTask;
    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private PointF[] mLocationPoints;
    private Paint mPaint;
    protected BarcodeType mBarcodeType = BarcodeType.HIGH_FREQUENCY;
    private ValueAnimator mAutoZoomAnimator;
    private long mLastAutoZoomTime = 0;
    private final boolean isAutoZoom;
    private MultiFormatReader mMultiFormatReader;
    private Map<DecodeHintType, Object> mHintMap;

    private PreView(@NonNull Context context, Builder builder) {
        super(context);
        int cornerLineColor = builder.cornerLineColor;
        isAutoZoom = builder.isAutoZoom;
        final int color = builder.scanColor;
        Bitmap bitmap = QRCodeUtil.makeTintBitmap(builder.scanDrawable, color);
        mScanBoxView = new PreViewBox(this, builder.cornerWidth, builder.cornerLength, builder.cornerLineColor
                , builder.scanBoxSize, builder.scanBarCodeBoxHeight, bitmap, builder.borderSize, builder.borderColor, builder.scanDuration, builder.scanBoxVerticalBias
                , builder.gridMode, builder.scanOnlyBoxAre);
        mCameraPreview = new CameraPreview(context);
        mCameraPreview.setDelegate(new CameraPreview.Delegate() {
            @Override
            public void onStartPreview() {
                setOneShotPreviewCallback();
            }
        });
        addView(mCameraPreview);
        mScanBoxView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mScanBoxView);
        mPaint = new Paint();
        mPaint.setColor(cornerLineColor);
        mPaint.setStyle(Paint.Style.FILL);
        setupReader();
        if (!builder.defaultSquare) {
            mScanBoxView.setSquare(false);
        }
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mLocationPoints == null) {
            return;
        }
        for (PointF pointF : mLocationPoints) {
            canvas.drawCircle(pointF.x, pointF.y, 10, mPaint);
        }
        mLocationPoints = null;
        postInvalidateDelayed(2000);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mSpotAble || (mProcessDataTask != null && (mProcessDataTask.getStatus() == AsyncTask.Status.PENDING
                || mProcessDataTask.getStatus() == AsyncTask.Status.RUNNING))) {
            return;
        }
        mProcessDataTask = new ProcessDataTask(camera, data, this, QRCodeUtil.isPortrait(getContext())).perform();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAutoZoomAnimator != null) {
            mAutoZoomAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }

    private void setOneShotPreviewCallback() {
        if (mSpotAble && mCameraPreview.isPreviewing()) {
            try {
                mCamera.setOneShotPreviewCallback(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ScanResult processBitmapData(Bitmap bitmap) {
        return new ScanResult(QRCodeDecoder.syncDecodeQRCode(bitmap));
    }

    ScanResult processData(byte[] data, int width, int height, boolean isRetry) {
        Result rawResult = null;
        Rect scanBoxAreaRect = null;

        try {
            PlanarYUVLuminanceSource source;
            scanBoxAreaRect = mScanBoxView.getScanBoxAreaRect(height);
            if (scanBoxAreaRect != null) {
                source = new PlanarYUVLuminanceSource(data, width, height, scanBoxAreaRect.left, scanBoxAreaRect.top, scanBoxAreaRect.width(),
                        scanBoxAreaRect.height(), false);
            } else {
                source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
            }

            rawResult = mMultiFormatReader.decodeWithState(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
            if (rawResult == null) {
                rawResult = mMultiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
                if (rawResult != null) {
                    QRCodeUtil.d("GlobalHistogramBinarizer 没识别到，HybridBinarizer 能识别到");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mMultiFormatReader.reset();
        }

        if (rawResult == null) {
            return null;
        }

        String result = rawResult.getText();
        if (TextUtils.isEmpty(result)) {
            return null;
        }

        BarcodeFormat barcodeFormat = rawResult.getBarcodeFormat();
        QRCodeUtil.d("格式为：" + barcodeFormat.name());

        // 处理自动缩放和定位点
        boolean isNeedAutoZoom = isNeedAutoZoom(barcodeFormat);
        if (isNeedAutoZoom) {
            ResultPoint[] resultPoints = rawResult.getResultPoints();
            final PointF[] pointArr = new PointF[resultPoints.length];
            int pointIndex = 0;
            for (ResultPoint resultPoint : resultPoints) {
                pointArr[pointIndex] = new PointF(resultPoint.getX(), resultPoint.getY());
                pointIndex++;
            }

            if (transformToViewCoordinates(pointArr, scanBoxAreaRect, isNeedAutoZoom, result)) {
                return null;
            }
        }
        return new ScanResult(result);
    }

    void onPostParseBitmapOrPicture(ScanResult scanResult) {
        if (mDelegate != null) {
            String result = scanResult == null ? null : scanResult.result;
            mDelegate.onScanQRCodeSuccess(result);
        }
    }

    public void onScanBoxRectChanged(Rect rect) {
        mCameraPreview.onScanBoxRectChanged(rect);
    }

    public void setType(BarcodeType barcodeType, Map<DecodeHintType, Object> hintMap) {
        mBarcodeType = barcodeType;
        mHintMap = hintMap;

        if (mBarcodeType == BarcodeType.CUSTOM && (mHintMap == null || mHintMap.isEmpty())) {
            throw new RuntimeException("barcodeType 为 BarcodeType.CUSTOM 时 hintMap 不能为空");
        }
        setupReader();
    }

    private void setupReader() {
        mMultiFormatReader = new MultiFormatReader();
        if (mBarcodeType == BarcodeType.ONE_DIMENSION) {
            mMultiFormatReader.setHints(QRCodeDecoder.ONE_DIMENSION_HINT_MAP);
        } else if (mBarcodeType == BarcodeType.TWO_DIMENSION) {
            mMultiFormatReader.setHints(QRCodeDecoder.TWO_DIMENSION_HINT_MAP);
        } else if (mBarcodeType == BarcodeType.ONLY_QR_CODE) {
            mMultiFormatReader.setHints(QRCodeDecoder.QR_CODE_HINT_MAP);
        } else if (mBarcodeType == BarcodeType.ONLY_CODE_128) {
            mMultiFormatReader.setHints(QRCodeDecoder.CODE_128_HINT_MAP);
        } else if (mBarcodeType == BarcodeType.ONLY_EAN_13) {
            mMultiFormatReader.setHints(QRCodeDecoder.EAN_13_HINT_MAP);
        } else if (mBarcodeType == BarcodeType.HIGH_FREQUENCY) {
            mMultiFormatReader.setHints(QRCodeDecoder.HIGH_FREQUENCY_HINT_MAP);
        } else if (mBarcodeType == BarcodeType.CUSTOM) {
            mMultiFormatReader.setHints(mHintMap);
        } else {
            mMultiFormatReader.setHints(QRCodeDecoder.ALL_HINT_MAP);
        }
    }

    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }


    public void startCamera() {
        startCamera(mCameraId);
    }

    public void showScanRect() {
        mScanBoxView.setVisibility(View.VISIBLE);
    }


    public void hiddenScanRect() {
        mScanBoxView.setVisibility(View.GONE);
    }

    public void startCamera(int cameraFacing) {
        if (mCamera != null || Camera.getNumberOfCameras() == 0) {
            return;
        }
        int ultimateCameraId = findCameraIdByFacing(cameraFacing);
        if (ultimateCameraId != NO_CAMERA_ID) {
            startCameraById(ultimateCameraId);
            return;
        }

        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            ultimateCameraId = findCameraIdByFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            ultimateCameraId = findCameraIdByFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        if (ultimateCameraId != NO_CAMERA_ID) {
            startCameraById(ultimateCameraId);
        }
    }

    private int findCameraIdByFacing(int cameraFacing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            try {
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (cameraInfo.facing == cameraFacing) {
                    return cameraId;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return NO_CAMERA_ID;
    }

    private void startCameraById(int cameraId) {
        try {
            mCameraId = cameraId;
            mCamera = Camera.open(cameraId);
            mCameraPreview.setCamera(mCamera);
        } catch (Exception e) {
            e.printStackTrace();
            if (mDelegate != null) {
                mDelegate.onScanQRCodeOpenCameraError();
            }
        }
    }


    public void stopCamera() {
        try {
            stopSpotAndHiddenRect();
            if (mCamera != null) {
                mCameraPreview.stopCameraPreview();
                mCameraPreview.setCamera(null);
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void startSpot() {
        mSpotAble = true;
        startCamera();
        setOneShotPreviewCallback();
    }


    public void stopSpot() {
        mSpotAble = false;

        if (mProcessDataTask != null) {
            mProcessDataTask.cancelTask();
            mProcessDataTask = null;
        }

        if (mCamera != null) {
            try {
                mCamera.setOneShotPreviewCallback(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSpotAndHiddenRect() {
        stopSpot();
        hiddenScanRect();
    }

    public void startSpotAndShowRect() {
        startSpot();
        showScanRect();
    }

    public void openFlashlight() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mCameraPreview.openFlashlight();
            }
        }, mCameraPreview.isPreviewing() ? 0 : 500);
    }

    public void closeFlashlight() {
        mCameraPreview.closeFlashlight();
    }


    public void onDestroy() {
        stopCamera();
        mDelegate = null;
    }

    public void setSquare(boolean square) {
        mScanBoxView.setSquare(square);
    }

    public boolean isSquare() {
        return mScanBoxView.isSquare();
    }

    private boolean isNeedAutoZoom(BarcodeFormat barcodeFormat) {
        return isAutoZoom && barcodeFormat == BarcodeFormat.QR_CODE;
    }

    protected boolean transformToViewCoordinates(final PointF[] pointArr, final Rect scanBoxAreaRect, final boolean isNeedAutoZoom, final String result) {
        if (pointArr == null || pointArr.length == 0) {
            return false;
        }

        try {
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            boolean isMirrorPreview = mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
            int statusBarHeight = QRCodeUtil.getStatusBarHeight(getContext());

            PointF[] transformedPoints = new PointF[pointArr.length];
            int index = 0;
            for (PointF qrPoint : pointArr) {
                transformedPoints[index] = transform(qrPoint.x, qrPoint.y, size.width, size.height, isMirrorPreview, statusBarHeight, scanBoxAreaRect);
                index++;
            }
            mLocationPoints = transformedPoints;
            postInvalidate();

            if (isNeedAutoZoom) {
                return handleAutoZoom(transformedPoints, result);
            }
            return false;
        } catch (Exception e) {
            mLocationPoints = null;
            e.printStackTrace();
            return false;
        }
    }

    private PointF transform(float originX, float originY, float cameraPreviewWidth, float cameraPreviewHeight, boolean isMirrorPreview, int statusBarHeight,
                             final Rect scanBoxAreaRect) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        PointF result;
        float scaleX;
        float scaleY;

        if (QRCodeUtil.isPortrait(getContext())) {
            scaleX = viewWidth / cameraPreviewHeight;
            scaleY = viewHeight / cameraPreviewWidth;
            result = new PointF((cameraPreviewHeight - originX) * scaleX, (cameraPreviewWidth - originY) * scaleY);
            result.y = viewHeight - result.y;
            result.x = viewWidth - result.x;

            if (scanBoxAreaRect == null) {
                result.y += statusBarHeight;
            }
        } else {
            scaleX = viewWidth / cameraPreviewWidth;
            scaleY = viewHeight / cameraPreviewHeight;
            result = new PointF(originX * scaleX, originY * scaleY);
            if (isMirrorPreview) {
                result.x = viewWidth - result.x;
            }
        }

        if (scanBoxAreaRect != null) {
            result.y += scanBoxAreaRect.top;
            result.x += scanBoxAreaRect.left;
        }

        return result;
    }

    private boolean handleAutoZoom(PointF[] locationPoints, final String result) {
        if (mCamera == null || mScanBoxView == null) {
            return false;
        }
        if (locationPoints == null || locationPoints.length < 1) {
            return false;
        }
        if (mAutoZoomAnimator != null && mAutoZoomAnimator.isRunning()) {
            return true;
        }
        if (System.currentTimeMillis() - mLastAutoZoomTime < 1200) {
            return true;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if (!parameters.isZoomSupported()) {
            return false;
        }

        float point1X = locationPoints[0].x;
        float point1Y = locationPoints[0].y;
        float point2X = locationPoints[1].x;
        float point2Y = locationPoints[1].y;
        float xLen = Math.abs(point1X - point2X);
        float yLen = Math.abs(point1Y - point2Y);
        int len = (int) Math.sqrt(xLen * xLen + yLen * yLen);

        int scanBoxWidth = mScanBoxView.getRectWidth();
        if (len > scanBoxWidth / 4) {
            return false;
        }
        // 二维码在扫描框中的宽度小于扫描框的 1/4，放大镜头
        final int maxZoom = parameters.getMaxZoom();
        final int zoomStep = maxZoom / 4;
        final int zoom = parameters.getZoom();
        post(new Runnable() {
            @Override
            public void run() {
                startAutoZoom(zoom, Math.min(zoom + zoomStep, maxZoom), result);
            }
        });
        return true;
    }

    private void startAutoZoom(int oldZoom, int newZoom, final String result) {
        mAutoZoomAnimator = ValueAnimator.ofInt(oldZoom, newZoom);
        mAutoZoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mCameraPreview == null || !mCameraPreview.isPreviewing()) {
                    return;
                }
                int zoom = (int) animation.getAnimatedValue();
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setZoom(zoom);
                mCamera.setParameters(parameters);
            }
        });
        mAutoZoomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onPostParseData(new ScanResult(result));
            }
        });
        mAutoZoomAnimator.setDuration(600);
        mAutoZoomAnimator.setRepeatCount(0);
        mAutoZoomAnimator.start();
        mLastAutoZoomTime = System.currentTimeMillis();
    }

    void onPostParseData(ScanResult scanResult) {
        if (!mSpotAble) {
            return;
        }
        String result = scanResult == null ? null : scanResult.result;
        if (TextUtils.isEmpty(result)) {
            try {
                if (mCamera != null) {
                    mCamera.setOneShotPreviewCallback(this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mSpotAble = false;
            try {
                if (mDelegate != null) {
                    mDelegate.onScanQRCodeSuccess(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface Delegate {

        void onScanQRCodeSuccess(String result);


        void onScanQRCodeOpenCameraError();
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private int cornerWidth = -1;
        private int cornerLength = -1;
        private int cornerLineColor = Color.CYAN;
        private int scanBoxSize = -1;
        private int scanBarCodeBoxHeight = -1;
        private Bitmap scanDrawable = null;
        private int scanColor = Color.CYAN;
        private int borderSize = -1;
        private int borderColor = Color.WHITE;
        private int scanDuration = 1000;
        private float scanBoxVerticalBias = 0.5f;
        private boolean scanOnlyBoxAre = true;
        private boolean isAutoZoom = false;
        private boolean gridMode = true;
        private boolean defaultSquare = true;

        private Builder() {
        }

        public Builder cornerWidth(int val) {
            cornerWidth = val;
            return this;
        }

        public Builder cornerLength(int val) {
            cornerLength = val;
            return this;
        }

        public Builder cornerLineColor(int val) {
            cornerLineColor = val;
            return this;
        }

        public Builder square(boolean square) {
            this.defaultSquare = square;
            return this;
        }

        public Builder scanBoxSize(int val) {
            scanBoxSize = val;
            return this;
        }

        public Builder scanBarCodeBoxHeight(int val) {
            scanBarCodeBoxHeight = val;
            return this;
        }

        public Builder scanIndicator(Bitmap bitmap, boolean gridMode) {
            scanDrawable = bitmap;
            this.gridMode = gridMode;
            return this;
        }

        public Builder scanColor(int val) {
            scanColor = val;
            return this;
        }

        public Builder borderSize(int val) {
            borderSize = val;
            return this;
        }

        public Builder borderColor(int val) {
            borderColor = val;
            return this;
        }

        public Builder scanDuration(int val) {
            scanDuration = val;
            return this;
        }

        public Builder scanBoxVerticalBias(float val) {
            scanBoxVerticalBias = val;
            return this;
        }

        public Builder scanOnlyBoxAre(boolean val) {
            scanOnlyBoxAre = val;
            return this;
        }

        public Builder isAutoZoom(boolean val) {
            isAutoZoom = val;
            return this;
        }

        public PreView build(Context context) {
            DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
            if (cornerWidth < 0) {
                cornerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, displayMetrics);
            }
            if (cornerLength < 0) {
                cornerLength = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics);
            }
            if (scanBoxSize < 0) {
                scanBoxSize = displayMetrics.widthPixels / 2;
            }
            if (scanBarCodeBoxHeight < 0) {
                scanBarCodeBoxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140, displayMetrics);
            }
            if (scanDrawable == null) {
                if (gridMode) {
                    scanDrawable = BitmapFactory.decodeResource(context.getResources(), R.mipmap.qrcode_default_grid_scan_line);
                } else {
                    scanDrawable = BitmapFactory.decodeResource(context.getResources(), R.mipmap.qrcode_default_scan_line);
                }
            }
            if (borderSize < 0) {
                borderSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics);
            }
            return new PreView(context, this);
        }
    }
}
