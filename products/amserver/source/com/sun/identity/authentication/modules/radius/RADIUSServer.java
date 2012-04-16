/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
 *
 */
package com.sun.identity.authentication.modules.radius;

/**
 *
 * @author Peter Major
 */
public class RADIUSServer {

    private String host;
    private int port;

    public RADIUSServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RADIUSServer)) {
            return false;
        }
        final RADIUSServer other = (RADIUSServer) obj;
        //if they aren't both null, or they aren't equals
        if ((this.host == null) ? (other.host != null) : !this.host.equals(other.host)) {
            return false;
        }
        return (this.port == other.port);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 29 * hash + this.port;
        return hash;
    }

    @Override
    public String toString() {
        return "RADIUS server: " + host + ":" + port;
    }
}
