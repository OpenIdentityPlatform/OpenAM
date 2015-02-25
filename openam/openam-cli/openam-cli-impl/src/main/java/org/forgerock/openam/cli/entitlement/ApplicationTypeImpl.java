/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.cli.entitlement;

import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Supporting class for ApplicationType CLI commands
 * @author Mark de Reeper mark.dereeper@forgerock.com
 */
public class ApplicationTypeImpl extends AuthenticatedCommand {
    
    public static final String PARAM_APPL_TYPE_NAME = "name";
    public static final String PARAM_APPL_TYPE_NAMES = "names";
    public static final String ATTR_APPLICATIONTYPE = "applicationType";
    public static final String ATTR_ACTIONS = "actions";
    public static final String ATTR_RESOURCE_COMPARATOR = "resourceComparator";
    public static final String ATTR_SAVE_INDEX = "saveIndexImpl";
    public static final String ATTR_SEARCH_INDEX = "searchIndexImpl";

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
    
    protected Subject getAdminSubject() {
        
        if (adminSubject == null) {
            adminSubject = SubjectUtils.createSubject(getAdminSSOToken());
        }
        
        return adminSubject;
    }

    protected void setApplicationTypeAttributes(ApplicationType applType,
        Map<String, Set<String>> attributeValues) throws CLIException {

        Map<String, Boolean> actions = getActions(attributeValues);
        if (actions != null) {
            applType.setActions(actions);
        }
    }

    protected Map<String, Boolean> getActions(Map<String, Set<String>> attributeValues) {
        
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
                String v = action.substring(idx++);
                results.put(a, Boolean.parseBoolean(v));
            }
        }
        
        return results;
    }
    
    protected Class getClassAttribute(String attributeName, Map<String, Set<String>> attributeValues) {
    
        Class result = null;
        
        Set<String> classes = attributeValues.get(attributeName);
     
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        
        for (String clazz : classes) {
            try {
                result = Class.forName(clazz);
                // Should only be one defined, take first one.
                break;
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }
        
        return result;
    }
}