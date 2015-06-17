/*
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
 * $Id: DefaultPartnerAccountMapper.java,v 1.7 2010/01/09 19:41:52 qcheng Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.saml.plugins;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.SubjectStatement;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.SubjectQuery;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.RDN;
import org.forgerock.opendj.ldap.SearchScope;

/**
 * The class <code>DefaultPartnerAccountMapper</code> provide a default
 * implementation of the <code>PartnerAccountMapper</code> interface. 
 * <p>
 * The implementation assumes two sites have exactly the same DIT structure,
 * and it maps remote user to the anonymous user by default if the DIT 
 * structure could not be determined.
 */

public class DefaultPartnerAccountMapper implements PartnerAccountMapper {

    static String ANONYMOUS_USER = "anonymous";
 
    /**
     * Default Constructor
     */
    public DefaultPartnerAccountMapper() {}

    /**
     * Returns user account in OpenAM to which the
     * subject in the assertion is mapped. This method will be called in POST
     * profile, ARTIFACT profile, AttributeQuery and AuthorizationDecisionQuery.
     *
     * @param assertions a list of authentication assertions returned from
     *                   partner side, this will contains user's identity in
     *                   the partner side. The object in the list will be
     *                   <code>com.sun.identity.saml.assertion.Assertion</code>
     * @param sourceID source ID for the site from which the subject
     *                 originated.
     * @param targetURL value for TARGET query parameter when the user
     *                  accessing the SAML aware servlet or post profile
     *                  servlet
     * @return Map which contains NAME, ORG and ATTRIBUTE keys, value of the
     *             NAME key is the user DN, value of the ORG is the user
     *             organization  DN, value of the ATTRIBUTE is a Map
     *             containing key/value pairs which will be set as properties
     *             on the OpenAM SSO token, the key is the SSO
     *             property name, the value is a String value of the property.
     *             Returns empty map if the mapped user could not be obtained
     *             from the subject.
     */
    public Map getUser(List assertions, String sourceID, String targetURL) {
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("DefaultPartnerAccountMapper:getUser(" +
                                    "List) targetURL = " + targetURL);
        }

        Map map = new HashMap();
        Subject subject = null;
        Assertion assertion = (Assertion)assertions.get(0);
        Iterator iter = assertion.getStatement().iterator();
        while (iter.hasNext()) {
            Statement statement = (Statement)iter.next();
            if (statement.getStatementType() !=
                Statement.AUTHENTICATION_STATEMENT) {

                continue;
            }

            Subject sub = ((SubjectStatement)statement).getSubject();
            SubjectConfirmation subConf = sub.getSubjectConfirmation();
            if (subConf == null) {
                continue;
            }

            Set cms = subConf.getConfirmationMethod(); 
            if (cms == null || cms.isEmpty()) {
                continue;
            }

            String cm = (String)cms.iterator().next();

            if (cm != null &&
                (cm.equals(SAMLConstants.CONFIRMATION_METHOD_ARTIFACT)||
                 cm.equals(
                     SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT)||
                 cm.equals(SAMLConstants.CONFIRMATION_METHOD_BEARER))) {

                 subject = sub;
                 break;
            }
        }

        if (subject != null) {
            getUser(subject, sourceID, map);
            Map attrMap = new HashMap();
            SAMLUtils.addEnvParamsFromAssertion(attrMap, assertion, subject);
            if (!attrMap.isEmpty()) {
                map.put(ATTRIBUTE, attrMap);
            }
        }

        return map;
    }

    /**
     * Returns user account in OpenAM to which the
     * subject in the query is mapped. This method will be called in
     * AttributeQuery.The returned Map is subject to changes per SAML
     * specification.
     *
     * @param subjectQuery subject query returned from partner side,
     *                  this will contains user's identity in the partner side.
     * @param sourceID source ID for the site from which the subject
     *                 originated.
     * @return Map which contains NAME and ORG keys, value of the
     *             NAME key is the user DN, value of the ORG is the user
     *             organization  DN. Returns empty map if the mapped user
     *             could not be obtained from the subject.
     */
    public Map getUser(SubjectQuery subjectQuery,String sourceID) {
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("DefaultPartnerAccountMapper:getUser(" +
                                    "SubjectQuery)");
        }

        Map<String, String> map = new HashMap<>();
        getUser(subjectQuery.getSubject(), sourceID, map);
        return map;
    }

    protected void getUser(Subject subject, String sourceID, Map<String, String> map) {
        // No need to check SSO in SubjectConfirmation here
        // since AssertionManager will handle it without calling account mapper
        NameIdentifier nameIdentifier = subject.getNameIdentifier();
        if (nameIdentifier != null) {
            String name = nameIdentifier.getName();
            String org = nameIdentifier.getNameQualifier();
            String rootSuffix = SMSEntry.getRootSuffix();
            if (name != null && (name.length() != 0)) {
                if (org != null && (org.length() != 0)) {
                    DN dn1 = DN.valueOf(name);
                    DN dn2 = DN.valueOf(org);
                    if (dn1.isInScopeOf(dn2, SearchScope.SUBORDINATES)) {
                        StringBuilder sb = new StringBuilder(50);
                        for (RDN rdn : dn1) {
                            sb.append(rdn.toString()).append(",");
                        }
                        sb.append(rootSuffix);
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("DefaultPAccountMapper: " 
                                + "name = " + sb.toString());
                        }
                        map.put(NAME, sb.toString()); 
                    } else {
                        SAMLUtils.debug.warning("DefaultPAMapper:to anonymous");
                        // map to anonymous user
                        map.put(NAME, ANONYMOUS_USER); 
                    }
                } else {
                    SAMLUtils.debug.warning("DefaultAccountMapper: Org null.");
                    // map to anonymous user
                    map.put(NAME, ANONYMOUS_USER); 
                } 
            } else {
                SAMLUtils.debug.warning("DefaultAccountMapper: Name is null");
                // map to anonymous user
                map.put(NAME, ANONYMOUS_USER); 
            }
            map.put(ORG, "/"); 
        }
    } 
}
