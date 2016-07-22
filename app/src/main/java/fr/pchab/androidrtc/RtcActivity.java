package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.webrtc.DataChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import butterknife.BindView;
import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.VideoCodec;

public class RtcActivity extends Activity implements View.OnClickListener ,ScreenDecoder.setDecoderListener , VideoCodec {
    Intent mqttServiceIntent=null;
    Intent videoServiceIntent=null;
    EditText to;
    EditText from;

    SurfaceView videoView;

    VideoViewService videoViewService;


    private MediaProjectionManager mMediaProjectionManager;
    private static final int REQUEST_CODE = 1;
    private ScreenRecorder mRecorder;
    private ScreenDecoder mDecorder;

    Surface surface;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        to=(EditText)findViewById(R.id.to);
        from=(EditText)findViewById(R.id.from);


        videoViewService=new VideoViewService();
        mDecorder= new ScreenDecoder(this);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        videoView=(SurfaceView)findViewById(R.id.screendisplay);
        surface = videoView.getHolder().getSurface();


        findViewById(R.id.fromsend).setOnClickListener(this);
        findViewById(R.id.tosend).setOnClickListener(this);
        findViewById(R.id.record).setOnClickListener(this);
        findViewById(R.id.ringoff).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id=view.getId();

        if(id==R.id.fromsend){
            subscribe_topic(from.getText().toString());
            startVideoService();
        }
        else if(id==R.id.tosend){
            Global.ToTopic = to.getText().toString();
            VideoViewService.getInstance().call();
        }
        else if(id==R.id.record){
            //                VideoViewService.getmInstance().ringOff();

            if(mRecorder!=null) {
                mRecorder.quit();
                mRecorder = null;

            }
            else{
                Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, REQUEST_CODE);

            }
        }
        else if(id==R.id.ringoff){
            VideoViewService.getInstance().ringOff();
         }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }

          mRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection);
        mRecorder.start();
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();

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

                                if(mqttServiceIntent!=null )
                                    stopService(mqttServiceIntent);
                                if(videoServiceIntent!=null)
                                    stopService(videoServiceIntent);

                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();

            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }



    //MQTT 서비스 connect
    public void subscribe_topic(String sub_topic) {
        Global.Mytopic=sub_topic;
        mqttServiceIntent = new Intent(this,ServiceMqtt.class);
        this.startService(mqttServiceIntent);
    }

    private void startVideoService() {
        videoServiceIntent = new Intent(this,VideoViewService.class);
        this.startService(videoServiceIntent);

    }


    @Override
    public void startDecoder(ByteBuffer buffer) {

        if(mDecorder==null)
            mDecorder= new ScreenDecoder(this);

        mDecorder.setRun(true);
        mDecorder.start();

        mDecorder.init(surface,buffer);
        Log.e(Global.TAG_,"startDecoder");

    }

    @Override
    public void stopDecoder() {

        if(mDecorder!=null) {
            mDecorder.quit();
            mDecorder = null;
            Log.e(Global.TAG_,"stopDecoder");
        }

    }


}