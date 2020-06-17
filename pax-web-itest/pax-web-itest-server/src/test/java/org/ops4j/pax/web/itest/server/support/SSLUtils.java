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
package org.ops4j.pax.web.itest.server.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instead of storing certificates for Pax Web tests in {@code src/test/resources}, we'll generate them here
 * (if needed), showing the <em>canonical</em> way to do it.
 */
public class SSLUtils {

	public static final Logger LOG = LoggerFactory.getLogger(SSLUtils.class);
	public static final DateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private SSLUtils() {
	}

	public static void generateKeyStores() throws Exception {
		File serverKeystoreFile = new File("target/server.jks");
		File clientKeystoreFile = new File("target/client.jks");
		File clientKeystoreP12File = new File("target/client.p12");

		if (serverKeystoreFile.isFile() && clientKeystoreFile.isFile()) {
			return;
		}

		// sun.security.rsa.RSAKeyPairGenerator
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "SunJSSE");
//		assertThat(getField(kpg, "spi").getClass().getName()).isEqualTo("sun.security.rsa.RSAKeyPairGenerator");

		// 5 key pairs: 1 for CA, 2 for server keystore and 2 for client keystore

		// `openssl genrsa -out ca1.key`
		// $ openssl asn1parse -in ca1.key -i
		//     0:d=0  hl=4 l=1187 cons: SEQUENCE
		//     4:d=1  hl=2 l=   1 prim:  INTEGER           :00
		//     7:d=1  hl=4 l= 257 prim:  INTEGER           :DF93B4A74247...
		//   268:d=1  hl=2 l=   3 prim:  INTEGER           :010001
		//   273:d=1  hl=4 l= 256 prim:  INTEGER           :5A8C09880BD1...
		//   533:d=1  hl=3 l= 129 prim:  INTEGER           :F540222F8DDC...
		//   665:d=1  hl=3 l= 129 prim:  INTEGER           :E9606196298B...
		//   797:d=1  hl=3 l= 128 prim:  INTEGER           :1C3748B797E6...
		//   928:d=1  hl=3 l= 129 prim:  INTEGER           :D8D5F055F12B...
		//  1060:d=1  hl=3 l= 128 prim:  INTEGER           :165E65231AFE...
		//
		// $ openssl asn1parse -in ca1-public.key -i
		//     0:d=0  hl=4 l= 290 cons: SEQUENCE
		//     4:d=1  hl=2 l=  13 cons:  SEQUENCE
		//     6:d=2  hl=2 l=   9 prim:   OBJECT            :rsaEncryption
		//    17:d=2  hl=2 l=   0 prim:   NULL
		//    19:d=1  hl=4 l= 271 prim:  BIT STRING
		//
		// $ openssl asn1parse -in ca1-public.key -i -strparse 19
		//     0:d=0  hl=4 l= 266 cons: SEQUENCE
		//     4:d=1  hl=4 l= 257 prim:  INTEGER           :DF93B4A74247...
		//   265:d=1  hl=2 l=   3 prim:  INTEGER           :010001

		// in Java (RSA/SunJSSE):
		// public key:
		//  - BigInteger n;       // modulus
		//  - BigInteger e;       // public exponent
		// private key:
		//  - BigInteger n;       // modulus
		//  - BigInteger e;       // public exponent
		//  - BigInteger d;       // private exponent
		//  - BigInteger p;       // prime p
		//  - BigInteger q;       // prime q
		//  - BigInteger pe;      // prime exponent p
		//  - BigInteger qe;      // prime exponent q
		//  - BigInteger coeff;   // CRT coeffcient

		// main method to generate key pair
		KeyPair caKeyPair = kpg.generateKeyPair();
		PrivateKey caPrivateKey = caKeyPair.getPrivate(); // sun.security.rsa.RSAPrivateCrtKeyImpl
		PublicKey caPublicKey = caKeyPair.getPublic();    // sun.security.rsa.RSAPublicKeyImpl

