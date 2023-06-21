/*
 * Copyright 2019 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import jakarta.servlet.http.HttpServlet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.ops4j.pax.web.itest.osgi.support.MockServerController;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.http.HttpService;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This test check whether pax-web-spi contains proper private packages and whether the imports are sufficient
 * to perform various tasks related to class/annotation discovery.
 */
@RunWith(PaxExam.class)
public class PaxWebRuntimeIntegrationTest extends AbstractOsgiTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebRuntimeIntegrationTest.class);

	@Inject
	private MetaTypeService metaTypeService;

	@Configuration
	public Option[] configure() throws IOException {
		prepareBundles();

		return combine(baseConfigure(), combine(paxWebCore(), paxWebRuntime()));
	}

	@Test
	public void checkPaxWebRuntimeMetaTypeWithMetaTypeService() throws IOException {
		Bundle runtime = bundle("org.ops4j.pax.web.pax-web-runtime");
		MetaTypeInformation info = metaTypeService.getMetaTypeInformation(runtime);
		ObjectClassDefinition ocd = info.getObjectClassDefinition(PaxWebConstants.PID, null);
		AttributeDefinition[] attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);

		assertTrue(attributes.length > 0);

		AttributeDefinition ad = Arrays.stream(attributes)
				.filter(a -> a.getID().equals(PaxWebConfig.PID_CFG_TEMP_DIR)).findFirst().orElse(null);
		assertNotNull(ad);

		assertThat(ad.getCardinality(), equalTo(0));
		assertThat("Property should not be resolved", ad.getDefaultValue()[0], equalTo("${java.io.tmpdir}"));
	}

	/**
	 * Having only pax-web-runtime without actual implementation that could register
	 * {@link }ServerControllerFactory}, we can't obtain an instance of {@link HttpService}.
	 */
	@Test
	public void checkNoHttpServiceAvailable() {
		ServiceReference<HttpService> ref = context.getServiceReference(HttpService.class);
		assertNull(ref);
	}

	/**
	 * Register and unregister {@link ServerControllerFactory} to check if {@link HttpService} is available.
	 * @throws Exception
	 */
	@Test
	public void registerFakeServerControllerFactory() throws Exception {
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);

		ServiceListener sl1 = (event) -> {
			if (event.getType() == ServiceEvent.REGISTERED) {
				String[] classes = (String[]) event.getServiceReference().getProperty(Constants.OBJECTCLASS);
				if (Arrays.asList(classes).contains("org.ops4j.pax.web.service.http.HttpService")) {
					latch1.countDown();
				}
			}
		};
		context.addServiceListener(sl1);

		// don't change to lambda, otherwise maven-failsafe-plugin fails
		@SuppressWarnings("Convert2Lambda")
		ServiceRegistration<ServerControllerFactory> reg = context.registerService(ServerControllerFactory.class, new ServerControllerFactory() {
			@Override
			public ServerController createServerController(org.ops4j.pax.web.service.spi.config.Configuration config) {
				return new MockServerController() {
					@Override
					public void sendBatch(Batch batch) {
						latch2.countDown();
					}
				};
			}
		}, null);

		assertTrue(latch1.await(5, TimeUnit.SECONDS));
		context.removeServiceListener(sl1);

		ServiceReference<HttpService> ref = context.getServiceReference(HttpService.class);
		HttpService http = context.getService(ref);

		http.registerServlet("/s1", new HttpServlet() { }, null, null);
		assertTrue(latch2.await(5, TimeUnit.SECONDS));

		ServiceListener sl2 = (event) -> {
			if (event.getType() == ServiceEvent.UNREGISTERING) {
				String[] classes = (String[]) event.getServiceReference().getProperty(Constants.OBJECTCLASS);
				if (Arrays.asList(classes).contains("org.ops4j.pax.web.service.http.HttpService")) {
					latch3.countDown();
				}
			}
		};
		context.addServiceListener(sl2);

		reg.unregister();

		assertTrue(latch3.await(5, TimeUnit.SECONDS));
		context.removeServiceListener(sl2);

		ref = context.getServiceReference(HttpService.class);
		assertNull("HttpService should no longer be available", ref);
	}

	/**
	 * Register and unregister {@link ServerControllerFactory} to check if {@link HttpService} is available.
	 * @throws Exception
	 */
	@Test
	public void checkTrackersForHttpServiceFactory() throws Exception {
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);

		ServiceListener sl1 = (event) -> {
			if (event.getType() == ServiceEvent.REGISTERED) {
				String[] classes = (String[]) event.getServiceReference().getProperty(Constants.OBJECTCLASS);
				if (Arrays.asList(classes).contains("org.ops4j.pax.web.service.http.HttpService")) {
					latch1.countDown();
				}
			}
		};
		context.addServiceListener(sl1);

		ServiceRegistration<ServerControllerFactory> reg = context.registerService(ServerControllerFactory.class, new ServerControllerFactory() {
			@Override
			public ServerController createServerController(org.ops4j.pax.web.service.spi.config.Configuration config) {
				return new MockServerController() {
					@Override
					public void sendBatch(Batch batch) {
						super.sendBatch(batch);
						latch2.countDown();
					}
				};
			}
		}, null);

		assertTrue(latch1.await(5, TimeUnit.SECONDS));
		context.removeServiceListener(sl1);

		// take 2 different bundles
		File f1 = new File("target/bundles/empty-bundle1.jar");
		File f2 = new File("target/bundles/empty-bundle2.jar");
		Bundle b1 = context.installBundle(f1.toURI().toURL().toString(), new FileInputStream(f1));
		Bundle b2 = context.installBundle(f2.toURI().toURL().toString(), new FileInputStream(f2));
		b1.start();
		b2.start();

		// service tracker for HttpService - it will trigger once. Not for both bundle-scoped HttpService services,
		// but for single ServiceFactory that produces bundle-scoped HttpService instances
		ServiceTracker<HttpService, HttpService> tracker = new ServiceTracker<>(context, HttpService.class, new ServiceTrackerCustomizer<HttpService, HttpService>() {
			@Override
			public HttpService addingService(ServiceReference<HttpService> reference) {
				boolean factory = !reference.getProperty(Constants.SERVICE_SCOPE).equals(Constants.SCOPE_SINGLETON);
				LOG.info("adding service {}{}", reference, factory ? " (factory)" : "");
				latch3.countDown();
				return null;
			}

			@Override
			public void modifiedService(ServiceReference<HttpService> reference, HttpService service) {
				LOG.info("modified service {} / {}", reference, service);
			}

			@Override
			public void removedService(ServiceReference<HttpService> reference, HttpService service) {
				LOG.info("removed service {} / {}", reference, service);
			}
		});
		tracker.open();

		// we can retrieve HttpService OSGi services now - it is bundle scoped, so we can
		// retrieve different services from single reference.
		ServiceReference<HttpService> ref1 = b1.getBundleContext().getServiceReference(HttpService.class);
		ServiceReference<HttpService> ref2 = b2.getBundleContext().getServiceReference(HttpService.class);
		HttpService http1 = b1.getBundleContext().getService(ref1);
		HttpService http2 = b2.getBundleContext().getService(ref2);
		assertNotNull(http1);
		assertNotNull(http2);
		assertSame(ref1, ref2);
		assertNotSame(http1, http2);

		b1.stop();
		b2.stop();

		reg.unregister();

		tracker.close();

		assertTrue(latch3.await(5, TimeUnit.SECONDS));
		assertThat(latch3.getCount(), equalTo(0L));

		ServiceReference<HttpService> ref = context.getServiceReference(HttpService.class);
		assertNull("HttpService should no longer be available", ref);
	}

	/**
	 * Method called by surefire/failsafe, not to be used by @Test method inside OSGi container (even if native)
	 * @throws IOException
	 */
	public static void prepareBundles() throws IOException {
		File dir = new File("target/bundles");
		dir.mkdirs();
		FileUtils.cleanDirectory(dir);
		InputStream bundle1 = TinyBundles.bundle()
				.set("Bundle-ManifestVersion", "2")
				.set("Bundle-SymbolicName", "b1")
				.build();
		File f1 = new File(dir, "empty-bundle1.jar");
		f1.delete();
		try (FileOutputStream fos = new FileOutputStream(f1)) {
			IOUtils.copy(bundle1, fos);
		}

		InputStream bundle2 = TinyBundles.bundle()
				.set("Bundle-ManifestVersion", "2")
				.set("Bundle-SymbolicName", "b2")
				.build();
		File f2 = new File(dir, "empty-bundle2.jar");
		f2.delete();
		try (FileOutputStream fos = new FileOutputStream(f2)) {
			IOUtils.copy(bundle2, fos);
		}
	}

}
