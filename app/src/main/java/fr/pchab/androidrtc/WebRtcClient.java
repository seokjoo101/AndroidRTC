package fr.pchab.androidrtc;

import java.util.HashMap;
import java.util.LinkedList;

import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.opengl.EGLContext;
import android.util.Log;
import org.webrtc.*;

public class WebRtcClient {

    private final static String TAG ="seok";
    public static boolean bTosend =false;
    private static WebRtcClient mInstance;
    Peer peer;
    /**
     * Send a message through the signaling server
     *
     * @param to id of recipient
     * @param type type of message
     * @param payload payload of message
     * @throws JSONException
     */

    // Offer to - 자기전화번호 ,"offer", sdp.description
    //Candidate to - 자기전화번호 ,"candidate ", candidate
    // Answer to - 상대방전화번호 , "answer" , sdp.description

    JSONObject message = new JSONObject();
    String topic;
    String candidate;
    String sdp;
    private PeerConnectionFactory factory;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private PeerConnectionParameters pcParams;
    private MediaConstraints pcConstraints = new MediaConstraints();
    private MediaStream localMS;
    private VideoSource videoSource;
    private RtcListener mListener;
    private Socket client; //ServiceMqtt 의 sampleClient 사용

    public WebRtcClient(RtcListener listener, String host, PeerConnectionParameters params, EGLContext mEGLcontext) {
        mInstance = this;
        mListener = listener;
        pcParams = params;


        PeerConnectionFactory.initializeAndroidGlobals(listener, true, true,
                params.videoCodecHwAcceleration, mEGLcontext);
        factory = new PeerConnectionFactory();
        MessageHandler messageHandler = new MessageHandler();


//****        sampleClient -> messageHandler.onMessage
//****        sampleClient -> messageHandler.onId



//        try {
//            client = IO.socket(host);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//        client.on("id", messageHandler.onId);
//        client.on("message", messageHandler.onMessage);
//        client.connect();
//        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));


        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

//        start("test");

    }


