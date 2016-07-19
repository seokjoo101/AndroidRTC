package fr.pchab.androidrtc;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import org.webrtc.DataChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.pchab.androidrtc.base.Global;

/**
 * Created by Seokjoo on 2016-07-19.
 */
public class ScreenDecoder extends Thread implements DataChannel.Observer{

    private static final String VIDEO = "video/";

    private MediaCodec mDecoder;
    public static ScreenDecoder minstance;
    public static  MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    public static ScreenDecoder getInstance(){
        if(minstance!=null)
            return minstance;
        else
            return null;
    }

    private boolean eosReceived;


    private setSurfaceListener setSurfaceListener;

    ScreenDecoder(setSurfaceListener  surfaceListener){
        minstance=this;
        setSurfaceListener=surfaceListener;
    }

    public boolean init(Surface surface) {
        eosReceived = false;
        try {

                MediaFormat format = MediaFormat.createVideoFormat(ScreenRecorder.MIME_TYPE, 1280, 720);

                String mime = format.getString(MediaFormat.KEY_MIME);
                mDecoder = MediaCodec.createDecoderByType(ScreenRecorder.MIME_TYPE);
                Log.d(Global.TAG_, "format : " + format);
                mDecoder.configure(format, surface, null, 0 /* Decoder */);
                mDecoder.start();


        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void run() {


        boolean isInput = true;
        boolean first = false;
        long startWhen = 0;

        while (!eosReceived) {
            if (isInput) {
                int inputIndex = mDecoder.dequeueInputBuffer(ScreenRecorder.TIMEOUT_US);

                if (inputIndex >= 0) {
                    // fill inputBuffers[inputBufferIndex] with valid data
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);


                    inputBuffer.put(byteBuffer);


//                    mDecoder.queueSecureInputBuffer(inputIndex,0,null,info.presentationTimeUs,0);

                    mDecoder.queueInputBuffer(inputIndex, 0, 500000, 5000000, 0);

                    inputBuffer.clear();

                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, ScreenRecorder.TIMEOUT_US);

            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(Global.TAG_, "INFO_OUTPUT_BUFFERS_CHANGED");
                    mDecoder.getOutputBuffers();
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.d(Global.TAG_, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder.getOutputFormat());
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
			    	Log.d(Global.TAG_, "INFO_TRY_AGAIN_LATER");
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
                Log.d(Global.TAG_, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        mDecoder.stop();
        mDecoder.release();

    }

    public void close() {
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


    boolean IsRun=false;
    ByteBuffer byteBuffer;

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
//        Log.i(Global.TAG_,"receive buffer : " + buffer.data);

        if(!IsRun){
            setSurfaceListener.setSurface();
            IsRun=true;
        }
        byteBuffer=buffer.data;

    }

    interface setSurfaceListener{
        public void setSurface();
    }
}
