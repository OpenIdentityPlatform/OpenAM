/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: SavedHttpServletRequest.java,v 1.3 2008/11/20 08:43:40 rsoika Exp $
 */
package com.sun.security.sam;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import java.security.Principal;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * TODO: url correlation on restore
 * Max buffer size handling
 * @author monzillo
 */
public class SavedHttpServletRequest extends HttpServletRequestWrapper {

    private ServletInputStream stream;
    private ArrayList locales;
    private boolean attached;
    private boolean isSecure;
    private boolean readerRead;
    private boolean streamRead;
    private Cookie[] cookies;
    private Hashtable attributes;
    private Hashtable<String,ArrayList<String>> headers;
    private Hashtable parameters;
    private int contentLength;
    private int localPort;
    private int remotePort;
    private int serverPort;
    private String characterEncoding;
    private String contentType;
    private String localAddr;
    private String localName;
    private String method;
    private String pathInfo;
    private String pathTranslated;
    private String queryString;
    private String protocol;
    private String remoteAddr;
    private String remoteHost;
    private String requestURI;
    private String scheme;
    private String serverName;
    private String servletPath;
    private StringBuffer requestURL;
    private static final int MAX_CONTENT_LENGTH = 256;

    public SavedHttpServletRequest(HttpServletRequest request)
            throws IOException {

        super(request);

        stream = createStreamWrapper(request, MAX_CONTENT_LENGTH);

        initSavedRequest(request);
    }

    public SavedHttpServletRequest(HttpServletRequest request,
            int maxContentLength) throws IOException {
        super(request);

        stream = createStreamWrapper(request, maxContentLength);

        initSavedRequest(request);
    }

    private ServletInputStream createStreamWrapper(HttpServletRequest request,
            int maxContentLength)
            throws IOException {

        ServletInputStream rvalue;
        try {
            rvalue = new ServletInputStreamWrapper(request, maxContentLength);
            readerRead = streamRead = false;
        } catch (IllegalStateException ise) {
            rvalue = null;
        }
        return rvalue;
    }

    private void initSavedRequest(HttpServletRequest request)
            throws IOException {

        attached = false;

        isSecure = request.isSecure();

        cookies = request.getCookies();

        locales = new ArrayList();
        Enumeration lValues = request.getLocales();
        while (lValues.hasMoreElements()) {
            locales.add(lValues.nextElement());
        }

        attributes = new Hashtable();
        Enumeration aNames = request.getAttributeNames();
        while (aNames.hasMoreElements()) {
            String name = (String) aNames.nextElement();
            attributes.put(name, request.getAttribute(name));
        }

        headers = new Hashtable<String,ArrayList<String>>();
        Enumeration hNames = request.getHeaderNames();
        while (hNames.hasMoreElements()) {
            String name = (String) hNames.nextElement();
            Enumeration<String> values = request.getHeaders(name);
            ArrayList<String> list = new ArrayList();
            while (values.hasMoreElements()) {
                list.add(values.nextElement());
            }
            headers.put(name, list);
        }

        parameters = new Hashtable();
        Enumeration pNames = request.getParameterNames();
        while (pNames.hasMoreElements()) {
            String name = (String) pNames.nextElement();
            parameters.put(name, request.getParameterValues(name));
        }

        localPort = request.getLocalPort();

        remotePort = request.getRemotePort();

        serverPort = request.getServerPort();

        characterEncoding = request.getCharacterEncoding();

        contentType = request.getContentType();

        localAddr = request.getLocalAddr();

        localName = request.getLocalName();

        method = request.getMethod();

        pathInfo = request.getPathInfo();

        pathTranslated = request.getPathTranslated();

        protocol = request.getProtocol();

        queryString = request.getQueryString();

        remoteAddr = request.getRemoteAddr();

        remoteHost = request.getRemoteHost();

        requestURI = request.getRequestURI();

        scheme = request.getScheme();

        serverName = request.getServerName();

        servletPath = request.getServletPath();

        requestURL = request.getRequestURL();

    }

    // Methods from HttpServletRequest

