# CA, pwd: secret
openssl req -new -x509 -outform pem -out ca.cer -keyout ca.key -newkey rsa:2048 -days $((10*365+10)) -subj '/C=PL/O=OPS4J/CN=CA-test-Undertow'

# server-bad, pwd: secret (self-signed)
openssl req -new -x509 -outform pem -out server-bad.cer -keyout server-bad.key -newkey rsa:2048 -days $((10*365+10)) -subj '/C=PL/O=OPS4J/CN=server-bad'

# server req, pwd: secret2
openssl req -new -outform pem -out server.req -keyout server.key -newkey rsa:2048 -subj '/C=PL/O=OPS4J/CN=server' -reqexts v3_req_san2

    [ v3_req_san2 ]

    basicConstraints = CA:FALSE
    keyUsage = nonRepudiation, digitalSignature, keyEncipherment
    extendedKeyUsage = serverAuth, clientAuth
    subjectAltName = DNS:localhost,IP:127.0.0.1

# client req, pwd: secret2
openssl req -new -outform pem -out client.req -keyout client.key -newkey rsa:2048 -subj '/C=PL/O=OPS4J/CN=client' -reqexts v3_req_client

    [ v3_req_client ]

    basicConstraints = CA:FALSE
    keyUsage = nonRepudiation, digitalSignature, keyEncipherment
    extendedKeyUsage = clientAuth

# client2 req, self-signed, pwd: secret2
openssl req -new -x509 -outform pem -out client2.cer -keyout client2.key -newkey rsa:2048 -days $((10*365+10)) -subj '/C=PL/O=OPS4J/CN=client2' -reqexts v3_req_client -extfile ~/.ssl/openssl.cnf -extensions usr_cert_client

    [ v3_req_client ]

    basicConstraints = CA:FALSE
    keyUsage = nonRepudiation, digitalSignature, keyEncipherment
    extendedKeyUsage = clientAuth

# sign server req
openssl x509 -req -in server.req -out server.cer -CA ca.cer -CAkey ca.key -CAcreateserial -days $((40*365+10)) -extfile ~/.ssl/openssl.cnf -extensions usr_cert_san2

    [ usr_cert_san2 ]

    basicConstraints=CA:FALSE
    keyUsage = nonRepudiation, digitalSignature, keyEncipherment
    nsComment = "OpenSSL Generated Certificate"
    subjectKeyIdentifier=hash
    authorityKeyIdentifier=keyid,issuer
    extendedKeyUsage = serverAuth, clientAuth
    subjectAltName = DNS:localhost,IP:127.0.0.1

# sign client req
openssl x509 -req -in client.req -out client.cer -CA ca.cer -CAkey ca.key -CAserial ca.srl -days $((40*365+10)) -extfile ~/.ssl/openssl.cnf -extensions usr_cert_client

    [ usr_cert_client ]

    basicConstraints = CA:FALSE
    keyUsage = nonRepudiation, digitalSignature, keyEncipherment
    nsComment = "OpenSSL Generated Certificate"
    subjectKeyIdentifier=hash
    authorityKeyIdentifier=keyid,issuer
    extendedKeyUsage = clientAuth

# PKCS12 export (to import in portecle), pwd: secret1, key-pwd: secret2
openssl pkcs12 -in server.cer -inkey server.key -out server.p12 -CAfile ca.cer -name server -caname ca -chain -export
openssl pkcs12 -in server-bad.cer -inkey server-bad.key -out server-bad.p12 -name server-bad -chain -export
openssl pkcs12 -in client.cer -inkey client.key -out client.p12 -CAfile ca.cer -name client -caname ca -chain -export
openssl pkcs12 -in client2.cer -inkey client2.key -out client2.p12 -name client -export
