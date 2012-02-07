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
 * $Id: EntityManager.java,v 1.3 2008/06/25 05:41:44 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;

/**
 * This class has the responsibility of creating structural entities. A
 * structural entity is an entry which contains other entries (example: people
 * container). The information in the DIT (under "StructureTemplates") defines
 * the structure . For example, when an Organization is created, it will contain
 * a "Groups" container. This class creates the structure of entities as defined
 * in the DIT.
 */
public class EntityManager implements IUMSConstants {

    /**
     * Get the instance of Entity Manager.
     * 
     * @return Instance of Entity manager.
     * 
     * @throws UMSException
     *             If an exception occurs.
     */
    public static synchronized EntityManager getEntityManager()
            throws UMSException {
        if (_instance == null) {
            _instance = new EntityManager();
        }
        return _instance;
    }

    /**
     * This method will use methods provided by ConfigManager to get "Structure
     * Template" information from the DIT. It will use this information to
     * create the "Structural Entity".
     * 
     * @param principal
     *            The Principal.
     * @param pObject
     *            The persistent object for which the entities apply.
     * @param pGUID
     *            The guid of the parent object.
     * @throws UMSException
     *             if an exception occurs.
     */
    public void execute(java.security.Principal principal,
            PersistentObject pObject, Guid pGUID) throws UMSException {

        String className;
        HashMap hm = null;
        Set set = null;
        Iterator iter = null;
        Attr attr = null;
        AttrSet attrSet = null;
        String[] attrValues;

        if (pObject == null) {
            String msg = i18n
                    .getString(IUMSConstants.PERSISTENT_OBJECT_PARAM_NULL);
            throw new UMSException(msg);
        }

        _principal = principal;
        _pObject = pObject;
        _stack = new Stack();

        className = _pObject.getClass().getName();
        _parentObject = _pObject;

        if (debug.messageEnabled()) {
            debug.message("GETTING ENTITY FOR:CLASS:" + className + ",PARENT:"
                    + pGUID.getDn());
        }

        try {
            set = _configManager.getEntity(pGUID, className);
            if (!set.isEmpty()) {
                if (set.size() > 1) {
                    attrSet = findEntity(_pObject, set);
                } else {
                    Iterator it = set.iterator();
                    if (it.hasNext())
                        attrSet = (AttrSet) it.next();
                }
            } else
                return;

            if (attrSet == null) {
                String args[] = new String[1];
                args[0] = className;
                String msg = i18n.getString(
                        IUMSConstants.STRUCTURE_TEMPLATE_ATTRSET_NULL, args);
                throw new UMSException(msg);
            }
        } catch (ConfigManagerException cme) {
            String args[] = new String[1];
            args[0] = cme.getMessage();
            String msg = i18n.getString(IUMSConstants.CONFIG_MGR_ERROR, args);
            throw new UMSException(msg);
        }

        if (debug.messageEnabled()) {
            debug.message("ENTITY ATTRSET:" + attrSet);
        }
        attr = attrSet.getAttribute(ENTITY_CHILDNODE);
        if (attr == null) {
            return;
        }
        attrValues = attr.getStringValues();
        for (int i = 0; i < attrValues.length; i++) {
            hm = new HashMap();
            hm.put(attrValues[i], _parentObject.getGuid());
            _stack.push(hm);
        }
        while (!_stack.empty()) {
            hm = (HashMap) _stack.pop();
            set = hm.keySet();
            iter = set.iterator();
            String childNodeName = (String) iter.next();
            Guid parentGuid = (Guid) hm.get(childNodeName);
            try {
                Set childSet = _configManager.getEntity(pGUID, childNodeName);
                if (!childSet.isEmpty()) {
                    iter = childSet.iterator();
                    if (iter.hasNext())
                        attrSet = (AttrSet) iter.next();
                }
                if (childSet.isEmpty() | attrSet == null)
                    return;
            } catch (ConfigManagerException cme) {
                String args[] = new String[1];
                args[0] = cme.getMessage();
                String msg = i18n.getString(IUMSConstants.CONFIG_MGR_ERROR,
                        args);
                throw new UMSException(msg);
            }

            // Create Object
            //
            PersistentObject pObj = createObject(attrSet, parentGuid, pGUID);

            attr = attrSet.getAttribute(ENTITY_CHILDNODE);
            if (attr != null) {
                attrValues = attr.getStringValues();
                for (int j = 0; j < attrValues.length; j++) {
                    hm = new HashMap();
                    hm.put(attrValues[j], pObj.getGuid());
                    _stack.push(hm);
                }
            }
        }
    }

