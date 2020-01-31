/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.internal;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ConfigurationImplTest {

	@Test
	public void immediateProperties() throws NoSuchFieldException, IllegalAccessException {
		Dictionary<String, String> props = new Hashtable<>();
		props.put(PaxWebConfig.PID_CFG_HTTP_PORT, "1234");
		PropertyResolver resolver = new DictionaryPropertyResolver(props);

		Configuration config = ConfigurationBuilder.getConfiguration(resolver);
		Field f = ConfigurationImpl.class.getDeclaredField("propertyResolver");
		f.setAccessible(true);
		f.set(config, null);

		assertThat("Should be eagerly fetched and property resolver is not needed",
				config.server().getHttpPort(), equalTo(1234));
		try {
			config.get("unknown property", String.class);
			fail("Should fail on missing property resolver");
		} catch (IllegalStateException ignored) {
		}
	}

}
