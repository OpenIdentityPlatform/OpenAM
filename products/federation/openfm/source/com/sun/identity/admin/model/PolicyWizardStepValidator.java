package com.sun.identity.admin.model;

public abstract class PolicyWizardStepValidator extends WizardStepValidator {
    public PolicyWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected PolicyWizardBean getPolicyWizardBean() {
        return (PolicyWizardBean)getWizardBean();
    }
}
