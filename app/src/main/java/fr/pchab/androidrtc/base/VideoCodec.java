package fr.pchab.androidrtc.base;

/**
 * Created by Seokjoo on 2016-07-21.
 */
public interface VideoCodec {

    // video size
    int width =1280;
    int height =720;
    int bitrate =6000000;

    // parameters for codec
    String MIME_TYPE = "video/3gpp"; // H.264 Advanced Video Coding
    int FRAME_RATE = 30; // 30 fps
    int IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    int TIMEOUT_US = 10000;

}
