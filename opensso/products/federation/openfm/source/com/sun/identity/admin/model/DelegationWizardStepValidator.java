package com.sun.identity.admin.model;

public abstract class DelegationWizardStepValidator extends WizardStepValidator {
    public DelegationWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected DelegationWizardBean getDelegationWizardBean() {
        return (DelegationWizardBean)getWizardBean();
    }
}
