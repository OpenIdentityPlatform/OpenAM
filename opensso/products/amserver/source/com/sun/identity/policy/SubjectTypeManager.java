/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SubjectTypeManager.java,v 1.5 2009/01/28 05:35:01 ww203982 Exp $
 *
 */




package com.sun.identity.policy;

import java.util.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.*;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import com.sun.identity.shared.ldap.util.DN;

/**
 * The class <code>SubjectTypeManager</code> provides
 * methods to get a list of configured <code>Subject
 * </code> objects, and to obtain a factory object for it.
 *
 * @supported.all.api
 */
public class SubjectTypeManager {

    private static String SUBJECT = "Subject";

    private SSOToken token;
    private PolicyManager pm;

    private ResourceBundle rb;
    private Subjects realmSubjects = null;
    private Map sharedSubjects = Collections.synchronizedMap(new HashMap());
    private static AMResourceBundleCache amCache = 
            AMResourceBundleCache.getInstance();
    private String pmRealmName;

    static Debug debug = PolicyManager.debug;

    /**
     * Constructs a <code>SubjectTypeManager</code> object
     */
    protected SubjectTypeManager() throws SSOException {
        token = ServiceTypeManager.getSSOToken();
        String lstr = token.getProperty("Locale");
        java.util.Locale loc = com.sun.identity.shared.locale.Locale.getLocale(
            lstr);
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Constructs a <code>SubjectTypeManager</code> object
     * @param pm <code>PolicyManager</code> to initialize
     * <code>SubjectTypeManager</code> with
     */
    protected SubjectTypeManager(PolicyManager pm)  {
        this.pm = pm;
        pmRealmName = new DN(pm.getOrganizationDN()).toRFCString()
                .toLowerCase();
        token = pm.token;
        java.util.Locale loc;
        try {
            String lstr = token.getProperty("Locale");
            loc = com.sun.identity.shared.locale.Locale.getLocale(lstr);
        } catch (SSOException ex) {
            debug.error(
                "SubjectTypeManager:Unable to retreive locale from SSOToken",
                ex);
            loc = Locale.getDefaultLocale();
        }

         if (debug.messageEnabled()) {
            debug.message("SubjectManager locale="+loc+"\tI18nFileName = "+
                     ResBundleUtils.rbName);
        }
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Returns a set of all valid subject type names defined by the policy
     * service.
     * Examples are <code>LDAPRole</code>, <code>LDAPGroup</code>, etc.
     *
     * @return a set of all valid subject type names defined by the policy
     *         service.
     * @throws SSOException if the <code>SSOToken</code> used to create 
     *                      the <code>PolicyManager</code> has become invalid
     * @throws PolicyException for any other abnormal condition
     */
    public Set getSubjectTypeNames() throws SSOException,
            PolicyException {
        return (PolicyManager.getPluginSchemaNames(SUBJECT));
    }

    /**
     * Returns a set of valid subject type names configured for the
     * organization.
     * Examples are <code>LDAPRole</code>, <code>LDAPGroup</code>, etc.
     *
     * @return a set of valid subject type names configured for the
     *         organization.
     * @throws SSOException if the <code>SSOToken</code> used to create 
     *                      the <code>PolicyManager</code> has become invalid
     * @throws PolicyException for any other abnormal condition
     */
    public Set getSelectedSubjectTypeNames() throws SSOException,
            PolicyException {
        Map policyConfig = pm.getPolicyConfig();
        Set selectedSubjects = null;
        if (policyConfig != null) {
            selectedSubjects = 
                    (Set)policyConfig.get(PolicyConfig.SELECTED_SUBJECTS); 
        }
        if ( selectedSubjects == null) {
            selectedSubjects = Collections.EMPTY_SET;
        }
        return selectedSubjects;
    }

    /**
     * Returns the type of the <code>Subject</code> implementation.
     * For example <code>LDAPRoles</code>, <code>LDAPGroups</code> etc.
     *
     * @param subject <code>Subject</code> for which this method will
     * return its associated type
     *
     * @return type of the <code>Subject</code>, e.g., <code>LDAPRoles</code>,
     *         <code>LDAPGroups</code>, etc. Returns <code>null</code> if
     *         not present.
     */
    public String getSubjectTypeName(Subject subject) {
        return (subjectTypeName(subject));
    }

    /**
     * Returns the I18N properties file name that should be
     * used to localize display names for the given
     * subject type.
     *
     * @param subjectType subject type name
     *
     * @return i18n properties file name
     */
    protected String getI18NPropertiesFileName(String subjectType) {
        // %%% Need to get the file name from plugin schema
        return (null);
    }

    /**
     * Returns the I18N key to be used to localize the
     * display name for the subject type name.
     *
     * @param subjectType subject type name
     *
     * @return i18n key to obtain the display name
     */
    public String getI18NKey(String subjectType) {
        PluginSchema ps = PolicyManager.getPluginSchema(SUBJECT, subjectType);
        if (ps != null) {
            return (ps.getI18NKey());
        }
        return (null);
    }

    /**
     * Returns the display name for the subject type
     * @param subjectType subject type
     * @return display name for the subject type
     */
    public String getDisplayName(String subjectType) {
        String displayName = null;
        String i18nKey = getI18NKey(subjectType);
        if (i18nKey == null || i18nKey.length() == 0) {
            displayName = subjectType;
        } else {
            displayName = Locale.getString(rb,i18nKey,debug);
        }
        return displayName;
    }

    /**
     * Returns an instance of the <code>Subject</code> given the subject type
     * name.
     *
     * @param subjectType subject type.
     * @return an instance of the <code>Subject</code> given the subject type
     * name.
     * @throws NameNotFoundException if the <code>Subject</code> for the
     *            <code>subjectType</code> name is not found
     * @throws PolicyException for any other abnormal condition
     */
    public Subject getSubject(String subjectType)
        throws NameNotFoundException, PolicyException {
        PluginSchema ps = PolicyManager.getPluginSchema(SUBJECT, subjectType);
        if (ps == null) {
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "invalid_subject", null,
                subjectType, PolicyException.USER_COLLECTION));
        }

        // Construct the object
        Subject answer = null;
        try {
            String className = ps.getClassName();
            answer = (Subject) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw (new PolicyException(e));
        }

        //initialize with policy config
        answer.initialize(pm.getPolicyConfig());
        return (answer);
    }

