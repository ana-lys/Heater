package com.firstapp.heater;

import android.graphics.Color;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Fragment_actuator#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Fragment_temp extends Fragment {

    View view;
    TextView TvTemperature;
    ItemViewModel VmActuator;

    private LineChart LcTemp;

    private List<String> xValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_temp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.i("Entry", "onViewCreated: ");
        super.onViewCreated(view, savedInstanceState);
        TvTemperature = view.findViewById(R.id.TvTemperature);
        LcTemp = view.findViewById(R.id.LcTemperature);
        Description description = new Description();
        description.setText("Temp/Sec");
        description.setPosition(0,0);
        LcTemp.setDescription(description);
        LcTemp.getAxisRight().setDrawLabels(false);
        XAxis xAxis = LcTemp.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        List<Entry> entries = new ArrayList<>();
        VmActuator = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        VmActuator.getLiveTemperature().observe(getViewLifecycleOwner(),newData->{
            String[] parts = newData.split(",");
            float fTemp = Float.parseFloat(parts[0]);
            float fTime = Float.parseFloat(parts[1])/2.0f;
            Entry newEntry = new Entry(fTime, fTemp);
            TvTemperature.setText(String.valueOf(fTemp));
            if (!entries.isEmpty()) {
                Entry lastEntry = entries.get(entries.size() - 1);
                if(lastEntry.getX()==0.0f && fTime ==0.0f) {
                    entries.clear();
                    lastEntry.setX(-1.0f);
                    entries.add(lastEntry);
                }
            }
//            if(entries.size()>150){
//                List<Entry> temp = new ArrayList<>();
//                for(int i=0 ; i <entries.size(); i+=2){
//                    temp.add(entries.get(i));
//                }
//                entries.clear();
//                entries.addAll(temp);
//            }
            entries.add(newEntry);

//            Log.d("Entries",String.valueOf(entries.size()));
            LineDataSet dataSet = new LineDataSet(entries, "Temperature");
            dataSet.setColor(Color.BLUE);
            dataSet.setLineWidth(2f);
            dataSet.setDrawCircles(false);  // Disable drawing circles/points
            dataSet.setDrawValues(false);

            LineData lineData = new LineData();
            lineData.addDataSet(dataSet);
            LcTemp.setData(lineData);
            LcTemp.invalidate();
        });
    }
}