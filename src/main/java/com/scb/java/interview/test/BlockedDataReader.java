package com.scb.java.interview.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockedDataReader<K> {
    private final Map<K, ReadWriteLock> locks;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();

    public BlockedDataReader() {
        this.locks = new HashMap<>();
    }

    public ReadWriteLock set(K key, final ReadWriteLock readWriteLock) {
        var newReadWriteLock = locks.get(key);
        if (Objects.isNull(newReadWriteLock)) {
            final var newSecondReadWriteLock = locks.get(key);
            if(Objects.isNull(newSecondReadWriteLock)) {
                readWriteLock.writeLock().lock();
                locks.put(key, readWriteLock);
                return readWriteLock;
            }
            newReadWriteLock = newSecondReadWriteLock;
        }
        return newReadWriteLock;
    }

    public ReadWriteLock get(K key) {
        readLock.lock();
        try {
            return locks.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public boolean contains(K key) {
        readLock.lock();
        try {
            return locks.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }
}
