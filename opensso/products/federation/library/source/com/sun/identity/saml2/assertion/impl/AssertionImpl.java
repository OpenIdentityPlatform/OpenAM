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
 * $Id: AssertionImpl.java,v 1.8 2009/05/09 15:43:59 mallas Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.ParseException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.AuthzDecisionStatement;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.Advice;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.xmlenc.EncManager;
import com.sun.identity.saml2.xmlsig.SigManager;
import com.sun.identity.saml2.common.SAML2Utils;

/**
 * The <code>Assertion</code> element is a package of information
 * that supplies one or more <code>Statement</code> made by an issuer. 
 * There are three kinds of assertions: Authentication, Authorization Decision, 
 * and Attribute assertions.
 */

public class AssertionImpl implements Assertion {
    private String version;
    private Date issueInstant;
    private Subject subject;
    private Advice advice;
    private String signature;
    private Conditions conditions;
    private String id;
    private List statements = new ArrayList();
    private List authnStatements = new ArrayList();
    private List authzDecisionStatements = new ArrayList();
    private List attributeStatements = new ArrayList();
    private Issuer issuer;
    private boolean isMutable = true;
    private String signedXMLString = null;
    private Boolean isSignatureValid = null;

    public static String ASSERTION_ELEMENT = "Assertion";
    public static String ASSERTION_VERSION_ATTR = "Version";
    public static String ASSERTION_ID_ATTR = "ID";
    public static String ASSERTION_ISSUEINSTANT_ATTR = "IssueInstant";
    public static String XSI_TYPE_ATTR = "xsi:type";
    public static String ASSERTION_ISSUER = "Issuer";
    public static String ASSERTION_SIGNATURE = "Signature";
    public static String ASSERTION_SUBJECT = "Subject";
    public static String ASSERTION_CONDITIONS = "Conditions";
    public static String ASSERTION_ADVICE = "Advice";
    public static String ASSERTION_STATEMENT = "Statement";
    public static String ASSERTION_AUTHNSTATEMENT = "AuthnStatement";
    public static String ASSERTION_AUTHZDECISIONSTATEMENT =
                                                "AuthzDecisionStatement";
    public static String ASSERTION_ATTRIBUTESTATEMENT =
                                         "AttributeStatement";

   /** 
    * Default constructor
    */
    public AssertionImpl() {
    }

