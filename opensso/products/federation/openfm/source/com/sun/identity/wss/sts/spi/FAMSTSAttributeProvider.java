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
 * $Id: FAMSTSAttributeProvider.java,v 1.22 2010/01/15 18:54:35 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.sts.spi;

import javax.security.auth.Subject;
import com.sun.xml.ws.api.security.trust.*;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.iplanet.security.x509.CertUtils;
import java.security.cert.X509Certificate;
import org.w3c.dom.Element;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.sts.STSClientUserToken;
import com.sun.identity.wss.sts.FAMSTSException;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.sts.ClientUserToken;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.wss.sts.config.FAMSTSConfiguration;
import javax.xml.stream.XMLStreamReader;
import com.sun.xml.wss.saml.util.SAMLUtil;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.wss.security.SAML11AssertionValidator;
import com.sun.identity.wss.security.SAML2AssertionValidator;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.logging.LogUtil;
import com.sun.identity.wss.trust.ClaimType;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * The STS attribute provider is used to retrieve an authenticated user or
 * profile attributes and gives it to the assertion generator so that
 * these attributes could be part of SAML attribute statements. 
 *
 * The attribute checks first if the end user's SSOToken is present in the
 * <code>OnBehalfOf</code> element in the WS-Trust request and generates
 * SAML Attributes from the user profile. This is the case usually if the STS
 * and web services client is deployed locally on the same or trusted Federal
 * OpenSSO instances. If not, it tries to retrieve the web services 
 * client profile attributes if it exists.
 */ 
public class FAMSTSAttributeProvider implements STSAttributeProvider {

    private static final String FAM_TOKEN = "FAMToken";
    private static final String SAML_ATTRIBUTE_MAP = "SAMLAttributeMapping"; 
    private static final String NAMEID_MAPPER_CLASS = "NameIDMapper";
    private static final String ATTR_NAMESPACE = "AttributeNamespace";
    private static final String STS = "sts";
    private static final String MEMBERSHIPS = "Memberships";
    private static final String INCLUDE_MEMBERSHIPS = "includeMemberships";
    private static final String defaultNS = "http://example.com";
    
    private Map attributeMap = new HashMap();        
    private SSOToken ssoToken = null;
    protected static SSOTokenManager tokenManager;
    
    /**
     * Returns all claimed attributes for a given subject.
     */
    public Map<QName, List<String>> getClaimedAttributes(Subject subject, 
            String appliesTo, String tokenType, Claims claims) {

        FAMSTSConfiguration stsConfig = new FAMSTSConfiguration();
        String subjectName = null;
        
        try {
            subjectName = getSubjectNameFromCustomToken(subject,stsConfig);
        } catch (FAMSTSException fse) {
            STSUtils.debug.error("FAMSTSAttributeProvider.getClaimedAttributes"
                + " getSubjectNameFromCustomToken failed : ", fse);
            return null;
        }

        if(subjectName == null) {
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: subject is null from 'On Behalf Of' OR Custom token");
           }
           subjectName = getAuthenticatedSubject(subject);
        }
        
