package com.cumulocity.me.smartrest.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.io.HttpConnection;

import com.cumulocity.me.smartrest.client.SmartResponse;
import com.cumulocity.me.util.IOUtils;
import com.cumulocity.me.util.StringUtils;

public class SmartResponseImpl implements SmartResponse {

    private final int status;
    private final String message;
    private final SmartRow[] dataRows;

    public SmartResponseImpl(int status, String message, String input) throws IOException {
        this.status = status;
        this.message = message;
        if (isSuccessful()) {
            this.dataRows = readData(StringUtils.split(input, "\n"));
        } else {
            this.dataRows = null;
        }
    }
    
    public SmartResponseImpl(int status, String message, SmartRow[] dataRows) {
        this.status = status;
        this.message = message;
        this.dataRows = dataRows;
    }
    
    public boolean isSuccessful() {
        return status < 300;
    }
    
    public boolean isTimeout() {
        return status == 408;
    }
    
    public int getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
            
    public SmartRow[] getDataRows() {
        return dataRows;
    }
    
    
    private SmartRow[] readData(String[] input) throws IOException {
        if (!isSuccessful()) {
            return null;
        }
        SmartRow[] parsedLines = new SmartRow[input.length];
        for (int i = 0; i < input.length; i++) {
            parsedLines[i] = new SmartRow(input[i]);
        }
        return parsedLines;
    }
}
