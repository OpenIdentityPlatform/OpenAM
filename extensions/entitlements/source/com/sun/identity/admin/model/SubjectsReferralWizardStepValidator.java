package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.List;
import javax.faces.application.FacesMessage;

public class SubjectsReferralWizardStepValidator extends ReferralWizardStepValidator {
    public SubjectsReferralWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        List<RealmBean> realmBeans = getReferralWizardBean().getReferralBean().getRealmBeans();
        if (realmBeans == null || realmBeans.size() == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noSubjectsSummary"));
            mb.setDetail(r.getString(this, "noSubjectsDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            getMessagesBean().addMessageBean(mb);

            return false;
        }

        return true;
    }
}