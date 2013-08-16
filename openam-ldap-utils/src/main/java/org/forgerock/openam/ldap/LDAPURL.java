/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.openam.ldap;

/**
 * A simple domain object to represent a simple LDAP URL, the URL can have two main formats:
 * <ul>
 *  <li>scheme://host:port</li>
 *  <li>host:port</li>
 * </ul>
 *
 * @author Peter Major
 */
public final class LDAPURL implements Comparable<LDAPURL> {

    private static final String COLON_SLASH_SLASH = "://";
    private static final int DEFAULT_PORT = 389;
    private static final char SEPARATOR = ':';
    private final String host;
    private final int port;
    private final Boolean isSSL;

    private LDAPURL(String host, int port, Boolean isSSL) {
        this.host = host;
        this.port = port;
        this.isSSL = isSSL;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    /**
     * Returns a Boolean instance that tells whether SSL is enabled. The return value can have 3 values:
     * <ul>
     *  <li>null - no protocol scheme has been provided when creating the LDAPURL instance</li>
     *  <li>TRUE - protocol scheme was provided and it was "ldaps"</li>
     *  <li>FALSE - protocol scheme was provided and it was "ldap"</li>
     * </ul>
     * Due to the possible null value, auto-boxing MUST NOT be used when retrieving this value.
     *
     * @return Returns a Boolean instance - which may be null -, that tells whether this LDAPURL is SSL enabled.
     */
    public Boolean isSSL() {
        return isSSL;
    }

    /**
     * Parses an LDAP URL string and constructs an LDAPURL instance. The following two formats are supported currently:
     * <ul>
     *  <li>scheme://host:port</li>
     *  <li>host:port</li>
     * </ul
     *
     * @param url An LDAP URL that needs to be parsed.
     * @return An LDAPURL instance that represents the passed in URL.
     */
    public static LDAPURL valueOf(String url) {
        Boolean isSSL = null;
        String host;
        int port;
        int firstIdx = url.indexOf(COLON_SLASH_SLASH);
        if (firstIdx != -1) {
            String scheme = url.substring(0, firstIdx);
            if (scheme.equalsIgnoreCase("ldaps")) {
                isSSL = true;
            } else {
                isSSL = false;
            }
        }
        int lastIdx = url.indexOf(SEPARATOR, firstIdx + 1);
        if (lastIdx != -1) {
            try {
                port = Integer.parseInt(url.substring(lastIdx + 1));
            } catch (NumberFormatException nfe) {
                port = DEFAULT_PORT;
            }
        } else {
            port = DEFAULT_PORT;
        }
        firstIdx = firstIdx == -1 ? 0 : firstIdx + COLON_SLASH_SLASH.length();
        lastIdx = lastIdx == -1 ? url.length() : lastIdx;
        host = url.substring(firstIdx, lastIdx);
        if (port < 1 || port > 65535) {
            port = DEFAULT_PORT;
        }

        return new LDAPURL(host, port, isSSL);
    }

    /**
     * Creates a new LDAPURL instance based on the provided host and port values.
     *
     * @param host The LDAP host.
     * @param port The LDAP port.
     * @return An LDAPURL instance that represents the provided host/port.
     */
    public static LDAPURL valueOf(String host, int port) {
        return valueOf(host, port, null);
    }

    /**
     * Creates a new LDAPURL instances based on the provided parameters.
     *
     * @param host The LDAP host.
     * @param port The LDAP port.
     * @param isSSL Whether SSL is enabled or not for this LDAP server, may be null.
     * @return An LDAPURL instance that represetns the provided parameters.
     */
    public static LDAPURL valueOf(String host, int port, Boolean isSSL) {
        return new LDAPURL(host, port, isSSL);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 19 * hash + this.port;
        hash = 19 * hash + (this.isSSL != null ? this.isSSL.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LDAPURL other = (LDAPURL) obj;
        if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if (this.isSSL != other.isSSL && (this.isSSL == null || !this.isSSL.equals(other.isSSL))) {
            return false;
        }
        return true;
    }

    /**
     * Returns a String representation of this LDAPURL instance. If the scheme has been provided it will be part of
     * the representation as well, otherwise just the simple host:port format will be returned.
     *
     * @return String representation of this LDAPURL instance.
     */
    @Override
    public String toString() {
        if (isSSL != null) {
            return (isSSL ? "ldaps" : "ldap") + COLON_SLASH_SLASH + host + SEPARATOR + port;
        } else {
            return host + SEPARATOR + port;
        }
    }

    @Override
    public int compareTo(LDAPURL o) {
        return toString().compareTo(o.toString());
    }
}
