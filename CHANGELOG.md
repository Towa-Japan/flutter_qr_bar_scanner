## [4.0.0] - 2025/2/3

* upgraded flutter to 3.24.5
* upgraded gradle to 8.3
* upgraded android gradle plugin to 8.1.4
* upgraded mlkit barcode-scanning to 17.3.0
* upgraded native_device_orientation to 2.0.3
* bumped compileSdkVersion to 31
* bumped minSdkVersion to 19

* __breaking__ removed support for iOS
* __breaking__ return bounding box and type of detected barcode along with barcode string contents
* __breaking__ return raw data of detected barcode to allow for handling of non-utf8 encodings or non-string data
* __breaking__ return list of detected barcodes instead of one at a time

* [bug](https://github.com/contactlutforrahman/flutter_qr_bar_scanner/issues/40): auto focus does not work on some Android models
* bug: camera size selection does not work well when multiple aspect ratios are supported

## [3.0.2] - October 31, 2021

* Fixed bugs

## [3.0.1] - October 31, 2021

* Solved missing plugins exception

## [3.0.0] - October 31, 2021

* Solved deprecated API issue

## [2.0.1] - August 28, 2021

* Solved bugs

## [2.0.0] - June 03, 2021

* Upgraded to Flutter Null safety

## [1.0.2] - April 24, 2020

* Fixed pod error

## [1.0.1] - February 9, 2020

* Updated documentation

## [1.0.0] - February 9, 2020

* Flutter QR Bar Scanner initial release