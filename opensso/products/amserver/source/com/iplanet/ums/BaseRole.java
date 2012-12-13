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
 * $Id: BaseRole.java,v 1.4 2008/06/25 05:41:44 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import java.util.HashSet;
import java.util.Iterator;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.ldap.aci.ACI;
import com.iplanet.services.ldap.aci.ACIParseException;
import com.iplanet.services.ldap.aci.QualifiedCollection;

/**
 * Abstract base class for all roles.
 *
 * @supported.api
 */
public abstract class BaseRole extends PersistentObject implements IRole {
    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    /**
     * Default constructor
     * 
     * @see com.iplanet.ums.PersistentObject#PersistentObject()
     */
    protected BaseRole() {
        super();
    }

    /**
     * Constructs a BaseRole object from a principal and guid.
     * 
     * @see com.iplanet.ums.PersistentObject#PersistentObject(
     *      java.security.Principal
     *      p, String guid)
     */
    BaseRole(java.security.Principal p, String guid) throws UMSException {
        super();
    }

    /**
     * Constructs a BaseRole from a creation template
     * and attribute set.
     * 
     * @see com.iplanet.ums.PersistentObject#PersistentObject(CreationTemplate
     *      template, AttrSet attrSet)
     *
     * @supported.api
     */
    public BaseRole(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Checks if a given identifier is a member of the role.
     * 
     * @param po
     *            member to be checked for membership
     * 
     * @return <code>true</code> if it is a member
     * @exception UMSException
     *                on failure to read object for guid
     *
     * @supported.api
     */
    public boolean hasMember(PersistentObject po) throws UMSException {
        boolean hasTheMember = false;
        Attr attr = po.getAttribute(COMPUTED_MEMBER_ATTR_NAME);
        if (attr != null) {
            if (attr.contains(getGuid().getDn())
                    || attr.contains(getGuid().getDn().toLowerCase())) {
                hasTheMember = true;
            }
        }
        return hasTheMember;
    }

    /**
     * Returns the attribute access rights associated with the role.
     * 
     * @return AccessRightObject associated with the role
     *
     * @supported.api
     */
    public AccessRightObject getAccessRight() throws UMSException,
            ACIParseException {

        QualifiedCollection readPerm = null;
        QualifiedCollection writePerm = null;

        // get parent GUID
        if (parentObject == null) {
            parentObject = getParentObject();
        }
        // get ACIS from parent object
        Iterator acis = parentObject.getACI().iterator();
        // go throw each ACI to see if it sets the access right for the role
        if (acis != null) {
            String guid = getGuid().getDn().trim();
            while (acis.hasNext()) {
                ACI aci = (ACI) acis.next();
                if (debug.messageEnabled()) {
                    debug.message("BaseRole.getAccessRight ACI.toString ="
                            + aci.toString());
                }
                // try to find out if this ACI is for this role
                // checking the name of the aci,
                // better solution is to check the roledn, TBD
                String aciName = aci.getName();
                if (aciName.equalsIgnoreCase(READ_PERM_HEADER + guid)) {
                    readPerm = aci.getTargetAttributes();
                    if (writePerm != null)
                        break;
                    else
                        continue;
                }
                if (aciName.equalsIgnoreCase(WRITE_PERM_HEADER + guid)) {
                    writePerm = aci.getTargetAttributes();
                    if (readPerm != null)
                        break;
                    else
                        continue;
                }
            }
        }

        if (readPerm == null) {
            if (writePerm == null) {
                return new AccessRightObject(null, null);
            } else {
                return new AccessRightObject(null, writePerm.getCollection());
            }
        } else {
            if (writePerm == null) {
                return new AccessRightObject(readPerm.getCollection(), null);
            } else {
                return new AccessRightObject(readPerm.getCollection(),
                        writePerm.getCollection());
            }
        }
    }

    /**
     * Creates attribute access rights for the role;
     * existing attribute access rights for the role will be replaced.
     * 
     * @param accessRight
     *            New access right to be set to the role
     *
     * @supported.api
     */
    public void newAccessRight(AccessRightObject accessRight)
            throws UMSException, ACIParseException {

        ACI readACI = null;
        ACI writeACI = null;

        // get parent GUID
        if (parentObject == null) {
            parentObject = getParentObject();
        }

        // get ACIS from parent object
        Iterator acis = parentObject.getACI().iterator();
        // go throw each ACI to see if it sets the access right for the role
        if (acis != null) {
            String guid = getGuid().getDn().trim();
            while (acis.hasNext()) {
                ACI aci = (ACI) acis.next();
                if (debug.messageEnabled()) {
                    debug.message("BaseRole.newAccessRight ACI.toString ="
                            + aci.toString());
                }
                // try to find out if this ACI is for this role
                // checking the name of the aci,
                // better solution is to check the roledn, TBD
                String aciName = aci.getName();
                if (aciName.equals(READ_PERM_HEADER + guid)) {
                    readACI = aci;
                    if (writeACI != null)
                        break;
                    else
                        continue;
                }
                if (aciName.equals(WRITE_PERM_HEADER + guid)) {
                    writeACI = aci;
                    if (readACI != null)
                        break;
                    else
                        continue;
                }
            }
        }

        if (readACI != null) {
            debug.message("modify existing read aci");
            // modify existing read ACI
            Attr attr = new Attr(ACI.ACI, readACI.getACIText());
            if (debug.messageEnabled()) {
                debug.message("readaci.ACIText :" + readACI.getACIText());
            }
            parentObject.modify(attr, ModSet.DELETE);
            ACI newReadACI = ACI.valueOf(readACI.toString());
            QualifiedCollection readAttrs = new QualifiedCollection(accessRight
                    .getReadableAttributeNames(), false);
            newReadACI.setTargetAttributes(readAttrs);
            attr = new Attr(ACI.ACI, newReadACI.toString());
            parentObject.modify(attr, ModSet.ADD);
        } else {
            debug.message("new read aci");
            // add new read ACI
            ACI newReadACI = new ACI(READ_PERM_HEADER + getGuid().getDn());
            newReadACI.setName(READ_PERM_HEADER + getGuid().getDn());
            QualifiedCollection readAttrs = new QualifiedCollection(accessRight
                    .getReadableAttributeNames(), false);
            newReadACI.setTargetAttributes(readAttrs);

            // set Allow "read" permission
            HashSet hs = new HashSet();
            hs.add(READ_PERM_STRING);
            QualifiedCollection perm = new QualifiedCollection(hs, false);
            newReadACI.setPermissions(perm);

            // set applied role
            hs = new HashSet();
            hs.add(getGuid().getDn());
            newReadACI.setRoles(hs);
            Attr attr = new Attr(ACI.ACI, newReadACI.toString());
            if (debug.messageEnabled()) {
                debug.message("READ " + getGuid().getDn() + "="
                        + newReadACI.toString());
            }
            parentObject.modify(attr, ModSet.ADD);
        }

        if (writeACI != null) {
            debug.message("modify existing write aci");
            // modify existing read ACI
            Attr attr = new Attr(ACI.ACI, writeACI.getACIText());
            if (debug.messageEnabled()) {
                debug.message("writeaci.ACIText :" + writeACI.getACIText());
            }
            parentObject.modify(attr, ModSet.DELETE);
            ACI newWriteACI = ACI.valueOf(writeACI.toString());
            QualifiedCollection qual = new QualifiedCollection(accessRight
                    .getWritableAttributeNames(), false);
            newWriteACI.setTargetAttributes(qual);
            attr = new Attr(ACI.ACI, newWriteACI.toString());
            parentObject.modify(attr, ModSet.ADD);
        } else {
            debug.message("new write aci");
            // add new write ACI
            ACI newWriteACI = new ACI(WRITE_PERM_HEADER + getGuid().getDn());
            newWriteACI.setName(WRITE_PERM_HEADER + getGuid().getDn());
            QualifiedCollection writeAttrs = new QualifiedCollection(
                    accessRight.getWritableAttributeNames(), false);
            newWriteACI.setTargetAttributes(writeAttrs);

            // set Allow "write" permission
            HashSet hs = new HashSet();
            hs.add(WRITE_PERM_STRING);
            QualifiedCollection perm = new QualifiedCollection(hs, false);
            newWriteACI.setPermissions(perm);

            // set applied role
            hs = new HashSet();
            hs.add(getGuid().getDn());
            newWriteACI.setRoles(hs);
            Attr attr = new Attr(ACI.ACI, newWriteACI.toString());
            if (debug.messageEnabled()) {
                debug.message("Write " + getGuid().getDn() + "="
                        + newWriteACI.toString());
            }
            parentObject.modify(attr, ModSet.ADD);
        }

        // save ACI changes to parent persistent store
        parentObject.save();
    }

    // need to set cosattribute to be "operational" to avoid adding objectclass
    // to every user entry, but need to get response back from DS team (TBD)
    // but for now, just set cosattribute operational
    private PersistentObject parentObject = null;

    private static final String READ_PERM_STRING = "read";

    private static final String READ_PERM_HEADER = "Read permission for ";

    private static final String WRITE_PERM_STRING = "write";

    private static final String WRITE_PERM_HEADER = "Write permission for ";
}
