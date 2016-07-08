package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.List;

//맨처음 CALL하는것만 MQTT로 하고 node.js 로 sdp 랑 candidate 주는걸로
public class RtcActivity extends Activity implements WebRtcClient.RtcListener,ServiceMqtt.MqttLIstener {
    private final static int VIDEO_CALL_SENT = 666;
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_H_264 = "H.264";


    private static final String AUDIO_CODEC_OPUS = "opus";
    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after call is connected.
    private static final int LOCAL_X_CONNECTED = 0;
    private static final int LOCAL_Y_CONNECTED = 50;
    private static final int LOCAL_WIDTH_CONNECTED = 100;
    private static final int LOCAL_HEIGHT_CONNECTED = 50;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 50;
    Intent serviceIntent;
    EditText to;
    EditText from;
    Button bFromsend;
    Button bTosend;
    ImageButton bHnagOff;
    private RendererCommon.ScalingType scalingType =  RendererCommon.ScalingType.SCALE_ASPECT_FILL;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private WebRtcClient client;
    private String mSocketAddress;
    private String callerId;
    private ServiceMqtt.MqttLIstener mMqttLIstener;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 1:
                    startService(serviceIntent);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);



        from=(EditText)findViewById(R.id.from);
        to=(EditText)findViewById(R.id.to);

        bFromsend=(Button)findViewById(R.id.fromsend);
        bTosend=(Button) findViewById(R.id.tosend);
        bHnagOff=(ImageButton)findViewById(R.id.hangoff);

        bFromsend.setOnClickListener(new View.OnClickListener() {
            //topic 으로 regit.(subscribe)
            public void onClick(View v) {
                subscribe_topic(from.getText().toString());
                startCam();
            }
        });

        bTosend.setOnClickListener(new View.OnClickListener() {

            //상대 topic 으로 publish
            public void onClick(View v) {
                Global.ToTopic = to.getText().toString();
                client.call(Global.ToTopic);
            }
        });
        bHnagOff.setOnClickListener(new View.OnClickListener() {
            //topic 으로 regit.(subscribe)
            public void onClick(View v) {

                onRemoveRemoteStream();
            }
        });





        //풀스크린 , 스크린 유지 , keyguard dismiss , Lock , TURN SCREN
        getWindow().addFlags(
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
                        | LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON);


        mSocketAddress = "http://" + getResources().getString(R.string.host);
        mSocketAddress += (":" + getResources().getString(R.string.port) + "/");


        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);

        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        remoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
        localRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, scalingType, true);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            final List<String> segments = intent.getData().getPathSegments();
            callerId = segments.get(0);
        }



    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            //하드웨어 뒤로가기 버튼에 따른 이벤트 설정
            case KeyEvent.KEYCODE_BACK:

                new AlertDialog.Builder(this)
                        .setTitle("프로그램 종료")
                        .setMessage("프로그램을 종료 하시겠습니까?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 프로세스 종료.

                                stopService(serviceIntent);

                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();

                break;

            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);

        PeerConnectionParameters params = new PeerConnectionParameters(
                true, false, displaySize.x, displaySize.y, 30, 1, VIDEO_CODEC_H_264, true, 1, AUDIO_CODEC_OPUS, true);

        //      해상도를 galaxy3에 맞춘다.
//        PeerConnectionParameters params = new PeerConnectionParameters(
//                true, false, 720, 1280, 30, 1, VIDEO_CODEC_VP8, true, 1, AUDIO_CODEC_OPUS, true);


        client = new WebRtcClient(this, mSocketAddress, params );

    }

    @Override
    public void onPause() {
        super.onPause();
        vsv.onPause();
        if(client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if(client != null) {
            client.onResume();
}
}

    @Override
    public void onDestroy() {
        if(client != null) {
            client.onDestroy();
        }
        super.onDestroy();
//        this.stopService(serviceIntent);

    }

    public void answer(String callerId) throws JSONException {
        //상대방 전화번호


        client.sendMessage(callerId, "init", null);
//        startCam();
    }

    public void call(String callId) {
        Intent msg = new Intent(Intent.ACTION_SEND);
        msg.putExtra(Intent.EXTRA_TEXT, mSocketAddress + callId);
        msg.setType("text/plain");
        startActivityForResult(Intent.createChooser(msg, "Call someone :"), VIDEO_CALL_SENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VIDEO_CALL_SENT) {
//            startCam();
        }
    }

    public void startCam() {
        // Camera settings
        client.start("android_test");
    }

    @Override
    public void onCallReady(String callId) {

    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bHnagOff.setVisibility(View.VISIBLE);

            }
        });
     }

    @Override
    public void onRemoveRemoteStream() {
        VideoRendererGui.update(localRender,
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
                scalingType,true);
    }

    public void subscribe_topic(String sub_topic) {
        Global.Mytopic=sub_topic;
        serviceIntent = new Intent(this,ServiceMqtt.getInstance().getClass());
        this.startService(serviceIntent);
        ServiceMqtt.getInstance().setListener(this);
//        mHandler.sendEmptyMessageDelayed(1,3000);


        // MQTT 서비스 subtopic과 함께 connect
    }

    @Override
    public void getMessage(String msg) {
    }

}