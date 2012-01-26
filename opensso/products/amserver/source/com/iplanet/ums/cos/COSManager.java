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
 * $Id: COSManager.java,v 1.5 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

package com.iplanet.ums.cos;

import java.security.Principal;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.Guid;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.SchemaManager;
import com.iplanet.ums.SearchResults;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.UMSObject;

/**
 * This class has the responsibility of adding, removing and replacing COS
 * definitions. It also provides search capabilities for COS definitions.
 * @supported.api
 */
public class COSManager {

    /**
     * This constructor sets the parent Directory entry which identifies the
     * location of COS definitions which will be managed. It also gets an
     * instance of a SchemaManager which will be used to update schema entries
     * for COS assignments.
     * 
     * @param token Authenticated principal's single sign on token.
     * @param guid The unique identifier specifying where COS definitions will
     *        be managed.
     * @throws UMSException if the token authentication fails, or if
     *         the guid for the parent entry is not valid.
     */
    protected COSManager(SSOToken token, Guid guid) throws UMSException {
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        _parentObject = UMSObject.getObject(token, guid);
        try {
            _schemaManager = SchemaManager.getSchemaManager(token
                    .getPrincipal());
        } catch (SSOException se) {
            throw new UMSException("Bad Authentication Token "
                    + se.getMessage());
        }
    }

    /**
     * This constructor sets the parent Directory entry which identifies the
     * location of COS definitions which will be managed. It also gets an
     * instance of a SchemaManager which will be used to update schema entries
     * for COS assignments.
     * 
     * @param principal
     *            Authenticated principal
     * @param guid
     *            The unique identifier specifying where COS definitions will be
     *            managed.
     * 
     * @throws UMSException
     *             The exception thrown if there is a problem determining the
     *             parent entry, or getting the SchemaManager instance.
     */
    protected COSManager(Principal principal, Guid guid) throws UMSException {
        _parentObject = UMSObject.getObject(principal, guid);
        _schemaManager = SchemaManager.getSchemaManager(principal);
    }

    /**
     * This method returns an instance of a COS Manager.
     * 
     * @param token Authenticated principal's single sign on token.
     * @param guid COS definitions will be managed under the level identified by
     *        this guid.
     * @throws UMSException
     *             The exception thrown from the COSManager constructor.
     * @supported.api
     */
    public static COSManager getCOSManager(SSOToken token, Guid guid)
            throws UMSException {
        return new COSManager(token, guid);
    }

    /**
     * This method returns an instance of a COS Manager.
     * 
     * @param principal Authenticated principal.
     * @param guid COS definitions will be managed under the level identified by
     *        this guid.
     * @throws UMSException
     *             The exception thrown from the data layer.
     */
    public static COSManager getCOSManager(Principal principal, Guid guid)
            throws UMSException {
        return new COSManager(principal, guid);
    }

    /**
     * This method adds a COS definition to the persistent store. The definition
     * is added under the specified "guid" parameter.
     * 
     * @param cosDef
     *            The COS definition to be added.
     * 
     * @throws UMSException
     *             The exception thrown from the data layer.
     * @supported.api
     */
    public void addDefinition(ICOSDefinition cosDef) throws UMSException {
        if (!(cosDef instanceof DirectCOSDefinition)) {
            String msg = i18n.getString(IUMSConstants.INVALID_COSDEFINITION);
            throw new UMSException(msg);
        }
        String[] cosAttributes = cosDef.getCOSAttributes();
        AbstractCollection aList = (AbstractCollection) Arrays
                .asList(ICOSDefinition.qualifiers);
        for (int i = 0; i < cosAttributes.length; i++) {
            String cosAttribute = null;
            String qualifier = null;
            StringTokenizer st = new StringTokenizer(cosAttributes[i]);
            if (st.hasMoreTokens()) {
                cosAttribute = st.nextToken();
            }
            if (cosAttribute == null) {
                String msg = i18n.getString(
                        IUMSConstants.INVALID_COS_ATTRIBUTE_QUALIFIER);
                throw new UMSException(msg);
            }
            if (st.hasMoreTokens())
                qualifier = st.nextToken();
            if (qualifier == null) {
                qualifier = ICOSDefinition.qualifiers[ICOSDefinition.DEFAULT];
                cosDef.removeCOSAttribute(cosAttribute);
                cosDef.addCOSAttribute(cosAttribute, ICOSDefinition.DEFAULT);
            }
            if (!aList.contains(qualifier)) {
                String msg = i18n.getString(
                        IUMSConstants.INVALID_COS_ATTRIBUTE_QUALIFIER);
                throw new UMSException(msg);
            }
        }
        PersistentObject po = (PersistentObject) cosDef;
        _parentObject.addChild(po);
    }

