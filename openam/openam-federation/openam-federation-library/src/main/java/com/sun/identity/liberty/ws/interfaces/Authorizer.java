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
 * $Id: Authorizer.java,v 1.2 2008/06/25 05:47:18 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.interfaces;

/**
 * This class <code>Authorizer</code> is an interface for identity service
 * to check authorization of a <code>WSC</code>.
 * @supported.all.api
 */
public interface Authorizer {

    /**
     * Key of a parameter Map which contains information useful for policy
     * evaluation. The value of this key is id of the user whose resource id
     * being accessed.
     */
    public static final String USER_ID = "userID";

    /**
     * Key of a parameter Map which contains information useful for policy
     * evaluation. The value of this key is the authentication mechanism
     * web service consumer used.
     */
    public static final String AUTH_TYPE = "authType";

    /**
     * Key of a parameter Map which contains information useful for policy
     * evaluation. The value of this key is
     * <code>com.sun.identity.liberty.ws.soapbinding.Message</code>.
     */
    public static final String MESSAGE = "message";


    /**
     * Checks if the <code>WSC</code> is authorized to query or modify the
     * select data.
     *
     * @param credential credential of a <code>WSC</code>.
     * @param action request action.
     * @param data Object who is being accessed.
     * @param env A Map contains information useful for policy evaluation.
     *          The following key is defined and its value should be 
     *          passed in:
     *          Key: <code>USER_ID</code>
     *          Value: id of the user whose resource is being accessed.
     *          Key: <code>AUTH_TYPE</code>
     *          Value: The authentication mechanism <code>WSC</code> used.
     *          Key: <code>MESSAGE</code>
     *          Value:
     *          <code>com.sun.identity.liberty.ws.soapbinding.Message
     *          </code>.
     * @return true if the <code>WSC</code> is authorized.
     */
    public boolean isAuthorized(Object credential, String action, 
                                Object data, java.util.Map env);

    /**
     * Returns authorization decision for the given action(query or modify)
     * and to the given select data 
     * @param credential credential of a <code>WSC</code>.
     * @param action request action.
     * @param data Object who is being accessed.
     * @param env A Map contains information useful for policy evaluation.
     *          The following key is defined and its value should be passed in:
     *          Key: <code>USER_ID</code>
     *          Value: id of the user whose resource is being accessed.
     *          Key: <code>AUTH_TYPE</code>
     *          Value: The authentication mechanism <code>WSC</code> used.
     *          Key: <code>MESSAGE</code>
     *          Value:
     *          <code>com.sun.identity.liberty.ws.soapbinding.Message</code>.
     * @return <code>AuthorizationDecision</code> object contains authorization 
     *             decision information for the given resource.
     * @exception Exception
     */
    public Object getAuthorizationDecision(
                  Object credential,
                  String action,
                  Object data,
                  java.util.Map env)
    throws Exception;

}
