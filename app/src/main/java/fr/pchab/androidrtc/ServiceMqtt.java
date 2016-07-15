package fr.pchab.androidrtc;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import fr.pchab.androidrtc.base.Global;

public class ServiceMqtt extends Service  implements MqttCallback {

    private static ServiceMqtt mInstance;
    public MqttLIstener mListener;



    MqttClient sampleClient;

    String broker;
    String clientId;

    MemoryPersistence persistence = new MemoryPersistence();


    //싱글톤
    public static ServiceMqtt getInstance(){
        if(mInstance!=null)
            return mInstance;
        else {
            return new ServiceMqtt();
        }
    }

    //디바이스 고유의 Serial Number 얻어오는 메서드
    @SuppressLint("NewApi")
    private static String getDeviceSerialNumber() {
        try {
            return (String) Build.class.getField("SERIAL").get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setListener(MqttLIstener listener){
        mListener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //broker 와 connection set
        mInstance = this;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        connectMQTT();


        return super.onStartCommand(intent, flags, startId);
    }

    private void connectMQTT() {
        broker       = "tcp://61.38.158.169:1883";
        clientId     = getDeviceSerialNumber();
        int qos  = 1;

        // Log.i(Global.TAG,"클라이언트 아이디 " +clientId);

        try {
            sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            sampleClient.connect(connOpts);

            sampleClient.setCallback(this);

            Log.i(Global.TAG,"Mqtt Connect : " +sampleClient.isConnected());

            /******** client에 콜백을 Set ********/

            sampleClient.subscribe(Global.Mytopic,qos);

//            Log.i(Global.TAG,"Client Connected " + sampleClient.isConnected());

        } catch(MqttException me) {
            Log.i(Global.TAG,"reason "+me.getReasonCode());
            Log.i(Global.TAG,"msg "+me.getMessage());
            Log.i(Global.TAG,"loc "+me.getLocalizedMessage());
            Log.i(Global.TAG,"cause "+me.getCause());
            Log.i(Global.TAG,"excep"+me);

            me.printStackTrace();
        }
    }



    public void publish(String topicto,String message){
/*
        String topicto = topic_To.getText().toString();
        String message = send_message.getText().toString();
*/

//        int qos = 1;


        try{
            Log.i(Global.TAG,"Publishing message: "+ message);

//            sampleClient.publish(Global.ToTopic,message.getBytes(),qos,false);

            MqttMessage message1= new MqttMessage(message.getBytes());
            sampleClient.getTopic(Global.ToTopic).publish(message1);







        }catch(MqttException me) {
            Log.i(Global.TAG,"reason "+me.getReasonCode());
            Log.i(Global.TAG,"msg "+me.getMessage());
            Log.i(Global.TAG,"loc "+me.getLocalizedMessage());
            Log.i(Global.TAG,"cause "+me.getCause());
            Log.i(Global.TAG,"excep"+me);

            me.printStackTrace();
        }

    }



    private void disconnectMQTT(){
        try {
            sampleClient.disconnect();
            Log.i(Global.TAG, "Disconnected");

        }catch(MqttException me) {
            Log.i(Global.TAG,"reason "+me.getReasonCode());
            Log.i(Global.TAG,"msg "+me.getMessage());
            Log.i(Global.TAG,"loc "+me.getLocalizedMessage());
            Log.i(Global.TAG,"cause "+me.getCause());
            Log.i(Global.TAG,"excep"+me);

            me.printStackTrace();
        }
    }


    @Override
    public void connectionLost(Throwable cause) {
        Log.i(Global.TAG,cause.getLocalizedMessage());

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.i(Global.TAG,"Arrived Topic  " + topic);
        Log.i(Global.TAG,"Arrived Message  " + message.toString());
        if(WebRtcClient.getmInstance()!=null)
            WebRtcClient.getmInstance().getMessage(message.toString());
//        mListener.getMessage(message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }


    //getmessage 콜백
    public interface MqttLIstener {

        void getMessage(String msg);

    }












}
