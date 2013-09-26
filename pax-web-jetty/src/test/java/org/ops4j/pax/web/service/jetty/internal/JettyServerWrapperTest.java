package org.ops4j.pax.web.service.jetty.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;

@RunWith(MockitoJUnitRunner.class)
public class JettyServerWrapperTest {
	private static final String KNOWN_CONTEXT_NAME = "TestContext";
	private static final String BUNDLE_SYMBOLIC_NAME = "BundleSymbolicName";
	private static final int NUMBER_OF_CONCURRENT_EXECUTIONS = 2;
	private static final int REPETITIONS_OF_MULTI_THREADED_TEST = 1000;
	@Mock
	private ServerModel serverModelMock;
	@Mock
	private ContextModel contextModelMock;
	@Mock
	private HttpContext httpContextMock;
	@Mock
	private Bundle bundleMock;
	@Mock
	private BundleContext bundleContextMock;
	private volatile Exception exceptionInRunnable;

	@Before
	public void mockIt() {
		when(contextModelMock.getContextName()).thenReturn(KNOWN_CONTEXT_NAME);
		when(contextModelMock.getHttpContext()).thenReturn(httpContextMock);
		when(contextModelMock.getBundle()).thenReturn(bundleMock);
		when(bundleMock.getHeaders()).thenReturn(
				new Hashtable<String, String>());
		when(bundleMock.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
		when(bundleMock.getBundleContext()).thenReturn(bundleContextMock);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getOrCreateContextDoesNotRegisterMultipleServletContextsForSameContextModelSingleThreaded()
			throws Exception {
		final JettyServerWrapper jettyServerWrapperUnderTest = new JettyServerWrapper(serverModelMock);
		try {
			jettyServerWrapperUnderTest.start();
			jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
			jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);

			verify(bundleContextMock, times(1)).registerService(
					same(ServletContext.class), any(ServletContext.class),
					any(Dictionary.class));
		} finally {
			jettyServerWrapperUnderTest.stop();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getOrCreateContextDoesNotRegisterMultipleServletContextsForSameContextModelMultiThreaded()
			throws Exception {
		final JettyServerWrapper jettyServerWrapperUnderTest = new JettyServerWrapper(serverModelMock);
		try {
			jettyServerWrapperUnderTest.start();
			final CountDownLatch countDownLatch = new CountDownLatch(1);
			final Runnable getOrCreateContextRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						countDownLatch.await();
						jettyServerWrapperUnderTest
								.getOrCreateContext(contextModelMock);
					} catch (final InterruptedException ex) {
						// ignore
					} catch (final Exception ex) { // CHECKSTYLE:SKIP
						exceptionInRunnable = ex;
					}
				}
			};
			final ExecutorService executor = Executors
					.newFixedThreadPool(NUMBER_OF_CONCURRENT_EXECUTIONS);
			for (int i = 0; i < NUMBER_OF_CONCURRENT_EXECUTIONS; i++) {
				executor.execute(getOrCreateContextRunnable);
			}
			countDownLatch.countDown();
			executor.shutdown();
			final boolean terminated = executor.awaitTermination(10,
					TimeUnit.SECONDS);
			if (exceptionInRunnable != null) {
				exceptionInRunnable = null;
				throw exceptionInRunnable;
			}
			assertTrue("could not shutdown the executor within the timeout",
					terminated);

			verify(bundleContextMock, times(1)).registerService(
					same(ServletContext.class), any(ServletContext.class),
					any(Dictionary.class));
		} finally {
			jettyServerWrapperUnderTest.stop();
		}
	}

	@Test
	public void executeMultiThreadedTestMultipleTimes() throws Throwable {
		int i = 0;
		try {
			for (; i < REPETITIONS_OF_MULTI_THREADED_TEST; i++) {
				getOrCreateContextDoesNotRegisterMultipleServletContextsForSameContextModelMultiThreaded();
				reset(bundleContextMock);
			}
		} catch (final Throwable ex) { //CHECKSTYLE:SKIP
			System.out.println("Broken in Run #" + i);
			throw ex;
		}
	}

	@Test
	public void sequenceOfGetOrCreateContextGetContextRemoveContext()
			throws Exception {
		final JettyServerWrapper jettyServerWrapperUnderTest = new JettyServerWrapper(serverModelMock);
		try {
			jettyServerWrapperUnderTest.start();
			jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
			final HttpServiceContext httpServiceContext = jettyServerWrapperUnderTest
					.getContext(httpContextMock);
			jettyServerWrapperUnderTest.removeContext(httpContextMock);
			final HttpServiceContext httpServiceContextAfterRemoved = jettyServerWrapperUnderTest
					.getContext(httpContextMock);

			assertNotNull(httpServiceContext);
			assertNull(httpServiceContextAfterRemoved);
		} finally {
			jettyServerWrapperUnderTest.stop();
		}
	}
}
