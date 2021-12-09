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

import javax.crypto.SecretKeyFactory;

import org.jasypt.commons.CommonUtils;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JasyptTest {

	public static final Logger LOG = LoggerFactory.getLogger(JasyptTest.class);

	private static String[] algorithms;
	private static String[] ivAlgorithms;
	private static String[] saltAlgorithms;

	@BeforeClass
	public static void algorithms() {
		// SecretKeyFactory algorithms for JDK 1.8, 11 and 17 that use javax.crypto.spec.PBEKeySpec
		algorithms = new String[] {
				"PBEWithMD5AndDES",
				"PBEWithMD5AndTripleDES",
				"PBEWithSHA1AndDESede",
				"PBEWithSHA1AndRC2_128",
				"PBEWithSHA1AndRC2_40",
				"PBEWithSHA1AndRC4_128",
				"PBEWithSHA1AndRC4_40"
		};
		ivAlgorithms = new String[] {
				"PBEWithHmacSHA1AndAES_128",
				"PBEWithHmacSHA1AndAES_256",
				"PBEWithHmacSHA224AndAES_128",
				"PBEWithHmacSHA224AndAES_256",
				"PBEWithHmacSHA256AndAES_128",
				"PBEWithHmacSHA256AndAES_256",
				"PBEWithHmacSHA384AndAES_128",
				"PBEWithHmacSHA384AndAES_256",
				"PBEWithHmacSHA512AndAES_128",
				"PBEWithHmacSHA512AndAES_256"
		};
		// these algorithms can't be used... (salt is required at javax.crypto.SecretKeyFactory.generateSecret() stage)
		// but Jasypt sets only the password here.
		// salt/ic/iv are used only when encrypting the data using PBE password.
		saltAlgorithms = new String[] {
				"PBKDF2WithHmacSHA1",
				"PBKDF2WithHmacSHA224",
				"PBKDF2WithHmacSHA256",
				"PBKDF2WithHmacSHA384",
				"PBKDF2WithHmacSHA512"
		};
	}

	@Test
	public void encryptionDecryption() throws Exception {
		LOG.info("== Algorithms that do not require IV");
		for (String alg : algorithms) {
			StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
			EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
			config.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_BASE64);
			config.setKeyObtentionIterations(StandardPBEByteEncryptor.DEFAULT_KEY_OBTENTION_ITERATIONS);
			config.setAlgorithm(alg);
			config.setPassword("masterpasswordfortest");
			enc.setConfig(config);
			LOG.info("alg: {}, provider: {}, encrypt(passw0rd) = {}", alg, SecretKeyFactory.getInstance(alg).getProvider().getName(),
					enc.encrypt("passw0rd"));
		}
		LOG.info("== Algorithms that require IV");
		for (String alg : ivAlgorithms) {
			StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
			EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
			config.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_BASE64);
			config.setKeyObtentionIterations(StandardPBEByteEncryptor.DEFAULT_KEY_OBTENTION_ITERATIONS);
			config.setAlgorithm(alg);
			config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
			config.setPassword("masterpasswordfortest");
			enc.setConfig(config);
			LOG.info("alg: {}, provider: {}, encrypt(passw0rd) = {}", alg, SecretKeyFactory.getInstance(alg).getProvider().getName(),
					enc.encrypt("passw0rd"));
		}
		LOG.info("== Algorithms that require salt (WARNs expected)");
		for (String alg : saltAlgorithms) {
			StandardPBEStringEncryptor enc = new StandardPBEStringEncryptor();
			EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
			config.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_BASE64);
			config.setKeyObtentionIterations(StandardPBEByteEncryptor.DEFAULT_KEY_OBTENTION_ITERATIONS);
			// javax.crypto.SecretKeyFactory.getInstance()
			config.setAlgorithm(alg);
			config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
			config.setPassword("masterpasswordfortest");
			enc.setConfig(config);
			try {
				LOG.info("alg: {}, encrypt(passw0rd) = {}", alg, enc.encrypt("passw0rd"));
			} catch (EncryptionInitializationException e) {
				LOG.warn("alg: {}, enc initialization error: {}", alg, e.getMessage());
			}
		}
	}

}
