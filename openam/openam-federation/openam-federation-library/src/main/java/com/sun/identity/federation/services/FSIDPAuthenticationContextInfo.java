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
 * $Id: FSIDPAuthenticationContextInfo.java,v 1.3 2008/11/10 22:56:58 veiming Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.shared.validation.IntegerValidator;
import com.sun.identity.shared.validation.ValidationException;
import java.io.Serializable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.HashMap;

/**
 * This class is used to store the IDP Authentication Context Information.
 */
public class FSIDPAuthenticationContextInfo
    implements Serializable
{

    private String authenticationContext = null;
    private String moduleIndicatorKey = null;
    private String moduleIndicatorValue = null;
    private int level = -1;

    /**
     * Default constructor.
     */
    public FSIDPAuthenticationContextInfo() {
    }
    
    /**
     * Constructs a new object.
     * @param infoString a string that contains one idp authentication context
     *  mapping
     */
    public FSIDPAuthenticationContextInfo(String infoString)
	throws FSException
    {
        if (infoString == null) {
            throw new FSException("nullInput", null);
        }
        StringTokenizer stk = new StringTokenizer(
            infoString, IFSConstants.ATTRIBUTE_SEPARATOR);
        while (stk.hasMoreTokens()) {
            String token = stk.nextToken();
            int equalSign = token.indexOf(IFSConstants.KEY_VALUE_SEPARATOR);
            if (equalSign == -1) {
                throw new FSException("wrongInput", null);
            }
            try {
                String key = token.substring(0, equalSign);
                String value = 
                    token.substring(equalSign +1, token.length());
                if (key.equalsIgnoreCase(IFSConstants.AUTH_CONTEXT_NAME)) {
                    authenticationContext = value;
                } else if (key.equalsIgnoreCase(
                    IFSConstants.MODULE_INDICATOR_KEY)) 
                {
                    moduleIndicatorKey = value;
                } else if (key.equalsIgnoreCase(
                    IFSConstants.MODULE_INDICATOR_VALUE))
                {
                    moduleIndicatorValue = value;
                } else if (key.equalsIgnoreCase(
                    IFSConstants.LEVEL))
                {
                    level = Integer.parseInt(value);
                } else {
                    throw new FSException("wrongInput", null);
                }
            } catch (IndexOutOfBoundsException ie) {
                throw new FSException("wrongInput", null);
            } catch (NumberFormatException ne) {
                throw new FSException("wrongInput", null);
            }
        }
        if (authenticationContext == null ||
            moduleIndicatorKey == null ||
            moduleIndicatorValue == null ||
            level == -1)
        {
            throw new FSException("wrongInput", null);
        }
    }
  
    /**
     * Creates new <code>FSIDPAuthenticationContextInfo</code> instance.
     *
     * @param authenticationContext Authentication Context.
     * @param moduleIndicatorKey module indicator key.
     * @param moduleIndicatorValue module indicator value.
     * @param level Level of the auth module
     * @throws FSException if <code>authenticationContext</code> is blank.
     */
    public FSIDPAuthenticationContextInfo(
        String authenticationContext,
        String moduleIndicatorKey,
        String moduleIndicatorValue,
        int level
    ) throws FSException
    {
	setAuthenticationContext(authenticationContext);
	setLevel(level);
	setModuleIndicatorKey(moduleIndicatorKey);
	setModuleIndicatorValue(moduleIndicatorValue);
    }
  
    /**
     * Creates new <code>FSIDPAuthenticationContextInfo</code> instance.
     *
     * @param authenticationContext Authentication Context.
     * @param moduleIndicatorKey module indicator key.
     * @param moduleIndicatorValue module indicator value.
     * @param level Level
     * @throws FSException if <code>authenticationContext</code> is blank,
     *  or cannot obtain an int from <code>level</code>.
     */
    public FSIDPAuthenticationContextInfo(
        String authenticationContext,
        String moduleIndicatorKey,
        String moduleIndicatorValue,
        String level
    ) throws FSException
    {
	setAuthenticationContext(authenticationContext);
	setLevel(level);
	setModuleIndicatorKey(moduleIndicatorKey);
	setModuleIndicatorValue(moduleIndicatorValue);
    }
  
    /**
     * Returns authentication context.
     * @return authentication context.
     */
    public String getAuthenticationContext() {
        return authenticationContext;
    }
    
    /**
     * Sets authentication context.
     *
     * @param authenticationContext authentication context.
     * @throws FSException if
     *         <code>authenticationContext</code> is blank.
     */
    public void setAuthenticationContext(String authenticationContext)
	throws FSException
    {
	if ((authenticationContext == null) ||
	    (authenticationContext.trim().length() == 0)
	) {
	    throw new FSException(
		IFSConstants.META_MISSING_AUTH_CONTEXT, null);
	}

	this.authenticationContext = authenticationContext;
    }
    
    /**
     * Returns level.
     *
     * @return level.
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Sets level.
     *
     * @param level Strength level.
     * @throws FSException if <code>level</code> is not
     *         an integer.
     */
    public void setLevel(String level)
	throws FSException
    {
	IntegerValidator validator = IntegerValidator.getInstance();

	try {
	    validator.validate(level);
	    this.level = Integer.parseInt(level);
	} catch (ValidationException e) {
	    throw new FSException(
		IFSConstants.META_INVALID_LEVEL, null);
	}
    }
    
    /**
     * Sets level.
     *
     * @param level level of the auth module.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns module indicator key.
     * @return module indicator key.
     */
    public String getModuleIndicatorKey() {
        return moduleIndicatorKey;
    }

    /**
     * Sets module indicator key.
     *
     * @param moduleIndicatorKey module indicator key.
     */
    public void setModuleIndicatorKey(String moduleIndicatorKey) {
        this.moduleIndicatorKey = moduleIndicatorKey;
    }
    
    /**
     * Returns module indicator.
     * @return module indicator of the module that implements this
     *         Authentication Context.
     */
    public String getModuleIndicatorValue() {
        return moduleIndicatorValue;
    }
    
    /**
     * Sets module indicator value.
     * @param moduleIndicatorValue module indicator.
     */
    public void setModuleIndicatorValue(String moduleIndicatorValue) {
         this.moduleIndicatorValue = moduleIndicatorValue;
    }
    
}
