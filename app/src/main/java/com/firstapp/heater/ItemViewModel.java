package com.firstapp.heater;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemViewModel extends ViewModel {
    private final MutableLiveData<String> LiveActuator = new MutableLiveData<String>();
    private final MutableLiveData<String> LiveTemperature = new MutableLiveData<String>();
    private final MutableLiveData<String> LiveVideoPath = new MutableLiveData<String>();

    void setLiveActuator(String ActuatorData){
        LiveActuator.setValue(ActuatorData);
    }
    void setLiveTemperature(String TemperatureData){
        LiveTemperature.setValue(TemperatureData);
    }

    void setVideoHistory (String VideoPath) { LiveVideoPath.setValue(VideoPath);};

    public LiveData<String> getLiveActuator(){
        return LiveActuator;
    }

    public  LiveData<String> getLiveTemperature(){
        return LiveTemperature;
    }

    public  LiveData<String> getLiveVideoPath(){
        return LiveVideoPath;
    }
}
