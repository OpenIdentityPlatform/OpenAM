package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class CotSamlV2RemoteIdpCreateWizardStepValidator extends SamlV2RemoteIdpCreateWizardStepValidator {
    public CotSamlV2RemoteIdpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingExitingCot = getSamlV2RemoteIdpCreateWizardBean().isCot();
        String cotname = getSamlV2RemoteIdpCreateWizardBean().getNewCotName();

        if (!usingExitingCot) {
            if ((cotname == null) || (cotname.length() == 0)) {
                popUpErrorMessage(
                        "invalidCotSummary",
                        "invalidCotDetail",
                        SamlV2RemoteIdpCreateWizardStep.COT.toInt());

                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateCot(cotname)) {
                popUpErrorMessage(
                        "cotExistSummary",
                        "cotExistDetail",
                        SamlV2RemoteIdpCreateWizardStep.COT.toInt());
                return false;
            }
        }
        return true;
    }
}