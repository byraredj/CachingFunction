package com.scb.java.interview.test;

import java.util.StringJoiner;

public class ThreadDetail {

    public enum ReadFrom {
        CACHE,
        DATASERVICE
    }

    public enum LockType {
        READ,
        WRITE,
        NO_LOCK
    }

    private final ReadFrom readFrom;
    private final LockType lockType;

    public ThreadDetail(ReadFrom readFrom, LockType lockType) {
        this.readFrom = readFrom;
        this.lockType = lockType;
    }

    public ReadFrom getReadFrom() {
        return readFrom;
    }

    public LockType getLockType() {
        return lockType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ThreadDetail.class.getSimpleName() + "[", "]")
                .add("readFrom=" + readFrom)
                .add("lockType=" + lockType)
                .toString();
    }
}
