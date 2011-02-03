package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class CotSamlV2HostedSpCreateWizardStepValidator extends SamlV2HostedSpCreateWizardStepValidator {
    public CotSamlV2HostedSpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingExitingCot = getSamlV2HostedSpCreateWizardBean().isCot();
        String cotname = getSamlV2HostedSpCreateWizardBean().getNewCotName();

        if (!usingExitingCot) {
            if ((cotname == null) || (cotname.length() == 0)) {

                popUpErrorMessage(
                        "invalidCotSummary",
                        "invalidCotDetail",
                        SamlV2HostedSpCreateWizardStep.COT.toInt());

                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateCot(cotname)) {

                popUpErrorMessage(
                        "invalidCotSummary",
                        "invalidCotDetail",
                        SamlV2HostedSpCreateWizardStep.COT.toInt());
                return false;
            }

        }
        return true;
    }
}