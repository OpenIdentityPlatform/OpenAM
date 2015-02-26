package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.NamePattern;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.ViewApplicationDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import java.util.regex.Matcher;
import javax.faces.application.FacesMessage;

public class NameApplicationCreateWizardStepValidator extends ApplicationWizardStepValidator {
    public NameApplicationCreateWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        if (!validatePattern()) {
            return false;
        }
        if (!validateNotExists()) {
            return false;
        }

        return true;
    }
    private boolean validatePattern() {
        String appName = getApplicationWizardBean().getViewApplication().getName();
        // avoid NPE in matcher
        if (appName == null) {
            appName = "";
        }
        Matcher matcher = NamePattern.get().matcher(appName);

        if (matcher.matches()) {
            return true;
        }
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidNameSummary"));
        mb.setDetail(r.getString(this, "invalidNameDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        Effect e;

        e = new InputFieldErrorEffect();
        getApplicationWizardBean().setNameInputEffect(e);

        getMessagesBean().addMessageBean(mb);

        return false;
    }

    private boolean validateNotExists() {
        if (ViewApplicationDao.getInstance().exists(getApplicationWizardBean().getViewApplication())) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "existsSummary"));
            mb.setDetail(r.getString(this, "existsDetail", getApplicationWizardBean().getViewApplication().getName()));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);

            getMessagesBean().addMessageBean(mb);

            return false;
        }

        return true;

    }
}