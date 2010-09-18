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
 * $Id: AssertionManagerImpl.java,v 1.3 2008/08/07 21:41:35 hengming Exp $
 *
 */


package com.sun.identity.saml;

import java.util.*;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;

public class AssertionManagerImpl implements AssertionManagerIF {

    private static AssertionManager assertionManager;
    private static Object lock = new Object();

    /**
     * Checks for exceptions during initialization.
     */
    private static void checkInitialization() throws SAMLException {
        if (assertionManager == null) {
            synchronized(lock) {
                if (assertionManager == null) {
                    try {
                        assertionManager = AssertionManager.getInstance();
                    } catch (SAMLException samle) {
                    // This should not happen . Write to error.
                        SAMLUtils.debug.error(
                            "AssertionManagerImpl: Unable to get "
                                + "AssertionManager", samle);
                        throw(samle);
                    }
                }
            }
        }
    }

    public void checkForLocal() {
        AssertionManagerClient.isLocal = true;
    }

    public String createAssertion(String ssoToken) throws SAMLException {
        checkInitialization();
        Object token = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            token = sessionProvider.getSession(ssoToken);
        } catch (SessionException ssoe) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "AssertionManagerImpl:createAssertion(SSO) " + ssoe);
            }
            throw (new SAMLException(ssoe.getMessage()));
        }

        Assertion a = assertionManager.createAssertion(token);
        // a cannot be null since if a cannot be returned, SAMLException 
        // would be thrown
        return (a.toString(true, true));
    }

    public String createAssertion2(String ssoToken, List attributes)
        throws SAMLException
    {
        checkInitialization();
        Object token = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            token = sessionProvider.getSession(ssoToken);
        } catch (SessionException ssoe) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "AssertionManagerImpl:createAssertion(SSO + attrs) " +
                    ssoe);
            }
            throw (new SAMLException(ssoe.getMessage()));
        }
        LinkedList ll = new LinkedList();
        for (Iterator iter = attributes.iterator(); iter.hasNext();) {
            ll.add(new Attribute(XMLUtils.toDOMDocument((String)
                    iter.next(), SAMLUtils.debug).getDocumentElement()));
        }
        Assertion a = assertionManager.createAssertion(token, ll);
        return (a.toString(true, true));
    }

    public String createAssertionArtifact(String assertion, String target)
        throws SAMLException
    {
        checkInitialization();
        Assertion a = new Assertion(XMLUtils.toDOMDocument(assertion,
                SAMLUtils.debug).getDocumentElement());
        // no need to check null since SAMLException would be thrown if
        // there is any error.

        AssertionArtifact aa = assertionManager.createAssertionArtifact(
            a, SAMLUtils.byteArrayToString(Base64.decode(target)));
        return (aa.getAssertionArtifact());
    }

    public String getAssertion(String artifact, Set destID)
        throws SAMLException
    {
        checkInitialization();
        Set destSet = new HashSet(); 
        Iterator it = destID.iterator();
        while (it.hasNext()) {
            destSet.add(SAMLUtils.byteArrayToString(Base64.decode((
                                          (String) it.next()))));
        }
        Assertion a = assertionManager.getAssertion(new AssertionArtifact(
                                                    artifact), destSet);
        return (a.toString(true, true));
    }

    public String getAssertion2(String artifact, String destID)
        throws SAMLException
    {
        checkInitialization();
        Assertion a = assertionManager.getAssertion(new AssertionArtifact(
                artifact), SAMLUtils.byteArrayToString(Base64.decode(destID)));
        return (a.toString(true, true));
    }
    
    public String getAssertionByIdRef(String idref, Set destID)
         throws SAMLException
    {
        checkInitialization();
        Set destSet = new HashSet(); 
        Iterator it = destID.iterator();
        while (it.hasNext()) {
            destSet.add(SAMLUtils.byteArrayToString(Base64.decode((
                                          (String) it.next()))));
        }
        Assertion a = assertionManager.getAssertion(new AssertionIDReference(
                                                    idref), destSet);
        return (a.toString(true, true));
    }

    public String getAssertionByIdRef2(String idref, String destID)
         throws SAMLException
    {
        checkInitialization();
        Assertion a = assertionManager.getAssertion(new AssertionIDReference(
            idref), SAMLUtils.byteArrayToString(Base64.decode(destID)));
        return (a.toString(true, true));
    }

    public String getAssertionByIdRefToken(String idref, String ssoToken)
        throws SAMLException
    {
        checkInitialization();
        Object token = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            token = sessionProvider.getSession(ssoToken);
        } catch (SessionException ssoe) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "AssertionManagerImpl:getAssertionByIdRefToken: " + ssoe);
            }
            throw (new SAMLException(ssoe.getMessage()));
        }
            
        Assertion a = assertionManager.getAssertion(new AssertionIDReference(
            idref), token);
        return (a.toString(true, true));
    }

    public Set getAssertionArtifacts(String ssoToken) throws SAMLException {
        checkInitialization();
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object token = sessionProvider.getSession(ssoToken);
            return (assertionManager.getAssertionArtifacts(token));
        } catch (SessionException ssoe) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "AssertionManagerImpl:getAssertionArtifacts: " + ssoe);
            }
            throw (new SAMLException(ssoe.getMessage()));
        }
    }

    public Set getAssertions(String ssoToken) throws SAMLException {
        checkInitialization();
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object token = sessionProvider.getSession(ssoToken);
            return (assertionManager.getAssertions(token));
        } catch (SessionException ssoe) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "AssertionManagerImpl:getAssertions: " + ssoe);
            }
            throw (new SAMLException(ssoe.getMessage()));
        }
    }
}
