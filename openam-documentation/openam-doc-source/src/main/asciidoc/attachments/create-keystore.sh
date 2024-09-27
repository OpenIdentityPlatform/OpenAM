#!/usr/bin/env bash
#
# create-keystore.sh
# copyright (c) 2016 ForgeRock AS.
#
# Keytool must be in your path
#
# Author: Craig McDonnell
#

signature_keystore=keystore-signature.jks
verification_keystore=keystore-verifier.jks
signature_cert=signature.cert
storepass=password
keypass=password
storetype=JCEKS

# Generate the keystore-signature.jks file

keytool -genkeypair \
        -alias "Signature" \
        -dname CN=a \
        -keystore $signature_keystore \
        -storepass $storepass \
        -storetype $storetype \
        -keypass $keypass \
        -keyalg RSA \
        -sigalg SHA256withRSA

# Generate Password

keytool -genseckey \
        -alias "Password" \
        -keystore $signature_keystore \
        -storepass $storepass \
        -storetype $storetype \
        -keypass $keypass \
        -keyalg HmacSHA256 \
        -keysize 256

# Verify (on screen) contents of keystore-signature.jks

keytool -list \
        -keystore $signature_keystore \
        -storepass $storepass \
        -storetype $storetype

# Export SecretKey for verifier

keytool -importkeystore \
        -srckeystore $signature_keystore \
        -destkeystore $verification_keystore \
        -srcstoretype $storetype \
        -deststoretype $storetype \
        -srcstorepass $storepass \
        -deststorepass $storepass \
        -srcalias Password \
        -destalias Password \
        -srckeypass $keypass \
        -destkeypass $keypass

# Export the PublicKey from the signature keystore

keytool -exportcert \
        -alias "Signature" \
        -keystore $signature_keystore \
        -storepass $storepass \
        -storetype $storetype \
        -file $signature_cert

# Import the PublicKey into the verification keystore

keytool -importcert \
        -alias "Signature" \
        -keystore $verification_keystore \
        -storepass $storepass \
        -storetype $storetype \
        -file $signature_cert