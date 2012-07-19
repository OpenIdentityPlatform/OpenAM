/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.openam;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.restlet.data.Protocol;
import org.restlet.data.Reference;

/**
 * An OpenAMParameters is a proxy class to AMConfig.properties.
 * <p/>
 * If the OpenAM SDK is available in the class path this class proxies the calls
 * to {@link com.iplanet.am.util.SystemProperties} otherwise it keeps the
 * properties in its own {@link #getAttributes()} Map.
 * 
 * @author Laszlo Hordos
 */
public class OpenAMParameters {

    public enum IndexType {
        SERVICE, MODULE;

        public String getQueryParameter() {
            return name().toLowerCase();
        }
    }

    public static final String AUTHENTICATE = "authenticate";
    public static final String LOGOUT = "logout";
    public static final String ISTOKENVALID = "isTokenValid";
    public static final String ATTRIBUTES = "attributes";
    public static final String AUTHORIZE = "authorize";

    public static final String AM_SERVER_PROTOCOL = "com.iplanet.am.server.protocol";
    public static final String AM_SERVER_HOST = "com.iplanet.am.server.host";
    public static final String AM_SERVER_PORT = "com.iplanet.am.server.port";
    public static final String AM_SERVICES_DEPLOYMENT_DESCRIPTOR =
            "com.iplanet.am.services.deploymentDescriptor";
    public static final String SERVICES_DEBUG_DIRECTORY = "com.iplanet.services.debug.directory";
    public static final String AM_APPLICATION_USERNAME = "com.sun.identity.agents.app.username";
    public static final String AM_APPLICATION_PASSWORD = "com.iplanet.am.service.password";
    public static final String AM_SERVER_LOGINURL = "com.sun.identity.loginurl";

    public static final boolean OPENAM_SDK;

    static {
        boolean isPresent = true;
        try {
            com.iplanet.am.util.SystemProperties.lastModified();
        } catch (NoClassDefFoundError e) {
            isPresent = false;
        }
        OPENAM_SDK = isPresent;
    }

    /**
     * The modifiable attributes map.
     */
    private final ConcurrentMap<String, Object> attributes;

    private String loginIndexName = "DataStore";
    private String orgName = "/";
    private String locale = null;
    private IndexType loginIndexType = null;

    public OpenAMParameters() {
        this.attributes = new ConcurrentHashMap<String, Object>();
    }

    public Reference getOpenAMServerRef() {
        Reference baseRef = new Reference();
        baseRef.setScheme(getServerProtocol().getSchemeName());
        baseRef.setHostDomain(getServerHost());
        baseRef.setHostPort(getServerPort());
        baseRef.setPath(getServerDeploymentURI());
        return baseRef;
    }

    /**
     * Protocol to access OpenAM (http or https)
     */
    public Protocol getServerProtocol() {
        String value = getProperty(AM_SERVER_PROTOCOL);
        return value != null ? Protocol.valueOf(value) : null;
    }

    public void setServerProtocol(Protocol serverProtocol) {
        attributes.put(AM_SERVER_PROTOCOL, serverProtocol.getName());
    }

    /**
     * Fully qualified domain name for OpenAM, such as openam.example.com
     */
    public String getServerHost() {
        return getProperty(AM_SERVER_HOST);
    }

    public void setServerHost(String serverHost) {
        attributes.put(AM_SERVER_HOST, serverHost);
    }

    /**
     * OpenAM port number such as 8080 or 8443
     */
    public int getServerPort() {
        String value = getProperty(AM_SERVER_PORT);
        return value != null ? Integer.parseInt(value) : null;
    }

    public void setServerPort(int serverPort) {
        attributes.put(AM_SERVER_PORT, Integer.toString(serverPort));
    }

    /**
     * URI entry point to OpenAM such as openam
     * <p/>
     * The leading "/" is removed
     */
    public String getServerDeploymentURI() {
        String uri = getProperty(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        if (null != uri) {
            return uri.startsWith("/") ? uri : "/" + uri;
        }
        return "";
    }

    public void setServerDeploymentURI(String serverDeploymentURI) {
        attributes.put(AM_SERVER_HOST, serverDeploymentURI);
    }

    /**
     * Where to write the debug messages for the client samples
     */
    public String getDebugDirectory() {
        return getProperty(SERVICES_DEBUG_DIRECTORY);
    }

    public void setDebugDirectory(String debugDirectory) {
        attributes.put(SERVICES_DEBUG_DIRECTORY, debugDirectory);
    }

    /**
     * An user agent configured to access OpenAM, such as UrlAccessAgent set up
     * when OpenAM was installed
     */
    public String getApplicationUserName() {
        return getProperty(AM_APPLICATION_USERNAME);
    }

    public void setApplicationUserName(String applicationUserName) {
        attributes.put(AM_APPLICATION_USERNAME, applicationUserName);
    }

    /**
     * The user agent password
     */
    public String getApplicationUserPassword() {
        return getProperty(AM_APPLICATION_PASSWORD);
    }

    public void setApplicationUserPassword(String applicationUserPassword) {
        attributes.put(AM_APPLICATION_PASSWORD, applicationUserPassword);
    }

    public String getLoginIndexName() {
        return loginIndexName;
    }

    public void setLoginIndexName(String loginIndexName) {
        this.loginIndexName = loginIndexName;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getLocale() {
        return locale != null && locale.length() > 0 ? locale : null;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public IndexType getLoginIndexType() {
        return loginIndexType;
    }

    public void setLoginIndexType(IndexType loginIndexType) {
        this.loginIndexType = loginIndexType;
    }

    public ConcurrentMap<String, Object> getAttributes() {
        return attributes;
    }

    // -------------------------------------------------------------

    protected String getProperty(String name) {
        Object value = attributes.get(name);
        if (OPENAM_SDK) {
            value = com.iplanet.am.util.SystemProperties.get(name);
        }
        return value instanceof String ? (String) value : null;
    }
}
