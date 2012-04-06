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
 */

package com.sun.identity.workflow;

/**
 * List of parameter keys.
 */
public interface ParameterKeys {
    String P_ENTITY_ID = "entityId";
    String P_DOMAIN_ID = "domainId";
    String P_META_DATA = "metadata";
    String P_EXENDED_DATA = "extendeddata";
    String P_REALM = "realm";
    String P_IDP_E_CERT = "idpecert";
    String P_IDP_S_CERT = "idpscert";
    String P_SP_E_CERT = "idpecert";
    String P_SP_S_CERT = "idpscert";
    String P_COT = "cot";
    String P_IDP = "idp";
    String P_ATTR_MAPPING = "attributemappings";
    String P_DEF_ATTR_MAPPING = "defaultattributemappings";
    String P_ASSERT_CONSUMER = "assertionconsumer";
    String P_SERVLET_CONTEXT = "_servlet_context_";
}
