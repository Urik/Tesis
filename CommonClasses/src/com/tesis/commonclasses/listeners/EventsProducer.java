package com.tesis.commonclasses.listeners;

public interface EventsProducer<T> {
    void addListener(T listener);
    boolean removeListener(T listener);
    void startListening();
    void stopListening();
}
