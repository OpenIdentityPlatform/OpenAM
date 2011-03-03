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
 * $Id: ClientHandler.java,v 1.11 2009/10/18 18:41:27 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.client;

import com.sun.identity.proxy.handler.Handler;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Headers;
import com.sun.identity.proxy.http.Response;
import com.sun.identity.proxy.util.CIStringSet;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.client.protocol.RequestProxyAuthentication;
import org.apache.http.client.protocol.RequestTargetAuthentication;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

/**
 * A handler class that submits requests via the Apache HttpComponents Client.
 * <p>
 * This handler does not verify hostnames for outgoing SSL connections. This is
 * because the proxy can access the SSL endpoint using an IP address instead
 * of the hostname.
 *
 * @author Paul C. Bryan
 */
public class ClientHandler implements Handler
{
    /** Default maximum number of collections through HTTP client. */
    private static final int DEFAULT_CONNECTIONS = 64;

    /** Headers that are suppressed in request. */
    private static final CIStringSet SUPPRESS_REQUEST_HEADERS = new CIStringSet(Arrays.asList(
     "Connection", "Content-Encoding", "Content-Length", "Content-Type", "Expect", "Keep-Alive",
      "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers", "Transfer-Encoding", "Upgrade"));

    /** Headers that are suppressed in response. */
    private static final CIStringSet SUPPRESS_RESPONSE_HEADERS = new CIStringSet(Arrays.asList(
     "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers",
     "Transfer-Encoding", "Upgrade"));

    /** Delimiter to split tokens within the Connection header. */
    private static final Pattern DELIM_TOKEN = Pattern.compile("[,\\s]+");

    /** The HTTP client to transmit requests through. */
    private DefaultHttpClient httpClient;

    /**
     * Creates a new client handler with a default maximum number of connections.
     */
    public ClientHandler() {
        this(DEFAULT_CONNECTIONS);
    }

    /**
     * Returns a new SSL socket factory that does not perform hostname
     * verification.
     *
     * @return the new SSL socket factory.
     */
    private static SSLSocketFactory newSSLSocketFactory() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae); // TODO: handle this better?
        }
        try {
            sslContext.init(null, null, null);
        }
        catch (KeyManagementException kme) {
            throw new IllegalStateException(kme); // TODO: handle this better?
        }
        SSLSocketFactory sslSocketFactory = new SSLSocketFactory(sslContext);
        sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return sslSocketFactory;
    }

    /**
     * Returns the names of the headers specified in the Connection header.
     * These headers need to be treated as hop-by-hop headers and be
     * suppressed.
     *
     * @param headers the headers to search for the Connection header within.
     * @return the set of headers to additionally suppress.
     */
    private CIStringSet getConnectionHeaders(Headers headers) {
        CIStringSet set = new CIStringSet();
        List<String> values = headers.get("Connection");
        if (values != null) {
            for (String value : values) {
                set.addAll(Arrays.asList(DELIM_TOKEN.split(value)));
            }
        }
        return set;
    }            

    /**
     * Creates a new client handler with the specified maximum number of
     * connections.
     *
     * @param connections the maximum number of connections to open.
     */
    public ClientHandler(int connections)
    {
        BasicHttpParams parameters = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(parameters, connections);
        ConnManagerParams.setMaxConnectionsPerRoute(parameters, new ConnPerRouteBean(connections));
        HttpProtocolParams.setVersion(parameters, HttpVersion.HTTP_1_1);
        HttpClientParams.setRedirecting(parameters, false);

        SchemeRegistry registry = new SchemeRegistry();

        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", newSSLSocketFactory(), 443));

        ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(parameters, registry);

        httpClient = new DefaultHttpClient(connectionManager, parameters);
        httpClient.removeRequestInterceptorByClass(RequestAddCookies.class);
        httpClient.removeRequestInterceptorByClass(RequestProxyAuthentication.class);
        httpClient.removeRequestInterceptorByClass(RequestTargetAuthentication.class);
        httpClient.removeResponseInterceptorByClass(ResponseProcessCookies.class);

// TODO: set timeout to drop stalled connections?
// FIXME: prevent automatic retry by apache httpclient
    }

    /**
     * Submits the exchange request to the remote server. Creates and
     * populates the exchange response from that provided by the remote server.
     */
    @Override
    public void handle(Exchange exchange) throws IOException, HandlerException
    {
        // recover any previous response connection, if present
        if (exchange.response != null && exchange.response.entity != null) {
            exchange.response.entity.close();
        }

        HttpRequestBase clientRequest = (exchange.request.entity != null ?
         new EntityRequest(exchange.request) : new NonEntityRequest(exchange.request));

        clientRequest.setURI(exchange.request.uri);

        // connection headers to suppress
        CIStringSet suppressConnection = new CIStringSet();

        // parse request connection headers to be treated as hop-to-hop
        suppressConnection.clear();
        suppressConnection.addAll(getConnectionHeaders(exchange.request.headers));

        // request headers
        for (String name : exchange.request.headers.keySet()) {
            if (!SUPPRESS_REQUEST_HEADERS.contains(name) && !suppressConnection.contains(name)) {
                for (String value : exchange.request.headers.get(name)) {
                    clientRequest.addHeader(name, value);
                }
            }
        }

        HttpResponse clientResponse = httpClient.execute(clientRequest);

        exchange.response = new Response();

        // response entity
        HttpEntity clientResponseEntity = clientResponse.getEntity();
        if (clientResponseEntity != null) {
            exchange.response.entity = clientResponseEntity.getContent();
        }

        // response status line
        StatusLine statusLine = clientResponse.getStatusLine();
        exchange.response.version = statusLine.getProtocolVersion().toString();
        exchange.response.status = statusLine.getStatusCode();
        exchange.response.reason = statusLine.getReasonPhrase();

        // parse response connection headers to be suppressed in response
        suppressConnection.clear();
        suppressConnection.addAll(getConnectionHeaders(exchange.response.headers));

        // response headers
        for (HeaderIterator i = clientResponse.headerIterator(); i.hasNext();) {
            Header header = i.nextHeader();
            String name = header.getName();
            if (!SUPPRESS_RESPONSE_HEADERS.contains(name) && !suppressConnection.contains(name)) {
                exchange.response.headers.add(name, header.getValue());
            }
        }

// TODO: decide if need to try-finally to call httpRequest.abort?
    }
}

