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
 * $Id: ISPermission.java,v 1.5 2008/08/19 19:09:17 veiming Exp $
 *
 */

package com.sun.identity.policy.jaas;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;

import com.sun.identity.authentication.service.SSOTokenPrincipal;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;
import com.sun.identity.shared.debug.Debug;

import java.security.Permission;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.PermissionCollection;

import javax.security.auth.Subject;
import java.security.Principal;

import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.Collections;

/**
 * This class provides the support for JAAS Authorization service 
 * Its a new JAAS <code>Permission</code> which extends the
 * {@link java.security.Permission} class. This is the only
 * API which gets used by an application/container to evaluate policy against
 * the OpenSSO Policy framework. This class provides implementations
 * of all the required abstract methods of <code>java.security.Permission</code>
 * , in a way that the policy evaluation is made against the OpenSSO
 * Enterprise's Policy service.
 * <p>
 * For example, one would use this class as follows to evaluate policy
 * permissions:
 * <pre>
 * ISPermission perm = new ISPermission("iPlanetAMWebAgentService",
 *                  "http://www.sun.com:80","GET");
 * AccessController.checkPermission(perm);
 * </pre>
 * If OpenSSO has the policy service
 * <code>iPlanetAMWebAgentService</code> which has a <code>Rule</code> defined
 * for resource <code>http://www.sun.com:80</code>
 * with action "GET" with allow privilege, this call will return quietly, if
 * such a policy is not found then access is denied and Exception thrown
 * accordingly. Also <code>ISPermission</code> co-exists with the 
 * permissions specified in the JDK policy store ( by default file <code>
 * sun.security.provider.PolicyFile</code> or defined on the command line 
 * using the -D option.
 *
 * <p>
 * @see java.security.Permission
 * @see javax.security.auth.Subject
 * @see java.security.ProtectionDomain
 * <p>
 *
 * @supported.all.api
 */
public class ISPermission extends Permission {
    private Subject subject;
    private CodeSource codesource;
    private ProtectionDomain protectionDomain;
    private String serviceName;
    private String resourceName;
    private String actions;
    private Set actionSet;
    private Map envParams = Collections.synchronizedMap(Collections.EMPTY_MAP);
    private PolicyEvaluatorFactory policyEvalFactory;
    static Debug debug = Debug.getInstance("amPolicy");

    /**
     * Constructs an <code>ISPermission</code> instance, with the specified
     * <code>ProtectionDomain</code>.
     *
     * @param pd <code>ProtectionDomain</code> for which this
     *        <code>ISPermission</code> is being created.
     */
    protected ISPermission(ProtectionDomain pd) {
        super("ISPermission"); 
        if (debug.messageEnabled()) {
            debug.message("ISPermission(protectionDomain) constructor "
                + "called ");
        }
        this.protectionDomain = pd;
    }

    /**
     * Constructs an <code>ISPermission</code> instance, with the specified
     * <code>Subject</code> and the <code>CodeSource</code>.
     *
     * @param subject <code>Subject</code> for which this
     *        <code>ISPermission</code> is being created.
     * @param codesource <code>CodeSource</code> for which this permission is
     *        being created.
     */
    public ISPermission(Subject subject,CodeSource codesource) {
        super("ISPermission"); 
        if (debug.messageEnabled()) {
            debug.message("ISPermission(subject,codesource) constructor "
                + "called ");
        }
        this.subject = subject;
        this.codesource = codesource;
    }

    /**
     * Constructs an <code>ISPermission</code> instance, with the specified
     * <code>CodeSource</code>.
     *
     * @param codesource <code>CodeSource</code> for which this permission is
     *        being created.
     */
    public ISPermission(CodeSource codesource) {
        super("ISPermission"); 
        if (debug.messageEnabled()) {
            debug.message("ISPermission(codesource) constructor "
                + "called ");
        }
        this.codesource = codesource;
    }

