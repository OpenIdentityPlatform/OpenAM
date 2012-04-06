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
 * $Id: ConfigManagerUMS.java,v 1.6 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import com.iplanet.am.util.Cache;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.util.GuidUtils;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.security.ServerInstanceAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.util.DN;

/**
 * Configuration Manager is responsible for getting configuration parameters for
 * UMS. ConfigManager is implemented as a singleton and the configurations can
 * be accessed as follows:
 * <p>
 * 
 * <pre>
 *  ConfigManager cm = ConfigManager.getConfigManager(); 
 *     AttrSet tempAtts=
 *             cm.getTemplate(&quot;o=foo,o=isp&quot;, &quot;BasicUser&quot;); 
 *     Set hs =
 *     cm.getEntity(&quot;o=foo,o=isp&quot;, 
 *              &quot;com.iplanet.ums.PeopleContainer&quot; );
 *     String [] sTemplateNames = 
 *         cm.getCreationTemplateNames(&quot;o=foo, o=isp&quot;); *
 * </pre>
 * 
 * ConfigManager obsoletes the use of previous Config class which opens up
 * resource file and reading whenever a configuation is needed. ConfigManager
 * speeds up the retrieval in caching the configurations at start up time once
 * as a singleton and share it among the ums package. ConfigManager uses the
 * default PROXY as AuthPrincipal for this instance of the server.
 * 
 * </p>
 */
public class ConfigManagerUMS implements java.io.Serializable {

    /**
     * i18n keys
     */
    static final String ERROR_CM_INITIATE = "error-cminitiate";

    static final String ERROR_CM = "error-cm";

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Get the singleton instance of Config Manager.
     * 
     * @return Singleton instance of Configuration Manager.
     */
    public static synchronized ConfigManagerUMS getConfigManager()
            throws ConfigManagerException {
        if (_instance == null) {
            try {
                _instance = new ConfigManagerUMS();
            } catch (Exception e) {
                _debug.error(e.toString());
                String args[] = new String[1];
                args[0] = e.toString();
                throw new ConfigManagerException(i18n.getString(
                        IUMSConstants.ERROR_CM_INITIATE, args));
            }
        }
        return _instance;
    }

