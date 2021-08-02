package com.scb.java.interview.test;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class CacheImpl<K, V> implements Cache<K, V> {

    private final Function<K, V> dataService;
    private final ConcurrentMap<K, V> dataCache;
    private final BlockedDataReader<K> blockedDataReader;

    public CacheImpl(Function<K, V> dataService) {
        this.dataService = dataService;
        this.dataCache = new ConcurrentHashMap<>();
        this.blockedDataReader = new BlockedDataReader<>();
    }

    @Override
    public V get(K key) {
        if (!dataCache.containsKey(key)) {
            if (!blockedDataReader.contains(key)) {
                return readValueFromDataService(key);
            } else {
                final var readWriteLock = blockedDataReader.get(key);
                return readValueFromCache(key, readWriteLock);
            }
        } else {
            ThreadDetailService.capture(Thread.currentThread().getName(),
                    new ThreadDetail(ThreadDetail.ReadFrom.CACHE, ThreadDetail.LockType.NO_LOCK));
            return dataCache.get(key);
        }
    }

    private V readValueFromDataService(K key) {
        V value;

        final var oldLock = new ReentrantReadWriteLock();
        final var newLock = blockedDataReader.set(key, oldLock);

        if (newLock != oldLock)
            return readValueFromCache(key, newLock);

        try {
            ThreadDetailService.induceSleep(1000, ThreadDetailService.NON_PROD);
            value = dataService.apply(key);
            if (value != null) {
                dataCache.put(key, value);
            }
        } finally {
            newLock.writeLock().unlock();
        }
        if (Objects.nonNull(value))
            ThreadDetailService.capture(Thread.currentThread().getName(),
                    new ThreadDetail(ThreadDetail.ReadFrom.DATASERVICE, ThreadDetail.LockType.WRITE));

        return value;
    }

    private V readValueFromCache(K key, final ReadWriteLock readWriteLock) {
        V value;

        if (dataCache.containsKey(key)) {
            ThreadDetailService.capture(Thread.currentThread().getName(),
                    new ThreadDetail(ThreadDetail.ReadFrom.CACHE, ThreadDetail.LockType.NO_LOCK));
            return dataCache.get(key);
        }

        readWriteLock.readLock().lock();
        try {
            value = dataCache.get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
        ThreadDetailService.capture(Thread.currentThread().getName(),
                new ThreadDetail(ThreadDetail.ReadFrom.CACHE, ThreadDetail.LockType.READ));

        return value;
    }
}
