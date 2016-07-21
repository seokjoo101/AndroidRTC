package fr.pchab.androidrtc;

import android.media.MediaCodec;
import android.media.MediaFormat;
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

public class ScreenDecoder extends Thread implements DataChannel.Observer ,VideoCodec{

    private static final String VIDEO = "video/";

    private MediaCodec mDecoder;

    public boolean eosReceived;
    public setDecoderListener setDecoderListener;

    boolean IsRun;
    ByteBuffer byteBuffer;

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

    public boolean init(Surface surface) {


        eosReceived = false;
        try {

                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
                Log.e(Global.TAG_, "format : " + format);
                mDecoder.configure(format, surface, null, 0 /* Decoder */);
                mDecoder.start();


        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Global.TAG_,"Init exception : " + e);
        }

        return true;
    }


    @Override
    public void run() {

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        mDecoder.getOutputBuffers();

        boolean isInput = true;
        boolean first = false;
        long startWhen = 0;

        while (!eosReceived) {

            if (isInput) {
//                dequeueInputBuffer를 통해 현재 사용 가능한 index를 받아 온다.
                int inputIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);

                if (inputIndex >= 0) {
                    //해당 index에 접근하여 실제 Byte를 사용
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);

//                    ByteBuffer inputBuffer = inputBuffers[inputIndex];

                    inputBuffer.clear();
                    inputBuffer.put(byteBuffer);

                    byte[] b = new byte[byteBuffer.remaining()];
                    byteBuffer.get(b);


                    mDecoder.queueInputBuffer(inputIndex, 0, 1000 ,5000000, 0);
                    Log.i(Global.TAG_, "byteBuffer : "+ byteBuffer);
                    Log.i(Global.TAG_, "byte array length  : "+ b.length);
                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            Log.e(Global.TAG_, "outIndex : "+ outIndex);

            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.e(Global.TAG_, "INFO_OUTPUT_BUFFERS_CHANGED");
                    mDecoder.getOutputBuffers();
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.e(Global.TAG_, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder.getOutputFormat());
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
			    	Log.e(Global.TAG_, "INFO_TRY_AGAIN_LATER");
                    break;

                default:
                    if (!first) {
                        startWhen = System.currentTimeMillis();
                        first = true;
                    }
                    try {
                        long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                        Log.d(Global.TAG_, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);

                    break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(Global.TAG_, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();

        }

    }

    public void quit() {
        eosReceived = true;

    }


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

}

