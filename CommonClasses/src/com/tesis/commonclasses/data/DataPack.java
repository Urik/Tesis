package com.tesis.commonclasses.data;

import java.util.ArrayList;

/**
 * Created by joaquin on 10/12/13.
 * Estos van a ser los objetos que se van a enviar al server, contieen todos los datos necesarios
 */

public class DataPack {
    private String phoneNumber;
    private ArrayList<PerformanceData> data;

    public DataPack() {
        data = new ArrayList<PerformanceData>();
    }

    public String getAsJson() {
        return "";
    }
}