        if(subjectName == null) {            
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: subject is null from authenticated subject");
           }
           Element samlAssertionE = null;
           Iterator iter = subject.getPublicCredentials().iterator();
           Object  object = iter.next();
           if(object instanceof X509Certificate) {
              X509Certificate cert = (X509Certificate)object;
              subjectName = CertUtils.getSubjectName(cert); 
           } else if (object instanceof XMLStreamReader) {
              XMLStreamReader reader = (XMLStreamReader) object;
              X509Certificate cert = null;
              //To create a DOM Element representing the Assertion :                      
              try {
                  samlAssertionE = SAMLUtil.createSAMLAssertion(reader);
                  parseSAMLAssertion(samlAssertionE, subject, stsConfig);
              } catch (Exception ex) {
                  STSUtils.debug.error("FAMSTSAttributeProvider.getClaimed"
                      +  "Attributes: assertion validation failed");
              }                                               
           } else if (object instanceof Element) {
               samlAssertionE = (Element)object;
               if(samlAssertionE.getLocalName().equals("Assertion")) {
                  parseSAMLAssertion(samlAssertionE, subject, stsConfig);
               }
           }
        }
        if(subjectName == null) {
           if(STSUtils.debug.messageEnabled()) {
              STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
              "Attributes: subject from X509certificate is null" +
              " Checking in subject principals");
           }
           Set<Principal> principals = subject.getPrincipals();
           if (principals != null){
               final Iterator iterator = principals.iterator();
               while (iterator.hasNext()){                   
                    String cnName = principals.iterator().next().getName();
                    String[] cnNameParts = cnName.split(",");
                    if ((cnNameParts.length == 1) && 
                        (cnNameParts[0].indexOf("=") != -1)) {
                        int pos = cnNameParts[0].indexOf("=");
                        subjectName = cnNameParts[0].substring(pos+1);
                    } else {
                        subjectName = cnName;
                    }
                    break;
               }       
           }
        }
        
        if(STSUtils.debug.messageEnabled()) {
            STSUtils.debug.message("FAMSTSAttributeProvider.getClaimed" +
                "Attributes: subjectName : " + subjectName);
        }

        if(subjectName == null) {
           STSUtils.debug.error("FAMSTSAttributeProvider.getClaimed" + 
                " Subject could not found.");
           return null;
        }
        
        String[] data = {subjectName};
        LogUtil.access(Level.INFO,
                LogUtil.IDENTITY_SUBJECT_NAME,
                data,
                null);

        Map<QName, List<String>> attrs = new HashMap<QName, List<String>>();
        String namespace = defaultNS;
        Set tmp = null;
        Map agentConfig = null;
        
        if(appliesTo != null) {
           if(stsConfig.getSTSEndpoint().equals(appliesTo)){
              agentConfig = STSUtils.getSTSSAMLAttributes(stsConfig);
           } else {
              agentConfig = STSUtils.getAgentAttributes(
                 appliesTo, null, null, ProviderConfig.WSP);
           }
        }
        
        if(agentConfig != null && !agentConfig.isEmpty()) {
           tmp = (Set)agentConfig.get(ATTR_NAMESPACE);
           if (tmp != null && !tmp.isEmpty()) {
               namespace = (String)tmp.iterator().next();
           }
        }

	    QName nameIdQName = new QName(namespace,
                            STSAttributeProvider.NAME_IDENTIFIER);
	    List<String> nameIdAttrs = new ArrayList<String>();
	    nameIdAttrs.add(getUserPseduoName(subjectName, agentConfig));
	    attrs.put(nameIdQName, nameIdAttrs);
        
        if(agentConfig == null || agentConfig.isEmpty()) {
           STSUtils.debug.error("FAMSTSAttributeProvider.getClaimed" + 
                " Agent configuration not defined for " + appliesTo);           
           return attrs;
        }

        
        tmp = (Set)agentConfig.get(SAML_ATTRIBUTE_MAP);
        Map samlAttributeMap = null;
        if(tmp != null && !tmp.isEmpty()) {
           samlAttributeMap = WSSUtils.getSAMLAttributes(
                   subjectName, tmp, namespace, ssoToken);
        }
        if(samlAttributeMap != null) {
           attrs.putAll(samlAttributeMap);
        }
        
        //Adding the attributes from authenticated SAML Assertion to the
        //newly created assertion.
        if(attributeMap != null && !attributeMap.isEmpty()) {
           Set entries = attributeMap.keySet();
           for(Iterator  attrIter = entries.iterator(); attrIter.hasNext();) {
               String key = (String)attrIter.next();
               String val = (String)attributeMap.get(key);
               QName attrQName = new QName(namespace, key);
               List<String> valList = new ArrayList<String>();
               valList.add(val);
               attrs.put(attrQName, valList);
           }
        }

        boolean includeMemberships = Boolean.valueOf(
                CollectionHelper.getMapAttr(
                agentConfig, INCLUDE_MEMBERSHIPS, "false")).booleanValue(); 
        if(includeMemberships){
           Map memberships = WSSUtils.getMembershipAttributes(
                   subjectName, namespace); 
           if(memberships != null && !memberships.isEmpty()) {
              attrs.putAll(memberships);
           }
        }
        if(claims != null) {
           Set claimNames = getClaimNames(claims);
           Map claimAttrs = WSSUtils.getRequestedClaims(
                   subjectName, claimNames, ssoToken);
           attrs.putAll(claimAttrs);
        }
        String attrsStr = attrs.toString();
        String[] data2 = {attrsStr};
        LogUtil.access(Level.INFO,
                    LogUtil.ATTR_MAP_FOR_SP,
                    data2,
                    null);
       
	    return attrs;
    }
    
    private String getUserPseduoName(String userName, Map agentConfig){
      
        if(agentConfig == null) {
           return userName;
        }
                
        String nameIDImpl = CollectionHelper.getMapAttr(
                            agentConfig, NAMEID_MAPPER_CLASS); 
        if(nameIDImpl == null) {
           return userName;
        }

        return WSSUtils.getUserPseduoName(userName, nameIDImpl);

    }
       
    /**
     * Returns end user's principal if OpenSSO Token is present or
     * any other custom token, otherwise returns null.
     */
    private String getSubjectNameFromCustomToken(Subject subject,
        FAMSTSConfiguration stsConfig) throws FAMSTSException {
        Iterator iter = subject.getPublicCredentials().iterator();
        while(iter.hasNext()) {
            Object object = iter.next();
            if(object instanceof Element) {
               Element credential = (Element)object;
               if(credential.getLocalName().equals(FAM_TOKEN)) {
                  try {
                      STSClientUserToken userToken =
                          new STSClientUserToken(credential);
                      String tokenID = userToken.getTokenId();
                      if(userToken.getType().equals(
                              SecurityToken.WSS_SAML2_TOKEN)) {
                          Element assertionE = XMLUtils.toDOMDocument(
                              tokenID, STSUtils.debug).getDocumentElement();
                          SAML2AssertionValidator validator = 
                              new SAML2AssertionValidator(
                              assertionE, stsConfig);
                          return validator.getSubjectName();
                      } else if(userToken.getType().equals(
                              SecurityToken.WSS_SAML_TOKEN)) {                          
                          Element assertionE = XMLUtils.toDOMDocument(
                              tokenID, STSUtils.debug).getDocumentElement();
                          SAML11AssertionValidator validator = 
                              new SAML11AssertionValidator(
                              assertionE, stsConfig);
                          return validator.getSubjectName();
                      } else if(userToken.getType().equals(
                              SecurityToken.WSS_FAM_SSO_TOKEN)) {

                          String appTokenId = userToken.getAppTokenID();
                          SSOToken currentToken = getSSOToken(tokenID,appTokenId);
                          ssoToken = currentToken;
                          if (ssoToken != null) {
                              return ssoToken.getPrincipal().getName();
                          } else {
                              return null;
                          }
                      } else {
                         return null;
                      }
                  } catch (FAMSTSException fae) {
                      if(STSUtils.debug.messageEnabled()) {
                         STSUtils.debug.message("FAMSTSAttributeProvider.get" +
                         "SubjectNameFromCustomToken: FAMException", fae);
                      }
                      throw new FAMSTSException(fae.getMessage());
                  } catch (SSOException se) {
                      if(STSUtils.debug.messageEnabled()) {
                         STSUtils.debug.message("FAMSTSAttributeProvider.get" +
                         "SubjectNameFromCustomToken: SSOException", se);
                      }
                      throw new FAMSTSException(se.getMessage());
                  } catch (SecurityException sec) {
                      if(STSUtils.debug.messageEnabled()) {
                         STSUtils.debug.message("FAMSTSAttributeProvider.get" +
                         "SubjectNameFromCustomToken: SecurityException", sec);
                      }
                      throw new FAMSTSException(sec.getMessage());
                  }
               } else {
                  String customToken = stsConfig.getClientUserTokenClass();
                  if(customToken != null && customToken.length() != 0) {
                     try {
                         Class customTokenClass = 
                             (Thread.currentThread().getContextClassLoader()).
                             loadClass(customToken);  
                         ClientUserToken userToken = (ClientUserToken)
                                      customTokenClass.newInstance();
                         userToken.parse(credential);
                         return userToken.getPrincipalName();
                     } catch (Exception ex) {
                         //The element could be anything not necessarily
                         // OBOToken. 
                         if(STSUtils.debug.messageEnabled()) {
                            STSUtils.debug.message("FAMSTSAttributeProvider." +
                             "CheckForCustomTokens: " + ex.getMessage());
                         }
                     }
                  }
               }
            }
        }
        return null;
    }
       
    /**
     *  Returns the principal from the authenticated Subject if available
     *  through private credentials
     * @param subject authenticated subject
     * @return the authenticated principal name
     */
    private String getAuthenticatedSubject(Subject subject) {        
        final Subject sub = subject;
        try {                
            AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                   Set creds = sub.getPrivateCredentials();
                   if(creds != null && !creds.isEmpty()) {
                      for(Iterator iter = creds.iterator(); iter.hasNext();) {
                          Object cred = iter.next();
                          if(cred instanceof SSOToken) {
                             ssoToken = (SSOToken)cred;
                          }
                      }
                   }
                   return null;
                }
            });               
                
        } catch (Exception e) {
            STSUtils.debug.error("FAMSTSAttributeProvider.getAuthenticated" +
                   "Subject: Priveleged exception error", e);
            return null;                    
        }
        try {
            if(ssoToken != null) {
               return ssoToken.getPrincipal().getName(); 
            }
        } catch (SSOException se) {
           STSUtils.debug.error("FAMSTSAttributeProvider.getAuthenticated" +
                   "Subject: SSOException", se);
        }
        return null;
    }
    
    private void parseSAMLAssertion(Element samlAssertionE, Subject subject,
         FAMSTSConfiguration stsConfig) {
                              
         X509Certificate cert = null;
         //To create a DOM Element representing the Assertion :                      
         try {
             //samlAssertionE = SAMLUtil.createSAMLAssertion(reader);
             String namespace = samlAssertionE.getNamespaceURI();
             if(SAMLConstants.assertionSAMLNameSpaceURI.equals(
                          namespace)) {
                SAML11AssertionValidator validator =
                   new SAML11AssertionValidator(samlAssertionE, stsConfig);
                cert = validator.getKeyInfoCert();
                attributeMap = validator.getAttributes();
             } else if (
                SAML2Constants.ASSERTION_NAMESPACE_URI.equals(namespace)) {
                SAML2AssertionValidator validator =
                     new SAML2AssertionValidator(samlAssertionE, stsConfig);
                cert = validator.getKeyInfoCert();
                attributeMap = validator.getAttributes();
             }                          
             if(cert != null) {
                subject.getPublicCredentials().add(cert); 
             }
         } catch (Exception ex) {
             STSUtils.debug.error("FAMSTSAttributeProvider.parseSAMLAssertion"
                 +  " failed : ", ex);
         }
    }
    
    private Set getClaimNames(Claims claims) {
        Set claimNames = new HashSet();
        String dialect = claims.getDialect();
        if(dialect != null && ClaimType.IDENTITY_NS.equals(dialect)) {
           List list = claims.getAny();
           if(list != null && !list.isEmpty()) {
              Iterator iter = list.iterator();
              while(iter.hasNext()) {
                  Element claimE = (Element)iter.next();
                  try {
                      ClaimType claimType = new ClaimType(claimE);                  
                      claimNames.add(claimType.getName());
                  } catch (Exception ex) {
                      STSUtils.debug.message("FAMSTSAttributeProvider."
                              + " getClaimNames: ", ex);
                     // ignore 
                  }
              }
           }
        }
        return claimNames;
    }

    /**
     * Check if agent token ID is appended to the token string.
     * if yes, we use it as a restriction context. This is meant
     * for cookie hijacking feature where agent appends the agent token ID
     * to the user sso token before sending it over to the server for
     * validation.
     */
    protected SSOToken getSSOToken(String token, String appTokenId)
        throws SSOException {

        boolean useAppToken = Boolean.valueOf(
                            SystemConfigurationUtil.getProperty(
                            Constants.IS_ENABLE_UNIQUE_COOKIE,
                            "false")).booleanValue();

        if (tokenManager == null) {
            tokenManager = SSOTokenManager.getInstance();
        }
        if (!useAppToken) {
            return tokenManager.createSSOToken(token);
        }

        SSOToken stoken = null;
        final String ftoken = token;

        try {           
            Object context = tokenManager.createSSOToken(appTokenId);
            stoken = (SSOToken)RestrictedTokenContext.doUsing(context,
                new RestrictedTokenAction() {
                    public Object run() throws Exception {
                        return tokenManager.createSSOToken(ftoken);
                    }
            });
       } catch (SSOException e) {
           STSUtils.debug.error("FAMSTSAttributeProvider:getSSOToken", e);
           return tokenManager.createSSOToken(token);
       } catch (Exception e) {
           STSUtils.debug.error("FAMSTSAttributeProvider:getSSOToken", e);
       }
       return stoken;
    }
}

