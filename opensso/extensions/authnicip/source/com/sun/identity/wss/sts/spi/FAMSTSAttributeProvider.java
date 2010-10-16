/* The contents of this file are subject to the terms
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
 * $Id: FAMSTSAttributeProvider.java,v 1.2 2008/03/31 05:25:08 mrudul_uchil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.wss.sts.spi;

import javax.security.auth.Subject;
import com.sun.xml.ws.api.security.trust.*;
import com.sun.xml.wss.SubjectAccessor;
import java.security.Principal;
import java.util.*;
import javax.xml.namespace.*;
import com.iplanet.security.x509.CertUtils;
import java.security.cert.X509Certificate;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import org.w3c.dom.Element;
import com.sun.identity.wss.sts.STSConstants;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import org.w3c.dom.NodeList;

import org.xmldap.exceptions.TokenIssuanceException;
import com.sun.identity.cardfactory.PPIDHelper;
import com.sun.identity.cardfactory.CardSpaceConstants;

public class FAMSTSAttributeProvider implements STSAttributeProvider {

    public static Debug debug = Debug.getInstance("famSTSAttributeProvider");

    public Map<QName, List<String>> getClaimedAttributes(Subject subject_input, String appliesTo, String tokenType, Claims claims) {
        Subject subject = SubjectAccessor.getRequesterSubject();
        if (subject == null) {
            subject = new Subject();
            SubjectAccessor.setRequesterSubject(subject);
        }
        debug.message("Subject : " + subject.toString());
        debug.message("Subject principals : " + subject.getPrincipals());
        //debug.message("Subject private cred : " + subject.getPrivateCredentials());
        debug.message("Subject public cred : " + subject.getPublicCredentials());
        debug.message("Applies To : " + appliesTo);
        debug.message("Token type : " + tokenType);

        String name = null;
        String uid = null;
        String givenname = null;
        String sn = null;
        String mail = null;
        String privatepersonalidentifier = null;
        String streetaddress = null;
        String city = null;
        String state = null;
        String postalcode = null;
        String country = null;
        String homephone = null;
        String workphone = null;
        String mobilephone = null;
        String gender = null;
        String dob = null;
        String webpage = null;

        Set<Object> pubCred = subject.getPublicCredentials();

        if ((pubCred != null) && !pubCred.isEmpty()) {
            Iterator iter = pubCred.iterator();
            Object object = iter.next();
            if (object instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) object;
                name = CertUtils.getSubjectName(cert);
            }
        }

        Set<Principal> principals = subject.getPrincipals();
        if ((principals != null) && !principals.isEmpty()) {
            final Iterator iterator = principals.iterator();
            while (iterator.hasNext()) {
                String cnName = principals.iterator().next().getName();
                int pos = cnName.indexOf("=");
                name = cnName.substring(pos + 1);
                break;
            }
        }

        final SubjectSecurity subjectSecurity = new SubjectSecurity();
        final Subject sub = subject;
        if (sub != null) {
            debug.message("sub : " + sub.toString());
        }
        try {
            AccessController.doPrivileged(
                    new PrivilegedAction() {

                        public Object run() {
                            Set creds = sub.getPrivateCredentials();
                            if (creds != null) {
                                Iterator iter = creds.iterator();
                                while (iter.hasNext()) {
                                    Object credObj = iter.next();
                                    if (credObj instanceof SSOToken) {
                                        subjectSecurity.ssoToken = (SSOToken) credObj;
                                    }
                                }
                            }
                            return null;
                        }
                    });
            debug.message("subjectSecurity.ssoToken : " + subjectSecurity.ssoToken);
            if (subjectSecurity.ssoToken != null) {
                debug.message("Valid SSOToken : " + (subjectSecurity.ssoToken).getTokenID());
                debug.message("Valid User DN from SSOToken : " + (subjectSecurity.ssoToken).getPrincipal().getName());
                try {
                    AMIdentity amid = IdUtils.getIdentity(subjectSecurity.ssoToken);
                    Map attrs = amid.getAttributes();
                    debug.message("User Attributes: ");

                    for (Iterator i = attrs.keySet().iterator(); i.hasNext();) {
                        String attrName = (String) i.next();
                        Set values = (Set) attrs.get(attrName);
                        debug.message(attrName + "=" + values);
                    }
                    uid = (String) ((Set) attrs.get("uid")).iterator().next();
                    givenname = (String) ((Set) attrs.get("givenname")).iterator().next();
                    sn = (String) ((Set) attrs.get("sn")).iterator().next();
                    mail = (String) ((Set) attrs.get("mail")).iterator().next();
                    privatepersonalidentifier = (String) ((Set) attrs.get("privatepersonalidentifier")).iterator().next();
                    streetaddress = (String) ((Set) attrs.get("postaladdress")).iterator().next();
                    city = (String) ((Set) attrs.get("locality")).iterator().next();
                    state = (String) ((Set) attrs.get("stateorprovince")).iterator().next();
                    country = (String) ((Set) attrs.get("country")).iterator().next();
                    postalcode = (String) ((Set) attrs.get("postalcode")).iterator().next();
                    homephone = (String) ((Set) attrs.get("telephonenumber")).iterator().next();
                    workphone = (String) ((Set) attrs.get("otherphone")).iterator().next();
                    mobilephone = (String) ((Set) attrs.get("mobilephone")).iterator().next();
                    gender = (String) ((Set) attrs.get("gender")).iterator().next();
                    dob = (String) ((Set) attrs.get("dateofbirth")).iterator().next();
                    webpage = (String) ((Set) attrs.get("webpage")).iterator().next();
                } catch (Exception e) {
                    debug.error("Error in retrieving User attributes from AMIdentity : " + e.toString());
                }
                debug.message("uid : " + uid);
                debug.message("given name : " + givenname);
                debug.message("last name : " + sn);
                debug.message("email : " + mail);
                debug.message("privatepersonalidentifier : " + privatepersonalidentifier);
            }
        } catch (Exception e) {
            debug.error("Error in retrieving SSOToken properties : " + e.toString());
        }

        Map<QName, List<String>> attrs = new HashMap<QName, List<String>>();

        QName nameIdQName =
                new QName("http://sun.com", STSAttributeProvider.NAME_IDENTIFIER);
        List<String> nameIdAttrs = new ArrayList<String>();
        nameIdAttrs.add(getUserPseduoName(name));
        attrs.put(nameIdQName, nameIdAttrs);

        /*
        QName testQName = new QName("http://sun.com","Role");
        List<String> testAttrs = new ArrayList<String>();
        testAttrs.add(getUserRole(name));
        attrs.put(testQName,testAttrs);*/

        List<Object> claimTypeList = claims.getAny();
        String dialect = claims.getDialect();
        Map<QName, String> otherAttrs = claims.getOtherAttributes();

        debug.message("claimTypeList : " + claimTypeList);
        debug.message("dialect : " + dialect);
        debug.message("otherAttrs : " + otherAttrs);

        String[] claimTypes = new String[claimTypeList.size()];
        for (int i = 0; i < claimTypeList.size(); i++) {
            org.w3c.dom.Element claimType = (org.w3c.dom.Element) claimTypeList.get(i);
            claimTypes[i] = claimType.getAttribute("Uri");
            String optional = claimType.getAttribute("Optional");
            debug.message("claimTypes[" + i + "] : " + claimTypes[i]);
            debug.message("optional : " + optional);
            if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_GIVENNAME))) {
                QName gnQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_GIVENNAME);
                List<String> gnAttrs = new ArrayList<String>();
                gnAttrs.add(givenname);
                attrs.put(gnQName, gnAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_SURNAME))) {
                QName snQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_SURNAME);
                List<String> snAttrs = new ArrayList<String>();
                snAttrs.add(sn);
                attrs.put(snQName, snAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_EMAILADDRESS))) {
                QName emailQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_EMAILADDRESS);
                List<String> emailAttrs = new ArrayList<String>();
                emailAttrs.add(mail);
                attrs.put(emailQName, emailAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_STREETADDRESS))) {
                QName streetaddressQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_STREETADDRESS);
                List<String> streetadressAttrs = new ArrayList<String>();
                streetadressAttrs.add(streetaddress);
                attrs.put(streetaddressQName, streetadressAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_CITY))) {
                QName cityQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_CITY);
                List<String> cityAttrs = new ArrayList<String>();
                cityAttrs.add(city);
                attrs.put(cityQName, cityAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_STATE))) {
                QName stateQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_STATE);
                List<String> stateAttrs = new ArrayList<String>();
                stateAttrs.add(state);
                attrs.put(stateQName, stateAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_POSTALCODE))) {
                QName postalcodeQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_POSTALCODE);
                List<String> postalcodeAttrs = new ArrayList<String>();
                postalcodeAttrs.add(postalcode);
                attrs.put(postalcodeQName, postalcodeAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_COUNRTY))) {
                QName countryQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_COUNTRY);
                List<String> countryAttrs = new ArrayList<String>();
                countryAttrs.add(country);
                attrs.put(countryQName, countryAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_HOMEPHONE))) {
                QName homephoneQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_HOMEPHONE);
                List<String> homephoneAttrs = new ArrayList<String>();
                homephoneAttrs.add(homephone);
                attrs.put(homephoneQName, homephoneAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_WORKPHONE))) {
                QName workphoneQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_WORKPHONE);
                List<String> workphoneAttrs = new ArrayList<String>();
                workphoneAttrs.add(workphone);
                attrs.put(workphoneQName, workphoneAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_MOBILEPHONE))) {
                QName mobilephoneQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_MOBILEPHONE);
                List<String> mobilephoneAttrs = new ArrayList<String>();
                mobilephoneAttrs.add(mobilephone);
                attrs.put(mobilephoneQName, mobilephoneAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_GENDER))) {
                QName genderQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_GENDER);
                List<String> genderAttrs = new ArrayList<String>();
                genderAttrs.add(gender);
                attrs.put(genderQName, genderAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_DOB))) {
                QName dobQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_DOB);
                List<String> dobAttrs = new ArrayList<String>();
                dobAttrs.add(dob);
                attrs.put(dobQName, dobAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_WEBPAGE))) {
                QName webpageQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_WEBPAGE);
                List<String> webpageAttrs = new ArrayList<String>();
                webpageAttrs.add(webpage);
                attrs.put(webpageQName, webpageAttrs);
            } else if (claimTypes[i] != null && (claimTypes[i].equals(CardSpaceConstants.IC_NS_PRIVATEPERSONALIDENTIFIER))) {
                QName ppidQName = new QName(CardSpaceConstants.IC_NAMESPACE, CardSpaceConstants.IC_PRIVATEPERSONALIDENTIFIER);
                List<String> ppidAttrs = new ArrayList<String>();

                String cardId = null;
                String clientPseudonym = null; 
                String rpCert = null; 
                
                List<Object> extInfo = claims.getSupportingProperties();
                for (int index = 0; index < extInfo.size(); index++) {
                    if (extInfo.get(index) instanceof Element) {
                        Element ele = (Element) extInfo.get(index);
                        if ("InformationCardReference".equals(ele.getLocalName())) {
                            NodeList cardIds = ele.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2005/05/identity", "CardId");
                            if (cardIds.getLength() > 0) {
                                cardId = cardIds.item(0).getFirstChild().getNodeValue();

                            }

                            //
                            // get value for CardVersion similarly
                            //
                            
                        } else if ("ClientPseudonym".equals(ele.getLocalName())) {
                            //
                            //  get value for PPID
                            // 
                            NodeList nlPpid = ele.getElementsByTagNameNS(CardSpaceConstants.INFOCARD_NAMESPACE, "PPID"); 
                            if (nlPpid.getLength() >0) {
                                clientPseudonym = nlPpid.item(0).getFirstChild().getNodeValue(); 
                            }
                            
                        } else if ("Identity".equals(ele.getLocalName())) {
                            NodeList certs = ele.getElementsByTagNameNS("*", "X509Certificate");
                            if (certs.getLength() > 0) {
                                // Base64 encoding of the RP certificate
                                rpCert = certs.item(0).getFirstChild().getNodeValue();
                            }
                        }
                    }
                }

                if (clientPseudonym != null) {
                    ppidAttrs.add(clientPseudonym);
                } else {
                    try {
                    
                    CertificateFactory cf = CertificateFactory.getInstance("X509"); 
                    rpCert = "-----BEGIN CERTIFICATE-----\n" + rpCert + "\n-----END CERTIFICATE-----"; 
                    
                    InputStream is = new ByteArrayInputStream(rpCert.getBytes()); 
                    Collection certChain = cf.generateCertificates(is);
                    X509Certificate[] chain= new X509Certificate[certChain.size()]; 
                    
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(is); 
                    int j = 0; 
                    
                    while (certChain.iterator().hasNext()) {
                        chain[j] = (X509Certificate) certChain.iterator().next(); 
                    }
                    
                    String ppid = PPIDHelper.generateRPPPID(cardId, cert, chain); 
                    
                    } catch (CertificateException ex) {
                        throw new RuntimeException("Problem with the RP cert", ex); 
                    } catch (TokenIssuanceException ex) {
                        throw new RuntimeException("Problem with calculating the PPID", ex); 
                    }
                    
                    
                }
                attrs.put(ppidQName, ppidAttrs);
            }
        }

        debug.message("All attrs : " + attrs);

        return attrs;
    }

    private String getUserPseduoName(String userName) {

        if ("jsmith".equals(userName)) {
            return "jsmith";
        }

        if ("jondoe".equals(userName)) {
            return "jondoe";
        }

        return userName;
    }

    private String getUserRole(String userName) {
        if ("jsmith".equals(userName)) {
            return "gold ";
        }

        if ("jondoe".equals(userName)) {
            return "silver";
        }

        return "wsc";
    }

    private class SubjectSecurity {

        SSOToken ssoToken = null;
    }
}
