package com.mux.libcamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.mux.libcamera.encoders.Encoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class CamcorderBase {
    protected Encoder videoEncoder, audioEncoder;
    protected List<Size> supportedCaptureSizes;
    protected int captureSizeIndex = 0;

    public static CamcorderBase CreateCamera(int cameraApiLevel, Activity ctx, int cameraId,
            OnCameraOpenListener listener) throws CameraAccessException {
        CamcorderBase camcorder = null;
        if (cameraApiLevel == 1) {
            camcorder = new com.mux.libcamera.camera1.Camcorder(ctx, cameraId, listener);
        } else {
            CameraManager manager = (CameraManager) ctx.getSystemService(
                    Context.CAMERA_SERVICE);
            if (manager != null) {
                String[] list = manager.getCameraIdList();
                if (list.length > cameraId) {
                    camcorder = new com.mux.libcamera.camera2.Camcorder(ctx, list[cameraId],
                            listener);
                    // Choose the sizes for camera preview and video recording
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(list[cameraId]);
                    StreamConfigurationMap map = characteristics
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        throw new RuntimeException("Cannot get available preview/video sizes");
                    }
                    camcorder.supportedCaptureSizes = new ArrayList<>();
                    for(Size size : map.getOutputSizes(MediaRecorder.class)) {
                        camcorder.supportedCaptureSizes.add(size);
                    }
                    Collections.sort(camcorder.supportedCaptureSizes, new Comparator<Size>() {
                        @Override
                        public int compare(Size p1, Size p2) {
                            return p1.getWidth() - p2.getWidth();
                        }
                    });
                    camcorder.setCaptureSizeIndex(0);
                }
            }
        }
        int rotation = ctx.getWindowManager().getDefaultDisplay().getRotation();
        switch(rotation) {
            case Surface.ROTATION_0:
                ctx.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                ctx.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_180:
                ctx.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                ctx.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
        return camcorder;
    }

    public void release(Activity ctx) {
        ctx.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    public abstract View getPreview();

    public void startRecord(Activity activity, String streamKey, RtmpHandler.RtmpListener listner) throws IOException {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void stopRecord(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void pauseRecord(boolean pause) {
        if (videoEncoder != null) {
            videoEncoder.pauseEncoding(pause);
        }
        if (audioEncoder != null) {
            audioEncoder.pauseEncoding(pause);
        }
    }

    public List<Size> getSupportedCaptureSizes() {
        return supportedCaptureSizes;
    }

    public void setCaptureSizeIndex(int index) {
        captureSizeIndex = index;
    }

    public abstract void takeSnapshot();

    public interface OnCameraOpenListener {
        void onOpened(boolean result);
    }
}
