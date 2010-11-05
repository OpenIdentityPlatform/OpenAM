package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class MetadataSamlV2RemoteSpCreateWizardStepValidator extends SamlV2RemoteSpCreateWizardStepValidator {

    public MetadataSamlV2RemoteSpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingMetaDataFile = getSamlV2RemoteSpCreateWizardBean().isMeta();

        if (!usingMetaDataFile) {

            String url = getSamlV2RemoteSpCreateWizardBean().getMetaUrl();
            if ((url == null) || (url.length() == 0)) {
                popUpErrorMessage(
                        "invalidMetaUrlSummary",
                        "invalidMetaUrlDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }
            if (!SamlV2CreateSharedDao.getInstance().validateUrl(url)) {
                popUpErrorMessage(
                        "urlErrorSummary",
                        "urlErrorDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }

        } else {

            String meta = getSamlV2RemoteSpCreateWizardBean().getStdMetaFile();
            if ((meta == null) || meta.length() == 0) {
                popUpErrorMessage(
                        "invalidMetafileSummary",
                        "invalidMetafileDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateMetaFormat(meta)) {
                getSamlV2RemoteSpCreateWizardBean().setStdMetaFilename("");
                getSamlV2RemoteSpCreateWizardBean().setStdMetaFile("");
                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }
        }

        return true;
    }
}
