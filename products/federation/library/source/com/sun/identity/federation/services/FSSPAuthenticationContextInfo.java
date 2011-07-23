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
 * $Id: FSSPAuthenticationContextInfo.java,v 1.3 2008/11/10 22:56:58 veiming Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.shared.validation.PositiveIntegerValidator;
import com.sun.identity.shared.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The <CODE>FSSPAuthenticationContextInfo</CODE> is used to contain the 
 * information about the AuthenticationContext to AuthenticationLevel mapping
 */
public class FSSPAuthenticationContextInfo {

    private String authenticationContext = null;
    private int authenticationLevel = -1;
    
    /** 
     * Creates new <code>FSSPAuthenticationContextInfo</code> object.
     */
    public FSSPAuthenticationContextInfo() {
    }
    
    /**
     * Constructs a new object.
     * @param mapping a string that contains service provider's authentication
     *  context mapping. It is retrieved from the provider's extended meta.
     * @throws FSException if an error occured during the parsing
     */
    public FSSPAuthenticationContextInfo(String mapping)
	throws FSException
    {
        if (mapping == null) {
            throw new FSException("nullInput", null);
        }
        StringTokenizer stk = new StringTokenizer(
            mapping, IFSConstants.ATTRIBUTE_SEPARATOR);
        while (stk.hasMoreTokens()) {
            String token = stk.nextToken();
            int equalSign = token.indexOf(IFSConstants.KEY_VALUE_SEPARATOR);
            if (equalSign == -1) {
                throw new FSException("wrongInput", null);
            }
            try {
                String key = token.substring(0, equalSign);
                String value = token.substring(equalSign+1, token.length());
                if (key.equalsIgnoreCase(IFSConstants.AUTH_CONTEXT_NAME)) {
                    authenticationContext = value;
                } else if (key.equalsIgnoreCase(IFSConstants.LEVEL)) {
                    authenticationLevel = Integer.parseInt(value);
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
            authenticationLevel == -1) {
            throw new FSException("wrongInput", null);
        }
    }

    /** 
     * Creates new FSSPAuthenticationContextInfo 
     * @param authenticationContext the authContext name.
     * @param authenticationLevel the authentication level.
     * @throws FSSPAuthenticationContextInfo if
     *         <code>authenticationContext</code> or
     *         <code>authenticationLevel</code> is invalid.
     */
    public FSSPAuthenticationContextInfo(
	String authenticationContext, 
	int authenticationLevel
    ) throws FSException
    {
	setAuthenticationContext(authenticationContext);
	setAuthenticationLevel(authenticationLevel);
    }

    /** 
     * Creates new FSSPAuthenticationContextInfo 
     *
     * @param authenticationContext the authContext name.
     * @param authenticationLevel the authentication level.
     * @throws FSSPAuthenticationContextInfo if
     *         <code>authenticationContext</code> or
     *         <code>authenticationLevel</code> is invalid.
     */
    public FSSPAuthenticationContextInfo(
	String authenticationContext, 
	String authenticationLevel
    ) throws FSException
    {
	setAuthenticationContext(authenticationContext);
	setAuthenticationLevel(authenticationLevel);
    }
    
    /**
     * Returns the authentication context. 
     * @return Authentication Context.
     */
    public String getAuthenticationContext() {
        return authenticationContext;
    }
    
    /**
     * Sets authentication context.
     *
     * @param authenticaionContext Authentication Context.
     * @throws FSException if
     *         <code>authenticaionContext</code> is blank.
     */
    public void setAuthenticationContext(String authenticaionContext)
	throws FSException
    {
	if ((authenticaionContext == null) ||
	    (authenticaionContext.trim().length() == 0)
	) {
	    throw new FSException(
		IFSConstants.META_MISSING_AUTH_CONTEXT, null);
	}
	
        this.authenticationContext = authenticaionContext;
    }
    
    /**
     * Returns authentication level.
     * @return Authentication Level.
     */
    public int getAuthenticationLevel() {
        return authenticationLevel;
    }
    
    /**
     * Sets authentication level.
     *
     * @param authenticationLevel Authentication Level.
     * @throws FSException if <code>authenticationLevel</code>
     *         is negative.
     */
    public void setAuthenticationLevel(int authenticationLevel) 
	throws FSException
    {
	if (authenticationLevel < 0) {
	    throw new FSException(
		IFSConstants.META_INVALID_LEVEL, null);
	}
        this.authenticationLevel = authenticationLevel;
    }
    
    /**
     * Sets authentication level.
     *
     * @param authenticationLevel Authentication Level.
     * @throws FSException if <code>authenticationLevel</code>
     *         is negative.
     */
    public void setAuthenticationLevel(String authenticationLevel) 
	throws FSException
    {
	PositiveIntegerValidator validator =
	    PositiveIntegerValidator.getInstance();

	try {
	    validator.validate(authenticationLevel);
	    this.authenticationLevel = Integer.parseInt(authenticationLevel);
	} catch (ValidationException e) {
	    throw new FSException(
		IFSConstants.META_INVALID_LEVEL, null);
	}
    }
    
}
