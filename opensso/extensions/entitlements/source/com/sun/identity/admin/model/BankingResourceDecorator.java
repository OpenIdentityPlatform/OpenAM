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
 * $Id: BankingResourceDecorator.java,v 1.3 2009/06/04 11:49:14 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SubjectDao;
import java.util.List;

public class BankingResourceDecorator extends ResourceDecorator {

    private SubjectDao subjectDao;

    public void setSubjectDao(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    public void decorate(Resource r) {
        assert (r instanceof BankingResource);
        BankingResource br = (BankingResource) r;

        if (br.equals(br.ALL_ACCOUNTS)) {
            ViewSubject vs = subjectDao.getSubjectType().newViewSubject();
            vs.setName("*");
            br.setOwner(vs);
        } else {

            List<ViewSubject> vss = subjectDao.getViewSubjects(br.getName());
            assert (vss.size() <= 1);

            if (vss.size() == 1) {
                br.setOwner(vss.get(0));
            }
        }
    }
}