    /**
     * Reads the directory server, via SMS APIs for a given org (/b/a) and for a
     * given template (<code>StructureTemplates</code>,
     * <code>CreationTemplates</code>, etc.).
     * 
     * @param org  Name of organization <code>/pepsi/coke</code>.
     * @param c  List of template names.
     * @param template Name of template.
     */
    private void loadCache(String org, Set c, String template) {
        //
        // To get all attributes for each component name from DS and store
        // in cache. Calls getServiceAttrs()
        //
        Iterator iter = c.iterator();
        String entName = "";
        AttrSet attrSet = null;
        while (iter.hasNext()) {
            entName = iter.next().toString();
            Map entityAttributes = new HashMap();
            attrSet = new AttrSet();

            try {
                entityAttributes = getServiceAttributes(org, entName);
            } catch (SMSException smse) {
                // Don't throw an exception, just log it. Cache will return
                // a null
                _debug.error("ConfigManager->loadCache: SMSException: "
                        + smse.toString());
            } catch (SSOException ssoe) {
                // Don't throw an exception, just log it. Cache will return
                // a null
                _debug.error("ConfigManager->loadCache: SSOException: "
                        + ssoe.toString());
            }

            if (entityAttributes.isEmpty())
                continue;

            for (Iterator it = entityAttributes.entrySet().iterator(); it
                    .hasNext();) {
                Map.Entry ent = (Map.Entry) it.next();
                Set hs = (Set) ent.getValue();
                Iterator itera = hs.iterator();

                itera = hs.iterator();
                while (itera.hasNext()) {
                    attrSet.add(new Attr((String) ent.getKey(), (String) itera
                            .next()));
                }
            }

            String key = org + "/" + template + "/";
            Attr classAttr = null;
            if (template.equals(ENTITY)) {
                // This search is for EntityManager
                classAttr = attrSet.getAttribute(CLASS);
                if (classAttr != null) {
                    Set hs = new HashSet();
                    if (_cch.containsKey(key + classAttr.getValue())) {
                        hs = (Set) _cch.get(key + classAttr.getValue());
                        hs.add(attrSet);
                    } else {
                        hs.add(attrSet);
                    }
                    _cch.put(key + classAttr.getValue(), hs);
                    if (_debug.messageEnabled())
                        _debug.message("ConfigManager->loadCache KEY:" + key
                                + classAttr.getValue() + " VALUE:" + hs);
                }
                int l = entName.lastIndexOf("/");
                String cname = entName.substring(l + 1);
                // This search is for EntityManager
                Set hset = new HashSet();
                hset.add(attrSet);
                _cch.put(key + cname, hset);
                if (_debug.messageEnabled())
                    _debug.message("ConfigManager->loadCache KEY:" + key
                            + cname + " VALUE:" + hset);
            }
            // } else {
            if ((template.equals(SEARCH)) || template.equals(CREATION)) {
                // This search is for TemplateManager
                classAttr = attrSet.getAttribute(JAVACLASS);
                if (classAttr != null) {
                    _cch.put(key + classAttr.getValue(), attrSet);
                    if (_debug.messageEnabled())
                        _debug.message("ConfigManager->loadCache KEY:" + key
                                + classAttr.getValue() + " VALUE:" + attrSet);
                }
                // This search is for TemplateManager
                classAttr = attrSet.getAttribute(ATTRNAME);
                if (classAttr != null) {
                    _cch.put(key + classAttr.getValue(), attrSet);
                    _debug.message("ConfigManager->loadCache KEY:" + key
                            + classAttr.getValue() + " VALUE:" + attrSet);
                }
            }
            if (template.equals(OBJECTRESOLVER)) {
                // This adds the String[][] for ObjectResolver to cache
                String oc_jc_map_string = attrSet.getValue(OC_JC_MAP);
                if (oc_jc_map_string != null) {
                    _cch
                            .put(OBJECTRESOLVERPATH,
                                    getOC_JC_MAP(oc_jc_map_string));
                    _debug.message("ConfigManager->loadCache KEY:"
                            + OBJECTRESOLVERPATH + " VALUE:" + attrSet);
                }
            }
        }
    }

