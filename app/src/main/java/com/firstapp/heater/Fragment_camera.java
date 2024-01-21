/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firstapp.heater;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import static org.opencv.core.CvType.CV_8UC4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Fragment_camera extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    protected boolean mIsMultiTouch = false;
    protected float mFingerSpacing = 0;
    protected float mThumbFingerSize = 0; // TODO: as option
    protected float mZoomLevel = 1f; // TODO: as option
    protected float mMaximumZoomLevel; // TODO: as option
    protected float mMinimumZoomLevel; // TODO: as option

    private CameraCharacteristics mCameraCharacteristics;

    protected android.graphics.Rect mZoom;

    ImageView imageView,stencil;
    Button focus,zoomLock;

    Boolean bZoomLock = false,bFocus = false,bSkew = false;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Fragment_camera";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        public ArrayList<Mat> CvResult = new ArrayList<Mat>();
        public ArrayList<Float> Temp = new ArrayList<Float>();

        public ArrayList<Mat> CvSkewd = new ArrayList<Mat>();

        int fourCC = VideoWriter.fourcc('M', 'J','P','G');
        int api_ref = Videoio.CAP_OPENCV_MJPEG;
//        Videoio.CAP_OPENCV_MJPEG
        VideoWriter videoWriter ;

        int frameCount= 0 , w = 0 , h = 0;

        long lastTime = System.currentTimeMillis();

        boolean start = true ;

        private boolean isStoragePermissionGranted() {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                return true;
            }
        }

        public void startCV() throws IOException {
            lastTime = System.currentTimeMillis();

            Bitmap originalBitmap = mTextureView.getBitmap();

            w = originalBitmap.getWidth();
            h = originalBitmap.getHeight();

            org.opencv.core.Size frameSize = new org.opencv.core.Size( w,h );

            if(isStoragePermissionGranted()){

                String folderName = "video";
                File folder = new File(folderName);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "Vid" + timeStamp + ".avi";
                File file = new File(getContext().getExternalFilesDir(folderName), "/" + imageFileName);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
//                if(!file.exists()){
//                    file.createNewFile();
//                }
//
                Log.d(TAG, "startCV: file exist "+file.getAbsolutePath()+" "+file.exists());

                videoWriter = new VideoWriter();
                videoWriter.open(file.getAbsolutePath(), api_ref , fourCC, 60, frameSize,true);
                if (!videoWriter.isOpened())
                {
                Log.e(TAG, "startCV: videowriteer failed");
                }
                CvResult.clear();

                Temp.clear();

                CvSkewd.clear();

                start = false;
            }

        }

        public void endCV(){
            if(!start) {
                Log.d(TAG, "onSurfaceTextureUpdated: release");
                videoWriter.release();
                start = true;
            }
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        public Float getFps(){

            // Check if one second has passed
            long currentTime = System.currentTimeMillis(); // or System.nanoTime();
            long elapsedTime = currentTime - lastTime;
            lastTime = currentTime;
            // Calculate and display the FPS
            Float fps = 1000.f / elapsedTime ;
            Log.d(TAG, "getFps: "+fps);
            return fps;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            Bitmap originalBitmap = mTextureView.getBitmap();
            Bitmap finalbitmap = originalBitmap;

            if(bSkew) {

                if(start){
                    try {
                        startCV();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

//                getFps();

                Mat originalMat = new Mat();
                Utils.bitmapToMat(originalBitmap, originalMat);

                // Define source and destination points for perspective transformation
                org.opencv.core.Point[] srcPoints = new org.opencv.core.Point[4];
                for (int i = 0; i < 4; i++) {
                    srcPoints[i] = new org.opencv.core.Point(apFocusPoints.get(i).x, apFocusPoints.get(i).y);
                }
                org.opencv.core.Point[] dstPoints = {new org.opencv.core.Point(0, h), new org.opencv.core.Point(0, 0), new org.opencv.core.Point(w, 0), new org.opencv.core.Point(w, h)};

                // Create a perspective transformation matrix
                Mat perspectiveMatrix = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcPoints), new MatOfPoint2f(dstPoints));

                // Apply the perspective transformation
                Mat warpedMat = new Mat() ,warpedMatGray = new Mat();
                Imgproc.warpPerspective(originalMat, warpedMat, perspectiveMatrix, new org.opencv.core.Size(w, h));

                ArrayList<Mat> SC_mat = new ArrayList<Mat>(3);
                Mat[] der_mat = new Mat[3];
                
                warpedMat.convertTo(warpedMatGray, CvType.CV_32FC3);
                Core.split(warpedMatGray, SC_mat);

                Mat sumMat = new Mat();
                Core.add( SC_mat.get(0), SC_mat.get(1),sumMat);
                Core.add( SC_mat.get(2), sumMat, sumMat );
                Core.multiply(sumMat,new Scalar(0.33),sumMat);
                Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(sumMat);
                double minValue = minMaxResult.minVal;
                double maxValue = minMaxResult.maxVal;
                double scale = 255.0 / (maxValue - minValue);
                double shift = -minValue * scale;
                Core.convertScaleAbs(sumMat, sumMat, scale, shift);
                finalbitmap = Bitmap.createBitmap(sumMat.cols(), sumMat.rows(), Bitmap.Config.ARGB_8888);
                sumMat.convertTo(sumMat, CvType.CV_32F);
                CvResult.add(sumMat);
                if (CvResult.size() > 20)
                {   Mat DiffSum = new Mat();
                    Mat diff1 = new Mat() , diff5 = new Mat() ,diff20 =new Mat();
                    Core.subtract(CvResult.get(CvResult.size()-1),sumMat,diff1);
                    Core.subtract(CvResult.get(CvResult.size()-5),sumMat,diff5);
                    Core.subtract(CvResult.get(CvResult.size()-20),sumMat,diff20);
                    Core.add(diff1,diff5,DiffSum);
                    Core.add(DiffSum,diff20,DiffSum);
                    Mat positiveMask = new Mat(),negativeMask = new Mat();
                    Core.compare(DiffSum, new Scalar(150), positiveMask, Core.CMP_GT);
                    Core.compare(DiffSum, new Scalar(-100), negativeMask, Core.CMP_LT);

                    int positiveCount = Core.countNonZero(positiveMask);
                    int negativeCount = Core.countNonZero(negativeMask);

                    warpedMat.setTo(new Scalar(255, 0, 0, 255), positiveMask);
                    warpedMat.setTo(new Scalar(0, 0, 255, 255), negativeMask);
//                    Mat outputImage = new Mat(h, w*2,CV_8UC4);
//
//                    Mat leftRegion = outputImage.colRange(0, w);
//                    originalMat.copyTo(leftRegion);
//                    Mat rightRegion = outputImage.colRange(w , w*2);
//                    warpedMat.copyTo(rightRegion);
                    if(positiveCount!=0 && negativeCount!=0){
                        Log.d(TAG, "count "+positiveCount+" "+negativeCount);
                        videoWriter.write(warpedMat);
                    }
                    CvResult.remove(0);
                }
                else{
                }

                Utils.matToBitmap(warpedMat, finalbitmap);
            }
            else{
                endCV();
            }
            imageView.setImageBitmap(finalbitmap);
            }


    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    private TextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    private Handler trackingHandler;

    private boolean bTracking = false;
    private PointF pTrackingCenter,pTrackingOffset;

    ArrayList<Point> apFocusPoints;
    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;
    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailable: ");
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
            }
        }




        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void startTrackingThread() {

        bTracking = true;
        Log.d(TAG, "startTrackingThread: ");
        trackingHandler = new Handler();
        trackingHandler.post(new Runnable() {
            @Override
            public void run() {
//                Log.d(TAG, "run: thread ");
                trackingHandler.postDelayed(this, 100); // Adjust the interval as needed
            }
        });
    }

    // Stop the tracking thread
    private void stopTrackingThread() {
        bTracking = false;
        Log.d(TAG, "stopTrackingThread: ");
        if (trackingHandler != null) {
            trackingHandler.removeCallbacksAndMessages(null);
            trackingHandler = null;
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            Log.d(TAG, "bigEnough.size() > 0: "+Collections.min(bigEnough, new CompareSizesByArea()));
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            Log.d(TAG,"notBigEnough.size() > 0"+Collections.max(notBigEnough, new CompareSizesByArea()));
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Fragment_camera newInstance() {
        return new Fragment_camera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
//        view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView =  view.findViewById(R.id.texture);
        imageView = view.findViewById(R.id.image);
        stencil = view.findViewById(R.id.stencil);
        focus = view.findViewById(R.id.buttonFocus);
        zoomLock = view.findViewById(R.id.buttonLockZoom);
        pTrackingOffset = new PointF(0,0);
        focus.setEnabled(false);
        focus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apFocusPoints = new ArrayList<Point>();
                bFocus = true;
                bSkew = false;
            }
        });
        zoomLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bZoomLock = !bZoomLock;
                if(bZoomLock) {
                    zoomLock.setText("Zoom Locked");
                    focus.setEnabled(true);
                }
                else{
                    focus.setEnabled(false);
                    zoomLock.setText("Zoom Not Locked");
                }
            }
        });
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            float mFingerSpacing = 0;
            boolean mIsMultiTouch = false;
            float mZoomLevel = 1f;
            android.graphics.Rect mZoom;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    android.graphics.Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    if (rect == null) {
                        return false;
                    }
                    int action = event.getAction();
