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
 * $Id: HttpsClient.java,v 1.3 2008/06/25 05:41:34 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.https;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Vector;

import org.mozilla.jss.CryptoManager;
import org.mozilla.jss.CryptoManager.NotInitializedException;
import org.mozilla.jss.crypto.X509Certificate;
import org.mozilla.jss.ssl.SSLClientCertificateSelectionCallback;
import org.mozilla.jss.ssl.SSLSocket;

import sun.misc.RegexpPool;
import sun.net.www.http.HttpClient;
import java.net.SocketException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * This class provides HTTPS client URL support, building on the standard
 * "sun.net.www" HTTP protocol handler.  HTTPS is the same protocol as HTTP,
 * but differs in the transport layer which it uses:  <UL>
 *
 *        <LI>There's a <em>Secure Sockets Layer</em> between TCP
 *        and the HTTP protocol code.
 *
 *        <LI>It uses a different default TCP port.
 *
 *        <LI>It doesn't use application level proxies, which can see and
 *        manipulate HTTP user level data, compromising privacy.  It uses
 *        low level tunneling instead, which hides HTTP protocol and data
 *        from all third parties.  (Traffic analysis is still possible).
 *
 *        <LI>It does basic server authentication, to protect
 *        against "URL spoofing" attacks.  This involves deciding
 *        whether the X.509 certificate chain identifying the server
 *        is trusted, and verifying that the name of the server is
 *        found in the certificate.  (The application may enable an
 *        anonymous SSL cipher suite, and such checks are not done
 *        for anonymous ciphers.)
 *
 *        <LI>It exposes key SSL session attributes, specifically the
 *        cipher suite in use and the server's X509 certificates, to
 *        application software which knows about this protocol handler.
 *
 *        </UL>
 *
 * <P> System properties used include:  <UL>
 *
 *        <LI><em>https.proxyHost</em> ... the host supporting SSL
 *        tunneling using the conventional CONNECT syntax
 *
 *        <LI><em>https.proxyPort</em> ... port to use on proxyHost
 *
 *        <LI><em>https.cipherSuites</em> ... comma separated list of
 *        SSL cipher suite names to enable.
 *
 *        <LI><em>http.nonProxyHosts</em> ... 
 *
 *        </UL>
 *
 * @version 1.24
 * @author David Brownell
 * @author Bill Foote
 */

