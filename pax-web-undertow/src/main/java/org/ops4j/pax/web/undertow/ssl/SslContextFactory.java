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
package org.ops4j.pax.web.undertow.ssl;

import static org.ops.pax.web.spi.WebContainerConstants.PROPERTY_SSL_KEYSTORE;
import static org.ops.pax.web.spi.WebContainerConstants.PROPERTY_SSL_KEYSTORE_TYPE;
import static org.ops.pax.web.spi.WebContainerConstants.PROPERTY_SSL_PASSWORD;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.ops4j.util.property.PropertyResolver;

public class SslContextFactory {

    public final static TrustManager[] TRUST_ALL_CERTS = new X509TrustManager[] { new DummyTrustManager() };

    private PropertyResolver resolver;

    public SslContextFactory(PropertyResolver resolver) {
        this.resolver = resolver;
    }

    public SSLContext createSslContext() throws IOException {
        String keyStoreName = resolver.get(PROPERTY_SSL_KEYSTORE);
        String keyStoreType = resolver.get(PROPERTY_SSL_KEYSTORE_TYPE);
        String keyStorePassword = resolver.get(PROPERTY_SSL_PASSWORD);
        KeyStore keyStore = loadKeyStore(keyStoreName, keyStoreType, keyStorePassword);

        KeyManager[] keyManagers = buildKeyManagers(keyStore, keyStorePassword.toCharArray());
        TrustManager[] trustManagers = buildTrustManagers(null);

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        }
        catch (NoSuchAlgorithmException | KeyManagementException exc) {
            throw new IOException("Unable to create and initialise the SSLContext", exc);
        }

        return sslContext;
    }

    private KeyStore loadKeyStore(final String location, String type, String storePassword)
        throws IOException {
        String url = location;
        if (url.indexOf(':') == -1) {
            url = "file:" + location;
        }

        final InputStream stream = new URL(url).openStream();
        try {
            KeyStore loadedKeystore = KeyStore.getInstance(type);
            loadedKeystore.load(stream, storePassword.toCharArray());
            return loadedKeystore;
        }
        catch (KeyStoreException | NoSuchAlgorithmException | CertificateException exc) {
            throw new IOException(String.format("Unable to load KeyStore %s", location), exc);
        }
        finally {
            stream.close();
        }
    }

    private static TrustManager[] buildTrustManagers(final KeyStore trustStore) throws IOException {
        TrustManager[] trustManagers = null;
        if (trustStore == null) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }
            catch (NoSuchAlgorithmException | KeyStoreException exc) {
                throw new IOException("Unable to initialise TrustManager[]", exc);
            }
        }
        else {
            trustManagers = TRUST_ALL_CERTS;
        }
        return trustManagers;
    }

    private static KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] storePassword)
        throws IOException {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, storePassword);
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException exc) {
            throw new IOException("Unable to initialise KeyManager[]", exc);
        }
        return keyManagers;
    }

}
