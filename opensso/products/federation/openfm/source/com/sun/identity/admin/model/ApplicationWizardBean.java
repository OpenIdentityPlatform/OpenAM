/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ApplicationWizardBean.java,v 1.4 2009/12/11 23:45:51 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.ViewApplicationTypeDao;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;
import static com.sun.identity.admin.model.ApplicationWizardStep.*;

public abstract class ApplicationWizardBean extends WizardBean {

    private boolean nameEditable = false;
    private ViewApplication viewApplication;
    private Effect nameInputEffect;
    private Map<String, ViewApplicationType> viewApplicationTypeMap;
    private ApplicationSummary nameSummary = new NameApplicationSummary(this);
    private ApplicationSummary descriptionSummary = new DescriptionApplicationSummary(this);
    private ApplicationSummary resourcesSummary = new ResourcesApplicationSummary(this);
    private ApplicationSummary subjectsSummary = new SubjectsApplicationSummary(this);
    private ApplicationSummary actionsSummary = new ActionsApplicationSummary(this);
    private ApplicationSummary conditionsSummary = new ConditionsApplicationSummary(this);
    private ApplicationSummary overrideSummary = new OverrideApplicationSummary(this);
    private ApplicationSummary applicationTypeSummary = new ApplicationTypeApplicationSummary(this);

    protected abstract void resetViewApplication();

    @Override
    public void reset() {
        super.reset();
        reset(true, null);
    }

    private void reset(boolean resetName, ViewApplicationType vat) {
        viewApplicationTypeMap = ViewApplicationTypeDao.getInstance().getViewApplicationTypeMap();

        String name = null;
        String description = null;
        if (!resetName && viewApplication != null) {
            name = viewApplication.getName();
            description = viewApplication.getDescription();
        }

        resetViewApplication();

        if (name != null) {
            viewApplication.setName(name);
        }
        if (description != null) {
            viewApplication.setDescription(description);
        }
        if (viewApplication.getResources().size() == 0) {
            viewApplication.getResources().add(new UrlResource());
        }

        if (viewApplication.getViewApplicationType() == null) {
            if (vat == null) {
                viewApplication.setViewApplicationType(viewApplicationTypeMap.entrySet().iterator().next().getValue());
            } else {
                viewApplication.setViewApplicationType(vat);
            }
        }
    }

    public String getViewApplicationTypeName() {
        return getViewApplication().getViewApplicationType().getName();
    }

    public void setViewApplicationTypeName(String name) {
        if (!name.equals(viewApplication.getViewApplicationType().getName())) {
            ViewApplicationType vat = viewApplicationTypeMap.get(name);
            assert (vat != null);
            reset(false, vat);
            getViewApplication().getBooleanActionsBean().setActions(vat.getActions());
        }
    }

    public List<SelectItem> getViewApplicationTypeNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (Map.Entry<String, ViewApplicationType> entry : viewApplicationTypeMap.entrySet()) {
            if (!entry.getValue().isCreatable()) {
                continue;
            }
            SelectItem si = new SelectItem(entry.getValue().getName(), entry.getValue().getTitle());
            items.add(si);
        }

        return items;
    }

    public Effect getNameInputEffect() {
        return nameInputEffect;
    }

    public void setNameInputEffect(Effect nameInputEffect) {
        this.nameInputEffect = nameInputEffect;
    }

    public boolean isNameEditable() {
        return nameEditable;
    }

    public void setNameEditable(boolean nameEditable) {
        this.nameEditable = nameEditable;
    }

    private String getPanelLabel(ApplicationWizardStep aws) {
        Resources r = new Resources();
        String label;

        switch (aws) {
            case NAME:
                label = r.getString(this, "namePanelLabel");
                break;

            case RESOURCES:
                label = r.getString(this, "resourcesPanelLabel", getViewApplication().getResourcesSize());
                break;

            case SUBJECTS:
                label = r.getString(this, "subjectsPanelLabel", getViewApplication().getSubjectTypes().size());
                break;

            case ACTIONS:
                // TODO: count
                label = r.getString(this, "actionsPanelLabel", getViewApplication().getBooleanActionsBean().getActions().size());
                break;

            case CONDITIONS:
                // TODO: count
                label = r.getString(this, "conditionsPanelLabel", getViewApplication().getConditionTypes().size());
                break;

            case OVERRIDE:
                label = r.getString(this, "overridePanelLabel");
                break;

            case SUMMARY:
                label = r.getString(this, "summaryPanelLabel");
                break;

            default:
                throw new AssertionError("unhandled application wizard step: " + aws);
        }

        return label;
    }

    public String getNamePanelLabel() {
        return getPanelLabel(NAME);
    }

    public String getResourcesPanelLabel() {
        return getPanelLabel(RESOURCES);
    }

    public String getSubjectsPanelLabel() {
        return getPanelLabel(SUBJECTS);
    }

    public String getActionsPanelLabel() {
        return getPanelLabel(ACTIONS);
    }

    public String getConditionsPanelLabel() {
        return getPanelLabel(CONDITIONS);
    }

    public String getOverridePanelLabel() {
        return getPanelLabel(OVERRIDE);
    }

    public String getSummaryPanelLabel() {
        return getPanelLabel(SUMMARY);
    }

    public ViewApplication getViewApplication() {
        return viewApplication;
    }

    public List<String> getSubjectTypeNames() {
        List<String> names = new ArrayList<String>();
        for (SubjectType st : getViewApplication().getSubjectTypes()) {
            names.add(st.getName());
        }

        return names;
    }

    public List<String> getConditionTypeNames() {
        List<String> names = new ArrayList<String>();
        for (ConditionType st : getViewApplication().getConditionTypes()) {
            names.add(st.getName());
        }

        return names;
    }

    public void setSubjectTypeNames(List<String> subjectTypeNames) {
        Map<String, SubjectType> subjectTypeNameMap = SubjectFactory.getInstance().getSubjectTypeNameMap();
        getViewApplication().getSubjectTypes().clear();
        for (String name : subjectTypeNames) {
            SubjectType st = subjectTypeNameMap.get(name);
            assert (st != null);
            getViewApplication().getSubjectTypes().add(st);
        }
    }

    public void setConditionTypeNames(List<String> conditionTypeNames) {
        Map<String, ConditionType> conditionTypeNameMap = ConditionFactory.getInstance().getConditionTypeNameMap();
        getViewApplication().getConditionTypes().clear();
        for (String name : conditionTypeNames) {
            ConditionType ct = conditionTypeNameMap.get(name);
            assert (ct != null);
            getViewApplication().getConditionTypes().add(ct);
        }
    }

    public ApplicationSummary getNameSummary() {
        return nameSummary;
    }

    public ApplicationSummary getDescriptionSummary() {
        return descriptionSummary;
    }

    public ApplicationSummary getResourcesSummary() {
        return resourcesSummary;
    }

    public ApplicationSummary getActionsSummary() {
        return actionsSummary;
    }

    public ApplicationSummary getSubjectsSummary() {
        return subjectsSummary;
    }

    public ApplicationSummary getConditionsSummary() {
        return conditionsSummary;
    }

    public ApplicationSummary getOverrideSummary() {
        return overrideSummary;
    }

    public ApplicationSummary getApplicationTypeSummary() {
        return applicationTypeSummary;
    }

    public void setViewApplication(ViewApplication viewApplication) {
        this.viewApplication = viewApplication;
    }
}
