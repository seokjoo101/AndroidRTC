package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.ByteBuffer;

import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.VideoCodec;

/**
 * Created by Seokjoo on 2016-07-26.
 */
public class LoginActivity extends Activity implements View.OnClickListener ,  ScreenDecoder.setDecoderListener ,VideoCodec{

    Intent mqttServiceIntent=null;
    Intent videoServiceIntent=null;
    EditText to;
    EditText from;
    private ScreenRecorder mRecorder;

    private MediaProjectionManager mMediaProjectionManager;
    private static final int REQUEST_CODE = 1;
    private ScreenDecoder mDecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        to=(EditText)findViewById(R.id.to);
        from=(EditText)findViewById(R.id.from);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        findViewById(R.id.fromsend).setOnClickListener(this);
        findViewById(R.id.tosend).setOnClickListener(this);
        findViewById(R.id.record).setOnClickListener(this);
        findViewById(R.id.ringoff).setOnClickListener(this);
        mDecorder= new ScreenDecoder(this);

        Point displaySize = new Point();

        /*windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(displaySize);
        Global.width=displaySize.x;
        Global.height=displaySize.x;
*/
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
    private WindowManager windowManager;



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

    public void startDecoder( ) {

        Intent abc= new Intent(this,RtcActivity.class);
        startActivity(abc);

         if(mDecorder==null)
            mDecorder= new ScreenDecoder(this);

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

