package com.towagifu.flutter_qr_bar_scanner;

import java.util.NoSuchElementException;

public enum CameraOrientation {
    AWAY_FROM_USER,
    TOWARDS_USER;

    public static CameraOrientation parse(String value) throws NoSuchElementException {
        switch (value) {
            case "awayFromUser":
                return AWAY_FROM_USER;
            case "towardsUser":
                return TOWARDS_USER;
            default:
                throw new NoSuchElementException(value);
        }
    }
}
