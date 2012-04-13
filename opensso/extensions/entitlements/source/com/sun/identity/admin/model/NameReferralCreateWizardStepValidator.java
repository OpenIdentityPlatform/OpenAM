package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.NamePattern;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.ReferralDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import java.util.regex.Matcher;
import javax.faces.application.FacesMessage;

public class NameReferralCreateWizardStepValidator extends ReferralWizardStepValidator {
    public NameReferralCreateWizardStepValidator(WizardBean wizardBean) {
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
            String name = getReferralWizardBean().getReferralBean().getName();
        Matcher matcher = NamePattern.get().matcher(name);

        if (!matcher.matches()) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "invalidNameSummary"));
            mb.setDetail(r.getString(this, "invalidNameDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            Effect e;

            e = new InputFieldErrorEffect();
            getReferralWizardBean().setNameInputEffect(e);

            getMessagesBean().addMessageBean(mb);

            return false;
        }

        return true;}

    private boolean validateNotExists() {
        if (ReferralDao.getInstance().referralExists(getReferralWizardBean().getReferralBean())) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "existsSummary"));
            mb.setDetail(r.getString(this, "existsDetail", getReferralWizardBean().getReferralBean().getName()));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            getMessagesBean().addMessageBean(mb);
            return false;
        }

        return true;
    }
}