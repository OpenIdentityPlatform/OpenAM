package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class CotSamlV2HostedIdpCreateWizardStepValidator extends SamlV2HostedIdpCreateWizardStepValidator {
    public CotSamlV2HostedIdpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingExistingCot = getSamlV2HostedIdpCreateWizardBean().isCot();
        String cotname = getSamlV2HostedIdpCreateWizardBean().getNewCotName();

        if (!usingExistingCot) {
            if ((cotname == null) || cotname.length() == 0) {
                popUpErrorMessage(
                        "invalidCotSummary",
                        "invalidCotDetail",
                        SamlV2HostedIdpCreateWizardStep.COT.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateCot(cotname)) {
                popUpErrorMessage(
                        "cotExistSummary",
                        "cotExistDetail",
                        SamlV2HostedIdpCreateWizardStep.COT.toInt());
                return false;
            }
        }
        return true;
    }
}