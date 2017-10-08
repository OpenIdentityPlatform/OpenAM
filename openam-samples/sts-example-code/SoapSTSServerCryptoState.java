/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.forgerock.openam.functionaltest.sts.frmwk.soap;

/**
 * Encapsulates the Crypto state for a published soap-sts instance. An instance of this class will be passed to the
 * SoapSTSIntegrationTestModule to guide the SoapSTSKeystoreConfig created for published soap-sts instances in the
 * SoapSTSInstanceConfigFactory. This class is an analogue to the SoapSTSClientCryptoState class. Both ultimately inform
 * the CallbackHandler passed to the CXF runtime, which will be asked to provide the crypto context necessary to satisfy
 * the SecurityPolicy bindings regulating access to published soap-sts instances.
 */
public class SoapSTSServerCryptoState {
    public static class SoapSTSServerCryptoStateBuilder {
        private String keystoreLocation;
        private String keystorePassword;
        private String decryptionKeyAlias;
        private String decryptionKeyPassword;
        private String signatureKeyAlias;
        private String signatureKeyPassword;

        private SoapSTSServerCryptoStateBuilder() {}

        /**
         *
         * @param keystoreLocation location of keystore, in classpath or filesystem
         * @return builder
         */
        public SoapSTSServerCryptoStateBuilder keystoreLocation(String keystoreLocation) {
            this.keystoreLocation = keystoreLocation;
            return this;
        }

        /**
         *
         * @param keystorePassword keystore password. Note for two-way TLS, the server's private key entry password,
         *                         and the keystore password must be the same

         * @return builder
         */
        public SoapSTSServerCryptoStateBuilder keystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        /**
         * In a asymmetric binding, messages from server to client will be encrypted with the server's public key, and thus
         * he alias to the server's private key entry must be specified.
         * @param decryptionKeyAlias alias of server's private key
         * @return builder
         */
        public SoapSTSServerCryptoStateBuilder decryptionKeyAlias(String decryptionKeyAlias) {
            this.decryptionKeyAlias = decryptionKeyAlias;
            return this;
        }

        /**
         * In a asymmetric binding, messages from server to client will be encrypted with the server's public key, and thus
         *the password to the server's private key entry must be specified.
         * @param decryptionKeyPassword password to server's private key
         * @return builder
         */
        public SoapSTSServerCryptoStateBuilder decryptionKeyPassword(String decryptionKeyPassword) {
            this.decryptionKeyPassword = decryptionKeyPassword;
            return this;
        }

        /**
         * In a asymmetric binding, messages from server to client must be signed by the server's private key as identified
         * by this alias.
         * @param signatureKeyAlias alias to server's private key
         * @return builder
         */
        public SoapSTSServerCryptoStateBuilder signatureKeyAlias(String signatureKeyAlias) {
            this.signatureKeyAlias = signatureKeyAlias;
            return this;
        }

        /**
         * In a asymmetric binding, messages from server to client must be signed by the server's private key - this is the
         * password for the key alias immediately above
         * @param signatureKeyPassword password for server's private key
         * @return builder
         */
        public SoapSTSServerCryptoStateBuilder signatureKeyPassword(String signatureKeyPassword) {
            this.signatureKeyPassword = signatureKeyPassword;
            return this;
        }


        public SoapSTSServerCryptoState build() {
            return new SoapSTSServerCryptoState(this);
        }
    }
    private final String keystoreLocation;
    private final String keystorePassword;
    private final String decryptionKeyAlias;
    private final String decryptionKeyPassword;
    private final String signatureKeyAlias;
    private final String signatureKeyPassword;

    private SoapSTSServerCryptoState(SoapSTSServerCryptoStateBuilder builder) {
        this.keystoreLocation = builder.keystoreLocation;
        this.keystorePassword = builder.keystorePassword;
        this.decryptionKeyAlias = builder.decryptionKeyAlias;
        this.decryptionKeyPassword = builder.decryptionKeyPassword;
        this.signatureKeyAlias = builder.signatureKeyAlias;
        this.signatureKeyPassword = builder.signatureKeyPassword;
    }

    public static SoapSTSServerCryptoStateBuilder builder() {
        return new SoapSTSServerCryptoStateBuilder();
    }

    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getDecryptionKeyAlias() {
        return decryptionKeyAlias;
    }

    public String getDecryptionKeyPassword() {
        return decryptionKeyPassword;
    }

    public String getSignatureKeyAlias() {
        return signatureKeyAlias;
    }

    public String getSignatureKeyPassword() {
        return signatureKeyPassword;
    }

    public static SoapSTSServerCryptoState defaultSoapSTSServerCryptoState() {
        // the deployable soap-sts .war file will be created with the sts-example-server .jks packaged at root of
        // classpath in .war file
        return SoapSTSServerCryptoState.builder()
                .keystoreLocation("sts-example-server.jks")
                .keystorePassword("password")
                .decryptionKeyAlias("sts-example-server")
                .decryptionKeyPassword("password")
                .signatureKeyAlias("sts-example-server")
                .signatureKeyPassword("password")
                .build();
    }
}