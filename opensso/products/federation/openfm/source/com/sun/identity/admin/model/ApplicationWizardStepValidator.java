package com.sun.identity.admin.model;

public abstract class ApplicationWizardStepValidator extends WizardStepValidator {
    public ApplicationWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected ApplicationWizardBean getApplicationWizardBean() {
        return (ApplicationWizardBean)getWizardBean();
    }
}
