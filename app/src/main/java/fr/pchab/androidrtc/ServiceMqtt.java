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

import fr.pchab.webrtcclient.WebRtcClient;

public class ServiceMqtt extends Service  implements MqttCallback {

    private static final String TAG = "seok";
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


    public void setListener(MqttLIstener listener){
        mListener = listener;
    }

    //getmessage 콜백
    public interface MqttLIstener {

        void getMessage(String msg);

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

        Log.i(TAG,"Service onStart");
        String sub = intent.getStringExtra("subtopic");
        connectMQTT(sub);


        return super.onStartCommand(intent, flags, startId);
    }

    private void connectMQTT(String subtopic) {
        broker       = "tcp://61.38.158.169:1883";
        clientId     = getDeviceSerialNumber();
        int qos  = 2;

        // Log.i(TAG,"클라이언트 아이디 " +clientId);

        try {
            sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            sampleClient.connect(connOpts);

            sampleClient.setCallback(this);

            /******** client에 콜백을 Set ********/

            sampleClient.subscribe(subtopic,qos);

            Log.i(TAG,"Client Connected " + sampleClient.isConnected());

        } catch(MqttException me) {
            Log.i(TAG,"reason "+me.getReasonCode());
            Log.i(TAG,"msg "+me.getMessage());
            Log.i(TAG,"loc "+me.getLocalizedMessage());
            Log.i(TAG,"cause "+me.getCause());
            Log.i(TAG,"excep"+me);

            me.printStackTrace();
        }
    }



    public void publish(String topicto,String message){
/*
        String topicto = topic_To.getText().toString();
        String message = send_message.getText().toString();
*/

        int qos = 2;


        try{
            Log.i(TAG,"Publishing message: "+ message);

            sampleClient.publish(topicto,message.getBytes(),qos,true);

            Log.i(TAG,"Message published");






        }catch(MqttException me) {
            Log.i(TAG,"reason "+me.getReasonCode());
            Log.i(TAG,"msg "+me.getMessage());
            Log.i(TAG,"loc "+me.getLocalizedMessage());
            Log.i(TAG,"cause "+me.getCause());
            Log.i(TAG,"excep"+me);

            me.printStackTrace();
        }

    }



    private void disconnectMQTT(){
        try {
            sampleClient.disconnect();
            Log.i(TAG, "Disconnected");

        }catch(MqttException me) {
            Log.i(TAG,"reason "+me.getReasonCode());
            Log.i(TAG,"msg "+me.getMessage());
            Log.i(TAG,"loc "+me.getLocalizedMessage());
            Log.i(TAG,"cause "+me.getCause());
            Log.i(TAG,"excep"+me);

            me.printStackTrace();
        }
    }


    @Override
    public void connectionLost(Throwable cause) {
        Log.i(TAG,cause.getLocalizedMessage());

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        /*Log.i(TAG,"Arrived topic  " + topic);
        Log.i(TAG,"Arrived message  " + message.toString());
        Log.i(TAG,"Arrived payload  " + message.getPayload());*/

        mListener.getMessage(message.toString());

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

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


}
