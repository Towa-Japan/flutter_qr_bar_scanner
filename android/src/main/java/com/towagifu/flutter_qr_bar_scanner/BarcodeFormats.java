package com.towagifu.flutter_qr_bar_scanner;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public enum BarcodeFormats {

    ALL_FORMATS(Barcode.FORMAT_ALL_FORMATS),
    CODE_128(Barcode.FORMAT_CODE_128),
    CODE_39(Barcode.FORMAT_CODE_39),
    CODE_93(Barcode.FORMAT_CODE_93),
    CODABAR(Barcode.FORMAT_CODABAR),
    DATA_MATRIX(Barcode.FORMAT_DATA_MATRIX),
    EAN_13(Barcode.FORMAT_EAN_13),
    EAN_8(Barcode.FORMAT_EAN_8),
    ITF(Barcode.FORMAT_ITF),
    QR_CODE(Barcode.FORMAT_QR_CODE),
    UPC_A(Barcode.FORMAT_UPC_A),
    UPC_E(Barcode.FORMAT_UPC_E),
    PDF417(Barcode.FORMAT_PDF417),
    AZTEC(Barcode.FORMAT_AZTEC);

    BarcodeFormats(int intValue) {
        this.intValue = intValue;
    }

    public final int intValue;

    private static Map<String, Integer> formatsMap;

    static {
        BarcodeFormats[] values = BarcodeFormats.values();
        formatsMap = new HashMap<>(values.length * 4 / 3);
        for (BarcodeFormats value : values) {
            formatsMap.put(value.name(), value.intValue);
        }
    }

    /**
     * Return the integer value resuling from OR-ing all of the values
     * of the supplied strings.
     * <p>
     * Note that if ALL_FORMATS is defined as well as other values, ALL_FORMATS
     * will be ignored (following how it would work with just OR-ing the ints).
     *
     * @param strings - list of strings representing the various formats
     * @return integer value corresponding to OR of all the values.
     */
    static int intFromStringList(List<String> strings) {
        if (strings == null) return BarcodeFormats.ALL_FORMATS.intValue;
        int val = 0;
        for (String string : strings) {
            Integer asInt = BarcodeFormats.formatsMap.get(string);
            if (asInt != null) {
                val |= asInt;
            }
        }
        return val;
    }

    static BarcodeScannerOptions optionsFromStringList(List<String> strings) {
        if (strings == null) {
            return new BarcodeScannerOptions.Builder().setBarcodeFormats(ALL_FORMATS.intValue).build();
        }

        List<Integer> ints = new ArrayList<>(strings.size());
        for (int i = 0, l = strings.size(); i < l; ++i) {
            Integer integer = BarcodeFormats.formatsMap.get(strings.get(i));
            if (integer != null) {
                ints.add(integer);
            }
        }

        if (ints.size() == 0) {
            return new BarcodeScannerOptions.Builder().setBarcodeFormats(ALL_FORMATS.intValue).build();
        }

        if (ints.size() == 1) {
            return new BarcodeScannerOptions.Builder().setBarcodeFormats(ints.get(0)).build();
        }

        int first = ints.get(0);
        int[] rest = new int[ints.size() - 1];
        int i = 0;
        for (Integer e : ints.subList(1, ints.size())) {
            rest[i++] = e;
        }


        return new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(first, rest).build();
    }

    public static BarcodeFormats getFromValue(final int value) {
        for (BarcodeFormats bf : BarcodeFormats.values()) {
            if(bf.intValue == value) {
                return bf;
            }
        }
        throw new NoSuchElementException();
    }

}
