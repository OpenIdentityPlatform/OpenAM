package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.NamePattern;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.PolicyDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;
import java.util.regex.Matcher;
import javax.faces.application.FacesMessage;

public class NamePolicyCreateWizardStepValidator extends PolicyWizardStepValidator {
    public NamePolicyCreateWizardStepValidator(WizardBean wizardBean) {
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
        String policyName = getPolicyWizardBean().getPrivilegeBean().getName();
        Matcher matcher = NamePattern.get().matcher(policyName);

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
        getPolicyWizardBean().setPolicyNameInputEffect(e);

        e = new MessageErrorEffect();
        getPolicyWizardBean().setPolicyNameMessageEffect(e);

        getMessagesBean().addMessageBean(mb);

        return false;
    }

    private boolean validateNotExists() {
        if (PolicyDao.getInstance().privilegeExists(getPolicyWizardBean().getPrivilegeBean())) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "existsSummary"));
            mb.setDetail(r.getString(this, "existsDetail", getPolicyWizardBean().getPrivilegeBean().getName()));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            getMessagesBean().addMessageBean(mb);

            return false;
        }

        return true;

    }
}