    // return value defined by attached request or operation is unsupported
    @Override
    public String getAuthType() {
        if (attached) {
            return super.getAuthType();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public String getContextPath() {
        if (attached) {
            return super.getContextPath();
        }
        throw new UnsupportedOperationException();
    }

    // should restore merge in cookies from new request?
    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    // should restore merge in headers from new request?
    @Override
    public long getDateHeader(String name) {
        String hValue = getHeader(name);
        if (hValue != null) {
            try {
                return ((SimpleDateFormat) SimpleDateFormat.getInstance()).parse(hValue).getTime();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return -1;
    }

    @Override
    public String getHeader(String name) {
        ArrayList<String> value = headers.get(name);
        if (value != null && value.size() > 0) {
            return value.get(0);
        }
        return null;
    }

    @Override
    public Enumeration getHeaderNames() {
        return headers.keys();
    }

    @Override
    public Enumeration getHeaders(final String name) {

        return new Enumeration<String>() {

            private int position = 0;
            private ArrayList<String> values = headers.get(name);

            public boolean hasMoreElements() {
                return (position < values.size());
            }

            public String nextElement() {
                if (position < values.size()) {
                    return values.get(position++);
                }
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public int getIntHeader(String name) {
        String hValue = getHeader(name);
        if (hValue != null) {
            return new Integer(hValue).intValue();
        }
        return -1;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return pathTranslated;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public String getRemoteUser() {
        if (attached) {
            return super.getRemoteUser();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public String getRequestedSessionId() {
        if (attached) {
            return super.getRequestedSessionId();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        return requestURL;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public HttpSession getSession() {
        if (attached) {
            return super.getSession();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public HttpSession getSession(boolean create) {
        if (attached) {
            return super.getSession(create);
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public Principal getUserPrincipal() {
        if (attached) {
            return super.getUserPrincipal();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        if (attached) {
            return super.isRequestedSessionIdFromCookie();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        if (attached) {
            return super.isRequestedSessionIdFromUrl();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public boolean isRequestedSessionIdFromURL() {
        if (attached) {
            return super.isRequestedSessionIdFromURL();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public boolean isRequestedSessionIdValid() {
        if (attached) {
            return super.isRequestedSessionIdValid();
        }
        throw new UnsupportedOperationException();
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public boolean isUserInRole(String role) {
        if (attached) {
            return super.isUserInRole(role);
        }
        throw new UnsupportedOperationException();
    }

    // Methods from ServletRequest

    // should restore merge in attributes from new request?
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration getAttributeNames() {
        return attributes.keys();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletInputStream getInputStream() {
        if (stream == null) {
            throw new IllegalStateException();
        }
        if (readerRead) {
            throw new IllegalStateException();
        }
        streamRead = true;
        return stream;
    }

    @Override
    public String getLocalAddr() {
        return localAddr;
    }

    @Override
    public Locale getLocale() {
        return (Locale) locales.get(0);
    }

    @Override
    public Enumeration getLocales() {

        return new Enumeration<Locale>() {

            private int position = 0;

            public boolean hasMoreElements() {
                return (position < locales.size());
            }

            public Locale nextElement() {
                if (position < locales.size()) {
                    return (Locale) locales.get(position++);
                }
                throw new NoSuchElementException();
            }
        };

    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    @Override
    public Map getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration getParameterNames() {
        return parameters.keys();
    }

    @Override
    public String[] getParameterValues(String name) {
        return (String[]) parameters.get(name);
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (stream == null) {
            throw new IllegalStateException();
        }

        if (streamRead) {
            throw new IllegalStateException();
        }

// does getCharacterEncoding return the nameof a character set?
        BufferedReader rvalue = new BufferedReader(characterEncoding == null ? new InputStreamReader(stream) : new InputStreamReader(stream, characterEncoding));

        readerRead =
                true;
        return rvalue;
    }

// Deprecated, jst throw exception, as otherwise
// would have to call deprecated api to save value
// so that it could be available from saved request.
    @Override
    public String getRealPath(
            String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    // return value defined by attached request or operation is unsupported
    @Override
    public RequestDispatcher getRequestDispatcher(
            String path) {
        if (attached) {
            return super.getRequestDispatcher(path);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

// cannot be called after the request is saved
    @Override
    public void setCharacterEncoding(String enc) {
        throw new UnsupportedOperationException();
    }

// will return null when wrapper is detached
    @Override
    public ServletRequest getRequest() {
        return (attached ? super.getRequest() : null);
    }

    @Override
    public void setRequest(ServletRequest request) {
        super.setRequest(request);
        attached = true;
    }

    class ServletInputStreamWrapper extends ServletInputStream {

        int size;
        int[] lineLengths;
        InputStream byteStream;

        ServletInputStreamWrapper(HttpServletRequest request,
                int maxContentLength) throws IOException,
                IllegalStateException {

            contentLength = request.getContentLength();

            if (contentLength > 0) {
                if (maxContentLength > 0 && maxContentLength < contentLength) {
                    throw new IOException("content length too large");
                }
            }

            int i = 1;
            int p = 0;

            byte[] bytes = new byte[contentLength > 0
                    ? contentLength : (maxContentLength > 0
                    ? maxContentLength
                    : MAX_CONTENT_LENGTH)];

            ServletInputStream requestStream = request.getInputStream();

            ArrayList lengths = new ArrayList();

            while (i > 0 && bytes.length > p) {
                i = requestStream.readLine(bytes, p , bytes.length - p);
                lengths.add(new Integer(i));
                if (i > 0) {
                    p += i;
                }
            }

            if (contentLength <= 0 && i != -1) {
        		// try to read one more byte ...
        		byte[] overflow = new byte[1];
        		if (requestStream.readLine(overflow, 0, 1) > 0)
        			throw new IOException("content too large");
        	}

            lineLengths = new int[lengths.size()];

            byteStream = new ByteArrayInputStream(bytes);

            for (i = 0; i < lengths.size(); i++) {
                lineLengths[i] = ((Integer) lengths.get(i)).intValue();
            }

            size = p;
        }

        @Override
        public int readLine(byte[] bytes, int off, int len)
                throws IOException {

            int lineEnd = 0;
            int position = size - stream.available();

            for (int l : lineLengths) {

                if (l > 0) {
                    lineEnd += l;
                }

                if (lineEnd > position) {
                    break;
                }
            }

            if (lineEnd > position) {
                return read(bytes, off, lineEnd - position);
            }

            return -1;
        }

        @Override
        public int available() throws IOException {
            return byteStream.available();
        }

        @Override
        public void close() throws IOException {
            byteStream.close();
        }

        @Override
        public void mark(int readLimit) {
            byteStream.mark(readLimit);
        }

        @Override
        public boolean markSupported() {
            return byteStream.markSupported();
        }

        public int read() throws IOException {
            return byteStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return byteStream.read(b);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            return byteStream.read(b, off, len);
        }

        @Override
        public void reset() throws IOException {
            byteStream.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            return byteStream.skip(n);
        }
    }
}

