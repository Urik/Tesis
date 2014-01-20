package com.tesis.datacollector.listeners;

import java.util.Date;

public interface PhoneIsMakingACallListener {
    void handlePhoneIsMakingACall(String number, Date date);
}
