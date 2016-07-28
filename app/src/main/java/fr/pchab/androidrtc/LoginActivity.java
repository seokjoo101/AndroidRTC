package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

import java.nio.ByteBuffer;

import fr.pchab.androidrtc.R;
import fr.pchab.androidrtc.RtcActivity;
import fr.pchab.androidrtc.ScreenDecoder;
import fr.pchab.androidrtc.ScreenRecorder;
import fr.pchab.androidrtc.ServiceMqtt;
import fr.pchab.androidrtc.VideoViewService;
import fr.pchab.androidrtc.base.Global;
import fr.pchab.androidrtc.base.VideoCodec;

/**
 * Created by Seokjoo on 2016-07-26.
 */
public class LoginActivity extends Activity {

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        findViewById(R.id.fromsend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Global.Mytopic=((EditText) findViewById(R.id.from)).getText().toString();
                savePreferences();
                goMainActivity();
            }
        });
    }

    // 값 저장하기
    private void savePreferences(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("login",true);
        editor.putString("mytopic",Global.Mytopic);
        editor.commit();
    }

    void goMainActivity(){
        startActivity(new Intent(getApplication(), CallActivity.class)); // 로딩이 끝난후 이동할 Activity
        LoginActivity.this.finish(); // 로딩페이지 Activity Stack에서 제거
    }
}

