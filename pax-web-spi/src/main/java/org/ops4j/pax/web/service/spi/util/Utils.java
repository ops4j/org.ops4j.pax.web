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
package org.ops4j.pax.web.service.spi.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.servlet.ServletConfig;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Utils {

	private static final Bundle bundle = FrameworkUtil.getBundle(Utils.class);

	private Utils() { }

	public static boolean same(Dictionary<String, ?> cfg1, Dictionary<String, ?> cfg2) {
		if (cfg1 == null) {
			return cfg2 == null;
		} else if (cfg2 == null) {
			return false;
		} else if (cfg1.size() != cfg2.size()) {
			return false;
		} else {
			boolean result = true;
			Enumeration<String> keys = cfg1.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				Object v1 = cfg1.get(key);
				Object v2 = cfg2.get(key);
				result = Objects.equals(v1, v2);
				if (!result) {
					break;
				}
			}
			return result;
		}
	}

	public static boolean same(Object v1, Object v2) {
		return Objects.equals(v1, v2);
	}

	/**
	 * <p>Helper method to convert incoming {@link Dictionary} with unspecified types to map of Strings.</p>
	 *
	 * <p>Especially useful when translating from ConfigurationAdmin configs to maps specified at e.g.,
	 * {@link ServletConfig#getInitParameterNames()}.</p>
	 *
	 * @param dictionary dictionary of potentially diffent types of keys and values
	 * @return map of Strings
	 */
	public static Map<String, String> toMap(final Dictionary<?, ?> dictionary) {
		Map<String, String> converted = new HashMap<>();
		if (dictionary != null) {
			Enumeration<?> enumeration = dictionary.keys();
			while (enumeration.hasMoreElements()) {
				Object k = enumeration.nextElement();
				Object v = dictionary.get(k);

				String key = null;
				String value = null;

				if (k instanceof String) {
					key = (String) k;
				} else {
					// should not be null
					key = k.toString();
				}
				if (v instanceof String) {
					value = (String) v;
				} else {
					value = v == null ? null : v.toString();
				}
				converted.put(key, value);
			}
		}

		return converted;
	}

	public static String resolve(String value) {
		Map<String, String> props = new HashMap<>();
		props.put("_v", value);
		InterpolationHelper.performSubstitution(props, bundle == null ? null : bundle.getBundleContext());
		return props.get("_v");
	}

}
