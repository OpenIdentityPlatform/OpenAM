/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Query.java,v 1.2 2008/06/25 05:47:37 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

/**
 * This is an abstract base class for SAML query. It corresponds to the
 * <code>&lt;samlp:QueryAbstractType&gt;</code> in SAML protocol schema.
 *
 * @supported.all.api
 */
public abstract class Query {

    /**
     * The Query is unsupported.
     */
    public static final int NOT_SUPPORTED_QUERY = -1;

    /**
     * The Query is an Authentication Query.
     */
    public static final int AUTHENTICATION_QUERY = 0;

    /**
     * The Query is an Authorization Decision Query.
     */
    public static final int AUTHORIZATION_DECISION_QUERY = 1;

    /**
     * The Query is an Attribute Query.
     */
    public static final int ATTRIBUTE_QUERY = 2;

    /**
     * default constructor.
     */
    protected Query() {
    }

    /**
     * Returns the type of the Query such as <code>AuthenticationQuery</code> or
     * <code>AuthorizationDecisionQuery</code>.
     *
     * @return the type of Query.
     */
    public abstract int getQueryType();

    /**
     * Creates a String representation of the element.
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
     */
    public abstract String toString(boolean includeNS, boolean declareNS);
}
