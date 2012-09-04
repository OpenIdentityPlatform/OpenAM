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
 * $Id: EntitlementSubjectImpl.java,v 1.2 2009/10/29 19:05:18 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.security.Principal;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * EntitlementSubject to represent group identity for membership check.
 */
public abstract class EntitlementSubjectImpl implements SubjectImplementation {
    private String uuid;
    private String pSubjectName;
    private boolean exclusive;

    /**
     * Constructor
     */
    public EntitlementSubjectImpl() {
    }

    /**
     * Constructor
     *
     * @param uuid the universal ID of subject.
     */
    public EntitlementSubjectImpl(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Constructor.
     *
     * @param uuid is the universal Id of subject.
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when it was created from OpenSSO policy Subject
     */
    public EntitlementSubjectImpl(String uuid, String pSubjectName) {
        this.uuid = uuid;
        this.pSubjectName = pSubjectName;
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            uuid = jo.has("uuid") ? jo.optString("uuid") : null;
            pSubjectName = jo.has("pSubjectName") ?
                jo.optString("pSubjectName") : null;
            exclusive = jo.has("exclusive") ?
                Boolean.parseBoolean(jo.optString("exclusive")) : false;
        } catch (JSONException e) {
            PrivilegeManager.debug.error("EntitlementSubjectImpl.setState", e);
        }
    }

    /**
     * Returns state of the object.
     *
     * @return state of the object encoded as string.
     */
    public String getState() {
        return toString();
    }

    /**
     * Returns JSONObject mapping of the object.
     *
     * @return JSONObject mapping  of the object.
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("uuid", uuid);
        jo.put("pSubjectName", pSubjectName);
        if (exclusive) {
            jo.put("exclusive", exclusive);
        }
        return jo;
    }

    /**
     * Returns string representation of the object.
     *
     * @return string representation of the object.
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("EntitlementSubjectImpl.toString", e);
        }
        return s;
    }

    /**
     * Sets the Identifier.
     *
     * @param uuid Identifier.
     */
    public void setID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Returns the Identifier.
     * @return Identifier.
     */
    public String getID() {
        return uuid;
    }

    /**
     * Sets OpenSSO policy subject name of the object
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when GroupSubject was created from
     * OpenSSO policy Subject
     */
    public void setPSubjectName(String pSubjectName) {
        this.pSubjectName = pSubjectName;
    }

    /**
     * Returns OpenSSO policy subject name of the object
     * @return subject name as used in OpenSSO policy,
     * this is releavant only when GroupSubject was created from
     * OpenSSO policy Subject
     */
    public String getPSubjectName() {
        return pSubjectName;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        EntitlementSubjectImpl object = (EntitlementSubjectImpl) obj;
        if (uuid == null) {
            if (object.getID() != null) {
                return false;
            }
        } else {
            if (!uuid.equals(object.getID())) {
                return false;
            }
        }
        if (pSubjectName == null) {
            if (object.getPSubjectName() != null) {
                return false;
            }
        } else {
            if (!pSubjectName.equals(object.getPSubjectName())) {
                return false;
            }
        }
        return (exclusive == object.exclusive);
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = 0;
        if (uuid != null) {
            code += uuid.hashCode();
        }
        if (pSubjectName != null) {
            code += pSubjectName.hashCode();
        }
        if (exclusive) {
            code += Boolean.TRUE.hashCode();
        } else {
            code += Boolean.FALSE.hashCode();
        }
        return code;
    }

    protected boolean hasPrincipal(Subject subject, String uuid) {
        Set<Principal> userPrincipals = subject.getPrincipals();
        for (Principal p : userPrincipals) {
            if (p.getName().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> for exclusive.
     *
     * @return <code>true</code> for exclusive.
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Sets exclusive.
     *
     * @param flag <code>true</code> for exclusive.
     */
    public void setExclusive(boolean flag) {
        exclusive = flag;
    }
}
