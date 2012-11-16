/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FedCLIConstants.java,v 1.9 2008/06/25 05:49:52 qcheng Exp $
 *
 */

package com.sun.identity.federation.cli;

/**
 * Constants for CLI options and arguments.
 */
public interface FedCLIConstants {
    /**
     * Realm name.
     */
    String ARGUMENT_REALM = "realm";

    /**
     * Entity ID.
     */
    String ARGUMENT_ENTITY_ID = "entityid";

    /**
     * Signing enabled.
     */
    String ARGUMENT_SIGN = "sign";

    /**
     * Metadata option.
     */
    String ARGUMENT_METADATA = "meta-data-file";

    /**
     * Extended Configuration option.
     */
    String ARGUMENT_EXTENDED_DATA = "extended-data-file";

    /**
     * Circle of Trust.
     */
    String ARGUMENT_COT = "cot";

    /**
     * Trusted Providers.
     */
    String ARGUMENT_TRUSTED_PROVIDERS = "trustedproviders";

    /**
     * Prefix.
     */
    String ARGUMENT_PREFIX = "prefix";

    /**
     * Service Provider.
     */
    String ARGUMENT_SERVICE_PROVIDER = "serviceprovider";

    /**
     * Identity Provider.
     */
    String ARGUMENT_IDENTITY_PROVIDER = "identityprovider";

    /**
     * Attribute Query Provider.
     */
    String ARGUMENT_ATTRIBUTE_QUERY_PROVIDER = "attrqueryprovider";

    /**
     * Attribute Authority.
     */
    String ARGUMENT_ATTRIBUTE_AUTHORITY = "attrauthority";

    /**
     * Authentication Authority.
     */
    String ARGUMENT_AUTHN_AUTHORITY = "authnauthority";

    /**
     * Policy Decision Point.
     */
    String ARGUMENT_PDP = "xacmlpdp";

    /**
     * Policy Enforcement Point.
     */
    String ARGUMENT_PEP = "xacmlpep";

    /**
     * Affiliation.
     */
    String ARGUMENT_AFFILIATION = "affiliation";

    
    /**
     * Affiliation Owner ID.
     */
    String ARGUMENT_AFFI_OWNERID = "affiownerid";

   
    /**
     * Affiliation Members.
     */
    String ARGUMENT_AFFI_MEMBERS = "affimembers";

    /**
     * Service Provider Signing Certificate Alias.
     */
    String ARGUMENT_SP_S_CERT_ALIAS = "spscertalias";

    /**
     * Identity Provider Signing Certificate Alias.
     */
    String ARGUMENT_IDP_S_CERT_ALIAS = "idpscertalias";

    /**
     * Attribute Query Provider Signing Certificate Alias.
     */
    String ARGUMENT_ATTRQ_S_CERT_ALIAS = "attrqscertalias";

    /**
     * Attribute Authority Signing Certificate Alias.
     */
    String ARGUMENT_ATTRA_S_CERT_ALIAS = "attrascertalias";

    /**
     * Authentication Authority Signing Certificate Alias.
     */
    String ARGUMENT_AUTHNA_S_CERT_ALIAS = "authnascertalias";

    /**
     * Affiliation Signing Certificate Alias.
     */
    String ARGUMENT_AFFI_S_CERT_ALIAS = "affiscertalias";

    /**
     * Policy Decision Point Signing Certificate Alias.
     */
    String ARGUMENT_PDP_S_CERT_ALIAS = "xacmlpdpscertalias";

    /**
     * Policy Enforcement Point Signing Certificate Alias.
     */
    String ARGUMENT_PEP_S_CERT_ALIAS = "xacmlpepscertalias";

    /**
     * Service Provider Encryption Certificate Alias.
     */
    String ARGUMENT_SP_E_CERT_ALIAS = "specertalias";

    /**
     * Identity Provider Encryption Certificate Alias.
     */
    String ARGUMENT_IDP_E_CERT_ALIAS = "idpecertalias";

    /**
     * Attribute Query Provider Encryption Certificate Alias.
     */
    String ARGUMENT_ATTRQ_E_CERT_ALIAS = "attrqecertalias";

    /**
     * Attribute Authority Encryption Certificate Alias.
     */
    String ARGUMENT_ATTRA_E_CERT_ALIAS = "attraecertalias";

    /**
     * Authentication Authority Encryption Certificate Alias.
     */
    String ARGUMENT_AUTHNA_E_CERT_ALIAS = "authnaecertalias";

    /**
     * Affiliation Encryption Certificate Alias.
     */
    String ARGUMENT_AFFI_E_CERT_ALIAS = "affiecertalias";

    /**
     * Policy Decision Point Encryption Certificate Alias.
     */
    String ARGUMENT_PDP_E_CERT_ALIAS = "xacmlpdpecertalias";

    /**
     * Policy Enforcement Point Encryption Certificate Alias.
     */
    String ARGUMENT_PEP_E_CERT_ALIAS = "xacmlpepecertalias";

    /**
     * Extended Configuration Data only option.
     */
    String ARGUMENT_EXTENDED_ONLY = "extendedonly";

    /**
     * Specification version e.g. SAML2, IDFF
     */
    String SPECIFICATION_VERSION = "spec";

    /**
     * SAML2 specification.
     */
    String SAML2_SPECIFICATION = "saml2";

    /**
     * IDFF specification.
     */
    String IDFF_SPECIFICATION = "idff";
    
    /**
     * WS-Federation specification.
     */
    String WSFED_SPECIFICATION = "wsfed";
}
