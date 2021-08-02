package com.scb.java.interview.test;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class DataService<K, V> implements Function<K, V> {

    private final ConcurrentMap<K, V> dataService;

    public DataService(ConcurrentMap<K, V> dataService) {
        this.dataService = dataService;
    }

    @Override
    public V apply(K k) {
        return dataService.get(k);
    }
}
