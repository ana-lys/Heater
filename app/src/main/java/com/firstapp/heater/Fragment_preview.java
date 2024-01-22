package com.firstapp.heater;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.android.Utils;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;
import org.opencv.videoio.Videoio;

public class Fragment_preview extends Fragment {

    //    View view;
    ItemViewModel VmHistory;

    private volatile boolean isThreadRunning = false;
    ImageView imageView ;

    private Thread videoThread;

    private static final String TAG = "Fragment_preview";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VmHistory = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override

        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            imageView = view.findViewById(R.id.videoView);

            VmHistory.getLiveVideoPath().observe(getViewLifecycleOwner(), item -> {
                Log.d(TAG, "onViewCreated: " + item);

                // Stop the previous thread if it is running
                if (isThreadRunning) {
                    isThreadRunning = false;
                    try {
                        videoThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Start a new thread
                videoThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isThreadRunning = true;
                        VideoCapture videoCapture = new VideoCapture();
                        int api_ref = Videoio.CAP_OPENCV_MJPEG;
                        videoCapture.open(item, api_ref);
                        Mat frame = new Mat();

                        while (isThreadRunning && videoCapture.read(frame)) {
                            // Convert the frame to bitmap
                            final Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(frame, bitmap);

                            // Update the ImageView on the main UI thread
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageBitmap(bitmap);
                                }
                            });

                            // Introduce a delay between frame updates
                            try {
                                Thread.sleep(15);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        isThreadRunning = false;
                    }
                });
                videoThread.start();
            });
        }

}