package com.tesis.datacollector;

/**
 * Created by joaquin on 10/29/13.
 */
public class SignalAvg {
    private int count;

    private float signal;

    SignalAvg(){
        count = 0;
        signal = 0;
    }

    public void clear(){
        count = 0;
        signal = 0;
    }

    public void addSignalValue(float inSignal){
        count++;
        if(count == 0){
            signal = inSignal;
        }else{
            signal += (inSignal - signal) / count;
        }
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public float getSignal() {
        return signal;
    }
}
