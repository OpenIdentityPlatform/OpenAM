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
 * $Id: ApplicationImpl.java,v 1.2 2009/09/25 05:52:53 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public class ApplicationImpl extends AuthenticatedCommand {
    public static final String PARAM_APPL_TYPE_NAME = "applicationtype";
    public static final String PARAM_APPL_NAME = "name";
    public static final String PARAM_APPL_NAMES = "names";
    public static final String ATTR_APPLICATIONTYPE = "applicationType";
    public static final String ATTR_ACTIONS = "actions";
    public static final String ATTR_RESOURCES = "resources";
    public static final String ATTR_SUBJECTS = "subjects";
    public static final String ATTR_CONDITIONS = "conditions";
    public static final String ATTR_ENTITLEMENT_COMBINER = 
        "entitlementCombiner";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_SUBJECT_ATTRIBUTE_NAMES =
        "subjectAttributeNames";
    public static final String ATTR_RESOURCE_COMPARATOR =
        "resourceComparator";
    public static final String ATTR_SAVE_INDEX = "saveIndexImpl";
    public static final String ATTR_SEARCH_INDEX = "searchIndexImpl";
    public static final String ATTR_CREATED_BY = "createdBy";
    public static final String ATTR_CREATION_DATE = "creationDate";
    public static final String ATTR_LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String ATTR_LAST_MODIFICATION_DATE = "lastModifiedDate";

    private Subject adminSubject;
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
    }

    protected static String getString(
        String key, Map<String, Set<String>> map) {
        Set<String> set = map.get(key);
        return ((set == null) || set.isEmpty()) ? null : set.iterator().next();
    }
    
    protected Subject getAdminSubject() {
        if (adminSubject == null) {
            adminSubject = SubjectUtils.createSubject(getAdminSSOToken());
        }
        return adminSubject;
    }

    protected ApplicationType getApplicationType(String name)
        throws CLIException {
        ApplicationType applType =
            ApplicationTypeManager.getAppplicationType(getAdminSubject(), name);
        if (applType == null) {
            String msg = getResourceString("application-type-invalid");
            Object[] param = {name};
            throw new CLIException(MessageFormat.format(msg, param),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        return applType;
    }

    protected void setApplicationAttributes(Application appl,
        Map<String, Set<String>> attributeValues,
        boolean bCreate) throws CLIException {

        Map<String, Boolean> actions = getActions(attributeValues);
        if (actions != null) {
            appl.setActions(actions);
        }

        Set<String> resources = attributeValues.get(ATTR_RESOURCES);
        if ((resources == null) || resources.isEmpty()) {
            if (bCreate) {
                throw new CLIException(getResourceString("resources-required"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } else {
            appl.setResources(resources);
        }

        Set<String> subjects = attributeValues.get(ATTR_SUBJECTS);
        if ((subjects == null) || subjects.isEmpty()) {
            if (bCreate) {
                throw new CLIException(getResourceString("subjects-required"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } else {
            appl.setSubjects(subjects);
        }

        Set<String> conditions = attributeValues.get(ATTR_CONDITIONS);
        if ((conditions == null) || conditions.isEmpty()) {
            if (bCreate) {
                throw new CLIException(getResourceString("conditions-required"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } else {
            appl.setConditions(conditions);
        }

        Class entitlementCombiner = getEntitlementCombiner(
            attributeValues);
        if (entitlementCombiner == null) {
            if (bCreate) {
                throw new CLIException(getResourceString(
                    "entitlement-combiner-required"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } else {
            appl.setEntitlementCombiner(entitlementCombiner);
        }

        Set<String> names = attributeValues.get(ATTR_SUBJECT_ATTRIBUTE_NAMES);
        if ((names != null) && !names.isEmpty()) {
            appl.setAttributeNames(names);
        }
        
        Class resourceComparator = getResourceComparator(attributeValues);
        if (resourceComparator != null) {
            try {
                appl.setResourceComparator(resourceComparator);
            } catch (InstantiationException ex) {
                throw new CLIException(ex,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            } catch (IllegalAccessException ex) {
                throw new CLIException(ex,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }

        Class saveIndex = getSaveIndex(attributeValues);
        if (saveIndex != null) {
            try {
                appl.setSaveIndex(saveIndex);
            } catch (InstantiationException ex) {
                throw new CLIException(ex,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            } catch (IllegalAccessException ex) {
                throw new CLIException(ex,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }

        Class searchIndex = getSearchIndex(attributeValues);
        if (searchIndex != null) {
            try {
                appl.setSearchIndex(searchIndex);
            } catch (InstantiationException ex) {
                throw new CLIException(ex,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            } catch (IllegalAccessException ex) {
                throw new CLIException(ex,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }

    private Map<String, Boolean> getActions(
        Map<String, Set<String>> attributeValues) {
        Set<String> actions = attributeValues.get(ATTR_ACTIONS);
        if ((actions == null) || actions.isEmpty()) {
            return null;
        }
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        for (String action : actions) {
            action = action.trim();
            int idx = action.indexOf('=');
            if (idx == -1) {
                results.put(action, true);
            } else {
                String a = action.substring(0, idx);
                String v = action.substring(idx+1);
                results.put(a, Boolean.parseBoolean(v));
            }
        }
        return results;
    }

    private Class getEntitlementCombiner(
        Map<String, Set<String>> attributeValues) throws CLIException {
        String comb = getString(ATTR_ENTITLEMENT_COMBINER,
            attributeValues);
        if ((comb == null) || (comb.trim().length() == 0)) {
            return null;
        }
        try {
            Class clazz = Class.forName(comb);
            Class superClasses = clazz.getSuperclass();
            boolean found = false;
            while ((superClasses != null) && !found) {
                if (superClasses.equals(EntitlementCombiner.class)) {
                    found = true;
                } else {
                    superClasses = clazz.getSuperclass();
                }
            }

            if (found) {
                return clazz;
            }

            Object[] params = {comb};
            throw new CLIException(MessageFormat.format(
                "entitlement-combiner-does-not-extend-superclass",
                params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);

        } catch (ClassNotFoundException ex) {
            Object[] params = {comb};
            throw new CLIException(MessageFormat.format(
                "entitlement-combiner-class-not-found",
                params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private Class getResourceComparator(
        Map<String, Set<String>> attributeValues)
        throws CLIException {
        String comp = getString(ATTR_RESOURCE_COMPARATOR,
            attributeValues);
        if ((comp == null) || (comp.trim().length() == 0)) {
            return null;
        }
        try {
            Class clazz = Class.forName(comp);
            Object obj = clazz.newInstance();

            if (obj instanceof ResourceName) {
                return clazz;
            }

            Object[] params = {comp};
            throw new CLIException(MessageFormat.format(
                "resource-comparator-does-not-extend-interface", params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (InstantiationException ex) {
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalAccessException ex) {
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ClassNotFoundException ex) {
            Object[] params = {comp};
            throw new CLIException(MessageFormat.format(
                "resource-comparator-class-not-found", params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private Class getSaveIndex(
        Map<String, Set<String>> attributeValues)
        throws CLIException {
        String saveIndex = getString(ATTR_SAVE_INDEX, attributeValues);
        if ((saveIndex == null) || (saveIndex.trim().length() == 0)) {
            return null;
        }
        try {
            Class clazz = Class.forName(saveIndex);
            Object obj = clazz.newInstance();

            if (obj instanceof ISaveIndex) {
                return clazz;
            }

            Object[] params = {saveIndex};
            throw new CLIException(MessageFormat.format(
                "save-index-does-not-extend-interface", params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (InstantiationException ex) {
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalAccessException ex) {
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ClassNotFoundException ex) {
            Object[] params = {saveIndex};
            throw new CLIException(MessageFormat.format(
                "save-index-class-not-found", params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private Class getSearchIndex(
        Map<String, Set<String>> attributeValues)
        throws CLIException {
        String searchIndex = getString(ATTR_SEARCH_INDEX, attributeValues);
        if ((searchIndex == null) || (searchIndex.trim().length() == 0)) {
            return null;
        }
        try {
            Class clazz = Class.forName(searchIndex);
            Object obj = clazz.newInstance();

            if (obj instanceof ISearchIndex) {
                return clazz;
            }

            Object[] params = {searchIndex};
            throw new CLIException(MessageFormat.format(
                "search-index-does-not-extend-interface", params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (InstantiationException ex) {
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalAccessException ex) {
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ClassNotFoundException ex) {
            Object[] params = {searchIndex};
            throw new CLIException(MessageFormat.format(
                "search-index-class-not-found", params),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
