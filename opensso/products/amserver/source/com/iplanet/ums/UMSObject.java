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
 * $Id: UMSObject.java,v 1.5 2008/06/25 05:41:46 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * UMSObject class exposes public methods that serve as the entry points to the
 * UMS SDK. This class is used to replace the public static methods that are
 * previously available in Session class.
 * <p>
 * 
 * <pre>
 *       // Previous access class with authenticated context
 *       //
 *       UMSObject.getObject( Context ctx, String guid );
 *       UMSObject.removeObject( Context ctx, String guid );
 *       UMSObject.logout( Context ctx );
 * 
 *       // New API with the use of Principal interface that represents an
 *       // authenticated principal
 *       UMSObject.getObject( java.security.Principal principal, String guid );
 *       UMSObject.removeObject(java.security.Principal principal, String guid);
 * </pre>
 *
 * @supported.api
 */
public class UMSObject {

    public static final String UMS = "UMS";

    public static final String ENTITY = "ENTITY";

    public static final String UTIL = "UTIL";

    private static Debug debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Returns the persistent object of correct subclass, for the given id.
     * The Java class to construct the persistent object is inferred from the
     * default creation templates registered with the Template manager.
     * 
     * @param token Authenticated principal's single sign on token.
     * @param guid GUID identification of the object to get.
     * @return the object read, all non operational attributes are read.
     * @throws UMSException if there is an error while instantiating
     *         the right type of object. In addition, it propagates any
     *         exception from the datalayer.
     * @supported.api
     */
    static public PersistentObject getObject(SSOToken token, Guid guid)
            throws UMSException {
        return getObject(token, guid, null);
    }

    /**
     * Returns the persistent object of correct subclass, for the given ID. The
     * Java class to construct the persistent object is inferred from the
     * default creation templates registered with the Template manager.
     * 
     * @param token Authenticated principal's single sign on token.
     * @param guid GUID identification of the object to get
     * @param attrNames attribute names to read.
     * @return the object read.
     * @throws UMSException if there is an error while instantiating
     *         the right type of object. In addition, it propagates any
     *         exception from the datalayer.
     * @supported.api
     */
    static public PersistentObject getObject(
        SSOToken token,
        Guid guid,
        String[] attrNames
    ) throws UMSException {
        Principal principal = null;
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        try {
            principal = token.getPrincipal();
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.BAD_TOKEN_HDL),
                    se);
        }
        return getObject(principal, guid, attrNames);
    }

    /**
     * Removes an object identified by the given ID.
     * 
     * @param token Authenticated principal's single sign on token.
     * @param guid GUID identification of the object to be removed.
     * @throws EntryNotFoundException if the entry is not found.
     * @throws UMSException if there is an error while removing the object from
     *         persistence store
     * @supported.api
     */
    static public void removeObject(SSOToken token, Guid guid)
            throws EntryNotFoundException, UMSException {
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        try {
            DataLayer.getInstance().deleteEntry(token.getPrincipal(), guid);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.BAD_TOKEN_HDL),
                    se);
        }
    }

    public static PersistentObject getObject(Principal principal, Guid guid)
            throws UMSException {
        return getObject(principal, guid, null);
    }

    public static PersistentObject getObject(Principal principal, Guid guid,
            String[] attrNames) throws UMSException {
        AttrSet attrSet = null;
        if (attrNames == null) {
            attrSet = DataLayer.getInstance().read(principal, guid);
        } else {
            int length = attrNames.length;
            String[] attrNames1 = new String[length + 1];
            System.arraycopy(attrNames, 0, attrNames1, 0, length);
            attrNames1[length] = "objectclass";
            attrSet = DataLayer.getInstance().read(principal, guid, attrNames1);
        }
        String id = guid.getDn();
        if (id == null) {
            String msg = i18n.getString(IUMSConstants.BAD_ID);
            throw new IllegalArgumentException(msg);
        }
        Class javaClass = TemplateManager.getTemplateManager()
                .getJavaClassForEntry(id, attrSet);
        PersistentObject po = null;
        try {
            po = (PersistentObject) javaClass.newInstance();
        } catch (Exception e) {
            String args[] = new String[1];
            args[0] = e.toString();
            String msg = i18n
                    .getString(IUMSConstants.NEW_INSTANCE_FAILED, args);
            throw new UMSException(msg);
        }
        po.setAttrSet(attrSet);
        po.setGuid(guid);
        po.setPrincipal(principal);
        return po;
    }

    /**
     * Return a PersistentObject given an authenticated token and guid. The
     * validity of the returned PersistentObject can not be guaranteed since the
     * object is created in memory, not instantiated from the persistent
     * storage. Using the PersistentObject returned from this method may result
     * exceptions in the later part of the application if the given guid is not
     * valid or represents an entry that does not exist.
     * 
     * @param token
     *            Valid and authenticated token
     * @param guid
     *            Globally unique identifier for the entity
     * @return the PersistentObject created in memory
     * @throws UMSException
     *             for failure to create the object
     * 
     * @supported.api
     */
    public static PersistentObject getObjectHandle(SSOToken token, Guid guid)
            throws UMSException {

        String dn = guid.getDn();
        if (token == null || dn == null) {
            String msg;
            if (token == null) {
                msg = i18n.getString(IUMSConstants.NULL_TOKEN);
                debug.error("UMSObject.PersistentObject: token is null");
            } else {
                msg = i18n.getString(IUMSConstants.BAD_GUID);
                debug.error("UMSObject.PersistentObject: dn is null");
            }

            throw new UMSException(msg);
        }

        Principal principal = null;
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        try {
            principal = token.getPrincipal();
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.BAD_TOKEN_HDL),
                    se);
        }

        PersistentObject po = new PersistentObject();
        po.setGuid(guid);
        po.setPrincipal(principal);
        return po;
    }

}
