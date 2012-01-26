package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.SamlV2CreateSharedDao;

public class MetadataSamlV2RemoteIdpCreateWizardStepValidator extends SamlV2RemoteIdpCreateWizardStepValidator {
    public MetadataSamlV2RemoteIdpCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        boolean usingMetaDataFile = getSamlV2RemoteIdpCreateWizardBean().isMeta();

        if (!usingMetaDataFile) {

            String url = getSamlV2RemoteIdpCreateWizardBean().getMetaUrl();

            if ((url == null) || (url.length() == 0)) {
                popUpErrorMessage(
                        "invalidMetaUrlSummary",
                        "invalidMetaUrlDetail",
                        SamlV2RemoteIdpCreateWizardStep.METADATA.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateUrl(url)) {
                popUpErrorMessage(
                        "urlErrorSummary",
                        "urlErrorDetail",
                        SamlV2RemoteIdpCreateWizardStep.METADATA.toInt());
                return false;
            }

        } else {

            String meta = getSamlV2RemoteIdpCreateWizardBean().getStdMetaFile();

            if ((meta == null) || (meta.length() == 0)) {
                popUpErrorMessage(
                        "invalidMetafileSummary",
                        "invalidMetafileDetail",
                        SamlV2RemoteIdpCreateWizardStep.METADATA.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateMetaFormat(meta)) {
                getSamlV2RemoteIdpCreateWizardBean().setStdMetaFilename("");
                getSamlV2RemoteIdpCreateWizardBean().setStdMetaFile("");
                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatDetail",
                        SamlV2RemoteIdpCreateWizardStep.METADATA.toInt());
                return false;
            }
        }

        return true;
    }
}