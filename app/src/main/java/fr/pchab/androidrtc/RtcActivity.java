package fr.pchab.androidrtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.os.Handler;
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

public class RtcActivity extends Activity implements   VideoCodec {

    SurfaceView videoView;





     Surface surface;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);






        videoView=(SurfaceView)findViewById(R.id.screendisplay);
        surface = videoView.getHolder().getSurface();

        Handler hd = new Handler();

        hd.post(new surfaceHandler());
     }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 세로 전환시
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(Global.TAG,"세로");
        }
        // 가로 전환시
        else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(Global.TAG,"가로");
        }
     }

    private class surfaceHandler implements Runnable{
        public void run() {
            ScreenDecoder.getInstance().init(surface);


        }
    }

}