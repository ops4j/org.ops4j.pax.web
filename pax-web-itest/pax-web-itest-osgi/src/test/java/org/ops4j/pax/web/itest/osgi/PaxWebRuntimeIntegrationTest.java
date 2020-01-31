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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.osgi.support.MockServerController;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This test check whether pax-web-spi contains proper private packages and whether the imports are sufficient
 * to perform various tasks related to class/annotation discovery.
 */
@RunWith(PaxExam.class)
public class PaxWebRuntimeIntegrationTest extends AbstractControlledBase2 {

	public static Logger LOG = LoggerFactory.getLogger(PaxWebRuntimeIntegrationTest.class);

	@Inject
	private MetaTypeService metaTypeService;

	@Configuration
	public Option[] configure() {
		return combine(baseConfigure(), combine(paxWebCore(), paxWebRuntime(), metaTypeService()));
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
				if (Arrays.asList(classes).contains("org.osgi.service.http.HttpService")) {
					latch1.countDown();
				}
			}
		};
		context.addServiceListener(sl1);

		ServiceRegistration<ServerControllerFactory> reg = context.registerService(ServerControllerFactory.class, new ServerControllerFactory() {
			@Override
			public ServerController createServerController(ServerModel serverModel) {
				return new MockServerController() {
					@Override
					public void addServlet(ServletModel model) {
						latch2.countDown();
						super.addServlet(model);
					}
				};
			}
		}, null);

		assertTrue(latch1.await(5, TimeUnit.SECONDS));
		context.removeServiceListener(sl1);

		ServiceReference<HttpService> ref = context.getServiceReference(HttpService.class);
		HttpService http = context.getService(ref);

		http.registerServlet("/s1", new HttpServlet() {}, null, null);
		assertTrue(latch2.await(5, TimeUnit.SECONDS));

		ServiceListener sl2 = (event) -> {
			if (event.getType() == ServiceEvent.UNREGISTERING) {
				String[] classes = (String[]) event.getServiceReference().getProperty(Constants.OBJECTCLASS);
				if (Arrays.asList(classes).contains("org.osgi.service.http.HttpService")) {
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

}
