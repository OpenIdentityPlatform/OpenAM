/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ISPermissionCollection.java,v 1.3 2008/06/25 05:43:50 qcheng Exp $
 *
 */
package com.sun.identity.policy.jaas;

import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.shared.debug.Debug;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class represents a collection of <code>ISPermission</code> objects.
 * It extends from <code>PermissionCollection</code> providing implementation 
 * of abstract methods like <code>add</code>, <code>implies</code, <code>
 * elements</code>. The rest of the required methods are used from the parent
 * class.
 *
 * <p>With any PermissionCollection, you can:
 * <UL>
 * <LI> add a permission to the collection using the <code>add</code> method.
 * <LI> check to see if a particular permission is implied in the
 *      collection, using the <code>implies</code> method.
 * <LI> enumerate all the permissions, using the <code>elements</code> method.
 * </UL>
 * <P>
 *
 * <p>When it is desirable to group together a number of Permission objects of 
 * the same type, the <code>newPermissionCollection</code> method on that 
 * particular type of Permission object should first be called. The default 
 * behavior of the ISPermission class is to simply return  a new instance of
 * <code>ISPermissionCollection()</code>.
 * The caller of <code>newPermissionCollection</code> would need to store 
 * permissions of the ISPermission in ISPermissionCollection returned.
 *
 *<p>The ISPermissionCollection returned by the
 * <code>Permission.newPermissionCollection</code>
 * method is a homogeneous collection, which stores only ISPermission objects.
 *
 * @see java.security.Permission
 * @see java.security.PermissionCollection
 * @see java.security.Permissions
 * <p>
 *
 */
public class ISPermissionCollection extends PermissionCollection {

    static Debug debug = Debug.getInstance("amPolicy");

    private Hashtable perms = new Hashtable(11);

    /**
     * Adds a permission object to the current collection of 
     * ISPermission objects.
     *
     * @param perm the ISPermission object to add.
     *
     * @exception SecurityException if this PermissionCollection object
     *            has been marked readonly
     * exception IllegalArgumentException if the passed permission is not of
     *           ISPermission instance.
     *
     */
    public void add(Permission perm) {
        if (! (perm instanceof ISPermission)) {
            String objs[] = { perm.toString() };
            throw (new IllegalArgumentException(
                    ResBundleUtils.getString(
                    "invalid_permission", objs)));
        }
        if (isReadOnly()) {
            throw new SecurityException(ResBundleUtils.getString(
                "readonly_permission_collection"));
        }
        debug.message("ISPermissionCollection::add(perm) called");
        if (debug.messageEnabled()) {
            debug.message("ISPermissionCollection::perm:"+perm.toString());
        }
        perms.put(perm.getName(), perm);
    }

    /**
     * This method returns an Enumeration of permissions in this collection, 
     * which is ISPermission in our case. This method gets called internally 
     * by java security code, to get all the permissions stored in this 
     * permission collection.
     * @return Enumeration of all the ISPermissions held in this collection.
     */

    public Enumeration elements() {
        debug.message("ISPermissionCollection::calling elements....");        
        if (debug.messageEnabled()) {
            for (Enumeration e = perms.elements(); e.hasMoreElements();) {
                debug.message("ISPC::perms::"+e.nextElement().toString());
            }
        }
        return perms.elements();
    }

    /**
     * Checks to see if the specified permission is implied by
     * the collection of ISPermission objects held in this 
     * ISPermissionCollection.
     * This method takes in a permission and loops through all the 
     * permissions in its store and call their <code>implies></code> to 
     * evaluate the result.
     *
     * @param perm the Permission object to compare.
     *
     * @return true if "permission" is implied by the  permissions in
     *         the collection, false if not.
     */
    public boolean implies(Permission perm) {
        boolean allowed = false;
        debug.message("calling implies in ISPermissionCollection....");
        if (debug.messageEnabled()) {
            debug.message("ISPC::implies:: perm:"+perm.toString());
        }
        for (Enumeration e = perms.elements(); e.hasMoreElements(); ) {
            if (((Permission)e.nextElement()).implies(perm)) {
                allowed = true;
                break;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ISPermssionCollection:: returning: "+allowed);
        }
        return allowed;
    }
}
