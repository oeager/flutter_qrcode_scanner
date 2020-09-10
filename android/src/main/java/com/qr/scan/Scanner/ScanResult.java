package com.qr.scan.Scanner;

import android.graphics.PointF;

public class ScanResult {
    final String result;
    private final PointF[] resultPoints;

    ScanResult(String result) {
        this.result = result;
        this.resultPoints = null;
    }

    public ScanResult(String result, PointF[] resultPoints) {
        this.result = result;
        this.resultPoints = resultPoints;
    }
}
