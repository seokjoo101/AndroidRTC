package fr.pchab.androidrtc;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import org.webrtc.DataChannel;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.VideoCodec;

/**
 * Created by Seokjoo on 2016-07-19.
 */

public class ScreenDecoder   implements DataChannel.Observer ,VideoCodec{

    private static final String VIDEO = "video/";

    private MediaCodec mDecoder;

    public boolean eosReceived;
    public setDecoderListener setDecoderListener;

    boolean IsRun;
    ByteBuffer byteBuffer;
    byte[] mBuffer = new byte[0];

    private static ScreenDecoder minstance;

    public static ScreenDecoder getInstance(){
        if(minstance!=null)
            return minstance;
        else
            return null;
    }
    ScreenDecoder(setDecoderListener  decoderListener){
        minstance=this;
        setDecoderListener=decoderListener;
        IsRun=false;
    }

    boolean mConfigured=false;
   @Override
    public void onBufferedAmountChange(long l) {
        Log.i(Global.TAG_,"onBufferedAmountChange "  );
    }


    @Override
    public void onStateChange() {
        Log.i(Global.TAG_,"onStateChange"  );
        Log.i(Global.TAG_,"data channel state " + VideoViewService.getInstance().client.dataChannel.state());
    }


    @Override
    public void onMessage(DataChannel.Buffer buffer) {
//        Log.i(Global.TAG_,"receive buffer : " + buffer.data);

        if(!IsRun){
            setDecoderListener.startDecoder();
            IsRun=true;
        }

        byteBuffer=buffer.data;

     }

    interface setDecoderListener{
        void startDecoder();
        void stopDecoder();

    }






    Worker mWorker;

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        if (mWorker != null) {
            mWorker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }


    public void configure(Surface surface, int width, int height, byte[] csd0, int offset, int size) {
        if (mWorker != null) {
            mWorker.configure(surface, width, height, ByteBuffer.wrap(csd0, offset, size));
        }
    }
    public void start() {
        if (mWorker == null) {
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
        }
    }


    class Worker extends Thread {

        volatile boolean mRunning;
        MediaCodec mCodec;
        volatile boolean mConfigured;
        long mTimeoutUs;

        public Worker() {
            mTimeoutUs = 10000l;
        }

        public void setRunning(boolean running) {
            mRunning = running;
        }

        public void configure(Surface surface, int width, int height, ByteBuffer csd0) {
            if (mConfigured) {
                throw new IllegalStateException("Decoder is already configured");
            }
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            // little tricky here, csd-0 is required in order to configure the codec properly
            // it is basically the first sample from encoder with flag: BUFFER_FLAG_CODEC_CONFIG
            format.setByteBuffer("csd-0", csd0);
            try {
                mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create codec", e);
            }
            mCodec.configure(format, surface, null, 0);
            mCodec.start();
            mConfigured = true;
        }

        @SuppressWarnings("deprecation")
        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            if (mConfigured && mRunning) {
                int index = mCodec.dequeueInputBuffer(mTimeoutUs);
                if (index >= 0) {
                    ByteBuffer buffer;
                    // since API 21 we have new API to use
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer = mCodec.getInputBuffers()[index];
                        buffer.clear();
                    } else {
                        buffer = mCodec.getInputBuffer(index);
                    }
                    if (buffer != null) {
                        buffer.put(data, offset, size);
                        buffer.put(data, offset, size);
                        mCodec.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (mRunning) {
                    if (mConfigured) {
                        int index = mCodec.dequeueOutputBuffer(info, mTimeoutUs);
                        if (index >= 0) {
                            // setting true is telling system to render frame onto Surface
                            mCodec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    } else {
                        // just waiting to be configured, then decode and render
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            } finally {
                if (mConfigured) {
                    mCodec.stop();
                    mCodec.release();
                }
            }
        }
    }





}

