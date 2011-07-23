package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.faces.application.FacesMessage;

public class ResourcesApplicationWizardStepValidator extends ApplicationWizardStepValidator {
    public ResourcesApplicationWizardStepValidator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {
        List<Resource> resources =
                getApplicationWizardBean().getViewApplication().getResources();

        List<Resource> trimmed = new ArrayList<Resource>();
        for (Resource r: resources) {
            if (r.getName() == null || r.getName().trim().length() == 0) {
                trimmed.add(r);
            }
        }
        resources.removeAll(trimmed);

        if (resources == null || resources.size() == 0) {
            resources.add(new UrlResource("*"));

            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noResourcesSummary"));
            mb.setDetail(r.getString(this, "noResourcesDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_INFO);

            getMessagesBean().addMessageBean(mb);
        }

        Set<Resource> rs = new HashSet<Resource>(resources);
        if (rs.size() != resources.size()) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "duplicateResourceSummary"));
            mb.setDetail(r.getString(this, "duplicateResourceDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_INFO);

            getMessagesBean().addMessageBean(mb);
            
            resources.clear();
            resources.addAll(rs);
        }

        return true;
    }
}