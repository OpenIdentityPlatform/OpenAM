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
 * $Id: SAMLUtils.java,v 1.16 2010/01/09 19:41:06 qcheng Exp $
 *
 * Portions Copyrighted 2012-2016 ForgeRock AS.
 */

package com.sun.identity.saml.common;

import static org.forgerock.openam.utils.Time.*;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.text.StringCharacterIterator;
import java.text.CharacterIterator;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.security.MessageDigest;

import java.net.URL;
import java.net.MalformedURLException;

import org.w3c.dom.*;

import com.sun.identity.common.PeriodicGroupRunnable;
import com.sun.identity.common.ScheduleableGroupAction;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.encode.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import javax.xml.soap.MimeHeaders;
import javax.xml.soap.MimeHeader;

import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.Condition;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.SubjectStatement;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.plugins.PartnerAccountMapper;
import com.sun.identity.saml.protocol.*;
import com.sun.identity.saml.servlet.POSTCleanUpRunnable;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.SAMLClient;
import com.sun.identity.federation.common.FSUtils;

import javax.xml.parsers.DocumentBuilder;

import org.apache.xml.security.c14n.Canonicalizer;

/**
 * This class contains some utility methods for processing SAML protocols.
 *
 * 
 */
public class SAMLUtils  extends SAMLUtilsCommon {

    /**
     * Attribute that specifies maximum content length for SAML request in
     * <code>AMConfig.properties</code> file.
     */
    public static final String HTTP_MAX_CONTENT_LENGTH =
        "com.sun.identity.saml.request.maxContentLength";

    /**
     * Default maximum content length is set to 16k.
     */
    public static final int defaultMaxLength = 16384;

    /**
     * Default maximum content length in string format.
     */
    public static final String DEFAULT_CONTENT_LENGTH =
    String.valueOf(defaultMaxLength);

    private static final String ERROR_JSP = "/saml2/jsp/autosubmittingerror.jsp";
    
    private static int maxContentLength = 0;
    private static Map idTimeMap = Collections.synchronizedMap(new HashMap());
    private static TaskRunnable cGoThrough = null;
    private static TaskRunnable cPeriodic = null;
    private static Object ssoToken;
 
    static {
        org.apache.xml.security.Init.init();
        if (SystemConfigurationUtil.isServerMode()) {
            long period = ((Integer) SAMLServiceManager.getAttribute(
                        SAMLConstants.CLEANUP_INTERVAL_NAME)).intValue() * 1000;
            cGoThrough = new POSTCleanUpRunnable(period, idTimeMap);
            TimerPool timerPool = SystemTimerPool.getTimerPool();
            timerPool.schedule(cGoThrough, new Date(((currentTimeMillis()
                    + period) / 1000) * 1000));
            ScheduleableGroupAction periodicAction = new
                ScheduleableGroupAction() {
                public void doGroupAction(Object obj) {
                    idTimeMap.remove(obj);
                }
            };
            cPeriodic = new PeriodicGroupRunnable(periodicAction, period,
                180000, true);
            timerPool.schedule(cPeriodic, new Date(((currentTimeMillis() +
                    period) / 1000) * 1000));
        }
        try {
            maxContentLength = Integer.parseInt(SystemConfigurationUtil.
                getProperty(SAMLUtils.HTTP_MAX_CONTENT_LENGTH,
                SAMLUtils.DEFAULT_CONTENT_LENGTH));
        } catch (NumberFormatException ne) {
            SAMLUtils.debug.error("Wrong format of SAML request max content "
                + "length. Take default value.");
            maxContentLength=  SAMLUtils.defaultMaxLength;
        }
    }
    
