package com.scb.java.interview.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@DisplayName("Blocked Data Reader Tests")
class BlockedDataReaderTest {

    private Map<String, String> map;
    private BlockedDataReader<String> blockedDataReader;

    @BeforeEach
    public void init() {
        map = new HashMap<>();
        blockedDataReader = new BlockedDataReader<>();
    }

    @Test
    @DisplayName("Test Adding New Elements")
    public void testAddingElements() {
        prepareData();
        assertThat(blockedDataReader.contains("One"), is(true));
    }

    @Test
    @DisplayName("Test Adding Duplicate Elements")
    public void testAddingDuplicateElements() {
        prepareData();
        ReadWriteLock newLock = new ReentrantReadWriteLock();
        blockedDataReader.set("One", newLock);
        assertThat(blockedDataReader.get("One"), not(newLock));

        ReadWriteLock lockFour = new ReentrantReadWriteLock();
        ReadWriteLock newLockFour = new ReentrantReadWriteLock();
        blockedDataReader.set("Four", lockFour);
        blockedDataReader.set("Four", newLockFour);
        assertThat(blockedDataReader.get("Four"), is(lockFour));
    }

    @Test
    @DisplayName("Test Contains Method With existing key")
    public void testContains() {
        prepareData();
        assertThat(blockedDataReader.contains("One"), is(true));
    }

    @Test
    @DisplayName("Test Contains Method With non existing key")
    public void testContainsNoKey() {
        prepareData();
        assertThat(blockedDataReader.contains("Five"), is(false));
    }

    private void prepareData() {
        ReadWriteLock lockOne = new ReentrantReadWriteLock();
        ReadWriteLock lockTwo = new ReentrantReadWriteLock();
        ReadWriteLock lockThree = new ReentrantReadWriteLock();
        blockedDataReader.set("One", lockOne);
        blockedDataReader.set("Two", lockTwo);
        blockedDataReader.set("Three", lockThree);
    }

}