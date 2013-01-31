/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: UMChangeUserPasswordModelImpl.java,v 1.3 2009/09/28 18:59:56 babysunil Exp $
 *
 */
/**
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */
package com.sun.identity.console.user.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class UMChangeUserPasswordModelImpl
    extends AMModelBase
    implements UMChangeUserPasswordModel
{
  
    public UMChangeUserPasswordModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns user name.
     *
     * @param userId Universal ID of user.
     * @return user name.
     */
    public String getUserName(String userId) {
        String userName = "";
        try {        
            AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
            userName = amid.getName();
        } catch (IdRepoException e) {
            debug.warning("UMChangeUserPasswordModelImpl.getUserName", e);
        }
        return userName;
    }

    /** 
     * Returns user password.
     *
     * @param userId Universal ID of user.
     * @return user password.
     * @throws AMConsoleException if password cannot be obtained.
     */
    public String getPassword(String userId)
        throws AMConsoleException {
        String password = "";
        String[] params = {userId, AMAdminConstants.ATTR_USER_PASSWORD};

        try {
            logEvent("ATTEMPT_READ_IDENTITY_ATTRIBUTE_VALUE", params);
            AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
            Set set = amid.getAttribute(AMAdminConstants.ATTR_USER_PASSWORD);

            if ((set != null) && !set.isEmpty()) {
                password = (String)set.iterator().next();
            }

            logEvent("SUCCEED_READ_IDENTITY_ATTRIBUTE_VALUE", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_PASSWORD,
                strError};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_PASSWORD,
                strError};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            throw new AMConsoleException(strError);
        }

        return password;
    }


    /**
     * Modifies user password.
     *
     * @param userId Universal ID of user.
     * @param password New password.
     * @throws AMConsoleException if password cannot be modified.
     */
    public void changePassword(String userId, String password)
        throws AMConsoleException
    {
        String[] params = {userId, AMAdminConstants.ATTR_USER_PASSWORD};
        try {        
            logEvent("ATTEMPT_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);

            AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
            Map map = new HashMap(2);
            Set set = new HashSet(2);
            set.add(password);
            map.put(AMAdminConstants.ATTR_USER_PASSWORD, set);
            amid.setAttributes(map);
            amid.store();

            logEvent("SUCCEED_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_PASSWORD,
                strError};
            logEvent("SSO_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_PASSWORD,
                strError};
            logEvent("IDM_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
      * Modifies user password after validating old password.
      *
      * @param userId Universal ID of user.
      * @param oldpwd old password.
      * @param newpwd New password.
      * @throws AMConsoleException if password cannot be modified.
      */
     public void changePwd(String userId, String oldpwd, String newpwd)
         throws AMConsoleException {
         String[] params = {userId, AMAdminConstants.ATTR_USER_OLD_PASSWORD};
         try {
             logEvent("ATTEMPT_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);
 
             AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
             amid.changePassword(oldpwd, newpwd);
 
             logEvent("SUCCEED_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);
         } catch (SSOException e) {
             String strError = getErrorString(e);
             String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_OLD_PASSWORD,
                 strError};
             logEvent("SSO_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                 paramsEx);
             throw new AMConsoleException(strError);
         } catch (IdRepoException e) {
             String strError = getErrorString(e);
             String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_OLD_PASSWORD,
                 strError};
             logEvent("IDM_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                 paramsEx);
             throw new AMConsoleException(strError);
         }
     }

     /**
      * {@inheritDoc}
      */
     public boolean isOldPasswordRequired() {
        Map<String, Set<String>> attrs = getConsoleAttributes();
        Set<String> vals = attrs.get(AMAdminConstants.ATTR_USER_OLD_PASSWORD);
        return Boolean.valueOf(vals.iterator().next());
     }
}
