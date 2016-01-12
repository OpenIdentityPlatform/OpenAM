/*
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
 * $Id: ActionMapper.java,v 1.4 2008/08/19 19:11:13 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.saml.plugins;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.protocol.AuthorizationDecisionQuery;

import java.util.Map;
import java.util.Set;

/**
 * The class <code>ActionMapper</code> is an interface that is 
 * implemented to get SSO information and map partner actions to
 * OpenAM authorization decisions.
 * <p>
 * A different implementation of the interface may be developed for different
 * partner. The mapping between the partner source ID and the implementation
 * class are configured at the <code>Trusted Partner Sites</code> field
 * in SAML service.
 *
 * @supported.all.api
 */
public interface ActionMapper {

    /**
     * Key to hold a list of actions that are permitted.
     */
    public static final String PERMIT = "Permit";

    /**
     * Key to hold a list of actions that are denied.
     */
    public static final String DENY = "Deny";

    /**
     * Key to hold a list of actions that are indeterminate.
     */
    public static final String INDETERMINATE = "Indeterminate";

    /**
     * Returns the single sign on token id to OpenAM from the query.
     *
     * @param query The received <code>AuthorizationDecisionQuery</code>.
     * @return String which is the single sign on token ID. Return null if the
     *         OpenAM single sign on token id could not be obtained
     *         from the query.
     */
    public String getSSOTokenID(AuthorizationDecisionQuery query);

    /**
     * Returns the Assertion that contains Authentication information that
     * can be used to obtain single sign on token.
     *
     * @param query The received <code>AuthorizationDecisionQuery</code>.
     * @param sourceID The <code>SourceID</code> from which this query is
     *        coming from.
     * @return Assertion The assertion contained inside the query.
     */
    public Assertion getSSOAssertion(AuthorizationDecisionQuery query,
         String sourceID);

    /**
     * Returns Action Decisions for the user.
     * The returned Map is subject to changes per SAML specification.
     *
     * @param query The received <code>AuthorizationDecisionQuery</code>.
     * @param token User sessioin to retrieve the decisions.
     * @param sourceID The <code>sourceID</code> from which the query is coming
     *        from.
     * @return Map which contains the following possible key value pairs:
     *         <ul>
     *         <li><code>PERMIT</code> List of permitted actions, or
     *         <li><code>DENY</code> List of denied actions, or
     *         <li><code>INDETERMINATE</code> List of indeterminate actions
     *         </ul>
     * @exception SAMLException if an error occurs
     */
    public Map getAuthorizationDecisions(AuthorizationDecisionQuery query,
        Object token, String sourceID) throws SAMLException;
}
