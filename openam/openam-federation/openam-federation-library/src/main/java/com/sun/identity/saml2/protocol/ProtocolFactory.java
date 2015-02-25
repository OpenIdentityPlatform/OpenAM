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
 * $Id: ProtocolFactory.java,v 1.5 2008/06/25 05:47:57 qcheng Exp $
 *
 */

package com.sun.identity.saml2.protocol;

import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.impl.ArtifactImpl;
import com.sun.identity.saml2.protocol.impl.ArtifactResolveImpl;
import com.sun.identity.saml2.protocol.impl.ArtifactResponseImpl;
import com.sun.identity.saml2.protocol.impl.AssertionIDRequestImpl;
import com.sun.identity.saml2.protocol.impl.AttributeQueryImpl;
import com.sun.identity.saml2.protocol.impl.AuthnQueryImpl;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;
import com.sun.identity.saml2.protocol.impl.ExtensionsImpl;
import com.sun.identity.saml2.protocol.impl.GetCompleteImpl;
import com.sun.identity.saml2.protocol.impl.IDPEntryImpl;
import com.sun.identity.saml2.protocol.impl.IDPListImpl;
import com.sun.identity.saml2.protocol.impl.LogoutRequestImpl;
import com.sun.identity.saml2.protocol.impl.LogoutResponseImpl;
import com.sun.identity.saml2.protocol.impl.ManageNameIDRequestImpl;
import com.sun.identity.saml2.protocol.impl.ManageNameIDResponseImpl;
import com.sun.identity.saml2.protocol.impl.NameIDMappingRequestImpl;
import com.sun.identity.saml2.protocol.impl.NameIDMappingResponseImpl;
import com.sun.identity.saml2.protocol.impl.NameIDPolicyImpl;
import com.sun.identity.saml2.protocol.impl.NewEncryptedIDImpl;
import com.sun.identity.saml2.protocol.impl.NewIDImpl;
import com.sun.identity.saml2.protocol.impl.RequestedAuthnContextImpl;
import com.sun.identity.saml2.protocol.impl.RequesterIDImpl;
import com.sun.identity.saml2.protocol.impl.ResponseImpl;
import com.sun.identity.saml2.protocol.impl.ScopingImpl;
import com.sun.identity.saml2.protocol.impl.SessionIndexImpl;
import com.sun.identity.saml2.protocol.impl.StatusMessageImpl;
import com.sun.identity.saml2.protocol.impl.StatusImpl;
import com.sun.identity.saml2.protocol.impl.StatusCodeImpl;
import com.sun.identity.saml2.protocol.impl.StatusDetailImpl;

/**
 * This is the factory class to obtain object instances for concrete elements in
 * the protocol schema. This factory class provides 3 methods for each element.
 * <code>createElementName()</code>,
 * <code>createElementName(String value)</code>,
 * <code>createElementName(org.w3c.dom.Element value)</code>.
 *
 * @supported.all.api
 */
public class ProtocolFactory  {
    
    private static ProtocolFactory protoInstance = new ProtocolFactory();
    
   /* Constructor for ProtocolFactory */
    private ProtocolFactory() {
    }
    
    /**
     * Returns an instance of the <code>ProtocolFactory</code> Object.
     *
     * @return an instance of the <code>ProtocolFactory</code> object.
     */
    public static ProtocolFactory getInstance() {
        return protoInstance;
    }
    
