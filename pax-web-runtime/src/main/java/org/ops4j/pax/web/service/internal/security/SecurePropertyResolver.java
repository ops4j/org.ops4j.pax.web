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
package org.ops4j.pax.web.service.internal.security;

import org.jasypt.commons.CommonUtils;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.util.property.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to manually configure (by pax-web-runtime) an instance of {@link StringEncryptor}.
 */
public class SecurePropertyResolver implements PropertyResolver {

	public static final Logger LOG = LoggerFactory.getLogger(SecurePropertyResolver.class);

	private String prefix;
	private String suffix;

	private final PropertyResolver delegate;

	private final StringEncryptor encryptor;

	public SecurePropertyResolver(PropertyResolver delegate) {
		this.delegate = delegate;

		prefix = delegate.get(PaxWebConfig.PID_CFG_ENC_PREFIX);
		if (prefix == null || "".equals(prefix)) {
			prefix = "ENC(";
		}
		suffix = delegate.get(PaxWebConfig.PID_CFG_ENC_SUFFIX);
		if (suffix == null || "".equals(suffix)) {
			suffix = ")";
		}

		String provider = delegate.get(PaxWebConfig.PID_CFG_ENC_PROVIDER);
		if ("SunJCE".equals(provider)) {
			provider = null;
		}
		String algorithm = delegate.get(PaxWebConfig.PID_CFG_ENC_ALGORITHM);
		if (algorithm == null || "".equals(algorithm.trim())) {
			algorithm = "PBEWithHmacSHA256AndAES_128";
		}
		String ic = delegate.get(PaxWebConfig.PID_CFG_ENC_ITERATION_COUNT);
		String ep = delegate.get(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD_ENV);
		String sp = delegate.get(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD_SYS);
		String mp = delegate.get(PaxWebConfig.PID_CFG_ENC_MASTERPASSWORD);

		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		EnvironmentStringPBEConfig config = new EnvironmentStringPBEConfig();
		config.setAlgorithm(algorithm);
		if (provider != null) {
			config.setProviderName(provider);
		}
		if (ic != null && !"".equals(ic)) {
			try {
				Integer icv = Integer.parseInt(ic);
				config.setKeyObtentionIterations(icv);
			} catch (NumberFormatException e) {
				LOG.warn("Illegal value for iteration count ({}), setting the value to 1000", ic);
				config.setKeyObtentionIterations(1000);
			}
		} else {
			config.setKeyObtentionIterations(1000);
		}
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(StringEncryptor.class.getClassLoader());
			config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
			config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

		config.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_BASE64);

		if (ep != null && !"".equals(ep)) {
			config.setPasswordEnvName(ep);
		} else if (sp != null && !"".equals(sp)) {
			config.setPasswordSysPropertyName(sp);
		} else if (mp != null && !"".equals(mp)) {
			config.setPasswordCharArray(mp.toCharArray());
		}

		encryptor.setConfig(config);

		this.encryptor = encryptor;
	}

	public SecurePropertyResolver(PropertyResolver resolver, StringEncryptor encryptor) {
		this.delegate = resolver;
		this.encryptor = encryptor;

		prefix = delegate.get(PaxWebConfig.PID_CFG_ENC_PREFIX);
		if (prefix == null || "".equals(prefix)) {
			prefix = "ENC(";
		}
		suffix = delegate.get(PaxWebConfig.PID_CFG_ENC_SUFFIX);
		if (suffix == null || "".equals(suffix)) {
			suffix = ")";
		}
	}

	public static PropertyResolver wrap(PropertyResolver resolver) {
		return new SecurePropertyResolver(resolver);
	}

	public static PropertyResolver wrap(PropertyResolver resolver, Object encryptor) {
		if (!StringEncryptor.class.isAssignableFrom(encryptor.getClass())) {
			throw new IllegalArgumentException("Can't use " + encryptor + " - it is not an instance of org.jasypt.encryption.StringEncryptor");
		}
		return new SecurePropertyResolver(resolver, (StringEncryptor) encryptor);
	}

	@Override
	public String get(String property) {
		if (property.startsWith(PaxWebConfig.PID_CFG_ENC_PROPERTY_PREFIX)) {
			// just delegate
			return delegate.get(property);
		}

		String value = delegate.get(property);
		if (value == null) {
			return null;
		}

		if (value.startsWith(prefix) && value.endsWith(suffix)) {
			return encryptor.decrypt(value.substring(prefix.length(), value.length() - suffix.length()));
		}

		return value;
	}

}
