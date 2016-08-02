/*
 * Copyright 2014 Harald Wellmann.
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
package org.ops4j.pax.web.jaas.util;


public class HexConverter {

	private static final byte[] HEX_BYTES = new byte[]
			{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	/**
	 * Take the supplied byte array and convert it to to a byte array of the encoded
	 * hex values.
	 * <br>
	 * Each byte on the incoming array will be converted to two bytes on the return
	 * array.
	 *
	 * @param toBeConverted - the bytes to be encoded.
	 * @return the encoded byte array.
	 */
	public static byte[] convertToHexBytes(byte[] toBeConverted) {
		if (toBeConverted == null) {
			throw new NullPointerException("Parameter to be converted can not be null");
		}

		byte[] converted = new byte[toBeConverted.length * 2];
		for (int i = 0; i < toBeConverted.length; i++) {
			byte b = toBeConverted[i];
			converted[i * 2] = HEX_BYTES[b >> 4 & 0x0F];
			converted[i * 2 + 1] = HEX_BYTES[b & 0x0F];
		}

		return converted;
	}
}
