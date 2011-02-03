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
 * $Id: SubjectsPolicySummary.java,v 1.6 2009/06/11 19:20:40 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;

public class SubjectsPolicySummary extends PolicySummary {

    public SubjectsPolicySummary(PolicyWizardBean policyWizardBean) {
        super(policyWizardBean);
    }

    public String getLabel() {
        Resources r = new Resources();
        String label = r.getString(this, "label");
        return label;
    }

    public String getValue() {
        ViewSubject vs = getPolicyWizardBean().getPrivilegeBean().getViewSubject();
        if (vs == null) {
            return "0";
        }
        return Integer.toString(vs.getSizeLeafs());
    }

    public boolean isExpandable() {
        return getPolicyWizardBean().getPrivilegeBean().getViewSubject().getSizeLeafs() > 0;

    }
    public String getIcon() {
        return "../image/role.png";
    }

    public String getTemplate() {
        return "/admin/facelet/template/policy-summary-subjects.xhtml";
    }

    public int getGotoStep() {
        return PolicyWizardStep.SUBJECTS.toInt();
    }

}