    /**
     * Returns the <code>AssertionIDRequest</code> Object.
     *
     * @return the <code>AssertionIDRequest</code> object.
     * @throws SAML2Exception if <code>AssertionIDRequest</code> cannot be
     *     created.
     */
    public AssertionIDRequest createAssertionIDRequest() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION_ID_REQUEST);
	if (obj == null) {
            return new AssertionIDRequestImpl();
	} else {
            return (AssertionIDRequest) obj;
	}
    }
    
    /**
     * Returns the <code>AssertionIDRequest</code> Object.
     *
     * @param value the Document Element of <code>AssertionIDRequest</code>
     *     object.
     * @return the <code>AssertionIDRequest</code> object.
     * @throws SAML2Exception if <code>AssertionIDRequest</code> cannot be
     *     created.
     */
    
    public AssertionIDRequest createAssertionIDRequest(Element value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION_ID_REQUEST, value);
	if (obj == null) {
            return new AssertionIDRequestImpl(value);
	} else {
            return (AssertionIDRequest) obj;
	}
    }
    
    /**
     * Returns the <code>AssertionIDRequest</code> Object.
     *
     * @param value <code>AssertionIDRequest</code> XML String.
     * @return the <code>AssertionIDRequest</code> object.
     * @throws SAML2Exception if <code>AssertionIDRequest</code> cannot be
     *     created.
     */
    public AssertionIDRequest createAssertionIDRequest(String value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION_ID_REQUEST, value);
	if (obj == null) {
            return new AssertionIDRequestImpl(value);
	} else {
            return (AssertionIDRequest) obj;
	}
    }

    /**
     * Returns the <code>AttributeQuery</code> Object.
     *
     * @return the <code>AttributeQuery</code> object.
     * @throws SAML2Exception if <code>AttributeQuery</code> cannot be created.
     */
    public AttributeQuery createAttributeQuery() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE_QUERY);
	if (obj == null) {
            return new AttributeQueryImpl();
	} else {
            return (AttributeQuery) obj;
	}
    }
    
    /**
     * Returns the <code>AttributeQuery</code> Object.
     *
     * @param value the Document Element of <code>AttributeQuery</code> object.
     * @return the <code>AttributeQuery</code> object.
     * @throws SAML2Exception if <code>AttributeQuery</code> cannot be created.
     */
    
    public AttributeQuery createAttributeQuery(Element value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE_QUERY, value);
	if (obj == null) {
            return new AttributeQueryImpl(value);
	} else {
            return (AttributeQuery) obj;
	}
    }
    
    /**
     * Returns the <code>AttributeQuery</code> Object.
     *
     * @param value <code>AttributeQuery</code> XML String.
     * @return the <code>AttributeQuery</code> object.
     * @throws SAML2Exception if <code>AttributeQuery</code> cannot be created.
     */
    public AttributeQuery createAttributeQuery(String value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE_QUERY, value);
	if (obj == null) {
            return new AttributeQueryImpl(value);
	} else {
            return (AttributeQuery) obj;
	}
    }

    /**
     * Returns the <code>AuthnQuery</code> Object.
     *
     * @return the <code>AuthnQuery</code> object.
     * @throws SAML2Exception if <code>AuthnQuery</code> cannot be created.
     */
    public AuthnQuery createAuthnQuery() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_QUERY);
	if (obj == null) {
            return new AuthnQueryImpl();
	} else {
            return (AuthnQuery) obj;
	}
    }
    
    /**
     * Returns the <code>AuthnQuery</code> Object.
     *
     * @param value the Document Element of <code>AuthnQuery</code> object.
     * @return the <code>AuthnQuery</code> object.
     * @throws SAML2Exception if <code>AuthnQuery</code> cannot be created.
     */
    
    public AuthnQuery createAuthnQuery(Element value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_QUERY, value);
	if (obj == null) {
            return new AuthnQueryImpl(value);
	} else {
            return (AuthnQuery) obj;
	}
    }
    
    /**
     * Returns the <code>AuthnQuery</code> Object.
     *
     * @param value <code>AuthnQuery</code> XML String.
     * @return the <code>AuthnQuery</code> object.
     * @throws SAML2Exception if <code>AuthnQuery</code> cannot be created.
     */
    public AuthnQuery createAuthnQuery(String value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_QUERY, value);
	if (obj == null) {
            return new AuthnQueryImpl(value);
	} else {
            return (AuthnQuery) obj;
	}
    }

    /**
     * Returns the <code>AuthnRequest</code> Object.
     *
     * @return the <code>AuthnRequest</code> object.
     * @throws SAML2Exception if <code>AuthnRequest</code> cannot be created.
     */
    public AuthnRequest createAuthnRequest() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_REQUEST);
	if (obj == null) {
            return new AuthnRequestImpl();
	} else {
            return (AuthnRequest) obj;
	}
    }
    
    /**
     * Returns the <code>AuthnRequest</code> Object.
     *
     * @param value the Document Element of <code>AuthnRequest</code> object.
     * @return the <code>AuthnRequest</code> object.
     * @throws SAML2Exception if <code>AuthnRequest</code> cannot be created.
     */
    
    public AuthnRequest createAuthnRequest(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_REQUEST, value);
	if (obj == null) {
            return new AuthnRequestImpl(value);
	} else {
            return (AuthnRequest) obj;
	}
    }
    
    /**
     * Returns the <code>AuthnRequest</code> Object.
     *
     * @param value <code>AuthnRequest</code> XML String.
     * @return the <code>AuthnRequest</code> object.
     * @throws SAML2Exception if <code>AuthnRequest</code> cannot be created.
     */
    public AuthnRequest createAuthnRequest(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_REQUEST, value);
	if (obj == null) {
            return new AuthnRequestImpl(value);
	} else {
            return (AuthnRequest) obj;
	}
    }
    
    /**
     * Returns the <code>Extensions</code> Object.
     *
     * @return the <code>Extensions</code> object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */
    public Extensions createExtensions() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.EXTENSIONS);
	if (obj == null) {
            return new ExtensionsImpl();
	} else {
            return (Extensions) obj;
	}
    }
    
    /**
     * Returns the <code>Extensions</code> Object.
     *
     * @param value the Document Element of <code>Extensions</code> object.
     * @return instance of <code>Extensions</code> object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */
    
    public Extensions createExtensions(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.EXTENSIONS, value);
	if (obj == null) {
            return new ExtensionsImpl(value);
	} else {
            return (Extensions) obj;
	}
    }
    
    /**
     * Returns the <code>Extensions</code> Object.
     *
     * @param  value XML String Representation of <code>Extensions</code>
     *	    object.
     * @return instance of <code>Extensions<code> object.
     * @throws SAML2Exception if <code>Extensions</code> cannot be created.
     */
    public Extensions createExtensions(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.EXTENSIONS, value);
	if (obj == null) {
            return new ExtensionsImpl(value);
	} else {
            return (Extensions) obj;
	}
    }
    
    /**
     * Returns the <code>GetComplete</code> Object.
     *
     * @return instance of <code>GetComplete</code> object.
     * @throws SAML2Exception if <code>GetComplete</code> cannot be created.
     */
    public GetComplete createGetComplete() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.GET_COMPLETE);
	if (obj == null) {
            return new GetCompleteImpl();
	} else {
            return (GetComplete) obj;
	}
    }
    
    /**
     * Returns the <code>GetComplete</code> Object.
     *
     * @param value Document Element of <code>GetComplete</code> object.
     * @return instance of <code>GetComplete</code> Object.
     * @throws SAML2Exception if <code>GetComplete</code> cannot be created.
     */
    
    public GetComplete createGetComplete(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.GET_COMPLETE, value);
	if (obj == null) {
            return new GetCompleteImpl(value);
	} else {
            return (GetComplete) obj;
	}
    }
    
    /**
     * Returns the <code>GetComplete</code> Object.
     *
     * @param value XML String representation of <code>GetComplete</code>
     *	    object.
     * @return instance of <code>GetComplete</code> Object.
     * @throws SAML2Exception if <code>GetComplete</code> cannot be created.
     */
    public GetComplete createGetComplete(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.GET_COMPLETE, value);
	if (obj == null) {
            return new GetCompleteImpl(value);
	} else {
            return (GetComplete) obj;
	}
    }
    
    /**
     * Returns the <code>IDPEntry</code> Object.
     *
     * @return instance of <code>IDPEntry<code> object.
     * @throws SAML2Exception if <code>IDPEntry<code> cannot be created.
     */
    public IDPEntry createIDPEntry() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.IDPENTRY);
	if (obj == null) {
            return new IDPEntryImpl();
	} else {
            return (IDPEntry) obj;
	}
    }
    
    /**
     * Returns the <code>IDPEntry</code> Object.
     *
     * @param value Document Element of <code>IDPEntry<code> object.
     * @return instance of <code>IDPEntry<code> object.
     * @throws SAML2Exception if <code>IDPEntry<code> cannot be created.
     */
    public IDPEntry createIDPEntry(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.IDPENTRY, value);
	if (obj == null) {
            return new IDPEntryImpl(value);
	} else {
            return (IDPEntry) obj;
	}
    }
    
    /**
     * Returns the <code>IDPEntry</code> Object.
     *
     * @param value XML Representation of the <code>IDPEntry<code> object.
     * @return instance of <code>IDPEntry<code> Object.
     * @throws SAML2Exception if <code>IDPEntry<code> cannot be created.
     */
    public IDPEntry createIDPEntry(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.IDPENTRY, value);
	if (obj == null) {
            return new IDPEntryImpl(value);
	} else {
            return (IDPEntry) obj;
	}
    }
    
    /**
     * Returns the <code>IDPList</code> Object.
     *
     * @return instance of <code>IDPList<code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */
    public IDPList createIDPList() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.IDPLIST);
	if (obj == null) {
            return new IDPListImpl();
	} else {
            return (IDPList) obj;
	}
    }
    
    /**
     * Returns the <code>IDPList</code> Object.
     *
     * @param value Document Element of <code>IDPList</code> Object.
     * @return instance of <code>IDPList<code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */
    public IDPList createIDPList(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.IDPLIST, value);
	if (obj == null) {
            return new IDPListImpl(value);
	} else {
            return (IDPList) obj;
	}
    }
    
    /**
     * Returns the <code>IDPList</code> Object.
     *
     * @param value XML String Representation of <code>IDPList</code> Object.
     * @return instance of <code>IDPList</code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */
    public IDPList createIDPList(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.IDPLIST, value);
	if (obj == null) {
            return new IDPListImpl(value);
	} else {
            return (IDPList) obj;
	}
    }
    
    /**
     * Returns the <code>NameIDPolicy</code> Object.
     *
     * @return instance of <code>NameIDPolicy</code> Object.
     * @throws SAML2Exception if <code>NameIDPolicy<code> cannot be created.
     */
    public NameIDPolicy createNameIDPolicy() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEID_POLICY);
	if (obj == null) {
            return new NameIDPolicyImpl();
	} else {
            return (NameIDPolicy) obj;
	}
    }
    
    /**
     * Returns the <code>NameIDPolicy</code> Object.
     *
     * @param value Document Element of <code>NameIDPolicy</code> Object.
     * @return instance of <code>NameIDPolicy</code> Object.
     * @throws SAML2Exception if <code>NameIDPolicy<code> cannot be created.
     */
    public NameIDPolicy createNameIDPolicy(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEID_POLICY, value);
	if (obj == null) {
            return new NameIDPolicyImpl(value);
	} else {
            return (NameIDPolicy) obj;
	}
    }
    
    /**
     * Returns the <code>NameIDPolicy</code> Object.
     *
     * @param value XML String Representation of <code>NameIDPolicy</code>
     *	    object.
     * @return instance of <code>NameIDPolicy</code> object.
     * @throws SAML2Exception if <code>NameIDPolicy<code> cannot be created.
     */
    public NameIDPolicy createNameIDPolicy(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEID_POLICY, value);
	if (obj == null) {
            return new NameIDPolicyImpl(value);
	} else {
            return (NameIDPolicy) obj;
	}
    }
    
    /**
     * Returns the <code>RequesterID</code> Object.
     *
     * @return instance of <code>RequesterID</code> Object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */
    public RequesterID createRequesterID() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.REQUESTERID);
	if (obj == null) {
            return new RequesterIDImpl();
	} else {
            return (RequesterID) obj;
	}
    }
    
    /**
     * Returns the <code>RequesterID</code> Object.
     *
     * @param value Document Element of <code>RequesterID</code> Object.
     * @return instance of <code>RequesterID</code> Object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */
    public RequesterID createRequesterID(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.REQUESTERID, value);
	if (obj == null) {
            return new RequesterIDImpl(value);
	} else {
            return (RequesterID) obj;
	}
    }
    
    /**
     * Returns the <code>RequesterID</code> Object.
     *
     * @param value XML String Representation of <code>RequesterID</code>
     *	    object.
     * @return instance of <code>RequesterID</code> Object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */
    public RequesterID createRequesterID(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.REQUESTERID, value);
	if (obj == null) {
            return new RequesterIDImpl(value);
	} else {
            return (RequesterID) obj;
	}
    }
    
    /**
     * Returns the <code>Scoping</code> Object.
     *
     * @return instance of <code>Scoping</code> Object.
     * @throws SAML2Exception if <code>Scoping<code> cannot be created.
     */
    public Scoping createScoping() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SCOPING);
	if (obj == null) {
            return new ScopingImpl();
	} else {
            return (Scoping) obj;
	}
    }
    
    /**
     * Returns the <code>Scoping</code> Object.
     *
     * @param value Document Element of <code>Scoping</code> Object.
     * @return instance of <code>Scoping</code> Object.
     * @throws SAML2Exception if <code>Scoping<code> cannot be created.
     */
    public Scoping createScoping(Element value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SCOPING, value);
	if (obj == null) {
            return new ScopingImpl(value);
	} else {
            return (Scoping) obj;
	}
    }
    
    /**
     * Returns the <code>Scoping</code> Object.
     *
     * @param value XML String Representation of <code>Scoping</code> Object.
     * @return instance of <code>Scoping</code> Object.
     * @throws SAML2Exception if <code>Scoping<code> cannot be created.
     */
    public Scoping createScoping(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SCOPING, value);
	if (obj == null) {
            return new ScopingImpl(value);
	} else {
            return (Scoping) obj;
	}
    }
    
    /**
     * Returns a mutable requested authentication context object.
     *
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return the <code>RequestedAuthnContext</code> object.
     */
    public RequestedAuthnContext createRequestedAuthnContext()
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.REQUESTED_AUTHN_CONTEXT);
	if (obj == null) {
            return new RequestedAuthnContextImpl();
	} else {
            return (RequestedAuthnContext) obj;
	}
    }
    
    /**
     * Returns an immutable requested authentication context object.
     *
     * @param value DOM element representing requested authentication
     *        context object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public RequestedAuthnContext createRequestedAuthnContext(Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.REQUESTED_AUTHN_CONTEXT, value);
	if (obj == null) {
            return new RequestedAuthnContextImpl(value);
	} else {
            return (RequestedAuthnContext) obj;
	}
    }
    
    /**
     * Returns an immutable requested authentication context object.
     *
     * @param value XML string representing requested authentication
     *        context object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public RequestedAuthnContext createRequestedAuthnContext(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.REQUESTED_AUTHN_CONTEXT, value);
	if (obj == null) {
            return new RequestedAuthnContextImpl(value);
	} else {
            return (RequestedAuthnContext) obj;
	}
    }
    
    /**
     * Returns a mutable manage name identifier request object.
     *
     * @return the <code>ManageNameIDRequest</code> object.
     */
    public ManageNameIDRequest createManageNameIDRequest() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.MANAGE_NAMEID_REQUEST);
	if (obj == null) {
            return new ManageNameIDRequestImpl();
	} else {
            return (ManageNameIDRequest) obj;
	}
    }
    
    /**
     * Returns an immutable manage name identifier request object.
     *
     * @param value DOM element representing <code>ManageNameIDRequest</code>
     *        object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public ManageNameIDRequest createManageNameIDRequest(Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.MANAGE_NAMEID_REQUEST, value);
	if (obj == null) {
            return new ManageNameIDRequestImpl(value);
	} else {
            return (ManageNameIDRequest) obj;
	}
    }
    
    /**
     * Returns an immutable manage name identifier request object.
     *
     * @param value XML string representing <code>ManageNameIDRequest</code>
     *        object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public ManageNameIDRequest createManageNameIDRequest(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.MANAGE_NAMEID_REQUEST, value);
	if (obj == null) {
            return new ManageNameIDRequestImpl(value);
	} else {
            return (ManageNameIDRequest) obj;
	}
    }
    
    /**
     * Returns a mutable manage name identifier response object.
     *
     * @return the <code>ManageNameIDResponse</code> object.
     */
    public ManageNameIDResponse createManageNameIDResponse() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.MANAGE_NAMEID_RESPONSE);
	if (obj == null) {
            return new ManageNameIDResponseImpl();
	} else {
            return (ManageNameIDResponse) obj;
	}
    }
    
    /**
     * Returns an immutable manage name identifier response object.
     *
     * @param value DOM element representing <code>ManageNameIDResponse</code>
     *        object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public ManageNameIDResponse createManageNameIDResponse(Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.MANAGE_NAMEID_RESPONSE, value);
	if (obj == null) {
            return new ManageNameIDResponseImpl(value);
	} else {
            return (ManageNameIDResponse) obj;
	}
    }
    
    /**
     * Returns an immutable manage name identifier response object.
     *
     * @param value XML String representing <code>ManageNameIDResponse</code>
     *        object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public ManageNameIDResponse createManageNameIDResponse(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.MANAGE_NAMEID_RESPONSE, value);
	if (obj == null) {
            return new ManageNameIDResponseImpl(value);
	} else {
            return (ManageNameIDResponse) obj;
	}
    }
    
    /**
     * Returns an new identifier object.
     *
     * @param value DOM element representing <code>NewID</code>
     *        object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public NewID createNewID(Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NEWID, value);
	if (obj == null) {
            return new NewIDImpl(value);
	} else {
            return (NewID) obj;
	}
    }
    
    /**
     * Returns an new identifier object.
     *
     * @param value of the <code>NewID<code>.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public NewID createNewID(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NEWID, value);
	if (obj == null) {
            return new NewIDImpl(value);
	} else {
            return (NewID) obj;
	}
    }
    
    /**
     * Returns an immutable new encrypted identifier object.
     *
     * @param value DOM element representing <code>NewEncryptedID</code>
     *        object.
     * @throws SAML2Exception if it failed to instantiate the object.
     * @return an immutable requested authentication context object.
     */
    public NewEncryptedID createNewEncryptedID(Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NEW_ENCRYPTEDID, value);
	if (obj == null) {
            return new NewEncryptedIDImpl(value);
	} else {
            return (NewEncryptedID) obj;
	}
    }
    
    /**
     * Returns an immutable new encrypted identifier object.
     *
     * @param value XML String representing <code>NewEncryptedID</code>
     *        object.
     * @return an immutable requested authentication context object.
     * @throws SAML2Exception if it failed to instantiate the object.
     */
    public NewEncryptedID createNewEncryptedID(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NEW_ENCRYPTEDID, value);
	if (obj == null) {
            return new NewEncryptedIDImpl(value);
	} else {
            return (NewEncryptedID) obj;
	}
    }
    
    /**
     * Returns the <code>LogoutRequest</code> Object.
     *
     * @return the <code>LogoutRequest</code> object.
     */
    public LogoutRequest createLogoutRequest() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.LOGOUT_REQUEST);
	if (obj == null) {
            return new LogoutRequestImpl();
	} else {
            return (LogoutRequest) obj;
	}
    }
    
    /**
     * Returns the <code>LogoutRequest</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>org.w3c.dom.Element</code> object representing the
     * <code>LogoutRequest</code> object.
     * @return the <code>LogoutRequest</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public LogoutRequest createLogoutRequest(org.w3c.dom.Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.LOGOUT_REQUEST, value);
	if (obj == null) {
            return new LogoutRequestImpl(value);
	} else {
            return (LogoutRequest) obj;
	}
    }
    
    /**
     * Returns the <code>LogoutRequest</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>String</code> representing the
     * <code>LogoutRequest</code> object.
     * @return the <code>LogoutRequest</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public LogoutRequest createLogoutRequest(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.LOGOUT_REQUEST, value);
	if (obj == null) {
            return new LogoutRequestImpl(value);
	} else {
            return (LogoutRequest) obj;
	}
    }
    
    /**
     * Returns the <code>LogoutResponse</code> Object.
     *
     * @return the <code>LogoutResponse</code> object.
     */
    public LogoutResponse createLogoutResponse() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.LOGOUT_RESPONSE);
	if (obj == null) {
            return new LogoutResponseImpl();
	} else {
            return (LogoutResponse) obj;
	}
    }
    
    /**
     * Returns the <code>LogoutResponse</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>org.w3c.dom.Element</code> representing the
     * <code>LogoutResponse</code> object.
     * @return the <code>LogoutResponse</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public LogoutResponse createLogoutResponse(org.w3c.dom.Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.LOGOUT_RESPONSE, value);
	if (obj == null) {
            return new LogoutResponseImpl(value);
	} else {
            return (LogoutResponse) obj;
	}
    }
    
    /**
     * Returns the <code>LogoutResponse</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>String</code> representing the
     * <code>LogoutResponse</code> object.
     * @return the <code>LogoutResponse</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public LogoutResponse createLogoutResponse(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.LOGOUT_RESPONSE, value);
	if (obj == null) {
            return new LogoutResponseImpl(value);
	} else {
            return (LogoutResponse) obj;
	}
    }
    
    /**
     * Returns the <code>Status</code> Object.
     *
     * @return the <code>Status</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public Status createStatus() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS);
	if (obj == null) {
            return new StatusImpl();
	} else {
            return (Status) obj;
	}
    }
    
    /**
     * Returns the <code>Status</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>org.w3c.dom.Element</code> representing the
     * <code>Status</code> object.
     * @return the <code>Status</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public Status createStatus(org.w3c.dom.Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS, value);
	if (obj == null) {
            return new StatusImpl(value);
	} else {
            return (Status) obj;
	}
    }
    
    /**
     * Returns the <code>Status</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>String</code> representing the
     * <code>Status</code> object.
     * @return the <code>Status</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public Status createStatus(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS, value);
	if (obj == null) {
            return new StatusImpl(value);
	} else {
            return (Status) obj;
	}
    }
    
    /**
     * Returns the <code>StatusCode</code> Object.
     *
     * @return the <code>StatusCode</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public StatusCode createStatusCode() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_CODE);
	if (obj == null) {
            return new StatusCodeImpl();
	} else {
            return (StatusCode) obj;
	}
    }
    
    /**
     * Returns the <code>StatusCode</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>org.w3c.dom.Element</code> representing the
     * <code>StatusCode</code> object.
     * @return the <code>StatusCode</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public StatusCode createStatusCode(org.w3c.dom.Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_CODE, value);
	if (obj == null) {
            return new StatusCodeImpl(value);
	} else {
            return (StatusCode) obj;
	}
    }
    
    /**
     * Returns the <code>StatusCode</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>String</code> representing the
     * <code>StatusCode</code> object.
     * @return the <code>StatusCode</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public StatusCode createStatusCode(String value) throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_CODE, value);
	if (obj == null) {
            return new StatusCodeImpl(value);
	} else {
            return (StatusCode) obj;
	}
    }
    
    /**
     * Returns the <code>StatusDetail</code> Object.
     *
     * @return the <code>StatusDetail</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public StatusDetail createStatusDetail() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_DETAIL);
	if (obj == null) {
            return new StatusDetailImpl();
	} else {
            return (StatusDetail) obj;
	}
    }
    
    /**
     * Returns the <code>StatusDetail</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>org.w3c.dom.Element</code> representing the
     * <code>StatusDetail</code> object.
     * @return the <code>StatusDetail</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public StatusDetail createStatusDetail(org.w3c.dom.Element value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_DETAIL, value);
	if (obj == null) {
            return new StatusDetailImpl(value);
	} else {
            return (StatusDetail) obj;
	}
    }
    
    /**
     * Returns the <code>StatusDetail</code> Object. This object will be
     * immutable.
     *
     * @param value the <code>String</code> representing the
     * <code>StatusDetail</code> object.
     * @return the <code>StatusDetail</code> object.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public StatusDetail createStatusDetail(String value)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_DETAIL, value);
	if (obj == null) {
            return new StatusDetailImpl(value);
	} else {
            return (StatusDetail) obj;
	}
    }
    
    /**
     * Returns the <code>StatusMessage</code> Object.
     *
     * @param value A String <code>StatusMessage</code> value
     * @return the <code>StatusMessage</code> object.
     */
    public StatusMessage createStatusMessage(String value) {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.STATUS_MESSAGE, value);
	if (obj == null) {
            return new StatusMessageImpl(value);
	} else {
            return (StatusMessage) obj;
	}
    }
    
    /**
     * Returns the <code>SessionIndex</code> Object.
     *
     * @param value A String <code>SessionIndex</code> value
     * @return the <code>SessionIndex</code> object.
     */
    public SessionIndex createSessionIndex(String value) {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SESSION_INDEX, value);
	if (obj == null) {
            return new SessionIndexImpl(value);
	} else {
            return (SessionIndex) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>Artifact</code>.
     *
     * @param typecode two byte sequence representing <code>TypeCode</code>.
     * @param endpointIndex integer value representing
     *          <code>EndpointIndex</code>.
     * @param sourceID String format of 20-byte sequence. Usually obtained
     *          from taking the SHA-1 hash of the identification URL (called
     *          provider ID).
     * @param messageHandle String format of 20-byte sequence identifying
     *          a message. This value is constructed from a cryptographically
     *          strong random or pseudorandom number sequence.
     * @return a new instance of <code>Artifact</code>.
     * @throws SAML2Exception if it fails to instantiate the object.
     */
    public Artifact createArtifact(byte[] typecode,
    int endpointIndex,
    String sourceID,
    String messageHandle)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ARTIFACT, 
            typecode, endpointIndex, sourceID, messageHandle);
	if (obj == null) {
            return new ArtifactImpl(typecode, endpointIndex,
                sourceID, messageHandle);
	} else {
            return (Artifact) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>Artifact</code>.
     * The return object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *          <code>Artifact</code>.
     * @return a new instance of <code>Artifact</code>.
     * @throws SAML2Exception if error occurs while processing the
     *		<code>Element</code>.
     */
    public Artifact createArtifact(org.w3c.dom.Element elem)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ARTIFACT, 
            elem);
	if (obj == null) {
            return new ArtifactImpl(elem);
	} else {
            return (Artifact) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>Artifact</code>.
     * The return object is immutable.
     *
     * @param encodedArtifactValue <code>Artifact Base64</code> encoded String.
     * @return a new instance of <code>Artifact</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     */
    public Artifact createArtifact(String encodedArtifactValue)
	throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ARTIFACT, 
            encodedArtifactValue);
	if (obj == null) {
            return new ArtifactImpl(encodedArtifactValue);
	} else {
            return (Artifact) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>ArtifactResolve</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>ArtifactResolve</code>.
     */
    public ArtifactResolve createArtifactResolve() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ARTIFACT_RESOLVE);
	if (obj == null) {
            return new ArtifactResolveImpl();
	} else {
            return (ArtifactResolve) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>ArtifactResolve</code>.
     * The return object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *          <code>ArtifactResolve</code>.
     * @return a new instance of <code>ArtifactResolve</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     */
    public ArtifactResolve createArtifactResolve(org.w3c.dom.Element elem)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ARTIFACT_RESOLVE, elem);
	if (obj == null) {
            return new ArtifactResolveImpl(elem);
	} else {
            return (ArtifactResolve) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>ArtifactResolve</code>.
     * The return object is immutable.
     *
     * @param xml a XML String representation of <code>ArtifactResolve</code>.
     * @return a new instance of <code>ArtifactResolve</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     */
    public ArtifactResolve createArtifactResolve(String xml)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ARTIFACT_RESOLVE, xml);
	if (obj == null) {
            return new ArtifactResolveImpl(xml);
	} else {
            return (ArtifactResolve) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>ArtifactResponse</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>ArtifactResponse</code>.
     */
    public ArtifactResponse createArtifactResponse() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ARTIFACT_RESPONSE);
	if (obj == null) {
            return new ArtifactResponseImpl();
	} else {
            return (ArtifactResponse) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>ArtifactResponse</code>.
     * The return object is immutable.
     *
     * @param elem an <code>Element</code> representing
     *          <code>ArtifactResponse</code>.
     * @return a new instance of <code>ArtifactResponse</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     */
    public ArtifactResponse createArtifactResponse(org.w3c.dom.Element elem)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ARTIFACT_RESPONSE, elem);
	if (obj == null) {
            return new ArtifactResponseImpl(elem);
	} else {
            return (ArtifactResponse) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>ArtifactResponse</code>.
     * The return object is immutable.
     *
     * @param xml a XML String representation of <code>ArtifactResponse</code>.
     * @return a new instance of <code>ArtifactResponse</code>.
     * @throws com.sun.identity.saml2.common.SAML2Exception if error occurs
     *          while processing the XML string.
     */
    public ArtifactResponse createArtifactResponse(String xml)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ARTIFACT_RESPONSE, xml);
	if (obj == null) {
            return new ArtifactResponseImpl(xml);
	} else {
            return (ArtifactResponse) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>Response</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>Response</code>.
     */
    public Response createResponse() {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.RESPONSE);
	if (obj == null) {
            return new ResponseImpl();
	} else {
            return (Response) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>Response</code>.
     * The return object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *          <code>Response</code>.
     * @return a new instance of <code>Response</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     */
    public Response createResponse(org.w3c.dom.Element elem)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.RESPONSE, elem);
	if (obj == null) {
            return new ResponseImpl(elem);
	} else {
            return (Response) obj;
	}
    }
    
    /**
     * Returns a new instance of <code>Response</code>.
     * The return object is immutable.
     *
     * @param xml a XML String representation of <code>Response</code>.
     * @return a new instance of <code>Response</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     */
    public Response createResponse(String xml)
    throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.RESPONSE, xml);
	if (obj == null) {
            return new ResponseImpl(xml);
	} else {
            return (Response) obj;
	}
    }

    /**
     * Returns the <code>NameIDMappingRequest</code> Object.
     *
     * @return the <code>NameIDMappingRequest</code> object.
     */
    public NameIDMappingRequest createNameIDMappingRequest() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEIDMAPPING_REQ);
        if (obj == null) {
            return new NameIDMappingRequestImpl();
        } else {
            return (NameIDMappingRequest) obj;
        }
    }
     
    /**
     * Returns the <code>NameIDMappingRequest</code> Object.
     *
     * @param elem the Document Element of <code>NameIDMappingRequest</code>
     *     object.
     * @return the <code>NameIDMappingRequest</code> object.
     * @throws SAML2Exception if <code>NameIDMappingRequest</code> cannot be
     *     created.
     */
    public NameIDMappingRequest createNameIDMappingRequest(Element elem)
        throws SAML2Exception {

        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEIDMAPPING_REQ, elem);
        if (obj == null) {
            return new NameIDMappingRequestImpl(elem);
        } else {
            return (NameIDMappingRequest) obj;
        }
    }
    
    /**
     * Returns the <code>NameIDMappingRequest</code> Object.
     *
     * @param value <code>NameIDMappingRequest</code> XML String.
     * @return the <code>NameIDMappingRequest</code> object.
     * @throws SAML2Exception if <code>NameIDMappingRequest</code> cannot be
     *     created.
     */
    public NameIDMappingRequest createNameIDMappingRequest(String value)
        throws SAML2Exception {

        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEIDMAPPING_REQ, value);
        if (obj == null) {
            return new NameIDMappingRequestImpl(value);
        } else {
            return (NameIDMappingRequest) obj;
        }
    }

    /**
     * Returns the <code>NameIDMappingResponse</code> Object.
     *
     * @return the <code>NameIDMappingResponse</code> object.
     */
    public NameIDMappingResponse createNameIDMappingResponse() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEIDMAPPING_RES);
        if (obj == null) {
            return new NameIDMappingResponseImpl();
        } else {
            return (NameIDMappingResponse) obj;
        }
    }
     
    /**
     * Returns the <code>NameIDMappingResponse</code> Object.
     *
     * @param elem the Document Element of <code>NameIDMappingResponse</code>
     *     object.
     * @return the <code>NameIDMappingResponse</code> object.
     * @throws SAML2Exception if <code>NameIDMappingResponse</code> cannot be
     *     created.
     */
    public NameIDMappingResponse createNameIDMappingResponse(Element elem)
        throws SAML2Exception {

        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEIDMAPPING_RES, elem);
        if (obj == null) {
            return new NameIDMappingResponseImpl(elem);
        } else {
            return (NameIDMappingResponse) obj;
        }
    }
    
    /**
     * Returns the <code>NameIDMappingResponse</code> Object.
     *
     * @param value <code>NameIDMappingResponse</code> XML String.
     * @return the <code>NameIDMappingResponse</code> object.
     * @throws SAML2Exception if <code>NameIDMappingResponse</code> cannot be
     *     created.
     */
    public NameIDMappingResponse createNameIDMappingResponse(String value)
        throws SAML2Exception {

        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEIDMAPPING_RES, value);
        if (obj == null) {
            return new NameIDMappingResponseImpl(value);
        } else {
            return (NameIDMappingResponse) obj;
        }
    }
}

