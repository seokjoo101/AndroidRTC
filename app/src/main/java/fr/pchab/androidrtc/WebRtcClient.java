package fr.pchab.androidrtc;

import android.app.Service;
import android.content.ContentResolver;
import android.util.Log;
import android.widget.VideoView;

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
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

import java.util.LinkedList;

import fr.pchab.androidrtc.base.Global;

//compile 'io.pristine:libjingle:11139@aar'
//11139
public class WebRtcClient  {
    private static WebRtcClient mInstance;
    Peer peer;

    DataChannel dataChannel;


    /**
     * Send a message through the signaling server
     *
     * @param to id of recipient
     * @param type type of message
     * @param payload payload of message
     * @throws JSONException
     */

    /*Caller
    Offer to - 상대방전화번호 ,"offer", sdp.description
    Candidate to - 상대방전화번호 ,"candidate ", candidate*/

    /*Callee
    Answer to - 상대방전화번호 , "answer" , sdp.description
    Candidate to - 상대방전화번호 ,"candidate ", candidate

    */

    JSONObject message = new JSONObject();
    private PeerConnectionFactory factory;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private MediaConstraints pcConstraints = new MediaConstraints();
    private RtcListener mListener;


    public WebRtcClient(RtcListener listener ) {
        mInstance = this;
        mListener = listener;

        PeerConnectionFactory.initializeAndroidGlobals(listener, true, true,
                true);
        factory = new PeerConnectionFactory();


        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

    }


