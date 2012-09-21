package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class CotSamlV2RemoteSpCreateWizardStepValidator extends SamlV2RemoteSpCreateWizardStepValidator {

    public CotSamlV2RemoteSpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingExitingCot = getSamlV2RemoteSpCreateWizardBean().isCot();
        String cotname = getSamlV2RemoteSpCreateWizardBean().getNewCotName();

        if (!usingExitingCot) {
            if ((cotname == null) || (cotname.length() == 0)) {
                popUpErrorMessage(
                        "invalidCotSummary",
                        "invalidCotDetail",
                        SamlV2RemoteSpCreateWizardStep.COT.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateCot(cotname)) {
                popUpErrorMessage(
                        "cotExistSummary",
                        "cotExistDetail",
                        SamlV2RemoteSpCreateWizardStep.COT.toInt());
                return false;
            }
        }
        return true;
    }
}