		// direct access to data knowing the key hierarchy
		((RSAPublicKey) caPublicKey).getPublicExponent();
		((RSAPrivateKey) caPrivateKey).getPrivateExponent();
		((RSAPrivateCrtKey) caPrivateKey).getPrimeP();
		((RSAPrivateCrtKey) caPrivateKey).getPrimeQ();

//		assertThat(((RSAPrivateKey) caPrivateKey).getModulus()).isEqualTo(((RSAPublicKey) caPublicKey).getModulus());

		// key (actual key) to specification (information allowing to recreate the key)
		KeyFactory kf = KeyFactory.getInstance("RSA", "SunJSSE");
//		assertThat(getField(kf, "spi").getClass().getName()).isEqualTo("sun.security.rsa.RSAKeyFactory");
		// sun.security.rsa.RSAKeyFactory knows which interfaces/classes it can handle and directly takes what's
		// needed, e.g., java.security.interfaces.RSAPrivateCrtKey.getPrimeExponentP() in translateKey()
		// then, after successful translation of the key, it creates instance of
		// java.security.spec.RSAPrivateCrtKeySpec

		RSAPrivateCrtKeySpec caPrivateKeySpec = kf.getKeySpec(caPrivateKey, RSAPrivateCrtKeySpec.class);
//		assertThat(((RSAPrivateKey) caPrivateKey).getModulus()).isEqualTo(caPrivateKeySpec.getModulus());

		KeyPair server1KeyPair = kpg.generateKeyPair();
		PrivateKey server1PrivateKey = server1KeyPair.getPrivate();
		PublicKey server1PublicKey = server1KeyPair.getPublic();
		KeyPair server2KeyPair = kpg.generateKeyPair();
		PrivateKey server2PrivateKey = server2KeyPair.getPrivate();
		PublicKey server2PublicKey = server2KeyPair.getPublic();

		KeyPair client1KeyPair = kpg.generateKeyPair();
		PrivateKey client1PrivateKey = client1KeyPair.getPrivate();
		PublicKey client1PublicKey = client1KeyPair.getPublic();
		KeyPair client2KeyPair = kpg.generateKeyPair();
		PrivateKey client2PrivateKey = client2KeyPair.getPrivate();
		PublicKey client2PublicKey = client2KeyPair.getPublic();

		LOG.info("CA private key format: " + caPrivateKey.getFormat());
		LOG.info("CA public key format: " + caPublicKey.getFormat());
		Files.write(new File("target/ca-public.key").toPath(), caPublicKey.getEncoded());
		Files.write(new File("target/ca-private.key").toPath(), caPrivateKey.getEncoded());

		Files.write(new File("target/server1-private.key").toPath(), server1PrivateKey.getEncoded());
		Files.write(new File("target/client1-private.key").toPath(), client1PrivateKey.getEncoded());
		Files.write(new File("target/server2-private.key").toPath(), server2PrivateKey.getEncoded());
		Files.write(new File("target/client2-private.key").toPath(), client2PrivateKey.getEncoded());

