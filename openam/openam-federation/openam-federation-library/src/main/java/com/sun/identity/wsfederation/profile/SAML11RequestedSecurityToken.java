/**
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
 * $Id: SAML11RequestedSecurityToken.java,v 1.7 2009/12/14 23:42:48 mallas Exp $
 * 
 */

package com.sun.identity.wsfederation.profile;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectStatement;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.logging.LogUtil;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * SAML11RequestedSecurityToken represents a concrete RequestedSecurityToken -
 * one containing a SAML 1.1 assertion.
 */
public class SAML11RequestedSecurityToken implements RequestedSecurityToken {
    // Just get this system property once - it should never change!
    private static boolean removeCarriageReturns = 
        System.getProperty("line.separator").equals("\r\n");
    private static Debug debug = WSFederationUtils.debug;
    protected Assertion assertion = null;
    protected String xmlString = null;
    protected boolean signed = false;
    protected Element assertionE = null;

    /**
     * Creates a SAML11RequestedSecurityToken given a DOM Node
     * @param token a DOM Node representing a RequestedSecurityToken
     */
    public SAML11RequestedSecurityToken(Node token) 
        throws WSFederationException {
        String classMethod = 
            "SAML11RequestedSecurityToken.SAML11RequestedSecurityToken(Node)";
        
        if ( ! token.getLocalName().equals("RequestedSecurityToken") ){
            debug.error("Got node " + 
                token.getLocalName() + " (expecting " + 
                SAMLConstants.assertionSAMLNameSpaceURI + ":" + 
                SAMLConstants.TAG_ASSERTION + ")");
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("invalidToken"));
        }
        
        Element ae = (Element)token.getFirstChild();
        