    /**
     * CACHE MANAGEMENT for entity and template components.
     * 
     * @param org A string identifier for the cache.
     * @throws ConfigManagerException
     *         The default components would be loaded through an XML
     *         schema/configuration file <code>ConfigManager</code> has to
     *         get the components and the related attributes from the directory
     *         Server initially to store them in the cache by calling the SMS
     *         API.
     */
    void updateCache(String org) throws ConfigManagerException {

        Set eNames = new HashSet();
        Set sNames = new HashSet();
        Set cNames = new HashSet();

        // If org = "" (base level), then add OBJECTRESOLVER ATTRSET TO CACHE
        if (org.equals(_rootDN)) {
            Set oSet = new HashSet();
            oSet.add(OBJECTRESOLVERPATH);
            loadCache(org, oSet, OBJECTRESOLVER);
        }
        try {
            eNames = getServiceComponents(org, ENTITYPATH, true);
        } catch (SMSException smse) {
            // Don't do anything. This is an LDAP problem,
            // cache will just return NULL
            if (_debug.warningEnabled())
                _debug.warning("ConfigManager->updateCache: SMSException: "
                        + smse.toString());
        } catch (SSOException ssoe) {
            // Don't do anything. This is an LDAP problem,
            // cache will just return NULL
            if (_debug.warningEnabled())
                _debug.warning("ConfigManager->updateCache: SSOException: "
                        + ssoe.toString());
        }
        try {
            sNames = getServiceComponents(org, SEARCHPATH, true);
        } catch (SMSException smse) {
            // Don't do anything. This is an LDAP problem,
            // cache will just return NULL
            if (_debug.warningEnabled())
                _debug.warning("ConfigManager->updateCache: SMSException: "
                        + smse.toString());
        } catch (SSOException ssoe) {
            // Don't do anything. This is an LDAP problem,
            // cache will just return NULL
            if (_debug.warningEnabled())
                _debug.warning("ConfigManager->updateCache: SSOException: "
                        + ssoe.toString());
        }
        try {
            cNames = getServiceComponents(org, CREATIONPATH, true);
        } catch (SMSException smse) {
            // Don't do anything. This is an LDAP problem,
            // cache will just return NULL
            if (_debug.warningEnabled())
                _debug.warning("ConfigManager->updateCache: SMSException: "
                        + smse.toString());
        } catch (SSOException ssoe) {
            // Don't do anything. This is an SSO problem,
            // cache will just return NULL
            if (_debug.warningEnabled())
                _debug.warning("ConfigManager->updateCache: SSOException: "
                        + ssoe.toString());
        }

        if (cNames.isEmpty() && eNames.isEmpty() && sNames.isEmpty()) {
            _checkListCache.put(org.toLowerCase(), "dummy");
            return;
        }
        // This is a search for searchTemplateNames only.
        if (!sNames.isEmpty()) {
            Iterator it = sNames.iterator();
            Set set = new HashSet();
            while (it.hasNext()) {
                String s = new String();
                String t;
                t = (String) it.next();
                int count = t.lastIndexOf("/");
                s = t.substring(count + 1);
                set.add(s);
            }

            _cch.put(org + "/" + SEARCH + "Names", set);
            if (_debug.messageEnabled())
                _debug.message("ConfigManager->updateCache: " + org + "/"
                        + SEARCH + "Names :" + set);
        }
        // This is a search for creationTemplateNames only.
        if (!cNames.isEmpty()) {
            Iterator it = cNames.iterator();
            Set set = new HashSet();
            while (it.hasNext()) {
                String s = new String();
                String t;
                t = (String) it.next();
                int count = t.lastIndexOf("/");
                s = t.substring(count + 1);
                set.add(s);
            }
            _cch.put(org + "/" + CREATION + "Names", set);
            if (_debug.messageEnabled())
                _debug.message("ConfigManager->updateCache: " + org + "/"
                        + CREATION + "Names :" + set);
        }

        loadCache(org, eNames, ENTITY);
        loadCache(org, cNames, CREATION);
        loadCache(org, sNames, SEARCH);
        _checkListCache.put(org.toLowerCase(), "dummy");
    }

