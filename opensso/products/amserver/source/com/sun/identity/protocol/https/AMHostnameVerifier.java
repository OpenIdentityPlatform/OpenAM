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
 * $Id: AMHostnameVerifier.java,v 1.2 2008/06/25 05:43:54 qcheng Exp $
 *
 */

package com.sun.identity.protocol.https;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.X500Name;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;


public class AMHostnameVerifier implements HostnameVerifier {

    public static boolean trustAllServerCerts = false;

    public static boolean checkSubjectAltName = false;

    public static boolean resolveIPAddress = false;

    public static HashSet sslTrustHosts = new HashSet();

    static private Debug debug = Debug.getInstance("amJSSE");
    
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
            debug.message("AMHostnameVerifier trustAllServerCerts = " +
                                   trustAllServerCerts);
            debug.message("AMHostnameVerifier checkSubjectAltName = " +
                                   checkSubjectAltName);
            debug.message("AMHostnameVerifier  resolveIPAddress = " +
                                   resolveIPAddress);
            debug.message("AMHostnameVerifier  SSLTrustHostList = " +
                                   sslTrustHosts.toString());
        }
    }

    public boolean verify(String hostname, SSLSession session) {
    	if (trustAllServerCerts) {
            return true;
        }

        boolean approve = true;
    	X509Certificate peercert =  null;
	String cn = null;

        try {
            X509Certificate[] peercerts = 
                   (X509Certificate[]) session.getPeerCertificates();
	    peercert =  peercerts[0];
            String subjectDN = peercert.getSubjectDN().getName();
            cn = new X500Name(subjectDN).getCommonName();
        } catch (Exception ex) {
            debug.error("AMHostnameVerifier:"+ex.toString());
        }

        if (cn == null) 
            return false;

        if (!sslTrustHosts.isEmpty()) {
            if (sslTrustHosts.contains(cn.toLowerCase())) {
                return true;
            }
        }

        if (resolveIPAddress) {
            try {
                approve = InetAddress.getByName(cn).getHostAddress().equals(
                             InetAddress.getByName(hostname).getHostAddress());
            }
            catch (UnknownHostException ex) {
                if (debug.messageEnabled()) {
                    debug.message("AMHostnameVerifier:", ex);
                }
                approve = false;
            }
        } else {
            approve = false;
        }
        
        if (checkSubjectAltName && !approve) {
            try {
                Iterator i = 
                    (Iterator) peercert.getSubjectAlternativeNames().iterator();
                for (; !approve && i.hasNext();) {
                    approve = compareHosts((GeneralName) i.next(), hostname);
                }
            } catch (Exception ex) {
                return false;
            }
        }
        
        return approve;
    }

    private boolean compareHosts(GeneralName generalName, String hostname) {
        try {
            if (generalName.getType() == GeneralNameInterface.NAME_DNS) {
                String name = generalName.toString();
    	        name = name.substring(name.indexOf(':')+1).trim();
                return InetAddress.getByName(name).equals(
                        InetAddress.getByName(hostname));
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
            debug.message("AMHostnameVerifier  SSLTrustHostList = " +
                                   hostlist);
        }
        StringTokenizer st = new StringTokenizer(hostlist, ",");
        sslTrustHosts.clear();
        while (st.hasMoreTokens()) {
            sslTrustHosts.add(((String)st.nextToken()).trim().toLowerCase());
        }
    }
}
        