    /**
     * Removes the COS definition.
     * 
     * @param name
     *            The name of the definition to be removed.
     * 
     * @throws UMSException
     *             The exception thrown from the data layer.
     * @supported.api
     */
    public void removeDefinition(String name) throws UMSException {
        Guid guid = new Guid(ICOSDefinition.DEFAULT_NAMING_ATTR + "=" + name
                + "," + _parentObject.getGuid().getDn());
        _parentObject.removeChild(guid);
    }

    /**
     * Updates the contents of a COS definition with the new contents. The COS
     * definition must already exist in the persistent layer, before its
     * contents can be replaced.
     * 
     * @param cosDef
     *            The COS definition containing new contents, which will replace
     *            the same definition in the persistent layer.
     * 
     * @throws UMSException
     *             The exception thrown from the data layer.
     * @supported.api
     */
    public void updateDefinition(ICOSDefinition cosDef) throws UMSException {
        PersistentObject pObject = (PersistentObject) cosDef;
        if (pObject.getGuid() == null) {
            String msg = i18n
                    .getString(IUMSConstants.REPLACE_DEFINITION_NOT_PERSISTENT);
            throw new UMSException(msg);
        }
        pObject.save();
    }

    /**
     * Returns COS definition given the name.
     * 
     * @param name Name of the COS definition.
     * @return A COS definition with the specified name.
     * @throws UMSException if exception occurred at the data layer.
     * @throws COSNotFoundException if the COS object is not found.
     * @supported.api
     */
    public ICOSDefinition getDefinition(String name) throws UMSException,
            COSNotFoundException {
        ICOSDefinition cosDef = null;
        SearchResults sr = _parentObject.getChildren(
                ICOSDefinition.COSSUPERDEF_NAME_SEARCH + name + ")",
                DEF_ATTRIBUTE_NAMES, null);
        while (sr.hasMoreElements()) {
            cosDef = (ICOSDefinition) sr.next();
            if (cosDef.getName().equals(name)) {
                break;
            } else {
                cosDef = null;
            }
        }
        if (cosDef == null) {
            String msg = i18n.getString(IUMSConstants.COS_DEFINITION_NOT_FOUND);
            throw new COSNotFoundException(msg);
        }
        sr.abandon();
        return cosDef;
    }

    /**
     * Retrieves all COS definitions for the current organization. This
     * COSManager instance applies to an organization.
     * 
     * @return A collection of COS definition objects.
     * 
     * @throws UMSException
     *             The exception thrown from the data layer.
     * @supported.api
     */
    public Collection getDefinitions() throws UMSException {
        Collection cosDefinitions = new ArrayList();
        SearchResults sr = _parentObject.search(
                ICOSDefinition.COSSUPERDEF_SEARCH, DEF_ATTRIBUTE_NAMES, null);
        while (sr.hasMoreElements()) {
            cosDefinitions.add(sr.next());
        }
        return cosDefinitions;
    }

    /**
     * Assigns a COS (as defined by a COS definition) to the persistent object.
     * The COS target persistent object could be a user, group, organization,
     * organizationalunit, etc. The COS target object must be persistent before
     * this method can be used.
     * 
     * @param pObject
     *            The COS target persistent object.
     * @param cosDef
     *            A COS definition.
     * @param cosTemplate
     *            A COS template. This only applies for COS and Indirect COS
     *            definitions. For pointer COS definitions, this parameter can
     *            be null.
     * 
     * @throws UMSException
     *             If a data layer exception occurs.
     * @supported.api
     */
    public void assignCOSDef(PersistentObject pObject, ICOSDefinition cosDef,
            COSTemplate cosTemplate) throws UMSException {
        if (pObject == null || cosDef == null) {
            String msg = i18n
                    .getString(IUMSConstants.COS_DEF_OR_TARGET_OBJECT_NULL);
            throw new UMSException(msg);
        }

        // Do validation....
        //
        if (pObject.getGuid() == null) {
            String msg = i18n
                    .getString(IUMSConstants.COS_TARGET_OBJECT_NOT_PERSISTENT);
            throw new UMSException(msg);
        }

        if (!(cosDef instanceof DirectCOSDefinition)) {
            String msg = i18n.getString(IUMSConstants.INVALID_COSDEFINITION);
            throw new UMSException(msg);
        }

        if (cosDef instanceof DirectCOSDefinition) {
            assignDirectCOSDef(pObject, (DirectCOSDefinition) cosDef,
                    cosTemplate, _schemaManager);
        }
    }

