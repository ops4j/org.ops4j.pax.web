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
package org.ops4j.pax.web.service.internal.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Objects;

public class Utils {

	private Utils() {}

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

}