    /**
     * Constructor
     * iPlanet-PRIVATE-DEFAULT-CONSTRUCTOR
     */
    private SAMLUtils() {
    }
    
    
    /**
     * Generates an ID String with length of SAMLConstants.ID_LENGTH.
     * @return string the ID String; or null if it fails.
     */
    public static String generateAssertionID() {
        String encodedID = generateID();
        if (encodedID == null) {
            return null;
        }
        
        String id = null;
        try {
            id = SystemConfigurationUtil.getServerID(
            SAMLServiceManager.getServerProtocol(),
            SAMLServiceManager.getServerHost(),
            Integer.parseInt(SAMLServiceManager.getServerPort()),
            SAMLServiceManager.getServerURI());
        } catch (Exception ex) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtil:generateAssertionID: "
                + "exception obtain serverID:", ex);
            }
        }
        if (id == null) {
            return encodedID;
        } else {
            return (encodedID + id);
        }
    }
    
    /**
     * Verifies if an element is a type of a specific query.
     * Currently, this method is used by class AuthenticationQuery,
     * AuthorizationDecisionQuery, and AttributeQuery.
     * @param element a DOM Element which needs to be verified.
     * @param queryname A specific name of a query, for example,
     *          AuthenticationQuery, AuthorizationDecisionQuery, or
     *                AttributeQuery.
     * @return true if the element is a type of the specified query; false
     *                otherwise.
     */
    public static boolean checkQuery(Element element, String queryname) {
        String tag = element.getLocalName();
        if (tag == null) {
            return false;
        } else if (tag.equals("Query") || tag.equals("SubjectQuery")) {
            NamedNodeMap nm = element.getAttributes();
            int len = nm.getLength();
            String attrName;
            Attr attr;
            boolean found = false;
            for (int j = 0; j < len; j++) {
                attr = (Attr) nm.item(j);
                attrName = attr.getLocalName();
                if ((attrName != null) && (attrName.equals("type")) &&
                (attr.getNodeValue().equals(queryname + "Type"))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        } else if (!tag.equals(queryname)) {
            return false;
        }
        return true;
    }
    
    /**
     * Generates sourceID of a site.
     * @param siteURL a String that uniquely identifies a site.
     * @return <code>Base64</code> encoded SHA digest of siteURL.
     */
    public static String generateSourceID(String siteURL) {
        if ((siteURL == null) || (siteURL.length() == 0)) {
            SAMLUtils.debug.error("SAMLUtils.genrateSourceID: empty siteURL.");
            return null;
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLUtils.generateSourceID: Exception when"
            + " generating digest:",e);
            return null;
        }
        md.update(SAMLUtils.stringToByteArray(siteURL));
        byte byteResult[] = md.digest();
        String result = null;
        try {
            result = Base64.encode(byteResult).trim();
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLUtils.generateSourceID: Exception:",e);
        }
        return result;
    }

    /**
     * Generates assertion handle.
     * @return 20-byte random string to be used to form an artifact.
     */
    public static String generateAssertionHandle() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAMLConstants.ID_LENGTH];
        random.nextBytes(bytes);
        String id = null;
        try {
            id = SystemConfigurationUtil.getServerID(
                SAMLServiceManager.getServerProtocol(),
                SAMLServiceManager.getServerHost(),
                Integer.parseInt(SAMLServiceManager.getServerPort()),
                SAMLServiceManager.getServerURI());
        } catch (Exception ex) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtil:generateAssertionHandle: "
                + "exception obtain serverID:", ex);
            }
        }
        if (id != null) {
            byte idBytes[] = stringToByteArray(id);
            // TODO: should we check if idBytes.length == 2 ?
            if (idBytes.length < bytes.length) {
                for (int i = 1; i <= idBytes.length; i++) {
                    bytes[bytes.length - i] = idBytes[idBytes.length - i];
                }
            }
        }
        return byteArrayToString(bytes);
    }

    /**
     * Converts a HEX encoded string to a byte array.
     * @param hexString HEX encoded string
     * @return byte array.
     */
    public static byte[] hexStringToByteArray(String hexString) {
        int read = hexString.length();
        byte[] byteArray = new byte[read/2];
        for (int i=0, j=0; i < read; i++, j++) {
            String part = hexString.substring(i,i+2);
            byteArray[j] =
            new Short(Integer.toString(Integer.parseInt(part,16))).
            byteValue();
            i++;
        }
        return byteArray;
    }

    /**
     * Converts HEX encoded string to Base64 encoded string.
     * @param hexString HEX encoded string.
     * @return Base64 encoded string.
     */
    public static String hexStringToBase64(String hexString) {
        int read = hexString.length();
        byte[] byteArray = new byte[read/2];
        for (int i=0, j=0; i < read; i++, j++) {
            String part = hexString.substring(i,i+2);
            byteArray[j] =
            new Short(Integer.toString(Integer.parseInt(part,16))).
            byteValue();
            i++;
        }
        String encodedID = null;
        try {
            encodedID = Base64.encode(byteArray).trim();
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtil:hexStringToBase64: "
                + "exception encode input:", e);
            }
        }
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("base 64 source id is :"+encodedID);
        }
        return encodedID;
    }
    
    /**
     * Gets sourceSite corresponding to an issuer from the partner URL list.
     * @param issuer The issuer string.
     * @return SAMLServiceManager.SOAPEntry of the issuer if it's on the list;
     *                        null otherwise.
     */
    public static SAMLServiceManager.SOAPEntry getSourceSite(String issuer) {
        if (issuer == null) {
            return null;
        }
        Map entries = (Map) SAMLServiceManager.getAttribute(
        SAMLConstants.PARTNER_URLS);
        if (entries == null) {
            SAMLUtils.debug.error("SAMLUtils.isOnPartnerURLList: PartnerURL "
            + "list is null.");
            return null;
        }
        
        Iterator entryIter = entries.values().iterator();
        boolean found = false;
        SAMLServiceManager.SOAPEntry srcSite = null;
        String theIssuer = null;
        while (entryIter.hasNext()) {
            srcSite = (SAMLServiceManager.SOAPEntry) entryIter.next();
            if ((srcSite != null) &&
            ((theIssuer = srcSite.getIssuer()) != null) &&
            (theIssuer.equals(issuer))) {
                found = true;
                break;
            }
        }
        if (found) {
            return srcSite;
        } else {
            return null;
        }
    }
    
    /**
     * Returns site ID based on the host name. The site ID
     * will be in Base64 encoded format. This method will print out site ID
     * to the standard output
     * @param args host name
     */
    public static void main(String args[]) {
        
        if (args.length != 1) {
            System.out.println("usage : java SAMLUtils <host_name>");
            return;
        }
        
        System.out.println(generateSourceID(args[0]));
    }
    
    /**
     * Checks if a <code>SubjectConfirmation</code> is correct.
     * @param sc <code>SubjectConfirmation</code> instance to be checked.
     * @return true if the <code>SubjectConfirmation</code> instance passed in
     * has only one <code>ConfirmationMethod</code>, and this
     * <code>ConfirmationMethod</code> is set to
     * <code>SAMLConstants.CONFIRMATION_METHOD_IS</code>.
     */
    public static boolean isCorrectConfirmationMethod(SubjectConfirmation sc) {
        if (sc == null) {
            return false;
        }
        
        Set cmSet = sc.getConfirmationMethod();
        if ((cmSet == null) || (cmSet.size() != 1)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtils.isCorrectConfirmationMethod:"
                + " missing ConfirmationMethod in the Subject.");
            }
            return false;
        }
        
        String conMethod = (String) cmSet.iterator().next();
        if ((conMethod == null) ||
        (!conMethod.equals(SAMLConstants.CONFIRMATION_METHOD_IS))) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtils.isCorrectConfirmationMethod:"
                + " wrong ConfirmationMethod value.");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns true if the assertion is valid both time wise and
     * signature wise, and contains at least one AuthenticationStatement.
     * @param assertion <code>Assertion</code> instance to be checked.
     * @return <code>true</code> if the assertion is valid both time wise and
     * signature wise, and contains at least one AuthenticationStatement.
     */
    public static boolean isAuthNAssertion(Assertion assertion) {
        if (assertion == null) {
            return false;
        }
        
        if ((!assertion.isTimeValid()) || (!assertion.isSignatureValid())) {
            return false;
        }
        
        Set statements = assertion.getStatement();
        Statement statement = null;
        Iterator iterator = statements.iterator();
        while (iterator.hasNext()) {
            statement = (Statement) iterator.next();
            if (statement.getStatementType() ==
            Statement.AUTHENTICATION_STATEMENT) {
                return true;
            }
        } // loop through statements
        return false;
    }
 
    /**
     * Converts a string to a byte array.
     * @param input a String to be converted.
     * @return result byte array.
     */
    public static byte[] stringToByteArray(String input) {
        char chars[] = input.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }
 

    /**
     * Returns server ID.
     * @param idTypeString An ID string
     * @return server ID part of the id.
     */
    public static String getServerID(String idTypeString) {
        if (idTypeString == null) {
            return null;
        }
        int len = idTypeString.length();
        String id = null;
        if (len >= SAMLConstants.SERVER_ID_LENGTH) {
            id = idTypeString.substring((len - SAMLConstants.SERVER_ID_LENGTH),
            len);
            return id;
        } else {
            return null;
        }
    }
    
    /**
     * Returns server url of a site.
     * @param str Server ID.
     * @return Server url corresponding to the server id.
     */
    public static String getServerURL(String str) {
        String id = SAMLUtils.getServerID(str);
        if (id == null) {
            return null;
        }
        
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("SAMLUtils.getServerURL: id=" + id);
        }
        
        String remoteUrl = null;
        try {
            remoteUrl = SystemConfigurationUtil.getServerFromID(id);
        } catch (SystemConfigurationException se) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtils.getServerURL: ServerEntry" +
                "NotFoundException for " + id);
            }
            return null;
        }
        String thisUrl = SAMLServiceManager.getServerURL();
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("SAMLUtils.getServerURL: remoteUrl=" +
            remoteUrl + ", thisUrl=" + thisUrl);
        }
        if ((remoteUrl == null) || (thisUrl == null) ||
        (remoteUrl.equalsIgnoreCase(thisUrl))) {
            return null;
        } else {
            return remoteUrl;
        }
    }
    
    /**
     * Returns full service url.
     * @param shortUrl short URL of the service.
     * @return full service url.
     */
    public static String getFullServiceURL(String shortUrl) {
        String result = null;
        try {
            URL u = new URL(shortUrl);
            URL weburl = SystemConfigurationUtil.getServiceURL(
                SAMLConstants.SAML_AM_NAMING, u.getProtocol(), u.getHost(),
                    u.getPort(), u.getPath());
            result = weburl.toString();
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtils.getFullServiceURL:" +
                "full remote URL is: " + result);
            }
        } catch (Exception e) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("SAMLUtils.getFullServiceURL:" +
                "Exception:", e);
            }
        }
        return result;
    }

    /**
     * Returns attributes included in <code>AttributeStatement</code> of the
     * assertion.
     * @param envParameters return map which includes name value pairs of 
     *   attributes included in <code>AttributeStatement</code> of the assertion
     * @param assertion an <code>Assertion</code> object which contains
     *   <code>AttributeStatement</code>
     * @param subject the <code>Subject</code> instance from
     *   <code>AuthenticationStatement</code>. The <code>Subject</code>
     *   included in <code>AttributeStatement</code> must match this
     *   <code>Subject</code> instance.
     */
    public static void addEnvParamsFromAssertion(Map envParameters,
    Assertion assertion,
    com.sun.identity.saml.assertion.Subject subject) {
        Set statements = assertion.getStatement();
        Statement statement = null;
        Iterator stmtIter = null;
        List attrs = null;
        Iterator attrIter = null;
        Attribute attribute = null;
        Element attrValue = null;
        List attrValues = null;
        String attrName = null;
        String attrValueString = null;
        if ((statements != null) && (!statements.isEmpty())) {
            stmtIter = statements.iterator();
            while (stmtIter.hasNext()) {
                statement = (Statement) stmtIter.next();
                if (statement.getStatementType() ==
                Statement.ATTRIBUTE_STATEMENT) {
                    // check for subject
                    if (!subject.equals(
                    ((AttributeStatement)statement).getSubject())) {
                        continue;
                    }
                    
                    attrs = ((AttributeStatement) statement).getAttribute();
                    attrIter = attrs.iterator();
                    while (attrIter.hasNext()) {
                        attribute = (Attribute) attrIter.next();
                        try {
                            attrValues = attribute.getAttributeValue();
                        } catch (Exception e) {
                            debug.error("SAMLUtils.addEnvParamsFromAssertion:"+
                            " cannot obtain attribute value:", e);
                            continue;
                        }
                        attrName = attribute.getAttributeName();
                        List attrValueList = null;

                        for(Iterator avIter = attrValues.iterator();
                            avIter.hasNext(); ) {

                            attrValue = (Element) avIter.next();
                            if (!XMLUtils.hasElementChild(attrValue)) {
                                attrValueString =
                                    XMLUtils.getElementValue(attrValue);
                                if (attrValueList == null) {
                                    attrValueList = new ArrayList();
                                }
                                attrValueList.add(attrValueString);
                            }
                        }
                        if (attrValueList != null) {
                            if (debug.messageEnabled()) {
                                debug.message(
                                    "SAMLUtils.addEnvParamsFromAssertion:" +
                                    " attrName = " + attrName +
                                    " attrValue = " + attrValueList);
                            }
                            String[] attrValueStrs = (String[])attrValueList.
                                toArray(new String[attrValueList.size()]);
                            try {
                                envParameters.put(attrName, attrValueStrs);
                            } catch (Exception ex) {
                                if (debug.messageEnabled()) {
                                    debug.message(
                                        "SAMLUtils.addEnvParamsFromAssertion:",
                                        ex);
                                }
                            }
                        } else if (debug.messageEnabled()) {
                            if (debug.messageEnabled()) {
                                debug.message(
                                    "SAMLUtils.addEnvParamsFromAssertion:" +
                                    " attrName = " + attrName +
                                    " has no value");
                            }
                        }
                    }
                } // if it's an attribute statement
            }
        }
    }

    /**
     * Returns maximum content length of a SAML request.
     * @return maximum content length of a SAML request.
     */
    public static int getMaxContentLength() {
        return maxContentLength;
    }
    
    // ************************************************************************
    // Methods used by SAML Servlets
    // ************************************************************************
 
    /**
     * Checks content length of a http request to avoid dos attack.
     * In case SAML inter-op with other SAML vendor who may not provide content
     * length in HttpServletRequest. We decide to support no length restriction
     * for Http communication. Here, we use a special value (e.g. 0) to
     * indicate that no enforcement is required.
     * @param request <code>HttpServletRequest</code> instance to be checked.
     * @exception ServletException if context length of the request exceeds
     *   maximum content length allowed.
     */
    public static void checkHTTPContentLength(HttpServletRequest request)
    throws ServletException {
        if (maxContentLength != 0) {
            int length =  request.getContentLength();
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("HttpRequest content length= " +length);
            }
            if (length > maxContentLength) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message(
                        "content length too large" + length);
                }
                throw new ServletException(
                SAMLUtils.bundle.getString("largeContentLength"));
            }
        }
    }
    
    /**
     * Post assertions and attributes to the target url.
     * This method opens a URL connection to the target specified and POSTs
     * assertions to it using the passed HttpServletResponse object. It POSTs
     * multiple parameter names "assertion" with value being each of the 
     * <code>Assertion</code> in the passed Set.
     * @param response <code>HttpServletResponse</code> object
     * @param out The print writer which for content is to be written too.
     * @param assertion List of <code>Assertion</code>s to be posted.
     * @param targeturl target url
     * @param attrMap Map of attributes to be posted to the target
     */
    public static void postToTarget(HttpServletResponse response, PrintWriter out,
                                    List assertion, String targeturl, Map attrMap) throws IOException {
        out.println("<HTML>");
        out.println("<HEAD>\n");
        out.println("<TITLE>Access rights validated</TITLE>\n");
        out.println("</HEAD>\n");
        out.println("<BODY Onload=\"document.forms[0].submit()\">");
        Iterator it = null;
        if (SAMLUtils.debug.messageEnabled()) {
            out.println("<H1>Access rights validated</H1>\n");
            out.println("<meta http-equiv=\"refresh\" content=\"20\">\n");
            out.println("<P>We have verified your access rights <STRONG>" +
            "</STRONG> according to the assertion shown "
            +"below. \n");
            out.println("You are being redirected to the resource.\n");
            out.println("Please wait ......\n");
            out.println("</P>\n");
            out.println("<HR><P>\n");
            if (assertion != null) {
                it = assertion.iterator();
                while (it.hasNext()) {
                    out.println(SAMLUtils.displayXML((String)it.next()));
                }
            }
            out.println("</P>\n");
        }
        out.println("<FORM METHOD=\"POST\" ACTION=\"" + targeturl + "\">");
        if (assertion != null) {
            it = assertion.iterator();
            while (it.hasNext()) {
                out.println("<INPUT TYPE=\"HIDDEN\" NAME=\""+
                SAMLConstants.POST_ASSERTION_NAME + "\"");
                out.println("VALUE=\"" +
                    URLEncDec.encode((String)it.next()) + "\">");
            }
        }
        if (attrMap != null && !attrMap.isEmpty()) {
            StringBuffer attrNamesSB = new StringBuffer();
            Set entrySet = attrMap.entrySet();
            for(Iterator iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String attrName = HTMLEncode((String)entry.getKey(), '\"');
                String attrValue = HTMLEncode((String)entry.getValue(), '\"');
                out.println("<INPUT TYPE=\"HIDDEN\" NAME=\""+ attrName +
                "\" VALUE=\"" + attrValue + "\">");
                if (attrNamesSB.length() > 0) {
                    attrNamesSB.append(":");
                }
                attrNamesSB.append(attrName);
            }
            out.println("<INPUT TYPE=\"HIDDEN\" NAME=\""+
            SAMLConstants.POST_ATTR_NAMES + "\" VALUE=\"" +
            attrNamesSB + "\">");
        }
        out.println("</FORM>");
        out.println("</BODY></HTML>");
        out.close();
    }
    
    /**
     * Returns true of false based on whether the target passed as parameter
     * accepts form POST.
     * @param targetIn url to be checked
     * @return true if it should post assertion to the target passed in; false
     *   otherwise.
     */
    public static boolean postYN(String targetIn) {
        SAMLUtils.debug.message("Inside postYN()");
        if ((targetIn == null) || (targetIn.length() == 0)) {
            return false;
        }
        Set targets = (Set) SAMLServiceManager.
        getAttribute(SAMLConstants.POST_TO_TARGET_URLS);
        if ((targets == null) || (targets.size() == 0)) {
            return false;
        }
        URL targetUrl = null;
        try {
            targetUrl = new URL(targetIn);
        } catch (MalformedURLException me ) {
            SAMLUtils.debug.error("SAMLUtils:postYN(): Malformed URL passed");
            return false;
        }
        String targetInHost = targetUrl.getHost();
        int targetInPort = targetUrl.getPort();
        String targetInPath = targetUrl.getPath();
        // making target string without protocol
        String targetToCompare = new StringBuffer(targetInHost.toLowerCase())
        .append(":").append(String.valueOf(targetInPort))
        .append("/").append(targetInPath).toString();
        if (targets.contains(targetToCompare)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Replaces every occurence of ch with 
     * "&#&lt;ascii code of ch>;"
     * @param srcStr orginal string to to be encoded.
     * @param ch the charactor needs to be encoded.
     * @return encoded string
     */
    public static String HTMLEncode(String srcStr, char ch) {
        if (srcStr == null) {
            return null;
        }
        
        int fromIndex = 0;
        int toIndex;
        StringBuffer dstSB = new StringBuffer();
        
        while((toIndex = srcStr.indexOf(ch, fromIndex)) != -1) {
            dstSB.append(srcStr.substring(fromIndex, toIndex))
            .append("&#" + (int)ch + ";");
            fromIndex = toIndex + 1;
        }
        dstSB.append(srcStr.substring(fromIndex));
        
        return dstSB.toString();
    }

    /**
     * Displays an XML string.
     * This is a utility function used to hack up an HTML display of an XML
     * string.
     * @param input original string
     * @return encoded string so it can be displayed properly by html.
     */
    public static String displayXML(String input) {
        debug.message("In displayXML ");
        StringCharacterIterator iter = new StringCharacterIterator(input);
        StringBuffer buf = new StringBuffer();
        
        for(char c = iter.first();c != CharacterIterator.DONE;c = iter.next()) {
            if (c=='>') {
                buf.append("&gt;");
            } else if (c=='<') {
                buf.append("&lt;");
            } else if (c=='\n'){
                buf.append("<BR>\n");
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }
    
    /**
     * Gets the list of <code>Assertion</code> objects from a list of
     * 'String' assertions.
     * @param assertions List of assertions in string format
     * @return List of <code>Assertion</code> objects
     */
    public static List getListOfAssertions(List assertions) {
        List returnAssertions = new ArrayList();
        try {
            if (assertions != null) {
                Iterator it = assertions.iterator();
                while (it.hasNext()) {
                    Document doc = XMLUtils.toDOMDocument((String)it.next(),
                                                            debug);
                    Element root = doc.getDocumentElement();
                    if (root != null) {
                        Assertion assertion = new Assertion(root);
                        returnAssertions.add(assertion);
                    }
                }
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("SAMLUtils.getListOfAssertions : " +
                "Exception : ", e);
            }
        }
        return returnAssertions;
    }
    
    
    // ************************************************************************
    // Methods used / shared by SAML Authentication Module and SAML Servlets
    // ************************************************************************

    /**
     * Returns byte array from a SAML <code>Response</code>.
     * @param samlResponse <code>Response</code> object
     * @return byte array
     * @exception SAMLException if error occurrs during the process.
     */
    public static byte[] getResponseBytes(Response samlResponse)
        throws SAMLException
    {
        byte ret[] = null;
        try {
            ret = samlResponse.toString(true, true, true).
            getBytes(SAMLConstants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ue) {
            if (debug.messageEnabled()) {
                debug.message("getResponseBytes : " , ue);
            }
            throw new SAMLException(ue.getMessage());
        }
        return ret;
    }
 
    /**
     * Returns <code>Response</code> object from byte array.
     * @param bytes byte array
     * @return <code>Response</code> object
     */
    public static Response getResponse(byte [] bytes) {
        Response temp = null;
        if (bytes == null) {
            return null;
        }
        try {
            temp = Response.parseXML(new ByteArrayInputStream(bytes));
        } catch (SAMLException se) {
            debug.error("getResponse : " , se);
        }
        return temp;
    }

    /**
     * Verifies a <code>Response</code>.
     * @param response SAML <code>Response</code> object
     * @param requestUrl this server's POST profile URL
     * @param request <code>HttpServletRequest</code> object
     * @return true if the response is valid; false otherwise.
     */
    public static boolean verifyResponse(Response response,
    String requestUrl, HttpServletRequest request) {
        if(!response.isSigned()) {
            debug.message("verifyResponse: Response is not signed");
            return false;
        }
        if (!response.isSignatureValid()) {
            debug.message("verifyResponse: Response's signature is invalid.");
            return false;
        }

        // check Recipient == this server's POST profile URL(requestURL)
        String recipient = response.getRecipient();
        if ((recipient == null) || (recipient.length() == 0) ||
        ((!equalURL(recipient, requestUrl)) &&
        (!equalURL(recipient,getLBURL(requestUrl, request))))) {
            debug.error("verifyResponse : Incorrect Recipient.");
            return false;
        }
        
        // check status of the Response
        if (!response.getStatus().getStatusCode().getValue().endsWith(
        SAMLConstants.STATUS_CODE_SUCCESS_NO_PREFIX)) {
            debug.error("verifyResponse : Incorrect StatusCode value.");
            return false;
        }
        
        return true;
    }
    
    private static String getLBURL(String requestUrl,
                                 HttpServletRequest request)
    {
        String host = request.getHeader("host");
        if (host == null) {
            return requestUrl;
        }
        int index = requestUrl.indexOf("//");
        if (index == -1) {
            return requestUrl;
        }
        StringBuffer sb = new StringBuffer(200);
        sb.append(requestUrl.substring(0, index + 2)).append(host);
        String rest = requestUrl.substring(index +2, requestUrl.length());
        if ((index = rest.indexOf("/")) != -1) {
            sb.append(rest.substring(index, rest.length()));
        }
        if (debug.messageEnabled()) {
            debug.message("getLBURL: LBURL = " + sb.toString());
        }
        return sb.toString().trim();
    }
    
    // ************************************************************************
    // Methods used by SAML Authentication Module
    // ************************************************************************
    
    /**
     * Gets List of assertions in String format from a list of 
     * <code>Assertion</code> objects.
     * @param assertions List of <code>Assertion</code> objects.
     * @return List of assertions in String format
     */
    public static List getStrAssertions(List assertions) {
        List returnAssertions = new ArrayList();
        if (assertions != null) {
            Iterator it = assertions.iterator();
            while (it.hasNext()) {
                returnAssertions.add(
                    ((Assertion)(it.next())).toString(true,true));
            }
        }
        return returnAssertions;
    }
    
    /**
     * Verifies Signature for Post response.
     * @param samlResponse <code>Response</code> object from post profile.
     * @return true if the signature on the reponse is valid; false otherwise.
     */
    public static boolean verifySignature(Response samlResponse) {
        if ((samlResponse != null) &&
        (!samlResponse.isSigned() || (!samlResponse.isSignatureValid()))) {
            return false;
        }
        return true;
    }
    
    /**
     * Gets Attribute Map to be set in the Session.
     * @param partnerdest <code>SOAPEntry</code> object
     * @param assertions List of <code>Assertion</code>s
     * @param subject <code>Subject</code> object
     * @param target target of final SSO
     * @return Map which contains name and attributes.
     * @exception Exception if an error occurrs.
     */
    public static Map getAttributeMap(
    SAMLServiceManager.SOAPEntry partnerdest,
    List assertions,
    com.sun.identity.saml.assertion.Subject subject,
    String target)
    throws Exception {
        String srcID = partnerdest.getSourceID();
        String name = null;
        String org = null;
        Map attrMap = new HashMap();
        PartnerAccountMapper paMapper = partnerdest.getPartnerAccountMapper();
        
        if (paMapper != null) {
            Map map = paMapper.getUser(assertions, srcID, target);
            name = (String) map.get(PartnerAccountMapper.NAME);
            org =  (String) map.get(PartnerAccountMapper.ORG);
            attrMap = (Map) map.get(PartnerAccountMapper.ATTRIBUTE);
        }
        
        if (attrMap == null) {
            attrMap = new HashMap();
        }
        attrMap.put(SAMLConstants.USER_NAME, name);
        if ((org != null) && (org.length() != 0)) {
            attrMap.put(SessionProvider.REALM, org);
        } else {
            attrMap.put(SessionProvider.REALM, "/");
        }
        
        if (debug.messageEnabled()) {
            debug.message("getAttributeMap : " + "name = " +
            name + ", realm=" + org + ", attrMap = " + attrMap);
        }
        
        return attrMap;
    }
    
    /**
     * Checks response and get back a Map of relevant data including,
     * Subject, SOAPEntry for the partner and the List of Assertions.
     * @param response <code>Response</code> object
     * @return Map of data including Subject, SOAPEntry, and list of assertions.
     */
    public static Map verifyAssertionAndGetSSMap(Response response) {
        // loop to check assertions
        com.sun.identity.saml.assertion.Subject subject = null;
        SAMLServiceManager.SOAPEntry srcSite = null;
        List assertions = response.getAssertion();
        Iterator iter = assertions.iterator();
        Assertion assertion = null;
        String aIDString = null;
        String issuer = null;
        Iterator stmtIter = null;
        Statement statement = null;
        int stmtType = Statement.NOT_SUPPORTED;
        com.sun.identity.saml.assertion.Subject sub = null;
        SubjectConfirmation subConf = null;
        Set confMethods = null;
        String confMethod = null;
        Date date = null;
        while (iter.hasNext()) {
            assertion = (Assertion) iter.next();
            aIDString = assertion.getAssertionID();
            // make sure it's not being used
            if (idTimeMap.containsKey(aIDString)) {
                debug.error("verifyAssertion "
                + "AndGetSSMap: Assertion: " + aIDString + " is used.");
                return null;
            }
            
            // check issuer of the assertions
            issuer = assertion.getIssuer();
            if ((srcSite = SAMLUtils.getSourceSite(issuer)) == null) {
                debug.error("verifyAsserti "
                + "onAndGetSSMap: issuer is not on the Partner list.");
                return null;
            }
            
            if (!assertion.isSignatureValid()) {
                debug.error("verifyAssertion "
                + "AndGetSSMap: assertion's signature is not valid.");
                return null;
            }
            
            // must be valid (timewise)
            if (!assertion.isTimeValid()) {
                debug.error("verifyAssertion "
                + "AndGetSSMap: assertion's time is not valid.");
                return null;
            }
            
            // TODO: IssuerInstant of the assertion is within a few minutes
            // This is a MAY in spec. Which number to use for the few minutes?
            
            // TODO: check AudienceRestrictionCondition
            
            //for each assertion, loop to check each statement
            stmtIter = assertion.getStatement().iterator();
            while (stmtIter.hasNext()) {
                statement = (Statement) stmtIter.next();
                stmtType = statement.getStatementType();
                if ((stmtType == Statement.AUTHENTICATION_STATEMENT) ||
                (stmtType == Statement.ATTRIBUTE_STATEMENT) ||
                (stmtType == Statement.AUTHORIZATION_DECISION_STATEMENT)) {
                    sub = ((SubjectStatement)statement).getSubject();
                    
                    // ConfirmationMethod of each subject must be set to bearer
                    if (((subConf = sub.getSubjectConfirmation()) == null) ||
                    ((confMethods = subConf.getConfirmationMethod())
                    == null) ||
                    (confMethods.size() != 1)) {
                        debug.error("verify "
                        + "AssertionAndGetSSMap: missing or extra "
                        + "ConfirmationMethod.");
                        return null;
                    }
                    if (((confMethod = (String) confMethods.iterator().next())
                    == null) ||
                    (!confMethod.equals(
                    SAMLConstants.CONFIRMATION_METHOD_BEARER))) {
                        debug.error("verify "
                        + "AssertionAndGetSSMap:wrong ConfirmationMethod.");
                        return null;
                    }
                    
                    //TODO: must contain same Subject for all statements?
                    
                    if (stmtType == Statement.AUTHENTICATION_STATEMENT) {
                        //TODO: if it has SubjectLocality,its IP must == sender
                        // browser IP. This is a MAY item in the spec.
                        if (subject == null) {
                            subject = sub;
                        }
                    }
                }
            }
            
            // add the assertion to idTimeMap
            if (debug.messageEnabled()) {
                debug.message("Adding " + aIDString + " to idTimeMap.");
            }
            Conditions conds = assertion.getConditions();
            if ((conds != null) && ((date = conds.getNotOnorAfter()) != null)) {
                cGoThrough.addElement(aIDString);
                idTimeMap.put(aIDString, new Long(date.getTime()));
            } else {
                cPeriodic.addElement(aIDString);
                // it doesn't matter what we store for the value.
                idTimeMap.put(aIDString, aIDString);
            }
        }
        
        // must have at least one SSO assertion
        if ((subject == null) || (srcSite == null)) {
            debug.error("verifyAssertion AndGetSSMap: couldn't find Subject.");
            return null;
        }
        Map ssMap = new HashMap();
        ssMap.put(SAMLConstants.SUBJECT, subject);
        ssMap.put(SAMLConstants.SOURCE_SITE_SOAP_ENTRY, srcSite);
        ssMap.put(SAMLConstants.POST_ASSERTION, assertions);
        return ssMap;
    }
    
    /**
     * Checks if the Assertion is time valid and
     * if the Assertion is allowed by AudienceRestrictionCondition.
     *
     * @param assertion an Assertion object
     * @return true if the operation is successful otherwise, return false
     * @exception IOException IOException
     */
    private static boolean checkCondition(Assertion assertion)
        throws IOException
    {
        if (assertion == null) {
            return false;
        }
        if (!assertion.isSignatureValid()) {
            debug.error(bundle.getString("assertionSignatureNotValid"));
            return false;
        }
        // check if the Assertion is time valid
        if (!(assertion.isTimeValid())) {
            debug.error(bundle.getString("assertionTimeNotValid"));
            return false;
        }
        // check the Assertion is allowed by AudienceRestrictionCondition
        Conditions cnds = assertion.getConditions();
        Set audienceCnd = new HashSet();
        audienceCnd = cnds.getAudienceRestrictionCondition();
        Iterator it = null;
        if (audienceCnd != null) {
            if (!audienceCnd.isEmpty()) {
                it = audienceCnd.iterator();
                while (it.hasNext()) {
                    if ((((AudienceRestrictionCondition) it.next()).
                    evaluate()) == Condition.INDETERMINATE ) {
                        if (debug.messageEnabled()) {
                            debug.message("Audience " +
                            "RestrictionConditions is indeterminate.");
                        }
                    } else {
                        debug.error("Failed AudienceRestrictionCondition");
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Determines if there is a valid SSO Assertion
     * inside of SAML Response.
     *
     * @param assertions a List of <code>Assertion</code> objects
     * @return a Subject object
     * @exception IOException IOException
     */
    public static com.sun.identity.saml.assertion.Subject examAssertions(
    List assertions) throws IOException {
        if (assertions == null) {
            return null;
        }
        boolean validation = false;
        com.sun.identity.saml.assertion.Subject subject = null;
        Iterator iter = assertions.iterator();
        
        while (iter.hasNext()) {
            Assertion assertion = (Assertion)iter.next();
            
            if (!checkCondition(assertion)) {
                return null;
            }
            debug.message("Passed checking Conditions!");
            
            // exam the Statement inside the Assertion
            Set statements = new HashSet();
            statements = assertion.getStatement();
            
            if (statements == null || statements.isEmpty()) {
                debug.error(bundle.getString("noStatement"));
                return null;
            }
            Iterator iterator = statements.iterator();
            while (iterator.hasNext()) {
                Statement statement = (Statement) iterator.next();
                subject = ((SubjectStatement)statement).getSubject();
                SubjectConfirmation sc = subject.getSubjectConfirmation();
                Set cm = new HashSet();
                cm =  sc.getConfirmationMethod();
                if (cm == null || cm.isEmpty()) {
                    debug.error("Subject confirmation method is null");
                    return null;
                }
                String conMethod = (String) cm.iterator().next();
                // add checking artifact confirmation method identifier based
                // on Assertion version number
                if ((conMethod != null) && (assertion.getMajorVersion() ==
                SAMLConstants.ASSERTION_MAJOR_VERSION) &&
                (((assertion.getMinorVersion() ==
                SAMLConstants.ASSERTION_MINOR_VERSION_ONE) &&
                conMethod.equals(SAMLConstants.CONFIRMATION_METHOD_ARTIFACT))
                ||
                ((assertion.getMinorVersion() ==
                SAMLConstants.ASSERTION_MINOR_VERSION_ZERO) &&
                (conMethod.equals(
                SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT))))) {
                    if (debug.messageEnabled()) {
                        debug.message("Correct Confirmation method");
                    }
                } else {
                    debug.error("Wrong Confirmation Method.");
                    return null;
                }
                if (statement instanceof AuthenticationStatement) {
                    //found an SSO Assertion
                    validation = true;
                }
            }  // end of  while (iterator.hasNext()) for Statements
        } // end of while (iter.hasNext()) for Assertions
        
        if (!validation) {
            debug.error(bundle.getString("noSSOAssertion"));
            return null;
        }
        return subject;
    }

    /**
     * Return whether the signature on the object is valid or not.
     * @param xmlString input XML String 
     * @param idAttribute ASSERTION_ID_ATTRIBUTE or RESPONSE_ID_ATTRIBUTE
     * @param issuer the issuer of the Assertion 
     * @return true if the signature on the object is valid; false otherwise.
     */
    public static boolean checkSignatureValid(String xmlString, 
                                                  String idAttribute, 
                                                  String issuer)
    {
            String certAlias = null;
            boolean valid = true; 
            Map entries = (Map) SAMLServiceManager.getAttribute(
                                SAMLConstants.PARTNER_URLS);
        if (entries != null) {
            SAMLServiceManager.SOAPEntry srcSite =
                (SAMLServiceManager.SOAPEntry) entries.get(issuer);
            if (srcSite != null) {
                certAlias = srcSite.getCertAlias();
            }
        }
      
        try {
            SAMLUtils.debug.message("SAMLUtils.checkSignatureValid for certAlias {}", certAlias);
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            valid = manager.verifyXMLSignature(xmlString, 
                                   idAttribute, certAlias);
        } catch (Exception e) {
            SAMLUtils.debug.warning("SAMLUtils.checkSignatureValid:"+
                                " signature validation exception", e);
            valid = false;
        }
        if (!valid) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtils.checkSignatureValid:"+
                                        " Couldn't verify signature.");
            }
        }
        return valid;
    }

    /**
     * Sets the given <code>HttpServletResponse</code> object with the
     * headers in the given <code>MimeHeaders</code> object.
     * @param headers the <code>MimeHeaders</code> object
     * @param response the <code>HttpServletResponse</code> object to which the
     *        headers are to be written.
     */
    public static void setMimeHeaders(
         MimeHeaders headers, HttpServletResponse response) {
        
         if(headers == null || response == null) {
            debug.message("SAMLUtils.setMimeHeaders : null input");
            return;
         }

         for (Iterator iter = headers.getAllHeaders(); iter.hasNext();){
              MimeHeader header = (MimeHeader)iter.next();

              String[] values = headers.getHeader(header.getName());
              if (values.length == 1) {
                  response.setHeader(header.getName(), header.getValue());
              } else {
                  StringBuffer concat = new StringBuffer();
                  int i = 0;
                  while (i < values.length) {
                      if (i != 0) {
                          concat.append(',');
                      }
                      concat.append(values[i++]);
                   }
                   response.setHeader(header.getName(),concat.toString());
              }

         }
         return; 
    }

    /**
     * Returns a <code>MimeHeaders</code> object that contains the headers
     * in the given <code>HttpServletRequest</code> object.
     *
     * @param req the <code>HttpServletRequest</code> object.
     * @return a new <code>MimeHeaders</code> object containing the headers.
     */
    public static MimeHeaders getMimeHeaders(HttpServletRequest req) {

         MimeHeaders headers = new MimeHeaders();

         if(req == null) {
            debug.message("SAMLUtils.getMimeHeaders: null input");
            return headers;
         }

         Enumeration enumerator = req.getHeaderNames();

         while(enumerator.hasMoreElements()) {
             String headerName = (String)enumerator.nextElement();
             String headerValue = req.getHeader(headerName);

             StringTokenizer values = new StringTokenizer(headerValue, ",");
             while(values.hasMoreTokens()) {
                 headers.addHeader(headerName, values.nextToken().trim());
             }
         }

         return headers; 
    }
     
    /**
     * Returns the authenticaion login url with goto parameter
     * in the given <code>HttpServletRequest</code> object.
     *
     * @param req the <code>HttpServletRequest</code> object.
     * @return a new authenticaion login url with goto parameter.
     */
    public static String getLoginRedirectURL(HttpServletRequest req) {
        String qs = req.getQueryString();
        String gotoUrl = req.getRequestURL().toString();
        String key = null; 
        if (qs != null && qs.length() > 0) {
            gotoUrl = gotoUrl + "?" + qs;
            int startIdx = -1;
            int endIdx = -1; 
            StringBuffer result = new StringBuffer(); 
            if ((startIdx = qs.indexOf((String) SAMLServiceManager.
                getAttribute(SAMLConstants.TARGET_SPECIFIER))) > 0) {
                result.append(qs.substring(0, startIdx - 1)); 
            }    
            if ((endIdx = qs.indexOf("&", startIdx)) != -1) {
                if (startIdx == 0) {
                    result.append(qs.substring(endIdx + 1)); 
                } else {
                    result.append(qs.substring(endIdx));
                }    
            } 
            key = result.toString();
        }

        String reqUrl = req.getScheme() + "://" + req.getServerName() + ":" +
            req.getServerPort() + req.getContextPath();
        String redirectUrl = null;
        if (key == null || key.equals("")) {
            redirectUrl = reqUrl +"/UI/Login?goto=" +  
                URLEncDec.encode(gotoUrl);
        } else {
            redirectUrl = reqUrl +"/UI/Login?" + key + "&goto="+ 
                URLEncDec.encode(gotoUrl);
        }
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("Redirect to auth login via:" +
                redirectUrl);
        }    
        return redirectUrl; 
    }    
    
      
    /** 
     * Processes SAML Artifact
     * @param artifact SAML Artifact
     * @param target Target URL 
     * @return Attribute Map
     * @exception SAMLException if failed to get the Assertions or
     *     Attribute Map.
     */
    public static Map processArtifact(String[] artifact, String target) 
        throws SAMLException {
        List assts = null;  
        Subject assertionSubject = null; 
        AssertionArtifact firstArtifact = null;  
        Map sessMap = null; 
        // Call SAMLClient to do the Single-sign-on
        try {
            assts = SAMLClient.artifactQueryHandler(artifact, (String) null);
            //exam the SAML response
            if ((assertionSubject = examAssertions(assts)) == null) {
                return null; 
            }
            firstArtifact = new AssertionArtifact(artifact[0]);
            String sid = firstArtifact.getSourceID();
            Map partner = (Map) SAMLServiceManager.getAttribute(
                SAMLConstants.PARTNER_URLS);
            if (partner == null) {
                throw new SAMLException(bundle.getString
                    ("nullPartnerUrl"));
            }
            SAMLServiceManager.SOAPEntry partnerdest = 
                (SAMLServiceManager.SOAPEntry) partner.get(sid);
            if (partnerdest == null) {
                throw new SAMLException(bundle.getString
                    ("failedAccountMapping"));
            }
            sessMap = getAttributeMap(partnerdest, assts,
                assertionSubject, target); 
        } catch (Exception se) {
            debug.error("SAMLUtils.processArtifact :" , se);
            throw new SAMLException(
                bundle.getString("failProcessArtifact"));
        }    
        return sessMap;   
    }
    
    /**
     * Creates Session 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param attrMap Attribute Map 
     * @exception if failed to create Session
     */
    public static Object generateSession(HttpServletRequest request,
        HttpServletResponse response, 
        Map attrMap) throws SAMLException {  
        Map sessionInfoMap = new HashMap();
        String realm = (String) attrMap.get(SessionProvider.REALM);
        if ((realm == null) || (realm.length() == 0)) {
            realm = "/";
        } 
        sessionInfoMap.put(SessionProvider.REALM, realm);
        String principalName = 
            (String) attrMap.get(SessionProvider.PRINCIPAL_NAME);
        if (principalName == null) {
            principalName = (String) attrMap.get(SAMLConstants.USER_NAME);
        }
        sessionInfoMap.put(SessionProvider.PRINCIPAL_NAME, principalName);
        //TODO: sessionInfoMap.put(SessionProvider.AUTH_LEVEL, "0");
        Object session = null;  
        try {  
            SessionProvider sessionProvider = SessionManager.getProvider();
            session = sessionProvider.createSession(
                sessionInfoMap, request, response, null);
            setAttrMapInSession(sessionProvider, attrMap, session);
        } catch (SessionException se) {
            if (debug.messageEnabled()) {
                debug.message("SAMLUtils.generateSession:", se);
            }
            throw new SAMLException(se);
        }
        return session;
    }
    
    /**
     * Processes SAML Response
     * @param samlResponse SAML Response object
     * @param target Target URL 
     * @return Attribute Map
     * @exception SAMLException if failed to get Attribute Map.
     */
    public static Map processResponse(Response samlResponse, String target) 
        throws SAMLException {
        List assertions = null;    
        SAMLServiceManager.SOAPEntry partnerdest = null;
        Subject assertionSubject = null;

        // verify the signature
        boolean isSignedandValid = verifySignature(samlResponse);
        if (!isSignedandValid) {
            throw new SAMLException(bundle.getString("invalidResponse"));
        }

        // check Assertion and get back a Map of relevant data including,
        // Subject, SOAPEntry for the partner and the List of Assertions.
        Map ssMap = verifyAssertionAndGetSSMap(samlResponse);
        if (debug.messageEnabled()) {
            debug.message("processResponse: ssMap = " + ssMap);
        }
        
        if (ssMap == null) {
            throw new SAMLException(bundle.getString("invalidAssertion"));
        }
        assertionSubject = (com.sun.identity.saml.assertion.Subject)
            ssMap.get(SAMLConstants.SUBJECT);
        if (assertionSubject == null) {
            throw new SAMLException(bundle.getString("nullSubject"));
        }
        
        partnerdest = (SAMLServiceManager.SOAPEntry)ssMap
            .get(SAMLConstants.SOURCE_SITE_SOAP_ENTRY);
        if (partnerdest == null) {
            throw new SAMLException(bundle.getString("failedAccountMapping"));
        }
        
        assertions = (List)ssMap.get(SAMLConstants.POST_ASSERTION);
        Map sessMap = null;
        try { 
            sessMap = getAttributeMap(partnerdest, assertions,
                assertionSubject, target); 
        } catch (Exception se) {
            debug.error("SAMLUtils.processResponse :" , se);
            throw new SAMLException(
                bundle.getString("failProcessResponse"));
        }
        return sessMap;
    }
    
    /**
     *Sets the attribute map in the session
     *
     *@param attrMap, the Attribute Map
     *@param session, the valid session object
     *@exception SessionException if failed to set Attribute in the 
     *    Session.  
     */
    private static void setAttrMapInSession(
        SessionProvider sessionProvider,
        Map attrMap, Object session)
        throws SessionException {
        if (attrMap != null && !attrMap.isEmpty()) {
            Set entrySet = attrMap.entrySet();
            for(Iterator iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String attrName = (String)entry.getKey();
                String[] attrValues = null;
                if (attrName.equals(SAMLConstants.USER_NAME) ||
                    attrName.equals(SessionProvider.PRINCIPAL_NAME)) {
                    String attrValue = (String)entry.getValue();
                    attrValues = new String[1];
                    attrValues[0] = attrValue;
                } else if (attrName.equals(SessionProvider.REALM) ||
                    attrName.equals(SessionProvider.AUTH_LEVEL)) {
                    // ignore
                    continue; 
                } else {
                    attrValues = (String[])entry.getValue();
                }
                sessionProvider.setProperty(session, attrName, attrValues);
                if (debug.messageEnabled()) {
                    debug.message("SAMLUtils.setAttrMapInSessioin: attrName ="+
                        attrName);
                }
            }
        } 
    }

    /**
     * Compares two URLs to see if they are equal. Two URLs are equal if
     * they have same protocol, host, port and path (case ignored).
     * Note : the method is provided to avoid URL.equals() call which requires
     * name lookup. Name lookup is a blocking operation and very expensive
     * if the hostname could not be resolved.
     *
     * @return true if the URLs are equal, false otherwise.
     */
    private static boolean equalURL(String url1, String url2) {
        try {
            URL u1 = new URL(url1);
            URL u2 = new URL(url2);
            int port1 = u1.getPort();
            if (port1 == -1) {
                port1 = u1.getDefaultPort();
            }
            int port2 = u2.getPort();
            if (port2 == -1) {
                port2 = u2.getDefaultPort();
            }
            if ((u1.getProtocol().equalsIgnoreCase(u2.getProtocol())) &&
                (u1.getHost().equalsIgnoreCase(u2.getHost())) &&
                (port1 == port2) &&
                (u1.getPath().equalsIgnoreCase(u2.getPath()))) {
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException m) {
            debug.message("Error in SAMLUtils.equalURL", m);
            return false;
        }
    }

      /**
       * Gets input Node Canonicalized
       *
       * @param node Node
       * @return Canonical element if the operation succeeded.
       *     Otherwise, return null.
       */
      public static Element getCanonicalElement(Node node) {
          try {
              Canonicalizer c14n = Canonicalizer.getInstance(
                  "http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
              byte outputBytes[] = c14n.canonicalizeSubtree(node);
              DocumentBuilder documentBuilder = 
                 XMLUtils.getSafeDocumentBuilder(false);
              Document doc = documentBuilder.parse(
                  new ByteArrayInputStream(outputBytes));
              Element result = doc.getDocumentElement();
              return result;
          } catch (Exception e) {
              SAMLUtils.debug.error("Response:getCanonicalElement: " +
                  "Error while performing canonicalization on " +
                  "the input Node.");
              return null;
          }
      }
      
     /**
      * Sends to error page URL for SAML protocols. If the error page is
      * hosted in the same web application, forward is used with
      * parameters. Otherwise, redirection or HTTP POST is used with
      * parameters.
      * Three parameters are passed to the error URL:
      *  -- errorcode : Error key, this is the I18n key of the error message.
      *  -- httpstatuscode : Http status code for the error
      *  -- message : detailed I18n'd error message
      * @param request HttpServletRequest object
      * @param response HttpServletResponse object
      * @param httpStatusCode Http Status code
      * @param errorCode Error code
      * @param errorMsg Detailed error message
      */
     public static void sendError(HttpServletRequest request,
         HttpServletResponse response, int httpStatusCode,
         String errorCode, String errorMsg) {
                 String errorUrl = SystemConfigurationUtil.getProperty(
               SAMLConstants.ERROR_PAGE_URL,
               SAMLConstants.DEFAULT_ERROR_PAGE_URL);
         if(debug.messageEnabled()) {
            debug.message("SAMLUtils.sendError: error page" + errorUrl);
         }
         String tmp = errorUrl.toLowerCase();
         if (!tmp.startsWith("http://") && !tmp.startsWith("https://")) {
             // use forward
             String jointString = "?";
             if (errorUrl.indexOf("?") != -1) {
                 jointString = "&";
             }
             String newUrl = errorUrl.trim() + jointString
                  + SAMLConstants.ERROR_CODE + "=" + errorCode + "&"
                  + SAMLConstants.HTTP_STATUS_CODE + "=" + httpStatusCode
                  + "&" + SAMLConstants.ERROR_MESSAGE + "="
                  + URLEncDec.encode(errorMsg);

             forwardRequest(newUrl, request, response);
         } else {
           String binding = SystemConfigurationUtil.getProperty(
                            SAMLConstants.ERROR_PAGE_HTTP_BINDING,
                            SAMLConstants.HTTP_POST);
           if(SAMLConstants.HTTP_REDIRECT.equals(binding)) {
               // use FSUtils, this may be redirection or forward
              String jointString = "?";
              if (errorUrl.indexOf("?") != -1) {
                  jointString = "&";
              }
              String newUrl = errorUrl.trim() + jointString
                   + SAMLConstants.ERROR_CODE + "=" + errorCode + "&"
                   + SAMLConstants.HTTP_STATUS_CODE + "=" + httpStatusCode
                   + "&" + SAMLConstants.ERROR_MESSAGE + "="
                   + URLEncDec.encode(errorMsg);

              FSUtils.forwardRequest(request, response, newUrl) ;
           } else {
               // Populate request attributes to be available for rendering.
               request.setAttribute("ERROR_URL", errorUrl);
               request.setAttribute("ERROR_CODE_NAME", SAMLConstants.ERROR_CODE);
               request.setAttribute("ERROR_CODE", errorCode);
               request.setAttribute("ERROR_MESSAGE_NAME", SAMLConstants.ERROR_MESSAGE);
               request.setAttribute("ERROR_MESSAGE", URLEncDec.encode(errorMsg));
               request.setAttribute("HTTP_STATUS_CODE_NAME", SAMLConstants.HTTP_STATUS_CODE);
               request.setAttribute("HTTP_STATUS_CODE", httpStatusCode);
               request.setAttribute("SAML_ERROR_KEY", bundle.getString("samlErrorKey"));
               // Forward to auto-submitting form.
               forwardRequest(ERROR_JSP, request, response);
           }
         }
     }

    /**
     * Forwards to the passed URL.
     *
     * @param url
     *         Forward URL
     * @param request
     *         Request object
     * @param response
     *         Response object
     */
    private static void forwardRequest(String url, HttpServletRequest request, HttpServletResponse response) {
        try {
            request.getRequestDispatcher(url).forward(request, response);

        } catch (ServletException sE) {
            handleForwardError(url, sE, response);
        } catch (IOException ioE) {
            handleForwardError(url, ioE, response);
        }
    }

    /**
     * Handle any forward error.
     *
     * @param url
     *         Attempted forward URL
     * @param exception
     *         Caught exception
     * @param response
     *         Response object
     */
    private static void handleForwardError(String url, Exception exception, HttpServletResponse response) {
        debug.error("SAMLUtils.sendError: Exception occurred while trying to forward to resource: " + url, exception);

        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exception.getMessage());
        } catch (IOException ioE) {
            debug.error("Failed to inform the response of caught exception", ioE);
        }
    }

}
