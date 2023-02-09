/* jcifs smb client library in Java
 * Copyright (C) 2002  "Michael B. Allen" <jcifs at samba dot org>
 *                   "Eric Glass" <jcifs at samba dot org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.http;

import jcifs.Config;
import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.NtlmMessage;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps an <code>HttpURLConnection</code> to provide NTLM authentication
 * services.
 *
 * Please read <a href="../../../httpclient.html">Using jCIFS NTLM Authentication for HTTP Connections</a>.
 */
public class NtlmHttpURLConnection extends HttpURLConnection {

    private static final int MAX_REDIRECTS =
            Integer.parseInt(System.getProperty("http.maxRedirects", "20"));

    private static final int LM_COMPATIBILITY =
            Config.getInt("jcifs.smb.lmCompatibility", 0);

    private static final String DEFAULT_DOMAIN;

    private HttpURLConnection connection;

    private Map requestProperties;

    private Map headerFields;

    private ByteArrayOutputStream cachedOutput;

    private String authProperty;

    private String authMethod;

    private boolean handshakeComplete;

    static {
        String domain = System.getProperty("http.auth.ntlm.domain");
        if (domain == null) domain = Type3Message.getDefaultDomain();
        DEFAULT_DOMAIN = domain;
    }

    public NtlmHttpURLConnection(HttpURLConnection connection) {
        super(connection.getURL());
        this.connection = connection;
        requestProperties = new HashMap();
    }

    public void connect() throws IOException {
        if (connected) return;
        connection.connect();
        connected = true;
    }

    private void handshake() throws IOException {
        if (handshakeComplete) return;
        doHandshake();
        handshakeComplete = true;
    }

    public URL getURL() {
        return connection.getURL();
    }

