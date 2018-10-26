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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.setup;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Configuration Store Properties
 * This gets serialized to/from json as part of boot.json
 *
 * @since 14.0
 */
public class ConfigStoreProperties {
    private String baseDN = "dc=openam,dc=openidentityplatform,dc=org";
    private String dirManagerDN = "dc= Directory Manager";
    private String ldapHost = "localhost";
    private int ldapPort = 50389;
    private String ldapProtocol = "ldap";
    // Don' serialize password - it must be saved in the keystore
    @JsonIgnore
    private String dirManagerPassword;

    public String getDirManagerPassword() {
        return dirManagerPassword;
    }

    public void setDirManagerPassword(String dirManagerPassword) {
        this.dirManagerPassword = dirManagerPassword;
    }


    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String getDirManagerDN() {
        return dirManagerDN;
    }

    public void setDirManagerDN(String dirManagerDN) {
        this.dirManagerDN = dirManagerDN;
    }

    public String getLdapHost() {
        return ldapHost;
    }

    public void setLdapHost(String ldapHost) {
        this.ldapHost = ldapHost;
    }

    public int getLdapPort() {
        return ldapPort;
    }

    public void setLdapPort(int ldapPort) {
        this.ldapPort = ldapPort;
    }

    public String getLdapProtocol() {
        return ldapProtocol;
    }

    public void setLdapProtocol(String ldapProtocol) {
        this.ldapProtocol = ldapProtocol;
    }
}