        if (!(ae.getNamespaceURI().
                equals(SAMLConstants.assertionSAMLNameSpaceURI)
            && ae.getLocalName().equals(SAMLConstants.TAG_ASSERTION)))
        {
            debug.error("Got node " + 
                ae.getLocalName() + " (expecting " + 
                SAMLConstants.assertionSAMLNameSpaceURI + ":" + 
                SAMLConstants.TAG_ASSERTION + ")");
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("invalidToken"));
        }
        this.assertionE = ae;
        try {
            assertion = new Assertion(ae);
        }
        catch (SAMLException se)
        {
            if ( debug.messageEnabled() ) {
                debug.message("Caught SAMLException, " + 
                    "rethrowing",se);
            }
            throw new WSFederationException(se);
        }
        
        if ( debug.messageEnabled() ) {
            debug.message(classMethod + "found Assertion with issuer:" +
                assertion.getIssuer());
        }
        
        List signs = XMLUtils.getElementsByTagNameNS1(ae,
            SAMLConstants.XMLSIG_NAMESPACE_URI,
            SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            xmlString = XMLUtils.print(ae);
            signed = true;
            if ( debug.messageEnabled() ) {
                debug.message(classMethod + "found signature");
            }
        } else if (signsSize != 0) {
            if ( debug.messageEnabled() ) {
                debug.message(classMethod + 
                    "included more than one Signature element.");
            }
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("moreElement"));
        }
    }

    /**
     * Creates a SAML11RequestedSecurityToken.
     * @param realm the realm of the entities.
     * @param spEntityId service provider entity ID - consumer of the token.
     * @param idpEntityId identity provifer entity ID - issuer of the token.
     * @param notBeforeSkew number of seconds to subtract from current time
     * to form Assertion notBefore time.
     * @param effectiveTime length of time, in seconds, from Assertion's 
     * notBefore time to its notOnOrAfter time.
     * @param certAlias alias of the signing certificate. null means do not
     * sign the assertion
     * @param authMethod mechanism by which the subject authenticated to the 
     * identity provider
     * @param authInstant time at which the subject authenticated to the 
     * identity provider
     * @param ni SAML 1.1 NameIdentitifer for the subject
     * @param attributes List of com.sun.identity.saml.assertion.Attribute to 
     * include in the Assertion
     * @throws com.sun.identity.wsfederation.common.WSFederationException in 
     * case of error.
     */
    public SAML11RequestedSecurityToken(String realm, String spEntityId, 
        String idpEntityId, int notBeforeSkew, int effectiveTime,
        String certAlias, String authMethod, Date authInstant, 
        NameIdentifier ni, List attributes)
        throws WSFederationException
    {
        String classMethod = "SAML11RequestedSecurityToken."+
            "SAML11RequestedSecurityToken(String*)";
        
        try {
            Subject sub = new Subject(ni);
            Set<Statement> statements = 
                new HashSet<Statement>();
            statements.add(new AuthenticationStatement(authMethod, authInstant,
                            sub, null, null));

            if ((attributes != null) && (!attributes.isEmpty())) {
                statements.add(new AttributeStatement(sub, attributes));
            }

            Date issueInstant = new Date();

            long skewPeriod = (long)notBeforeSkew * 1000L;
            Date notBefore = new Date(issueInstant.getTime() - skewPeriod);            
            long period = (long)effectiveTime * 1000L;
            Date notAfter = new Date(issueInstant.getTime() + period);
            WSFederationMetaManager metaManager =
                WSFederationUtils.getMetaManager();
            FederationElement idp = 
                metaManager.getEntityDescriptor(realm, idpEntityId);
            FederationElement sp = 
                metaManager.getEntityDescriptor(realm, spEntityId);
            
            String issuer = metaManager.getTokenIssuerName(idp);
        
            List<String> targets = new ArrayList<String>();
            targets.add(metaManager.getTokenIssuerName(sp));
            
            AudienceRestrictionCondition arc = 
                new AudienceRestrictionCondition(targets);
            
            Conditions cond = new Conditions(notBefore, notAfter, null, arc);
            assertion = new Assertion(null /* assertionID */, issuer, 
                issueInstant, cond, statements);

            String aIDString = assertion.getAssertionID();

            if ( certAlias != null )
            {
                assertion.signXML(certAlias);
            }
            
            if (LogUtil.isAccessLoggable(Level.FINER)) {
                String[] data = { assertion.toString(true, true)};
                LogUtil.access(java.util.logging.Level.FINER,
                    LogUtil.ASSERTION_CREATED, data);
            } else {
                String[] data = { aIDString};
                LogUtil.access(java.util.logging.Level.INFO,
                    LogUtil.ASSERTION_CREATED, data);
            }
        } catch (SAMLException se) {
            throw new WSFederationException(se);
        }
    }

    /**
     * @return the unique identifier of the RequestedSecurityToken. Maps to the
     * SAML 1.1 Assertion's AssertionID
     */
    public String getTokenId()
    {
        return assertion.getAssertionID();
    }
    
    /**
     * @return the issuer of the RequestedSecurityToken.
     */
    public String getIssuer()
    {
        return assertion.getIssuer();
    }
    
    /**
     * @return a list of attributes of type 
     * <code>com.sun.identity.saml.assertion.Attribute</code>
     */
    public List getAttributes()
    {
        AttributeStatement attributeStatement = null;
        Iterator stmtIter = assertion.getStatement().iterator();
        while (stmtIter.hasNext()) {
            Statement statement = (Statement) stmtIter.next();
            if (statement.getStatementType() 
                == Statement.ATTRIBUTE_STATEMENT) {
                attributeStatement =
                    (AttributeStatement)statement;
                break;
            }
        }
        if ( attributeStatement == null ) {
            return null;
        }
        return attributeStatement.getAttribute();
    }

    /**
     * @return the underlying SAML 1.1 Assertion
     */
    public Assertion getAssertion()
    {
        return assertion;
    }
    
    /** 
     * This method marshalls the token, returning a String comprising the 
     * textual XML representation.
     * @return The textual XML representation of the token.
     */
    public String toString()
    {
        if(assertionE != null) {
           return XMLUtils.print(SAMLUtils.getCanonicalElement(
                  assertionE));
        }

        StringBuffer buffer = new StringBuffer();

        // Pass (true,true) to assertion.toString so we get namespace
        String assertionString = assertion.toString(true,true);
        if ( removeCarriageReturns )
        {
            // Xalan uses the line.separator system property when creating
            // output - i.e. on Windows, uses \r\n
            // We ALWAYS want \n, or signatures break in ADFS - issue # 3927
            //
            // NOTE - transformer.setOutputProperty(
            //     "{http://xml.apache.org/xalan}line-separator","\n");
            // DOESN'T WORK WITH com.sun.org.apache.xalan.internal
            //
            // Doing this here rather than in XMLUtils.print(Node, String)
            // minimizes the scope of the change.
            assertionString = assertionString.replaceAll("\r\n", "\n");
        }        

        buffer.append("<wst:RequestedSecurityToken>")
            .append(assertionString)
            .append("</wst:RequestedSecurityToken>");

        return buffer.toString();                
    }
    
    /**
     * Verifies the token's validity, checking the signature, validity period 
     * etc.
     * @param realm the realm of the local entity
     * @param hostEntityId the local entity ID
     * @param timeskew permitted skew between service provider and identity 
     * provider clocks, in seconds
     * @return a Map of relevant data including Subject and the List of 
     * Assertions.
     * @throws com.sun.identity.wsfederation.common.WSFederationException in 
     * case of any error - invalid token signature, token expired etc.
     */
    public Map<String,Object> verifyToken(String realm, String hostEntityId, 
        int timeskew)
        throws WSFederationException
    {
        String classMethod = "SAML11RequestedSecurityToken.verifyToken";
        
        // check that assertion issuer is trusted by the local entity
        String issuer = assertion.getIssuer();
        WSFederationMetaManager metaManager =
            WSFederationUtils.getMetaManager();
        String remoteEntityId = 
            metaManager.getEntityByTokenIssuerName(realm, issuer);
        if (! metaManager.isTrustedProvider(
                        realm, hostEntityId, remoteEntityId)) {
            String[] data = 
                {LogUtil.isErrorLoggable(Level.FINER)? this.toString() : 
                this.getTokenId(), 
                realm, hostEntityId};
            LogUtil.error(Level.INFO,
                    LogUtil.UNTRUSTED_ISSUER,
                    data,
                    null);
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("untrustedIssuer"));
        }

        SPSSOConfigElement spConfig = 
            metaManager.getSPSSOConfig(realm, hostEntityId);
        if ( spConfig == null )
        {
            debug.error(classMethod + "cannot find configuration for SP " 
                + hostEntityId);
            throw new WSFederationException("unableToFindSPConfiguration");
        }
        
        String strWantAssertionSigned = 
            WSFederationMetaUtils.getAttribute(spConfig, 
            WSFederationConstants.WANT_ASSERTION_SIGNED);
        
        // By default, we want to sign assertions
        boolean wantAssertionSigned = (strWantAssertionSigned != null)
            ? Boolean.parseBoolean(strWantAssertionSigned)
            : true;

        if ( wantAssertionSigned &&
            (!WSFederationUtils.isSignatureValid(assertion, realm, 
            remoteEntityId))) {
            // isSignatureValid will log the error
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("invalidSignature"));
        }

        // TODO: check AudienceRestrictionCondition

        Subject assertionSubject = null;
        
        Iterator stmtIter = assertion.getStatement().iterator();
        while (stmtIter.hasNext()) {
            Statement statement = (Statement) stmtIter.next();
            if (statement.getStatementType() 
                == Statement.AUTHENTICATION_STATEMENT) {
                assertionSubject =
                    ((SubjectStatement)statement).getSubject();
                break;
            }
        }
        
        if ( assertionSubject == null ) {
            String[] data = 
                {LogUtil.isErrorLoggable(Level.FINER)? this.toString() : 
                this.getTokenId()};
            LogUtil.error(Level.INFO,
                    LogUtil.MISSING_SUBJECT,
                    data,
                    null);
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("missingSubject"));
        }

        // must be valid (timewise)
        if (!WSFederationUtils.isTimeValid(assertion, timeskew)) {
            // isTimeValid will log the error
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("timeInvalid"));
        }

        List assertions = new ArrayList();
        assertions.add(assertion);

        Map<String,Object> attrMap = new HashMap<String,Object>();
        
        attrMap.put(SAML2Constants.SUBJECT, assertionSubject);
        attrMap.put(SAML2Constants.POST_ASSERTION, assertion);        
        attrMap.put(SAML2Constants.ASSERTIONS, assertions);

        // TODO
        int authLevel = 0;
        
        if (authLevel >= 0) {
            attrMap.put(SAML2Constants.AUTH_LEVEL, new Integer(authLevel));
        }
        
        Date sessionNotOnOrAfter = assertion.getConditions().getNotOnorAfter();
        if (sessionNotOnOrAfter != null) {
            long maxSessionTime = (sessionNotOnOrAfter.getTime() -
                    System.currentTimeMillis()) / 60000;
            if (maxSessionTime > 0) {
                attrMap.put(SAML2Constants.MAX_SESSION_TIME,
                        new Long(maxSessionTime));
            }
        }
        
        if ( debug.messageEnabled() ) {
            debug.message(classMethod +" Attribute Map : " + attrMap);
        }
        
        return attrMap;
    }
}
