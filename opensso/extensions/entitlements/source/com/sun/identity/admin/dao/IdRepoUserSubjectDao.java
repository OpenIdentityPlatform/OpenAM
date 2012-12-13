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
 * $Id: IdRepoUserSubjectDao.java,v 1.9 2009/06/04 11:49:11 veiming Exp $
 */

package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOException;
import com.sun.identity.admin.model.IdRepoUserViewSubject;
import com.sun.identity.admin.model.ViewSubject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IdRepoUserSubjectDao extends IdRepoSubjectDao {

    private String namingAttribute;

    protected IdType getIdType() {
        return IdType.USER;
    }

    public String getNamingAttribute() {
        return namingAttribute;
    }

    public void setNamingAttribute(String namingAttribute) {
        this.namingAttribute = namingAttribute;
    }

    @Override
    protected IdSearchResults getIdSearchResults(IdSearchControl idsc, String pattern) {
        if (!pattern.equals("*")) {
            pattern = "*";
        }

        return super.getIdSearchResults(idsc, pattern);
    }

    @Override
    protected IdSearchControl getIdSearchControl(String pattern) {
        IdSearchControl idsc = super.getIdSearchControl(pattern);
        Map searchMap = new HashMap();

        searchMap.put(getNamingAttribute(), Collections.singleton(pattern));
        searchMap.put("cn", Collections.singleton(pattern));
        searchMap.put("sn", Collections.singleton(pattern));
        searchMap.put("employeeNumber", Collections.singleton(pattern));

        idsc.setSearchModifiers(IdSearchOpModifier.OR, searchMap);
        return idsc;
    }

    @Override
    protected void decorate(ViewSubject vs, Map attrs) {
        super.decorate(vs, attrs);

        if (attrs != null) {
            IdRepoUserViewSubject idus = (IdRepoUserViewSubject) vs;
            Set snSet = (Set) attrs.get("sn");
            if (snSet != null && snSet.size() > 0) {
                String sn = (String) snSet.iterator().next();
                idus.setSn(sn);
            }
            Set enSet = (Set) attrs.get("employeeNumber");
            if (enSet != null && enSet.size() > 0) {
                String en = (String) enSet.iterator().next();
                idus.setEmployeeNumber(en);
            }
        }
    }
}
