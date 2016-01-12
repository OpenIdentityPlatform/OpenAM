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
 * $Id: FMSubjectMapper.java,v 1.4 2009/09/22 22:57:43 madan_ranganath Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.xacml.plugins;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;

import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.impl.FMSessionProvider;

import com.sun.identity.saml2.profile.IDPCache;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.Subject;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLSDKUtils;

import com.sun.identity.xacml.spi.SubjectMapper;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.w3c.dom.Element;

/**
 * This class implements SubjectMapper to map between XACML context 
 * Subject and native subject
 * This mapper recognises only the following XACML specification defined
 * attributeId
 * <pre>
 * urn:oasis:names:tc:xacml:1.0:subject:subject-id
 * </pre>
 * Only following dataTypes would be understood for subject-id
 * <pre>
 * urn:oasis:names:tc:xacml:1.0:data-type:x500Name
 * urn:sun:names:xacml:2.0:data-type:opensso-session-id
 * urn:sun:names:xacml:2.0:data-type:openfm-sp-nameid
 * </pre>
 * Only following value would be accepted for subject-category attribute 
 * of Subject
 * <pre>
 * urn:oasis:names:tc:xacml:1.0:subject-category:access-subject
 * </pre>
 * If the attribute or the value is not specified in the request, it would
 * default to this value. The Subject would map to null if a different
 * value has been specified
 * in error condition.
 *
 */
public class FMSubjectMapper implements SubjectMapper {

    FMSessionProvider fmSessionProvider = new FMSessionProvider();

    /**
     * Initializes the mapper implementation. This would be called immediately 
     * after constructing an instance of the implementation.
     *
     * @param pdpEntityId EntityID of PDP
     * @param pepEntityId EntityID of PEP
     * @param properties configuration properties
     * @exception XACMLException if can not initialize
     */
    public void initialize(String pdpEntityId, String pepEntityId, Map properties) 
            throws XACMLException {
    }

    /**
     * Returns native subject, OpenAM SSOToken
     * @param xacmlContextSubjects XACML  context Subject(s) from the
     * xacml-context:Request
     * @return native subject, OpenAM SSOToken, returns null if
     *         Subject did not match
     * @exception XACMLException if can not map to native subject
     */
    public Object mapToNativeSubject(List xacmlContextSubjects) 
            throws XACMLException {

        // Method curently supports only 
        // urn:sun:names:xacml:2.0:data-type:opensso-session-id
        // TODO : Support for
        // urn:oasis:names:tc:xacml:1.0:data-type:x500Name
        // urn:sun:names:xacml:2.0:data-type:openfm-sp-nameid

        if (xacmlContextSubjects == null) {
            return null;
        }
        String sid = null;
        String userName = null;
        //for (int subCount=0;subCount<xacmlContextSubjects.length;subCount++) {
        for (Iterator iter = xacmlContextSubjects.iterator(); iter.hasNext(); ) {
            //Subject subject = xacmlContextSubjects[subCount];
            Subject subject = (Subject)iter.next();
            if (subject == null) {
                continue;
            }
            URI subjectCategory = subject.getSubjectCategory();
            if ((subjectCategory != null) && (!subjectCategory.toString().
                equals(XACMLConstants.ACCESS_SUBJECT))) {
                continue;
            }
            List attributes = subject.getAttributes();
            if (attributes != null) {
                for (int count = 0; count < attributes.size(); count++) {
                    Attribute attr = (Attribute) attributes.get(count);
                    if (attr != null) {
                        URI tmpURI = attr.getAttributeId();
                        if (tmpURI.toString().equals(XACMLConstants.
                            SUBJECT_ID)) {
                            tmpURI = attr.getDataType();
                            if (tmpURI.toString().equals(
                                        XACMLConstants.OPENSSO_SESSION_ID)) {
                                Element sidElement = (Element)attr.getAttributeValues()
                                        .get(0);
                                sid = XMLUtils.getElementValue(sidElement);
                            } else if (tmpURI.toString().equals(
                                    XACMLConstants.X500NAME)) {
                                Element sidElement = (Element)attr.getAttributeValues()
                                        .get(0);
                                userName = XMLUtils.getElementValue(sidElement);
                            } else if (tmpURI.toString().equals(
                                    XACMLConstants.SAML2_NAMEID)) {
                                Element sidElement = (Element)attr.getAttributeValues()
                                        .get(0);
                                String nameID = XMLUtils.getElementValue(sidElement);
                                if (nameID != null) {
                                    userName = (String)
                                       IDPCache.userIDByTransientNameIDValue.get(nameID);
                                }
				// TODO:Need to support non-transient nameid format
                            }
                        }
                    }
            	}
            }
        }

        SSOToken ssoToken = null;
        if (sid != null) { //create ssoToken based on sessionId
            try {
                SSOTokenManager tokenManager = SSOTokenManager.getInstance();
                ssoToken = tokenManager.createSSOToken(sid);
            } catch (SSOException ssoExp) {
                if (XACMLSDKUtils.debug.messageEnabled()) {
                    XACMLSDKUtils.debug.message(
                            "FMSubjectMapper.mapToNativeSubject()"
                            + ":caught SSOException:", ssoExp);
                }
            }
        } 

        //create ssoToken based on x500name (userName)
        if ((ssoToken == null) && (userName != null)) {
            try {
               ssoToken = createFMSession(userName);
           } catch (SessionException se) {
                if (XACMLSDKUtils.debug.messageEnabled()) {
                    XACMLSDKUtils.debug.message(
                            "FMSubjectMapper.mapToNativeSubject()"
                            + ":caught SessionException:", se);
                }
           }
        } 
        return ssoToken;
    }

    private SSOToken createFMSession(String userName) throws SessionException {
        Map info = new HashMap();
        info.put(SessionProvider.REALM, "/");
        info.put(SessionProvider.PRINCIPAL_NAME, userName);
        SSOToken ssoToken = (SSOToken)fmSessionProvider.createSession(info, 
                null, null, null);
        return ssoToken;
    }
}