    public static WebRtcClient getmInstance(){
        if(mInstance!=null) {
            Log.i(Global.TAG , "NOT NULL");
            return mInstance;
        }else {
            Log.i(Global.TAG , "NULL");
            return null;
        }
    }

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        ServiceMqtt.getInstance().publish(Global.ToTopic,payload.toString());
//        Log.d(TAG,payload.toString());
     }

    public void call(String to){
        try {
            new CreateOfferCommand().execute(null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Call this method in Activity.onPause()
     */
    public void onPause() {
        if(videoSource != null) videoSource.stop();
    }

    /**
     * Call this method in Activity.onResume()
     */
    public void onResume() {
        if(videoSource != null) videoSource.restart();
    }

    /**
     * Call this method in Activity.onDestroy()
     */
    public void onDestroy() {

        peer.pc.dispose();
        videoSource.dispose();
        factory.dispose();
        client.disconnect();
        client.close();
    }

    /**
     * Start the client.
     *
     * Set up the local stream and notify the signaling server.
     * Call this method after onCallReady.
     *
     * @param name client name
     */
    public void start(String name){
        setCamera();


//        try {
//            JSONObject message = new JSONObject();
//            message.put("name", name);
//            client.emit("readyToStream", message);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    public void getMessage(String msg){
        JSONObject json = null;


        try {
            json = new JSONObject(msg);


        if(json!=null){
            Log.i(Global.TAG , "jason not null");

            Log.i(Global.TAG , "json.isNull : " + json.isNull("type"));

            Log.i(Global.TAG , "json.getString : " +  json.getString("type").equalsIgnoreCase("offer"));

            if(!json.isNull("type") && json.getString("type").equalsIgnoreCase("offer")){
                Log.i(Global.TAG , "OFFER : " + json.getString("sdp"));
                new CreateAnswerCommand().execute(json);
            }else if(!json.isNull("type") && json.getString("type").equalsIgnoreCase("answer")){
                new SetRemoteSDPCommand().execute(json);
                Log.i(Global.TAG , "answer : " + json.getString("sdp"));
            }else if (!json.isNull("type") && json.getString("tpye").equalsIgnoreCase("candidate")){
                Log.i(Global.TAG , "candidate : " + json.getString("candidate"));
                new AddIceCandidateCommand().execute(json);
            }
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setCamera(){
        localMS = factory.createLocalMediaStream("ARDAMS");
        if(pcParams.videoCallEnabled){
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(pcParams.videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(pcParams.videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(pcParams.videoFps)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(pcParams.videoFps)));

            videoSource = factory.createVideoSource(getVideoCapturer(), videoConstraints);
            localMS.addTrack(factory.createVideoTrack("ARDAMSv0", videoSource));
        }

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localMS.addTrack(factory.createAudioTrack("ARDAMSa0", audioSource));

        mListener.onLocalStream(localMS);

        peer = new Peer();
//        peer.pc.createOffer(peer,pcConstraints);


     }

    private VideoCapturer getVideoCapturer() {
        String frontCameraDeviceName = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        return VideoCapturerAndroid.create(frontCameraDeviceName);
    }

    /**
     * Implement this interface to be notified of events.
     */
    public interface RtcListener{
        void onCallReady(String callId);

        void onStatusChanged(String newStatus);

        void onLocalStream(MediaStream localStream);

        void onAddRemoteStream(MediaStream remoteStream, int endPoint);

        void onRemoveRemoteStream(int endPoint);
    }

    private interface Command{
        void execute( JSONObject payload) throws JSONException;
    }

    private class CreateOfferCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(TAG,"CreateOfferCommand");


            peer.pc.createOffer(peer, pcConstraints);
        }
    }

    private class CreateAnswerCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(TAG,"CreateAnswerCommand");

            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, pcConstraints);
        }
    }

    private class SetRemoteSDPCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.d(TAG,"SetRemoteSDPCommand");

            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
        }
    }

    private class AddIceCandidateCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(TAG,"AddIceCandidateCommand");
            PeerConnection pc = peer.pc;
            if (pc.getRemoteDescription() != null) {

                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                pc.addIceCandidate(candidate);
            }
        }
    }

    private class MessageHandler {
        private HashMap<String, Command> commandMap;
        private Emitter.Listener onId = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String id = (String) args[0];
                mListener.onCallReady(id);
            }
        };






        private MessageHandler() {
            this.commandMap = new HashMap<>();
            commandMap.put("init", new CreateOfferCommand());
            commandMap.put("offer", new CreateAnswerCommand());
            commandMap.put("answer", new SetRemoteSDPCommand());
            commandMap.put("candidate", new AddIceCandidateCommand());



        }
    }

    private class Peer implements SdpObserver, PeerConnection.Observer{
        private PeerConnection pc;
        private String id;
        private int endPoint;

        public Peer() {
            this.pc = factory.createPeerConnection(iceServers, pcConstraints, this);
            pc.addStream(localMS); //, new MediaConstraints()

            mListener.onStatusChanged("CONNECTING");
        }

        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            // TODO: modify sdp to use pcParams prefered codecs
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());
                payload.put("sdp", sdp.description);

                sendMessage(Global.ToTopic, sdp.type.canonicalForm(), payload);

                Log.i(TAG,"sdp.type.canonicalForm()  : "+ sdp.type.canonicalForm());

                pc.setLocalDescription(Peer.this, sdp);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i(TAG,"Sdp Send Fail");

            }
        }

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String s) {
            Log.i(TAG,"createFail "+s);

        }

        @Override
        public void onSetFailure(String s) {
            Log.i(TAG,"setFail "+s);
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            if(iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {

                mListener.onStatusChanged("DISCONNECTED");
            }
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("label", candidate.sdpMLineIndex); //int
                payload.put("id", candidate.sdpMid); //String
                payload.put("candidate", candidate.sdp); //String
                payload.put("type" , "candidate");

                sendMessage(id, "candidate", payload);

//                Log.i(TAG , "candidate : " + candidate.sdp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG,"onAddStream "+mediaStream.label());
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
            mListener.onAddRemoteStream(mediaStream, endPoint+1);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG,"onRemoveStream "+mediaStream.label());

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {}

        @Override
        public void onRenegotiationNeeded() {

        }
    }
}
