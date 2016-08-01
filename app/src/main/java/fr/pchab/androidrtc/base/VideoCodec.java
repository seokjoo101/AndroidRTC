package fr.pchab.androidrtc.base;

import android.media.MediaFormat;

/**
 * Created by Seokjoo on 2016-07-21.
 */
public interface VideoCodec {

    // video size
    int width =600;
    int height =800;
    int bitrate =1000000;

    // parameters for codec
    String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_H263; // H.264 Advanced Video Coding
    int FRAME_RATE = 30; // 30 fps
    int IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    int TIMEOUT_US = 10000;


}
