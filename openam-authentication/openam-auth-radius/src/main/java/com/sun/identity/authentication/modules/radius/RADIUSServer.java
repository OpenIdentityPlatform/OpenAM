/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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

package com.sun.identity.authentication.modules.radius;

/**
 * Represents a remote radius server to which to delegate authentication for the authentication module.
 */
public class RADIUSServer {

    /**
     * The remote radius host.
     */
    private String host;
    /**
     * The remote radius port.
     */
    private int port;

    /**
     * Constructs an instance.
     * @param host the remote host.
     * @param port the remote port.
     */
    public RADIUSServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Returns the host.
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port.
     * @return the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Answers true if the passed in object has the same class and identical values for host and port.
     *
     * @param obj
     *            an instance of RADIUSServer.
     * @return true if the passed in object has the same class and identical values for host and port.
     */
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

    /**
     * The hashcode of this instance.
     * @return the hashcode.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 29 * hash + this.port;
        return hash;
    }

    /**
     * Generated a string representation of this instance.
     *
     * @return the string representation.
     */
    @Override
    public String toString() {
        return "RADIUS server: " + host + ":" + port;
    }
}
