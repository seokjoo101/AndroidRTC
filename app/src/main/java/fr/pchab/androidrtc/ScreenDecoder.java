package fr.pchab.androidrtc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import org.webrtc.DataChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.VideoCodec;

/**
 * Created by Seokjoo on 2016-07-19.
 */

public class ScreenDecoder extends Thread implements DataChannel.Observer ,VideoCodec{


    private MediaCodec mDecoder;
    public boolean eosReceived;
    public setDecoderListener setDecoderListener;

    boolean IsRun;
    ByteBuffer byteBuffer;
    ByteBuffer csd0;
    boolean isInput = true;
    boolean configured = false;
    private static ScreenDecoder minstance;



    MediaFormat format;

    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

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


                 format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            format.setByteBuffer("csd-0",csd0);
                mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
                Log.e(Global.TAG_, "format : " + format);
                mDecoder.configure(format, surface, null, 0 /* Decoder */);
                mDecoder.start();
                configured=true;

             this.start();
      } catch (IOException e) {
            e.printStackTrace();
            Log.e(Global.TAG_,"Init exception : " + e);
        }

        return true;
    }



    void decode(ByteBuffer buffer){
        if (configured) {
//                dequeueInputBuffer를 통해 현재 사용 가능한 index를 받아 온다.
            int inputIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);

            if (inputIndex >= 0) {


                //해당 index에 접근하여 실제 Byte를 사용
                ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);

                if (inputBuffer != null){
                    inputBuffer.put(buffer);
                    mDecoder.queueInputBuffer(inputIndex, 0, 100000, 10000000, 0);

                    Log.i(Global.TAG_,"info size : "+info.size);
                    Log.i(Global.TAG_,"info presentationtime : "+info.presentationTimeUs);
                }

            }
        }
    }

    @Override
    public void run() {


        try {


                while (!eosReceived ) {
                    if(configured) {
                        int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
//                        Log.i(Global.TAG_, "outIndex : " + outIndex);
                        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {


                        }
                        else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            try {
                                // wait 10ms
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                            }
                        }
                        else if (outIndex >= 0) {


                            Log.i(Global.TAG_,"info size "+info.size);
                            Log.i(Global.TAG_,"info time "+info.presentationTimeUs);

                            mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    }
                }


        }
        finally {
            if(configured){

                release();

            }
        }


    }


    private void release() {
        if (mDecoder != null) {

            mDecoder.stop();
            mDecoder.release();
            mDecoder=null;
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
        String state= VideoViewService.getInstance().client.dataChannel.state().toString();
        Log.i(Global.TAG_,"data channel state " + state);

        if(state.equalsIgnoreCase("CLOSING"))
        {
        }
    }


    @Override
    public void onMessage(DataChannel.Buffer buffer) {
//        Log.d(Global.TAG_,"receive buffer : " + buffer.data);
        byteBuffer=buffer.data;

        if(!IsRun){
            csd0=byteBuffer;
            setDecoderListener.startDecoder( );
            IsRun=true;
        }


        decode(byteBuffer);

    }

    interface setDecoderListener{
        void startDecoder( );
        void stopDecoder();

    }

}

