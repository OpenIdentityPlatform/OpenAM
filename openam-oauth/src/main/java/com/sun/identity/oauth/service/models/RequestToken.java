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
 * $Id: RequestToken.java,v 1.2 2010/01/20 17:51:38 huacui Exp $
 *
 */

package com.sun.identity.oauth.service.models;

import com.sun.identity.oauth.service.PathDefs;
import com.sun.identity.oauth.service.util.OAuthProperties;
import java.util.Date;

/**
 * The OAuth request token 
 */
public class RequestToken {
    private String id;
    private String reqtUri;
    private String reqtVal;
    private String reqtSecret;
    private String reqtPpalid;
    private Date reqtLifetime;
    private Consumer consumerId;
    private String callback;
    private String verifier;
    private String etag;

    private static long lifeTime = 86400; // default life time in seconds

    static {
        String lifeTimeStr = OAuthProperties.get(PathDefs.REQUEST_TOKEN_LIFETIME);
        if (lifeTimeStr != null) {
            try {
                lifeTime = Long.parseLong(lifeTimeStr);
            } catch (NumberFormatException nfe) {
                lifeTime = 86400;
            }
        }
    }
    public RequestToken() {
        setReqtLifetime(new Date(System.currentTimeMillis() + lifeTime * 1000));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReqtUri() {
        return reqtUri;
    }

    public void setReqtUri(String reqtUri) {
        this.reqtUri = reqtUri;
    }

    public String getReqtVal() {
        return reqtVal;
    }

    public void setReqtVal(String reqtVal) {
        this.reqtVal = reqtVal;
    }

    public String getReqtSecret() {
        return reqtSecret;
    }

    public void setReqtSecret(String reqtSecret) {
        this.reqtSecret = reqtSecret;
    }

    public String getReqtPpalid() {
        return reqtPpalid;
    }

    public void setReqtPpalid(String reqtPpalid) {
        this.reqtPpalid = reqtPpalid;
    }

    public Date getReqtLifetime() {
        return reqtLifetime;
    }

    public void setReqtLifetime(Date reqtLifetime) {
        this.reqtLifetime = reqtLifetime;
    }

    public Consumer getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(Consumer consumerId) {
        this.consumerId = consumerId;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
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
        if (!(object instanceof RequestToken)) {
            return false;
        }
        RequestToken other = (RequestToken) object;
        if ((this.id == null && other.id != null) || 
            (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.sun.identity.oauth.service.models.RequestToken[id=" + id + "]";
    }

}
