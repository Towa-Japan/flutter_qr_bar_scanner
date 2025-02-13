# Flutter QR Bar Scanner

 A Full Screen Scanner for Scanning QR code and Barcode using Google's Mobile Vision API

 Reading & Scanning QR/Bar codes using Google's [MLKit](https://developers.google.com/ml-kit/vision/barcode-scanning).

 This plugin uses Android native APIs for reading images from the device's camera.
 It then pipes these images both to the MLKit Vision Barcode API which detects qr/bar codes etc,
 and outputs a preview image to be shown on a flutter texture.

 The plugin includes a widget which performs all needed transformations on the camera
 output to show within the defined area.

This has been forked from [contactlutforrahman/flutter_qr_bar_scanner](https://github.com/contactlutforrahman/flutter_qr_bar_scanner),
however there has been no activity for several years, including handling (or rejecting) pull requests.
As such, we have chosen to make improvements here without attempting to contribute to the original project.
We are open to contributing to a similar project rather than maintaining our own version if it meets our needs.
The original plugin supported both Android and iOS, however we are currently using only the Android version.
As we are unable to maintain and test an iOS version, we have chosen to remove support.
If someone is willing to maintain and test the iOS version, we are willing to accept contributions.

## Android Models

 With this new version of MLKit, there are two separate models you can use to do the barcode scanning. This
 plugin chooses to use the build-in model.  This will increase your code size by ~2.2MB but will
 result in better scanning and won't require a separate package to be downloaded in the background
 for barcode scanning to work properly.

## Usage

See the example for how to use this plugin; it is the best resource available as it shows
the plugin in use. However, these are the steps you need to take to
use this plugin.

First, figure out the area that you want the camera preview to be shown in. This is important
as the preview __needs__ to have a constrained size or it won't be able to build. This
is required due to the complex nature of the transforms needed to get the camera preview to
show correctly while still working with the screen rotated etc.

It may be possible to get the camera preview to work without putting it in a SizedBox or Container,
but the recommended way is to put it in a SizedBox or Container.

You then need to include the package and instantiate the camera.


```
import 'package:flutter/material.dart';
import 'package:flutter_qr_bar_scanner/qr_bar_scanner_camera.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _qrInfo = 'Scan a QR/Bar code';
  bool _camState = false;

  _qrCallback(Iterable<ScanResult> rslt) {
    if (rslt.isNotEmpty) {
      setState(() {
        _camState = false;

        _qrInfo = rslt.first.content;
      });
    }
  }

  _scanCode() {
    setState(() {
      _camState = true;
    });
  }

  @override
  void initState() {
    super.initState();
    _scanCode();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Flutter QR/Bar Code Reader'),
        ),
        body: Center(
          child: _camState
              ? SizedBox(
                  height: 1000,
                  width: 500,
                  child: QRBarScannerCamera(
                    onError: (context, error) => Text(
                      error.toString(),
                      style: TextStyle(color: Colors.red),
                    ),
                    qrCodeCallback: (code) {
                      _qrCallback(code);
                    },
                  ),
                )
              : Text(_qrInfo!),
        ),
      ),
    );
  }
}
```

The QrCodeCallback can do anything you'd like, and will keep receiving QR/Bar codes
until the camera is stopped.

There are also optional parameters to QRScannerCamera.

### `fit`

Takes as parameter the flutter `BoxFit`.
Setting this to different values should get the preview image to fit in
different ways, but only `BoxFit = cover` has been tested extensively.

### `notStartedBuilder`

A callback that must return a widget if defined.
This should build whatever you want to show up while the camera is loading (which can take
from milliseconds to seconds depending on the device).

### `child`

Widget that is shown on top of the QRScannerCamera. If you give it a specific size it may cause
weird issues so try not to.

### `key`

Standard flutter key argument. Can be used to get QRScannerCameraState with a GlobalKey.

### `offscreenBuilder`

A callback that must return a widget if defined.
This should build whatever you want to show up when the camera view is 'offscreen'.
i.e. when the app is paused. May or may not show up in preview of app.

### `onError`

Callback for if there's an error.

### 'formats'

A list of supported formats, all by default. If you use all, you shouldn't define any others.

These are the supported types:

```
  ALL_FORMATS,
  AZTEC,
  CODE_128,
  CODE_39,
  CODE_93,
  CODABAR,
  DATA_MATRIX,
  EAN_13,
  EAN_8,
  ITF,
  PDF417,
  QR_CODE,
  UPC_A,
  UPC_E
```

## Push and Pop

If you push a new widget on top of a the current page using the navigator, the camera doesn't
necessarily know about it.

## Contributions

Any kind of contribution will be appreciated.

## License
[MIT License](https://github.com/Towa-Japan/flutter_qr_bar_scanner/blob/master/LICENSE)
