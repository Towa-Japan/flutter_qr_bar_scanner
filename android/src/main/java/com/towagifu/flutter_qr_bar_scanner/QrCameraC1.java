package com.towagifu.flutter_qr_bar_scanner;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Implements QrCamera using Deprecated Camera API
 * NOTE: uses fully qualified names for android.hardware.Camera
 * so that deprecation warnings can be avoided.
 */
@TargetApi(16)
class QrCameraC1 implements QrCamera {

    private static final String TAG = FlutterQrBarScannerPlugin.LOG_TAG_PREFIX + QrCameraC1.class.getSimpleName();
    private static final int IMAGEFORMAT = ImageFormat.NV21;
    private final SurfaceTexture texture;
    private final QrDetector detector;
    private android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
    private int targetWidth, targetHeight;
    private android.hardware.Camera camera = null;
    private Context context;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    QrCameraC1(int width, int height, SurfaceTexture texture, Context context, QrDetector detector) {
        this.texture = texture;
        targetHeight = height;
        targetWidth = width;
        this.detector = detector;
        this.context = context;
    }


    private int getFirebaseOrientation() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int deviceRotation = windowManager.getDefaultDisplay().getRotation();
        int rotationCompensation = (ORIENTATIONS.get(deviceRotation) + info.orientation + 270) % 360;

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        int result;
        switch (rotationCompensation) {
            case 0:
                result = 0;
                break;
            case 90:
                result = 90;
                break;
            case 180:
                result = 180;
                break;
            case 270:
                result = 270;
                break;
            default:
                result = Surface.ROTATION_0;
                Log.e(TAG, "Bad rotation value: " + rotationCompensation);
        }
        return result;
    }

    @Override
    public void start() throws QrReader.Exception {
        int numberOfCameras = android.hardware.Camera.getNumberOfCameras();
        info = new android.hardware.Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            android.hardware.Camera.getCameraInfo(i, info);
            if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                camera = android.hardware.Camera.open(i);
                break;
            }
        }

        if (camera == null) {
            throw new QrReader.Exception(QrReader.Exception.Reason.noBackCamera);
        }

        final android.hardware.Camera.Parameters parameters = camera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.i(TAG, "Initializing with autofocus on.");
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);
        } else {
            Log.i(TAG, "Initializing with autofocus off as not supported.");
        }

        List<android.hardware.Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        android.hardware.Camera.Size size = getAppropriateSize(supportedSizes);

        parameters.setPreviewSize(size.width, size.height);
        texture.setDefaultBufferSize(size.width, size.height);

        parameters.setPreviewFormat(IMAGEFORMAT);

        try {
            camera.setPreviewCallback(new android.hardware.Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
                    android.hardware.Camera.Size previewSize = camera.getParameters().getPreviewSize();

                    if (data != null) {

                        Log.d(TAG, "frame size: " + previewSize.toString());
                        QrDetector.Frame frame = new Frame(data,
                            previewSize.width, previewSize.height, getFirebaseOrientation(), IMAGEFORMAT);
                        detector.detect(frame);
                    } else {
                        //TODO: something better here?
                        System.out.println("It's NULL!");
                    }
                }
            });
            camera.setPreviewTexture(texture);
            camera.startPreview();
            camera.autoFocus(new android.hardware.Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, android.hardware.Camera camera) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class Frame implements QrDetector.Frame {
        private byte[] data;
        private final int imageFormat;
        private final int width;
        private final int height;
        private final int rotationDegrees;

        Frame(byte[] data, int width, int height, int rotationDegrees, int imageFormat) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.rotationDegrees = rotationDegrees;
            this.imageFormat = imageFormat;
        }

        @Override
        public InputImage toImage() {
            //fromByteArray(byte[] byteArray, int width, int height, int rotationDegrees, int format)
            return InputImage.fromByteArray(data, width, height, rotationDegrees, imageFormat);
        }

        @Override
        public void close() {
            data = null;
        }
    }

    @Override
    public int getWidth() {
        return camera.getParameters().getPreviewSize().height;
    }

    @Override
    public int getHeight() {
        return camera.getParameters().getPreviewSize().width;
    }

    @Override
    public int getOrientation() {
        return (info.orientation + 270) % 360;
    }

    @Override
    public void stop() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    //Size here is Camera.Size, not android.util.Size as in the QrCameraC2 version of this method
    private android.hardware.Camera.Size getAppropriateSize(List<android.hardware.Camera.Size> sizes) {
        // assume sizes is never 0
        if (sizes.size() == 1) {
            Log.d(TAG, "selected camera size: " + sizes.get(0).toString());
            return sizes.get(0);
        }

        final int idealWidth = (info.orientation % 180 == 0) ? targetWidth : targetHeight;
        final int idealHeight = (info.orientation % 180 == 0) ? targetHeight : targetWidth;
        Log.d(TAG, "ideal camera size: " + idealWidth + " x " + idealHeight);

        android.hardware.Camera.Size ret = sizes.stream()
            .filter(s -> s.height >= idealHeight && s.width >= idealWidth)
            .min(Comparator.comparing(s -> s.height * s.width))
            .orElse(sizes.get(0));
        Log.d(TAG, "selected camera size: " + ret.toString());
        return ret;
    }
}
