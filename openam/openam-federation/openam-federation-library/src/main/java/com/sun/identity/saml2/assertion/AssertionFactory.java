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
 * $Id: AssertionFactory.java,v 1.3 2008/06/25 05:47:39 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import org.w3c.dom.Element;
import com.sun.identity.saml2.assertion.impl.ActionImpl;
import com.sun.identity.saml2.assertion.impl.AssertionIDRefImpl;
import com.sun.identity.saml2.assertion.impl.AttributeImpl;
import com.sun.identity.saml2.assertion.impl.EncryptedAttributeImpl;
import com.sun.identity.saml2.assertion.impl.AttributeStatementImpl;
import com.sun.identity.saml2.assertion.impl.AuthnContextImpl;
import com.sun.identity.saml2.assertion.impl.AuthnStatementImpl;
import com.sun.identity.saml2.assertion.impl.EncryptedAssertionImpl;
import com.sun.identity.saml2.assertion.impl.EncryptedIDImpl;
import com.sun.identity.saml2.assertion.impl.KeyInfoConfirmationDataImpl;
import com.sun.identity.saml2.assertion.impl.SubjectConfirmationDataImpl;
import com.sun.identity.saml2.assertion.impl.SubjectLocalityImpl;
import com.sun.identity.saml2.assertion.impl.AdviceImpl;
import com.sun.identity.saml2.assertion.impl.AudienceRestrictionImpl;
import com.sun.identity.saml2.assertion.impl.BaseIDImpl;
import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.assertion.impl.ConditionImpl;
import com.sun.identity.saml2.assertion.impl.ConditionsImpl;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.assertion.impl.OneTimeUseImpl;
import com.sun.identity.saml2.assertion.impl.ProxyRestrictionImpl;
import com.sun.identity.saml2.assertion.impl.SubjectImpl;
import com.sun.identity.saml2.assertion.impl.SubjectConfirmationImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 * This is the factory class to obtain instances of the objects defined
 * in assertion schema.
 * There are three ways to obtain an instance of a object type:
 * with no parameters, with a DOM tree element, or with an XML String.
 *
 * @supported.all.api
 */
public class AssertionFactory {

    private static AssertionFactory instance = new AssertionFactory();

    /**
     * Sole Constructor.
     */
    private AssertionFactory() {
    }

    /**
     * Returns the instance of <code>AssertionFactory</code>.
     * 
     * @return <code>AssertionFactory</code>.
     * 
     */
    public static AssertionFactory getInstance() {
        return instance;
    }

