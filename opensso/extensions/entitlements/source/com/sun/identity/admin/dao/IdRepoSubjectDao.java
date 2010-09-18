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
 * $Id: IdRepoSubjectDao.java,v 1.9 2009/06/04 11:49:11 veiming Exp $
 */

package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOException;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.IdRepoViewSubject;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.ViewSubject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class IdRepoSubjectDao extends SubjectDao {

    private int timeout = 5;
    private int limit = 100;

    protected abstract IdType getIdType();

    protected ViewSubject newViewSubject(AMIdentity ami) {
        IdRepoViewSubject idvs = (IdRepoViewSubject) getSubjectType().newViewSubject();
        idvs.setName(ami.getUniversalId());
        Map attrs;
        try {
            attrs = ami.getAttributes();
        } catch (IdRepoException idre) {
            attrs = null;
        } catch (SSOException ssoe) {
            attrs = null;
        }
        decorate(idvs, attrs);

        return idvs;
    }

    public List<ViewSubject> getViewSubjects() {
        return getViewSubjects("");
    }

    protected AMIdentity getAMIdentity(String name) {
        try {
            return IdUtils.getIdentity(new Token().getAdminSSOToken(), name);
        } catch (IdRepoException idre) {
            throw new RuntimeException(idre);
        }
    }

    protected ViewSubject getViewSubject(String name) {
        AMIdentity ami = getAMIdentity(name);
        ViewSubject vs = newViewSubject(ami);

        return vs;
    }

    protected IdSearchControl getIdSearchControl(String pattern) {
        IdSearchControl idsc = new IdSearchControl();
        idsc.setMaxResults(limit);
        idsc.setTimeOut(timeout);
        idsc.setAllReturnAttributes(true);

        return idsc;
    }

    protected IdSearchResults getIdSearchResults(IdSearchControl idsc, String pattern) {
        IdType idType = getIdType();
        String realmName = RealmsBean.getInstance().getRealmBean().getName();

        try {
            AMIdentityRepository repo = new AMIdentityRepository(new Token().getSSOToken(), realmName);
            IdSearchResults results = repo.searchIdentities(idType, pattern, idsc);
            return results;
        } catch (IdRepoException e) {
            throw new RuntimeException(e);
        } catch (SSOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getPattern(String filter) {
        String pattern;
        if (filter == null || filter.length() == 0) {
            pattern = "*";
        } else {
            pattern = "*" + filter + "*";
        }

        return pattern;
    }

    public List<ViewSubject> getViewSubjects(String filter) {
        String pattern = getPattern(filter);

        List<ViewSubject> subjects = new ArrayList<ViewSubject>();

        String realmName = null;
        if (realmName == null) {
            realmName = "/";
        }

        IdSearchControl idsc = getIdSearchControl(pattern);
        IdSearchResults results = getIdSearchResults(idsc, pattern);

        for (Object o : results.getSearchResults()) {
            AMIdentity ami = (AMIdentity) o;
            String uuid = ami.getUniversalId();
            ViewSubject vs = newViewSubject(ami);
            vs.setName(uuid);
            subjects.add(vs);
        }

        return subjects;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void decorate(ViewSubject vs) {
        assert (vs instanceof IdRepoViewSubject);
        AMIdentity ami = getAMIdentity(vs.getName());

        Map attrs;
        try {
            attrs = ami.getAttributes();
        } catch (IdRepoException idre) {
            attrs = null;
        } catch (SSOException ssoe) {
            attrs = null;
        }
        if (attrs != null) {
            decorate(vs, attrs);
        }
    }

    protected void decorate(ViewSubject vs, Map attrs) {
        if (attrs != null) {
            IdRepoViewSubject idvs = (IdRepoViewSubject) vs;
            Set cnSet = (Set) attrs.get("cn");
            if (cnSet != null && cnSet.size() > 0) {
                String cn = (String) cnSet.iterator().next();
                idvs.setCn(cn);
            }
        }
    }
}
