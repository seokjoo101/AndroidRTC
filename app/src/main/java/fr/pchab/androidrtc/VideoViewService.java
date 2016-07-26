package fr.pchab.androidrtc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.webrtc.DataChannel;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.WindowTouchView;

public class VideoViewService extends Service implements  WebRtcClient.RtcListener  {

    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP8 = "VP8";


    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 70;
    private static final int LOCAL_Y_CONNECTED = 70;
    private static final int LOCAL_WIDTH_CONNECTED = 30;
    private static final int LOCAL_HEIGHT_CONNECTED = 30;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;


    private View windowView;
    private WindowManager.LayoutParams windowViewLayoutParams;
    private WindowTouchPresenter windowTouchPresenter;


    private RendererCommon.ScalingType scalingType =  RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    public WebRtcClient client;

  /*  @BindView(R.id.glview_call)
    GLSurfaceView vsv;*/

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

       /* windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        initWindowLayout((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        windowTouchPresenter = new WindowTouchPresenter(this);

        ButterKnife.bind(this, windowView);*/


        init();

        /*vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);


        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
*/
        // local and remote render
/*        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);*/



    }


    private void init() {

        client = new WebRtcClient(this  );

        startCam();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            windowTouchPresenter.onTouch(event);
            return false;
        }
    };




    @Override
    public void onStatusChanged(String newStatus) {
        Log.i(Global.TAG, "state : "+ newStatus);

    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));

        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType,true);

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
        VideoRendererGui.update(remoteRender,
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType,true);
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                scalingType,true);



    }

    //통화 종료 될때
    @Override
    public void onRemoveRemoteStream() {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType,true);

        mHandler.post(new ToastRunnable("통화가 종료되었습니다"));
        ScreenDecoder.getInstance().setDecoderListener.stopDecoder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public void startCam() {
        client.start();
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
