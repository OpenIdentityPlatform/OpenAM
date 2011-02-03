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
 * $Id: DelegationInterface.java,v 1.5 2008/06/25 05:43:25 qcheng Exp $
 *
 */

package com.sun.identity.delegation.interfaces;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPrivilege;

/**
 * The interface <code>DelegationInterface</code> defines an interface for
 * delegation plugins that would register with delegation framework to manage
 * and evaluate delegation access control privileges and permissions.
 */

public interface DelegationInterface {

    /**
     * Initialize (or configure) the <code>DelegationInterface</code> object.
     * Usually it will be initialized with the environmrnt parameters set by the
     * system administrator.
     * 
     * Usually it will be initialized with the environment parameters set by the
     * system administrator.
     * 
     * @param  appToken <code>SSOToken</code> of the administrative user.
     * @param  configParams configuration parameters as a <code>Map</code>. 
     *         The values in the map is <code>java.util.Set</code>, which 
     *         contains one or more configuration parameters.
     * 
     * @throws DelegationException if an error occurred during initialization 
     *         of <code>DelegationInterface</code> instance
     */

    public void initialize(SSOToken appToken, Map configParams)
            throws DelegationException;

    /**
     * Returns all the delegation privileges associated with a realm.
     * 
     * @param  token  The <code>SSOToken</code> of the requesting user
     * @param  orgName The name of the realm from which the 
     *         delegation privileges are fetched.
     * 
     * @return <code>Set</code> of <code>DelegationPrivilege</code> objects 
     *         associated with the realm.
     * 
     * @throws SSOException  if invalid or expired single-sign-on token
     * @throws DelegationException  for any abnormal condition
     */

    public Set getPrivileges(SSOToken token, String orgName)
            throws SSOException, DelegationException;

    /**
     * Adds a delegation privilege to a specific realm. The permission will be
     * added to the existing privilege in the event that this method is trying
     * to add to an existing privilege.
     * 
     * @param token  The <code>SSOToken</code> of the requesting user
     * @param orgName The name of the realm to which the delegation privilege 
     *        is to be added.
     * @param privilege  The delegation privilege to be added.
     * 
     * @throws SSOException if invalid or expired single-sign-on token
     * @throws DelegationException if any abnormal condition occurred.
     */
    public void addPrivilege(SSOToken token, String orgName,
            DelegationPrivilege privilege) throws SSOException,
            DelegationException;

    /**
     * Removes a delegation privilege from a specific realm.
     * 
     * @param token The <code>SSOToken</code> of the requesting user
     * @param orgName The name of the realm from which the delegation 
     *         privilege is to be removed.
     * @param privilegeName The name of the delegation privilege to be removed.
     * 
     * @throws SSOException  if invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     */

    public void removePrivilege(SSOToken token, String orgName,
            String privilegeName) throws SSOException, DelegationException;

    /**
     * Returns a set of selected subjects of specified types matching the
     * pattern in the given realm. The pattern accepts "*" as the wild card for
     * searching subjects. For example, "a*c" matches with any subject starting
     * with a and ending with c.
     * 
     * @param token The <code>SSOToken</code> of the requesting user
     * @param orgName The name of the realm from which the subjects are fetched.
     * @param types a set of subject types. e.g. ROLE, GROUP.
     * @param pattern a filter used to select the subjects.
     * 
     * @return a set of subjects associated with the realm.
     * 
     * @throws SSOException if invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     */

    public Set getSubjects(SSOToken token, String orgName, Set types,
            String pattern) throws SSOException, DelegationException;

    /**
     * Returns a set of realm names, based on the input parameter
     * <code>organizationNames</code>, in which the "user" has some
     * delegation permissions.
     * 
     * @param token The <code>SSOToken</code> of the requesting user
     * @param organizationNames  a <code>Set</code> of realm names.
     * 
     * @return a <code>Set</code> of realm names in which the user has some 
     *         delegation permissions. It is a subset of 
     *         <code>organizationNames</code>
     * 
     * @throws SSOException if invalid or expired single-sign-on token
     * @throws DelegationException for any abnormal condition
     */

    public Set getManageableOrganizationNames(SSOToken token,
            Set organizationNames) throws SSOException, DelegationException;

    /**
     * Returns a boolean value indicating if a user has the the specified
     * permission.
     * 
     * @param token Single sign on token of the user evaluating permission.
     * @param permission Delegation permission to be evaluated
     * @param envParams Run-time environment parameters.
     * @return the result of the evaluation as a boolean value
     * 
     * @throws SSOException if single-sign-on token invalid or expired.
     * @throws DelegationException for any other abnormal condition.
     */
    public boolean isAllowed(SSOToken token, DelegationPermission permission,
            Map envParams) throws SSOException, DelegationException;

    /**
     * Returns a set of permissions that a user has.
     * 
     * @param token sso token of the user requesting permissions
     * @param orgName The name of the realm from which the delegation 
     *        permissions are fetched.
     * 
     * @return a <code>Set</code> of permissions that a user has
     * 
     * @throws SSOException if single-sign-on token invalid or expired
     * @throws DelegationException for any other abnormal condition
     */

    public Set getPermissions(SSOToken token, String orgName)
            throws SSOException, DelegationException;
}
