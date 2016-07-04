package fr.pchab.androidrtc;

import android.opengl.EGLContext;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;

import java.util.HashMap;
import java.util.LinkedList;

public class WebRtcClient {
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

    public WebRtcClient(RtcListener listener, String host, PeerConnectionParameters params, EGLContext mEGLcontext) {
        mInstance = this;
        mListener = listener;
        pcParams = params;


        PeerConnectionFactory.initializeAndroidGlobals(listener, true, true,
                params.videoCodecHwAcceleration, mEGLcontext);
        factory = new PeerConnectionFactory();


//****        sampleClient -> messageHandler.onMessage
//****        sampleClient -> messageHandler.onId



//        try {
//            client = IO.socket(host);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//        client.on("id", messageHandler.onId);
//        client.on("message", messageHandler.onMessage);
//        client.connect();`
//        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));


        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

//        start("test");

    }


    public static WebRtcClient getmInstance(){
        if(mInstance!=null) {
//            Log.i(Global.TAG , "NOT NULL");
            return mInstance;
        }else {
//            Log.i(Global.TAG , "NULL");
            return null;
        }
    }

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        ServiceMqtt.getInstance().publish(Global.ToTopic,payload.toString());

//        Log.d(Global.TAG,payload.toString());
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
    }

    public void start(String name){
        setCamera();


    }

    public void getMessage(String msg){
        JSONObject json = null;


        try {
            json = new JSONObject(msg);


            if(json!=null){

                Log.i(Global.TAG , "json.Type : " + json.getString("type"));


                if(!json.isNull("type") && json.getString("type").equalsIgnoreCase("offer")){
                    //CALLEE
//                Log.i(Global.TAG , "OFFER : " + json.getString("sdp"));
                    Global.ToTopic = json.getString("answerTopic");
//                Log.i(Global.TAG, "제발 " + Global.ToTopic);
                    new CreateAnswerCommand().execute(json);

                }else if(!json.isNull("type") && json.getString("type").equalsIgnoreCase("answer")){
                    //CALLER
                    new SetRemoteSDPCommand().execute(json);
//                Log.i(Global.TAG , "answer : " + json.getString("sdp"));

                }else if (!json.isNull("type") && json.getString("type").equalsIgnoreCase("candidate")){
//                Log.i(Global.TAG , "candidate : " + json.getString("candidate"));
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
            Log.i(Global.TAG,"CreateOfferCommand");

            peer.pc.createOffer(peer, pcConstraints);
        }
    }

    private class CreateAnswerCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(Global.TAG,"CreateAnswerCommand");

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
            Log.d(Global.TAG,"SetRemoteSDPCommand");

            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
        }
    }

    private class AddIceCandidateCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(Global.TAG,"AddIceCandidateCommand");
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

                //answer 보낼때 Global.ToTopic
                if(!sdp.type.canonicalForm().equalsIgnoreCase("answer")){
                    payload.put("answerTopic",Global.Mytopic);
                }

                sendMessage(Global.ToTopic, sdp.type.canonicalForm(), payload);
                Log.i(Global.TAG,"sdp.type.canonicalForm()  : "+ sdp.type.canonicalForm());

                pc.setLocalDescription(Peer.this, sdp);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i(Global.TAG,"Sdp Send Fail");

            }
        }

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String s) {
            Log.i(Global.TAG,"createFail "+s);

        }

        @Override
        public void onSetFailure(String s) {
            Log.i(Global.TAG,"setFail "+s);
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

//                Log.i(Global.TAG , "candidate : " + candidate.sdp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(Global.TAG,"onAddStream "+mediaStream.label());
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
            mListener.onAddRemoteStream(mediaStream, endPoint+1);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(Global.TAG,"onRemoveStream "+mediaStream.label());

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {}

        @Override
        public void onRenegotiationNeeded() {

        }
    }


}