		// ASN.1 structure of encoded Java classes:
		// public key:
		// $ openssl asn1parse -inform der -in ca-public-5901758342174098433.key -i
		//     0:d=0  hl=4 l= 290 cons: SEQUENCE
		//     4:d=1  hl=2 l=  13 cons:  SEQUENCE
		//     6:d=2  hl=2 l=   9 prim:   OBJECT            :rsaEncryption
		//    17:d=2  hl=2 l=   0 prim:   NULL
		//    19:d=1  hl=4 l= 271 prim:  BIT STRING
		//
		// $ openssl asn1parse -inform der -in ca-public-5901758342174098433.key -i -strparse 19
		//     0:d=0  hl=4 l= 266 cons: SEQUENCE
		//     4:d=1  hl=4 l= 257 prim:  INTEGER           :88C1DB1E6C12...
		//   265:d=1  hl=2 l=   3 prim:  INTEGER           :010001
		//
		// private key:
		// $ openssl asn1parse -inform der -in ca-private-7968518175395329768.key -i
		//     0:d=0  hl=4 l=1213 cons: SEQUENCE
		//     4:d=1  hl=2 l=   1 prim:  INTEGER           :00
		//     7:d=1  hl=2 l=  13 cons:  SEQUENCE
		//     9:d=2  hl=2 l=   9 prim:   OBJECT            :rsaEncryption
		//    20:d=2  hl=2 l=   0 prim:   NULL
		//    22:d=1  hl=4 l=1191 prim:  OCTET STRING      [HEX DUMP]:308204A302010002820101...
		//
		// $ openssl asn1parse -inform der -in ca-private-7968518175395329768.key -i -strparse 22
		//     0:d=0  hl=4 l=1187 cons: SEQUENCE
		//     4:d=1  hl=2 l=   1 prim:  INTEGER           :00
		//     7:d=1  hl=4 l= 257 prim:  INTEGER           :88C1DB1E6C12...
		//   268:d=1  hl=2 l=   3 prim:  INTEGER           :010001
		//   273:d=1  hl=4 l= 256 prim:  INTEGER           :3960E4B7B1EE...
		//   533:d=1  hl=3 l= 129 prim:  INTEGER           :DCF0F10D7BEE...
		//   665:d=1  hl=3 l= 129 prim:  INTEGER           :9E75333E7D30...
		//   797:d=1  hl=3 l= 129 prim:  INTEGER           :B4C5D715474F...
		//   929:d=1  hl=3 l= 128 prim:  INTEGER           :06C6580489F1...
		//  1060:d=1  hl=3 l= 128 prim:  INTEGER           :54338EFF7906...

		// Certificates - generated using Bouncycastle, because it's easier

