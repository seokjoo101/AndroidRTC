package fr.pchab.androidrtc;

import com.stephentuso.welcome.WelcomeScreenBuilder;
import com.stephentuso.welcome.ui.WelcomeActivity;
import com.stephentuso.welcome.util.WelcomeScreenConfiguration;

/**
 * Created by Seokjoo on 2016-07-15.
 */
public class MyWelcomeActivity extends WelcomeActivity {
    @Override
    protected WelcomeScreenConfiguration configuration() {
        return new WelcomeScreenBuilder(this)
                .theme(R.style.CustomWelcomeScreenTheme)
                .defaultTitleTypefacePath("Montserrat-Bold.ttf")
                .defaultHeaderTypefacePath("Montserrat-Bold.ttf")
                .titlePage(R.drawable.photo, "반갑습니다", R.color.orange_background)
                .titlePage(R.drawable.photo, "○○○은 단말화면을 \n공유하는 동시에\n영상 통화를 할 수 있습니다", R.color.blue_background)
                .basicPage(R.drawable.photo, "영상 회의", "동료와 내 핸드폰 화면을 보면서 \n영상회의를 할 수 있습니다", R.color.red_background)
                .parallaxPage(R.layout.parallax_example, "친구와 정보 공유", "여행 계획을 짜거나 약속을 정할 때 \n편리합니다", R.color.purple_background, 0.2f, 2f)
                .basicPage(R.drawable.photo, "함께 보는 비디오", "떨어져 있어도 같은 곳에서 비디오를 보는 듯\n 재미를 느낄 수 있습니다 ", R.color.teal_background)
                .titlePage(R.drawable.photo, "시작해볼까요?", R.color.blue_background)
                .swipeToDismiss(true)
                .exitAnimation(android.R.anim.fade_out)
                .build();
    }

}