    /**
     * Removes COS assignment from the persistent object. The COS target
     * persistent object could be a user, group, organization,
     * organizationalunit, etc. The COS target object must be persistent before
     * this method can be used.
     * 
     * @param pObject
     *            The COS target persistent object.
     * @param cosDef
     *            A COS definition.
     * @param cosTemplate
     *            A COS template.
     * 
     * @throws UMSException
     *             The exception thrown if any of the following occur: o the
     *             target persistent object or COS definition parameter is null.
     *             o the target object is not persistent. o the COS definition
     *             is not one of the valid COS definitions. o an exception is
     *             propagated from any of the "remove" methods.
     * @supported.api
     */
    public void removeCOSAssignment(PersistentObject pObject,
            ICOSDefinition cosDef, COSTemplate cosTemplate) throws UMSException 
            {
        if (pObject == null || cosDef == null) {
            String msg = i18n
                    .getString(IUMSConstants.COS_DEF_OR_TARGET_OBJECT_NULL);
            throw new UMSException(msg);
        }

        // Do validation....
        //
        if (pObject.getGuid() == null) {
            String msg = i18n
                    .getString(IUMSConstants.COS_TARGET_OBJECT_NOT_PERSISTENT);
            throw new UMSException(msg);
        }

        if (!(cosDef instanceof DirectCOSDefinition)) {
            String msg = i18n.getString(IUMSConstants.INVALID_COSDEFINITION);
            throw new UMSException(msg);
        }

        if (cosDef instanceof DirectCOSDefinition) {
            removeDirectCOSAssignment(pObject, (DirectCOSDefinition) cosDef,
                    cosTemplate, _schemaManager);
        }
    }

    /**
     * Removes a Direct COS assignment from a target persistent object. The COS
     * target persistent object could be a user, group, organization,
     * organizationalunit, etc. The COS target object must be persistent before
     * this method can be used.
     * 
     * @param pObject
     *            The COS target persistent object.
     * @param cosDef
     *            A COS definition.
     * @param sMgr
     *            A SchemaManager object, which is used to determine object
     *            classes for attributes.
     * 
     * @throws UMSException
     *             The exception thrown if any of the following occur: o an
     *             exception occurs determining the object class for the COS
     *             specifier. o an exception occurs determining the object class
     *             for the COS attributes. o there is an exception thrown rom
     *             the data layer.
     */
    private void removeDirectCOSAssignment(PersistentObject pObject,
            DirectCOSDefinition cosDef, COSTemplate cosTemplate,
            SchemaManager sMgr) throws UMSException {
        ArrayList aList;
        AttrSet attrSet = new AttrSet();

        try {
            // Include the attribute (whose name is the cosSpecifier)
            // in the attribute set for removal (only if it exists).
            //
            if (pObject.getAttribute(cosDef.getCOSSpecifier()) != null)
                attrSet.add(new Attr(cosDef.getCOSSpecifier(), cosTemplate
                        .getName()));

            // Get cosSpecifier object class - should only be one.
            // Include the cosSpecifier object class in the attribute
            // set for removal (only if itt exists).
            //
            aList = (ArrayList) sMgr.getObjectClasses(cosDef.getCOSSpecifier());
            String cosSpecObjectClass = (String) aList.get(0);
            if (objectClassExists(cosSpecObjectClass, pObject)) {
                attrSet.add(new Attr("objectclass", cosSpecObjectClass));
            }

            // Get the cos attributes from the definition (ex. mailquota).
            // For each of the attributes, get the objectclass. Include the
            // object classes in the attribute set for removal (if they exist).
            //
            String[] cosAttributes = cosDef.getCOSAttributes();
            String cosAttribute = null;
            for (int i = 0; i < cosAttributes.length; i++) {
                // Only get the attribute - not the qualifier
                //
                StringTokenizer st = new StringTokenizer(cosAttributes[i]);
                cosAttribute = st.nextToken();
                aList = (ArrayList) sMgr.getObjectClasses(cosAttribute);
                String cosAttributeObjectClass = (String) aList.get(0);
                if (objectClassExists(cosAttributeObjectClass, pObject)) {
                    attrSet
                            .add(new Attr("objectclass",
                                    cosAttributeObjectClass));
                }
            }

            if (attrSet.size() > 0) {
                ModSet modSet = new ModSet(attrSet, ModSet.DELETE);
                pObject.modify(modSet);
                pObject.save();
            }
        } catch (UMSException e) {
            LDAPException le = (LDAPException) e.getRootCause();
            if (le.getLDAPResultCode() == LDAPException.OBJECT_CLASS_VIOLATION) 
            {
                // Ignore... It's not a COS generated attribute's
                // object class.
            } else {
                throw e;
            }
        }
    }

