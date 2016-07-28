package fr.pchab.androidrtc;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import fr.pchab.androidrtc.base.Global;

public class VideoViewService extends Service implements  WebRtcClient.RtcListener  {



    private RendererCommon.ScalingType scalingType =  RendererCommon.ScalingType.SCALE_ASPECT_FILL;

    public WebRtcClient client;



    private Handler mHandler;


    private static VideoViewService mInstance;

    public  static VideoViewService getInstance(){
        if(mInstance!=null)
            return mInstance;
        else
            return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mInstance=this;


        init();



    }


    private void init() {

        client = new WebRtcClient(this);
        client.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }






    @Override
    public void onStatusChanged(String newStatus) {
        mHandler.post(new ToastRunnable(newStatus));

    }


    //통화 종료 될때
    @Override
    public void onRemoveRemoteStream() {

        ScreenDecoder.getInstance().setDecoderListener.stopDecoder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public  void call(){
         client.call(Global.ToTopic);
    }


    public void ringOff(){
        client.removeConnection();
    }

    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run(){
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }



 }
