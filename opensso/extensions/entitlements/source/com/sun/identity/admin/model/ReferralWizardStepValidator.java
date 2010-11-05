package com.sun.identity.admin.model;

public abstract class ReferralWizardStepValidator extends WizardStepValidator {
    public ReferralWizardStepValidator(WizardBean wb) {
        super(wb);
    }

    protected ReferralWizardBean getReferralWizardBean() {
        return (ReferralWizardBean)getWizardBean();
    }
}
