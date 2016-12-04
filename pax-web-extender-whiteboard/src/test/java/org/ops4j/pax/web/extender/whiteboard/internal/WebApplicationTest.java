/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class WebApplicationTest {

	private static final int NUMBER_OF_CONCURRENT_EXECUTIONS = 15;
	private static final int REPETITIONS_OF_MULTI_THREADED_TEST = 500;

	private static final Logger LOG = LoggerFactory
			.getLogger(WebApplicationTest.class);

	@Mock
	WebElement webElement;

	@Mock
	WebContainer oldService;

	@Mock
	WebContainer newService;

	@Mock
	HttpContextMapping httpContextMapping;

	@Mock
	ExtendedHttpServiceRuntime httpServiceRuntime;

	@Mock
	HttpContext httpContext;

	@Mock
	Bundle bundle;

	@Mock
	BundleContext bundleContext;

	// @InjectMocks
	WebApplication instanceUnderTest;

	private volatile Exception exceptionInRunnable;

	private Random random;

	@Before
	public void setUp() throws Exception {
		when(bundle.getBundleContext()).thenReturn(bundleContext);
		when(httpContextMapping.getHttpContext()).thenReturn(httpContext);

		Mockito.doNothing().when(webElement).register(any(WebContainer.class), any(HttpContext.class));
		Mockito.doNothing().when(webElement).unregister(any(WebContainer.class), any(HttpContext.class));

		instanceUnderTest = new WebApplication(bundle, "myID", false, new ExtendedHttpServiceRuntime(bundleContext));
		random = new Random();
	}

	@After
	public void tearDown() throws Exception {
		Mockito.reset(webElement);
	}

	@Test
	public void test() throws Exception {
		try {
			LOG.info("Running test");

			instanceUnderTest.setHttpContextMapping(httpContextMapping);

			final CountDownLatch countDownLatch = new CountDownLatch(1);
			final Runnable getOrCreateContextRunnable = new Runnable() {
				@Override
				public void run() {
					// CHECKSTYLE:OFF
					try {
						countDownLatch.await();

						int nextInt = random.nextInt(2);
						if (nextInt == 1) {
							LOG.info("addWebElement...");
							instanceUnderTest.addWebElement(webElement);
						} else {
							LOG.info("serviceChanged...");
							instanceUnderTest
									.serviceChanged(oldService, newService, Collections.EMPTY_MAP);
						}
					} catch (final InterruptedException ex) {
						// ignore
					} catch (final Exception ex) {
						exceptionInRunnable = ex;
					}
					// CHECKSTYLE:ON
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
		} finally {
			Mockito.reset(webElement);
		}
	}

	@Test
	// @Ignore
	public void executeMultiThreadedTestMultipleTimes() throws Throwable {
		int i = 0;
		try {
			for (; i < REPETITIONS_OF_MULTI_THREADED_TEST; i++) {
				LOG.info("multipleTimes: {}-run", i);
				test();
			}
		} catch (final Throwable ex) {
			System.out.println("Broken in Run #" + i);
			throw ex;
		}
	}

}
