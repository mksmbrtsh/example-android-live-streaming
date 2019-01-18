package com.mux.libcamera.camera2;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.util.Log;
import android.util.Size;
import android.view.View;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.mux.libcamera.CamcorderBase;
import com.mux.libcamera.SinkRtmp;
import com.mux.libcamera.encoders.Encoder;
import com.mux.libcamera.encoders.EncoderAudioAAC;
import com.mux.libcamera.encoders.EncoderVideoH264;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Camcorder extends CamcorderBase {
    private CameraTextureView cameraPreview;

    public Camcorder(Context ctx, String cameraId, OnCameraOpenListener listener) {
        cameraPreview = new CameraTextureView(ctx, cameraId, listener);
    }

    @Override
    public void release(Activity ctx) {
        super.release(ctx);
        cameraPreview.release();
    }

    @Override
    public View getPreview() {
        return cameraPreview;
    }

    private Encoder.ISink mSink;

    @Override
    public void startRecord(Activity activity, String streamKey, RtmpHandler.RtmpListener listener) throws IOException {
        super.startRecord(activity, streamKey, listener);
        Size capturedSize = supportedCaptureSizes.get(captureSizeIndex);
        videoEncoder = new EncoderVideoH264(capturedSize, true);
        audioEncoder = new EncoderAudioAAC(EncoderAudioAAC.SupportedSampleRate[7],
                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                EncoderAudioAAC.SupportBitRate[2]);
        mSink = new SinkRtmp("rtmp://192.168.6.31/oflaDemo", capturedSize, listener);
        videoEncoder.setSink(mSink);
        audioEncoder.setSink(mSink);
        audioEncoder.start();
        cameraPreview.startRecording(videoEncoder);
    }

    @Override
    public void stopRecord(Activity activity) {
        super.stopRecord(activity);
        cameraPreview.stop();
        audioEncoder.stop();
        mSink.close();
    }

    @Override
    public void takeSnapshot() {

    }

}