    public static WebRtcClient getmInstance(){
        if(mInstance!=null) {
            return mInstance;
        }else {
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
     * Call this method in Activity.onDestroy()
     */

    public void onDestroy() {

        peer.pc.dispose();
         factory.dispose();
    }


    public void start(){

        peer = new Peer();

    }


    synchronized public void getMessage(String msg){
        JSONObject json = null;

        Log.i(Global.TAG, "getMessage: " + peer.pc.iceConnectionState());
        try {
            json = new JSONObject(msg);

            if(json!=null){
                if(!json.isNull("type") && json.getString("type").equalsIgnoreCase("offer")){
                    //CALLEE
                    Global.ToTopic = json.getString("answerTopic");
                    Log.i(Global.TAG , "Totopic : " + Global.ToTopic);
                    new CreateAnswerCommand().execute(json);

                }else if(!json.isNull("type") && json.getString("type").equalsIgnoreCase("answer")){
                    //CALLER
                    new SetRemoteSDPCommand().execute(json);

                 }else if (!json.isNull("type") && json.getString("type").equalsIgnoreCase("candidate")){
                    //CALLEE , CALLER
                    new AddIceCandidateCommand().execute(json);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /**
     * Implement this interface to be notified of events.
     */
    public interface RtcListener{

        void onStatusChanged(String newStatus);

        void onRemoveRemoteStream();

    }

    private interface Command{
        void execute( JSONObject payload) throws JSONException;
    }

    private class CreateOfferCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(Global.TAG, "CreateOfferCommand");


            //Caller 다시 전화걸때 재연결
            if (peer.pc.iceConnectionState() == PeerConnection.IceConnectionState.CLOSED){

                reconnect();

            }
            peer.pc.createOffer(peer, pcConstraints);

        }
    }


    private class CreateAnswerCommand implements Command{
        public void execute( JSONObject payload) throws JSONException {
            Log.i(Global.TAG,"CreateAnswerCommand");

            //Calle 다시 전화걸때 재연결
            if(peer.pc.iceConnectionState() == PeerConnection.IceConnectionState.CLOSED) {
                reconnect();
            }

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
            Log.i(Global.TAG,"1 AddIceCandidateCommand");

            PeerConnection pc = peer.pc;

            Log.i(Global.TAG, "2 pc getRemoteDescription : " +pc.getRemoteDescription().toString());


            if (pc.getRemoteDescription() != null) {
                Log.i(Global.TAG,"pc.getRemoteDescription is Not Null");

                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );

                pc.addIceCandidate(candidate);

            }else
                Log.i(Global.TAG,"pc.getRemoteDescription is Null");
        }


    }



    private class Peer implements SdpObserver, PeerConnection.Observer  {
        private PeerConnection pc;
        private String id;

        public Peer() {
            this.pc = factory.createPeerConnection(iceServers, pcConstraints, this);

            // DataChannel 의 Label 과 init 객체의 id가 같아야 한다
            mListener.onStatusChanged("Mqtt connect : " + ServiceMqtt.getInstance().sampleClient.isConnected());

            DataChannel.Init da = new DataChannel.Init();
            da.id = 1;
//            da.ordered=false;
            dataChannel = this.pc.createDataChannel("1",da);
            dataChannel.registerObserver(ScreenDecoder.getInstance());

            Log.i(Global.TAG," ordered : " + da.ordered); // true
            Log.i(Global.TAG," negotiated : " + da.negotiated); //false
            Log.i(Global.TAG," protocol : " + da.protocol); // null
            Log.i(Global.TAG," maxRetransmits : " + da.maxRetransmits); //-1
            Log.i(Global.TAG," maxRetransmitTimeMs : " + da.maxRetransmitTimeMs); //-1
        }

        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            // TODO: modify sdp to use pcParams prefered codecs
            try {
                JSONObject payload = new JSONObject();
                payload.put("type", sdp.type.canonicalForm());payload.put("sdp", sdp.description);


                if(sdp.type.canonicalForm().equalsIgnoreCase("offer")){
                    payload.put("answerTopic",Global.Mytopic);
                }



                //OFFER 혹은 ANSWER 성공적으로 만들어 졌을 때
                Log.i(Global.TAG,"sdp.type.canonicalForm()  : "+ sdp.type.canonicalForm());
                sendMessage(Global.ToTopic, sdp.type.canonicalForm(), payload);

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
            Log.d(Global.TAG,"onIceConnectionChange :" + iceConnectionState);

            if(iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                removeConnection();
                mListener.onStatusChanged("통화 종료");
            }else if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED){
                //WebRTC peerconnection 성공했을 때
                mListener.onStatusChanged("연결 성공");

            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }


        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            //OFFER나 ANSWER가 만들어질때
            try {
                JSONObject payload = new JSONObject();
                payload.put("label", candidate.sdpMLineIndex); //int
                payload.put("id", candidate.sdpMid); //String
                payload.put("candidate", candidate.sdp); //String
                payload.put("type" , "candidate");

                sendMessage(id, "candidate", payload);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(Global.TAG,"onAddStream "+mediaStream.label());
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
//             mListener.onAddRemoteStream(mediaStream);

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(Global.TAG,"onRemoveStream "+mediaStream.label());
            mListener.onRemoveRemoteStream();
            peer.pc.removeStream(mediaStream);
         }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(Global.TAG,"dataChannel : " + dataChannel.state());
        }

        @Override
        public void onRenegotiationNeeded() {

        }


    }

    private void reconnect() {
        peer.pc = factory.createPeerConnection(iceServers, pcConstraints, peer);


        DataChannel.Init da = new DataChannel.Init();
        da.id = 1;
        dataChannel = peer.pc.createDataChannel("1",da);
        dataChannel.registerObserver(ScreenDecoder.getInstance());

    }

    public void removeConnection() {
        peer.pc.close();
        mListener.onRemoveRemoteStream();
        mListener.onStatusChanged("통화 종료");


    }

    void reCall(){
        peer.pc = factory.createPeerConnection(iceServers, pcConstraints, peer);
    }

    public void getStat(){

        peer.pc.getStats(new StatsObserver() {
            @Override
            public void onComplete(StatsReport[] statsReports) {

                for(StatsReport stats : statsReports) {
                    for (StatsReport.Value  s : stats.values){
                        Log.i(Global.TAG,"stats : "+ s.name + ", value : " + s.value );

                    }
                }
                Log.i(Global.TAG,"--------------------------------------------------------------------------------------------------------------------------------------------" );

            }
        },null);

    }

}
