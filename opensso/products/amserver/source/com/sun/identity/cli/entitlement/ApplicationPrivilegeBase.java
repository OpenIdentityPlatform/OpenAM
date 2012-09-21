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
 * $Id: ApplicationPrivilegeBase.java,v 1.2 2009/11/19 01:02:02 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectImplementation;
import com.sun.identity.entitlement.opensso.OpenSSOGroupSubject;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public abstract class ApplicationPrivilegeBase extends AuthenticatedCommand {
    public static final String PARAM_NAME = "name";
    public static final String PARAM_DESCRIPTION = "description";
    public static final String PARAM_ADD = "add";
    public static final String PARAM_ACTIONS = "actions";
    public static final String PARAM_SUBJECT_TYPE = "subjecttype";
    public static final String PARAM_SUBJECTS = "subjects";
    public static final String PARAM_APPL_NAME = "application";
    public static final String PARAM_RESOURCES = "resources";

    public static final String PARAM_ACTION_READ = "READ";
    public static final String PARAM_ACTION_MODIFY = "MODIFY";
    public static final String PARAM_ACTION_DELEGATE = "DELEGATE";
    public static final String PARAM_ACTION_ALL = "ALL";
    
    public static final String PARAM_SUBJECT_USER = "User";
    public static final String PARAM_SUBJECT_GROUP = "Group";


    private static Map<String, ApplicationPrivilege.PossibleAction> 
        mapActionsToEnum = new HashMap<String,
        ApplicationPrivilege.PossibleAction>();
    private static Map<ApplicationPrivilege.PossibleAction, String>
        mapEnumToActions = new HashMap<ApplicationPrivilege.PossibleAction,
        String>();

    static {
        mapActionsToEnum.put(PARAM_ACTION_READ,
            ApplicationPrivilege.PossibleAction.READ);
        mapActionsToEnum.put(PARAM_ACTION_MODIFY,
            ApplicationPrivilege.PossibleAction.READ_MODIFY);
        mapActionsToEnum.put(PARAM_ACTION_DELEGATE,
            ApplicationPrivilege.PossibleAction.READ_DELEGATE);
        mapActionsToEnum.put(PARAM_ACTION_ALL,
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE);

        mapEnumToActions.put(ApplicationPrivilege.PossibleAction.READ,
            PARAM_ACTION_READ);
        mapEnumToActions.put(ApplicationPrivilege.PossibleAction.READ_MODIFY,
            PARAM_ACTION_MODIFY);
        mapEnumToActions.put(ApplicationPrivilege.PossibleAction.READ_DELEGATE,
            PARAM_ACTION_DELEGATE);
        mapEnumToActions.put(
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE,
            PARAM_ACTION_ALL);

    }

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

    protected ApplicationPrivilege.PossibleAction getActions()
        throws CLIException {
        String actions = getStringOptionValue(PARAM_ACTIONS);
        ApplicationPrivilege.PossibleAction action = mapActionsToEnum.get(actions);
        if (action == null) {
            String[] param = {actions};
            throw new CLIException("privilege-application-action-invalid",
                param, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        return action;
    }

    protected Set<SubjectImplementation> getSubjects(RequestContext rc)
        throws CLIException {
        Set<SubjectImplementation> eSubjects =
            new HashSet<SubjectImplementation>();
        boolean bUser = isUserSubject();
        IdType idType = (bUser) ? IdType.USER : IdType.GROUP;
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        List<String> subjects = rc.getOption(PARAM_SUBJECTS);

        for (String s : subjects) {
            // create AMIdentity just to get the uuid.
            AMIdentity amid = new AMIdentity(null, s, idType, realm, null);
            String universalId = amid.getUniversalId();
            SubjectImplementation sbj = (bUser) ? new OpenSSOUserSubject(
                universalId) : new OpenSSOGroupSubject(universalId);
            eSubjects.add(sbj);
        }
        return eSubjects;
    }

    protected Map<String, Set<String>> getApplicationResourcesMap(
        RequestContext rc,
        String realm
    ) throws CLIException, EntitlementException {
        String appName = getStringOptionValue(PARAM_APPL_NAME);
        Subject subject = SubjectUtils.createSubject(getAdminSSOToken());

        Application application = ApplicationManager.getApplication(
            subject, realm, appName);
        if (application == null) {
            String[] param = {appName};
            throw new CLIException("privilege-application-application-invalid",
                param, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        Set<String> delResources = new HashSet<String>();
        List<String> resources = rc.getOption(PARAM_RESOURCES);
        
        // if resources is not provided, delegate all resources
        if ((resources == null) || resources.isEmpty()) {
            delResources.addAll(application.getResources());
        } else {
            delResources.addAll(resources);
        }

        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(appName, delResources);
        return map;
    }


    private boolean isUserSubject() throws CLIException {
        String subjectType = getStringOptionValue(PARAM_SUBJECT_TYPE);
        if ((subjectType.equalsIgnoreCase(PARAM_SUBJECT_USER))) {
            return true;
        }
        if ((subjectType.equalsIgnoreCase(PARAM_SUBJECT_GROUP))) {
            return false;
        }
        String[] param = {subjectType};
        throw new CLIException("privilege-application-subject-type-invalid",
            param, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
    }

    protected String getDisplayAction(ApplicationPrivilege appPrivilege) {
        ApplicationPrivilege.PossibleAction action =
            appPrivilege.getActionValues();
        return mapEnumToActions.get(action);
    }

    protected Map<String, Set<String>> getApplicationToResources(
        ApplicationPrivilege appPrivilege) {
        Map<String, Set<String>> applToRes = new HashMap<String, Set<String>>();
        Set<String> applNames = appPrivilege.getApplicationNames();
        for (String applName : applNames) {
            applToRes.put(applName,
                appPrivilege.getResourceNames(applName));
        }
        return applToRes;
    }
    protected Map<String, Set<String>> getSubjects(
        ApplicationPrivilege appPrivilege) {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        Set<SubjectImplementation> subjects = appPrivilege.getSubjects();

        for (SubjectImplementation subject : subjects) {
            String type = null;
            String uuid = null;
            if (subject instanceof OpenSSOUserSubject) {
                type = PARAM_SUBJECT_USER;
                uuid = ((OpenSSOUserSubject)subject).getID();
            } else if (subject instanceof OpenSSOGroupSubject) {
                type = PARAM_SUBJECT_GROUP;
                uuid = ((OpenSSOGroupSubject)subject).getID();
            }

            if (type != null) {
                Set<String> set = results.get(type);
                if (set == null) {
                    set = new HashSet<String>();
                    results.put(type, set);
                }
                set.add(uuid);
            }
        }
        return results;
    }

    protected void removeFromMap(
        Map<String, Set<String>> map1,
        Map<String, Set<String>> map2
    ) {
        for (String k : map2.keySet()) {
            Set set2 = map2.get(k);
            Set set1 = map1.get(k);

            if (set1 != null) {
                set1.removeAll(set2);
                if (set1.isEmpty()) {
                    map1.remove(k);
                }
            }
        }
    }

    protected Map<String, Set<String>> mergeMap(
        Map<String, Set<String>> map1,
        Map<String, Set<String>> map2
    ) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> keys = new HashSet<String>();
        for (String k : map1.keySet()) {
            keys.add(k);
        }
        for (String k : map2.keySet()) {
            keys.add(k);
        }

        for (String k : keys) {
            Set<String> set = new HashSet<String>();
            map.put(k, set);
            Set<String> set1 = map1.get(k);
            Set<String> set2 = map2.get(k);

            if (set1 != null) {
                set.addAll(set1);
            }
            if (set2 != null) {
                set.addAll(set2);
            }
        }

        return map;
    }
}
