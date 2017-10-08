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

package com.forgerock.openam.functionaltest.sts.frmwk.common;

import java.util.ArrayList;
import java.util.List;

/**
 * State to guide the publication of sts instances.
 */
public class STSPublishContext {
    public static class STSPublishContextBuilder {
        GeneratedTokenType generatedTokenType;
        OIDCSigningAlgorithmType oidcSigningAlgorithmType;
        boolean persistIssuedTokensInCTS;
        List<String> oidcAudiences;
        String oidcIssuer;
        String oidcClientSecret; //for hmac-signed oidc tokens

        String idpEntityId;
        String spEntityId;
        String spAcsUrl;
        String amKeystorePath; //signed or encrypted SAML2/OIDC tokens need path to OpenAM keystore with test private-key-entry
        /*
        If x509->SAML2/OIDC token transformations are being provided in a tls-offloaded context,
        this value specifies the header name that these offload engines will place the client's
        certificate, and where the STS instance expects to find this certificate.
         */
        String clientCertHeaderName;

        private STSPublishContextBuilder() {
            oidcAudiences = new ArrayList<>();
        }
        public STSPublishContextBuilder generatedTokenType(GeneratedTokenType generatedTokenType) {
            this.generatedTokenType = generatedTokenType;
            return this;
        }

        public STSPublishContextBuilder oidcSigningAlgorithmType(OIDCSigningAlgorithmType oidcSigningAlgorithmType) {
            this.oidcSigningAlgorithmType = oidcSigningAlgorithmType;
            return this;
        }

        public STSPublishContextBuilder persistIssuedTokensInCTS(boolean persistIssuedTokensInCTS) {
            this.persistIssuedTokensInCTS = persistIssuedTokensInCTS;
            return this;
        }

        public STSPublishContextBuilder addOidcAudience(String oidcAudience) {
            oidcAudiences.add(oidcAudience);
            return this;
        }

        public STSPublishContextBuilder oidcIssuer(String oidcIssuer) {
            this.oidcIssuer = oidcIssuer;
            return this;
        }

        public STSPublishContextBuilder oidcClientSecret(String oidcClientSecret) {
            this.oidcClientSecret = oidcClientSecret;
            return this;
        }

        public STSPublishContextBuilder idpEntityId(String idpEntityId) {
            this.idpEntityId = idpEntityId;
            return this;
        }

        public STSPublishContextBuilder spEntityId(String spEntityId) {
            this.spEntityId = spEntityId;
            return this;
        }

        public STSPublishContextBuilder spAcsUrl(String spAcsUrl) {
            this.spAcsUrl = spAcsUrl;
            return this;
        }

        public STSPublishContextBuilder amKeystorePath(String amKeystorePath) {
            this.amKeystorePath = amKeystorePath;
            return this;
        }

        public STSPublishContextBuilder clientCertHeaderName(String clientCertHeaderName) {
            this.clientCertHeaderName = clientCertHeaderName;
            return this;
        }

        public STSPublishContext build() {
            return new STSPublishContext(this) ;
        }
    }

    public enum GeneratedTokenType {
        SAML2, OIDC, BOTH;

        public boolean generateSAML2() {
            return (BOTH.ordinal() == ordinal()) || (SAML2.ordinal() == ordinal());
        }

        public boolean generateOIDC() {
            return (BOTH.ordinal() == ordinal()) || (OIDC.ordinal() == ordinal());
        }
    }
    public enum OIDCSigningAlgorithmType {HMAC, RSA}

    private final GeneratedTokenType generatedTokenType;
    private final OIDCSigningAlgorithmType oidcSigningAlgorithmType;
    private final boolean persistIssuedTokensInCTS;
    private final List<String> oidcAudiences;
    private final String oidcIssuer;
    private final String oidcClientSecret;

    private final String idpEntityId;
    private final String spEntityId;
    private final String spAcsUrl;

    private final String amKeystorePath;

    private final String clientCertHeaderName;

    private STSPublishContext(STSPublishContextBuilder builder) {
        this.generatedTokenType = builder.generatedTokenType;
        this.oidcSigningAlgorithmType = builder.oidcSigningAlgorithmType;
        this.persistIssuedTokensInCTS = builder.persistIssuedTokensInCTS;
        this.oidcAudiences = builder.oidcAudiences;
        this.oidcIssuer = builder.oidcIssuer;
        this.oidcClientSecret = builder.oidcClientSecret;
        this.idpEntityId = builder.idpEntityId;
        this.spEntityId = builder.spEntityId;
        this.spAcsUrl = builder.spAcsUrl;
        this.amKeystorePath = builder.amKeystorePath;
        this.clientCertHeaderName = builder.clientCertHeaderName;
    }

    public static STSPublishContextBuilder builder() {
        return new STSPublishContextBuilder();
    }
    public GeneratedTokenType getGeneratedTokenType() {
        return generatedTokenType;
    }

    public OIDCSigningAlgorithmType getOidcSigningAlgorithmType() {
        return oidcSigningAlgorithmType;
    }

    public boolean persistIssuedTokensInCTS() {
        return persistIssuedTokensInCTS;
    }

    public List<String> getOidcAudiences() {
        return oidcAudiences;
    }

    public String getOidcIssuer() {
        return oidcIssuer;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public String getSpAcsUrl() {
        return spAcsUrl;
    }

    public String getAmKeystorePath() {
        return amKeystorePath;
    }

    public String getClientCertHeaderName() {
        return clientCertHeaderName;
    }

    public static boolean publishSAML2(STSPublishContext stsPublishContext) {
        return stsPublishContext.generatedTokenType.generateSAML2();
    }

    public static boolean publishOIDC(STSPublishContext stsPublishContext) {
        return stsPublishContext.generatedTokenType.generateOIDC();
    }

    public static STSPublishContext buildDefaultPublishContext(String openAMKeystorePath, STSPublishContext.GeneratedTokenType generatedTokenType,
                                                               STSPublishContext.OIDCSigningAlgorithmType oidcSigningAlgorithmType) {
        STSPublishContext.STSPublishContextBuilder builder = STSPublishContext.builder();
        builder
                .persistIssuedTokensInCTS(true)
                .generatedTokenType(generatedTokenType)
                .amKeystorePath(openAMKeystorePath)
                //under what header will the sts expect to find the client cert.
                .clientCertHeaderName(CommonConstants.DEFAULT_STS_CERT_TOKEN_HEADER_NAME);
        if (generatedTokenType.generateOIDC()) {
            builder
                    .addOidcAudience(CommonConstants.DEFAULT_OIDC_AUDIENCE)
                    .oidcIssuer(CommonConstants.DEFAULT_OIDC_ISSUER)
                    .oidcSigningAlgorithmType(oidcSigningAlgorithmType);
            if (STSPublishContext.OIDCSigningAlgorithmType.HMAC.equals(oidcSigningAlgorithmType)) {
                builder.oidcClientSecret(CommonConstants.DEFAULT_OIDC_CLIENT_SECRET);
            }
            // no else - if we are signing with RSA, then we just set state corresponding to the default OpenAM client cert.
        }
        if (generatedTokenType.generateSAML2()) {
            builder
                    .idpEntityId(CommonConstants.DEFAULT_SAML2_IDP_ENTITY_ID)
                    .spEntityId(CommonConstants.DEFAULT_SAML2_SP_ENTITY_ID)
                    .spAcsUrl(CommonConstants.DEFAULT_SAML2_SP_ACS_URL);
        }
        return builder.build();
    }
}