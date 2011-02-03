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
 * $Id: InfocardIdRepoData.java,v 1.2 2009/09/15 13:27:13 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp;

import java.io.Serializable;
import java.security.Key;

/**
 * 
 * @author Patrick
 */
public class InfocardIdRepoData implements Serializable {

    private byte[] passwordArray = null;
    private String password = null;
    private String issuer = null;
    private String ppid = null;
    private int encPasswdLength = 0;
    private Key key = null;

    public InfocardIdRepoData() {
    }

    public String getIssuer() {
        return issuer;
    }

    public Key getKey() {
        return key;
    }

    public byte[] getPasswordArrray() {
        return passwordArray;
    }

    public String getPassword() {
        return password;
    }

    public String getPpid() {
        return ppid;
    }

    public int getEncPasswdLenght() {
        return encPasswdLength;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public void setPasswordArray(byte[] passwordArray) {
        this.passwordArray = passwordArray;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEncPasswdLength(int length) {
        this.encPasswdLength = length;
    }

    public void setPpid(String ppid) {
        this.ppid = ppid;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InfocardIdRepoData) {
            return super.equals(obj);
        } else {
            return false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}