package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.VideoCodec;

/**
 * Created by Seokjoo on 2016-07-26.
 */
public class CallActivity extends Activity implements View.OnClickListener,ScreenDecoder.setDecoderListener,VideoCodec {

    Intent mqttServiceIntent=null;
    Intent videoServiceIntent=null;
    Intent AlwaysOnServiceIntent;

    EditText to;
    EditText from;
    private ScreenRecorder mRecorder;

    private MediaProjectionManager mMediaProjectionManager;
    private static final int REQUEST_CODE = 1;
    private ScreenDecoder mDecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calling_activity);

        to=(EditText)findViewById(R.id.to);
        from=(EditText)findViewById(R.id.from);

        mDecorder= new ScreenDecoder(this);

        getTopic();


        Log.i(Global.TAG,"mytopic : " + Global.Mytopic);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
       findViewById(R.id.tosend).setOnClickListener(this);
        findViewById(R.id.record).setOnClickListener(this);
        findViewById(R.id.ringoff).setOnClickListener(this);

        }



    // 값 불러오기
    private void getTopic(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        Global.Mytopic = pref.getString("mytopic", "null");

        if(mqttServiceIntent==null)
            subscribe_topic();
        if(videoServiceIntent==null)
          startVideoService();

    }
    @Override
    public void onClick(View view) {
        int id=view.getId();

          if(id==R.id.tosend){
            Global.ToTopic = to.getText().toString();
            VideoViewService.getInstance().call();
        }
        else if(id==R.id.record){
            if(mRecorder!=null) {
                mRecorder.quit();
                mRecorder = null;

                stopService(AlwaysOnServiceIntent);
                Toast.makeText(this, "화면 공유를 종료합니다", Toast.LENGTH_SHORT).show();

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


    private  void startAlwaysOnTopService(){

        AlwaysOnServiceIntent = new Intent(this,AlwaysOnTopView.class);
        this.startService(AlwaysOnServiceIntent);
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
        Toast.makeText(this, "화면 공유를 시작합니다", Toast.LENGTH_SHORT).show();
        startAlwaysOnTopService();
        moveTaskToBack(true);

    }
    //MQTT 서비스 connect
    public void subscribe_topic() {
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
                                stopService(AlwaysOnServiceIntent);

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
