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
 * $Id: UserStore.java,v 1.3 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

/**
 * @author Jeffrey Bermudez
 */
public class UserStore {

    private String userStoreName;
    private Boolean storeName;
    private String directory;
    private Boolean directoryLocated;
    private Integer port;
    private String securePort;
    private String login;
    private String password;
    private String baseDN;


    public String getUserStoreName() {
        return userStoreName;
    }

    public void setUserStoreName(String userStoreName) {
        this.userStoreName = userStoreName;
    }

    public Boolean getStoreName() {
        return storeName;
    }

    public void setStoreName(Boolean storeName) {
        this.storeName = storeName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Boolean getDirectoryLocated() {
        return directoryLocated;
    }

    public void setDirectoryLocated(Boolean directoryLocated) {
        this.directoryLocated = directoryLocated;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSecurePort() {
        return securePort;
    }

    public void setSecurePort(String securePort) {
        this.securePort = securePort;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }
    
}
