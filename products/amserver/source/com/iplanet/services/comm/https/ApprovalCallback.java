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
 * $Id: ApprovalCallback.java,v 1.2 2008/06/25 05:41:34 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.https;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.mozilla.jss.ssl.SSLCertificateApprovalCallback.ValidityStatus;
import org.mozilla.jss.ssl.SSLCertificateApprovalCallback;
import org.mozilla.jss.crypto.X509Certificate;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertInfo;
import sun.security.x509.X509CertImpl;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.GeneralNames;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is implementation of <code>SSLCertificateApprovalCallback</code>
 * that is able to decide whether or not client approve the peer's cert,
 * instead of having NSS do that.
 */
public class ApprovalCallback implements SSLCertificateApprovalCallback {
    private String reqHost = null;

    static private ApprovalCallback theInstance = null;

    public static boolean trustAllServerCerts = false;

    public static boolean checkSubjectAltName = false;

    public static boolean resolveIPAddress = false;

    public static HashSet sslTrustHosts = new HashSet();

    private static Class[] argTypes = new Class [0];
    private static Object[] params = new Object [0];
    
    private static final String NEW_METHOD_NAME = "iterator";
    private static final String OLD_METHOD_NAME = "elements";
    private static Method method = null;
    private static Debug debug = Debug.getInstance("amJSS");
    
    static {        
        String tmp = 
            SystemProperties.get("com.iplanet.am.jssproxy.trustAllServerCerts");
        trustAllServerCerts = (tmp != null && tmp.equalsIgnoreCase("true"));

        tmp = 
            SystemProperties.get("com.iplanet.am.jssproxy.checkSubjectAltName");
        checkSubjectAltName = (tmp != null && tmp.equalsIgnoreCase("true"));

        tmp = SystemProperties.get("com.iplanet.am.jssproxy.resolveIPAddress");
        resolveIPAddress = (tmp != null && tmp.equalsIgnoreCase("true"));

        tmp = SystemProperties.get(
              "com.iplanet.am.jssproxy.SSLTrustHostList", null);
        if (tmp != null) {
            getSSLTrustHosts(tmp);
        }

        if (debug.messageEnabled()) {
            debug.message("ApprovalCallback trustAllServerCerts = " +
                                   trustAllServerCerts);
            debug.message("ApprovalCallback checkSubjectAltName = " +
                                   checkSubjectAltName);
            debug.message("ApprovalCallback resolveIPAddress = " +
                                   resolveIPAddress);
            debug.message("ApprovalCallback  SSLTrustHostList = " +
                                   sslTrustHosts.toString());
        }
    }

    private static Method getMethod() throws NoSuchMethodException {
            if (method == null) {
                String methodName = NEW_METHOD_NAME;
            Method [] methods = GeneralNames.class.getDeclaredMethods();
            for (int m = 0; m < methods.length; ++m) {
                      if (methods[m].getName().equals(OLD_METHOD_NAME)) {
                        methodName = OLD_METHOD_NAME;
                    break;
                }
            }
            method = GeneralNames.class.getMethod(methodName, argTypes);
            }
            return method;
    }

    /**
     * Creates an instance of <code>ApprovalCallback</code>.
     */
    private ApprovalCallback() {
    }

    /**
     * Creates an instance of <code>ApprovalCallback</code>.
     *
     * @param host Name of the host.
     */
    public ApprovalCallback(String host)
    {
        if (host != null) {
            reqHost = host.toLowerCase();
        }
    }

   /*
    * Generates a ApprovalCallback object.
    *
    * @return the new ApprovalCallback object.
    */    
    static public ApprovalCallback getInstance() {
        if (theInstance == null) {
            theInstance = new ApprovalCallback();
        }

        return theInstance;
    }

