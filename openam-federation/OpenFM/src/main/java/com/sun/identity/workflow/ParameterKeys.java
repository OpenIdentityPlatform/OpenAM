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
 * $Id: ParameterKeys.java,v 1.7 2009/03/07 06:52:59 babysunil Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.workflow;

/**
 * List of parameter keys.
 */
public class ParameterKeys {
    public static final String P_ENTITY_ID = "entityId";
    public static final String P_DOMAIN_ID = "domainId";
    public static final String P_META_DATA = "metadata";
    public static final String P_EXTENDED_DATA = "extendeddata";
    public static final String P_REALM = "realm";
    public static final String P_IDP_E_CERT = "idpecert";
    public static final String P_IDP_S_CERT = "idpscert";
    public static final String P_SP_E_CERT = "idpecert";
    public static final String P_SP_S_CERT = "idpscert";
    public static final String P_COT = "cot";
    public static final String P_IDP = "idp";
    public static final String P_ATTR_MAPPING = "attributemappings";
    public static final String P_DEF_ATTR_MAPPING = "defaultattributemappings";
    public static final String P_ASSERT_CONSUMER = "assertionconsumer";
    public static final String P_SERVLET_CONTEXT = "_servlet_context_";
    public static final String P_CLIENT_ID = "clientId";
    public static final String P_CLIENT_SECRET = "clientSecret";
    public static final String P_CLIENT_SECRET_CONFIRM = "clientSecretConfirm";
    public static final String P_REDIRECT_URL = "redirectUrl";
    public static final String P_TYPE = "type";
    public static final String P_PROVIDER_NAME = "providerName";
    public static final String P_IMAGE_URL = "imageUrl";
    public static final String P_OPENID_DISCOVERY_URL = "discoveryUrl";
}
