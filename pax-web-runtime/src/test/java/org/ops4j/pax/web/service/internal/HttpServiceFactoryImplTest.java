/* Copyright 2007 Alin Dreghiciu.
 *
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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

public class HttpServiceFactoryImplTest {

	private StoppableHttpServiceFactory underTest;
	private Bundle bundle;
	private ServiceRegistration<HttpService> serviceRegistration;
	private StoppableHttpService httpService;
//
//	@SuppressWarnings("unchecked")
//	@Before
//	public void setUp() {
//		bundle = createMock(Bundle.class);
//		serviceRegistration = createMock(ServiceRegistration.class);
//		httpService = createMock(StoppableHttpService.class);
//		underTest = new HttpServiceFactoryImpl() {
//			@Override
//			HttpService createService(Bundle serviceBundle) {
//				return httpService;
//			}
//		};
//	}
//
//	@Test
//	public void checkGetServiceFlow() {
//		// prepare
//		replay(bundle, serviceRegistration, httpService);
//		// execute
//		Object result = underTest.getService(bundle, serviceRegistration);
//		assertNotNull("expect not null", result);
//		// verify
//		verify(bundle, serviceRegistration, httpService);
//	}
//
//	@Test
//	public void checkUngetServiceFlow() {
//		// prepare
//		httpService.stop();
//		replay(bundle, serviceRegistration, httpService);
//		// execute
//		underTest.ungetService(bundle, serviceRegistration, httpService);
//		// verify
//		verify(bundle, serviceRegistration, httpService);
//	}

}
