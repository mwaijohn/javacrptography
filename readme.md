### generating a csr with a private key
```openssl req -new -newkey rsa:2048 -nodes -keyout private.key -out csr.csr```

### generating certificate from csr
```openssl x509 -req -days 3650 -in csr.csr -signkey private.key -out certificate.crt```

### generate a keystore

```openssl pkcs12 -export -out keystore.p12 -inkey private.key -in certificate.crt -alias key0```