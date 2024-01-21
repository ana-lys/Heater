package com.firstapp.heater;

import static com.firstapp.heater.R.color.cred;
import static com.firstapp.heater.R.color.lightgreen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.chip.Chip;


public class Fragment_actuator extends Fragment {

    //    View view;
    ItemViewModel VmActuator;
    TextView TvHeating,TvFan;
    SeekBar SbHeating,SbFan;
    Chip Cbtn;

    int iHeatValue=0,iFanValue=0;

    boolean bAcitvate = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VmActuator = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        VmActuator.setLiveActuator("actuator_command");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_actuator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TvFan = view.findViewById(R.id.TvFan);
        TvHeating =view.findViewById(R.id.TvHeating);
        SbFan = view.findViewById(R.id.SbFan);
        SbHeating = view.findViewById(R.id.SbHeating);
        Cbtn = view.findViewById(R.id.chip);
        SbFan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TvFan.setText("Fan  :"+ getProgressString(progress));
                iFanValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SbHeating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TvHeating.setText("Heat :"+ getProgressString(progress));
                iHeatValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Cbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bAcitvate) {
                    VmActuator.setLiveActuator(iHeatValue+","+iFanValue);
                }
                else{
                    VmActuator.setLiveActuator("0,0");
                }
                bAcitvate = !bAcitvate;
                changeBtnDisp();
            }
        });

    }
    private String getProgressString(int progress) {
        String progressString;
        if (progress < 10) {
            progressString = "    " + progress + "%";
        } else if (progress < 100) {
            progressString = "  " + progress + "%";
        } else {
            progressString = progress + "%";
        }
        return progressString;
    }
    private void changeBtnDisp(){
        if(bAcitvate){
            Cbtn.setText("Proceed");
            Cbtn.setTextColor(getResources().getColor(lightgreen));
        }else {
            Cbtn.setText("Stop");
            Cbtn.setTextColor(getResources().getColor(cred));
        }
    }
}