    /**
     * Constructs an <code>ISPermission</code> instance, with the specified
     * service name, resource name and action name.
     * @param serviceName name of service for which this
     *        <code>ISPermission</code> is being created. This name needs to be
     *        one of the loaded services in the OpenSSO's policy
     *        engine. example: <code>iPlanetAMWegAgentService</code>
     *
     * @param resourceName name of the resource for which this 
     *        <code>ISPermission</code> is being defined.
     *
     * @param actions name of the action that needs to be checked for. It
     *        may be a <code>String</code> like "GET", "POST" in case of 
     *        service name <code>iPlanetAMWebAgentService</code>.
     */
    public ISPermission(String serviceName,String resourceName, String 
        actions) 
    {
        super("ISPermission");
        this.serviceName = serviceName;
        this.resourceName = resourceName;
        this.actions = actions;
        debug.message("ISPermission:: Constructor called");
    }


    /**
     * Constructs an <code>ISPermission</code> instance, with the specified
     * service name, resource name and action name.
     * @param serviceName name of service for which this
     *        <code>ISPermission</code> is being created. This name needs to be
     *        one of the loaded policy services in the OpenSSO.
     *        example:
     *        <code>iPlanetAMWegAgentService</code>
     *
     * @param resourceName name of the resource for which this 
     *        <code>ISPermission</code> is being defined.
     *
     * @param actions name of the action that needs to be checked for. It
     *        may be a <code>String</code> like "GET", "POST" in case of 
     *        service name <code>iPlanetAMWebAgentService</code>.
     * @param envParams a <code>java.util.Map</code> of environment parameters
     *        which are used by the  
     *        <code>com.sun.identity.policy.client.PolicyEvaluator</code> 
     *        to evaluate the <code>com.sun.identity.policy.Conditions</code> 
     *        associated with the policy. This is a Map of attribute-value pairs
     *        representing the environment under which the policy needs to be
     *        evaluated.
     */
    public ISPermission(String serviceName,String resourceName, 
        String actions, Map envParams) 
    {
        super("ISPermission");
        this.serviceName = serviceName;
        this.resourceName = resourceName;
        this.actions = actions;
        this.envParams = envParams;
        debug.message("ISPermission:: Constructor called");
    }

    /**
     * returns the name of the service associated with this <code>ISPermission
     * </code>.
     * @return <code>String</code> representing the name of the service for this
     *         permission.
     */
    public String getServiceName() {
        debug.message("ISPermission: getServiceName called");
        return serviceName;
    }

    /**
     * returns the name of the resource associated with this <code>ISPermission
     * </code>.
     * @return  <code>String</code> representing the name of the resource for 
     *          this permission.
     */
    public String getResourceName() {
        debug.message("ISPermission: getResourceName called");
        return resourceName;
    }

    /**
     * returns environment parameters and their values associated with this 
     * <code>ISPermission</code>.
     * @return  <code>Map</code> representing the environment parameters of
     *          this permission. The <code>Map</code> consists of attribute 
     *          value pairs.
     */
    public Map getEnvParams() {
        return envParams;
    }

    /**
     * returns a comma separated list of actions associated with this 
     * <code>ISPermission</code>.
     * @return  a comma separated <code>String</code> representing the name 
     *          of the action for this object. For example for:
     *          <pre>
     *          ISPermission isp = new ISPermission("iPlanetAMWebAgentService, 
     *              "http://www.sun.com:80", "GET, POST");
     *          getActions() would return "GET,POST"
     *          </pre>
     */
    public String getActions() {
        debug.message("ISPermission: getActions called");
        if (debug.messageEnabled()) {
            debug.message("returning actions:"+actions);
        }
        return actions;
    }

    /**
     * Returns true if two comma separated strings are equal.
     *
     * @param actions1 actions string.
     * @param actions2 actions string.
     * @return true if two comma separated strings are equal.
     */
    private boolean actionEquals(String actions1, String actions2) {
            Set actionSet1 = Collections.synchronizedSet(new HashSet());
            Set actionSet2 = Collections.synchronizedSet(new HashSet());
        if (actions1 != null) {
               StringTokenizer st = new StringTokenizer(actions1,",");
            while (st.hasMoreTokens()) {
                String action = (String)st.nextToken().trim();
                actionSet1.add(action);
            }
        }
        if (actions2 != null) {
               StringTokenizer st = new StringTokenizer(actions2,",");
            while (st.hasMoreTokens()) {
                String action = (String)st.nextToken().trim();
                actionSet2.add(action);
            }
        }
        return actionSet1.equals(actionSet2);
    }

