/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Request.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.services.ldap.event;

import com.sun.identity.shared.ldap.LDAPConnection;

import com.iplanet.sso.SSOToken;

class Request {

    // ldap message id
    private int _id;

    // ID returned to user.
    private String _reqID;

    // The authenticated id of the requester.
    private SSOToken _requester;

    // Search Root
    private String _baseDn;

    // Search scope
    private int _scope;

    // Search filter
    private String _filter;

    // Search attributes
    private String[] _attrs;

    // Search constraints
    private int _operations;

    // The event listener
    private IDSEventListener _listener;

    private LDAPConnection _connection;

    private long _lastUpdatedTime;

    /**
     * Request object constructor (package private)
     */
    Request(int id, String reqID, SSOToken requester, String baseDn, int scope,
            String filter, String[] attrs, int operations,
            IDSEventListener listener, LDAPConnection connection,
            long lastResponseTime) {
        _id = id;
        _reqID = reqID;
        _requester = requester;
        _baseDn = baseDn;
        _scope = scope;
        _filter = filter;
        _attrs = attrs;
        _operations = operations;
        _listener = listener;
        _connection = connection;
        _lastUpdatedTime = lastResponseTime;
    }

    /**
     * 
     */
    int getId() {
        return _id;
    }

    /**
     * 
     */
    String getRequestID() {
        return _reqID;
    }

    /**
     * 
     */
    SSOToken getRequester() {
        return _requester;
    }

    /**
     * 
     */
    String getBaseDn() {
        return _baseDn;
    }

    /**
     * 
     */
    int getScope() {
        return _scope;
    }

    /**
     * 
     */
    String getFilter() {
        return _filter;
    }

    /**
     * 
     */
    String[] getattrs() {
        return _attrs;
    }

    /**
     * 
     */
    int getOperations() {
        return _operations;
    }

    /**
     * Add Listsner
     */
    synchronized IDSEventListener getListener() {
        return _listener;
    }

    protected LDAPConnection getLDAPConnection() {
        return _connection;
    }

    protected long getLastUpdatedTime() {
        return _lastUpdatedTime;
    }

    protected void setLastUpdatedTime(long time) {
        _lastUpdatedTime = time;
    }

    /*
     * 
     */
    public String toString() {
        String str = "[EventEntry] base=" + _baseDn + " scope=" + _scope
                + " filter=" + _filter + " attrs={";
        for (int i = 0; i < _attrs.length; i++) {
            if (i > 0) {
                str += " ";
            }
            str += _attrs[i];
        }
        str += "} operations=" + _operations;
        str += " listener=" + _listener.toString();
        str += " id=" + _id + " last updated time: " + _lastUpdatedTime;
        return str;
    }

    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (this._listener != null ?
            this._listener.getClass().getName().hashCode() : 0);
        return hash;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof Request) {
            // Check the class nane
            Request r = (Request) obj;
            if (_listener.getClass().getName().equals(
                r._listener.getClass().getName())) {
                return (true);
            }
        }
        return (false);
    }
}
