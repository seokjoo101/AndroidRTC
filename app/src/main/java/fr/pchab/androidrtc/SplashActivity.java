package fr.pchab.androidrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by Seokjoo on 2016-07-26.
 */
public class SplashActivity extends Activity {
    boolean isRogin = false;
    Intent mqttService=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        getLoginState();
        Handler hd = new Handler();

        hd.postDelayed(new splashhandler() , 1000); // 3초 후에 hd Handler 실행
    }
    private class splashhandler implements Runnable{
        public void run() {
            if(isRogin){

                startActivity(new Intent(getApplication(), CallActivity.class)); // 로딩이 끝난후 이동할 Activity
                SplashActivity.this.finish(); // 로딩페이지 Activity Stack에서 제거
            }
            else{
                startActivity(new Intent(getApplication(), LoginActivity.class)); // 로딩이 끝난후 이동할 Activity
                SplashActivity.this.finish(); // 로딩페이지 Activity Stack에서 제거
            }
        }
    }


    @Override
    public void onBackPressed(){

    }


    // 값 불러오기
    private void getLoginState(){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        isRogin = pref.getBoolean("login", false);
    }
}
