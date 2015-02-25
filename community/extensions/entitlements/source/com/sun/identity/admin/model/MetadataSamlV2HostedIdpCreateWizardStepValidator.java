package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class MetadataSamlV2HostedIdpCreateWizardStepValidator extends SamlV2HostedIdpCreateWizardStepValidator {

    public MetadataSamlV2HostedIdpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingMetaDataFile =
                getSamlV2HostedIdpCreateWizardBean().isMeta();

        if (!usingMetaDataFile) {
            String newEntityName =
                    getSamlV2HostedIdpCreateWizardBean().getNewEntityName();
            if ((newEntityName == null) || (newEntityName.length() == 0) || (!SamlV2CreateSharedDao.getInstance().valideEntityName(newEntityName))) {
                popUpErrorMessage(
                        "invalidNameSummary",
                        "invalidNameDetail",
                        SamlV2HostedIdpCreateWizardStep.METADATA.toInt());
                return false;
            }

        } else {

            String stdFilename = getSamlV2HostedIdpCreateWizardBean().getStdMetaFile();
            String extFilename = getSamlV2HostedIdpCreateWizardBean().getExtMetaFile();
            if ((stdFilename == null) || (stdFilename.length() == 0) || (extFilename == null) || (extFilename.length() == 0)) {
                popUpErrorMessage(
                        "invalidMetafileSummary",
                        "invalidMetafileDetail",
                        SamlV2HostedIdpCreateWizardStep.METADATA.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateMetaFormat(stdFilename)) {
                getSamlV2HostedIdpCreateWizardBean().setStdMetaFilename("");
                getSamlV2HostedIdpCreateWizardBean().setStdMetaFile("");
                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatDetail",
                        SamlV2HostedIdpCreateWizardStep.METADATA.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().valideaExtendedMetaFormat(extFilename)) {
                getSamlV2HostedIdpCreateWizardBean().setExtMetaFilename("");
                getSamlV2HostedIdpCreateWizardBean().setExtMetaFile("");
                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatSummaryDetail",
                        SamlV2HostedIdpCreateWizardStep.METADATA.toInt());
                return false;
            }
        }

        return true;
    }
}
