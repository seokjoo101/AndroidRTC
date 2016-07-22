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

    boolean configured = false;
    private static ScreenDecoder minstance;

    public static ScreenDecoder getInstance(){
        if(minstance!=null)
            return minstance;
        else
            return null;
    }


    void setRun(boolean running){
        eosReceived = running;
    }


    ScreenDecoder(setDecoderListener  decoderListener){
        minstance=this;
        setDecoderListener=decoderListener;
        IsRun=false;
    }

    public boolean init(Surface surface,ByteBuffer buffer) {



        try {
            if (configured) {
                throw new IllegalStateException("Decoder is already configured");
            }
                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                format.setByteBuffer("bytebuffer",buffer);
                mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
                Log.i(Global.TAG_, "format : " + format);
                mDecoder.configure(format, surface, null, 0 /* Decoder */);
                mDecoder.start();
                configured=true;

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

//                     inputBuffer.clear();

                if (inputBuffer != null)
                    inputBuffer.put(buffer);


                mDecoder.queueInputBuffer(inputIndex, 0, 100000, 10000000, 0);
                Log.i(Global.TAG_, "byteBuffer : " + buffer);
            }
        }
    }

    @Override
    public void run() {

        try {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (!eosReceived ) {
                    if(configured) {
                        int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
                        Log.i(Global.TAG_, "outIndex : " + outIndex);

                        if (outIndex >= 0) {
                            mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    }else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    }
                }

        }
        finally {
            if(configured&& mDecoder!=null){

                mDecoder.stop();
                mDecoder.release();

            }
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
        byteBuffer=buffer.data;

        if(!IsRun){
            setDecoderListener.startDecoder(byteBuffer);
            IsRun=true;
        }


        decode(byteBuffer);


     }

    interface setDecoderListener{
        void startDecoder(ByteBuffer buffer);
        void stopDecoder();

    }

}