    /**
     * This method will determine which Structure Template entry to use if there
     * is more than one returned from ConfigManagerUMS. It will make the
     * determination based on the "filter" and "priority" attributes in each
     * Structure Template atrribute set.
     * 
     * @param pObject
     *            The persistentObject for which the Structure Template applies.
     * @param set
     *            The Set containing multiple Structure Template attribute sets.
     * @throws UMSException
     *             if an error occurs.
     */
    private AttrSet findEntity(PersistentObject pObject, Set set)
            throws UMSException {

        //
        // Loop through all attrSets in Set, and for each one:
        // o use the filter to find the entry for which the Structure
        // Template applies. If each of the attrSet filters finds
        // an entry, select the one with the highest priority (0 being
        // the highest).
        //
        AttrSet foundAttrSet = null;
        AttrSet attrSet = null;
        try {
            Iterator it = set.iterator();
            while (it.hasNext()) {
                attrSet = (AttrSet) it.next();
                String filter = attrSet.getAttribute(ENTITY_FILTER).getValue();
                SearchControl sc = new SearchControl();
                sc.setSearchScope(SearchControl.SCOPE_BASE);
                SearchResults sr = pObject.search(filter, sc);
                if (sr.hasMoreElements()) {
                    sr.abandon();
                    if ((foundAttrSet == null)
                            || (Integer.parseInt(attrSet.getAttribute(
                                    ENTITY_PRIORITY).getValue()) < Integer
                                    .parseInt(foundAttrSet.getAttribute(
                                            ENTITY_PRIORITY).getValue()))) {
                        foundAttrSet = attrSet;
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            String args[] = new String[3];
            args[0] = "" + attrSet;
            args[1] = "" + foundAttrSet;
            args[2] = nfe.getMessage();
            String msg = i18n.getString(
                    IUMSConstants.BAD_STRUCTURE_TEMPLATE_PRIORITY, args);
            throw new UMSException(msg);
        }
        return foundAttrSet;
    }

    /**
     * This method creates a Persistent Object and adds it to a "parent"
     * Persistent Object.
     * 
     * @param attrSet
     *            The attribute set used to create the child object.
     * @param parentGUID
     *            The identifier for the "parent" Persistent object.
     * @param searchGuid
     *            Where to get the CreationTemplate.
     * @return The child Persistent Object which was added to the parent.
     * 
     * @throws UMSException
     *             if an exception occurs.
     */
    private PersistentObject createObject(AttrSet attrSet, Guid parentGuid,
            Guid searchGuid) throws UMSException {

        String[] attrValues;

        DataLayer dataLayer = DataLayer.getInstance();
        //
        // Extract attributes from attribute sets.
        // Build an attribute set of name/value pairs which will
        // be used with the CreationTemplate to create the child
        // Persistent Object.
        //
        Attr tempAttr = attrSet.getAttribute(ENTITY_TEMPLATE);
        Attr attrAttr = attrSet.getAttribute(ENTITY_NAME);
        if (attrAttr == null) {
            String msg = i18n.getString(IUMSConstants.BAD_NAMING_ATTR);
            throw new UMSException(msg);
        }
        attrValues = attrAttr.getStringValues();
        attrSet = new AttrSet();
        for (int k = 0; k < attrValues.length; k++) {
            String attrName = null;
            String attrValue = null;
            int index = attrValues[k].indexOf(NAME_TOKEN_DELIM);
            if (index < 0) {
                String msg = i18n.getString(IUMSConstants.BAD_NAMING_ATTR);
                throw new UMSException(msg);
            }
            attrName = attrValues[k].substring(0, index);
            attrValue = attrValues[k].substring(index + 1, attrValues[k]
                    .length());
            attrSet.add(new Attr(attrName, attrValue));
        }

        //
        // Use the TemplateManager to get an instance of a CreationTemplate.
        // Get the class name from the CreationTemplate.
        // Instantiate the constructor which accepts the attribute set -
        // this creates the child object.
        // Add the child object to the parent.
        //
        String templateName = tempAttr.getValue();
        CreationTemplate ct = _templateMgr.getCreationTemplate(templateName,
                searchGuid);
        PersistentObject pObj = new PersistentObject(ct, attrSet);
        String idAttr = pObj.getNamingAttribute();
        String idValue = pObj.getAttribute(idAttr).getValue();
        Guid childGuid = new Guid(idAttr + "=" + idValue + ","
                + parentGuid.getDn());

        String[] attrNames = pObj.getAttributeNames();
        AttrSet atts = pObj.getAttributes(attrNames);

        try {
            dataLayer.addEntry(_principal, childGuid, atts);
        } catch (UMSException umse) {
            throw new UMSException(umse.getMessage());
        }

        UMSObject.getObject(_principal, childGuid);

        return pObj;
    }

    /**
     * Constructor
     * 
     * @throws UMSException
     *             if an exception occurs.
     */
    private EntityManager() throws UMSException {
        try {
            _configManager = ConfigManagerUMS.getConfigManager();
            _templateMgr = TemplateManager.getTemplateManager();
        } catch (ConfigManagerException cme) {
            throw new UMSException(cme.getMessage());
        }
    }

    private static final String ENTITY_CHILDNODE = "childNode";

    private static final String ENTITY_FILTER = "filter";

    private static final String ENTITY_PRIORITY = "priority";

    private static final String ENTITY_TEMPLATE = "template";

    private static final String ENTITY_NAME = "name";

    private static final String NAME_TOKEN_DELIM = "=";

    private PersistentObject _pObject;

    private PersistentObject _parentObject;

    private Stack _stack;

    private TemplateManager _templateMgr;

    private java.security.Principal _principal;

    private ConfigManagerUMS _configManager;

    private static EntityManager _instance = null;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }
}
