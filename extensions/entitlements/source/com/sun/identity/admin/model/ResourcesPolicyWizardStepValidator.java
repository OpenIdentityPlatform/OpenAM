package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.List;
import javax.faces.application.FacesMessage;

public class ResourcesPolicyWizardStepValidator extends PolicyWizardStepValidator {
    public ResourcesPolicyWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        List<Resource> resources =
                getPolicyWizardBean().getPrivilegeBean().getViewEntitlement().getResources();

        if (resources == null || resources.size() == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noResourcesSummary"));
            mb.setDetail(r.getString(this, "noResourcesDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            getMessagesBean().addMessageBean(mb);

            return false;
        }

        return true;
    }
}