// final for export control reasons (access to APIs); remove with care
public final class HttpsClient extends HttpClient
    implements SSLClientCertificateSelectionCallback
{
    // STATIC STATE and ACCESSORS THERETO
    private static Debug debug = Debug.getInstance("amJSS");

    // Host and port we use for proxying, or null if none are declared.
    private static String secureTunnelHost = null;
    private static int secureTunnelPort = 80;

    // regexp pool of hosts for which we should connect directly, not Tunnel
    // these are intialized from a property at class init time
    private static RegexpPool dontProxy = null;

    // HTTPS uses a different default port number than HTTP.
    private static final int        httpsPortNumber = 443;

    /** Returns the default HTTPS port (443) */
    protected int getDefaultPort () { return httpsPortNumber; }

    /* Set properties on startup */
    static {
        configureFIPSSSLoptions();
        resetSecureProperties();
    }


    // INSTANCE DATA

    // tunnel host/port for this instance.  instTunnelHost will be null
    // if tunneling is turned off for this client for any reason.
    private String        instTunnelHost;
    private int                instTunnelPort;

    private SSLSocket sslSocket = null;

    private String nickName = System.getProperty(
                     Constants.CLIENT_CERTIFICATE_ALIAS, null);

    /**
     * Re-initialize the dontProxy list, and other properties we depend on.
     */
    public static synchronized void resetSecureProperties() {
        // REMIND:  Don't change the name of resetSecureProperties -
        // sun.hotjava.applets.SecureHttpProxyApplet depends on it.

        String ourHost = getProperty("https.proxyHost");
        if (debug.messageEnabled()) {
            debug.message("HttpsClient: proxyHost = " + ourHost);
        }
        if (ourHost == null || "".equals(ourHost)) {
            secureTunnelHost = null;
            secureTunnelPort = 80;
        } else {
            secureTunnelHost = ourHost;
            secureTunnelPort = Integer
                .getInteger("https.proxyPort", 80)
                .intValue();
        }

        // REMIND:  The following is cut-and-paste from HttpClient.
        // The dontProxy stuff needs to be factored out into a protected
        // method that we can use, but we're after code freeze right now
        // (this has to work with both 1.1.1 and 1.1).

        dontProxy = new RegexpPool();
        String rawList = getProperty("http.nonProxyHosts");
        debug.message("HttpsClient: nonProxyHosts = " + rawList);

        if(rawList != null) {
            java.util.StringTokenizer st
                = new java.util.StringTokenizer(rawList, "|", false);
            try {
                while (st.hasMoreTokens()) {
                    dontProxy.add(st.nextToken().toLowerCase(), Boolean.TRUE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // CONSTRUCTOR, FACTORY


    /**
     * Create an HTTPS client URL.  Traffic will be tunneled through any
     * intermediate nodes rather than proxied, so that confidentiality
     * of data exchanged can be preserved.  However, note that all the
     * anonymous SSL flavors are subject to "person-in-the-middle"
     * attacks against confidentiality.  If you enable use of those
     * flavors, you may be giving up the protection you get through
     * SSL tunneling.
     *
     * @param url https URL with which a connection must be established
     */
    public HttpsClient (URL url) throws IOException {
        // HttpClient-level proxying is always disabled,
        // because we override doConnect to do tunneling instead.
        super(url, true);
        if (debug.messageEnabled()) {
            debug.message("HttpsClient: HttpsClient " + url);
        }
    }

    public static HttpClient New (URL url) throws IOException {
        if (debug.messageEnabled()) {
            debug.message("HttpsClient: New " + url);
        }
        return new HttpsClient (url);
    }

    /**
     * Returns true if host is on the "don't proxy" list.
     */
    protected boolean isNonProxyHost() {
        if (dontProxy.match(url.getHost().toLowerCase()) != null) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(url.getHost());
            String host = addr.getHostAddress();
            if (dontProxy.match(host) != null) {
                return true;
            }
        } catch (UnknownHostException ignored) {
            debug.error("Host name is unknown but ignored.", ignored);
        }
        return false;
    }

    /**
     * Overrides HTTP protocol handler method so that we return an SSL
     * socket, not a TCP socket.  This establishes a secure tunnel if
     * appropriate.
     *
     * @param host the host to connect to
     * @param port the port on that host.
     * @exception IOException on errors including a host doesn't
     *        authenicate corectly.
     * @exception UnknownHostException if "host" is unknown
     */
    protected Socket doConnect (String host, int port)
    throws IOException
    {
        String hostname = null;
        
        try {
            hostname = InetAddress.getByName(host).getHostName();
        } catch (UnknownHostException e) {
            if (debug.messageEnabled()) {
                debug.message("Error : HttpsClient.doConnect ", e);
            }
            hostname = host;
        }

        if (secureTunnelHost == null || isNonProxyHost()) {
            if (debug.messageEnabled()) {
                debug.message("HttpsClient: doConnect(" + hostname +
                                       ", " + port + ")");
            }
        
            sslSocket = new SSLSocket(
                         InetAddress.getByName(hostname),
                         port,
                         null,
                         0,
                         new ApprovalCallback(hostname),
                         this);
        }
        else {
            if (debug.messageEnabled()) {
                debug.message("HttpsClient: doConnect through proxy " + 
                    secureTunnelHost + ":" + secureTunnelPort);
            }

            if (debug.messageEnabled()) {
                debug.message("HttpsClient: doConnect(" + hostname +
                                       ", " + port + ")");
            }
            sslSocket = new SSLSocket(
                         InetAddress.getByName("localhost"),
                         JSSProxy.serverPort,
                         null,
                         0,
                         new ApprovalCallback(hostname),
                         this);

            Integer localport = new Integer(sslSocket.getLocalPort());
            String info = secureTunnelHost + " " + secureTunnelPort + " " +
                          host + " " + port;
            JSSProxy.connectHashMap.put(localport, info);
        } 

           return sslSocket;
    }


    /**
     * Returns the cipher suite in use on this connection.
     */
    public String getCipherSuite ()
    {
        try {
            return sslSocket.getStatus().getCipher();
        } catch (Exception e) {
            debug.error(
                "HttpsClient: Can't get cipher suite on the connection.");
            return null;
        }
    }

    /**
     * Returns the X.509 certificate chain with which the server
     * authenticated itself, or null if the server did not authenticate.
     */
    public X509Certificate [] getServerCertificateChain ()
    {
        return null;
    }

    private static String getProperty(String prop) {
        SecurityException ex = null;

        try {
            return System.getProperty(prop);
        } catch (SecurityException e) {
            ex = e;
        }

        /*
         * If we hit an exception then we're probably running
         * with an access controller. Turn on privileges and
         * try again.
         */
        try {
            return (String) java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction("prop"));
        } catch (LinkageError e) {
            debug.error("getProperty caught : ", e);
        }

        throw ex;
    }

    /**
     * Set client certificate nick name used for SSL handshake
     * @param name cert nick name
     */
    public void setClientCertificate(String name) {
        nickName = name;
    }

    public String select (Vector nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            debug.message("nicknames vector is null");
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message("all certs=" + nicknames.toString());
        }
        if (nickName == null) {
            // no cert nick is set, return first one
            debug.message("no nickname is set");
            return (String) nicknames.get(0);
        }
        if (nicknames.contains(nickName)) {
            debug.message("found nickname in vector");
            return nickName;
        }
        debug.message("no matching nickname found");
        return null;
    }
    
    static void configureFIPSSSLoptions() {
        try {
            /* if FIPS is enabled, configure only FIPS ciphersuites */
            if (CryptoManager.getInstance().FIPSEnabled()) {
                //Disable SSL2 and SSL3 ciphers
                SSLSocket.enableSSL2Default(false);
                SSLSocket.enableSSL3Default(false);
                SSLSocket.enableTLSDefault(true);
                /* TLS is enabled by default */
                debug.message("NSS database is confirued in FIPS mode.");
                debug.message("Enable FIPS ciphersuites only.");
                int ciphers[] =
                    org.mozilla.jss.ssl.SSLSocket.getImplementedCipherSuites();
                for (int i = 0; i < ciphers.length;  ++i) {
                    if (SSLSocket.isFipsCipherSuite(ciphers[i])) {
                        /* enable the FIPS ciphersuite */
                        SSLSocket.setCipherPreferenceDefault(ciphers[i], true);
                    } else if (SSLSocket.getCipherPreferenceDefault(
                               ciphers[i])) {
                        /* disable the non fips ciphersuite */
                        SSLSocket.setCipherPreferenceDefault(ciphers[i], false);
                    }
                }
            }
        } catch (SocketException ex) {
            debug.error("Error configuring FIPS SSL options.", ex);
        } catch (NotInitializedException ex) {
            debug.error("Error configuring FIPS SSL options.", ex);
        }
    }    
}
