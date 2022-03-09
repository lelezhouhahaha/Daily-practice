package com.zebra.util;

import android.util.Log;

public class SessionTimer {

    public long startTime       = 0;
    public long endTime         = 0;
    public long firstScanTime   = 0;
    public long currentScanTime = 0;
    public long totalScanTime   = 0;
    public long averageTime     = 0;
    public long minScanTime     = 0;
    public long maxScanTime     = 0;
    public long scansPerMin     = 0;
    public int runCount         = 0;
    public int successCount     = 0;


    private long sessionStartTime = 0;

    private long getCurrentTime()
    {
        return System.currentTimeMillis();
    }

    /**
     *  Call before each start scan
     */
    public void startSessionTimer()
    {
        startTime = getCurrentTime();
        sessionStartTime = startTime;

        endTime         = 0;
        firstScanTime   = 0;
        currentScanTime = 0;
        totalScanTime   = 0;
        averageTime     = 0;
        minScanTime     = 1000000;
        maxScanTime     = 0;
        scansPerMin     = 0;
        runCount        = 0;
        successCount    = 0;

    }


    public void calculateTime()
    {

        if(runCount == 1)
        {
            firstScanTime = getCurrentTime() - startTime;
            currentScanTime = firstScanTime;
        }
        else
        {
            currentScanTime = getCurrentTime() - startTime;
            totalScanTime += currentScanTime;

            //we ignore the first scan time for other statistics
            averageTime = totalScanTime / (runCount-1) ;

            long total = getCurrentTime() - sessionStartTime;
            scansPerMin = 60*1000*runCount/total;
        }

        if(currentScanTime > maxScanTime) maxScanTime = currentScanTime;
        if(currentScanTime < minScanTime) minScanTime = currentScanTime;
    }

    public void startSinlgeScanSession()
    {
        runCount++;
        startTime = getCurrentTime();
    }

    public void stopSinlgeScanSession()
    {
        endTime = getCurrentTime();
        successCount++;
    }
}
