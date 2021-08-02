package com.scb.java.interview.test;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThreadDetailService {

    private static final ConcurrentMap<String, ThreadDetail> threadDetails;
    private static final String ENV;
    public static final boolean NON_PROD;

    static {
        ENV = System.getProperty("com.scb.env");
        NON_PROD = Objects.isNull(ENV) || ENV.contains("TEST");
        threadDetails = new ConcurrentHashMap<>();
    }

    public static void capture(final String threadName, final ThreadDetail threadDetail) {
        if (NON_PROD)
            threadDetails.put(threadName, threadDetail);
    }

    public static ThreadDetail getThreadDetail(final String threadName) {
        if (NON_PROD)
            return threadDetails.get(threadName);
        else
            return null;
    }

    public static ConcurrentMap<String, ThreadDetail> getThreadDetails() {
        return threadDetails;
    }

    public static void induceSleep(final long ms, final boolean nonProd) {
        try {
            if (nonProd)
                Thread.sleep(ms);
        } catch (InterruptedException e) {
            System.out.println("Thread was Interrupted");
        }
    }

    public static void clear() {
        threadDetails.clear();
    }
}
