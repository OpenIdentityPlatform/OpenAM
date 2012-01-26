/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openam.authentication.service.protocol;

import com.sun.identity.shared.debug.Debug;

import java.io.Serializable;
import java.util.Enumeration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;


/**
 * Facade for the standard servlet HttpSession object.
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */

public class RemoteSession implements HttpSession, Serializable {
    public static final long serialVersionUID = 42L;

    /**
     * Wrapped session object.
     */
    private transient HttpSession session = null;
    private long creationTime = -1;
    private String id = null;
    private long lastAccessedTime = -1;
    private int maxInactiveInterval = -1;
    private boolean isNew = false;
    private Map internalAttributes = new HashMap();
    private Set internalAttributeNames = new HashSet();
    private transient Debug debug = null;

    private final static String SERIALIZABLE_INT = "java.io.Serializable";
    private final static String COLLECTION_INT = "java.util.Collection";
    
    /**
     * Construct a new session facade. This class wraps the standard HttpSession
     * object allowing it to become serializable.
     *
     * @param session The HttpSession object to be wrapped
     */
    public RemoteSession(HttpSession session) {
        super();
        debug = Debug.getInstance("remoteSession");
        this.session = (HttpSession) session;
        
        creationTime = session.getCreationTime();
        id = session.getId();
        lastAccessedTime = session.getLastAccessedTime();
        maxInactiveInterval = session.getMaxInactiveInterval();
        isNew = session.isNew();
        internalAttributes = new HashMap();
        
        // iterate over the attribute storing those that can be serialized
        Enumeration aNames = getAttributeNames();
        
        while (aNames.hasMoreElements()) {
            String attributeName = (String) aNames.nextElement();
            
            if (isSerializable(getAttribute(attributeName)) && 
                    !attributeName.equals("LoginCallbacks") &&
                    !attributeName.equals("AuthContext")) {
                internalAttributes.put(attributeName, getAttribute(attributeName));
                internalAttributeNames.add(attributeName);
                debug.message("adding attr=" + attributeName + ", " + getAttribute(attributeName));
            }
        }
    }

    /**
     * Construct a new session facade. Used on the remote end post deserialization
     */
    public RemoteSession() {
        debug = Debug.getInstance("remoteSession");
    }

    /**
     * The creation time of the session, either from local object pre serialization
     * or the stored value post serialization.
     *
     * @return The session creation time
     */
    public long getCreationTime() {
        return (session != null) ? session.getCreationTime() : creationTime;
    }

    /**
     * Returns the ID of the session, either from local object pre serialization
     * or the stored value post serialization.
     *
     * @return The session id
     */
    public String getId() {
        return (session != null) ? session.getId(): id;
    }

    /**
     * Returns the last accessed time of the session, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @return The last session accessed time
     */
    public long getLastAccessedTime() {
        return (session != null) ? session.getLastAccessedTime() : 
            lastAccessedTime;
    }

    /**
     * Returns the Servlet Context, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @return The servlet context
     */
    public ServletContext getServletContext() {
        return (session != null) ? session.getServletContext() : null;
    }

    /**
     * Sets the maximum inactive session interval on the session, only works
     * pre serialization
     *
     * @param interval The maximum inactivate interval
     */
    public void setMaxInactiveInterval(int interval) {
        if (session != null)
            session.setMaxInactiveInterval(interval);
    }


    /**
     * Gets the maximum inactive session interval, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @return The maximum session inactive interval
     */
    public int getMaxInactiveInterval() {
        return (session != null) ? session.getMaxInactiveInterval() : 
            maxInactiveInterval;
    }

    /**
     * Returns the session context, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @deprecated
     * @return The session context
     */
    public HttpSessionContext getSessionContext() {
        return (session != null) ? session.getSessionContext() : null;
    }

    /**
     * Fetch the value of a given session attribute, either from local object
     * pre serialization or the stored value post serialization.
     *
     * Only attributes that were serializable will be available post serialization
     *
     * @param name The name of the attribute to return
     * @return The value of the named attribute or null if not found or non-serializable
     */
    public Object getAttribute(String name) {
        return (session != null) ? session.getAttribute(name) :
            internalAttributes.get(name);
    }

    /**
     * Same functionality as @see getAttribute.
     *
     * @deprecated
     * @param name
     * @return The value of the named attribute or null if not found or non-serializable
     */
    public Object getValue(String name) {
        return (session != null) ? session.getAttribute(name) :
            internalAttributes.get(name);
    }

    /**
     * Returns a list of attribute names, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @return The attributes names
     */
    public Enumeration getAttributeNames() {
        return (session != null) ? session.getAttributeNames() :
            new Vector(internalAttributeNames).elements();
    }

    /**
     * Returns a list of value names, @see getAttributeNames
     *
     * @deprecated
     * @return The value names
     */
    public String[] getValueNames() {
        return (session != null) ? session.getValueNames() :
             (String[]) internalAttributeNames.toArray(new String[1]);
    }

    /**
     * Sets an attribute on the session, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @param name The name of the attribute to set
     * @param value The attributes value
     */
    public void setAttribute(String name, Object value) {
        if (session != null) {
            session.setAttribute(name, value);
            internalAttributes.put(name, value);
        } else {
            internalAttributes.put(name, value);
        }
    }

    /**
     * see @setAttribute
     *
     * @deprecated
     * @param name The name of the value to set
     * @param value The value of the value being set
     */
    public void putValue(String name, Object value) {
        if (session != null) {
            session.setAttribute(name, value);
            internalAttributes.put(name, value);
        } else {
            internalAttributes.put(name, value);
        }
    }

    /**
     * Removes an attribute from the session, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @param name The name of the attribute to remove
     */
    public void removeAttribute(String name) {
        if (session != null) {
            session.removeAttribute(name);
            internalAttributes.remove(name);
        } else {
            internalAttributes.remove(name);
        }
    }

    /**
     * @see #removeAttribute
     *
     * @deprecated
     * @param name The name of the value to remove
     */
    public void removeValue(String name) {
        if (session != null) {
            session.removeAttribute(name);
            internalAttributes.remove(name);
        } else {
            internalAttributes.remove(name);
        }
    }

    /**
     * Invalidates this session, only works pre-serialization
     */
    public void invalidate() {
        if (session != null)
            session.invalidate();
    }

    /**
     * Determines if the client knows about the session, either from local object
     * pre serialization or the stored value post serialization.
     *
     * @return true if unknown to the client
     */
    public boolean isNew() {
        return (session != null) ? session.isNew() : isNew;
    }

    /**
     * Tests if an object implements the java.io.Serializable interface.
     * 
     * @param obj The obj to test for serialization.
     * @return true if the object implements Serializable, false otherwise.
     */
    protected boolean isSerializable(Object obj) {
        boolean serializable = true;
        Class[] interfaces = obj.getClass().getInterfaces();
        
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].getName().equals(SERIALIZABLE_INT)) {
                serializable &= true;
            } else if (interfaces[i].getName().equals(COLLECTION_INT)) {
                serializable &= false;
            }
         }
        
        return serializable;
    }
}
