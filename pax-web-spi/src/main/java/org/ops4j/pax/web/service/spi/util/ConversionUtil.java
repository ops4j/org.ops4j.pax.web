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
package org.ops4j.pax.web.service.spi.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversionUtil {

	private static final Logger LOG = LoggerFactory
			.getLogger(ConversionUtil.class);

	private ConversionUtil() {
		super();
	}

	@SuppressWarnings("rawtypes")
	public static Map<String, String> convertToMap(
			final Dictionary<String, ?> dictionary) {
		Map<String, String> converted = new HashMap<>();
		if (dictionary != null) {
			Enumeration enumeration = dictionary.keys();
			try {
				while (enumeration.hasMoreElements()) {
					String key = (String) enumeration.nextElement();

					Object val = dictionary.get(key);
					String value = null;

					if (val instanceof String) {
						value = (String) val;
					} else {
						LOG.warn(
								"Beware dictionary value for key {} isn't of type String",
								key);
						value = val.toString();
					}
					converted.put(key, value);
				}
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(
						"Invalid init params for the servlet. The key and value must be Strings.");
			}
		}
		return converted;
	}

}
