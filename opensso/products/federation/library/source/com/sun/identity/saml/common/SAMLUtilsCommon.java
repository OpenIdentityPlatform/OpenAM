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
 * $Id: SAMLUtilsCommon.java,v 1.4 2008/11/10 22:57:00 veiming Exp $
 *
 */

package com.sun.identity.saml.common;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.xmlsig.PasswordDecoder;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.security.SecureRandom; 
import org.w3c.dom.*;

/**
 * This class contains a set of generic common utility methods.
 *
 */
public class SAMLUtilsCommon {
    private static String PASSWORD_DECODER = 
        "com.sun.identity.saml.xmlsig.passwordDecoder";
    private static String FM_PASSWORD_DECODER =      
        "com.sun.identity.saml.xmlsig.FMPasswordDecoder";
    
    /**
     * <code>SecureRandom</code> instance.
     */
    public static SecureRandom random = new SecureRandom();

    /**
     * Prefix for ids used in SAML service.
     */
    public static final String SAMLID_PREFIX = "s";

    /**
     * A handle for <code>SAMLConstants</code>.
     */
    public static SAMLConstants sc;

    /**
     * SAML resource bundle object.
     */
    public static ResourceBundle bundle =
                    Locale.getInstallResourceBundle("libSAML");

    /**
     * SAML debug object.
     */
    public static Debug debug = Debug.getInstance("libSAML");

    /**
     * Sets the <code>ResourceBundle</code> of the service.
     * @param resBundle <code>ResourceBundle</code> instance to be set.
     */
    public static void setResourceBundle(ResourceBundle resBundle) {
        bundle = resBundle;
    }

    /**
     * Sets the <code>Debug</code> of the service.
     * @param dbg <code>Debug</code> instance to be set.
     */
    public static void setDebugInstance(Debug dbg) {
        debug = dbg;
    }

    /**
     * Generates an ID String with length of SAMLConstants.ID_LENGTH.
     * @return string the ID String; or null if it fails.
     */
    public static String generateAssertionID() {
        return  generateID();
    }

