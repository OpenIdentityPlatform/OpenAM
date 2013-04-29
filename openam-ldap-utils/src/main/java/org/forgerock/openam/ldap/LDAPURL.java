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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.ldap;

/**
 * A simple domain object to represent a host:port combination.
 *
 * @author Peter Major
 */
public final class LDAPURL {

    private static final int DEFAULT_PORT = 389;
    private static final char SEPARATOR = ':';
    private final String url;
    private final int port;

    public LDAPURL(String urlAndPort) {
        int idx = urlAndPort.indexOf(SEPARATOR);
        int portFromUrl;
        if (idx != -1) {
            url = urlAndPort.substring(0, idx);
            try {
                portFromUrl = Integer.parseInt(urlAndPort.substring(idx + 1));
            } catch (NumberFormatException nfe) {
                portFromUrl = DEFAULT_PORT;
            }
        } else {
            url = urlAndPort;
            portFromUrl = DEFAULT_PORT;
        }
        if (portFromUrl < 1 || portFromUrl > 65535) {
            portFromUrl = DEFAULT_PORT;
        }
        port = portFromUrl;
    }

    public LDAPURL(String url, int port) {
        this.url = url;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 71 * hash + this.port;
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
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return url + SEPARATOR + port;
    }
}
