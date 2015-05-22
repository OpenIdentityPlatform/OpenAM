/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MetaTemplateParameters.java,v 1.3 2008/06/25 05:50:02 qcheng Exp $
 *
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.sun.identity.workflow;

import java.util.Arrays;
import java.util.List;

/**
 * Meta Template Parameters.
 */
public class MetaTemplateParameters {
    public static final String P_IDP = "idp";
    public static final String P_IDP_E_CERT = "idpecert";
    public static final String P_IDP_S_CERT = "idpscert";
    public static final String P_SP = "sp";
    public static final String P_SP_E_CERT = "specert";
    public static final String P_SP_S_CERT = "spscert";
    public static final String P_ATTR_AUTHORITY = "attra";
    public static final String P_ATTR_AUTHORITY_E_CERT = "attraecert";
    public static final String P_ATTR_AUTHORITY_S_CERT = "attrascert";
    public static final String P_ATTR_QUERY_PROVIDER = "attrq";
    public static final String P_ATTR_QUERY_PROVIDER_E_CERT = "attrqecert";
    public static final String P_ATTR_QUERY_PROVIDER_S_CERT = "attrqscert";
    public static final String P_AUTHN_AUTHORITY = "authna";
    public static final String P_AUTHN_AUTHORITY_E_CERT = "authnaecert";
    public static final String P_AUTHN_AUTHORITY_S_CERT = "authnascert";
    public static final String P_AFFILIATION = "affiliation";
    public static final String P_AFFI_OWNERID = "affiOwnerID";
    public static final String P_AFFI_MEMBERS = "affimembers";
    public static final String P_AFFI_E_CERT = "affiecert";
    public static final String P_AFFI_S_CERT = "affiscert";
    public static final String P_PDP = "pdp";
    public static final String P_PDP_E_CERT = "pdpecert";
    public static final String P_PDP_S_CERT = "pdpscert";
    public static final String P_PEP = "pep";
    public static final String P_PEP_E_CERT = "pepecert";
    public static final String P_PEP_S_CERT = "pepscert";
   
    /**
     * A list of all the SAML alias parameters.
     */
    public static final List<String> P_SAML_ALIASES = Arrays.asList(P_IDP, P_SP, P_AUTHN_AUTHORITY, P_ATTR_QUERY_PROVIDER,
            P_PEP, P_PDP, P_ATTR_AUTHORITY, P_AFFILIATION);
    
    /**
     * A list of all the WS_FED alias parameters.
     */
    public static final List<String> P_WS_FED_ALIASES = Arrays.asList(P_IDP, P_SP);
}
