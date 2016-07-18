package fr.pchab.androidrtc;

import android.view.MotionEvent;

import fr.pchab.androidrtc.base.BasePresenter;
import fr.pchab.androidrtc.base.WindowTouchView;

/**
 * Created by Seokjoo on 2016-07-14.
 */
public class WindowTouchPresenter extends BasePresenter<WindowTouchView> {

    private float touchPrevX;
    private float touchPrevY;

    public WindowTouchPresenter(WindowTouchView view) {
        super(view);
    }

    public void onTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                actionMove(event);
                break;

            case MotionEvent.ACTION_UP:
                actionUp(event);
                break;
        }
    }

    private void actionDown(MotionEvent event) {
        touchPrevX = event.getRawX();
        touchPrevY = event.getRawY();
    }

    private void actionMove(MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();

        float x = getDistance(rawX, touchPrevX);
        float y = getDistance(rawY, touchPrevY);

        touchPrevX = rawX;
        touchPrevY = rawY;

        getView().updateViewLayout((int) x, (int) y);
    }

    /**
     * @return Touch Distance
     */
    private float getDistance(float raw, float touchPrev) {
        return raw - touchPrev;
    }

    private void actionUp(MotionEvent event) {
        // Do noting...
    }

}
