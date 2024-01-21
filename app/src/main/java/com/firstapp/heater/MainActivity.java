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
import android.widget.ImageView;
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

    boolean MqStatus =false;
    private ItemViewModel VmActuator;
    TextView hello,result;

    private static final String TAG = "AndroidCameraApi";
    private Button btnTake;
    private Button btnGallery;
    private TextureView textureView;

    private ImageView imageView;

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
    private String folderName = "MyPhotoDir";
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
        result = findViewById(R.id.TvResult);
//        textureView = findViewById(R.id.texture);
//        btnTake = findViewById(R.id.buttonA);
//        imageView = findViewById(R.id.imageView);

//        handler = new Handler(Looper.getMainLooper());
//        runnable = new Runnable() {
//            int counter = 0;
//
//            @Override
//            public void run() {
//                // Perform any desired actions here
//                Log.d("HandlerExample", "Counter: " + counter);
//                counter++;
//                if(bCamAvailable){
//                    takePicture();
//                }
//                // Schedule the next execution
//                handler.postDelayed(this, DELAY_MILLIS);
//            }
//        };

        if(OpenCVLoader.initDebug()){
//            btnTake.setText("CV_OK");
        }
        else{
//            btnTake.setText("CV_Failed");
        }
//        if(btnTake != null)
//            btnTake.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    takePicture();
//                }});
        if(textureView != null)
            textureView.setSurfaceTextureListener(textureListener);

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
                            hello.setText(payload);
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

        replaceFragment(new Fragment_temp(),R.id.frame_layout);
        replaceFragment(new Fragment_actuator(),R.id.frame_layout2);
        replaceFragment(new Fragment_camera(),R.id.frame_layout3);
        VmActuator = new ViewModelProvider(this).get(ItemViewModel.class);
        VmActuator.getLiveActuator().observe(this, item->{
            result.setText(item);
            Log.d("observer", "NotFail ");
            if(MqStatus){
                publishMessage(item, "/start", 1, MqClient);}
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

    private void replaceFragment(Fragment fragment , int layout_id){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(layout_id ,fragment);
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

//    private void openCamera() {
//        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        Log.e(TAG, "is camera open");
//        try {
//            String[] cameraIdslist = manager.getCameraIdList();
//
//            cameraId = manager.getCameraIdList()[0];
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
//            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            assert map != null;
//            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
//            // Add permission for camera and let user grant the permission
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
//                return;
//            }
//            manager.openCamera(cameraId, stateCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
//        @Override
//        public void onOpened(CameraDevice camera) {
//            // This is called when the camera is open
//            Log.d(TAG, "onOpened");
//            cameraDevice = camera;
//            bCamAvailable = true;
//
//        }
//
//        @Override
//        public void onDisconnected(CameraDevice camera) {
//            cameraDevice.close();
//            bCamAvailable = false;
//        }
//
//        @Override
//        public void onError(CameraDevice camera, int error) {
//            cameraDevice.close();
//            cameraDevice = null;
//            bCamAvailable = false;
//        }
//    };

//    protected void createCameraPreview() {
//        try {
//            SurfaceTexture texture = textureView.getSurfaceTexture();
//            assert texture != null;
//            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
//            Surface surface = new Surface(texture);
//            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureRequestBuilder.addTarget(surface);
//            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    //The camera is already closed
//                    if (null == cameraDevice) {
//                        return;
//                    }
//                    // When the session is ready, we start displaying the preview.
//                    cameraCaptureSessions = cameraCaptureSession;
//                    updatePreview();
//                }
//
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
//                }
//            }, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//    protected void updatePreview() {
//        if (null == cameraDevice) {
//            Log.e(TAG, "updatePreview error, return");
//        }
//        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        try {
//            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//    protected void takePicture() {
//        if (cameraDevice == null) {
//            Log.e(TAG, "cameraDevice is null");
//            return;
//        }
//        if (!isExternalStorageAvailableForRW() || isExternalStorageReadOnly()) {
//            btnTake.setEnabled(false);
//        }
//        if (isStoragePermissionGranted()) {
//            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//            try {
//                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
//                Size[] jpegSizes = null;
//                if (characteristics != null) {
//                    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
//                }
//                int width = 640;
//                int height = 480;
//                if (jpegSizes != null && jpegSizes.length > 0) {
//                    width = jpegSizes[0].getWidth();
//                    height = jpegSizes[0].getHeight();
//                }
//                ImageReader reader = ImageReader.newInstance(1600, 1200, ImageFormat.JPEG, 1);
//                List<Surface> outputSurfaces = new ArrayList<Surface>(2);
//                outputSurfaces.add(reader.getSurface());
//                outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
//                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//                captureBuilder.addTarget(reader.getSurface());
//                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_MACRO);
//                // Orientation
//                int rotation = getWindowManager().getDefaultDisplay().getRotation();
//                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation)-1);
//                file = null;
//                folder = new File(folderName);
//                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                String imageFileName = "IMG_" + timeStamp + ".jpg";
//                file = new File(getExternalFilesDir(folderName), "/" + imageFileName);
//                if (!folder.exists()) {
//                    folder.mkdirs();
//                }
//                ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
//                    @Override
//                    public void onImageAvailable(ImageReader reader) {
//                        Image image = null;
//                        try {
//                            image = reader.acquireLatestImage();
//
//                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                            byte[] bytes = new byte[buffer.capacity()];
//                            buffer.get(bytes);
//                            save(bytes);
//
//                            BitmapFactory.Options options = new BitmapFactory.Options();
//                            options.inSampleSize = 1; // Adjust this if needed
//                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//
//                            Mat cvImage = new Mat();
//                            Utils.bitmapToMat(bitmap,cvImage);
//                            Mat grayMat = new Mat();
//                            Imgproc.cvtColor(cvImage, grayMat, Imgproc.COLOR_BGR2GRAY);
//                            Bitmap bitmap_gray = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888);
//                            Utils.matToBitmap(grayMat, bitmap_gray);
//                            imageView.setImageBitmap(bitmap_gray);
//
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        } finally {
//                            if (image != null) {
//                                image.close();
//                            }
//                        }
//                        reader.close();
//                    }
//
//                    private void save(byte[] bytes) throws IOException {
//                        OutputStream output = null;
//                        try {
//                            output = new FileOutputStream(file);
//                            output.write(bytes);
//                        } finally {
//                            if (null != output) {
//                                output.close();
//                            }
//                        }
//                    }
//                };
//                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
//                final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
//                    @Override
//                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//                        super.onCaptureCompleted(session, request, result);
//                        Log.d(TAG, "" + file);
////                        createCameraPreview();
//                    }
//                };
//                cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
//                    @Override
//                    public void onConfigured(CameraCaptureSession session) {
//                        try {
//                            session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
//                        } catch (CameraAccessException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public void onConfigureFailed(CameraCaptureSession session) {
//                    }
//                }, mBackgroundHandler);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private static boolean isExternalStorageReadOnly() {
//        String extStorageState = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
//            return true;
//        }
//        return false;
//    }
//
//    private boolean isExternalStorageAvailableForRW() {
//        // Check if the external storage is available for read and write by calling
//        // Environment.getExternalStorageState() method. If the returned state is MEDIA_MOUNTED,
//        // then you can read and write files. So, return true in that case, otherwise, false.
//        String extStorageState = Environment.getExternalStorageState();
//        if (extStorageState.equals(Environment.MEDIA_MOUNTED)) {
//            return true;
//        }
//        return false;
//    }
//
//    private boolean isStoragePermissionGranted() {
//        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                == PackageManager.PERMISSION_GRANTED) {
//            // Permission is granted
//            return true;
//        } else {
//            //Permission is revoked
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//            return false;
//        }
//    }
//    private void captureImagesContinuously() {
//        try {
//            // Create an ImageReader with desired configuration
//            ImageReader imageReader = ImageReader.newInstance(400, 300, ImageFormat.JPEG, 1);
//
//            // Set the OnImageAvailableListener to receive captured images
//            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
//                @Override
//                public void onImageAvailable(ImageReader reader) {
//                    // Retrieve the latest image from the ImageReader
//                    Image image = reader.acquireLatestImage();
//
//                    // Process the captured image using OpenCV
//                    Mat imageMat = imageToMat(image);
//
//                    // Perform your OpenCV image processing operations on 'imageMat' here
//                    // For example, you can apply filters, perform object detection, etc.
//
//                    // Convert the processed image back to an Image object
//                    Image processedImage = matToImage(imageMat);
//
//                    // Save or use the processed image as needed
//                    // ...
//
//                    // Close the captured image to release resources
//                    image.close();
//                }
//            }, null);
//
//            // Get the camera manager and the ID of the desired camera
//            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//            String cameraId = cameraManager.getCameraIdList()[0]; // Use the first available camera
//            // Open the camera device
//            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
//                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
//                    @Override
//                    public void onOpened(CameraDevice camera) {
//                        // Create a capture request
//                        try {
//                            CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//                            captureRequestBuilder.addTarget(imageReader.getSurface());
//
//                            // Continuously capture images
//                            camera.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
//                                @Override
//                                public void onConfigured(CameraCaptureSession session) {
//                                    try {
//                                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
//                                    } catch (CameraAccessException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//
//                                @Override
//                                public void onConfigureFailed(CameraCaptureSession session) {
//                                    // Handle configuration failure
//                                }
//                            }, null);
//                        } catch (CameraAccessException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public void onDisconnected(CameraDevice camera) {
//                        // Handle camera disconnection
//                    }
//
//                    @Override
//                    public void onError(CameraDevice camera, int error) {
//                        // Handle camera device error
//                    }
//                }, null);
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }


//    private Image matToImage(Mat mat) {
//        int width = mat.cols();
//        int height = mat.rows();
//        int bufferSize = width * height * (int) mat.elemSize();
//        byte[] data = new byte[bufferSize];
//        mat.get(0, 0, data);
//        ByteBuffer buffer = ByteBuffer.wrap(data);
//        Image.Plane[] planes = new Image.Plane[1];
//        Image image = new Image();
//        return image;
////        planes[0] = new Image.Plane(buffer, width, height, (int) mat.elemSize(), width * (int) mat.elemSize());
////        return ImageUtils.newInstance(width, height, ImageFormat.YUV_420_888, planes);
//    }

}