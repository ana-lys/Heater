package com.firstapp.heater;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.BitmapFactory;

import java.io.UnsupportedEncodingException;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    MqttAndroidClient MqClient;
    String MqClientID;
    MqttConnectOptions MqttOption;
    static String MqHost = "tcp://mustang.rmq.cloudamqp.com:1883";
    static String MqUser = "ggyfdoic:ggyfdoic";
    static String MqPassword = "22AeiYRN7FGHZqXUn5Hlz1Ga5Kg-qfby";

    boolean MqStatus = false;
    private ItemViewModel VmActuator;
    TextView hello, result;

    private static final String TAG = "AndroidCameraApi";


    private Switch switchWidget;


    private static final long DELAY_MILLIS = 200; // 1 second
    private Handler handler;
    private Runnable runnable;

    boolean bCamAvailable;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private File folder;
    private String folderName = "Video";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onResume() {
        super.onResume();
//        handler.postDelayed(runnable, DELAY_MILLIS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hello = findViewById(R.id.TvHello);
        hello.setText(" ");
        result = findViewById(R.id.TvResult);
        result.setText(" ");
        if (OpenCVLoader.initDebug()) {

        } else {

        }
        switchWidget = findViewById(R.id.switch1);

        MqClientID = MqttClient.generateClientId();
        MqClient = new MqttAndroidClient(this.getApplicationContext(), MqHost, MqClientID);
        MqttOption = new MqttConnectOptions();
        MqttOption.setUserName(MqUser);
        MqttOption.setPassword(MqPassword.toCharArray());
        try {
            IMqttToken MqToken = MqClient.connect(MqttOption);
            MqToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();

                    // Set up callback to handle incoming messages
                    MqClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            // Handle loss of connection
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            // Handle an incoming message. Convert the byte array to a string and display it.
                            String payload = new String(message.getPayload());
                            VmActuator.setLiveTemperature(payload);
//                            hello.setText(payload);
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            // Handle completed delivery of a message
                        }
                    });

                    // Subscribe to a topic if needed
                    try {
                        MqClient.subscribe("/temp", 1); // QoS is set to 1
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }

                    // Since we've successfully connected, now we can publish a message.
                    MqStatus = true;
                    publishMessage("Android_suck", "/hello", 1, MqClient);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                    Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        replaceFragment(new Fragment_temp(), R.id.frame_layout);
        replaceFragment(new Fragment_actuator(), R.id.frame_layout2);
        replaceFragment(new Fragment_camera(), R.id.frame_layout3);
        switchWidget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Perform actions based on the switch state
                if (isChecked) {
                    replaceFragment(new Fragment_history(), R.id.frame_layout2);
                    replaceFragment(new Fragment_preview(), R.id.frame_layout3);
                } else {
                    replaceFragment(new Fragment_actuator(), R.id.frame_layout2);
                    replaceFragment(new Fragment_camera(), R.id.frame_layout3);
                }
            }
        });



        VmActuator = new ViewModelProvider(this).get(ItemViewModel.class);
        VmActuator.getLiveActuator().observe(this, item -> {
            result.setText(item);
            if (MqStatus) {
                publishMessage(item, "/start", 1, MqClient);
            }
        });
        VmActuator.getLiveVideoPath().observe(this, item -> {
//            result.setText(item);
        });

    }

    private void publishMessage(String payload, String topic, int qos, MqttAndroidClient client) {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setQos(qos);
            client.publish(topic, message);
        } catch (MqttException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void replaceFragment(Fragment fragment, int layout_id) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(layout_id, fragment);
        fragmentTransaction.commit();
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Open your camera here
//            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

}