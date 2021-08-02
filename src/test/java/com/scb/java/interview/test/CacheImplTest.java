package com.scb.java.interview.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheImplTest {

    private final static Predicate<ThreadDetail> writeFilter = t -> t.getLockType().equals(ThreadDetail.LockType.WRITE);
    private final static Predicate<ThreadDetail> readFilter = t -> t.getLockType().equals(ThreadDetail.LockType.READ);
    private final static Predicate<ThreadDetail> noLockFilter = t -> t.getLockType().equals(ThreadDetail.LockType.NO_LOCK);
    private final static Predicate<ThreadDetail> dataServiceRead = t -> t.getReadFrom().equals(ThreadDetail.ReadFrom.DATASERVICE);
    private final static Predicate<ThreadDetail> cacheRead = t -> t.getReadFrom().equals(ThreadDetail.ReadFrom.CACHE);

    private ConcurrentMap<Integer, String> dataMap;
    private Function<Integer, String> dataService;
    private Cache<Integer, String> dataCache;

    @Mock
    private Cache<Integer, String> dataCacheMock;

    @Mock
    private DataService<Integer, String> dataServiceMock;

    @Mock
    private CacheImpl<Integer, String> readCacheMock;

    @BeforeEach
    public void init() {
        dataMap = new ConcurrentHashMap<>();
        dataMap.put(1, "One");
        dataMap.put(2, "Two");
        dataMap.put(3, "Three");
        dataMap.put(4, "Four");
        dataService = new DataService<>(dataMap);
        dataCache = new CacheImpl<>(dataService);
        dataCacheMock = new CacheImpl<>(dataServiceMock);
        ThreadDetailService.clear();
    }

    @Test
    @DisplayName("Test for key present in the DataMap")
    public void testSingleRead() {
        assertThat(dataCache.get(1), is("One"));
        assertThat(dataCache.get(1), is("One"));
    }

    @Test
    @DisplayName("Test for key not present in the DataMap")
    public void testNonPresentKey() {
        assertNull(dataCache.get(5));
    }

    @Test
    @DisplayName("Test DataService Function is invoked only once")
    public void testCacheReadUsingMock() {
        String value = "One";
        when(dataServiceMock.apply(anyInt())).thenReturn(value);
        when(dataCacheMock.get(anyInt())).thenReturn(value);

        assertThat(dataCacheMock.get(1), is(value));
        assertThat(dataCacheMock.get(1), is(value));

        verify(dataServiceMock, atMost(1)).apply(1);
    }

    @Test
    @DisplayName("Test Cache is invoked twice for the same key")
    public void testCacheVerifyUsingMock() {
        String first = "One";
        when(readCacheMock.get(anyInt())).thenReturn(first);

        assertThat(readCacheMock.get(1), is(first));
        assertThat(readCacheMock.get(1), is(first));

        verify(readCacheMock, times(2)).get(1);
    }

    @Test
    @DisplayName("Test two threads calling for the same key has one thread read from DataFunction and other from Cache")
    public void testTwoThreadsForSingleKey() throws InterruptedException, ExecutionException {
        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        Future<ConcurrentMap<String, ThreadDetail>>[] futures = new Future[numberOfThreads];

        for (int i = 1; i <= numberOfThreads; i++) {
            futures[i - 1] = service.submit(() -> {
                dataCache.get(1);
                latch.countDown();
                return ThreadDetailService.getThreadDetails();
            });
        }
        latch.await();
        ConcurrentMap<String, ThreadDetail> result = futures[0].get();

        assertThat(result.values().stream().filter(writeFilter).count(), is(1L));
        assertThat(result.values().stream().filter(readFilter.or(noLockFilter)).count(), is(1L));
    }

    @Test
    @DisplayName("Test two threads calling for different keys should have only two DataFunction Reads")
    public void testTwoThreadsForTwoKeys() throws InterruptedException, ExecutionException {
        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Future<ConcurrentMap<String, ThreadDetail>>> futures;

        Callable<ConcurrentMap<String, ThreadDetail>> firstKey = () -> {
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> secondKey = () -> {
            dataCache.get(2);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };

        Collection<Callable<ConcurrentMap<String, ThreadDetail>>> services = List.of(firstKey, secondKey);
        futures = service.invokeAll(services);
        latch.await();

        ConcurrentMap<String, ThreadDetail> result = futures.get(0).get();

        assertThat(result.values().stream().filter(writeFilter).count(), is(2L));
        assertThat(result.values().stream().filter(readFilter.or(noLockFilter)).count(), is(0L));

    }

    @Test
    @DisplayName("Test two keys on 100 Threads should have only 2 DataFunction reads")
    public void testThreadsForTwoKeysMoreThanProcessorCount() throws InterruptedException, ExecutionException {
        var numberOfThreads = 100;
        var service = Executors.newFixedThreadPool(numberOfThreads);
        var latch = new CountDownLatch(numberOfThreads);
        List<Future<ConcurrentMap<String, ThreadDetail>>> futures;

        Callable<ConcurrentMap<String, ThreadDetail>> firstKey = () -> {
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> secondKey = () -> {
            dataCache.get(2);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };

        Collection<Callable<ConcurrentMap<String, ThreadDetail>>> services = new ArrayList<>();
        for(var i = 1; i <= numberOfThreads; i++) {
            if(i % 2 == 0)
                services.add(firstKey);
            else
                services.add(secondKey);
        }

        futures = service.invokeAll(services);
        latch.await();

        ConcurrentMap<String, ThreadDetail> result = futures.get(0).get();

        assertThat(result.values().stream().filter(writeFilter).count(), is(2L));
        var expectedCount = numberOfThreads - 2L;
        assertThat(result.values().stream().filter(readFilter.or(noLockFilter)).count(), is(expectedCount));

    }

    @Test
    @DisplayName("Test multiple threads with various keys")
    public void testVariousKeysWithMultipleThreads() throws InterruptedException, ExecutionException {
        var numberOfThreads = 5;
        var service = Executors.newFixedThreadPool(numberOfThreads);
        var latch = new CountDownLatch(numberOfThreads);
        List<Future<ConcurrentMap<String, ThreadDetail>>> futures;

        Callable<ConcurrentMap<String, ThreadDetail>> firstKey = () -> {
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> secondKey = () -> {
            dataCache.get(2);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> threeKey = () -> {
            dataCache.get(3);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> fourKey = () -> {
            dataCache.get(4);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> fiveKey = () -> {
            dataCache.get(5);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };

        Collection<Callable<ConcurrentMap<String, ThreadDetail>>> services = List.of(firstKey, secondKey, threeKey, fourKey, fiveKey);

        futures = service.invokeAll(services);
        latch.await();

        ConcurrentMap<String, ThreadDetail> result = futures.get(0).get();

        assertThat(result.values().stream().filter(writeFilter).count(), is(4L));
        assertThat(result.values().stream().filter(readFilter.or(noLockFilter)).count(), is(0L));
    }

    @Test
    @DisplayName("Test Thread pulling the data from Cache after Initial Read with delay in between")
    public void testThreadPullingFromCache() throws InterruptedException, ExecutionException {
        var numberOfThreads = 2;
        var service = Executors.newFixedThreadPool(numberOfThreads);
        var latch = new CountDownLatch(numberOfThreads);
        List<Future<ConcurrentMap<String, ThreadDetail>>> futures;

        Callable<ConcurrentMap<String, ThreadDetail>> firstKey = () -> {
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> secondKey = () -> {
            Thread.sleep(1000l);
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };

        Collection<Callable<ConcurrentMap<String, ThreadDetail>>> services = List.of(firstKey, secondKey);

        futures = service.invokeAll(services);
        latch.await();

        ConcurrentMap<String, ThreadDetail> result = futures.get(0).get();

        assertThat(result.values().stream().filter(writeFilter).count(), is(1L));
        assertThat(result.values().stream().filter(dataServiceRead).count(), is(1L));
        assertThat(result.values().stream().filter(noLockFilter.or(readFilter)).count(), is(1L));
        assertThat(result.values().stream().filter(cacheRead).count(), is(1L));
    }

    @Test
    @DisplayName("Test Multiple Threads with same Key and 2 threads with different key should have 2 DataService reads and others from Cache")
    public void testMultipleThreadsWithTwoKeys() throws InterruptedException, ExecutionException {
        var numberOfThreads = Runtime.getRuntime().availableProcessors() + 5;
        var service = Executors.newFixedThreadPool(numberOfThreads);
        var latch = new CountDownLatch(numberOfThreads);
        List<Future<ConcurrentMap<String, ThreadDetail>>> futures;

        Callable<ConcurrentMap<String, ThreadDetail>> firstKey = () -> {
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> secondKey = () -> {
            dataCache.get(2);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };
        Callable<ConcurrentMap<String, ThreadDetail>> firstKeyWithDelay = () -> {
            dataCache.get(1);
            latch.countDown();
            return ThreadDetailService.getThreadDetails();
        };

        Collection<Callable<ConcurrentMap<String, ThreadDetail>>> services = new ArrayList<>(numberOfThreads);
        for(var i = 0; i < numberOfThreads - 5; i++) {
            services.add(firstKey);
        }
        services.add(secondKey);
        services.add(secondKey);
        services.add(firstKeyWithDelay);
        services.add(firstKeyWithDelay);
        services.add(firstKeyWithDelay);

        futures = service.invokeAll(services);
        latch.await();

        ConcurrentMap<String, ThreadDetail> result = futures.get(0).get();

        assertThat(result.values().stream().filter(writeFilter).count(), is(2L));
        assertThat(result.values().stream().filter(dataServiceRead).count(), is(2L));
        var expectedResult = numberOfThreads - 2L;
        assertThat(result.values().stream().filter(noLockFilter.or(readFilter)).count(), is(expectedResult));
        assertThat(result.values().stream().filter(cacheRead).count(), is(expectedResult));
    }

}