    /**
     * This constructor is used to build <code>Assertion</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Assertion</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public AssertionImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
        if (signature != null) {
            signedXMLString = xml;
        }   
    }

    /**
     * This constructor is used to build <code>Assertion</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Assertion</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public AssertionImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
        if (signature != null) {
            signedXMLString = XMLUtils.print(element,"UTF-8");
        }
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(ASSERTION_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): invalid local name " +
                 elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(ASSERTION_VERSION_ATTR);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): version missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_assertion_version"));
        } 
        version = attrValue;
       
        attrValue = element.getAttribute(ASSERTION_ID_ATTR);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): assertion id missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_assertion_id"));
        } 
        id = attrValue; 
     
        attrValue = element.getAttribute(ASSERTION_ISSUEINSTANT_ATTR);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): issue instant missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_issue_instant"));
        } 
        try {
            issueInstant = DateUtils.stringToDate(attrValue);   
        } catch (ParseException pe) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): invalid issue instant");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_date_format"));
        } 

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.processElement(): assertion has no subelements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelements"));
        }
   
        AssertionFactory factory = AssertionFactory.getInstance();
        int nextElem = 0;
        Node child = (Node)nodes.item(nextElem);
        while (child.getNodeType() != Node.ELEMENT_NODE) {
            if (++nextElem >= numOfNodes) {
                SAML2SDKUtils.debug.error("AssertionImpl.processElement():"
                    + " assertion has no subelements");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "missing_subelements"));
            }
            child = (Node)nodes.item(nextElem);
        }

        // The first subelement should be <Issuer>
        String childName = child.getLocalName();
        if ((childName == null) || (!childName.equals(ASSERTION_ISSUER))) {
            SAML2SDKUtils.debug.error("AssertionImpl.processElement():"+
                                     " the first element is not <Issuer>");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelement_issuer"));
        }
        issuer = factory.getInstance().createIssuer((Element)child);
        
        if (++nextElem >= numOfNodes) {
            return;
        }
        child = (Node)nodes.item(nextElem);
        while (child.getNodeType() != Node.ELEMENT_NODE) {
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
        }

        // The next subelement may be <ds:Signature>
        childName = child.getLocalName();
        if ((childName != null) &&
            childName.equals(ASSERTION_SIGNATURE)) {
            signature = XMLUtils.print((Element)child);
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
            while (child.getNodeType() != Node.ELEMENT_NODE) {
                if (++nextElem >= numOfNodes) {
                    return;
                }
                child = (Node)nodes.item(nextElem);
            }
            childName = child.getLocalName();
        } else {
            signature = null;
        }
      
        // The next subelement may be <Subject>
        if ((childName != null) && 
            childName.equals(ASSERTION_SUBJECT)) {
            subject = factory.createSubject((Element)child);
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
            while (child.getNodeType() != Node.ELEMENT_NODE) {
                if (++nextElem >= numOfNodes) {
                    return;
                }
                child = (Node)nodes.item(nextElem);
            }
            childName = child.getLocalName();
        } else {
            subject = null;
        }

        // The next subelement may be <Conditions>
        if ((childName != null) && 
            childName.equals(ASSERTION_CONDITIONS)) {
            conditions = factory.createConditions((Element)child);
            if (++nextElem >= numOfNodes) {
                return;
            }
            child = (Node)nodes.item(nextElem);
            while (child.getNodeType() != Node.ELEMENT_NODE) {
                if (++nextElem >= numOfNodes) {
                    return;
                }
                child = (Node)nodes.item(nextElem);
            }
            childName = child.getLocalName();
        } else {
            conditions = null;
        }
     
        // The next subelement may be <Advice>
        if ((childName != null) && 
            childName.equals(ASSERTION_ADVICE)) {
            advice = factory.createAdvice((Element)child);
            nextElem++;
        } else {
            advice = null;
        }
   
        // The next subelements are all statements    
        while (nextElem < numOfNodes) { 
            child = (Node)nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(ASSERTION_AUTHNSTATEMENT)) {
                        authnStatements.add(
                            factory.createAuthnStatement((Element)child));
                    } else if (childName.equals(
                        ASSERTION_AUTHZDECISIONSTATEMENT)) {
                        authzDecisionStatements.add(factory.
                            createAuthzDecisionStatement((Element)child));
                    } else if (childName.equals(
                        ASSERTION_ATTRIBUTESTATEMENT)) {
                        attributeStatements.add(factory.
                            createAttributeStatement((Element)child)); 
                    } else if ((childName != null) &&
                        childName.equals(ASSERTION_SIGNATURE)) {
                        signature = XMLUtils.print((Element)child);
                    } else {
                        String type = ((Element)child).getAttribute(
                            XSI_TYPE_ATTR);
                        if (childName.equals(ASSERTION_STATEMENT) && 
                                       (type != null && type.length() > 0)) {
                            statements.add(XMLUtils.print((Element)child));
                        } else {
                            SAML2SDKUtils.debug.error(
                                "AssertionImpl.processElement(): " + 
                                "unexpected subelement " + childName);
                            throw new SAML2Exception(SAML2SDKUtils.bundle.
                                getString("unexpected_subelement"));
                        }
                    }
                }
            }
            nextElem++;
        }
             
    }

    /**
     * Returns the version number of the assertion.
     *
     * @return The version number of the assertion.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version number of the assertion.
     *
     * @param version the version number.
     * @exception SAML2Exception if the object is immutable
     */
    public void setVersion(String version) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.version = version;
    }

    /**
     * Returns the time when the assertion was issued
     *
     * @return the time of the assertion issued
     */
    public Date getIssueInstant() {
        return issueInstant;
    }

    /**
     * Set the time when the assertion was issued
     *
     * @param issueInstant the issue time of the assertion
     * @exception SAML2Exception if the object is immutable
    */
    public void setIssueInstant(Date issueInstant) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.issueInstant = issueInstant;
    }
   
    /**
     * Returns the subject of the assertion
     *
     * @return the subject of the assertion
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Sets the subject of the assertion
     *
     * @param subject the subject of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setSubject(Subject subject) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.subject = subject;
    }

    /**
     * Returns the advice of the assertion
     *
     * @return the advice of the assertion
     */
    public Advice getAdvice() {
        return advice;
    }

    /**
     * Sets the advice of the assertion
     *
     * @param advice the advice of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAdvice(Advice advice) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.advice = advice;
    }

    /**
     * Returns the signature of the assertion
     *
     * @return the signature of the assertion
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Returns the conditions of the assertion
     *
     * @return the conditions of the assertion
     */
    public Conditions getConditions() {
        return conditions;
    }

    /**
     * Sets the conditions of the assertion
     *
     * @param conditions the conditions of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setConditions(Conditions conditions) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.conditions = conditions;
    }

    /**
     * Returns the id of the assertion
     *
     * @return the id of the assertion
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the id of the assertion
     *
     * @param id the id of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setID(String id) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.id = id;
    }

    /**
     * Returns the statements of the assertion
     *
     * @return the statements of the assertion
     */
    public List getStatements() {
        return statements;
    }

    /**
     * Returns the Authn statements of the assertion
     *
     * @return the Authn statements of the assertion
     */
    public List getAuthnStatements() {
        return authnStatements;
    }

    /**
     * Returns the <code>AuthzDecisionStatements</code> of the assertion
     *
     * @return the <code>AuthzDecisionStatements</code> of the assertion
     */
    public List getAuthzDecisionStatements() {
        return authzDecisionStatements;
    }

    /**
     * Returns the attribute statements of the assertion
     *
     * @return the attribute statements of the assertion
     */
    public List getAttributeStatements() {
        return attributeStatements;
    }

    /**
     * Sets the statements of the assertion
     *
     * @param statements the statements of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setStatements(List statements) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.statements = statements;
    }

    /**
     * Sets the <code>AuthnStatements</code> of the assertion
     *
     * @param statements the <code>AuthnStatements</code> of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAuthnStatements(List statements) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        authnStatements = statements;
    }

    /**
     * Sets the <code>AuthzDecisionStatements</code> of the assertion
     *
     * @param statements the <code>AuthzDecisionStatements</code> of
     * the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAuthzDecisionStatements(List statements)
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        authzDecisionStatements = statements;
    }

    /**
     * Sets the attribute statements of the assertion
     *
     * @param statements the attribute statements of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setAttributeStatements(List statements) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        attributeStatements = statements;
    }

    /**
     * Returns the issuer of the assertion
     *
     * @return the issuer of the assertion
     */
    public Issuer getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer of the assertion
     *
     * @param issuer the issuer of the assertion
     * @exception SAML2Exception if the object is immutable
     */
    public void setIssuer(Issuer issuer) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.issuer = issuer;
    }

    /**
     * Return whether the assertion is signed 
     *
     * @return true if the assertion is signed; false otherwise.
     */
    public boolean isSigned() {
        return (signature != null);
    }

    /**
     * Return whether the signature is valid or not.
     *
     * @param senderCert Certificate containing the public key
     *             which may be used for  signature verification;
     *             This certificate may also may be used to check
     *             against the certificate included in the signature
     * @return true if the signature is valid; false otherwise.
     * @throws SAML2Exception if the signature could not be verified
     */
    public boolean isSignatureValid(X509Certificate senderCert)
    throws SAML2Exception {

        if (isSignatureValid == null) {            
            if (signedXMLString == null) {
                signedXMLString = toXMLString(true, true);
            }
            isSignatureValid = Boolean.valueOf(
                SigManager.getSigInstance().verify(
                    signedXMLString, getID(), senderCert)
            );
        }
        return isSignatureValid.booleanValue();
    }

    /**
     * Sign the Assertion.
     *
     * @param privateKey Signing key
     * @param cert Certificate which contain the public key correlated to
     *             the signing key; It if is not null, then the signature
     *             will include the certificate; Otherwise, the signature
     *             will not include any certificate
     * @exception SAML2Exception if it could not sign the assertion.
     */
    public void sign(
        PrivateKey privateKey,
        X509Certificate cert
    ) throws SAML2Exception {

        Element signatureElement = 
            SigManager.getSigInstance().sign(
            toXMLString(true, true),
            getID(),
            privateKey,
            cert
        );
        signature = XMLUtils.print(signatureElement); 
        signedXMLString = XMLUtils.print(
            signatureElement.getOwnerDocument().
            getDocumentElement(), "UTF-8");
        makeImmutable();  
    }

    /**
     * Returns an <code>EncryptedAssertion</code> object.
     *
     * @param recipientPublicKey Public key used to encrypt the data encryption
     *                           (secret) key, it is the public key of the
     *                           recipient of the XML document to be encrypted.
     * @param dataEncAlgorithm Data encryption algorithm.
     * @param dataEncStrength Data encryption strength.
     * @param recipientEntityID Unique identifier of the recipient, it is used
     *                          as the index to the cached secret key so that
     *                          the key can be reused for the same recipient;
     *                          It can be null in which case the secret key will
     *                          be generated every time and will not be cached
     *                          and reused. Note that the generation of a secret
     *                          key is a relatively expensive operation.
     * @return <code>EncryptedAssertion</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    public EncryptedAssertion encrypt(
        Key recipientPublicKey,
        String dataEncAlgorithm,
        int dataEncStrength,
        String recipientEntityID
    ) throws SAML2Exception {
        
        Element el = EncManager.getEncInstance().encrypt(
            toXMLString(true, true),
            recipientPublicKey,
            dataEncAlgorithm,
            dataEncStrength,
            recipientEntityID,
            "EncryptedAssertion"
        );
        return AssertionFactory.getInstance().
            createEncryptedAssertion(el);
    }

    /**
     * Gets the validity of the assertion evaluating its conditions if
     * specified.
     *
     * @return false if conditions is invalid based on it lying between
     *         <code>NotBefore</code> (current time inclusive) and
     *         <code>NotOnOrAfter</code> (current time exclusive) values 
     *         and true otherwise or if no conditions specified.
     */
    public boolean isTimeValid() {
        if (conditions == null)  {
            return true;
        }
        else  {
            return conditions.checkDateValidity(System.currentTimeMillis());
        }
    }

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace
    *        qualifier is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception {

        if ((signature != null) && (signedXMLString != null)) {
            return signedXMLString;
        }

        StringBuffer sb = new StringBuffer(2000);
        String NS = "";
        String appendNS = "";
        if (declareNS) {
            NS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            appendNS = SAML2Constants.ASSERTION_PREFIX;
        }
        sb.append("<").append(appendNS).append(ASSERTION_ELEMENT).append(NS);
        if ((version == null) || (version.length() == 0)) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.toXMLString(): version missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_assertion_version"));
        } 
        sb.append(" ").append(ASSERTION_VERSION_ATTR).append("=\"").
            append(version).append("\"");
        if ((id == null) || (id.length() == 0)) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.toXMLString(): assertion id missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_assertion_id"));
        } 
        sb.append(" ").append(ASSERTION_ID_ATTR).append("=\"").
            append(id).append("\"");
        if (issueInstant == null) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.toXMLString(): issue instant missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_issue_instant")); 
        }
        String instantStr = DateUtils.toUTCDateFormat(issueInstant);
        sb.append(" ").append(ASSERTION_ISSUEINSTANT_ATTR).append("=\"").
            append(instantStr).append("\"").append(">\n");
        if (issuer == null) {
            SAML2SDKUtils.debug.error(
                "AssertionImpl.toXMLString(): issuer missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelement_issuer")); 
        }
        sb.append(issuer.toXMLString(includeNSPrefix, false));
        if (signature != null) {
            sb.append(signature); 
        }
        if (subject != null) {
            sb.append(subject.toXMLString(includeNSPrefix, false));
        }
        if (conditions != null) {
            sb.append(conditions.toXMLString(includeNSPrefix, false));
        }
        if (advice != null) {
            sb.append(advice.toXMLString(includeNSPrefix, false));
        }
        int length = 0;
        if (statements != null) {
            length = statements.size();
            for (int i = 0; i < length; i++) {
                String str = (String)statements.get(i);
                sb.append(str);
            }
        }
        if (authnStatements != null) {
            length = authnStatements.size();
            for (int i = 0; i < length; i++) {
                AuthnStatement st = (AuthnStatement)authnStatements.get(i);
                sb.append(st.toXMLString(includeNSPrefix, false));
            }
        }
        if (authzDecisionStatements != null) {
            length = authzDecisionStatements.size();
            for (int i = 0; i < length; i++) {
                AuthzDecisionStatement st = 
                    (AuthzDecisionStatement)authzDecisionStatements.get(i);
                sb.append(st.toXMLString(includeNSPrefix, false));
            }
        }
        if (attributeStatements != null) {
            length = attributeStatements.size();
            for (int i = 0; i < length; i++) {
                AttributeStatement st = 
                    (AttributeStatement)attributeStatements.get(i);
                sb.append(st.toXMLString(includeNSPrefix, false));
            }
        }
        sb.append("</").append(appendNS).append(ASSERTION_ELEMENT).
        append(">\n");
        //return SAML2Utils.removeNewLineChars(sb.toString());
       return sb.toString();
    }

   /**
    * Returns a String representation
    *
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString() throws SAML2Exception {
        return this.toXMLString(true, false);
    }

   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        if (isMutable) {
            if (authnStatements != null) {
                int length = authnStatements.size();
                for (int i = 0; i < length; i++) {
                    AuthnStatement authn = 
                        (AuthnStatement)authnStatements.get(i);
                    authn.makeImmutable();
                }
                authnStatements = Collections.unmodifiableList(
                                             authnStatements);
            }
            if (authzDecisionStatements != null) {
                int length = authzDecisionStatements.size();
                for (int i = 0; i < length; i++) {
                    AuthzDecisionStatement authz =
                        (AuthzDecisionStatement)authzDecisionStatements.get(i);
                    authz.makeImmutable();
                }
                authzDecisionStatements = Collections.unmodifiableList(
                                              authzDecisionStatements);
            }
            if (attributeStatements != null) {
                int length = attributeStatements.size();
                for (int i = 0; i < length; i++) {
                    AttributeStatement attr =
                        (AttributeStatement)attributeStatements.get(i);
                    attr.makeImmutable();
                }
                attributeStatements = Collections.unmodifiableList(
                                              attributeStatements);
            }
            if (statements != null) {
                statements = Collections.unmodifiableList(statements);
            }
            if (conditions != null) {
                conditions.makeImmutable();
            }
            if (issuer != null) {
                issuer.makeImmutable();
            }
            if (subject != null) {
                subject.makeImmutable();
            }
            if (advice != null) {
                advice.makeImmutable();
            }
            isMutable = false;
        }
    }

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable() {
        return isMutable;
    }
}
