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
 * $Id: Util.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.unittest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResourceResult;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Updated to provide more utility methods for test cases.
 */
public class Util {
    
    public static final String GET_ACTION = "GET";
    public static final String POST_ACTION = "POST";
    public static final String ALLOW_DECISION = "allow";
    public static final String DENY_DECISION = "deny";
    public static final String IPLANETAMWEBAGENTSERVICE = "iPlanetAMWebAgentService";

    private static Util instance = new Util();
    private Util() {
    }

    public static List<String> getFileContent(String fileName)
        throws IOException {
        File aFile = new File(fileName);
        List<String> list = new ArrayList<String>();
        BufferedReader input = new BufferedReader(new FileReader(aFile));
        try {
            String line = input.readLine();
            while (line != null) {
                list.add(line);
                line = input.readLine();
            }
        } finally {
            input.close();
        }
        return list;
    }

    public static List<String> getWebResource(String name)
        throws IOException {
        List<String> list = new ArrayList<String>();
        InputStream in = instance.getClass().getClassLoader()
            .getResourceAsStream(name);
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        try {
            String line = input.readLine();
            while (line != null) {
                list.add(line);
                line = input.readLine();
            }
        } finally {
            input.close();
        }
        return list;
    }

    public static List<JSONObject> toJSONObject(List<String> list, String objName)
        throws JSONException {
        List<JSONObject> objects = new ArrayList<JSONObject>();
        for (String s : list) {
            JSONObject json = new JSONObject(s);
            objects.add(json.optJSONObject(objName));
        }
        return objects;
    }
    
    /**
     * Return a SSOToken for the OpenAM admin user, usually for amadmin
     * @return A SSOToken for OpenAM admin user
     * @throws SSOException if there was a problem generating the SSOToken
     */
    public static SSOToken getAdminToken() throws SSOException {
         return (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
    
    /**
     * Create a AMIdentity User for using in Test cases, uses username for password.
     * @param adminToken The token with admin rights to create a new user.
     * @param username The username of the user.
     * @param realm The Realm to create the user in.
     * @return An AMIdentiy that represents the new user.
     * @throws IdRepoException if there was a problem creating the user.
     * @throws SSOException if there was an issue with the admin token.
     */
    public static AMIdentity createATestUser(SSOToken adminToken, 
            String username, String realm) throws IdRepoException, SSOException {
        
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, realm);
        Map<String, Set<String>> attrValues =new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(username);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);

        return amir.createIdentity(IdType.USER, username, attrValues);
    }
    
    /**
     * Create a AMIdentity Group for using in Test cases.
     * @param adminToken The token with admin rights to create a new user.
     * @param groupName The name of the group.
     * @param realm The Realm to create the group in.
     * @return An AMIdentiy that represents the new group.
     * @throws IdRepoException if there was a problem creating the group.
     * @throws SSOException if there was an issue with the admin token.
     */
    public static AMIdentity createATestGroup(SSOToken adminToken, 
            String groupName, String realm) throws IdRepoException, SSOException {
        
        AMIdentityRepository amir = new AMIdentityRepository(adminToken, realm);
        return amir.createIdentity(IdType.GROUP, groupName, Collections.EMPTY_MAP);
    }
            
    /**
     * Returns A Subject that represents all authenticated users.
     * @param pm The PolicyManager to use.
     * @return A Subject that represents all authenticated users.
     * @throws PolicyException if there was a problem creating the Subject.
     */
    public static Subject createAuthenticatedUsersSubject(PolicyManager pm) throws PolicyException {
        
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AuthenticatedUsers");
        
        return subject;
    }

    /**
     * Returns a Subject for the given AMIdentity.
     * @param pm The PolicyManager to use.
     * @param user The user to convert into a Subject
     * @return a Subject for the given AMIdentity.
     * @throws PolicyException if there was a problem creating the Subject.
     */
    public static Subject createAMIdentitySubject(PolicyManager pm, AMIdentity user) throws PolicyException {
        
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add(user.getUniversalId());
        subject.setValues(set);
        
        return subject;
    }

    /**
     * Create a HTTP Get/POST Rule for the given URL.
     * @param ruleName The name of the Rule
     * @param ruleUrl The URL of the Rule.
     * @return A HTTP Get/POST Rule for the given URL.
     * @throws PolicyException if there was a problem creating the Rule.
     */
    public static Rule createAGetPostRule(String ruleName, String ruleUrl) throws PolicyException {
        
        Map<String, Set<String>> actionValues = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(ALLOW_DECISION);
        actionValues.put(GET_ACTION, set);
        set = new HashSet<String>();
        set.add(ALLOW_DECISION);
        actionValues.put(POST_ACTION, set);
        
        return new Rule(ruleName, IPLANETAMWEBAGENTSERVICE, ruleUrl, actionValues);
    }
        
    /**
     * Checks the user/url combination against existing Policy rules.
     * @param userToken The user to use in the policy check.
     * @param url The URL to use in the policy check.
     * @param scope The scope of the policy check.
     * @return True if the policy check was OK for the given user/url combination.
     * @throws SSOException If there was a problem with the users token.
     * @throws PolicyException if there was a problem checking the url.
     * @throws NameNotFoundException  If there was a problem looking up the policy service.
     */
    public static boolean isGetPostAllowed(SSOToken userToken, String url, String scope) 
        throws SSOException, PolicyException, NameNotFoundException {
        
        PolicyEvaluator pe = new PolicyEvaluator(IPLANETAMWEBAGENTSERVICE);
        Set<ResourceResult> resResults = pe.getResourceResults(userToken,
            url, scope, Collections.EMPTY_MAP);
        ResourceResult resResult = resResults.iterator().next();
        PolicyDecision pd = resResult.getPolicyDecision();
        Map<String, ActionDecision> decisions = pd.getActionDecisions();
        ActionDecision get = decisions.get(GET_ACTION);
        ActionDecision post = decisions.get(POST_ACTION);

        return (get != null && get.getValues().contains(ALLOW_DECISION)) &&
                (post != null && post.getValues().contains(ALLOW_DECISION));
    }

    /**
     * Perform a Datastore module login.
     * @param username The username of the user to login as
     * @param password The password of the user to login as
     * @param realm The Realm of the Datastore to use.
     * @return A token as a result of the login or null if it failed.
     * @throws Exception if there was a problem logging in.
     */
    public static SSOToken datastoreLogin(String username, 
            String password, String realm) throws Exception {
        
        AuthContext lc = new AuthContext(realm);
        
        AuthContext.IndexType indexType = AuthContext.IndexType.MODULE_INSTANCE;
        lc.login(indexType, "DataStore");
        Callback[] callbacks = null;

        // Fill in callbacks requested from module
        while (lc.hasMoreRequirements()) {
            callbacks = lc.getRequirements();
            if (callbacks != null) {
                addLoginCallbackMessage(callbacks, username, password);
                lc.submitRequirements(callbacks);
            }
        }

        return (lc.getStatus() == AuthContext.Status.SUCCESS) ? lc.getSSOToken() : null;
    }

    /**
     * Helper method for the datastoreLogin method.
     * @param callbacks The Callbacks to apply the username/password to
     * @param username The username.
     * @param password The password.
     */
    public static void addLoginCallbackMessage(Callback[] callbacks, 
            String username, String password) {
        
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                ((NameCallback) callbacks[i]).setName(username);
            } else if (callbacks[i] instanceof PasswordCallback) {
                ((PasswordCallback) callbacks[i]).setPassword(
                    password.toCharArray());
            }
        }
    }
}