    private Set getServiceComponents(String orgName, String path, boolean b)
            throws SMSException, SSOException {
        ServiceConfig sc = (orgName.equals(_rootDN)) ? _smapi
                .getGlobalConfig(null) : _smapi.getOrganizationConfig(orgName,
                null);
        if (sc == null) {
            return new HashSet();
        }

        // Parser the "/" seperated path to get the right service config
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ((token.length() == 0) || (token.length() == 0)) {
                continue;
            }
            sc = sc.getSubConfig(token);
        }
        Set answer = new HashSet();
        Iterator comps = sc.getSubConfigNames().iterator();
        while (comps.hasNext()) {
            answer.add(path + "/" + comps.next());
        }
        return (answer);
    }

    private Map getServiceAttributes(String orgName, String path)
            throws SMSException, SSOException {

        ServiceConfig sc = (orgName.equals(_rootDN)) ? _smapi
                .getGlobalConfig(null) : _smapi.getOrganizationConfig(orgName,
                null);
        // Parser the "/" seperated path to get the right service config
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ((token.length() == 0) || (token.length() == 0)) {
                continue;
            }
            if (sc != null)
                sc = sc.getSubConfig(token);
            else
                return (java.util.Collections.EMPTY_MAP);
        }
        if (sc != null)
            return (sc.getAttributes());
        else
            return (java.util.Collections.EMPTY_MAP);
    }

    private void replaceServiceAttributes(String orgName, String path, 
            Map attrs)
            throws SMSException, SSOException {
        ServiceConfig sc = _smapi.getGlobalConfig(null);
        // Parser the "/" seperated path to get the right service config
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ((token.length() == 0) || (token.length() == 0)) {
                continue;
            }
            sc = sc.getSubConfig(token);
        }
        sc.setAttributes(attrs);
    }

    void deleteOrgFromCache(String org) throws ConfigManagerException {

        // Create this partial keys /b/a/StructureTemplates,
        // /b/a/CreationTemplates,/b/a/SearchTemplate,
        // so that with a root org, where the key would be ""
        // everything doesn't get deleted in the cache.
        String fdn;
        if (org == null || org.length() == 0) {
            // fdn = "";
            fdn = _rootDN;
        } else {
            /*
             * DN dn = new DN(org); DN root = new DN(_rootDN); String [] dns =
             * dn.explodeDN(true); String [] rootdns = root.explodeDN(true); int
             * len = dns.length; int rootLen = rootdns.length; StringBuffer sb =
             * new StringBuffer(); for (int k=0; k<len-rootLen; k++) {
             * sb.append("/").append(dns[len-k-rootLen -1]); } fdn =
             * sb.toString();
             */
            fdn = (new DN(org)).toRFCString().toLowerCase();
        }

        String cchPartialKey1 = fdn + "/" + ENTITY;
        String cchPartialKey2 = fdn + "/" + CREATION;
        String cchPartialKey3 = fdn + "/" + SEARCH;
        if (_debug.messageEnabled())
            _debug.message("ConfigManager->deleteOrgFromCache: Deleting " + org
                    + " from cache");
        _checkListCache.remove(org.toLowerCase());
        Enumeration e = _cch.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if ((key.indexOf(cchPartialKey1) == 0)
                    || (key.indexOf(cchPartialKey2) == 0)
                    || (key.indexOf(cchPartialKey3) == 0)) {
                if (_debug.messageEnabled())
                    _debug.message("ConfigManager->deleteOrgFromCache: "
                            + " Deleting..." + key);
                _cch.remove(key);
            }
        }
        // if org == "" (Base level) then remove ObjectResolver class too.
        if (org.length() == 0 && _cch.containsKey(OBJECTRESOLVERPATH)) {
            _cch.remove(OBJECTRESOLVERPATH);
            _oc_jc_map = null;
        }
    }

    /**
     * Config management.
     * 
     * @param guid GUID it is looking under.
     * @param name Name for which AttrSets are needed.
     * @param template Template name (<code>StructureTemplates</code>, etc.)
     * @return either an <code>AttrSet</code> or a collection depending on
     *         caller.
     */
    private Object getConfigData(Guid guid, String name, String template,
            int lookup) throws ConfigManagerException {

        // if guid is null, replace it use the rootDN
        if (guid == null)
            guid = new Guid(_rootDN);
        // if guid is not "o=...", find the organization guid
        /*
         * if (!guid.getDn().startsWith("o=")) { guid =
         * GuidUtils.getOrgGuid(guid); }
         */
        DN dn = new DN(guid.getDn());
        // Till we find what we are looking for,
        // traverse the tree in the cache
        while (true) {
            String fdn = _rootDN;
            boolean inCache = false;
            boolean checkedDS = false;
            String cacheKey = null;
            //
            // Explode the guid and build a string delimited by "/".
            // ex: guid->o=b,o=a fdn->/b/a
            // Build the cache key as fdn + name
            // ex: /b/a/com.iplanet.ums.organization
            // or /b/a/BasicUser
            fdn = dn.toRFCString().toLowerCase();
            // Special case Cache Key for OBJECTRESOLVER
            if (template.equals(OBJECTRESOLVER))
                cacheKey = OBJECTRESOLVERPATH;
            else
                cacheKey = fdn + "/" + template + "/" + name;
            if (_debug.messageEnabled())
                _debug.message("ConfigManager->getConfigData: fdn=" + fdn
                        + "   cacheKey=" + cacheKey);
            //
            // Check the cache for the entry.
            // If it's in the cache, return the information.
            //
            inCache = _cch.containsKey(cacheKey);
            if (inCache) {
                if (_debug.messageEnabled()) {
                    _debug.message("ConfigManager->getConfigData: get from " +
                            "cache for " + dn);
                }
                return _cch.get(cacheKey);
            }
            //
            // If it's not in the cache, check if we've looked in the
            // DS already. If we haven't checked the DS, go to the DS.
            // "updateCache" will get the info from the DS and update
            // both the cache (_cch) and _checkListCache.
            //
            checkedDS = _checkListCache.containsKey(fdn.toLowerCase());
            if (!checkedDS) {
                if (_debug.messageEnabled())
                    _debug.message("ConfigManager->getConfigData: updating " +
                            "cache for " + dn);
                synchronized (lock_cch) {
                    updateCache(fdn);
                }
                if (_cch.containsKey(cacheKey))
                    return _cch.get(cacheKey);
            }
            //
            // We've checked the DS already so get the parent and
            // try again.
            // Check if we need to traverse to the parent
            switch (lookup) {
            case TemplateManager.SCOPE_ORG:
                return null;
            case TemplateManager.SCOPE_TOP:
                dn = new DN(_rootDN);
                break;
            case TemplateManager.SCOPE_ANCESTORS:
                dn = dn.getParent();
                // After getting parent, check if this is already root of tree,
                // if so return null
                // if (dn.toString().length() == 0) return null;
                if (dn.isDescendantOf(_root) || dn.equals(_root))
                    break;
                else
                    return null;
            }
            if (_debug.messageEnabled())
                _debug.message("ConfigManager->getConfigData: Traversing " +
                        "parent: " + dn);
        }
    }

    public Set getConfigTemplateNames(Guid guid, String template, int lookup)
            throws ConfigManagerException {

        // if guid is null, replace it use the rootDN
        if (guid == null)
            guid = new Guid(_rootDN);
        // if guid is not "o=...", find the organization guid
        if (!guid.getDn().startsWith("o=")) {
            guid = GuidUtils.getOrgGuid(guid);
        }
        DN dn = new DN(guid.getDn());
        while (true) {
            String fdn = "";
            boolean inCache = false;
            boolean checkedDS = false;
            String cacheKey = null;
            //
            // Explode the guid and build a string delimited by "/".
            // ex: guid->o=b,o=a fdn->/b/a
            // Build the cache key as fdn + name
            // ex: /b/a/com.iplanet.ums.organization
            // or /b/a/BasicUser
            /*
             * String[] dns = dn.explodeDN(true); String[] rootdns =
             * root.explodeDN(true); int len = dns.length; int rootLen =
             * rootdns.length; for (int k=0; k<len-rootLen; k++) { fdn = fdn +
             * "/" + dns[len-k-rootLen-1]; }
             */
            fdn = dn.toRFCString().toLowerCase();
            cacheKey = fdn + "/" + template + "Names";
            //
            // Check the cache for the entry.
            // If it's in the cache, return the information.
            //
            if (_debug.messageEnabled())
                _debug.message("ConfigManager->getConfigTemplateNames: " +
                        "Looking for: " + cacheKey);
            inCache = _cch.containsKey(cacheKey);
            if (inCache)
                return (Set) _cch.get(cacheKey);
            //
            // If it's not in the cache, check if we've looked in the
            // DS already. If we haven't checked the DS, go to the DS.
            // "updateCache" will get the info from the DS and update
            // both the cache (_cch) and _checkListCache.
            //
            checkedDS = _checkListCache.containsKey(fdn.toLowerCase());
            if (!checkedDS) {
                if (_debug.messageEnabled())
                    _debug.message("ConfigManager->getConfigTemplateNames: " +
                            "updating " + dn);
                synchronized (lock_cch) {
                    updateCache(fdn);
                }
                if (_cch.containsKey(cacheKey))
                    return (Set) _cch.get(cacheKey);
            }

            //
            // We've checked the DS already so get the parent and
            // try again.
            switch (lookup) {
            case TemplateManager.SCOPE_ORG:
                return java.util.Collections.EMPTY_SET;
            case TemplateManager.SCOPE_TOP:
                dn = new DN(_rootDN);
                break;
            case TemplateManager.SCOPE_ANCESTORS:
                dn = dn.getParent();
                // After getting parent, check if this is already root of tree,
                // if so return null
                // if (dn.toString().length() == 0)
                if (dn.isDescendantOf(_root) || dn.equals(_root))
                    break;
                else
                    return java.util.Collections.EMPTY_SET;
            }
            _debug.message("ConfigManager->getConfigTemplateNames: " +
                    "Traversing parent: " + dn);
        }
    }

    /**
     * Returns the Attribute Key-Value set of a Structure Template
     * entry. It searches for all entries and returns the entry for which the
     * "class" Attribute matches the provided name.
     * 
     * @param guid GUI specifies the starting location for
     *        <code>ConfigManager</code> to begin searching for DIT
     *        information (for structural entities).
     * @param name Class name of the object for which the DIT information
     *        applies.
     * @return Collection of attrSets pertaining to the structure
     *         templates in the DIT.
     * @throws ConfigManagerException.
     */
    public Set getEntity(Guid guid, String name) throws ConfigManagerException {
        Set ret = (Set) getConfigData(guid, name, ENTITY,
                TemplateManager.SCOPE_ANCESTORS);
        return (ret == null) ?
            java.util.Collections.EMPTY_SET : ret;
    }

    /**
     * TEMPLATE MANAGER APIs
     * 
     */

    /**
     * Returns the Attribute Key-Value set of a Search or Creation
     * Template entry. It searches for all entries and returns the entry for
     * which the "name" Attribute matches the provided name.
     * 
     * @param guid Specifies the starting location for
     *        <code>ConfigManager</code> to begin searching for DIT
     *        information (for structural entities).
     * @param templateName Template name.
     * @param lookup
     * @return <code>AttrSet</code> value pertaining to the structural template
     *         in the DIT. Usage:
     *         <pre>
     *         AttrSet a = CM.getSearchTemplateForClass(
     *              principal, "o=foo,o=org", "BasicUserSearch");
     *         </pre>
     *         Converts the guid to <code>/iDA/foo/org</code> (for internal SMS
     *         representation). Looks for Search Template with attribute "name"
     *         matching <code>BasicUserSearch</code> and returns the first one
     *         matched. If found in cache, it returns that. Else it looks it up
     *         in the Directory through SMS (traverses the tree if need be).
     * @throws ConfigManagerException.
     */
    public AttrSet getSearchTemplate(Guid guid, String templateName, int lookup)
            throws ConfigManagerException {
        AttrSet ret = (AttrSet)getConfigData(guid, templateName, SEARCH,
                lookup);
        return (ret == null) ?
            com.iplanet.services.ldap.AttrSet.EMPTY_ATTR_SET : ret;
    }

    /**
     * Returns Attribute Key-Value set of a <code>CreationTemplate</code> entry.
     * It searches for all entries and returns the entry for which the
     * "name" Attribute matches the provided name.
     * 
     * @param guid Specifies the starting location for
     *        <code>ConfigManager</code> to begin searching for DIT information
     *        (for structural entities).
     * @param templateName Template name.
     * @param lookup
     * @return <code>AttrSet</code> value pertaining to the structural template
     *         in the DIT.  Usage:
     *         <pre>
     *         AttrSet a = CM.getCreationTemplateForClass(
     *              principal, "o=foo,o=org", "BasicUser");
     *         </pre>
     *         Converts the guid to <code>/iDA/foo/org</code> (for internal SMS
     *         representation). Looks for Search Template with attribute "name"
     *         matching <code>BasicUser</code> and returns the first one
     *         matched. If found in cache, it returns that. Else it looks it
     *         up in the Directory through SMS (traverses the tree if need be)
     * @throws ConfigManagerException.
     */
    public AttrSet getCreationTemplate(
        Guid guid,
        String templateName,
        int lookup
    ) throws ConfigManagerException {
        AttrSet ret = (AttrSet) getConfigData(guid, templateName, CREATION,
                lookup);
        if (ret == null)
            return com.iplanet.services.ldap.AttrSet.EMPTY_ATTR_SET;
        return ret;

    }

    /**
     * Returns the Attribute key-value pair of Creation templates under the
     * given organization by matching the the <code>javaclass</code> attribute
     * to the name provided. If no templates are listed under the current
     * organization then it traverses the org tree till it finds one, or returns
     * null.
     * 
     * @param guid Organization DN.
     * @param className Name of <code>javaclass</code> Attribute to be matched.
     * @param lookup
     * @return Attribute key-value pair of Creation templates.  Usage:
     *         <pre>
     *         AttrSet a = CM.getCreationTemplateForClass(
     *              principal, "o=foo,o=org", "com.iplanet.ums.BasicUser");
     *         </pre>
     *         Converts the guid to <code>/iDA/org</code>. Looks under
     *         <code>CreationTemplates/templates/org</code> for nodes where
     *         attribute "class" matches <code>com.iplanet.ums.BasicUser</code>.
     *         First looks up cache, if not found in cache, then looks up in
     *         Directory.
     */
    public AttrSet getCreationTemplateForClass(
        Guid guid,
        String className,
        int lookup
    ) throws ConfigManagerException {
        AttrSet ret = (AttrSet) getConfigData(
            guid, className, CREATION, lookup);
        if (ret == null) {
            return com.iplanet.services.ldap.AttrSet.EMPTY_ATTR_SET;
        }
        return ret;
    }

    /**
     * Returns an array of the Creation Template names under the
     * given organization. If there are no Creation Templates under the given
     * organization then it traveres the organization tree upwards till it finds
     * one, or returns null.
     * 
     * @param guid Organization to look under.
     * @return Set of Creation Template names.
     */
    public Set getCreationTemplateNames(Guid guid)
            throws ConfigManagerException {
        return getConfigTemplateNames(
                guid, CREATION, TemplateManager.SCOPE_ORG);
    }

    /**
     * Returns a set of the Search Template names under the given
     * organization. If there are no Search Templates under the given
     * organization then it traveres the organization tree upwards till it finds
     * one, or returns null.
     * 
     * @param guid Organization to look under.
     * @return Set of template name.
     * @throws ConfigManagerException.
     */
    public Set getSearchTemplateNames(Guid guid) throws ConfigManagerException {
        return getConfigTemplateNames(guid, SEARCH, TemplateManager.SCOPE_ORG);

    }

    /**
     * Gets the mapping between ldap entry objectclasses and the UMS Java class.
     * This is an array of objectclass, java class pairs. The
     * Objectclass/Javaclass pair for a superclass should be defined before that
     * of a subclass. This method returns a double-subscripted array for the
     * component "ObjectResolver" under the root tree. This component will not
     * exist under any other organization except the root.
     * 
     * @return an array of Objectclass/Javaclass pairs.
     * @exception ConfigManagerException.
     * 
     * Usage: String[][] a = CM.getClassResolver() Looks up the attributes at
     * the top level /ObjectResolver/templates/iDA Caches it first.
     */
    public String[][] getClassResolver() throws ConfigManagerException {
        // if it is not in the oc_jc_map cache, gets it
        if (_oc_jc_map == null) {
            _oc_jc_map = (String[][]) getConfigData(null, OBJECTRESOLVER,
                    OBJECTRESOLVER, TemplateManager.SCOPE_ORG);
        }
        return _oc_jc_map;
    }

    /**
     * Replaces an existing template.
     * 
     * @param guid the GUID it is looking under.
     * @param templateName Name of the template.
     * @param attrSet attribute-values pair to be replaced.
     * @exception ConfigManagerException.
     */
    public void replaceCreationTemplate(Guid guid, String templateName,
            AttrSet attrSet) throws ConfigManagerException {

        if (guid == null) {
            guid = new Guid(_rootDN);
        }
        DN dn = new DN(guid.getDn());
        String org = null;
        String[] dns = dn.explodeDN(true);

        for (int k = 0; k < dns.length - 1; k++) {
            org = org + "/" + dns[k];
        }

        String service = CREATIONPATH + "/" + templateName;

        Map map = convertToMap(attrSet);

        try {
            replaceServiceAttributes(org, service, map);
        } catch (SMSException e) {
            String args[] = new String[1];
            args[0] = e.toString();
            throw new ConfigManagerException(i18n.getString(
                    IUMSConstants.ERROR_CM, args));
        } catch (SSOException se) {
            String args[] = new String[1];
            args[0] = se.toString();
            throw new ConfigManagerException(i18n.getString(
                    IUMSConstants.ERROR_CM, args));
        }

    }

    private Map convertToMap(AttrSet attrs) {
        HashMap map = new HashMap();
        String[] names = attrs.getAttributeNames();
        for (int i = 0; i < names.length; i++) {
            Attr attr = attrs.getAttribute(names[i]);
            String[] values = attr.getStringValues();
            HashSet set = new HashSet();
            set.addAll(Arrays.asList(values));
            map.put(names[i], set);
        }
        return map;
    }

    private String[][] getOC_JC_MAP(String oc_jc_map_string) {
        StringTokenizer st = new StringTokenizer(oc_jc_map_string, ";");
        int tokencount = st.countTokens();
        String[][] oc_jc_map = new String[tokencount][2];
        int i = 0;
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            int index = s.indexOf('=');
            if (index >= 0) {
                String oc = s.substring(0, index);
                String jc = s.substring(index + 1, s.length());
                oc_jc_map[i][0] = oc;
                oc_jc_map[i][1] = jc;
                i++;
            }
        }
        return oc_jc_map;
    }

    /**
     * Construct configuration.
     */
    private ConfigManagerUMS() throws ConfigManagerException {
        _cch = new Hashtable();
        _checkListCache = new Cache(10000);
        String[] args = new String[1];

        try {
            DSConfigMgr dm = DSConfigMgr.getDSConfigMgr();
            ServerInstance si = dm.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            _rootDN = si.getBaseDN();
            _root = new DN(_rootDN);
            _rootDN = _root.toRFCString().toLowerCase();
            if (_debug.messageEnabled())
                _debug
                        .message("ConfigManager->Constructor: root DN "
                                + _rootDN);
            String p = si.getAuthID();
            _principal = new AuthPrincipal(p);
            String psswd = (String) AccessController
                    .doPrivileged(new ServerInstanceAction(si));
            AuthContext ac = new AuthContext(_principal, psswd.toCharArray());
            SSOToken token = ac.getSSOToken();
            try {
                SSOTokenManager.getInstance().validateToken(token);
            } catch (SSOException e) {
                args[0] = e.toString();
                throw new ConfigManagerException(i18n.getString(
                        IUMSConstants.INVALID_TOKEN, args));
            }
            // _smapi = new ServiceConfigManager(_principal);
            _smapi = new ServiceConfigManager(token, UMS_SRVC, UMS_VERSION);
            _listener = new CMListener();
            _lid = _smapi.addListener(_listener);
        } catch (Exception e) {
            _debug.error("ConfigManager->Constructor: Caught exception " + e);
            e.printStackTrace();
            args[0] = e.toString();
            throw new ConfigManagerException(i18n.getString(
                    IUMSConstants.ERROR_CM, args));
        }

    }

    protected static final String UMS_SRVC = "DAI";

    private static final String UMS_VERSION = "1.0";

    private static final String TEMPLATEPATH = "/templates";

    static final String ENTITYPATH = TEMPLATEPATH + "/StructureTemplates";

    static final String SEARCHPATH = TEMPLATEPATH + "/SearchTemplates";

    static final String CREATIONPATH = TEMPLATEPATH + "/CreationTemplates";

    private static final String OBJECTRESOLVERPATH = TEMPLATEPATH
            + "/ObjectResolver";

    private static final String OBJECTRESOLVER = "ObjectResolver";

    private static final String ENTITY = "StructureTemplates";

    private static final String SEARCH = "SearchTemplates";

    private static final String CREATION = "CreationTemplates";

    private static final String CLASS = "class";

    private static final String JAVACLASS = "javaclass";

    private static final String ATTRNAME = "name";

    private static final String OC_JC_MAP = "oc_jc_map";

    private static ConfigManagerUMS _instance = null;

    static Hashtable _cch = null;

    private static final Object lock_cch = new Object();

    private static Cache _checkListCache = null;

    private static AuthPrincipal _principal;

    private static ServiceConfigManager _smapi;

    private static String _rootDN;

    private static DN _root;

    private static CMListener _listener;

    private static String _lid; // Listener ID

    private static Debug _debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);

    private static String[][] _oc_jc_map; // separate cache for oc_jc_map
}
