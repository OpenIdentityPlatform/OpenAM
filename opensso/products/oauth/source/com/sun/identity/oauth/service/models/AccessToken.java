/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccessToken.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service.models;

import com.sun.identity.oauth.service.PathDefs;
import com.sun.identity.oauth.service.util.OAuthProperties;
import java.util.Date;

/**
 * The OAuth Access Token
 */
public class AccessToken {
    private String id;
    private String acctUri;
    private String acctVal;
    private String acctSecret;
    private String acctPpalid;
    private Date acctLifetime;
    private Short acctOnetime;
    private Consumer consumerId;
    private String etag;

    private static long lifeTime = 86400; // default life time in seconds

    static {
        String lifeTimeStr = OAuthProperties.get(PathDefs.ACCESS_TOKEN_LIFETIME);
        if (lifeTimeStr != null) {
            try {
                lifeTime = Long.parseLong(lifeTimeStr);
            } catch (NumberFormatException nfe) {
                lifeTime = 86400;
            }
        }
    }

    public AccessToken() {
        setAcctLifetime(new Date(System.currentTimeMillis() + lifeTime * 1000));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAcctUri() {
        return acctUri;
    }

    public void setAcctUri(String acctUri) {
        this.acctUri = acctUri;
    }

    public String getAcctVal() {
        return acctVal;
    }

    public void setAcctVal(String acctVal) {
        this.acctVal = acctVal;
    }

    public String getAcctSecret() {
        return acctSecret;
    }

    public void setAcctSecret(String acctSecret) {
        this.acctSecret = acctSecret;
    }

    public String getAcctPpalid() {
        return acctPpalid;
    }

    public void setAcctPpalid(String acctPpalid) {
        this.acctPpalid = acctPpalid;
    }

    public Date getAcctLifetime() {
        return acctLifetime;
    }

    public void setAcctLifetime(Date acctLifetime) {
        this.acctLifetime = acctLifetime;
    }

    public Short getAcctOnetime() {
        return acctOnetime;
    }

    public void setAcctOnetime(Short acctOnetime) {
        this.acctOnetime = acctOnetime;
    }

    public Consumer getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(Consumer consumerId) {
        this.consumerId = consumerId;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AccessToken)) {
            return false;
        }
        AccessToken other = (AccessToken) object;
        if ((this.id == null && other.id != null) || 
            (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.sun.identity.oauth.service.models.AccessToken[id=" + id + "]";
    }

}
