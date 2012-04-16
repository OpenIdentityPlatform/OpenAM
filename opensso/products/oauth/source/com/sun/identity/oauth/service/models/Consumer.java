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
 * $Id: Consumer.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service.models;

import java.util.Collection;

/**
 * The OAuth Consumer
 */
public class Consumer {
    private String id;
    private String consName;
    private String consSecret;
    private String consRsakey;
    private String consKey;
    private Collection<RequestToken> requestTokenCollection;
    private Collection<AccessToken> accessTokenCollection;
    private String etag;

    public Consumer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConsName() {
        return consName;
    }

    public void setConsName(String consName) {
        this.consName = consName;
    }

    public String getConsSecret() {
        return consSecret;
    }

    public void setConsSecret(String consSecret) {
        this.consSecret = consSecret;
    }

    public String getConsRsakey() {
        return consRsakey;
    }

    public void setConsRsakey(String consRsakey) {
        this.consRsakey = consRsakey;
    }

    public String getConsKey() {
        return consKey;
    }

    public void setConsKey(String consKey) {
        this.consKey = consKey;
    }

    public Collection<RequestToken> getRequestTokenCollection() {
        return requestTokenCollection;
    }

    public void setRequestTokenCollection(Collection<RequestToken> requestTokenCollection) {
        this.requestTokenCollection = requestTokenCollection;
    }

    public Collection<AccessToken> getAccessTokenCollection() {
        return accessTokenCollection;
    }

    public void setAccessTokenCollection(Collection<AccessToken> accessTokenCollection) {
        this.accessTokenCollection = accessTokenCollection;
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
        if (!(object instanceof Consumer)) {
            return false;
        }
        Consumer other = (Consumer) object;
        if ((this.id == null && other.id != null) || 
            (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.sun.identity.oauth.service.models.Consumer[id=" + id + "]";
    }

}