    /**
     * Generates an ID String with length of SAMLConstants.ID_LENGTH.
     * @return string the ID String; or null if it fails.
     */
    public static String generateID() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAMLConstants.ID_LENGTH];
        random.nextBytes(bytes);
        String encodedID = SAMLID_PREFIX + byteArrayToHexString(bytes);
        if (SAMLUtilsCommon.debug.messageEnabled()) {
            SAMLUtilsCommon.debug.message(
                    "SAMLUtils.generated ID is: " + encodedID);
        }

        return encodedID;
    }

    /**
     * Converts a byte array to a hex string.
     */
    public static String byteArrayToHexString(byte[] byteArray) {
        int readBytes = byteArray.length;
        StringBuffer hexData = new StringBuffer();
        int onebyte;
        for (int i=0; i < readBytes; i++) {
          onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
          hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }

    /**
     * Generates end element tag.
     * It takes in the name of element and produces a String 
     * as output which is in XML format. For example given
     * "SubjectConfirmation", It produces output like
     * &lt;/saml:SubjectConfirmation&gt;
     * if includeNS is  true  else produces &lt;/SubjectConfirmation&gt;
     * @param elementName name of an element
     * @param includeNS true to include namespace prefix; false otherwise.
     * @return String which is an xml element end tag.
     */
    public static String makeEndElementTagXML(String elementName, 
                        boolean includeNS) 
    {

        StringBuffer xml = new StringBuffer(100);
        String appendNS="";
        if (includeNS)  {
            appendNS="saml:";
        }
        xml.append(sc.START_END_ELEMENT).append(appendNS).append(elementName).
            append(sc.RIGHT_ANGLE).append(sc.NL);
        return xml.toString();
    }

    /**
     * Generates xml element start tag.
     * This utility method takes in the name fo element and produces a
     * String as output which is in XML format. For example given
     * "SubjectConfirmation". It produces output like
     * &lt;saml:SubjectConfirmation xmlns:saml=
     * "http://www.oasis-open.org/committees/security/docs/
     * draft-sstc-schema-assertion-16.xsd"&gt; where nameSpace is defined in 
     * <code>AssertionBase</code> class if declareNS and includeNS are true.
     * @param elementName name of the element.
     * @param includeNS true to include namespace prefix; false otherwise.
     * @param declareNS true to include namespace declaration; false otherwise.
     * @return xml element start tag.
     */
    public static String makeStartElementTagXML(String elementName, 
        boolean includeNS, boolean declareNS) 
    {
        StringBuffer xml = new StringBuffer(1000);
        String appendNS="";
        String NS="";
        if (includeNS) {
            appendNS="saml:";
        }
        if (declareNS)  {
            NS = sc.assertionDeclareStr;
        }
        xml.append(sc.LEFT_ANGLE).append(appendNS).append(elementName).
            append(NS).append(sc.RIGHT_ANGLE);
        return xml.toString();
    }

    /**
     * Verifies if an element is a type of a specific statement.
     * Currently, this method is used by class AuthenticationStatement,
     * AuthorizationDecisionStatement and AttributeStatement.
     * @param element a DOM Element which needs to be verified.
     * @param statementname A specific name of a statement, for example,
     *          AuthenticationStatement, AuthorizationDecisionStatement or
     *          AttributeStatement
     * @return true if the element is of the specified type; false otherwise.
     */
    public static boolean checkStatement(Element element, String statementname){
        String tag = element.getLocalName();
        if (tag == null) {
            return false;
        } else if (tag.equals("Statement") || tag.equals("SubjectStatement")) {
            NamedNodeMap nm = element.getAttributes();
            int len = nm.getLength();
            String attrName = null;
            Attr attr = null;
            for (int j = 0; j < len; j++) {
                attr = (Attr) nm.item(j);
                attrName = attr.getLocalName();
                if ((attrName != null) && (attrName.equals("type")) &&
                (attr.getNodeValue().equals(statementname + "Type"))) {
                    return true;
                }
            }
        } else if (tag.equals(statementname)) {
            return true;
        }
        return false;
    }
    
    /**
     * Decodes a password. 
     * The value passed is the value to be decoded using the decoder class
     * defined in FederationConfig.properties. The decoded value
     * will be returned unless the decoder class is not defined, or cannot
     * be located. In that case, the original value will be returned.
     *
     * @param password original password.
     * @return decoded password.
     */
    public static String decodePassword(String password)  {
        String decodePwdSpi = SystemConfigurationUtil.getProperty(
            PASSWORD_DECODER, FM_PASSWORD_DECODER);   
        String decoPasswd;
        try { 
            PasswordDecoder pwdDecoder = (PasswordDecoder) 
                Class.forName(decodePwdSpi).newInstance(); 
            decoPasswd = pwdDecoder.getDecodedPassword(password);
        } catch (Throwable t) {
            decoPasswd = password;
        }                   
        return decoPasswd;                     
    }
    
    /**
     * Removes new line charactors.
     * @param s A String to be checked.
     * @return a String with new line charactor removed.
     */
    public static String removeNewLineChars(String s) {
        String retString = null;
        if ((s != null) && (s.length() > 0) && (s.indexOf('\n') != -1)) {
            char[] chars = s.toCharArray();
            int len = chars.length;
            StringBuffer sb = new StringBuffer(len);
            for (int i = 0; i < len; i++) {
                char c = chars[i];
                if (c != '\n') {
                    sb.append(c);
                }
            }
            retString = sb.toString();
        } else {
            retString = s;
        }
        return retString;
    }

    /**
     * Decodes the Base64 encoded <code>sourceid</code> and returns
     * a String of the raw-byte source id.
     *
     * @param encodedID A String representing the Base64 encoded source id.
     * @return A String representing the raw byte source id.
     *
     */
    public static String getDecodedSourceIDString(String encodedID) {
        String result = null;
        if (encodedID == null) {
            SAMLUtils.debug.error("SAMLUtils.getDecodedSourceIDString: null "
            + "input.");
            return null;
        }
        
        try {
            result = byteArrayToString(Base64.decode(encodedID));
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLUtils.getDecodedSourceIDString: ", e);
            return null;
        }
        
        return result;
    }


    /**
     * Converts byte array to string.
     * @param bytes byte array to be converted.
     * @return result string.
     */
    public static String byteArrayToString(byte[] bytes) {
        char chars[] = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }
        return new String(chars);
    }
}
