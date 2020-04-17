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
package org.ops4j.pax.web.service.internal;

import org.apache.commons.httpclient.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.web.service.DefaultHttpServiceConfiguration;
import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.SimpleHttpServiceConfiguration;
import org.osgi.framework.Bundle;

public class IntegrationTests {

	private RegistrationsCluster m_registrationsCluster;
	private ServerController m_serverController;
	private Bundle m_bundle;
	protected HttpClient m_client;
	protected HttpServiceProxy m_httpService;

	@Before
	public void setUp() {
		System.out.println("--------------------------------------------------------------------------- start setUp");
		m_registrationsCluster = new RegistrationsClusterImpl();
		m_serverController = new ServerControllerImpl(
				new JettyFactoryImpl()
		);
		HttpServiceConfigurer configurer = new HttpServiceConfigurerImpl(m_serverController);
		SimpleHttpServiceConfiguration config =
				new SimpleHttpServiceConfiguration(new DefaultHttpServiceConfiguration());
		config.setSessionTimeout(1);
		configurer.configure(config);
		m_bundle = org.easymock.EasyMock.createMock(Bundle.class);
		m_httpService = new HttpServiceProxy(
				new StartedHttpService(m_bundle, m_serverController, m_registrationsCluster)
		);
		m_client = new HttpClient();
		System.out.println("----------------------------------------------------------------------------- end setUp");
	}

	@After
	public void tearDown() {
		System.out.println("------------------------------------------------------------------------ start tearDown");
		m_serverController.stop();
		m_httpService.stop();

		m_client = null;
		m_bundle = null;
		m_registrationsCluster = null;
		m_serverController = null;
		m_httpService = null;

		System.out.println("-------------------------------------------------------------------------- end tearDown");
	}
}