   /*
    * Invoked by JSS protocol handler whenever ssl handshaking hits issue.
    * It validates reported issue if it can be ignored.
    *
    * @return <code>true</code> if the reported issue can be ignored.
    */    
    public boolean approve(
        X509Certificate cert,
        SSLCertificateApprovalCallback.ValidityStatus status
    )
    {  
        ValidityItem item;
        Enumeration errors = status.getReasons();
        int reason;

        if (trustAllServerCerts) {
            return true;
        }

        if ((reqHost == null) && !errors.hasMoreElements()) { 
            return true;
        }

        boolean approve = true;

        while (approve && errors.hasMoreElements()) {
            item = (SSLCertificateApprovalCallback.ValidityItem) 
                                                         errors.nextElement();
            reason = item.getReason();
            if (debug.messageEnabled()) {
                debug.message("ApprovalCallback: reason " +
                                       reason);
            }

            // bad domain -12276
            if (reason != ValidityStatus.BAD_CERT_DOMAIN) {
                approve = false;
            }
            else {
                String cn = null;
                
                try {
                    String subjectDN = cert.getSubjectDN().getName();
                    cn = new X500Name(subjectDN).getCommonName();
                }
                catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("ApprovalCallback:",
                                               ex);
                    }
                    approve = false;
                }

                if (cn == null) { 
                    return false;
                }

                if (!sslTrustHosts.isEmpty()) {
                    if (debug.messageEnabled()) {
                        debug.message(
                            "ApprovalCallback: server cert CN : " + cn);
                    }
 
                    if (sslTrustHosts.contains(cn.toLowerCase())) {
                        return true;
                    }
                }

                if (resolveIPAddress) {
                    try {
                        approve = 
                            InetAddress.getByName(cn).getHostAddress().equals(
                              InetAddress.getByName(reqHost).getHostAddress());
                    }
                    catch (UnknownHostException ex) {
                        if (debug.messageEnabled()) {
                            debug.message("ApprovalCallback:",
                                                   ex);
                        }
                        approve = false;
                    }
                } else
                    approve = false;

                if (!approve && checkSubjectAltName) {
                    try {
                        X509CertImpl certImpl = 
                                new X509CertImpl(cert.getEncoded());
                        X509CertInfo cinfo = 
                                new X509CertInfo(certImpl.getTBSCertificate());
                        CertificateExtensions exts = (CertificateExtensions) 
                                            cinfo.get(X509CertInfo.EXTENSIONS);
                        SubjectAlternativeNameExtension altNameExt = 
                            (SubjectAlternativeNameExtension)
                            exts.get(SubjectAlternativeNameExtension.NAME);

                        if (altNameExt != null) {
                            GeneralNames names = (GeneralNames) altNameExt.get
                                (SubjectAlternativeNameExtension.SUBJECT_NAME);
                        
                            Method meth = getMethod();
                            GeneralName generalname = null;  
                            if (meth.getName().equals(OLD_METHOD_NAME)) {
                                // pre 1.4.2 implementation
                                Enumeration e = 
                                    (Enumeration) meth.invoke(names, params);
                                for (; !approve && e.hasMoreElements();) {
                                    approve = 
                                    compareHosts((GeneralName) e.nextElement());
                                }
                            } else {
                                // post 1.4.2 implementation
                                Iterator i = 
                                    (Iterator) meth.invoke(names, params);
                                for (; !approve && i.hasNext();) {
                                    approve = 
                                    compareHosts((GeneralName) i.next());
                                }
                                }
                        }
                    }
                    catch (Exception ex) {
                        return false;
                    }
                }
            }
        }
        return approve;
    }

    private boolean compareHosts(GeneralName generalName) {
        try {
            if (generalName.getType() == GeneralNameInterface.NAME_DNS) {
                String name = generalName.toString();
                    name = name.substring(name.indexOf(':')+1).trim();
                return InetAddress.getByName(name).equals(
                   InetAddress.getByName(reqHost));
            }
        } catch (UnknownHostException e) {
            if (debug.messageEnabled()) {
                debug.message(e.toString());
            }
        }
        return false;
    }

    static private void getSSLTrustHosts(String hostlist) {
        if (debug.messageEnabled()) {
            debug.message("ApprovalCallback  SSLTrustHostList = " +
                                   hostlist);
        }
        StringTokenizer st = new StringTokenizer(hostlist, ",");
        sslTrustHosts.clear();
        while (st.hasMoreTokens()) {
            sslTrustHosts.add(((String)st.nextToken()).trim().toLowerCase());
        }
    }
}