    /**
     * Returns a <code>Set</code> of actions for this Permission.
     * @param actions comma separated actions string.
     * @return set of actions in this permsision.
     *
     */
    private Set actionsInSet(String actions) {
        if (actionSet == null) {
                actionSet = Collections.synchronizedSet(new HashSet());
        } else {
            return actionSet;
        }
        if (actions != null) {
               StringTokenizer st = new StringTokenizer(actions,",");
            while (st.hasMoreTokens()) {
                String action = (String)st.nextToken();
                actionSet.add(action);
            }
        }
        return actionSet;
    }
        
    /**
     * returns the <code>Subject</code>associated with this <code>ISPermission
     * </code>.
     * @return  <code>javax.security.auth.Subject</code> representing the 
     *          subject of this permission.
     */
    public Subject getSubject() {
        debug.message("ISPermission:: getSubject called ");
        return subject;
    }

    /**
     * returns the <code>CodeSource</code>associated with this 
     * <code>ISPermission</code>.
     * @return <code>java.security.CodeSource</code> representing the 
     *         <code>codesource</code> of this permission. 
     */
    public CodeSource getCodeSource() {
        debug.message("ISPermission:: getCodeSource called ");
        return codesource;
    }

    /**
     * returns the <code>ProtectionDomain</code>associated with this 
     * <code>ISPermission</code>.
     * @return <code>java.security.ProtectionDomain</code> representing the 
     *         <code>protectionDomain</code> of this permission. 
     */
    public ProtectionDomain getProtectionDomain() {
        debug.message("ISPermission:: getProtectionDomain called ");
        return protectionDomain;
    }

