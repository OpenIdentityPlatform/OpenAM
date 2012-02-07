package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.List;
import javax.faces.application.FacesMessage;

public class ResourcesReferralWizardStepValidator extends ReferralWizardStepValidator {
    public ResourcesReferralWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        List<Resource> resources = getReferralWizardBean().getReferralBean().getResources();
        if (resources != null) {
            for (Resource r : getReferralWizardBean().getReferralBean().getResources()) {
                ApplicationResource rr = (ApplicationResource) r;
                ViewEntitlement ve = rr.getViewEntitlement();
                if (ve.getResources() != null && ve.getResources().size() > 0) {
                    return true;
                }
            }
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "noResourcesSummary"));
        mb.setDetail(r.getString(this, "noResourcesDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        getMessagesBean().addMessageBean(mb);

        return false;
    }
}