		X500Name caName = new X500Name("cn=CA");
		X500Name server1Name = new X500Name("cn=server1");
		X500Name client1Name = new X500Name("cn=client1");
		X500Name server2Name = new X500Name("cn=server2");
		X500Name client2Name = new X500Name("cn=client2");

		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				caName, BigInteger.ZERO,
				DATE.parse("2019-01-01 00:00:00"), DATE.parse("2039-01-01 00:00:00"),
				caName, caPublicKey);

		// extensions - https://tools.ietf.org/html/rfc5280#section-4.2

		// subject key identifier: https://tools.ietf.org/html/rfc5280#section-4.2.1.2
		// it's mandatory for CA certificates. All certificates signed by this CA certificate should have
		// "authority key identifier" matching "subject key identifier" of the signing CA certificate
		// RFC5280 defines two methods of deriving SKI from public key of CA:
		// 1) SHA1(BIT STRING from public key)
		// 2) 0100 + 60 least significant bits of SHA1(BIT STRING from public key)
		ASN1InputStream caPublicKeyASN1InputStream = new ASN1InputStream(caPublicKey.getEncoded());
		ASN1Sequence s = (ASN1Sequence) caPublicKeyASN1InputStream.readObject();
		DERBitString bitString = (DERBitString) s.getObjectAt(1);
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		byte[] caKeyId = sha1.digest(bitString.getBytes());
		builder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(caKeyId));

		ASN1InputStream server1PublicKeyASN1InputStream = new ASN1InputStream(server1PublicKey.getEncoded());
		s = (ASN1Sequence) server1PublicKeyASN1InputStream.readObject();
		bitString = (DERBitString) s.getObjectAt(1);
		sha1.reset();
		byte[] server1KeyId = sha1.digest(bitString.getBytes());
		ASN1InputStream server2PublicKeyASN1InputStream = new ASN1InputStream(server2PublicKey.getEncoded());
		s = (ASN1Sequence) server2PublicKeyASN1InputStream.readObject();
		bitString = (DERBitString) s.getObjectAt(1);
		sha1.reset();
		byte[] server2KeyId = sha1.digest(bitString.getBytes());
		ASN1InputStream client2PublicKeyASN1InputStream = new ASN1InputStream(client2PublicKey.getEncoded());
		s = (ASN1Sequence) client2PublicKeyASN1InputStream.readObject();
		bitString = (DERBitString) s.getObjectAt(1);
		sha1.reset();
		byte[] client2KeyId = sha1.digest(bitString.getBytes());

		// authority key identifier: https://tools.ietf.org/html/rfc5280#section-4.2.1.1
		// it's used to identify the public key matching the private key used to sign some certificate.
		// this extension is mandatory except for self-signed certificates. But in this case, if present,
		// it should match "subject key identifier"
		builder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(caKeyId));

		// basic constraints: https://tools.ietf.org/html/rfc5280#section-4.2.1.9
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

		// key usage: https://tools.ietf.org/html/rfc5280#section-4.2.1.3
		// CA self-signed certificate created using `openssl` has:
		// X509v3 Key Usage:
		//     Certificate Sign, CRL Sign
		builder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

		// org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
		// openssl by default uses "sha256WithRSAEncryption"
		ContentSigner caContentSigner = new JcaContentSignerBuilder("SHA1WITHRSA").build(caPrivateKey);
		ContentSigner server2ContentSigner = new JcaContentSignerBuilder("SHA1WITHRSA").build(server2PrivateKey);
		ContentSigner client2ContentSigner = new JcaContentSignerBuilder("SHA1WITHRSA").build(client2PrivateKey);

		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();

		X509Certificate caX509Certificate = converter.getCertificate(builder.build(caContentSigner));
		Files.write(new File("target/ca.cer").toPath(), caX509Certificate.getEncoded());
		JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter("target/ca.cer.pem"));
		pemWriter.writeObject(caX509Certificate);
		pemWriter.close();

		// in simplest form (no extensions), it's simply:
		// $ openssl x509 -inform der -in ca.cer -noout -text
		// Certificate:
		//     Data:
		//         Version: 3 (0x2)
		//         Serial Number: 0 (0x0)
		//         Signature Algorithm: sha1WithRSAEncryption
		//         Issuer: CN = CA
		//         Validity
		//             Not Before: Jan  1 00:00:00 2019 GMT
		//             Not After : Jan  1 00:00:00 2039 GMT
		//         Subject: CN = CA
		//         Subject Public Key Info:
		//             Public Key Algorithm: rsaEncryption
		//                 RSA Public-Key: (2048 bit)
		//                 Modulus:
		//                     00:c4:78:dd:84:a1:6f:8f:5b:e2:4d:8e:a7:3e:11:
		//                     c4:22:34:ad:90:89:65:82:c2:be:a6:73:9c:db:1d:
		// ...

		// server and client certificates (2x)
		// server1 and client1 will be signed by CA, server2 and client2 will be self-signed

		JcaX509v3CertificateBuilder server1Builder = new JcaX509v3CertificateBuilder(
				caName, BigInteger.ONE,
				DATE.parse("2019-01-01 00:00:00"), DATE.parse("2039-01-01 00:00:00"),
				server1Name, server1PublicKey);

		JcaX509v3CertificateBuilder client1Builder = new JcaX509v3CertificateBuilder(
				caName, new BigInteger("2"),
				DATE.parse("2019-01-01 00:00:00"), DATE.parse("2039-01-01 00:00:00"),
				client1Name, client1PublicKey);

		JcaX509v3CertificateBuilder server2Builder = new JcaX509v3CertificateBuilder(
				server2Name, BigInteger.ZERO,
				DATE.parse("2019-01-01 00:00:00"), DATE.parse("2039-01-01 00:00:00"),
				server2Name, server2PublicKey);

		JcaX509v3CertificateBuilder client2Builder = new JcaX509v3CertificateBuilder(
				client2Name, BigInteger.ZERO,
				DATE.parse("2019-01-01 00:00:00"), DATE.parse("2039-01-01 00:00:00"),
				client2Name, client2PublicKey);

		server1Builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
		server1Builder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment));
		server1Builder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(caKeyId));
		server1Builder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(server1KeyId));
		// https://tools.ietf.org/html/rfc5280#section-4.2.1.12
		server1Builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
		// https://tools.ietf.org/html/rfc5280#section-4.2.1.6
		server1Builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1")));

		client1Builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
		client1Builder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment));
		client1Builder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(caKeyId));
		client1Builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));

		server2Builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
		server2Builder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment));
		server2Builder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(server2KeyId));
		server2Builder.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(server2KeyId));
		server2Builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
		server2Builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1")));

		client2Builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
		client2Builder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment));
		client2Builder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(client2KeyId));
		client2Builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));

		// server1 and client1 are signed by CA
		X509Certificate server1X509Certificate = converter.getCertificate(server1Builder.build(caContentSigner));
		X509Certificate client1X509Certificate = converter.getCertificate(client1Builder.build(caContentSigner));
		// server2 and client2 are self-signed
		X509Certificate server2X509Certificate = converter.getCertificate(server2Builder.build(server2ContentSigner));
		X509Certificate client2X509Certificate = converter.getCertificate(client2Builder.build(client2ContentSigner));

		Files.write(new File("target/server1.cer").toPath(), server1X509Certificate.getEncoded());
		Files.write(new File("target/client1.cer").toPath(), client1X509Certificate.getEncoded());
		Files.write(new File("target/server2.cer").toPath(), server2X509Certificate.getEncoded());
		Files.write(new File("target/client2.cer").toPath(), client2X509Certificate.getEncoded());

		// turn X509 certificates into JKS keystores

		// https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#CertificateFactory
		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		Certificate caCer = cf.generateCertificate(new FileInputStream("target/ca.cer"));
		Certificate server1Cer = cf.generateCertificate(new FileInputStream("target/server1.cer"));
		Certificate client1Cer = cf.generateCertificate(new FileInputStream("target/client1.cer"));
		Certificate server2Cer = cf.generateCertificate(new FileInputStream("target/server2.cer"));
		Certificate client2Cer = cf.generateCertificate(new FileInputStream("target/client2.cer"));

		kf = KeyFactory.getInstance("RSA", "SunJSSE");

		// java.security.spec.InvalidKeySpecException: Only RSAPrivate(Crt)KeySpec and PKCS8EncodedKeySpec supported for RSA private keys
