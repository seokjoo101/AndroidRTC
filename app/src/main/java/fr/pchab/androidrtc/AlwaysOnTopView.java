package fr.pchab.androidrtc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.pchab.androidrtc.base.WindowTouchView;

public class AlwaysOnTopView extends Service implements WindowTouchView {


    private View windowView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowViewLayoutParams;
    private WindowTouchPresenter windowTouchPresenter;

    @BindView(R.id.recordImage)
    ImageView recording;

    @Override
    public void onCreate() {
        super.onCreate();

         windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        initWindowLayout((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        windowTouchPresenter = new WindowTouchPresenter(this);



    }

    private void initWindowLayout(LayoutInflater layoutInflater) {
        windowView = layoutInflater.inflate(R.layout.alwaysontop_view, null);
        ButterKnife.bind(this, windowView);

        Animation anim = AnimationUtils.loadAnimation
                (getApplicationContext(), // 현재화면 제어권자
                        R.anim.alpha_ani);      // 에니메이션 설정한 파일



        windowViewLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                30, 30, // X, Y 좌표
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        windowViewLayoutParams.gravity =   Gravity.START;

        windowManager.addView(windowView, windowViewLayoutParams);
        recording.startAnimation(anim);
        windowView.setOnTouchListener(touchListener);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(windowView!=null){
            windowManager.removeView(windowView);
            windowView=null;
//            recording.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateViewLayout(int x, int y) {

        if (windowViewLayoutParams != null) {
            windowViewLayoutParams.x += x;
            windowViewLayoutParams.y += y;

            windowManager.updateViewLayout(windowView, windowViewLayoutParams);
        }
    }


    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            windowTouchPresenter.onTouch(event);
            return false;
        }
    };
}
