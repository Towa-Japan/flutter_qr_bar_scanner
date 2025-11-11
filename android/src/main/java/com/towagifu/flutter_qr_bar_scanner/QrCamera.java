package com.towagifu.flutter_qr_bar_scanner;

interface QrCamera {
    void start(CameraOrientation orientation) throws QrReader.Exception;
    void setTorchState(boolean isOn);
    void stop();
    int getOrientation();
    int getWidth();
    int getHeight();
}
