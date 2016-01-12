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
 * $Id: DefaultActionMapper.java,v 1.4 2008/08/19 19:12:24 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.saml.plugins;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.PolicyEvaluator;

import com.sun.identity.saml.AssertionManager;
import com.sun.identity.saml.AssertionManagerClient;

import com.sun.identity.saml.assertion.Action;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.assertion.Evidence;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.protocol.AuthorizationDecisionQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.iplanet.sso.SSOToken;

/**
 * The class <code>DefaultActionMapper</code> provide a default
 * implementation of the <code>ActionMapper</code> interface. 
 */
public class DefaultActionMapper implements ActionMapper {

    /**
     * Default Constructor
     */
    public DefaultActionMapper() {}

    /**
     * This method exams the SubjectConfirmation of the Subject in the
     * AuthorizationDecisionQuery. If the SubjectConfirmation has only one
     * ConfirmationMethod; and this ConfirmationMethod is equals to
     * "urn:com:sun:identity"; and its SubjectConfirmationData contains
     * TEXT node only, then the method returns the concatenated string of all
     * the TEXT nodes. Otherwise, it returns null.
     * <p>
     * @see com.sun.identity.saml.plugins.ActionMapper#getSSOTokenID
     */
    public String getSSOTokenID(AuthorizationDecisionQuery query) {
	if (query == null) {
	    return null;
	}
	SubjectConfirmation sc = query.getSubject().getSubjectConfirmation();
	if (sc == null) {
	    return null;
	}

	if (!SAMLUtils.isCorrectConfirmationMethod(sc)) {
	    return null;
	}

	Element scData = sc.getSubjectConfirmationData();
	return XMLUtils.getElementString(scData);
    }

    /**
     * This method exams the Evidence in the AuthorizationDecisionQuery.
     * It returns the first valid Assertion that contains at least one
     * AuthenticationStatement.
     * <p>
     * @see com.sun.identity.saml.plugins.ActionMapper#getSSOAssertion
     */
    public Assertion getSSOAssertion(AuthorizationDecisionQuery query,
							String sourceID)
    {
	if (query == null) {
	    return null;
	}

	Assertion assertion = null;
	// check evidence
	Evidence evi = query.getEvidence();
	if (evi != null) {
	    Set assertions = evi.getAssertion();
	    if (assertions != null) {
		Iterator iter = assertions.iterator();
		while(iter.hasNext()) {
		    assertion = (Assertion) iter.next();
		    if (SAMLUtils.isAuthNAssertion(assertion)) {
			return assertion;
		    }
		} // loop through assertions
	    }

	    Set idRefs = evi.getAssertionIDReference();
	    if (idRefs != null) {
		Iterator iter = idRefs.iterator();
		try {
		    AssertionManager am = AssertionManager.getInstance();
		    AssertionIDReference idRef = null;
		    while(iter.hasNext()) {
			idRef = (AssertionIDReference) iter.next();
			try {
			    // get the assertion from server id
			    String remoteUrl = SAMLUtils.getServerURL(
					idRef.getAssertionIDReference());
			    if (remoteUrl != null) { // not this server
				// call AssertionManagerClient.getAssertion
				if (SAMLUtils.debug.messageEnabled()) {
				    SAMLUtils.debug.message("DefaultActionMap"
					+ "per: calling another in lb site:" +
					remoteUrl);
				}
				AssertionManagerClient amc =
				    new AssertionManagerClient(
					SAMLUtils.getFullServiceURL(remoteUrl));
				assertion = amc.getAssertion(idRef, sourceID);
			    } else {
				assertion = am.getAssertion(idRef, sourceID);
			    }
			} catch (Exception e) {
			    if (SAMLUtils.debug.messageEnabled()) {
				SAMLUtils.debug.message("DefaultActionMapper."
				+ "getSSOAssertion: exception when retrieving "
				+ "Assertion from IDRef:" + e);
			    }
			    continue;
			}
			if (SAMLUtils.isAuthNAssertion(assertion)) {
			    return assertion;
			}
		    }
		} catch (Exception e) {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("DefaultActionMapper: Couldn't"
			    + " obtain AssertionManager instance:" + e);
		    }
		}
	    }
	}
	return null;
    }

    /**
     * This method first converts the AttributeStatements in Evidence to
     * OpenAM Policy API environment variables. The Attributes in
     * the AttributeStatement(s) are expected to be OpenAM
     * attributes.
     * It then query the Policy decision one action at a time. Currently,
     * it handles actions defined in urn:oasis:names:tc:SAML:1.0:ghpp only.
     * This action Namespace is mapped to OpenAM
     * iPlanetAMWebAgentService.
     */
    public Map getAuthorizationDecisions(AuthorizationDecisionQuery query,
				Object token, String sourceID)
				throws SAMLException {
	if ((query == null) || (token == null)) {
	    SAMLUtils.debug.message("DefaultActionMapper: null input.");
	    throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
	}
	Evidence evidence = query.getEvidence();
	Subject querySubject = query.getSubject();
	Map envParameters = convertEvidence(evidence, querySubject, sourceID);
	List permitActions = new ArrayList();
	List denyActions = new ArrayList();
	List actions = query.getAction();
	Iterator iterator = actions.iterator();
	PolicyEvaluator pe = null;
	String resource = query.getResource();
	Action action = null;
	String actionNamespace = null;
	while (iterator.hasNext()) {
	    action = (Action) iterator.next();
	    // get ActionNameSpace
	    actionNamespace = action.getNameSpace();
	    if ((actionNamespace != null) && (actionNamespace.equals(
				SAMLConstants.ACTION_NAMESPACE_GHPP)))
	    {
		try {
		    if (pe == null) {
			pe = new PolicyEvaluator("iPlanetAMWebAgentService");
		    }
		    boolean result = pe.isAllowed((SSOToken) token,
                        resource, action.getAction(), envParameters);
		    if (result) {
			permitActions.add(action);
		    } else {
			denyActions.add(action);
		    }
		} catch (Exception e) {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("DefaultActionMapper: "
				+ "Exception from policy:" + e);
		    }
		    continue; // indeterminate
		}
	    }
	} // while loop for each action

	Map resultMap = new HashMap();
	if (!permitActions.isEmpty()) {
	    resultMap.put(ActionMapper.PERMIT, permitActions);
	} else if (!denyActions.isEmpty()) {
	    resultMap.put(ActionMapper.DENY, denyActions);
	} else {
	    resultMap.put(ActionMapper.INDETERMINATE, actions);
	}
	return resultMap;
    }

    private Map convertEvidence(Evidence evidence,
				Subject subject,
				String sourceID) {
	Map envParams = new HashMap();
	if (evidence == null) {
	    return envParams;
	}

	Iterator iterator = null;
	Assertion assertion = null;
	String siteName = (String) SAMLServiceManager.getAttribute(
						SAMLConstants.ISSUER_NAME);
	String issuer = null;

	Set idRefs = evidence.getAssertionIDReference();
	if (idRefs != null) {
	    iterator = idRefs.iterator();
	    try {
		AssertionManager am = AssertionManager.getInstance();
		AssertionIDReference idRef = null;
		while(iterator.hasNext()) {
		    idRef = (AssertionIDReference) iterator.next();
		    try {
			// get the assertion from server id
			String remoteUrl = SAMLUtils.getServerURL(
					idRef.getAssertionIDReference());
			if (remoteUrl != null) { // not this server
			    // call AssertionManagerClient.getAssertion
			    if (SAMLUtils.debug.messageEnabled()) {
				SAMLUtils.debug.message("DefaultActionMapper:"
				    + "calling another server in lb site:" +
				    remoteUrl);
			    }
			    AssertionManagerClient amc =
				new AssertionManagerClient(
				    SAMLUtils.getFullServiceURL(remoteUrl));
			    assertion = amc.getAssertion(idRef, sourceID);
			} else {
			    assertion = am.getAssertion(idRef, sourceID);
			}
		    } catch (Exception e) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("DefaultActionMapper: "
				+ "couldn't retrieve assertion from idRef:"+ e);
			}
			continue;
		    }
		    // no need to check signature or time validation
                    SAMLUtils.addEnvParamsFromAssertion(envParams, assertion,
                                                        subject);
		}
	    } catch (Exception e) {
		if (SAMLUtils.debug.messageEnabled()) {
		    SAMLUtils.debug.message("DefaultActionMapper: Couldn't "
			+ "obtain AssertionManager instance:" + e);
		}
	    }
	}
		
	Set assertions = evidence.getAssertion();
	if (assertions != null) {
	    iterator = assertions.iterator();
	    while(iterator.hasNext()) {
		assertion = (Assertion) iterator.next();
		if ((!assertion.isSignatureValid()) ||
		    (!assertion.isTimeValid()))
		{
		    continue;
		}
		issuer = assertion.getIssuer();
		if ((siteName != null) && (siteName.equals(issuer))) {
		    // this server is the issuer
		} else {
		    // is issuer trusted
		    SAMLServiceManager.SOAPEntry sourceSite =
					SAMLUtils.getSourceSite(issuer);
		    if (sourceSite == null) {
			continue;
		    }
		}
                SAMLUtils.addEnvParamsFromAssertion(envParams, assertion,
                                                    subject);
	    }
	}
	return envParams;
    }
}