    /**
     * Returns a new instance of <code>Advice</code>.
     *
     * @return a new instance of <code>Advice</code>
     * 
     */
    public Advice createAdvice() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ADVICE);
        if (obj == null) {
            return new AdviceImpl();
        } else {
            return (Advice) obj;
        }
    }

    /**
     * Returns a new instance of <code>Advice</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Advice</code>
     * @return a new instance of <code>Advice</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Advice createAdvice(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ADVICE, elem);
        if (obj == null) {
            return new AdviceImpl(elem);
        } else {
            return (Advice) obj;
        }
    }

    /**
     * Returns a new instance of <code>Advice</code>
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Advice</code>
     * @return a new instance of <code>Advice</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public Advice createAdvice(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ADVICE, xml);
        if (obj == null) {
            return new AdviceImpl(xml);
        } else {
            return (Advice) obj;
        }
    }

    /**
     * Returns a new instance of <code>Assertion</code>.
     *
     * @return a new instance of <code>Assertion</code>
     * 
     */
    public Assertion createAssertion() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ASSERTION);
        if (obj == null) {
            return new AssertionImpl();
        } else {
            return (Assertion) obj;
        }
    }

    /**
     * Returns a new instance of <code>Assertion</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Assertion</code>
     * @return a new instance of <code>Assertion</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Assertion createAssertion(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION, elem);
        if (obj == null) {
            return new AssertionImpl(elem);
        } else {
            return (Assertion) obj;
        }
    }

    /**
     * Returns a new instance of <code>Assertion</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Assertion</code>
     * @return a new instance of <code>Assertion</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public Assertion createAssertion(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION, xml);
        if (obj == null) {
            return new AssertionImpl(xml);
        } else {
            return (Assertion) obj;
        }
    }

    /**
     * Returns a new instance of <code>AssertionIDRef</code>.
     *
     * @return a new instance of <code>AssertionIDRef</code>
     * 
     */
    public AssertionIDRef createAssertionIDRef() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION_ID_REF);
        if (obj == null) {
            return new AssertionIDRefImpl();
        } else {
            return (AssertionIDRef) obj;
        }
    }

    /**
     * Returns a new instance of <code>AssertionIDRef</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>AssertionIDRef</code>
     * @return a new instance of <code>AssertionIDRef</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public AssertionIDRef createAssertionIDRef(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION_ID_REF, elem);
        if (obj == null) {
            return new AssertionIDRefImpl(elem);
        } else {
            return (AssertionIDRef) obj;
        }
    }

    /**
     * Returns a new instance of <code>AssertionIDRef</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>AssertionIDRef</code>
     * @return a new instance of <code>AssertionIDRef</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public AssertionIDRef createAssertionIDRef(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ASSERTION_ID_REF, xml);
        if (obj == null) {
            return new AssertionIDRefImpl(xml);
        } else {
            return (AssertionIDRef) obj;
        }
    }

    /**
     * Returns a new instance of <code>AudienceRestriction</code>.
     *
     * @return a new instance of <code>AudienceRestriction</code>
     * 
     */
    public AudienceRestriction createAudienceRestriction() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUDIENCE_RESTRICTION);
        if (obj == null) {
            return new AudienceRestrictionImpl();
        } else {
            return (AudienceRestriction) obj;
        }
    }

    /**
     * Returns a new instance of <code>AudienceRestriction</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of 
     *        <code>AudienceRestriction</code>
     * @return a new instance of <code>AudienceRestriction</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public AudienceRestriction createAudienceRestriction(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUDIENCE_RESTRICTION, elem);
        if (obj == null) {
            return new AudienceRestrictionImpl(elem);
        } else {
            return (AudienceRestriction) obj;
        }
    }

    /**
     * Returns a new instance of <code>AudienceRestriction</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of
     *        <code>AudienceRestriction</code>
     * @return a new instance of <code>AudienceRestriction</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public AudienceRestriction createAudienceRestriction(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUDIENCE_RESTRICTION, xml);
        if (obj == null) {
            return new AudienceRestrictionImpl(xml);
        } else {
            return (AudienceRestriction) obj;
        }
    }

    /**
     * Returns a new instance of <code>BaseID</code>.
     *
     * @return a new instance of <code>BaseID</code>
     * 
     */
    public BaseID createBaseID() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.BASEID);
        if (obj == null) {
            return new BaseIDImpl();
        } else {
            return (BaseID) obj;
        }
    }

    /**
     * Returns a new instance of <code>BaseID</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>BaseID</code>
     * @return a new instance of <code>BaseID</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public BaseID createBaseID(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.BASEID, elem);
        if (obj == null) {
            return new BaseIDImpl(elem);
        } else {
            return (BaseID) obj;
        }
    }

    /**
     * Returns a new instance of <code>BaseID</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>BaseID</code>
     * @return a new instance of <code>BaseID</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public BaseID createBaseID(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.BASEID, xml);
        if (obj == null) {
            return new BaseIDImpl(xml);
        } else {
            return (BaseID) obj;
        }
    }

    /**
     * Returns a new instance of <code>Condition</code>.
     *
     * @return a new instance of <code>Condition</code>
     * 
     */
    public Condition createCondition() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.CONDITION);
        if (obj == null) {
            return new ConditionImpl();
        } else {
            return (Condition) obj;
        }
    }

    /**
     * Returns a new instance of <code>Condition</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Condition</code>
     * @return a new instance of <code>Condition</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     *   
     */
    public Condition createCondition(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.CONDITION, elem);
        if (obj == null) {
            return new ConditionImpl(elem);
        } else {
            return (Condition) obj;
        }
    }

    /**
     * Returns a new instance of <code>Condition</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Condition</code>
     * @return a new instance of <code>Condition</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public Condition createCondition(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.CONDITION, xml);
        if (obj == null) {
            return new ConditionImpl(xml);
        } else {
            return (Condition) obj;
        }
    }

    /**
     * Returns a new instance of <code>Conditions</code>.
     *
     * @return a new instance of <code>Conditions</code>
     * 
     */
    public Conditions createConditions() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.CONDITIONS);
        if (obj == null) {
            return new ConditionsImpl();
        } else {
            return (Conditions) obj;
        }
    }

    /**
     * Returns a new instance of <code>Conditions</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Conditions</code>
     * @return a new instance of <code>Conditions</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Conditions createConditions(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.CONDITIONS, elem);
        if (obj == null) {
            return new ConditionsImpl(elem);
        } else {
            return (Conditions) obj;
        }
    }

    /**
     * Returns a new instance of <code>Conditions</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Conditions</code>
     * @return a new instance of <code>Conditions</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public Conditions createConditions(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.CONDITIONS, xml);
        if (obj == null) {
            return new ConditionsImpl(xml);
        } else {
            return (Conditions) obj;
        }
    }

    /**
     * Returns a new instance of <code>EncryptedAssertion</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of
     *        <code>EncryptedAssertion</code>
     * @return a new instance of <code>EncryptedAssertion</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public EncryptedAssertion createEncryptedAssertion(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ENCRYPTED_ASSERTION, elem);
        if (obj == null) {
            return new EncryptedAssertionImpl(elem);
        } else {
            return (EncryptedAssertion) obj;
        }
    }

    /**
     * Returns a new instance of <code>EncryptedAssertion</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>EncryptedAssertion</code>
     * @return a new instance of <code>EncryptedAssertion</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public EncryptedAssertion createEncryptedAssertion(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ENCRYPTED_ASSERTION, xml);
        if (obj == null) {
            return new EncryptedAssertionImpl(xml);
        } else {
            return (EncryptedAssertion) obj;
        }
    }

    /**
     * Returns a new instance of <code>EncryptedID</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>EncryptedID</code>
     * @return a new instance of <code>EncryptedID</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public EncryptedID createEncryptedID(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ENCRYPTEDID, elem);
        if (obj == null) {
            return new EncryptedIDImpl(elem);
        } else {
            return (EncryptedID) obj;
        }
    }

    /**
     * Returns a new instance of <code>EncryptedID</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>EncryptedID</code>
     * @return a new instance of <code>EncryptedID</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public EncryptedID createEncryptedID(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ENCRYPTEDID, xml);
        if (obj == null) {
            return new EncryptedIDImpl(xml);
        } else {
            return (EncryptedID) obj;
        }
    }

    /**
     * Returns a new instance of <code>Issuer</code>.
     *
     * @return a new instance of <code>Issuer</code>
     * 
     */
    public Issuer createIssuer() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ISSUER);
        if (obj == null) {
            return new IssuerImpl();
        } else {
            return (Issuer) obj;
        }
    }

    /**
     * Returns a new instance of <code>Issuer</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Issuer</code>
     * @return a new instance of <code>Issuer</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Issuer createIssuer(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ISSUER, elem);
        if (obj == null) {
            return new IssuerImpl(elem);
        } else {
            return (Issuer) obj;
        }
    }

    /**
     * Returns a new instance of <code>Issuer</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Issuer</code>
     * @return a new instance of <code>Issuer</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public Issuer createIssuer(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ISSUER, xml);
        if (obj == null) {
            return new IssuerImpl(xml);
        } else {
            return (Issuer) obj;
        }
    }

    /**
     * Returns a new instance of <code>KeyInfoConfirmationData</code>.
     *
     * @return a new instance of <code>KeyInfoConfirmationData</code>
     * 
     */
    public KeyInfoConfirmationData createKeyInfoConfirmationData() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.KEYINFO_CONFIRMATION_DATA);
        if (obj == null) {
            return new KeyInfoConfirmationDataImpl();
        } else {
            return (KeyInfoConfirmationData) obj;
        }
    }

    /**
     * Returns a new instance of <code>KeyInfoConfirmationData</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of
     *        <code>KeyInfoConfirmationData</code>
     * @return a new instance of <code>KeyInfoConfirmationData</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public KeyInfoConfirmationData createKeyInfoConfirmationData(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.KEYINFO_CONFIRMATION_DATA, elem);
        if (obj == null) {
            return new KeyInfoConfirmationDataImpl(elem);
        } else {
            return (KeyInfoConfirmationData) obj;
        }
    }

    /**
     * Returns a new instance of <code>KeyInfoConfirmationData</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of
     *        <code>KeyInfoConfirmationData</code>
     * @return a new instance of <code>KeyInfoConfirmationData</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public KeyInfoConfirmationData createKeyInfoConfirmationData(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.KEYINFO_CONFIRMATION_DATA, xml);
        if (obj == null) {
            return new KeyInfoConfirmationDataImpl(xml);
        } else {
            return (KeyInfoConfirmationData) obj;
        }
    }

    /**
     * Returns a new instance of <code>NameID</code>.
     *
     * @return a new instance of <code>NameID</code>
     * 
     */
    public NameID createNameID() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.NAMEID);
        if (obj == null) {
            return new NameIDImpl();
        } else {
            return (NameID) obj;
        }
    }

    /**
     * Returns a new instance of <code>NameID</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>NameID</code>
     * @return a new instance of <code>NameID</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public NameID createNameID(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEID, elem);
        if (obj == null) {
            return new NameIDImpl(elem);
        } else {
            return (NameID) obj;
        }
    }

    /**
     * Returns a new instance of <code>NameID</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>NameID</code>
     * @return a new instance of <code>NameID</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public NameID createNameID(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.NAMEID, xml);
        if (obj == null) {
            return new NameIDImpl(xml);
        } else {
            return (NameID) obj;
        }
    }

    /**
     * Returns a new instance of <code>OneTimeUse</code>.
     *
     * @return a new instance of <code>OneTimeUse</code>
     * 
     */
    public OneTimeUse createOneTimeUse() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ONE_TIME_USE);
        if (obj == null) {
            return new OneTimeUseImpl();
        } else {
            return (OneTimeUse) obj;
        }
    }

    /**
     * Returns a new instance of <code>OneTimeUse</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>OneTimeUse</code>
     * @return a new instance of <code>OneTimeUse</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public OneTimeUse createOneTimeUse(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ONE_TIME_USE, elem);
        if (obj == null) {
            return new OneTimeUseImpl(elem);
        } else {
            return (OneTimeUse) obj;
        }
    }

    /**
     * Returns a new instance of <code>OneTimeUse</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>OneTimeUse</code>
     * @return a new instance of <code>OneTimeUse</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public OneTimeUse createOneTimeUse(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ONE_TIME_USE, xml);
        if (obj == null) {
            return new OneTimeUseImpl(xml);
        } else {
            return (OneTimeUse) obj;
        }
    }

    /**
     * Returns a new instance of <code>ProxyRestriction</code>.
     *
     * @return a new instance of <code>ProxyRestriction</code>
     * 
     */
    public ProxyRestriction createProxyRestriction() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.PROXY_RESTRICTION);
        if (obj == null) {
            return new ProxyRestrictionImpl();
        } else {
            return (ProxyRestriction) obj;
        }
    }

    /**
     * Returns a new instance of <code>ProxyRestriction</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>ProxyRestriction</code>
     * @return a new instance of <code>ProxyRestriction</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public ProxyRestriction createProxyRestriction(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.PROXY_RESTRICTION, elem);
        if (obj == null) {
            return new ProxyRestrictionImpl(elem);
        } else {
            return (ProxyRestriction) obj;
        }
    }

    /**
     * Returns a new instance of <code>ProxyRestriction</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>ProxyRestriction</code>
     * @return a new instance of <code>ProxyRestriction</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public ProxyRestriction createProxyRestriction(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.PROXY_RESTRICTION, xml);
        if (obj == null) {
            return new ProxyRestrictionImpl(xml);
        } else {
            return (ProxyRestriction) obj;
        }
    }

    /**
     * Returns a new instance of <code>Subject</code>.
     *
     * @return a new instance of <code>Subject</code>
     * 
     */
    public Subject createSubject() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.SUBJECT);
        if (obj == null) {
            return new SubjectImpl();
        } else {
            return (Subject) obj;
        }
    }

    /**
     * Returns a new instance of <code>Subject</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of <code>Subject</code>
     * @return a new instance of <code>Subject</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public Subject createSubject(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT, elem);
        if (obj == null) {
            return new SubjectImpl(elem);
        } else {
            return (Subject) obj;
        }
    }

    /**
     * Returns a new instance of <code>Subject</code>.
     * The return object is immutable.
     *
     * @param xml a XML string representation of <code>Subject</code>
     * @return a new instance of <code>Subject</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public Subject createSubject(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT, xml);
        if (obj == null) {
            return new SubjectImpl(xml);
        } else {
            return (Subject) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectConfirmation</code>.
     *
     * @return a new instance of <code>SubjectConfirmation</code>
     * 
     */
    public SubjectConfirmation createSubjectConfirmation() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_CONFIRMATION);
        if (obj == null) {
            return new SubjectConfirmationImpl();
        } else {
            return (SubjectConfirmation) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectConfirmation</code>.
     * The return object is immutable.
     *
     * @param elem a DOM Element representation of
     *        <code>SubjectConfirmation</code>
     * @return a new instance of <code>SubjectConfirmation</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public SubjectConfirmation createSubjectConfirmation(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_CONFIRMATION, elem);
        if (obj == null) {
            return new SubjectConfirmationImpl(elem);
        } else {
            return (SubjectConfirmation) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectConfirmation</code>. The return
     * object is immutable.
     *
     * @param xml a XML string representation of 
     *        <code>SubjectConfirmation</code>
     * @return a new instance of <code>SubjectConfirmation</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public SubjectConfirmation createSubjectConfirmation(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_CONFIRMATION, xml);
        if (obj == null) {
            return new SubjectConfirmationImpl(xml);
        } else {
            return (SubjectConfirmation) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectConfirmationData</code>.
     *
     * @return a new instance of <code>SubjectConfirmationData</code>
     * 
     */
    public SubjectConfirmationData createSubjectConfirmationData() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_CONFIRMATION_DATA);
        if (obj == null) {
            return new SubjectConfirmationDataImpl();
        } else {
            return (SubjectConfirmationData) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectConfirmationData</code>. The
     * return object is immutable.
     *
     * @param elem a DOM Element representation of
     *        <code>SubjectConfirmationData</code>
     * @return a new instance of <code>SubjectConfirmationData</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    DOM Element 
     * 
     */
    public SubjectConfirmationData createSubjectConfirmationData(Element elem)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_CONFIRMATION_DATA, elem);
        if (obj == null) {
            return new SubjectConfirmationDataImpl(elem);
        } else {
            return (SubjectConfirmationData) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectConfirmationData</code>. The
     * return object is immutable.
     *
     * @param xml a XML string representation of
     *        <code>SubjectConfirmationData</code>
     * @return a new instance of <code>SubjectConfirmationData</code>
     * @throws SAML2Exception if error occurs while processing the 
     *    XML string
     * 
     */
    public SubjectConfirmationData createSubjectConfirmationData(String xml)
        throws SAML2Exception {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_CONFIRMATION_DATA, xml);
        if (obj == null) {
            return new SubjectConfirmationDataImpl(xml);
        } else {
            return (SubjectConfirmationData) obj;
        }
    }

    /**
     * Returns a new instance of <code>Action</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>Action</code>.
     * 
     */
    public Action createAction() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ACTION);
        if (obj == null) {
            return new ActionImpl();
        } else {
            return (Action) obj;
        }
    }

    /**
     * Returns a new instance of <code>Action</code>. The return object
     * is immutable.
     *
     * @param elem an <code>Element</code> representing <code>Action</code>.
     * @return a new instance of <code>Action</code>.
     * @throws SAML2Exception if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public Action createAction(org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ACTION, elem);
        if (obj == null) {
            return new ActionImpl(elem);
        } else {
            return (Action) obj;
        }
    }

    /**
     * Returns a new instance of <code>Action</code>. The return object
     * is immutable.
     *
     * @param xml an XML String representing <code>Action</code>.
     * @return a new instance of <code>Action</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public Action createAction(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ACTION, xml);
        if (obj == null) {
            return new ActionImpl(xml);
        } else {
            return (Action) obj;
        }
    }

    /**
     * Returns a new instance of <code>Attribute</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>Attribute</code>.
     * 
     */
    public Attribute createAttribute() {
        Object obj = SAML2SDKUtils.getObjectInstance(SAML2SDKUtils.ATTRIBUTE);
        if (obj == null) {
            return new AttributeImpl();
        } else {
            return (Attribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>Attribute</code>. The return object
     * is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *                <code>Attribute</code>.
     * @return a new instance of <code>Attribute</code>.
     * @throws SAML2Exception if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public Attribute createAttribute(org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE, elem);
        if (obj == null) {
            return new AttributeImpl(elem);
        } else {
            return (Attribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>Attribute</code>. The return object
     * is immutable.
     *
     * @param xml an XML String representing <code>Attribute</code>.
     * @return a new instance of <code>Attribute</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public Attribute createAttribute(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE, xml);
        if (obj == null) {
            return new AttributeImpl(xml);
        } else {
            return (Attribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>AttributeStatement</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>AttributeStatement</code>.
     * 
     */
    public AttributeStatement createAttributeStatement() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE_STATEMENT);
        if (obj == null) {
            return new AttributeStatementImpl();
        } else {
            return (AttributeStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AttributeStatement</code>. The return
     * object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *          <code>AttributeStatement</code>.
     * @return a new instance of <code>AttributeStatement</code>.
     * @throws SAML2Exception if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public AttributeStatement createAttributeStatement(
                org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE_STATEMENT, elem);
        if (obj == null) {
            return new AttributeStatementImpl(elem);
        } else {
            return (AttributeStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AttributeStatement</code>. The return
     * object is immutable.
     *
     * @param xml an XML String representing <code>AttributeStatement</code>.
     * @return a new instance of <code>AttributeStatement</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public AttributeStatement createAttributeStatement(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ATTRIBUTE_STATEMENT, xml);
        if (obj == null) {
            return new AttributeStatementImpl(xml);
        } else {
            return (AttributeStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthnContext</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>AuthnContext</code>.
     * 
     */
    public AuthnContext createAuthnContext() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_CONTEXT);
        if (obj == null) {
            return new AuthnContextImpl();
        } else {
            return (AuthnContext) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthnContext</code>. The return object
     * is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *                <code>AuthnContext</code>.
     * @return a new instance of <code>AuthnContext</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     * 
     */
    public AuthnContext createAuthnContext(org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_CONTEXT, elem);
        if (obj == null) {
            return new AuthnContextImpl(elem);
        } else {
            return (AuthnContext) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthnContext</code>. The return object
     * is immutable.
     *
     * @param xml an XML String representing <code>AuthnContext</code>.
     * @return a new instance of <code>AuthnContext</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public AuthnContext createAuthnContext(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_CONTEXT, xml);
        if (obj == null) {
            return new AuthnContextImpl(xml);
        } else {
            return (AuthnContext) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthnStatement</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>AuthnStatement</code>.
     * 
     */
    public AuthnStatement createAuthnStatement() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_STATEMENT);
        if (obj == null) {
            return new AuthnStatementImpl();
        } else {
            return (AuthnStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthnStatement</code>. The return object
     * is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *                <code>AuthnStatement</code>.
     * @return a new instance of <code>AuthnStatement</code>.
     * @throws SAML2Exception if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public AuthnStatement createAuthnStatement(org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_STATEMENT, elem);
        if (obj == null) {
            return new AuthnStatementImpl(elem);
        } else {
            return (AuthnStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthnStatement</code>. The return
     * object is immutable.
     *
     * @param xml an XML String representing <code>AuthnStatement</code>.
     * @return a new instance of <code>AuthnStatement</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public AuthnStatement createAuthnStatement(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHN_STATEMENT, xml);
        if (obj == null) {
            return new AuthnStatementImpl(xml);
        } else {
            return (AuthnStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthzDecisionStatement</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>AuthzDecisionStatement</code>.
     * 
     */
    public AuthzDecisionStatement createAuthzDecisionStatement() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHZ_DECISION_STATEMENT);
        if (obj == null) {
            return null;
        } else {
            return (AuthzDecisionStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthzDecisionStatement</code>. The return
     * object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *          <code>AuthzDecisionStatement</code>.
     * @return a new instance of <code>AuthzDecisionStatement</code>.
     * @throws SAML2Exception if error occurs while processing the
     *                <code>Element</code>.
     * 
     */
    public AuthzDecisionStatement createAuthzDecisionStatement(
                org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHZ_DECISION_STATEMENT, elem);
        if (obj == null) {
            return null;
        } else {
            return (AuthzDecisionStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>AuthzDecisionStatement</code>. The return
     * object is immutable.
     *
     * @param xml an XML String representing
     *                <code>AuthzDecisionStatement</code>.
     * @return a new instance of <code>AuthzDecisionStatement</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public AuthzDecisionStatement createAuthzDecisionStatement(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.AUTHZ_DECISION_STATEMENT, xml);
        if (obj == null) {
            return null;
        } else {
            return (AuthzDecisionStatement) obj;
        }
    }

    /**
     * Returns a new instance of <code>EncryptedAttribute</code>. The return
     * object is immutable.
     *
     * @param elem an <code>Element</code> representation of
     *          <code>EncryptedAttribute</code>.
     * @return a new instance of <code>EncryptedAttribute</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     * 
     */
    public EncryptedAttribute createEncryptedAttribute(
                org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ENCRYPTED_ATTRIBUTE, elem);
        if (obj == null) {
            return new EncryptedAttributeImpl(elem);
        } else {
            return (EncryptedAttribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>EncryptedAttribute</code>. The return
     * object is immutable.
     *
     * @param xml an XML String representing <code>EncryptedAttribute</code>.
.
     * @return a new instance of <code>EncryptedAttribute</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public EncryptedAttribute createEncryptedAttribute(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ENCRYPTED_ATTRIBUTE, xml);
        if (obj == null) {
            return new EncryptedAttributeImpl(xml);
        } else {
            return (EncryptedAttribute) obj;
        }
    }

    /**
     * Returns a new instance of <code>Evidence</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>Evidence</code>.
     * 
     */
    public Evidence createEvidence() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.EVIDENCE);
        if (obj == null) {
            return null;
        } else {
            return (Evidence) obj;
        }
    }

    /**
     * Returns a new instance of <code>Evidence</code>. The return object is
     * immutable.
     *
     * @param elem a <code>Element</code> representation of
     *                <code>Evidence</code>.
     * @return a new instance of <code>Evidence</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     * 
     */
    public Evidence createEvidence(org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.EVIDENCE, elem);
        if (obj == null) {
            return null;
        } else {
            return (Evidence) obj;
        }
    }

    /**
     * Returns a new instance of <code>Evidence</code>. The return object
     * is immutable.
     *
     * @param xml an XML String representing <code>Evidence</code>.
     * @return a new instance of <code>Evidence</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public Evidence createEvidence(String xml) throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.EVIDENCE, xml);
        if (obj == null) {
            return null;
        } else {
            return (Evidence) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectLocality</code>.
     * Caller may need to call setters of the class to populate the object.
     *
     * @return a new instance of <code>SubjectLocality</code>.
     * 
     */
    public SubjectLocality createSubjectLocality() {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_LOCALITY);
        if (obj == null) {
            return new SubjectLocalityImpl();
        } else {
            return (SubjectLocality) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectLocality</code>. The return object
     * is immutable.
     *
     * @param elem an <code>Element</code> representing
     *                <code>SubjectLocality</code>.
     * @return a new instance of <code>SubjectLocality</code>.
     * @throws SAML2Exception if error occurs
     *          while processing the <code>Element</code>.
     * 
     */
    public SubjectLocality createSubjectLocality(org.w3c.dom.Element elem)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_LOCALITY, elem);
        if (obj == null) {
            return new SubjectLocalityImpl(elem);
        } else {
            return (SubjectLocality) obj;
        }
    }

    /**
     * Returns a new instance of <code>SubjectLocality</code>. The return object
     * is immutable.
     *
     * @param xml an XML String representing <code>SubjectLocality</code>.
     * @return a new instance of <code>SubjectLocality</code>.
     * @throws SAML2Exception if error occurs while processing the XML string.
     * 
     */
    public SubjectLocality createSubjectLocality(String xml)
                throws SAML2Exception
    {
        Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.SUBJECT_LOCALITY, xml);
        if (obj == null) {
            return new SubjectLocalityImpl(xml);
        } else {
            return (SubjectLocality) obj;
        }
    }
}
