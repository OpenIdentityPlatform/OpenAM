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
 * $Id: OpenSSOApplicationPrivilegeManager.java,v 1.16 2010/01/11 20:15:46 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferredApplication;
import com.sun.identity.entitlement.ReferredApplicationManager;
import com.sun.identity.entitlement.RegExResourceName;
import com.sun.identity.entitlement.ResourceMatch;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.entitlement.SubjectImplementation;
import com.sun.identity.entitlement.TimeCondition;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.Subject;

public class OpenSSOApplicationPrivilegeManager extends
    ApplicationPrivilegeManager {
    private static final String RESOURCE_PREFIX =
        "/sunEntitlementService/1.0/application/default/application";
    private static final String APPL_NAME = 
        DelegationManager.DELEGATION_SERVICE;
    private static final String SUN_AM_REALM_RESOURCE =
        "sms://*{0}/sunAMRealmService/*";
    private static final String SUN_IDREPO_RESOURCE =
        "sms://*{0}/sunIdentityRepositoryService/1.0/application/*";

    private static final String HIDDEN_REALM_DN =
        "o=sunamhiddenrealmdelegationservicepermissions,ou=services,";
    private static final String GHOST_PRIVILEGE_NAME_PREFIX = "^^";
    private static final RegExResourceName regExComparator = new
        RegExResourceName();

    private String realm;
    private Subject caller;
    private boolean bPolicyAdmin;
    private Permission delegatables;
    private Permission readables;
    private Permission modifiables;
    private String resourcePrefix;

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject dsameUserSubject = SubjectUtils.createSubject(adminToken);

    public OpenSSOApplicationPrivilegeManager(String realm, Subject caller)
        throws EntitlementException {
        super();
        this.realm = realm;
        this.caller = caller;
        bPolicyAdmin = isPolicyAdmin();
        init();
    }

    public Set<String> getDelegatableResourceNames(String applicationName) {
        Set<String> resourceNames = delegatables.getResourceNames(
            applicationName);
        return (resourceNames == null) ? Collections.EMPTY_SET :
            resourceNames;
    }

    public void addPrivilege(ApplicationPrivilege appPrivilege)
        throws EntitlementException {
        validatePrivilege(appPrivilege);
        Privilege[] privileges = toPrivilege(appPrivilege);
        PrivilegeManager pm = PrivilegeManager.getInstance(getHiddenRealmDN(),
            dsameUserSubject);

        for (Privilege p : privileges) {
            pm.addPrivilege(p);
        }
        
        cachePrivilege(privileges[0]);
    }

    private void validatePrivilege(ApplicationPrivilege appPrivilege)
        throws EntitlementException {
        Set<String> applicationNames = appPrivilege.getApplicationNames();
        if ((applicationNames == null) || applicationNames.isEmpty()) {
            throw new EntitlementException(320);
        }

        for (String n : applicationNames) {
            Application application = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, realm, n);
            if (application == null) {
                String[] params = {n};
                throw new EntitlementException(321, params);
            }

            Set<String> resources = appPrivilege.getResourceNames(n);
            if ((resources == null) || resources.isEmpty()) {
                throw new EntitlementException(322);
            }
            for (String r : resources) {
                if (!isDelegatableResource(application, r)) {
                    String[] params = {r};
                    throw new EntitlementException(323, params);
                }
            }
        }
    }

    public void removePrivilege(String name) throws EntitlementException {
        if (isDsameUser() || delegatables.hasPrivilege(name)) {
            PrivilegeManager pm = PrivilegeManager.getInstance(
                getHiddenRealmDN(), dsameUserSubject);
            pm.removePrivilege(name);
            pm.removePrivilege(GHOST_PRIVILEGE_NAME_PREFIX + name);
            readables.removePrivilege(name);
            modifiables.removePrivilege(name);
            delegatables.removePrivilege(name);
        } else {
            throw new EntitlementException(326);
        }
    }

    public void replacePrivilege(ApplicationPrivilege appPrivilege)
        throws EntitlementException {
        if (delegatables.hasPrivilege(appPrivilege.getName())) {
            validatePrivilege(appPrivilege);
            Privilege[] privileges = toPrivilege(appPrivilege);
            PrivilegeManager pm = PrivilegeManager.getInstance(
                getHiddenRealmDN(), dsameUserSubject);
            pm.modifyPrivilege(privileges[0]);
            cachePrivilege(privileges[0]);
            pm.modifyPrivilege(privileges[1]);
            cachePrivilege(privileges[1]);
        } else {
            throw new EntitlementException(326);
        }
    }

    private void cachePrivilege(Privilege p) {
        readables.evaluate(p);
        modifiables.evaluate(p);
        delegatables.evaluate(p);
    }

    /**
     * Creates two privileges here
     */
    private Privilege[] toPrivilege(ApplicationPrivilege appPrivilege)
        throws EntitlementException {
        Privilege[] results = new Privilege[2];
        try {
            Privilege actualP = Privilege.getNewInstance();
            actualP.setName(appPrivilege.getName());
            actualP.setDescription(appPrivilege.getDescription());
            Set<String> res = createDelegationResources(appPrivilege);
            Entitlement entitlement = new Entitlement(APPL_NAME, res,
                getActionValues(appPrivilege.getActionValues()));
            actualP.setEntitlement(entitlement);

            Privilege ghostP = Privilege.getNewInstance();
            ghostP.setName(GHOST_PRIVILEGE_NAME_PREFIX +
                appPrivilege.getName());
            Set<String> ghostRes = new HashSet<String>();

            String currentOrgDN = DNMapper.orgNameToDN(realm);
            Object[] param = {currentOrgDN};

            ghostRes.add(MessageFormat.format(SUN_AM_REALM_RESOURCE, param));
            ghostRes.add(MessageFormat.format(SUN_IDREPO_RESOURCE, param));
            entitlement = new Entitlement(APPL_NAME, ghostRes,
                getActionValues(ApplicationPrivilege.PossibleAction.READ));
            ghostP.setEntitlement(entitlement);
            
            Set<SubjectImplementation> subjects = appPrivilege.getSubjects();
            Set<EntitlementSubject> eSubjects = new 
                HashSet<EntitlementSubject>();
            for (SubjectImplementation i : subjects) {
                eSubjects.add((EntitlementSubject)i);
            }                        
            OrSubject orSubject = new OrSubject(eSubjects);
            actualP.setSubject(orSubject);
            actualP.setCondition(appPrivilege.getCondition());

            ghostP.setSubject(orSubject);
            ghostP.setCondition(appPrivilege.getCondition());

            Set<String> applIndexes = new HashSet<String>();
            applIndexes.addAll(appPrivilege.getApplicationNames());
            actualP.setApplicationIndexes(applIndexes);

            results[0] = actualP;
            results[1] = ghostP;
        } catch (UnsupportedEncodingException ex) {
            String[] params = {};
            throw new EntitlementException(324, params);
        }
        return results;
    }

    private Map<String, Boolean> getActionValues(
        ApplicationPrivilege.PossibleAction actions) {
        Map<String, Boolean> map = new HashMap<String, Boolean>();

        switch (actions) {
            case READ:
                map.put(ACTION_READ, true);
                break;
            case READ_MODIFY:
                map.put(ACTION_READ, true);
                map.put(ACTION_MODIFY, true);
                break;
            case READ_MODIFY_DELEGATE:
                map.put(ACTION_READ, true);
                map.put(ACTION_MODIFY, true);
                map.put(ACTION_DELEGATE, true);
                break;
            case READ_DELEGATE:
                map.put(ACTION_READ, true);
                map.put(ACTION_DELEGATE, true);
                break;
        }

        return map;
    }

    private ApplicationPrivilege.PossibleAction getActionValues(
        Map<String, Boolean> map) {

        Boolean bRead = map.get(ACTION_READ);
        boolean read = (bRead != null) && bRead.booleanValue();
        Boolean bModify = map.get(ACTION_MODIFY);
        boolean modify = (bModify != null) && bModify.booleanValue();
        Boolean bDelegate = map.get(ACTION_DELEGATE);
        boolean delegate = (bDelegate != null) && bDelegate.booleanValue();

        if (read && modify && delegate) {
            return ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE;
        }
        if (read && delegate) {
            return ApplicationPrivilege.PossibleAction.READ_DELEGATE;
        }
        if (read && modify) {
            return ApplicationPrivilege.PossibleAction.READ_MODIFY;
        }

        return ApplicationPrivilege.PossibleAction.READ;
    }

    private ApplicationPrivilege toApplicationPrivilege(Privilege p) 
        throws EntitlementException {
        ApplicationPrivilege ap = new ApplicationPrivilege(p.getName());
        ap.setDescription(p.getDescription());
        ap.setCreatedBy(p.getCreatedBy());
        ap.setCreationDate(p.getCreationDate());
        ap.setLastModifiedBy(p.getLastModifiedBy());
        ap.setLastModifiedDate(p.getLastModifiedDate());
        Entitlement ent = p.getEntitlement();
        Set<String> resourceNames = ent.getResourceNames();
        Map<String, Set<String>> mapAppToRes =
            getApplicationPrivilegeResourceNames(resourceNames);
        ap.setApplicationResources(mapAppToRes);
        ap.setActionValues(getActionValues(ent.getActionValues()));

        Set<SubjectImplementation> subjects = new
            HashSet<SubjectImplementation>();
        if (p.getSubject() instanceof OrSubject) {
            OrSubject orSubject = (OrSubject)p.getSubject();
            for (EntitlementSubject es : orSubject.getESubjects()) {
                if (es instanceof SubjectImplementation) {
                    subjects.add((SubjectImplementation)es);
                }
            }
        } else if (p.getSubject() instanceof SubjectImplementation) {
            subjects.add((SubjectImplementation)p.getSubject());
        }

        ap.setSubject(subjects);
        EntitlementCondition cond = p.getCondition();
        if (cond instanceof TimeCondition) {
            ap.setCondition((TimeCondition)cond);
        }
        return ap;
    }



    private Set<String> createDelegationResources(ApplicationPrivilege ap)
        throws UnsupportedEncodingException {
        Set<String> results = new HashSet<String>();
        Set<String> applicationNames = ap.getApplicationNames();
        for (String name : applicationNames) {
            results.add(createDelegationResources(name, ap.getResourceNames(
                name)));
        }
        return results;
    }

    private String createDelegationResources(
        String applicationName,
        Set<String> res) throws UnsupportedEncodingException {
        StringBuilder buff = new StringBuilder();
        buff.append(resourcePrefix).append("/").append(applicationName).append(
            "?");
        boolean first = true;

        for (String r : res) {
            if (first) {
                first = false;
            } else {
                buff.append("&");
            }
            buff.append(URLEncoder.encode(r, "UTF-8"));
        }
        return buff.toString();
    }

    private boolean isDelegatableResource(Application appl, String res) {
        Set<String> resources = getDelegatableResourceNames(appl.getName());

        if ((resources != null) && !resources.isEmpty()) {
            ResourceName resComp = appl.getResourceComparator();
            boolean isRegEx = (resComp instanceof RegExResourceName);

            for (String r : resources) {
                if (!r.endsWith("*")) {
                    if (!r.endsWith("/")) {
                        r += "/";
                    }
                    r += "*";
                }

                if (isRegEx) {
                    ResourceMatch result = resComp.compare(res, r, true);
                    if (result.equals(ResourceMatch.EXACT_MATCH) ||
                        result.equals(ResourceMatch.SUB_RESOURCE_MATCH) ||
                        result.equals(ResourceMatch.WILDCARD_MATCH)) {
                        return true;
                    }
                } else {
                    ResourceMatch result = resComp.compare(r, res, false);
                    if (result.equals(ResourceMatch.EXACT_MATCH) ||
                        result.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                        return true;
                    }
                    result = resComp.compare(res, r, true);
                    if (result.equals(ResourceMatch.EXACT_MATCH) ||
                        result.equals(ResourceMatch.SUB_RESOURCE_MATCH) ||
                        result.equals(ResourceMatch.WILDCARD_MATCH)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ApplicationPrivilege getPrivilege(String name)
        throws EntitlementException {
        Privilege privilege = delegatables.getPrivilege(name);
        if (privilege == null) {
            String[] param = {name};
            throw new EntitlementException(325, param);
        }
        return toApplicationPrivilege(privilege);
    }

    
    @Override
    public Set<String> search(Set<SearchFilter> filters) {
        Set<String> names = new HashSet<String>();
        Set<String> allNames = delegatables.getPrivilegeNames();

        if ((filters == null) || filters.isEmpty()) {
            names.addAll(allNames);
        } else {
            for (String name : allNames) {
                Privilege p = delegatables.getPrivilege(name);
                if (matchFilter(p, filters)) {
                    names.add(name);
                }
            }
        }

        return names;
    }

    private boolean matchFilter(Privilege p, Set<SearchFilter> filters) {
        for (SearchFilter filter : filters) {
            filter.getFilter();
            String filterName = filter.getName();

            if (filterName.equals(Privilege.NAME_ATTRIBUTE)) {
                if (attrCompare(p.getName(), filter)) {
                    return true;
                }
            } else if (filterName.equals(Privilege.DESCRIPTION_ATTRIBUTE)) {
                if (attrCompare(p.getDescription(), filter)) {
                    return true;
                }
            } else if (filterName.equals(Privilege.CREATED_BY_ATTRIBUTE)) {
                if (attrCompare(p.getCreatedBy(), filter)) {
                    return true;
                }
            } else if (filterName.equals(Privilege.LAST_MODIFIED_BY_ATTRIBUTE)) {
                if (attrCompare(p.getLastModifiedBy(), filter)) {
                    return true;
                }
            } else if (filterName.equals(Privilege.CREATION_DATE_ATTRIBUTE)) {
                if (attrCompare(p.getCreationDate(), filter)) {
                    return true;
                }
            } else if (filterName.equals(
                Privilege.LAST_MODIFIED_DATE_ATTRIBUTE)) {
                if (attrCompare(p.getLastModifiedDate(), filter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attrCompare(String value, SearchFilter filter) {
        String pattern = filter.getValue();
        if (pattern != null) {
            if (pattern.equalsIgnoreCase(value) ||
                DisplayUtils.wildcardMatch(value, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean attrCompare(long value, SearchFilter filter) {
        long filterLong = filter.getNumericValue();
        SearchFilter.Operator operator = filter.getOperator();

        if (operator == SearchFilter.Operator.EQUAL_OPERATOR) {
            return (filterLong == value);
        }
        if (operator == SearchFilter.Operator.GREATER_THAN_OPERATOR) {
            return (value > filterLong);
        }

        return (value < filterLong);
    }

    private void init()
        throws EntitlementException {
        resourcePrefix = "sms://" + DNMapper.orgNameToDN(realm) +
            RESOURCE_PREFIX;
        resourcePrefix = resourcePrefix.toLowerCase();
        initPrivilegeNames();
    }

    private void initPrivilegeNames()
        throws EntitlementException {
        initPermissionObjects();
        getPrivileges();
        getSubResourceRelatedPrivileges();
    }
    
    private void getSubResourceRelatedPrivileges() throws EntitlementException {
        if (!bPolicyAdmin) {
            Set<String> applNames = new HashSet<String>();
            applNames.addAll(readables.getApplications());
            applNames.addAll(modifiables.getApplications());
            applNames.addAll(delegatables.getApplications());
            
            if (!applNames.isEmpty()) {
                Set<String> hostIndex = new HashSet<String>();
                hostIndex.add("://" + DNMapper.orgNameToDN(realm));
                Set<String> pathParentIndexes = new HashSet<String>();
                
                for (String applName : applNames) {
                    pathParentIndexes.add(RESOURCE_PREFIX + "/" + applName);
                }
                ResourceSearchIndexes rIndex = new ResourceSearchIndexes(
                    hostIndex, null, pathParentIndexes);
                OpenSSOIndexStore db = new OpenSSOIndexStore(dsameUserSubject,
                            getHiddenRealmDN());
                Iterator<IPrivilege> results = db.search("/", rIndex,
                    Collections.EMPTY_SET, true, false);

                while (results.hasNext()) {
                    Privilege p = (Privilege) results.next();
                    delegatables.evaluate(p, true);
                    modifiables.evaluate(p, true);
                    readables.evaluate(p, true);
                }
            }
        }
    }

    private void getPrivileges() throws EntitlementException {
        Set<String> hostIndex = new HashSet<String>();
        hostIndex.add("://" + DNMapper.orgNameToDN(realm));
        Set<String> pathParentIndex = new HashSet<String>();
        pathParentIndex.add(RESOURCE_PREFIX);
        ResourceSearchIndexes rIndex = new ResourceSearchIndexes(
            hostIndex, null, pathParentIndex);
        SubjectAttributesManager sam = SubjectAttributesManager.getInstance(
            dsameUserSubject);

        Set<String> subjectIndex = (bPolicyAdmin) ? Collections.EMPTY_SET :
            sam.getSubjectSearchFilter(caller, APPL_NAME);
        OpenSSOIndexStore db = new OpenSSOIndexStore(dsameUserSubject,
            getHiddenRealmDN());
        Iterator<IPrivilege> results = db.search("/", rIndex, subjectIndex,
            true, false);

        while (results.hasNext()) {
            Privilege p = (Privilege) results.next();

            if (bPolicyAdmin || doesSubjectMatch(p, resourcePrefix)) {
                delegatables.evaluate(p);
                modifiables.evaluate(p);
                readables.evaluate(p);
            }
        }
    }

    private boolean doesSubjectMatch(
        Privilege privilege,
        String resourceName
   ) throws EntitlementException {
        SubjectAttributesManager mgr =
            SubjectAttributesManager.getInstance(dsameUserSubject, realm);
        SubjectDecision sDecision = privilege.getSubject().evaluate(realm,
            mgr, caller, resourceName, Collections.EMPTY_MAP);
        return sDecision.isSatisfied();
    }

    private void initPermissionObjects() throws EntitlementException {
        Set<String> actions = new HashSet<String>();
        actions.add(ACTION_READ);
        actions.add(ACTION_DELEGATE);
        delegatables = new Permission(actions, bPolicyAdmin,
            resourcePrefix);

        actions.clear();
        actions.add(ACTION_READ);
        actions.add(ACTION_MODIFY);
        modifiables = new Permission(actions, bPolicyAdmin,
            resourcePrefix);

        actions.clear();
        actions.add(ACTION_READ);
        readables = new Permission(actions, bPolicyAdmin,
            resourcePrefix);
    }

    private void addToMap(
        Map<String, Set<String>> map1,
        Map<String, Set<String>> map2) {
        if ((map2 != null) && !map2.isEmpty()) {
            for (String key2 : map2.keySet()) {
                Set<String> set1 = map1.get(key2);
                Set<String> set2 = map2.get(key2);

                if ((set1 == null) || set1.isEmpty()) {
                    map1.put(key2, set2);
                } else {
                    set1.addAll(set2);
                }
            }
        }
    }

    private static Map<String, Set<String>>
        getApplicationPrivilegeResourceNames(Set<String> resources) {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        for (String r : resources) {
            Map<String, Set<String>> map =
                getApplicationPrivilegeResourceNames(r);
            if ((map != null) && !map.isEmpty()) {
                results.putAll(map);
            }
        }
        return results;
    }

    private static Map<String, Set<String>>
        getApplicationPrivilegeResourceNames(String res) {
        int idx = res.indexOf('?');
        if (idx == -1) {
            return Collections.EMPTY_MAP;
        }
        String applicationName = res.substring(0, idx);
        int idx2 = applicationName.lastIndexOf("/");
        if (idx2 != -1) {
            applicationName = applicationName.substring(idx2 +1);
        }
        res = res.substring(idx+1);
        Set<String> resources = new HashSet<String>();

        StringTokenizer st = new StringTokenizer(res, "&");
        while (st.hasMoreTokens()) {
            try {
                String s = st.nextToken();
                resources.add(URLDecoder.decode(s, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                PrivilegeManager.debug.error(
                    "OpenSSOApplicationPrivilegeManager " +
                        ".getApplicationPrivilegeResourceNames", ex);
                return Collections.EMPTY_MAP;
            }
        }

        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(applicationName, resources);
        return map;
    }


    private boolean isPolicyAdmin() {
        if (isDsameUser()) {
            return true;
        }
        Subject adminSubject = SubjectUtils.createSuperAdminSubject();
        try {
            Evaluator eval = new Evaluator(adminSubject, APPL_NAME);
            Set<String> actions = new HashSet<String>();
            actions.add(ACTION_MODIFY);
            String res = "sms://" + DNMapper.orgNameToDN(realm) +
                "/iPlanetAMPolicyService/*";
            Entitlement e = new Entitlement(res, actions);
            return eval.hasEntitlement(getHiddenRealmDN(),
                caller, e, Collections.EMPTY_MAP);
        } catch (EntitlementException ex) {
            PrivilegeManager.debug.error(
                "OpenSSOApplicationPrivilegeManager.isPolicyAdmin", ex);
            return false;
        }
    }

    private boolean isDsameUser() {
        if (caller == PrivilegeManager.superAdminSubject) {
            return true;
        }
        Set<Principal> principals = caller.getPrincipals();
        if ((principals == null) || principals.isEmpty()) {
            return false;
        }

        String dsameuserDN = "id=dsameuser,ou=user," +
            SMSEntry.getRootSuffix();
        String adminUser = "id=amadmin,ou=user," +
            SMSEntry.getRootSuffix();
        Principal p = principals.iterator().next();
        String principalName = p.getName();
        
        if (DN.isDN(principalName)) {
            DN principalDN = new DN(p.getName());
            DN adminDN = new DN(adminUser);

            if (principalDN.equals(adminDN)) {
                return true;
            }
            DN dsameuser = new DN(dsameuserDN);

            if (principalDN.equals(dsameuser)) {
                return true;
            }
        }

        return false;
    }

    private static String getHiddenRealmDN() {
        return HIDDEN_REALM_DN + SMSEntry.getRootSuffix();
    }

    @Override
    public boolean hasPrivilege(
        Privilege p,
        ApplicationPrivilege.Action action
    ) throws EntitlementException {
        if (isPolicyAdmin()) {
            return true;
        }
        Permission permission = getPermissionObject(action);
        return permission.hasPermission(p);
    }

    @Override
    public boolean hasPrivilege(
        ReferralPrivilege p,
        ApplicationPrivilege.Action action
    ) throws EntitlementException {
        if (isPolicyAdmin()) {
            return true;
        }
        Permission permission = getPermissionObject(action);
        return permission.hasPermission(p);
    }

    @Override
    public boolean hasPrivilege(
        Application app,
        ApplicationPrivilege.Action action
    ) throws EntitlementException {
        if (action.equals(ApplicationPrivilege.Action.READ)) {
            if (isReferredApplication(app)) {
                return true;
            }
            if (isPolicyAdmin()) {
                return true;
            }

            Permission permission = getPermissionObject(action);
            return permission.hasPermission(app);
        } else {
            if (isReferredApplication(app)) {
                return false;
            }
            return isPolicyAdmin();
        }
    }

    private boolean isReferredApplication(Application app)
        throws EntitlementException {
        Set<ReferredApplication> appls =
            ReferredApplicationManager.getInstance().getReferredApplications(
            realm);
        String name = app.getName();
        for (ReferredApplication a : appls) {
            if (a.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getResources(String applicationName,
        ApplicationPrivilege.Action action) {
        Permission p = getPermissionObject(action);
        return p.getResourceNames(applicationName);
    }

    @Override
    public Set<String> getApplications(ApplicationPrivilege.Action action) {
        Permission p = getPermissionObject(action);
        return p.getApplications();
    }

    /**
     * Returns <code>true</code> if subject can create application.
     *
     * @param realm Realm where application is to be created.
     */
    public boolean canCreateApplication(String realm) {
        return isPolicyAdmin();
    }

    private Permission getPermissionObject(ApplicationPrivilege.Action action)
    {
        Permission p = readables;
        if (action == ApplicationPrivilege.Action.MODIFY) {
            p = modifiables;
        } else if (action == ApplicationPrivilege.Action.DELEGATE) {
            p =delegatables;
        }
        return p;
    }

    static Iterator<IPrivilege> getPrivileges(String realm) throws EntitlementException {
        Set<String> hostIndex = new HashSet<String>();
        hostIndex.add("://" + DNMapper.orgNameToDN(realm));
        Set<String> pathParentIndex = new HashSet<String>();
        pathParentIndex.add(RESOURCE_PREFIX);
        ResourceSearchIndexes rIndex = new ResourceSearchIndexes(
            hostIndex, null, pathParentIndex);
        Set<String> subjectIndex = Collections.EMPTY_SET;

        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        Subject dsameUserSubject = SubjectUtils.createSubject(adminToken);

        OpenSSOIndexStore db = new OpenSSOIndexStore(dsameUserSubject,
            getHiddenRealmDN());
        return db.search("/", rIndex, subjectIndex, true, false);
    }

    static void removeAllPrivileges(
        String realm
    ) throws EntitlementException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        Subject dsameUserSubject = SubjectUtils.createSubject(adminToken);

        for (Iterator<IPrivilege> i = getPrivileges(realm) ; i.hasNext(); ) {
            Privilege p = (Privilege)i.next();
            String name = p.getName();
            PrivilegeManager pm = PrivilegeManager.getInstance(
                getHiddenRealmDN(), dsameUserSubject);
            pm.removePrivilege(name);
            pm.removePrivilege(GHOST_PRIVILEGE_NAME_PREFIX + name);
        }
    }


    private class Permission {
        private Map<String, Privilege> privileges;
        private Map<String, Set<String>> appNameToResourceNames;
        private Set<String> actions;
        private boolean bPolicyAdmin;
        private String resourcePrefix;

        private Permission(Set<String> action, boolean bPolicyAdmin,
            String resourcePrefix) throws EntitlementException {
            this.actions = new HashSet<String>();
            this.actions.addAll(action);
            this.bPolicyAdmin = bPolicyAdmin;
            this.resourcePrefix = resourcePrefix;
            privileges = new HashMap<String, Privilege>();
            appNameToResourceNames = new HashMap<String, Set<String>>();

            if (bPolicyAdmin) {
                appNameToResourceNames.putAll(getAllResourceNamesInAllAppls());
            }
        }

        private Set<String> getApplications() {
            if (appNameToResourceNames.isEmpty()) {
                return Collections.EMPTY_SET;
            }
            Set<String> results = new HashSet<String>();
            results.addAll(appNameToResourceNames.keySet());
            return results;
        }

        private boolean hasPrivilege(String name) {
            return privileges.keySet().contains(name);
        }

        private void removePrivilege(String name) {
            privileges.remove(name);
        }

        private Privilege getPrivilege(String name) {
            return privileges.get(name);
        }

        private Set<String> getPrivilegeNames() {
            return privileges.keySet();
        }

        private Set<String> getResourceNames(String applName) {
            return appNameToResourceNames.get(applName);
        }

        private Map<String, Set<String>> getAllResourceNamesInAllAppls() 
            throws EntitlementException {
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
            Set<String> applNames = ApplicationManager.getApplicationNames(
                PrivilegeManager.superAdminSubject, realm);

            for (String s : applNames) {
                Application appl = ApplicationManager.getApplication(
                    PrivilegeManager.superAdminSubject, realm, s);
                map.put(s, appl.getResources());
            }
            return map;
        }

        private void evaluate(Privilege p, boolean subResource) {
            if (!privileges.keySet().contains(p.getName())) {
                Map<String, Set<String>> mapAppToRes = getResourceNames(p);

                for (String app : mapAppToRes.keySet()) {
                    if (isSubResource(app, mapAppToRes.get(app))) {
                        addToMap(appNameToResourceNames, mapAppToRes);
                        privileges.put(p.getName(), p);
                    }
                }
            }
        }

        private boolean isSubResource(String app, Set<String> targets) {
            Set<String> resources = appNameToResourceNames.get(app);
            if ((resources == null) || resources.isEmpty()) {
                return false;
            }

            for (String t : targets) {
                if (!isSubResource(resources, t)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isSubResource(Set<String> resources, String target) {
            for (String r : resources) {
                if (r.endsWith("/*")) {
                    r = r.substring(0, r.length()-2);
                }
                ResourceMatch m = regExComparator.compare(r, target, true);
                if (m.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                    return true;
                }
            }
            return false;
        }

        private void evaluate(Privilege p) {
            Map<String, Boolean> actionValues =
                p.getEntitlement().getActionValues();
            boolean desiredAction = bPolicyAdmin;

            if (!desiredAction) {
                for (String action : actions) {
                    Boolean result = actionValues.get(action);
                    desiredAction = (result != null) && result.booleanValue();
                    if (!desiredAction) {
                        break;
                    }
                }
            }

            if (desiredAction) {
                Map<String, Set<String>> map = getResourceNames(p);

                if ((map != null) && !map.isEmpty()) {
                    if (!bPolicyAdmin) {
                        addToMap(appNameToResourceNames, map);
                    }
                    privileges.put(p.getName(), p);
                }
            }
        }

        private Map<String, Set<String>> getResourceNames(Privilege p) {
            Entitlement ent = p.getEntitlement();

            for (String res : ent.getResourceNames()) {
                String lc = res.toLowerCase();
                if (!lc.startsWith(resourcePrefix)) {
                    return Collections.EMPTY_MAP;
                }
            }

            return getApplicationPrivilegeResourceNames(
                ent.getResourceNames());
        }

        private boolean hasPermission(Application application)
            throws EntitlementException {
            return appNameToResourceNames.containsKey(application.getName());
        }

        private boolean hasPermission(Privilege privilege) 
            throws EntitlementException {
            Entitlement ent = privilege.getEntitlement();
            String applName = ent.getApplicationName();
            Application appl = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, realm, applName);
            if (appl == null) {
                return false;
            }
            ResourceName resComp = appl.getResourceComparator();
            Set<String> pResources = ent.getResourceNames();

            Set<String> resources = appNameToResourceNames.get(applName);
            if ((resources == null) || resources.isEmpty()) {
                return false;
            }

            for (String r : pResources) {
                if (!isSubResource(resComp, resources, r)) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasPermission(ReferralPrivilege privilege) 
            throws EntitlementException {
            Map<String, Set<String>> map =
                privilege.getMapApplNameToResources();

            Set<String> applicationNames = map.keySet();
            for (String applName : applicationNames) {
                Application appl = ApplicationManager.getApplication(
                    PrivilegeManager.superAdminSubject, realm, applName);
                if (appl == null) {
                    return false;
                }
                ResourceName resComp = appl.getResourceComparator();
                Set<String> pResources = map.get(applName);

                Set<String> resources = appNameToResourceNames.get(applName);
                if ((resources == null) || resources.isEmpty()) {
                    return false;
                }

                for (String r : pResources) {
                    if (!isSubResource(resComp, resources, r)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isSubResource(
            ResourceName resComp,
            Set<String> resources,
            String res
        ) {
            for (String s : resources) {
                ResourceMatch result = resComp.compare(s, res, false);
                if (result.equals(ResourceMatch.EXACT_MATCH) ||
                    result.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                    return true;
                }
                result = resComp.compare(res, s, true);
                if (result.equals(ResourceMatch.WILDCARD_MATCH)) {
                    return true;
                }
            }
            return false;
        }
    }
}
