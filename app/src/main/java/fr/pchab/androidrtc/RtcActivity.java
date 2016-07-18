package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
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

import butterknife.BindView;
import fr.pchab.androidrtc.base.Global;

public class RtcActivity extends Activity  implements ScreenRecorder.surfaceListener {
    Intent mqttServiceIntent=null;
    Intent videoServiceIntent=null;
    EditText to;
    EditText from;
    Button bFromsend;
    Button bTosend;

    SurfaceView videoView;


    Button ringoff;

    VideoViewService videoViewService;

    private MediaProjectionManager mMediaProjectionManager;
    private static final int REQUEST_CODE = 1;
    private ScreenRecorder mRecorder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        videoViewService=new VideoViewService();

        from=(EditText)findViewById(R.id.from);
        to=(EditText)findViewById(R.id.to);
        videoView=(SurfaceView)findViewById(R.id.screendisplay);

        bFromsend=(Button)findViewById(R.id.fromsend);
        bTosend=(Button) findViewById(R.id.tosend);
        ringoff=(Button)findViewById(R.id.ringoff);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        bFromsend.setOnClickListener(new View.OnClickListener() {
            //topic 으로 regit.(subscribe)
            public void onClick(View v) {
                subscribe_topic(from.getText().toString());
                startVideoService();
             }
        });


        bTosend.setOnClickListener(new View.OnClickListener() {

            //상대 topic publish
            public void onClick(View v) {
                Global.ToTopic = to.getText().toString();
                VideoViewService.getmInstance().call();

            }
        });


        ringoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }
        // video size
        final int width = 1280;
        final int height = 720;
         final int bitrate = 6000000;
        mRecorder = new ScreenRecorder(this,width, height, bitrate, 1, mediaProjection);
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
    public void setSerface(Surface s) {
        s=videoView.getHolder().getSurface() ;
    }
}