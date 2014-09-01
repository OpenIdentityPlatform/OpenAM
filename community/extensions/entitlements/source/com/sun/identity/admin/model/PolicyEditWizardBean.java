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
 * $Id: PolicyEditWizardBean.java,v 1.3 2009/07/27 19:35:25 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.PolicyDao;

public class PolicyEditWizardBean extends PolicyWizardBean {
    private String privilegeName;

    @Override
    public void reset() {
        super.reset();

        setAllEnabled(true);
        gotoStep(4);
    }
    public static PolicyEditWizardBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        PolicyEditWizardBean pewb = (PolicyEditWizardBean)mbr.resolve("policyEditWizardBean");
        return pewb;
    }

    public void setPrivilegeName(String privilegeName) {
        this.privilegeName = privilegeName;
    }

    protected void resetPrivilegeBean() {
        PrivilegeBean pb = PolicyDao.getInstance().getPrivilegeBean(privilegeName);
        setPrivilegeBean(pb);
    }
}