    /**
     * Returns true if two <code>ISPermission</code> objects for equality.
     *
     * @param obj <code>ISPermission</code> object.
     * @return true if subject, <code>codesource</code>, service name, resource
     *         name actions and environment parameters of both objects are 
     *         equal.
     */
    public boolean equals(Object obj){
        boolean result = true;
        debug.message("ISPermission:: equals(Object) called ");
        if (obj == this) {
            if (debug.messageEnabled()) {
                debug.message("ISPermission::equals::this " +result);
            }
            return true;
        }
        if (obj instanceof ISPermission) {
            ISPermission perm = (ISPermission) obj;
            Subject subject = perm.getSubject();
            if (subject != null) {
                result = subject.equals(this.subject);
            } else {
                if (this.subject != null) {
                    result = false; // subject is null, while this.subject is 
                                    // not null.
                }
            }
            if (debug.messageEnabled()) {
                debug.message("ISPermission::subject equals:"+result);
            }
            if (result) {
                CodeSource codesource = perm.getCodeSource();
                if (codesource != null) {
                    result = codesource.equals(this.codesource);
                    if (debug.messageEnabled()) {
                          debug.message("ISPermission::codesource equals:"+
                            codesource.equals(this.codesource));
                    }
                } else {
                    if (this.codesource != null) {
                        result = false;
                    }
                }
            }
            if (result) {
                ProtectionDomain protectionDomain = perm.getProtectionDomain();
                if (protectionDomain != null) {
                    result = protectionDomain.equals(this.protectionDomain);
                    if (debug.messageEnabled()) {
                          debug.message("ISPermission::protectionDomain equals:"
                            + protectionDomain.equals(this.protectionDomain));
                    }
                } else {
                    if (this.protectionDomain != null) {
                        result = false;
                    }
                }
            }
            if (result) {
                String serviceName = perm.getServiceName();
                if (serviceName != null) {
                    result = serviceName.equals(this.serviceName); 
                    if (debug.messageEnabled()) {
                          debug.message("ISPermission::servicename equals:"+
                            serviceName.equals(this.serviceName));
                    }
                } else {
                    if (this.serviceName != null) {
                        result = false;
                    }
                }
            }
            if (result) {
                String resourceName = perm.getResourceName();
               if (resourceName != null) {
                   result = resourceName.equals(this.resourceName);
                    if (debug.messageEnabled()) {
                          debug.message("ISPermission::resourceName equals:"+
                            resourceName.equals(this.resourceName));
                    }
                } else {
                    if (this.resourceName != null) {
                        result = false;
                    }
                }
            }
            if (result) {
                String actions = perm.getActions();
                if (actions != null) {
                    result = actionEquals(actions,this.actions);
                    if (debug.messageEnabled()) {
                          debug.message("ISPermission::Actions equals:"+
                            actionEquals(actions,this.actions));
                    }
                } else {
                    if (this.actions != null) {
                        result = false;
                    }
                }
            }
            if (result) {
                Map envParams = perm.getEnvParams();
                if (envParams != null  && !envParams.isEmpty())  {
                    result = envParams.equals(this.envParams);
                    if (debug.messageEnabled()) {
                        debug.message("ISPermission::equals::envMap"
                            + envParams.equals(this.envParams));
                    }
                } else {
                    if (this.envParams != null && !this.envParams.isEmpty()) {
                        result = false;
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ISPermission::equals::returning " +result);
        }
        return result;
    }

    /**
     * Returns the hash code value for this Permission object.
     * <P>
     * The required <code>hashCode</code> behavior for Permission Objects is
     * the following: <p>
     * <ul>
     * <li>Whenever it is invoked on the same Permission object more than 
     *     once during an execution of a Java application, the 
     *     <code>hashCode</code> method
     *     must consistently return the same integer. This integer need not 
     *     remain consistent from one execution of an application to another 
     *     execution of the same application. <p>
     * <li>If two Permission objects are equal according to the 
     *     <code>equals</code> 
     *     method, then calling the <code>hashCode</code> method on each of the
     *     two Permission objects must produce the same integer result. 
     * </ul>
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        int hash = 0;
        if (subject != null) {
            hash = hash + this.subject.hashCode();
        }
        if (codesource != null) {
            hash = hash + this.codesource.hashCode();
        }
        if (protectionDomain != null) {
            hash = hash + this.protectionDomain.hashCode();
        }
        if (serviceName != null) {
            hash = hash + this.serviceName.hashCode();
        }
        if (resourceName != null) {
            hash = hash + this.resourceName.hashCode();
        }
        if (actions != null) {
            Set actionSet = actionsInSet(actions);
            hash = hash + actionSet.hashCode();
        }
        if (envParams != null) {
            hash = hash + this.envParams.hashCode();
        }
        if (debug.messageEnabled()) {
            debug.message("ISPermission::hashCode::"+hash);
        }
        return hash;
    }

    /**
     * Checks if the specified permission's actions are "implied by" 
     * this object's actions.
     * <P>
     * The <code>implies</code> method is used by the
     * <code>AccessController</code> to determine whether or not a requested
     * permission is implied by another permission that is known to be valid
     * in the current execution context.
     *
     * @param perm the permission to check against.
     *
     * @return true if the specified permission is implied by this object,
     *         false if not. The check is made against the OpenSSO's 
     *         policy service to determine this evaluation.
     */
    public boolean implies(Permission perm) {
        debug.message("ISPermission: implies called");
        boolean allowed = false;
        if (perm instanceof ISPermission) {
            debug.message("ISPermission:passed perm is of type ISPermission");
            if (protectionDomain != null) {
                debug.message("ISPermission:implies:protectionDomain not null");
                if (debug.messageEnabled()) {
                    debug.message("ISPermission::implies: protectionDomain:"
                    +protectionDomain.toString());
                }
                final String serviceName =((ISPermission)perm).getServiceName();
                final String resourceName =
                    ((ISPermission)perm).getResourceName();
                final String actions = ((ISPermission)perm).getActions();
                final Map envParams = ((ISPermission)perm).getEnvParams();
                if (debug.messageEnabled()) {
                    debug.message("ISPermission: resourceName="
                        +resourceName);
                    debug.message("ISPermission: serviceName="
                        +serviceName);
                    debug.message("ISPermission: actions="+actions);
                }
                SSOTokenPrincipal tokenPrincipal = null;
                try {
                    Principal[] principals = protectionDomain.getPrincipals();
                    // principals should have only one entry
                    Principal principal = (Principal)principals[0];
                    if (principal.getName().equals("com.sun.identity."
                        +"authentication.service.SSOTokenPrincipal")) {
                        if (debug.messageEnabled()) {
                            debug.message("ISPermission::implies:principals:"
                                +principal.toString());
                        }
                        tokenPrincipal = (SSOTokenPrincipal) principal;
                    }
                    if (tokenPrincipal == null) {
                        if (debug.messageEnabled()) {
                            debug.error("ISPermission::implies:"
                                + " Principal is null");
                        }
                    } else {
                        SSOTokenManager ssomgr = SSOTokenManager.getInstance();
                        final SSOToken token =
                            ssomgr.createSSOToken(tokenPrincipal.getName());
                        /* TODO currently ISPermission uses remote policy 
                        client API so if this class gets used from server side
                        , will always make remote call, need to make changes 
                        in this code to to make a local/remote call accordingly.
                        */
                            if (policyEvalFactory == null) { 
                             policyEvalFactory = 
                                PolicyEvaluatorFactory.getInstance(); 
                            } 
                            PolicyEvaluator policyEvaluator = 
                        policyEvalFactory.
                            getPolicyEvaluator(serviceName);
                        if (debug.messageEnabled()) {
                            debug.message("ISPermission::implies::created "
                                + "PolicyEvaluator for "+serviceName);
                        }
                        if (actions != null) {
                                   StringTokenizer st = 
                                new StringTokenizer(actions,",");
                                while (st.hasMoreTokens()) {
                                String action = (String)st.nextToken();
                                allowed  =  policyEvaluator.isAllowed(token, 
                                    resourceName , action ,envParams);
                                    if (!allowed) {
                                    break; // the final result is not allowwed
                                }
                                if (debug.messageEnabled()) {
                                    debug.message("ISPermission::result for "
                                        + action+" is :"+allowed);
                                }
                            }
                            if (debug.messageEnabled()) {
                                debug.message("ISPermission::result for "
                                    + actions+" is :"+allowed);
                            }
                        } else {
                            if (debug.messageEnabled()) {
                                debug.message("ISPermission:: actions is null");
                            }
                        }
                    }
                } catch (SSOException ssoe ) {
                    if (debug.messageEnabled()) {
                        debug.error("ISPermission::SSOException:"
                            +ssoe.getMessage());
                        ssoe.printStackTrace();
                    }
                } catch (Exception  e ) {
                    if (debug.messageEnabled()) {
                        debug.error("ISPermission::Exception:"
                            +e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                debug.message("ISPermission:: subject was null");
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ISPermission: allowed::"+allowed);
        }
        return allowed;
    }

    /**
     * Returns a <code>java.security.PermissionCollection</code> to store this 
     * kind of Permission.
     *
     * @return an instance of <code>ISPermissionCollection</code>
     */
    public PermissionCollection newPermissionCollection() {
        debug.message("ISPermission:: newISPermissionCollection() called");
        return new ISPermissionCollection();
    }


    /**
     * Returns a string describing this Permission. 
     * @return <code>String</code> containing information about this Permission.
     */
    public String toString() {
        StringBuffer str = new StringBuffer(200);
        str = str.append("(").append(getClass().getName()).append("\n");
        String actions = getActions();
        if (subject != null) {
            str = str.append(subject.toString()).append("\n");
        }
        if (codesource != null) {
            str = str.append(codesource.toString()).append("\n");
        }
        if ((serviceName != null) && (serviceName.length() != 0)) { 
            str = str.append("serviceName=").append(serviceName).append("\n");
        }
        if ((resourceName != null) && (resourceName.length() != 0)) { 
            str = str.append("resourceName=").append(resourceName).append("\n");
        }
        if ((actions != null) && (actions.length() != 0)) { 
            str = str.append("actions=").append(actions).append("\n");
        }
        if ((envParams != null) && !(envParams.isEmpty())) { 
            str = str.append("envParams=").append(envParams.values())
                     .append("\n");
        }
        str.append(")");
        return str.toString();
    }
}