    public int getContentLength() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getContentLength();
    }

    public String getContentType() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getContentType();
    }

    public String getContentEncoding() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getContentEncoding();
    }

    public long getExpiration() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getExpiration();
    }

    public long getDate() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getDate();
    }

    public long getLastModified() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getLastModified();
    }

    public String getHeaderField(String header) {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getHeaderField(header);
    }

    private Map getHeaderFields0() {
        if (headerFields != null) return headerFields;
        Map map = new HashMap();
        String key = connection.getHeaderFieldKey(0);
        String value = connection.getHeaderField(0);
        for (int i = 1; key != null || value != null; i++) {
            List values = (List) map.get(key);
            if (values == null) {
                values = new ArrayList();
                map.put(key, values);
            }
            values.add(value);
            key = connection.getHeaderFieldKey(i);
            value = connection.getHeaderField(i);
        }
        Iterator entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            entry.setValue(Collections.unmodifiableList((List)
                    entry.getValue()));
        }
        return (headerFields = Collections.unmodifiableMap(map));
    }

    public Map getHeaderFields() {
        if (headerFields != null) return headerFields;
        try {
            handshake();
        } catch (IOException ex) { }
        return getHeaderFields0();
    }

    public int getHeaderFieldInt(String header, int def) {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getHeaderFieldInt(header, def);
    }

    public long getHeaderFieldDate(String header, long def) {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getHeaderFieldDate(header, def);
    }

    public String getHeaderFieldKey(int index) {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getHeaderFieldKey(index);
    }

    public String getHeaderField(int index) {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getHeaderField(index);
    }

    public Object getContent() throws IOException {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getContent();
    }

    public Object getContent(Class[] classes) throws IOException {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getContent(classes);
    }

    public Permission getPermission() throws IOException {
        return connection.getPermission();
    }

    public InputStream getInputStream() throws IOException {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        try {
            connect();
        } catch (IOException ex) { }
        OutputStream output = connection.getOutputStream();
        cachedOutput = new ByteArrayOutputStream();
        return new CacheStream(output, cachedOutput);
    }

    public String toString() {
        return connection.toString();
    }

    public void setDoInput(boolean doInput) {
        connection.setDoInput(doInput);
        this.doInput = doInput;
    }

    public boolean getDoInput() {
        return connection.getDoInput();
    }

    public void setDoOutput(boolean doOutput) {
        connection.setDoOutput(doOutput);
        this.doOutput = doOutput;
    }

    public boolean getDoOutput() {
        return connection.getDoOutput();
    }

    public void setAllowUserInteraction(boolean allowUserInteraction) {
        connection.setAllowUserInteraction(allowUserInteraction);
        this.allowUserInteraction = allowUserInteraction;
    }

    public boolean getAllowUserInteraction() {
        return connection.getAllowUserInteraction();
    }

    public void setUseCaches(boolean useCaches) {
        connection.setUseCaches(useCaches);
        this.useCaches = useCaches;
    }

    public boolean getUseCaches() {
        return connection.getUseCaches();
    }

    public void setIfModifiedSince(long ifModifiedSince) {
        connection.setIfModifiedSince(ifModifiedSince);
        this.ifModifiedSince = ifModifiedSince;
    }

    public long getIfModifiedSince() {
        return connection.getIfModifiedSince();
    }

    public boolean getDefaultUseCaches() {
        return connection.getDefaultUseCaches();
    }

    public void setDefaultUseCaches(boolean defaultUseCaches) {
        connection.setDefaultUseCaches(defaultUseCaches);
    }

    public void setRequestProperty(String key, String value) {
        if (key == null) throw new NullPointerException();
        List values = new ArrayList();
        values.add(value);
        boolean found = false;
        Iterator entries = requestProperties.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                entry.setValue(values);
                found = true;
                break;
            }
        }
        if (!found) requestProperties.put(key, values);
        connection.setRequestProperty(key, value);
    }

    public void addRequestProperty(String key, String value) {
        if (key == null) throw new NullPointerException();
        List values = null;
        Iterator entries = requestProperties.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                values = (List) entry.getValue();
                values.add(value);
                break;
            }
        }
        if (values == null) {
            values = new ArrayList();
            values.add(value);
            requestProperties.put(key, values);
        }
        // 1.3-compatible.
        StringBuffer buffer = new StringBuffer();
        Iterator propertyValues = values.iterator();
        while (propertyValues.hasNext()) {
            buffer.append(propertyValues.next());
            if (propertyValues.hasNext()) {
                buffer.append(", ");
            }
        }
        connection.setRequestProperty(key, buffer.toString());
    }

    public String getRequestProperty(String key) {
        return connection.getRequestProperty(key);
    }

    public Map getRequestProperties() {
        Map map = new HashMap();
        Iterator entries = requestProperties.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            map.put(entry.getKey(),
                    Collections.unmodifiableList((List) entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    public void setInstanceFollowRedirects(boolean instanceFollowRedirects) {
        connection.setInstanceFollowRedirects(instanceFollowRedirects);
    }

    public boolean getInstanceFollowRedirects() {
        return connection.getInstanceFollowRedirects();
    }

    public void setRequestMethod(String requestMethod)
            throws ProtocolException {
        connection.setRequestMethod(requestMethod);
        this.method = requestMethod;
    }

    public String getRequestMethod() {
        return connection.getRequestMethod();
    }

    public int getResponseCode() throws IOException {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getResponseMessage();
    }

    public void disconnect() {
        connection.disconnect();
        handshakeComplete = false;
        connected = false;
    }

    public boolean usingProxy() {
        return connection.usingProxy();
    }

    public InputStream getErrorStream() {
        try {
            handshake();
        } catch (IOException ex) { }
        return connection.getErrorStream();
    }

    private int parseResponseCode() throws IOException {
        try {
            String response = connection.getHeaderField(0);
            int index = response.indexOf(' ');
            while (response.charAt(index) == ' ') index++;
            return Integer.parseInt(response.substring(index, index + 3));
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private void doHandshake() throws IOException {
        connect();
        try {
            int response = parseResponseCode();
            if (response != HTTP_UNAUTHORIZED && response != HTTP_PROXY_AUTH) {
                return;
            }
            Type1Message type1 = (Type1Message) attemptNegotiation(response);
            if (type1 == null) return; // no NTLM
            int attempt = 0;
            while (attempt < MAX_REDIRECTS) {
                connection.setRequestProperty(authProperty, authMethod + ' ' +
                        Base64.encode(type1.toByteArray()));
                connection.connect(); // send type 1
                response = parseResponseCode();
                if (response != HTTP_UNAUTHORIZED &&
                        response != HTTP_PROXY_AUTH) {
                    return;
                }
                Type3Message type3 = (Type3Message)
                        attemptNegotiation(response);
                if (type3 == null) return;
                connection.setRequestProperty(authProperty, authMethod + ' ' +
                        Base64.encode(type3.toByteArray()));
                connection.connect(); // send type 3
                if (cachedOutput != null && doOutput) {
                    OutputStream output = connection.getOutputStream();
                    cachedOutput.writeTo(output);
                    output.flush();
                }
                response = parseResponseCode();
                if (response != HTTP_UNAUTHORIZED &&
                        response != HTTP_PROXY_AUTH) {
                    return;
                }
                attempt++;
                if (allowUserInteraction && attempt < MAX_REDIRECTS) {
                    reconnect();
                } else {
                    break;
                }
            }
            throw new IOException("Unable to negotiate NTLM authentication.");
        } finally {
            cachedOutput = null;
        }
    }

    private NtlmMessage attemptNegotiation(int response) throws IOException {
        authProperty = null;
        authMethod = null;
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null && errorStream.available() != 0) {
            int count;
            byte[] buf = new byte[1024];
            while ((count = errorStream.read(buf, 0, 1024)) != -1);
        }
        String authHeader;
        if (response == HTTP_UNAUTHORIZED) {
            authHeader = "WWW-Authenticate";
            authProperty = "Authorization";
        } else {
            authHeader = "Proxy-Authenticate";
            authProperty = "Proxy-Authorization";
        }
        String authorization = null;
        List methods = (List) getHeaderFields0().get(authHeader);
        if (methods == null) return null;
        Iterator iterator = methods.iterator();
        while (iterator.hasNext()) {
            String currentAuthMethod = (String) iterator.next();
            if (currentAuthMethod.startsWith("NTLM")) {
                if (currentAuthMethod.length() == 4) {
                    authMethod = "NTLM";
                    break;
                }
                if (currentAuthMethod.indexOf(' ') != 4) continue;
                authMethod = "NTLM";
                authorization = currentAuthMethod.substring(5).trim();
                break;
            } else if (currentAuthMethod.startsWith("Negotiate")) {
                if (currentAuthMethod.length() == 9) {
                    authMethod = "Negotiate";
                    break;
                }
                if (currentAuthMethod.indexOf(' ') != 9) continue;
                authMethod = "Negotiate";
                authorization = currentAuthMethod.substring(10).trim();
                break;
            }
        }
        if (authMethod == null) return null;
        NtlmMessage message = (authorization != null) ?
                new Type2Message(Base64.decode(authorization)) : null;
        reconnect();
        if (message == null) {
            message = new Type1Message();
            if (LM_COMPATIBILITY > 2) {
                message.setFlag(NtlmFlags.NTLMSSP_REQUEST_TARGET, true);
            }
        } else {
            String domain = DEFAULT_DOMAIN;
            String user = Type3Message.getDefaultUser();
            String password = Type3Message.getDefaultPassword();
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                userInfo = URLDecoder.decode(userInfo);
                int index = userInfo.indexOf(':');
                user = (index != -1) ? userInfo.substring(0, index) : userInfo;
                if (index != -1) password = userInfo.substring(index + 1);
                index = user.indexOf('\\');
                if (index == -1) index = user.indexOf('/');
                domain = (index != -1) ? user.substring(0, index) : domain;
                user = (index != -1) ? user.substring(index + 1) : user;
            }
            if (user == null) {
                if (!allowUserInteraction) return null;
                try {
                    URL url = getURL();
                    String protocol = url.getProtocol();
                    int port = url.getPort();
                    if (port == -1) {
                        port = "https".equalsIgnoreCase(protocol) ? 443 : 80;
                    }
                    PasswordAuthentication auth =
                            Authenticator.requestPasswordAuthentication(null,
                                    port, protocol, "", authMethod);
                    if (auth == null) return null;
                    user = auth.getUserName();
                    password = new String(auth.getPassword());
                } catch (Exception ex) { }
            }
            Type2Message type2 = (Type2Message) message;
            message = new Type3Message(type2, password, domain, user,
                    Type3Message.getDefaultWorkstation(), 0);
        }
        return message;
    }

    private void reconnect() throws IOException {
        connection = (HttpURLConnection) connection.getURL().openConnection();
        connection.setRequestMethod(method);
        headerFields = null;
        Iterator properties = requestProperties.entrySet().iterator();
        while (properties.hasNext()) {
            Map.Entry property = (Map.Entry) properties.next();
            String key = (String) property.getKey();
            StringBuffer value = new StringBuffer();
            Iterator values = ((List) property.getValue()).iterator();
            while (values.hasNext()) {
                value.append(values.next());
                if (values.hasNext()) value.append(", ");
            }
            connection.setRequestProperty(key, value.toString());
        }
        connection.setAllowUserInteraction(allowUserInteraction);
        connection.setDoInput(doInput);
        connection.setDoOutput(doOutput);
        connection.setIfModifiedSince(ifModifiedSince);
        connection.setUseCaches(useCaches);
    }

    private static class CacheStream extends OutputStream {

        private final OutputStream stream;

        private final OutputStream collector;

        public CacheStream(OutputStream stream, OutputStream collector) {
            this.stream = stream;
            this.collector = collector;
        }

        public void close() throws IOException {
            stream.close();
            collector.close();
        }

        public void flush() throws IOException {
            stream.flush();
            collector.flush();
        }

        public void write(byte[] b) throws IOException {
            stream.write(b);
            collector.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            stream.write(b, off, len);
            collector.write(b, off, len);
        }

        public void write(int b) throws IOException {
            stream.write(b);
            collector.write(b);
        }

    }

}