    /**
     * Assigns a direct (Classic) COS definition to a persistent object.
     * 
     * @param pObject
     *            The target persistent object.
     * @param cosDef
     *            The direct (Classic) COS definition.
     * @param cosTemplate
     *            A COS template belonging to the definition.
     * @param sMgr
     *            A SchemaManager instance.
     * 
     * @throws UMSException
     *             if an exception occurs
     */
    private void assignDirectCOSDef(PersistentObject pObject,
            DirectCOSDefinition cosDef, COSTemplate cosTemplate,
            SchemaManager sMgr) throws UMSException {

        // Do validation....
        //
        if (cosDef.getGuid() == null) {
            String msg = i18n
                    .getString(IUMSConstants.COS_DEFINITION_NOT_PERSISTENT);
            throw new UMSException(msg);
        }

        // Make sure target entry is in same tree as COS Def parent.
        //
        DN targetDN = new DN(pObject.getGuid().getDn());
        DN cosParentDN = new DN(cosDef.getParentGuid().getDn());
        if (!(targetDN.isDescendantOf(cosParentDN))) {
            String msg = i18n
                    .getString(IUMSConstants.COS_TARGET_OBJECT_DIFFERENT_TREE);
            throw new UMSException(msg);
        }

        // If cosSpecifier is "nsRole", then we don't need to go
        // any further (we don't need to update target entries).
        //
        if (cosDef.getCOSSpecifier().equalsIgnoreCase("nsrole"))
            return;

        ArrayList aList;
        AttrSet attrSet = new AttrSet();

        // Get cosSpecifier object class - should only be one.
        // Update the target entry with cosSpecifier object class.
        // Only add it if it doesn't already exist.
        //
        aList = (ArrayList) sMgr.getObjectClasses(cosDef.getCOSSpecifier());
        String cosSpecObjectClass = (String) aList.get(0);
        if (!objectClassExists(cosSpecObjectClass, pObject)) {
            attrSet.add(new Attr("objectclass", cosSpecObjectClass));
        }

        // Get the cos attributes from the definition (ex. mailquota).
        // For each of the attributes, get the objectclass. These
        // will be used to attach to the target entry. This is only
        // done if the cos attribute qualifier is not "operational"
        // (you don't need to add cos attribute object classes for
        // "operational" cos attribute qualifier.
        //
        String[] cosAttributes = cosDef.getCOSAttributes();
        String qualifier = null;
        Arrays.asList(ICOSDefinition.qualifiers);
        Attr attr = cosTemplate.getAttribute("objectclass");
        String[] cosTempObjClasses = attr.getStringValues();

        for (int i = 0; i < cosAttributes.length; i++) {
            StringTokenizer st = new StringTokenizer(cosAttributes[i]);
            st.nextToken();
            qualifier = st.nextToken();
            if ((!qualifier.equals(
                    ICOSDefinition.qualifiers[ICOSDefinition.OPERATIONAL]))) {
                for (int j = 0; j < cosTempObjClasses.length; j++) {
                    if (!cosTempObjClasses[j].equalsIgnoreCase("top")
                       && !cosTempObjClasses[j].equalsIgnoreCase("costemplate")
                       && !objectClassExists(cosTempObjClasses[j], pObject)) 
                    {
                        if (!attrSet.contains("objectclass",
                                cosTempObjClasses[j])) {
                            attrSet.add(new Attr("objectclass",
                                    cosTempObjClasses[j]));
                        }
                    }
                }
            }
        }

        // Add the attribute name (cosSpecifier value) and attribute
        // value (cosTemplate name) only if it doesn't exist.
        //
        if (pObject.getAttribute(cosDef.getCOSSpecifier()) == null)
            attrSet.add(new Attr(cosDef.getCOSSpecifier(), cosTemplate
                    .getName()));

        if (attrSet.size() > 0) {
            ModSet modSet = new ModSet(attrSet);
            pObject.modify(modSet);
            pObject.save();
        }
    }

    /**
     * Utility method to check if an object class exists in a persistent object.
     * 
     * @param objectclass
     *            The object class.
     * @param pObject
     *            The persistent object.
     */
    private boolean objectClassExists(String objectClass,
            PersistentObject pObject) {
        Attr attr = pObject.getAttribute("objectclass");
        String[] vals = attr.getStringValues();
        for (int i = 0; i < vals.length; i++) {
            if (objectClass.equalsIgnoreCase(vals[i])) {
                return true;
            }
        }
        return false;
    }

    //
    // Definition Search Attributes
    //
    private static final String[] DEF_ATTRIBUTE_NAMES = { "objectclass",
            ICOSDefinition.DEFAULT_NAMING_ATTR, ICOSDefinition.COSTEMPLATEDN,
            ICOSDefinition.COSSPECIFIER, ICOSDefinition.COSATTRIBUTE,
            ICOSDefinition.ICOSSPECIFIER };

    private PersistentObject _parentObject;

    private SchemaManager _schemaManager;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
}
