package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import javax.faces.application.FacesMessage;

public abstract class SamlV2RemoteSpCreateWizardStepValidator extends WizardStepValidator {
    public SamlV2RemoteSpCreateWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected SamlV2RemoteSpCreateWizardBean getSamlV2RemoteSpCreateWizardBean() {
        return (SamlV2RemoteSpCreateWizardBean)getWizardBean();
    }

    protected void popUpErrorMessage(String summary, String detail, int step) {
        getSamlV2RemoteSpCreateWizardBean().setStdMetaFileProgress(0);
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summary));
        mb.setDetail(r.getString(this, detail));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        Effect e = new InputFieldErrorEffect();
        getSamlV2RemoteSpCreateWizardBean().setSamlV2RemoteCreateEntityInputEffect(e);
        getMessagesBean().addMessageBean(mb);
    }

}
