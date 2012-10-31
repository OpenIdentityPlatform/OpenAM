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
 * $Id: IdentitySubjectModelImpl.java,v 1.2 2008/06/25 05:43:07 qcheng Exp $
 *
 */

package com.sun.identity.console.policy.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class IdentitySubjectModelImpl
    extends PolicyModelImpl
    implements IdentitySubjectModel
{
    public IdentitySubjectModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns entity names.
     *
     * @param pattern Search Pattern.
     * @param strType Entity Type.
     * @param realmName Name of Realm.
     */
    public IdSearchResults getEntityNames(
        String realmName,
        String strType,
        String pattern
    ) throws AMConsoleException {
        if (realmName == null) {
            realmName = "/";
        }

        if ((pattern == null) || (pattern.trim().length() == 0)) {
            pattern = "*";
        }

        int sizeLimit = getSearchResultLimit();
        int timeLimit = getSearchTimeOutLimit();
        String[] params = {realmName, strType, pattern,
            Integer.toString(sizeLimit), Integer.toString(timeLimit)};

        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                getUserSSOToken(), realmName);
            IdType type = IdUtils.getType(strType);
                                                                                
            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setMaxResults(sizeLimit);
            idsc.setTimeOut(timeLimit);

            logEvent("ATTEMPT_SEARCH_IDENTITY", params);
            IdSearchResults results = repo.searchIdentities(
                type, pattern, idsc);
            logEvent("SUCCEED_SEARCH_IDENTITY", params);
            return results;
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strType, pattern,
                Integer.toString(sizeLimit), Integer.toString(timeLimit),
                strError};
            logEvent("IDM_EXCEPTION_SEARCH_IDENTITY", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strType, pattern,
                Integer.toString(sizeLimit), Integer.toString(timeLimit),
                strError};
            logEvent("SSO_EXCEPTION_SEARCH_IDENTITY", paramsEx);
            throw new AMConsoleException(strError);
        }
    }
}
