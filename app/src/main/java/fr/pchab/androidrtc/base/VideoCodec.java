package fr.pchab.androidrtc.base;

/**
 * Created by Seokjoo on 2016-07-21.
 */
public interface VideoCodec {

    // video size
    int width =1280;
    int height =720;
    int bitrate =4000000;

    // parameters for codec
    String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    int FRAME_RATE = 30; // 30 fps
    int IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    int TIMEOUT_US = 10000;

    //mpeg4 글자꺠짐 O / 회색줄, 글자 , 가로 전환했을때 전송 중단
    //3gpp 녹색줄 / 딜레이 
    //H.264 O /회색줄 ,딜레이 , 가로 전환했을때 전송 중단
}