//        PrivateKey serverKey = kf.generatePrivate(new X509EncodedKeySpec(Files.readAllBytes(new File("target/server-private.key").toPath())));
		PrivateKey server1Key = kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(new File("target/server1-private.key").toPath())));
		PrivateKey client1Key = kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(new File("target/client1-private.key").toPath())));
		PrivateKey server2Key = kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(new File("target/server2-private.key").toPath())));
		PrivateKey client2Key = kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(new File("target/client2-private.key").toPath())));

		KeyStore serverKeystore = KeyStore.getInstance("JKS", "SUN");
		KeyStore clientKeystore = KeyStore.getInstance("JKS", "SUN");
		KeyStore clientP12Keystore = KeyStore.getInstance("PKCS12");

		serverKeystore.load(null, null);
		serverKeystore.setCertificateEntry("ca", caCer);
		serverKeystore.setKeyEntry("server-self-signed", server2Key, "passw0rd".toCharArray(), new Certificate[] { server2Cer });
		serverKeystore.setKeyEntry("server", server1Key, "passw0rd".toCharArray(), new Certificate[] { server1Cer });

		clientKeystore.load(null, null);
		clientKeystore.setCertificateEntry("ca", caCer);
		clientKeystore.setKeyEntry("client-self-signed", client2Key, "passw0rd".toCharArray(), new Certificate[] { client2Cer });
		clientKeystore.setKeyEntry("client", client1Key, "passw0rd".toCharArray(), new Certificate[] { client1Cer });

		clientP12Keystore.load(null, null);
		clientP12Keystore.setCertificateEntry("ca", caCer);
		clientP12Keystore.setKeyEntry("client-self-signed", client2Key, "passw0rd".toCharArray(), new Certificate[] { client2Cer });
		clientP12Keystore.setKeyEntry("client", client1Key, "passw0rd".toCharArray(), new Certificate[] { client1Cer });

		serverKeystore.store(new FileOutputStream(serverKeystoreFile), "passw0rd".toCharArray());
		clientKeystore.store(new FileOutputStream(clientKeystoreFile), "passw0rd".toCharArray());
		clientP12Keystore.store(new FileOutputStream(clientKeystoreP12File), "passw0rd".toCharArray());
	}

}
