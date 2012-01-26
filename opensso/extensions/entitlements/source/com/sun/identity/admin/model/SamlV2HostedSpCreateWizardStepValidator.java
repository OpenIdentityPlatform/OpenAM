package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import javax.faces.application.FacesMessage;

public abstract class SamlV2HostedSpCreateWizardStepValidator extends WizardStepValidator {
    public SamlV2HostedSpCreateWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected SamlV2HostedSpCreateWizardBean getSamlV2HostedSpCreateWizardBean() {
        return (SamlV2HostedSpCreateWizardBean)getWizardBean();
    }

    protected void popUpErrorMessage(String summaryMsg, String detailMsg, int step) {
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summaryMsg));
        mb.setDetail(r.getString(this, detailMsg));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        Effect e = new InputFieldErrorEffect();
        getSamlV2HostedSpCreateWizardBean().setSamlV2HostedCreateEntityInputEffect(e);
        getMessagesBean().addMessageBean(mb);
    }

}