    /**
     * Adds a policy subject at realm. 
     *
     * @param subjectName name of the Subject instance 
     * @param subject Subject object to be added 
     *
     * @throws NameAlreadyExistsException if a Subject with the given name
     *          already exists at the realm
     * @throws InvalidNameException if the subject name is invalid
     *
     * @throws PolicyException if can not add the Subject 
     */
    public void addSubject(String subjectName, Subject subject) 
            throws NameAlreadyExistsException, InvalidNameException,
            PolicyException, SSOException {

        //we  really do not use the exclusive flag at realm level
        addSubject(subjectName, subject, false);
    }

    /**
     * Adds a policy subject at realm. 
     *
     * @param subjectName name of the Subject instance 
     * @param subject Subject object to be added 
     *
     * @param exclusive boolean flag indicating whether the subject 
     *        is to be exclusive subject. If subject is exclusive, 
     *        policy applies to users who are not members of the 
     *        subject. Otherwise, policy applies to members of the subject.
     *
     * @throws NameAlreadyExistsException if a Subject with the given name
     *          already exists at the realm
     * @throws InvalidNameException if the subject name is invalid
     *
     * @throws PolicyException if can not add the Subject 
     *
     *
     */
    private void addSubject(String subjectName, Subject subject, 
            boolean exclusive) 
            throws NameAlreadyExistsException, InvalidNameException,
            PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("Adding realm subject : " + subjectName
                    + ", in realm:" + pmRealmName);
        }
        if (realmSubjects == null) {
            initRealmSubjects();
        }
        realmSubjects.addSubject(subjectName, subject, exclusive);
        saveSubjects();
        if (debug.messageEnabled()) {
            debug.message("Added realm subject : " + subjectName
                    + ", in realm:" + pmRealmName);
        }
    }

    /**
     * Removes the subject with the given name  from the realm.
     * This method would throw PolicyException if the subject 
     * is being used by any policy.
     *
     * @param subjectName name of the Subject
     *
     * @return returns the Subject object being removed,
     *         returns <code>null</code> if Subject with 
     *         the given subjectName is not present 
     *
     * @throws PolicyException if can not remove the Subject 
     */
    public Subject removeSubject(String subjectName) 
            throws ObjectInUseException, PolicyException, SSOException {
        return removeSubject(subjectName, false);
    }

    /**
     * Removes the subject with the given name  from the realm.
     * This method would throw PolicyException if the subject 
     * is being used by any policy unless <code>forcedRemove</code> 
     * argument  is set to <code>true</code>. 
     * If the <code>forcedRemove</code> argument is set to 
     * <code>true</code> policies that are using the subject would 
     * be modified to  remove the references to the subject
     *
     * @param subjectName name of the Subject
     * @param forcedRemove if set to <code>true</code>, policies that
     *    use the subject would be modifed to remove the references
     *    to the subject. Otherwise, <code>ObjectInUseException</code>
     *    would be thrown if there is any policy using the subject
     *
     * @return returns the Subject object being removed,
     *         returns <code>null</code> if Subject with 
     *         the given subjectName is not present 
     *
     * @throws PolicyException if can not remove the Subject 
     */
    public Subject removeSubject(String subjectName, boolean forcedRemove) 
            throws ObjectInUseException, PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("Removing realm subject : " + subjectName
                    + ", in realm:" + pmRealmName);
        }
        if (realmSubjects == null) {
            initRealmSubjects();
        }
        if (forcedRemove) {
            Set userPolicies = pm.getPoliciesUsingRealmSubject(subjectName);
            for (Iterator policyIter = userPolicies.iterator();
                    policyIter.hasNext();) {
                Policy policy = (Policy)policyIter.next();
                policy.removeSubject(subjectName);
            }
        } else {
            Policy p = pm.getPolicyUsingRealmSubject(subjectName);
            if ( p != null) {
                //ObjectInUseException(String rbName, String errCode,         
                //Object[] args, String name, Object user) 
                throw new ObjectInUseException(null, null, null, null, null);
            }
        }
        Subject subject = realmSubjects.removeSubject(subjectName);
        saveSubjects();
        if (debug.messageEnabled()) {
            debug.message("Removed realm subject : " + subjectName
                    + ", in realm:" + pmRealmName);
        }
        return subject;
    }

    /**
     * Replaces an existing subject with the same name by the
     * current one at the realm. If a subject with the same name does 
     * not exist, it will be added.
     *
     * @param subjectName name of the Subject instance 
     * @param subject Subject that will replace an existing Subject
     *         with the same name
     *
     * @throws NameNotFoundException if a Subject instance
     *         with the given name is not present
     *
     * @throws PolicyException if can not replace the Subject 
     */
    public void replaceSubject(String subjectName, Subject subject) 
            throws NameNotFoundException, PolicyException, SSOException {

        //we  really do not use the exclusive flag at realm level
        replaceSubject(subjectName, subject, false);
    }

    /**
     * Replaces an existing subject with the same name by the
     * current one at the realm. If a subject with the same name does 
     * not exist, it will be added.
     *
     * @param subjectName name of the Subject instance 
     * @param subject Subject that will replace an existing Subject
     *         with the same name
     *
     * @param exclusive boolean flag indicating whether the subject 
     *        is to be exclusive subject. If subject is exclusive, 
     *        policy applies to users who are not members of the 
     *        subject. Otherwise, policy applies to members of the subject.
     *
     * @throws NameNotFoundException if a Subject instance
     *         with the given name is not present
     *
     * @throws PolicyException if can not replace the Subject 
     *
     *
     */
    private void replaceSubject(String subjectName, Subject subject, 
            boolean exclusive) 
            throws NameNotFoundException, PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("Replacing realm subject : " + subjectName
                    + ", in realm:" + pmRealmName);
        }
        if (realmSubjects == null) {
            initRealmSubjects();
        }
        realmSubjects.replaceSubject(subjectName, subject, exclusive);
        saveSubjects();
        if (debug.messageEnabled()) {
            debug.message("Replaced realm subject : " + subjectName
                    + ", in realm:" + pmRealmName);
        }
    }

    /**
     * Get the set of names of Subject(s) defined at the realm
     *
     * @return set of subject names
     */
    public Set getSubjectNames() throws PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("Getting subject names from realm: " 
                    +  pmRealmName);
        }
        if (realmSubjects == null) {
            initRealmSubjects();
        }
        Set subjectNames = realmSubjects.getSubjectNames();
        if (debug.messageEnabled()) {
            debug.message("Returning subject names from realm: " 
                    +  pmRealmName + ",subjectNames=" + subjectNames);
        }
        return subjectNames;
    }

    /**
     * Returns the Subject object identified by subjectName defined at 
     * the realm
     *
     * @param subjectName name of subject.
     *
     * @return Subject object
     *
     * @throws NameNotFoundException if a Subject with the given name
     * does not exist
     *
     * @throws PolicyException if can not get the Subject
     */
    public Subject getSubjectByName(String subjectName) 
            throws NameNotFoundException, PolicyException {
        if (debug.messageEnabled()) {
            debug.message("Getting subject by name from realm: " 
                    +  pmRealmName + ", subjectName=" + subjectName);
        }
        if (realmSubjects == null) {
            initRealmSubjects();
        }
        if (debug.messageEnabled()) {
            debug.message("Returning subject by name from realm: " 
                    +  pmRealmName + ", subjectName=" + subjectName);
        }
        return (Subject)realmSubjects.getSubject(subjectName).clone();
    }

    synchronized Subject getCachedSubjectByName(String subjectName) 
            throws  PolicyException {
        if (debug.messageEnabled()) {
            debug.message("Getting cached subject by name from realm: " 
                    +  pmRealmName + ", subjectName=" + subjectName);
        }
        if (realmSubjects == null) {
            initRealmSubjects();
        }
        if (debug.messageEnabled()) {
            debug.message("Returning cached subject by name from realm: " 
                    +  pmRealmName + ", subjectName=" + subjectName);
        }
        return (Subject)realmSubjects.fetchSubject(subjectName);
    }

    /**
     * Returns a handle to the Subject object identified by subjectName 
     * defined at the realm, to add to a policy. 
     * Returned Subject is backed by 
     * the Subject at the realm. However, you can not change the values
     * using the returned Subject. 
     *
     * @param subjectName name of subject.
     *
     * @return Subject object
     *
     * @throws NameNotFoundException if a Subject with the given name
     * does not exist
     *
     * @throws PolicyException if can not get the Subject
     *
     */
    Subject getSharedSubject(String subjectName) 
            throws  PolicyException {
        if (debug.messageEnabled()) {
            debug.message("Getting shared subject from realm: " 
                    +  pmRealmName + ", subjectName=" + subjectName);
        }
        Subject subject = (Subject)sharedSubjects.get(subjectName);
        if (subject == null) {
            subject = new SharedSubject(subjectName, this);
            sharedSubjects.put(subjectName, subject);
        }
        if (debug.messageEnabled()) {
            debug.message("Returning shared subject from realm: " 
                    +  pmRealmName + ", subjectName=" + subjectName);
        }
        return subject;
    }

    /**
     * Returns subject type name for the given <code>subject</code>
     * @return subject type name for the given <code>subject</code>
     */
    static String subjectTypeName(Subject subject) {
        if (subject == null) {
            return (null);
        }
        String answer = null;
        String className = subject.getClass().getName();
        Iterator items = PolicyManager.getPluginSchemaNames(SUBJECT).iterator();
        while (items.hasNext()) {
            String pluginName = (String) items.next();
            PluginSchema ps = PolicyManager.getPluginSchema(SUBJECT, 
                pluginName);
            if (className.equals(ps.getClassName())) {
                answer = pluginName;
                break;
            }
        }
        return (answer);
    }

    /**
     * Returns the view bean URL given the Subject
     *
     * @param subject subject for which to get the view bean URL
     *
     * @return view bean URL defined for the subject plugin in the policy
     *         service <code>PluginSchema</code>.
     */
    public String getViewBeanURL(Subject subject) {
        return PolicyManager.getViewBeanURL(SUBJECT, 
            subject.getClass().getName());
    }

    /**
     * Returns <code>PolicyManager</code> used by this object
     */
    PolicyManager getPolicyManager() {
        return pm;
    }

    /**
     * Saves the realm scoped <code>Subject</code> objects to persistent store
     */
    private void saveSubjects() throws PolicyException, SSOException {
        if (realmSubjects != null) {
            pm.saveRealmSubjects(realmSubjects);
        }
    }

    /**
     * Initializes the realm scoped <code>Subject</code> objects reading from
     * persistent store
     */
    private void initRealmSubjects() throws PolicyException {
        if (debug.messageEnabled()) {
            debug.message("Initializing realm subjects in realm : " 
                    +  pmRealmName); 
        }
        try {
            realmSubjects = pm.readRealmSubjects();
        } catch (SSOException ssoe){
            throw new PolicyException(ResBundleUtils.rbName,
                "could_not_initialize_realm_subjects", null, ssoe);
        }
        if (debug.messageEnabled()) {
            debug.message("Initialized realm subjects in realm : " 
                    +  pmRealmName); 
        }
    }

    /**
     * Resets the cached realm scoped <code>Subject</code> objects. 
     * Would read from persistent store on next access to realm scoped
     * <code>Subject</code> object
     */
    void resetRealmSubjects() {
        if (debug.messageEnabled()) {
            debug.message("Resetting realm subjects in realm : " 
                    +  pmRealmName); 
        }
        synchronized(this) {
            realmSubjects = null;
        }
        if (debug.messageEnabled()) {
            debug.message("Reset realm subjects in realm : " 
                    +  pmRealmName); 
        }
    }

}
