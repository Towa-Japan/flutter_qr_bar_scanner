package com.github.contactlutforrahman.flutter_qr_bar_scanner;

import java.util.stream.Stream;

public interface QrReaderCallbacks {
    void qrRead(Stream<QrBarcode> data);
}
