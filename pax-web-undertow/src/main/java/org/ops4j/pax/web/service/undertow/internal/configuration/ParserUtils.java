/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.undertow.internal.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class ParserUtils {

	private ParserUtils() {
	}

	public static Map<QName, String> toMap(Attributes atts) {
		if (atts == null || atts.getLength() == 0) {
			return Collections.emptyMap();
		}
		Map<QName, String> result = new HashMap<>();
		for (int i = 0; i < atts.getLength(); i++) {
			String ln = atts.getLocalName(i);
			String n = atts.getURI(i);
			String v = atts.getValue(i);
			if (n == null) {
				result.put(new QName(ln), v);
			} else {
				result.put(new QName(n, ln), v);
			}
		}

		return result;
	}

	public static Integer toInteger(String v, Locator locator, Integer defaultValue) throws SAXParseException {
		if (v == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(v);
		} catch (NumberFormatException e) {
			throw new SAXParseException("Can't parse " + v + " as integer value", locator);
		}
	}

	public static Long toLong(String v, Locator locator, Long defaultValue) throws SAXParseException {
		if (v == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(v);
		} catch (NumberFormatException e) {
			throw new SAXParseException("Can't parse " + v + " as long value", locator);
		}
	}

	public static String toStringValue(String v, Locator locator, String defaultValue) {
		if (v == null) {
			return defaultValue;
		}
		return v;
	}

	public static boolean toBoolean(String v, Locator locator, boolean defaultValue) {
		if (v == null) {
			return defaultValue;
		}
		return Boolean.parseBoolean(v);
	}

	public static List<String> toStringList(String v, Locator locator) {
		if (v == null || "".equals(v.trim())) {
			return Collections.emptyList();
		}

		return Arrays.asList(v.split("\\s+"));
	}

	public static <T> T ensureStack(Deque<Object> stack, Class<T> cls, String localName, Locator locator) throws SAXParseException {
		return ensureStack(stack, cls, localName, locator, false);
	}

	public static <T> T ensureStack(Deque<Object> stack, Class<T> cls, String localName, Locator locator, boolean pop) throws SAXParseException {
		if (stack == null || stack.isEmpty() || !(cls.equals(stack.peek().getClass()))) {
			throw new SAXParseException("Unexpected element \"" + localName + "\"", locator);
		}
		if (pop) {
			return cls.cast(stack.pop());
		} else {
			return cls.cast(stack.peek());
		}
	}

}
