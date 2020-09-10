package com.qr.scan.Scanner;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Created on 09.09 - 2020
 *
 * @author wenbosheng
 * Copyright (c) 2020 android Amber
 */
@SuppressLint("ViewConstructor")
public class PreViewBox extends View {
    private final int cornerWidth;
    private final int cornerLength;
    private final int cornerLineColor;
    private final int scanBoxSize;
    private final int scanBarCodeBoxHeight;
    private final Bitmap scanBitmap;
    private final int borderSize;
    private final int borderColor;
    private final int scanDuration;
    private final float scanBoxVerticalBias;
    private final boolean gridMode;
    private final float halfCornerSize;
    private final Paint mPaint;
    private final Rect mScanBox = new Rect();
    private final RectF dstScanRectF = new RectF();
    private final Rect srcScanRectF = new Rect();
    private final PreView mPreView;
    private final boolean isOnlyArea;
    private int refreshInterval;
    private boolean square = true;
    private int mMoveStepDistance;
    private float mScanLineTop;
    private float mScanLineLeft;
    private boolean attach = false;

    public PreViewBox(PreView preView, int cornerWidth, int cornerLength,
                      int cornerLineColor, int scanBoxSize,
                      int scanBarCodeBoxHeight, Bitmap scanBitmap,
                      int borderSize, int borderColor, int scanDuration,
                      float scanBoxVerticalBias, boolean gridMode,boolean isOnlyArea) {
        super(preView.getContext());
        this.mPreView = preView;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        this.isOnlyArea = isOnlyArea;
        mMoveStepDistance = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, Resources.getSystem().getDisplayMetrics());
        this.cornerWidth = cornerWidth;
        this.cornerLength = cornerLength;
        this.cornerLineColor = cornerLineColor;
        this.scanBoxSize = scanBoxSize;
        this.scanBarCodeBoxHeight = scanBarCodeBoxHeight;
        this.scanBitmap = scanBitmap;
        this.borderSize = borderSize;
        this.borderColor = borderColor;
        this.scanDuration = scanDuration;
        this.scanBoxVerticalBias = scanBoxVerticalBias;
        this.gridMode = gridMode;
        this.halfCornerSize = cornerWidth / 2f;
    }

    public void setSquare(boolean square) {
        if (this.square != square) {
            this.square = square;
            reLocateScanBox();
        }
    }
    public boolean isSquare(){
        return this.square;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //draw border
        if (borderSize > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(borderColor);
            mPaint.setStrokeWidth(borderSize);
            canvas.drawRect(mScanBox, mPaint);
        }
        //draw corner
        if (halfCornerSize > 0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(cornerLineColor);
            mPaint.setStrokeWidth(cornerWidth);
            canvas.drawLine(mScanBox.left - halfCornerSize, mScanBox.top, mScanBox.left - halfCornerSize + cornerLength, mScanBox.top,
                    mPaint);
            canvas.drawLine(mScanBox.left, mScanBox.top - halfCornerSize, mScanBox.left, mScanBox.top - halfCornerSize + cornerLength,
                    mPaint);
            canvas.drawLine(mScanBox.right + halfCornerSize, mScanBox.top, mScanBox.right + halfCornerSize - cornerLength, mScanBox.top,
                    mPaint);
            canvas.drawLine(mScanBox.right, mScanBox.top - halfCornerSize, mScanBox.right, mScanBox.top - halfCornerSize + cornerLength,
                    mPaint);

            canvas.drawLine(mScanBox.left - halfCornerSize, mScanBox.bottom, mScanBox.left - halfCornerSize + cornerLength,
                    mScanBox.bottom, mPaint);
            canvas.drawLine(mScanBox.left, mScanBox.bottom + halfCornerSize, mScanBox.left,
                    mScanBox.bottom + halfCornerSize - cornerLength, mPaint);
            canvas.drawLine(mScanBox.right + halfCornerSize, mScanBox.bottom, mScanBox.right + halfCornerSize - cornerLength,
                    mScanBox.bottom, mPaint);
            canvas.drawLine(mScanBox.right, mScanBox.bottom + halfCornerSize, mScanBox.right,
                    mScanBox.bottom + halfCornerSize - cornerLength, mPaint);
        }
        //draw scan
        if (square) {
            if (gridMode) {
                dstScanRectF.set(mScanBox.left + halfCornerSize, mScanBox.top + halfCornerSize + 0.5f,
                        mScanBox.right - halfCornerSize, mScanLineTop);

                srcScanRectF.set(0, (int) (scanBitmap.getHeight() - dstScanRectF.height()), scanBitmap.getWidth(),
                        scanBitmap.getHeight());

                if (srcScanRectF.top < 0) {
                    srcScanRectF.top = 0;
                    dstScanRectF.top = dstScanRectF.bottom - srcScanRectF.height();
                }
                canvas.drawBitmap(scanBitmap, srcScanRectF, dstScanRectF, mPaint);
            } else {
                dstScanRectF.set(mScanBox.left + halfCornerSize, mScanLineTop,
                        mScanBox.right - halfCornerSize, mScanLineTop + scanBitmap.getHeight());
                canvas.drawBitmap(scanBitmap, null, dstScanRectF, mPaint);
            }
        } else {
            if (gridMode) {
                dstScanRectF.set(mScanBox.left + halfCornerSize + 0.5f, mScanBox.top + halfCornerSize,
                        mScanLineLeft, mScanBox.bottom - halfCornerSize);

                srcScanRectF.set((int) (scanBitmap.getWidth() - dstScanRectF.width()), 0, scanBitmap.getWidth(),
                        scanBitmap.getHeight());

                if (srcScanRectF.left < 0) {
                    srcScanRectF.left = 0;
                    dstScanRectF.left = dstScanRectF.right - srcScanRectF.width();
                }

                canvas.drawBitmap(scanBitmap, srcScanRectF, dstScanRectF, mPaint);
            } else {
                dstScanRectF.set(mScanLineLeft, mScanBox.top + halfCornerSize, mScanLineLeft + scanBitmap.getWidth(),
                        mScanBox.bottom - halfCornerSize);
                canvas.drawBitmap(scanBitmap, null, dstScanRectF, mPaint);
            }
        }
        //move line
        if (square) {
            if (gridMode) {
                mScanLineTop += mMoveStepDistance;
                if (mScanLineTop > mScanBox.bottom - halfCornerSize) {
                    mScanLineTop = mScanBox.top + halfCornerSize + 0.5f;
                }
            } else {
                mScanLineTop += mMoveStepDistance;
                int scanLineSize = scanBitmap.getHeight();
                if (mScanLineTop + scanLineSize > mScanBox.bottom - halfCornerSize || mScanLineTop < mScanBox.top + halfCornerSize) {
                    mMoveStepDistance = -mMoveStepDistance;
                }
            }
        } else {
            if (gridMode) {
                mScanLineLeft += mMoveStepDistance;
                if (mScanLineLeft > mScanBox.right - halfCornerSize) {
                    mScanLineLeft = mScanBox.left + halfCornerSize + 0.5f;
                }
            } else {
                mScanLineLeft += mMoveStepDistance;
                int scanLineSize = scanBitmap.getWidth();
                if (mScanLineLeft + scanLineSize > mScanBox.right - halfCornerSize || mScanLineLeft < mScanBox.left + halfCornerSize) {
                    mMoveStepDistance = -mMoveStepDistance;
                }
            }
        }
        if (attach) {
            postInvalidateDelayed(refreshInterval, mScanBox.left, mScanBox.top, mScanBox.right, mScanBox.bottom);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        reLocateScanBox();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attach = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        attach = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

    }

    private void reLocateScanBox() {
        int left = (getWidth() - scanBoxSize) / 2;
        int height = square ? scanBoxSize : scanBarCodeBoxHeight;
        int top = (int) ((getHeight() - height) * scanBoxVerticalBias);
        mScanBox.set(left, top, left+scanBoxSize, top+height);
        mScanLineLeft = mScanBox.left + halfCornerSize + 0.5f;
        mScanLineTop = mScanBox.top + halfCornerSize + 0.5f;
        refreshInterval = (int) ((1.0f * scanDuration * mMoveStepDistance) / mScanBox.height());
        mPreView.onScanBoxRectChanged(new Rect(mScanBox));
    }

    public Rect getScanBoxAreaRect(int previewHeight) {
        if (isOnlyArea && getVisibility() == View.VISIBLE) {
            Rect rect = new Rect(mScanBox);
            float ratio = 1.0f * previewHeight / getMeasuredHeight();
            float centerX = rect.exactCenterX() * ratio;
            float centerY = rect.exactCenterY() * ratio;
            float halfWidth = rect.width() / 2f;
            float halfHeight = rect.height() / 2f;
            float newHalfWidth = halfWidth * ratio;
            float newHalfHeight = halfHeight * ratio;
            rect.left = (int) (centerX - newHalfWidth);
            rect.right = (int) (centerX + newHalfWidth);
            rect.top = (int) (centerY - newHalfHeight);
            rect.bottom = (int) (centerY + newHalfHeight);
            return rect;
        } else {
            return null;
        }
    }

    public int getRectWidth() {
        return mScanBox.width();
    }
}
