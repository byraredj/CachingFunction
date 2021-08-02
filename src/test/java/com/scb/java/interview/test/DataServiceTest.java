package com.scb.java.interview.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Data Function Tests")
class DataServiceTest {

    @Mock
    private DataService<Integer, String> dataServiceMock;

    private ConcurrentMap<Integer, String> dataMap;
    private Function<Integer, String> dataService;

    @BeforeEach
    private void init() {
        dataMap = new ConcurrentHashMap<>();
        dataMap.put(1, "One");
        dataMap.put(2, "Two");
        dataMap.put(3, "Three");
        dataMap.put(4, "Four");
        dataService = new DataService<>(dataMap);
    }

    @Test
    @DisplayName("Dataservice object creation")
    public void testDataService() {
        assertNotNull(dataService);
    }

    @Test
    @DisplayName("Check the count the same key invoked twice")
    public void testDataServiceInvokedForSameKeyTwice() {
        when(dataServiceMock.apply(anyInt())).thenReturn("One");

        assertThat(dataServiceMock.apply(1), is("One"));
        assertThat(dataServiceMock.apply(1), is("One"));
        verify(dataServiceMock, times(2)).apply(1);
    }

    @Test
    @DisplayName("Check the count for two different unique calls")
    public void testDataServiceInvokedForDifferentKeys() {
        when(dataServiceMock.apply(anyInt())).thenReturn("One", "Two");
        assertThat(dataServiceMock.apply(1), is("One"));
        assertThat(dataServiceMock.apply(2), is("Two"));
        verify(dataServiceMock, times(1)).apply(1);
        verify(dataServiceMock, times(1)).apply(2);
    }

    @Test
    @DisplayName("Ensure the input dataMap matches output dataMap from DataService")
    public void testMapInputAndOutput() {
        Set<Integer> inputKeys = dataMap.keySet();
        Collection<String> inputValues = dataMap.values();

        Set<Integer> outputKeys = new HashSet<>();
        Collection<String> outputValues = new LinkedList<>();
        inputKeys.forEach(k -> {
            outputKeys.add(k);
            outputValues.add(dataService.apply(k));
        });

        assertIterableEquals(inputKeys, outputKeys);
        assertIterableEquals(inputValues, outputValues);

    }

    @Test
    @DisplayName("To not allow null key")
    public void testNullKeyInserts() {
        assertThrows(NullPointerException.class, () -> dataMap.put(null, "Value"));
    }

    @Test
    @DisplayName("To not allow null value")
    public void testNullValueInserts() {
        assertThrows(NullPointerException.class, () -> dataMap.put(1, null));
    }

    @Test
    @DisplayName("Test to check invoking DataService with null value")
    public void testNullKeyToDataService() {
        assertThrows(NullPointerException.class, () -> dataService.apply(null));
    }

}