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
 * $Id: NameDelegationWizardStepValidator.java,v 1.2 2009/11/20 18:34:24 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.NamePattern;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.DelegationDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;
import java.util.regex.Matcher;
import javax.faces.application.FacesMessage;

public class NameDelegationWizardStepValidator extends DelegationWizardStepValidator {
    public NameDelegationWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        if (!validatePattern()) {
            return false;
        }
        if (!validateNotExists()) {
            return false;
        }

        return true;
    }
    private boolean validatePattern() {
        String name = getDelegationWizardBean().getDelegationBean().getName();
        Matcher matcher = NamePattern.get().matcher(name);

        if (matcher.matches()) {
            return true;
        }
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidNameSummary"));
        mb.setDetail(r.getString(this, "invalidNameDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        Effect e;

        e = new InputFieldErrorEffect();
        getDelegationWizardBean().setNameInputEffect(e);

        getMessagesBean().addMessageBean(mb);

        return false;
    }

    private boolean validateNotExists() {
        if (DelegationDao.getInstance().exists(getDelegationWizardBean().getDelegationBean())) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "existsSummary"));
            mb.setDetail(r.getString(this, "existsDetail", getDelegationWizardBean().getDelegationBean().getName()));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            getMessagesBean().addMessageBean(mb);

            return false;
        }

        return true;

    }
}