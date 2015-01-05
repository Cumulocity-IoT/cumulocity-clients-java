package com.cumulocity.smartrest.client.impl;

import com.cumulocity.smartrest.client.SmartConnection;

public class SmartHeartBeatWatcher {
    
    public static int HEARTBEAT_CHECK_INTERVAL = 720000;
    
    private SmartConnection connection;
    private Thread watcherThread;
    private boolean heartbeat;
    private Thread readerThread;
    
    public SmartHeartBeatWatcher (SmartConnection connection, Thread readerThread) {
        this.connection = connection;
        this.readerThread = readerThread;
    }
    
    public void start() {
        watcherThread = new Thread(new HeartBeatWatcher());
        watcherThread.start();
    }
    
    public void stop() {
        if (watcherThread.isAlive()) {
            watcherThread.interrupt();
        }
    }
    
    public synchronized void heartbeat() {
        heartbeat = true;
    }
    
    private class HeartBeatWatcher implements Runnable {
        public void run() {
            do {
                try {
                    Thread.sleep(HEARTBEAT_CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            } while (checkConnection());
        }
        
        private boolean checkConnection() {
            synchronized (connection) {
                if (!heartbeat) {
                    connection.closeConnection();
                    readerThread.interrupt();
                    return false;
                 }
                 heartbeat = false;
                 return true;
            }
        }
    }
}
