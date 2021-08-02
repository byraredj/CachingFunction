package com.scb.java.interview.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AOP Thread Service Tests")
class ThreadDetailServiceTest {

    @Test
    @DisplayName("To Check NonProd to call AOP methods")
    public void testProdOrNonProd() {
        System.setProperty("com.scb.env", "TEST");
        assertThat(ThreadDetailService.NON_PROD, is(true));
    }

    @Test
    @DisplayName("To Capture Thread Execution Details")
    public void toCaptureTestExecutionDetails() {
        ThreadDetail threadDetail = new ThreadDetail(ThreadDetail.ReadFrom.DATASERVICE,
                                                    ThreadDetail.LockType.WRITE);
        ThreadDetailService.capture("THREAD1",threadDetail);

        assertNotNull(ThreadDetailService.getThreadDetail("THREAD1"));
        assertThat(ThreadDetailService.getThreadDetail("THREAD1"), is(threadDetail));
    }
}