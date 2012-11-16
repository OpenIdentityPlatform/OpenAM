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
 * $Id: ApplicationPrivilegeCLITest.java,v 1.2 2009/11/16 21:42:20 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.IArgument;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectImplementation;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class ApplicationPrivilegeCLITest extends CLITestImpl {
    private static final String PRIVILEGE_NAME = "ApplicationPrivilegeCLITest";
    private AMIdentity user1;
    private AMIdentity user2;

    @Override
    protected void beforeClass() throws Exception {
        user1 = createUser("ApplicationPrivilegeCLITestUser1");
        user2 = createUser("ApplicationPrivilegeCLITestUser2");
    }

    @AfterClass
    public void cleanup() throws Exception {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(user1);
        identities.add(user2);
        amir.deleteIdentities(identities);
    }

    @Test
    public void createApplicationPrivilege() throws Exception {
        String[] args = new String[17];
        args[0] = "add-app-priv";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_APPL_NAME;
        args[6] = ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_ACTIONS;
        args[8] = ApplicationPrivilegeBase.PARAM_ACTION_ALL;

        args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECT_TYPE;
        args[10] = ApplicationPrivilegeBase.PARAM_SUBJECT_USER;

        args[11] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECTS;
        args[12] = user1.getUniversalId();

        args[13] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_DESCRIPTION;
        args[14] = "desc";

        args[15] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_RESOURCES;
        args[16] = "http://www.example.com";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        if (!ap.getDescription().equals("desc")) {
            throw new Exception(
                "ApplicationPrivilegeCLITest.createApplicationPrivilege: " +
                "description is incorrect.");
        }
        if (!ap.getActionValues().equals(
            ApplicationPrivilege.PossibleAction.READ_MODIFY_DELEGATE)
        ) {
            throw new Exception(
                "ApplicationPrivilegeCLITest.createApplicationPrivilege: " +
                "actions is incorrect.");
        }

        Set<String> resources = new HashSet<String>();
        resources.add("http://www.example.com");
        validateResources(ap, resources, "createApplicationPrivilege");

        Set<AMIdentity> users = new HashSet<AMIdentity>();
        users.add(user1);
        validateSubjects(ap, users, "createApplicationPrivilege");
    }

    private void validateResources(
        ApplicationPrivilege ap,
        Set<String> resources,
        String methodName
    ) throws Exception {
        Set<String> origResources = ap.getResourceNames(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        if ((origResources == null) ||
            (origResources.size() != resources.size())
        ) {
            throw new Exception(
                "ApplicationPrivilegeCLITest." + methodName + ": " +
                "resources size is incorrect.");
        }

        for (String r : origResources) {
            if (!resources.contains(r)) {
                throw new Exception(
                    "ApplicationPrivilegeCLITest." + methodName + ": " +
                    "resource is incorrect.");
            }
        }
    }

    private void validateSubjects(
        ApplicationPrivilege ap,
        Set<AMIdentity> users,
        String methodName)
        throws Exception {
        Set<SubjectImplementation> subjects = ap.getSubjects();
        if ((subjects == null) || (subjects.size() != users.size())) {
            throw new Exception(
                "ApplicationPrivilegeCLITest." + methodName + ": " +
                "subjects is empty.");
        }

        for (SubjectImplementation subject : subjects) {
            if (!(subject instanceof OpenSSOUserSubject)) {
                throw new Exception(
                    "ApplicationPrivilegeCLITest." + methodName + ": " +
                    "subject is incorrect.");
            }

            String uuid = ((OpenSSOUserSubject)subject).getID();
            boolean found = false;
            for (AMIdentity user : users) {
                if (uuid.equals(user.getUniversalId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new Exception(
                    "ApplicationPrivilegeCLITest." + methodName + ": " +
                    "uuid is incorrect.");
            }
        }
    }


    @Test (dependsOnMethods="createApplicationPrivilege")
    public void listApplicationPrivileges() throws Exception {
        String[] args = new String[3];
        args[0] = "list-app-privs";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }

    @Test (dependsOnMethods="listApplicationPrivileges")
    public void changeDescription() throws Exception {
        String[] args = new String[7];
        args[0] = "update-app-priv";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_DESCRIPTION;
        args[6] = "descChanged";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        if (!ap.getDescription().equals("descChanged")) {
            throw new Exception(
                "ApplicationPrivilegeCLITest.changeDescription: " +
                "description is incorrect.");
        }
    }


    @Test (dependsOnMethods="changeDescription")
    public void changeAction() throws Exception {
        String[] args = new String[7];
        args[0] = "update-app-priv";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_ACTIONS;
        args[6] = ApplicationPrivilegeBase.PARAM_ACTION_DELEGATE;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        if (!ap.getActionValues().equals(
            ApplicationPrivilege.PossibleAction.READ_DELEGATE)) {
            throw new Exception(
                "ApplicationPrivilegeCLITest.changeAction: " +
                "action is incorrect.");
        }
    }

    @Test (dependsOnMethods="changeAction")
    public void setSubjects() throws Exception {
        String[] args = new String[9];
        args[0] = "update-app-priv-subjects";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECT_TYPE;
        args[6] = ApplicationPrivilegeBase.PARAM_SUBJECT_USER;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECTS;
        args[8] = user2.getUniversalId();

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        Set<AMIdentity> users = new HashSet<AMIdentity>();
        users.add(user2);
        validateSubjects(ap, users, "setSubjects");
    }

    @Test (dependsOnMethods="setSubjects")
    public void addSubjects() throws Exception {
        String[] args = new String[10];
        args[0] = "update-app-priv-subjects";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECT_TYPE;
        args[6] = ApplicationPrivilegeBase.PARAM_SUBJECT_USER;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECTS;
        args[8] = user1.getUniversalId();

        args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_ADD;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        Set<AMIdentity> users = new HashSet<AMIdentity>();
        users.add(user1);
        users.add(user2);
        validateSubjects(ap, users, "setSubjects");
    }

     @Test (dependsOnMethods="addSubjects")
    public void removeSubjects() throws Exception {
        String[] args = new String[9];
        args[0] = "remove-app-priv-subjects";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECT_TYPE;
        args[6] = ApplicationPrivilegeBase.PARAM_SUBJECT_USER;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_SUBJECTS;
        args[8] = user2.getUniversalId();

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        Set<AMIdentity> users = new HashSet<AMIdentity>();
        users.add(user1);
        validateSubjects(ap, users, "setSubjects");
    }

    @Test(dependsOnMethods = "removeSubjects")
    public void setResources() throws Exception {
        String[] args = new String[10];
        args[0] = "update-app-priv-resources";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_APPL_NAME;
        args[6] = ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_RESOURCES;
        args[8] = "http://www.example1.com";
        args[9] = "http://www.example2.com";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        Set<String> resources = new HashSet<String>();
        resources.add("http://www.example1.com");
        resources.add("http://www.example2.com");
        validateResources(ap, resources, "setResources");
    }

    @Test(dependsOnMethods = "setResources")
    public void addResources() throws Exception {
        String[] args = new String[10];
        args[0] = "update-app-priv-resources";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_APPL_NAME;
        args[6] = ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_RESOURCES;
        args[8] = "http://www.example3.com";

        args[9] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_ADD;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        Set<String> resources = new HashSet<String>();
        resources.add("http://www.example1.com");
        resources.add("http://www.example2.com");
        resources.add("http://www.example3.com");
        validateResources(ap, resources, "setResources");
    }

    @Test(dependsOnMethods = "addResources")
    public void removeResources() throws Exception {
        String[] args = new String[9];
        args[0] = "remove-app-priv-resources";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_NAME;
        args[4] = PRIVILEGE_NAME;

        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_APPL_NAME;
        args[6] = ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;

        args[7] = CLIConstants.PREFIX_ARGUMENT_LONG +
            ApplicationPrivilegeBase.PARAM_RESOURCES;
        args[8] = "http://www.example3.com";

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        ApplicationPrivilege ap = apm.getPrivilege(PRIVILEGE_NAME);

        Set<String> resources = new HashSet<String>();
        resources.add("http://www.example1.com");
        resources.add("http://www.example2.com");
        validateResources(ap, resources, "setResources");
    }

    @Test(dependsOnMethods = "removeResources")
    public void removedApplicationPrivilege() throws Exception {
        String[] args = new String[5];
        args[0] = "remove-app-privs";

        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";

        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            DeleteApplicationPrivilege.PARAM_NAMES;
        args[4] = PRIVILEGE_NAME;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance("/", adminSubject);
        
        try {
            apm.getPrivilege(PRIVILEGE_NAME);
        } catch (EntitlementException ex) {
            if (ex.getErrorCode() != 325) {
                throw ex;
            }
        }
    }

    private AMIdentity createUser(String name)
        throws SSOException, IdRepoException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Map<String, Set<String>> attrValues = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(name);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);
        return amir.createIdentity(IdType.USER, name, attrValues);
    }
}
