package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class MetadataSamlV2HostedSpCreateWizardStepValidator extends SamlV2HostedSpCreateWizardStepValidator {

    public MetadataSamlV2HostedSpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingMetaDataFile = getSamlV2HostedSpCreateWizardBean().isMeta();
        String newEntityName = getSamlV2HostedSpCreateWizardBean().getNewEntityName();

        if (!usingMetaDataFile) {
            if ((newEntityName == null) || newEntityName.length() == 0 || (!SamlV2CreateSharedDao.getInstance().valideEntityName(newEntityName))) {

                popUpErrorMessage(
                        "invalidEntityNameSummary",
                        "invalidEntityNameDetail",
                        SamlV2HostedSpCreateWizardStep.METADATA.toInt());

                return false;
            }

        } else {
            String stdFilename = getSamlV2HostedSpCreateWizardBean().getStdMetaFile();
            String extFilename = getSamlV2HostedSpCreateWizardBean().getExtMetaFile();
            if ((stdFilename == null) || (stdFilename.length() == 0) || (extFilename == null) || (extFilename.length() == 0)) {

                popUpErrorMessage(
                        "invalidMetafileSummary",
                        "invalidMetafileDetail",
                        SamlV2HostedSpCreateWizardStep.METADATA.toInt());

                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateMetaFormat(stdFilename)) {

                getSamlV2HostedSpCreateWizardBean().setStdMetaFilename("");
                getSamlV2HostedSpCreateWizardBean().setStdMetaFile("");

                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatDetail",
                        SamlV2HostedSpCreateWizardStep.METADATA.toInt());

                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().valideaExtendedMetaFormat(extFilename)) {

                getSamlV2HostedSpCreateWizardBean().setExtMetaFilename("");
                getSamlV2HostedSpCreateWizardBean().setExtMetaFile("");

                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatNameDetail",
                        SamlV2HostedSpCreateWizardStep.METADATA.toInt());

                return false;
            }

        }

        return true;
    }
}
