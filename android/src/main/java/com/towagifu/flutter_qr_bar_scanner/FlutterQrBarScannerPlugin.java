package com.towagifu.flutter_qr_bar_scanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.TextureRegistry;

/**
 * FlutterQrBarScannerPlugin
 */
public class FlutterQrBarScannerPlugin implements MethodCallHandler, QrReaderCallbacks, QrReader.QRReaderStartedCallback, PluginRegistry.RequestPermissionsResultListener, FlutterPlugin, ActivityAware {
    public static final String LOG_TAG_PREFIX = "ctg.fqbrs.";

    private static final String TAG = FlutterQrBarScannerPlugin.LOG_TAG_PREFIX + FlutterQrBarScannerPlugin.class.getSimpleName();
    private static final int REQUEST_PERMISSION = 1;
    private MethodChannel channel;
    private Activity activity;
    private TextureRegistry textures;
    private Integer lastHeartbeatTimeout;
    private boolean waitingForPermissionResult;
    private boolean permissionDenied;
    private ReadingInstance readingInstance;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        Log.i(TAG, "Plugin Registration being performed: flutterPluginBinding " + binding);

        textures = binding.getTextureRegistry();
        channel = new MethodChannel(binding.getBinaryMessenger(), "com.towagifu/flutter_qr_bar_scanner");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if(channel != null) {
            channel.setMethodCallHandler(null);
            textures = null;
            channel = null;
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        attachToActivity(binding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        attachToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    private void attachToActivity(ActivityPluginBinding binding) {
        Log.i(TAG, "Attaching to activity: activityPluginBinding " + binding);

        activity = binding.getActivity();
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            waitingForPermissionResult = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permissions request granted.");
                stopReader();
            } else {
                Log.i(TAG, "Permissions request denied.");
                permissionDenied = true;
                startingFailed(new QrReader.Exception(QrReader.Exception.Reason.noPermissions));
                stopReader();
            }
            return true;
        }
        return false;
    }

    private void stopReader() {
        if (readingInstance != null) {
            if (readingInstance.reader != null) {
                readingInstance.reader.stop();
            }
            if (readingInstance.textureEntry != null) {
                readingInstance.textureEntry.release();
            }
        }
        readingInstance = null;
        lastHeartbeatTimeout = null;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {
        if(textures == null) {
            result.error("ENGINE_ERROR", "not attached to engine", null);
            return;
        }

        if(activity == null) {
            result.error("ACTIVITY_ERROR", "not attached to activity", null);
            return;
        }

        switch (methodCall.method) {
            case "start": {
                if (permissionDenied) {
                    permissionDenied = false;
                    result.error("QRREADER_ERROR", "noPermission", null);
                } else if (readingInstance != null) {
                    result.error("ALREADY_RUNNING", "Start cannot be called when already running", "");
                } else {
                    lastHeartbeatTimeout = methodCall.argument("heartbeatTimeout");
                    Integer targetWidth = methodCall.argument("targetWidth");
                    Integer targetHeight = methodCall.argument("targetHeight");
                    List<String> formatStrings = methodCall.argument("formats");
                    CameraOrientation orientation = CameraOrientation.parse(methodCall.argument("orientation"));

                    if (targetWidth == null || targetHeight == null) {
                        result.error("INVALID_ARGUMENT", "Missing a required argument", "Expecting targetWidth, targetHeight, and optionally heartbeatTimeout");
                        break;
                    }

                    BarcodeScannerOptions options = BarcodeFormats.optionsFromStringList(formatStrings);

                    TextureRegistry.SurfaceTextureEntry textureEntry = textures.createSurfaceTexture();
                    QrReader reader = new QrReader(targetWidth, targetHeight, activity, options,
                        this, this, textureEntry.surfaceTexture());

                    readingInstance = new ReadingInstance(reader, textureEntry, result);
                    try {
                        reader.start(
                            lastHeartbeatTimeout == null ? 0 : lastHeartbeatTimeout,
                            orientation
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                        result.error("IOException", "Error starting camera because of IOException: " + e.getLocalizedMessage(), null);
                    } catch (QrReader.Exception e) {
                        e.printStackTrace();
                        result.error(e.reason().name(), "Error starting camera for reason: " + e.reason().name(), null);
                    } catch (NoPermissionException e) {
                        waitingForPermissionResult = true;
                        ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
                    }
                }
                break;
            }
            case "setTorchState": {
                if (readingInstance != null && !waitingForPermissionResult) {
                    readingInstance.reader.qrCamera.setTorchState(methodCall.argument("isOn"));
                }
                result.success(null);
                break;
            }
            case "stop": {
                if (readingInstance != null && !waitingForPermissionResult) {
                    stopReader();
                }
                result.success(null);
                break;
            }
            case "heartbeat": {
                if (readingInstance != null) {
                    readingInstance.reader.heartBeat();
                }
                result.success(null);
                break;
            }
            default:
                result.notImplemented();
        }
    }

    @Override
    public void qrRead(Stream<QrBarcode> data) {
        channel.invokeMethod("qrRead",
            data.map(d -> d.getForChannel())
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public void started() {
        Map<String, Object> response = new HashMap<>();
        response.put("surfaceWidth", readingInstance.reader.qrCamera.getWidth());
        response.put("surfaceHeight", readingInstance.reader.qrCamera.getHeight());
        response.put("surfaceOrientation", readingInstance.reader.qrCamera.getOrientation());
        response.put("textureId", readingInstance.textureEntry.id());
        readingInstance.startResult.success(response);
    }

    private List<String> stackTraceAsString(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return null;
        }

        List<String> stackTraceStrings = new ArrayList<>(stackTrace.length);
        for (StackTraceElement el : stackTrace) {
            stackTraceStrings.add(el.toString());
        }
        return stackTraceStrings;
    }

    @Override
    public void startingFailed(Throwable t) {
        Log.w(TAG, "Starting QR Mobile Vision failed", t);
        List<String> stackTraceStrings = stackTraceAsString(t.getStackTrace());

        if (t instanceof QrReader.Exception) {
            QrReader.Exception qrException = (QrReader.Exception) t;
            readingInstance.startResult.error("QRREADER_ERROR", qrException.reason().name(), stackTraceStrings);
        } else {
            readingInstance.startResult.error("UNKNOWN_ERROR", t.getMessage(), stackTraceStrings);
        }
    }

    private class ReadingInstance {
        final QrReader reader;
        final TextureRegistry.SurfaceTextureEntry textureEntry;
        final Result startResult;

        private ReadingInstance(QrReader reader, TextureRegistry.SurfaceTextureEntry textureEntry, Result startResult) {
            this.reader = reader;
            this.textureEntry = textureEntry;
            this.startResult = startResult;
        }
    }
}
