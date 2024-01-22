package com.firstapp.heater;

import static com.firstapp.heater.R.color.cred;
import static com.firstapp.heater.R.color.lightgreen;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Fragment_history extends Fragment {

    //    View view;
    ItemViewModel VmHistory;

    Spinner spinner ;

    Button Preview, Refresh;

    private static final String TAG = "Fragment_history";

    boolean bAcitvate = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VmHistory = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
//

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spinner = view.findViewById(R.id.spinner);
        Preview = view.findViewById(R.id.btnPreview);
        Refresh = view.findViewById(R.id.btnRefresh);
        refresh();
        Refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
        Preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Retrieve the currently selected item
                Object selectedItem = spinner.getSelectedItem();
                String selectedString = selectedItem.toString();
                String folderName = "video";
                File subDirectory = new File(getContext().getExternalFilesDir(null), folderName);
                File absoluteFile = new File(subDirectory, selectedString);
                String absolutePath = absoluteFile.getAbsolutePath();
                VmHistory.setVideoHistory(absolutePath);
            }
        });
    }

    public void refresh(){
        List<CharSequence> itemList = new ArrayList<>();
        String folderName = "video";
        File subDirectory = new File(getContext().getExternalFilesDir(null), folderName);
        Log.d(TAG, "onViewCreated: " + subDirectory.getAbsolutePath());

        if (subDirectory.exists() && subDirectory.isDirectory()) {
            File[] files = subDirectory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String fileName = file.getName();
                        itemList.add(fileName);
                    }
                }
            }
        }
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(requireContext(), R.layout.custom_spinner_dropdown_item, itemList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

}