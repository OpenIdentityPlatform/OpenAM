package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import javax.faces.application.FacesMessage;

public class SubjectsPolicyWizardStepValidator extends PolicyWizardStepValidator {

    public SubjectsPolicyWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        if (getPolicyWizardBean().getPrivilegeBean().getViewSubject() == null ||
                getPolicyWizardBean().getPrivilegeBean().getViewSubject().getSizeLeafs() == 0) {

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
