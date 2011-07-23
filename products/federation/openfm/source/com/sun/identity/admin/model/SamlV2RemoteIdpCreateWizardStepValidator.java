package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import javax.faces.application.FacesMessage;

public abstract class SamlV2RemoteIdpCreateWizardStepValidator extends WizardStepValidator {
    public SamlV2RemoteIdpCreateWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected SamlV2RemoteIdpCreateWizardBean getSamlV2RemoteIdpCreateWizardBean() {
        return (SamlV2RemoteIdpCreateWizardBean)getWizardBean();
    }

    protected void popUpErrorMessage(String summaryMsg, String detailMsg, int step) {
        getSamlV2RemoteIdpCreateWizardBean().setStdMetaFileProgress(0);
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summaryMsg));
        mb.setDetail(r.getString(this, detailMsg));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        Effect e = new InputFieldErrorEffect();
        getSamlV2RemoteIdpCreateWizardBean().setSamlV2RemoteCreateEntityInputEffect(e);
        getMessagesBean().addMessageBean(mb);
    }
}