//                    Log.d(TAG, "onTouch: Focus trigger"+bFocus);

                    if((event.getPointerCount() == 1)&& bFocus && action == MotionEvent.ACTION_UP){

                        apFocusPoints.add(new Point((int)event.getX(0),(int)event.getY(0)));
                        Log.d(TAG, "Focus point  "+ apFocusPoints);
                        if(apFocusPoints.size()==4){
                            drawPointsOnImageView(stencil);
                            bFocus = false;
                            bSkew = true;
                        }
                    }


                    if ((action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) && mIsMultiTouch) {
                        stopTrackingThread();
                        mIsMultiTouch = false;
                        return false;
                    }
                    if (event.getPointerCount() == 2 && !bZoomLock) {

                        float currentFingerSpacing = getFingerSpacing(event);
                        PointF currentFingerCenter = getFingerCenter(event);
                        float delta = 0.075f;
                        Log.d(TAG, "zom level"+mZoomLevel);
                        if (mFingerSpacing != 0) {

                            if (currentFingerSpacing > ( mFingerSpacing + 5.0f )) {
                                if ((mMaximumZoomLevel - mZoomLevel) <= delta) {
                                    delta = mMaximumZoomLevel - mZoomLevel;
                                }
                                mZoomLevel = mZoomLevel + delta;
                                mFingerSpacing = currentFingerSpacing;
                            } else if (currentFingerSpacing < ( mFingerSpacing -5.0f)) {
                                if ((mZoomLevel - delta) < 1f) {
                                    delta = mZoomLevel - 1f;
                                }
                                mZoomLevel = mZoomLevel - delta;
                                mFingerSpacing = currentFingerSpacing;
                            }
                            PointF differential = new PointF(0,0);
                            if(bTracking){
                                differential = new PointF(currentFingerCenter.x - pTrackingCenter.x,currentFingerCenter.y-pTrackingCenter.y);
                                if(differential.length()>1.0){
                                    pTrackingOffset.offset(-differential.y/mZoomLevel*10,differential.x/mZoomLevel*10);
                                }
                            }
                            float ratio = 1f / mZoomLevel;

                            int maxOffsetX = Math.round((float) rect.width() * (1 - ratio)) / 2;
                            int maxOffsetY = Math.round((float) rect.height() * (1 - ratio)) / 2;
                            int offsetX =  Math.max(-maxOffsetX, Math.min(maxOffsetX, (int)pTrackingOffset.x));
                            int offsetY =  Math.max(-maxOffsetY, Math.min(maxOffsetY, (int)pTrackingOffset.y));
                            pTrackingOffset = new PointF(offsetX,offsetY);

                            int croppedWidth = rect.width() - Math.round((float) rect.width() * ratio);
                            int croppedHeight = rect.height() - Math.round((float) rect.height() * ratio);

                            mZoom = new android.graphics.Rect(croppedWidth / 2 + (int)pTrackingOffset.x, croppedHeight / 2 +(int)pTrackingOffset.y,
                                    rect.width() - croppedWidth / 2+(int)pTrackingOffset.x, rect.height() - croppedHeight / 2+(int)pTrackingOffset.y);
                            MeteringRectangle focusAreaTouch = new MeteringRectangle(rect.width()*4/5,
                                    rect.height()*4/5,
                                    300,
                                    300,
                                    MeteringRectangle.METERING_WEIGHT_MAX - 1);
                            mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mZoom);
//                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS,new MeteringRectangle[]{focusAreaTouch});
                        }
                        else {
                            mFingerSpacing = currentFingerSpacing;
                        }

                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, null);
                        mIsMultiTouch = true;
                        pTrackingCenter = currentFingerCenter;
                        if(!bTracking)
                            startTrackingThread();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onTouch: ",e );
                }
                return true;
            }
        });
    }

    public void drawPointsOnImageView(ImageView imageView) {
        Bitmap bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a new Canvas with the bitmap
        Canvas canvas = new Canvas(bitmap);

        // Clear the canvas by drawing a transparent color
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Create a paint object for drawing the points
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        paint.setTextSize(65);
        int circleRadius = 20;

        Point center = calculateCenterOfGravity(apFocusPoints);
        List<Point> vectors = calculateVectors(center, apFocusPoints);
        ArrayList<Float> angles = calculateAngles(vectors);
        ArrayList<Integer> order = getSortedIndexList(angles);
        apFocusPoints = rearrangeObjects(apFocusPoints, order);
        for (int i = 0; i < apFocusPoints.size(); i++) {
            Point nextPoint = new Point();
            if(i == (apFocusPoints.size()-1)){
                nextPoint = apFocusPoints.get(0);
            }
            else {
                nextPoint = apFocusPoints.get(i+1);
            }
            canvas.drawText(String.valueOf(i),apFocusPoints.get(i).x,apFocusPoints.get(i).y,paint);
            canvas.drawCircle(apFocusPoints.get(i).x, apFocusPoints.get(i).y, circleRadius, paint);
            canvas.drawLine(apFocusPoints.get(i).x, apFocusPoints.get(i).y, nextPoint.x, nextPoint.y, paint);
        }

        imageView.setImageBitmap(bitmap);
    }

    private static Point calculateCenterOfGravity(List<Point> points) {
        int totalX = 0;
        int totalY = 0;
        int size = points.size();

        for (Point point : points) {
            totalX += point.x;
            totalY += point.y;
        }

        int centerX = totalX / size;
        int centerY = totalY / size;
        return new Point(centerX, centerY);
    }

    private static <T> ArrayList<T> rearrangeObjects(ArrayList<T> o, ArrayList<Integer> i) {
        ArrayList<T> rearrangedList = new ArrayList<>(Collections.nCopies(o.size(), null));

        for (int index = 0; index < o.size(); index++) {
            int newIndex = i.get(index);
            rearrangedList.set(newIndex, o.get(index));
        }

        return rearrangedList;
    }

    private static List<Point> calculateVectors(Point center, List<Point> points) {
        List<Point> vectors = new ArrayList<>();

        for (Point point : points) {
            int vectorX = point.x - center.x;
            int vectorY = point.y - center.y;
            vectors.add(new Point(vectorX, vectorY));
        }

        return vectors;
    }

    private static ArrayList<Float> calculateAngles(List<Point> vectors) {
        ArrayList<Float> angles = new ArrayList<>();

        for (Point vector : vectors) {
            double angle = Math.atan2(vector.x, -vector.y);
            angles.add((float) Math.toDegrees(angle));
        }

        return angles;
    }

    static void bubbleSort(float arr[], int n , int index[])
    {
        int i, j , temp_index;
        float temp;
        boolean swapped;
        for (i = 0; i < n - 1; i++) {
            swapped = false;
            for (j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {

                    // Swap arr[j] and arr[j+1]
                    temp = arr[j];
                    temp_index = index[j];
                    arr[j] = arr[j + 1];
                    index[j] = index[j+1];
                    arr[j + 1] = temp;
                    index[j+1] = temp_index;
                    swapped = true;
                }
            }

            // If no two elements were
            // swapped by inner loop, then break
            if (swapped == false)
                break;
        }
    }

    public static ArrayList<Integer> getSortedIndexList(ArrayList<Float> floatList) {
        
        // Create an index array and sort it based on the values of the float array
        int n = floatList.size();
        int[] indexList = new int[n];
        float[] floatarray = new float[n];
        for (int i = 0; i < floatList.size(); i++) {
            indexList[i]=i;
            floatarray[i] = floatList.get(i);
        }
        bubbleSort(floatarray,n,indexList);
        ArrayList<Integer> sortedIndex = new ArrayList<Integer>(Collections.nCopies(n, 0));
        for (int i = 0; i < floatList.size(); i++){
            sortedIndex.set(indexList[i],i);
        }
        return  sortedIndex;
    }


    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                mCameraCharacteristics = manager.getCameraCharacteristics(cameraId);

                mMaximumZoomLevel = this.mCameraCharacteristics.get( CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM );
                Log.d(TAG, "setUpCameraOutputs: zoom"+ mMaximumZoomLevel);
                // We don't use a front facing camera in this sample.
                Integer facing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = mCameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;

                // Check if the flash is supported.
                Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.

        }
    }

    public void zoom( float delta )
    {
        Log.v( TAG, "zoom( float delta ) -> " + delta );

        final float zoomLevel = mZoomLevel + delta;

        if ( zoomLevel > mMaximumZoomLevel || zoomLevel < 0f )
        {
            return;
        }
        Log.v( TAG, "zoom = " + zoomLevel );

        mBackgroundHandler.post( () -> {
            final android.graphics.Rect rect = mCameraCharacteristics.get( CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE );

            mZoomLevel = zoomLevel;

            float ratio = 1f / mZoomLevel;

            int croppedWidth = rect.width() - Math.round( ( float ) rect.width() * ratio );
            int croppedHeight = rect.height() - Math.round( ( float ) rect.height() * ratio );

            mZoom = new android.graphics.Rect( croppedWidth / 2, croppedHeight / 2,
                    rect.width() - croppedWidth / 2, rect.height() - croppedHeight / 2 );
            try
            {
                mPreviewRequestBuilder.set( CaptureRequest.SCALER_CROP_REGION, mZoom );
                mCaptureSession.setRepeatingRequest( mPreviewRequestBuilder.build(), mCaptureCallback, null );
            }
            catch ( CameraAccessException e )
            {
                Log.e(TAG, "zoom: error ",e );
            }
        } );
    }
    private float getFingerSpacing(MotionEvent event) {
//        Log.d(TAG, "getFingerSpacing:xy1 xy2 "+event.getX(0)+" "+event.getY(0)+" "+event.getX(1)+" "+event.getY(1));
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    private PointF getFingerCenter(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        return new PointF(x/2.0f,y/2.0f);
    }

    /**
     * Opens the camera specified by {@link Fragment_camera#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            Log.d(TAG, "createCameraPreviewSession: "+mPreviewSize);
            // We configure the size of default buffer to be the size of camera preview we want.
            ;
            Log.d(TAG, "TextureVIew: "+mTextureView.getHeight()+" "+mTextureView.getWidth());
            texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                float yourMinFocus = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                                float yourMaxFocus = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);
                                Log.d(TAG, " min_max focus: "+yourMinFocus+" "+yourMaxFocus);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                                mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, yourMinFocus);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                                setAutoFlash(mPreviewRequestBuilder);
                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }


    @Override
    public void onClick(View view) {

    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage = null;
        /**
         * The file we save the image into.
         */

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

                mImage.close();

            }
        }

    }



    /**
     * Compares two {@code Size}s based on their areas.
     */
    class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */

