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
 * $Id: WebServicesClients.java,v 1.7 2009/06/09 00:41:37 madan_ranganath Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.policy.plugins;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Locale;
import java.security.Principal;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.security.x509.CertUtils;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.shared.debug.Debug;

/**
 * This subject represents web services clients that authenticate to web service
 * providers(PP service, Discovery Service) hosted by OpenAM. The
 * clients authenticate either by Anonymous, X509 token, ClientTLS, or
 * SAML token profile. This subject covers X509Token, ClientTLS and SAML Token
 * profiles. The subject values are the subject DNs associated with the 
 * web services clients certificates.
 */
public class WebServicesClients implements Subject {

    private Set selectedWebServicesClients = Collections.EMPTY_SET;

    private static Debug debug = Debug.getInstance("fmWebServicesClients");
    private static final String RESOURCE_BUNDLE = "fmWebServicesClients";

    /**
     * Default Constructor
     */
    public void WebServicesClients() {
	// do nothing
    }

    /**
     * Initialize the subject. No properties are required for this
     * subject.
     * @param configParams configurational information
     */
    public void initialize(Map configParams) {
	// do nothing
    }

    /**
     * Returns the syntax of the subject type.
     * @see com.sun.identity.policy.Syntax
     * @param token the <code>SSOToken</code>.
     * @return Syntax for this subject.
     */
    public Syntax getValueSyntax(SSOToken token) {
	return (Syntax.MULTIPLE_CHOICE);
    }

    /**
     * Returns certificate subject DNs in the KeyStore as possible values. 
     *
     * @param token the <code>SSOToken</code>
     *
     * @return <code>ValidValues</code> object wiht certificate subject DNs.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid names.
     */
    public ValidValues getValidValues(SSOToken token) 
            throws SSOException, PolicyException {
	return getValidValues(token, "*");
    }

    /**
     * Returns certificate subject DNs in the KeyStore as possible values. 
     *
     * @param token the <code>SSOToken</code>
     * @param pattern the pattern to match with valid values.
     *
     * @return <code>ValidValues</code> object wiht certificate subject DNs.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid names.
     */
    public ValidValues getValidValues(SSOToken token, String pattern) 
            throws SSOException, PolicyException {
	// TODO: ignoring the pattern for now. Do we need to take care of it?
	// probably we can ignore for this subject.
        Set subjects = new HashSet();
        try {
	    KeyProvider kp = null;
            try {
                kp = (KeyProvider)Class.forName(SystemConfigurationUtil.getProperty(
                    SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                    SAMLConstants.JKS_KEY_PROVIDER)).newInstance();
            } catch (ClassNotFoundException cnfe) {
                debug.error("WebServicesClients.getValidValues(): " +
                        " Couldn't find the class.", cnfe);
                kp = null;
            } catch (InstantiationException ie) {
                debug.error("WebServicesClients.getValidValues(): " +
                            " Couldn't instantiate the key provider instance.", 
                            ie);
                kp = null;
            } catch (IllegalAccessException iae) {
                debug.error("WebServicesClients.getValidValues(): " +
                            " Couldn't access the default constructor.", iae);
                kp = null;
            }
            if (kp != null) {
                KeyStore ks = kp.getKeyStore();
                if (ks != null) {
                    Enumeration aliases = ks.aliases();
                    while (aliases.hasMoreElements()) {
		        String alias = (String) aliases.nextElement();
                        if (debug.messageEnabled()) {
		            debug.message("WSClient.getValidValues: alias=" +
                                          alias);
		        }
		        // TODO: need to take care of certificate chaining
		        if (ks.isCertificateEntry(alias)) {
		            debug.message("WSClient.getValidValues: " +
                                       "alias is trusted.");
		            X509Certificate cert =
                                (X509Certificate)ks.getCertificate(alias);
		            if (cert != null) {
			        debug.message("WSClient.getValidValues:cert " +
                                              "not null");
                                String name = CertUtils.getSubjectName(cert);
			        if (name != null && name.length() != 0) {
			            subjects.add(name);
	 	                }
		            } else {
                                debug.message("WSClient.getValidValues: " +
                                              "cert is null");
		            }
		        } else {
		            debug.message("WSClient.getValidValues:alias " +
                                          "not trusted.");
                        }
                    }
                }
            }
	} catch (KeyStoreException kse) {
            if (debug.warningEnabled()) {
                debug.warning("WebServicesClients: couldn't get subjects", 
                        kse);
            }
            String[] objs = { kse.getMessage() };
            throw (new PolicyException(ResBundleUtils.rbName,
                "can_not_get_subject_values", objs, kse));
	} 
	return (new ValidValues(ValidValues.SUCCESS, subjects));
    }

    
    /**
     * Returns the value as is like other subjects.
     * @param value the input value
     * @param locale the locale in which value should be returned.
     *
     */
    public String getDisplayNameForValue(String value, Locale locale) {
	// does nothing
	return(value);
    }

    /**
     * Returns selected web services clients for this subject
     *
     * @return selected web services clients
     */
    public Set getValues() {
	return (selectedWebServicesClients);
    }

    /**
     * Sets the selected web services clients for this subject.
     * @param names the list of clients to set in the subject.
     *
     * @exception InvalidNameException if the given names are not valid
     */
    public void setValues(Set names) throws InvalidNameException {
	// does nothing
	if (names == null) {
            debug.error("WebServicesClients.setValues(): Invalid names");
	    throw (new InvalidNameException(RESOURCE_BUNDLE,
		"webservicesclients_subject_invalid_user_names", null, 
                "null", PolicyException.USER_COLLECTION));
	}
        selectedWebServicesClients = new HashSet();
        selectedWebServicesClients.addAll(names);
        if (debug.messageEnabled()) {
            debug.message("WebServicesClients.setValues(): selected web " +
			"service clients names=" + selectedWebServicesClients);
        }
    }

    
    /**
     * Determines if the token belongs to  the
     * <code>WebServicesClients</code> object.
     *
     * @param token SSOToken of the user
     *
     * @return <code>true</code> if the subject contains one of the web service
     *     client's certificate DNs(client ceritifcates, root CA certificate).
     *     <code>false</code> otherwise.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if an error occured while checking if the
     *     user is a member of this subject
     */

    public boolean isMember(SSOToken token) 
            throws SSOException, PolicyException {
	Principal principal = token.getPrincipal();
	String name = principal.getName();
	if (selectedWebServicesClients.contains(name)) {
	    debug.message("WebServicesClients.isMemeber():principal is member");
	    return true;
	}	
	String principals = token.getProperty("Principals");
	Set requestPrincipals = new HashSet();
	if (principals != null && principals.length() != 0) {
	    StringTokenizer st = new StringTokenizer(principals, "|");
	    while (st.hasMoreTokens()) {
		if (selectedWebServicesClients.contains(st.nextToken())) {
		if (debug.messageEnabled()) {
		    debug.message("WebServicesClients.isMemeber(): principals "
			+ "is member.");
		}
                     return true;
                }
            }
	}
	return false;
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o another object that will be compared with this one
     * @return <code>true</code> if equal; <code>false</code>
     * otherwise.
     */
    public boolean equals(Object o) {
	if (o instanceof WebServicesClients) {
            WebServicesClients client = (WebServicesClients) o;
            if ((selectedWebServicesClients != null) 
                 && (client.selectedWebServicesClients != null) 
                 && (selectedWebServicesClients.equals(
					client.selectedWebServicesClients))) {
	    
		return (true);
	    }
	}
	return (false);
    }

    
    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */

    public Object clone() {
        WebServicesClients theClone = null;
        try {
            theClone = (WebServicesClients) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (selectedWebServicesClients != null) {
            theClone.selectedWebServicesClients = new HashSet();
            theClone.selectedWebServicesClients.addAll(
						selectedWebServicesClients);
        }
	return theClone;
    }

    /**
    * Return a hash code for this <code>WebServicesClients</code>.
    *
    * @return a hash code for this <code>WebServicesClients</code> object.
    */

    public int hashCode() {
        return super.hashCode();
    }
}
