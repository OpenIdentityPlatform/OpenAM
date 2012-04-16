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
 * $Id: PartnerAccountMapper.java,v 1.4 2008/08/19 19:11:14 veiming Exp $
 *
 */


package com.sun.identity.saml.plugins;

import com.sun.identity.saml.protocol.SubjectQuery;

import java.util.List;
import java.util.Map;

/**
 * The class <code>PartnerAccountMapper</code> is an interface
 * that is implemented to map partner account to user account
 * in OpenSSO.  
 * <p>
 * Different partner would need to have a different implementation
 * of the interface. The mappings between the partner source ID and 
 * the implementation class are configured at the <code>Partner URLs</code> 
 * field in SAML service.
 *
 * @supported.all.api
 */

public interface PartnerAccountMapper {

    /** 
     * Key to hold user DN in returned map
     */
    public static final String NAME = "name";

    /** 
     * Key to hold organization DN in returned map
     */
    public static final String ORG = "org";

    /**
     * Key to hold attributes to be set as session properties.
     */
    public static final String ATTRIBUTE = "attribute";


    /**
     * Returns user account in OpenSSO to which the
     * subject in the assertion is mapped. This method will be called in POST
     * profile, <code>ARTIFACT</code> profile, <code>AttributeQuery</code> and
     * <code>AuthorizationDecisionQuery</code>.
     *
     * @param assertions a list of authentication assertions returned from
     *        partner side, this will contains user's identity in
     *        the partner side. The object in the list will be
     *        <code>com.sun.identity.saml.assertion.Assertion</code>
     * @param sourceID source ID for the site from which the subject
     *        originated.
     * @param targetURL value for <code>TARGET</code> query parameter when the
     *        user accessing the SAML aware servlet or post profile servlet.
     * @return Map which contains <code>NAME</code>, <code>ORG</code> and
     *         <code>ATTRIBUTE</code> keys, value of the <code>NAME</code>
     *         key is the user DN, value of the <code>ORG</code> is the user
     *         organization  DN, value of the <code>ATTRIBUTE</code> is a Map
     *         containing key/value pairs which will be set as properties
     *         on the OpenSSO SSO token, the key is the SSO
     *         property name, the value is a String value of the property.
     *         Returns empty map if the mapped user could not be obtained
     *         from the subject.
     */
    public Map getUser(List assertions,String sourceID,String targetURL);

    /**
     * Returns user account in OpenSSO to which the
     * subject in the query is mapped. This method will be called in
     * <code>AttributeQuery</code>.
     *
     * @param subjectQuery subject query returned from partner side,
     *        this will contains user's identity in the partner side.
     * @param sourceID source ID for the site from which the subject
     *        originated.
     * @return Map which contains <code>NAME</code> and <code>ORG</code> keys,
     *         value of the <code>NAME<code> key is the user DN, value of the
     *         <code>ORG</code> is the user organization  DN. Returns empty map
     *         if the mapped user could not be obtained from the subject.
     */
    public Map getUser(SubjectQuery subjectQuery,String sourceID);
}
