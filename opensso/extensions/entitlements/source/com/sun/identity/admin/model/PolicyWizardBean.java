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
 * $Id: PolicyWizardBean.java,v 1.22 2009/08/05 14:37:15 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Functions;
import com.sun.identity.admin.Resources;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;
import static com.sun.identity.admin.model.PolicyWizardStep.*;

public abstract class PolicyWizardBean
        extends WizardBean
        implements Serializable, PolicyNameBean, PolicyResourcesBean,
        PolicySubjectsBean, PolicyConditionsBean, PolicySummaryBean {

    private PrivilegeBean privilegeBean;
    private ViewApplicationsBean viewApplicationsBean;
    private Effect dropConditionEffect;
    private Effect dropSubjectContainerEffect;
    private Effect policyNameInputEffect;
    private Effect policyNameMessageEffect;
    private int advancedTabsetIndex = 0;
    private boolean policyNameEditable = false;
    private List<ConditionType> conditionTypes;
    private List<SubjectType> subjectTypes;
    private PolicySummary namePolicySummary = new NamePolicySummary(this);
    private PolicySummary descriptionPolicySummary = new DescriptionPolicySummary(this);
    private PolicySummary applicationPolicySummary = new ApplicationPolicySummary(this);
    private PolicySummary resourcesPolicySummary = new ResourcesPolicySummary(this);
    private PolicySummary subjectsPolicySummary = new SubjectsPolicySummary(this);
    private PolicySummary conditionsPolicySummary = new ConditionsPolicySummary(this);
    private PolicySummary actionsPolicySummary = new ActionsPolicySummary(this);
    private PolicySummary staticAttributesPolicySummary = new StaticAttributesPolicySummary(this);
    private PolicySummary userAttributesPolicySummary = new UserAttributesPolicySummary(this);

    public PolicyWizardBean() {
        super();
    }

    public ViewEntitlement getViewEntitlement() {
        return privilegeBean.getViewEntitlement();
    }

    protected abstract void resetPrivilegeBean();

    @Override
    public void reset() {
        super.reset();
        
        resetPrivilegeBean();

        advancedTabsetIndex = 0;

        namePolicySummary = new NamePolicySummary(this);
        descriptionPolicySummary = new DescriptionPolicySummary(this);
        applicationPolicySummary = new ApplicationPolicySummary(this);
        resourcesPolicySummary = new ResourcesPolicySummary(this);
        subjectsPolicySummary = new SubjectsPolicySummary(this);
        conditionsPolicySummary = new ConditionsPolicySummary(this);
        actionsPolicySummary = new ActionsPolicySummary(this);
        staticAttributesPolicySummary = new StaticAttributesPolicySummary(this);
        userAttributesPolicySummary = new UserAttributesPolicySummary(this);
    }

    public List<SelectItem> getViewApplicationNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (ViewApplication va : getViewApplicationsBean().getViewApplications().values()) {
            items.add(new SelectItem(va.getName(), va.getTitle()));
        }

        return items;
    }

    public Effect getDropConditionEffect() {
        return dropConditionEffect;
    }

    public void setDropConditionEffect(Effect dropConditionEffect) {
        this.dropConditionEffect = dropConditionEffect;
    }

    public Effect getDropSubjectContainerEffect() {
        return dropSubjectContainerEffect;
    }

    public void setDropSubjectContainerEffect(Effect dropSubjectContainerEffect) {
        this.dropSubjectContainerEffect = dropSubjectContainerEffect;
    }

    public int getAdvancedTabsetIndex() {
        return advancedTabsetIndex;
    }

    public void setAdvancedTabsetIndex(int advancedTabsetIndex) {
        this.advancedTabsetIndex = advancedTabsetIndex;
    }

    public PrivilegeBean getPrivilegeBean() {
        return privilegeBean;
    }

    public Effect getPolicyNameMessageEffect() {
        return policyNameMessageEffect;
    }

    public void setPolicyNameMessageEffect(Effect policyNameMessageEffect) {
        this.policyNameMessageEffect = policyNameMessageEffect;
    }

    public Effect getPolicyNameInputEffect() {
        return policyNameInputEffect;
    }

    public void setPolicyNameInputEffect(Effect policyNameInputEffect) {
        this.policyNameInputEffect = policyNameInputEffect;
    }

    public ViewApplication getViewApplication() {
        return getPrivilegeBean().getViewEntitlement().getViewApplication();
    }

    public void setViewApplicationName(String viewApplicationName) {
        ViewApplication va = viewApplicationsBean.getViewApplications().get(viewApplicationName);
        getPrivilegeBean().getViewEntitlement().setViewApplication(va);
        getPrivilegeBean().getViewEntitlement().getBooleanActionsBean().setActions(va);
    }

    private void resetViewApplication() {
        Map<String, ViewApplication> viewApplicationMap = viewApplicationsBean.getViewApplications();
        Collection<ViewApplication> viewApplications = (Collection<ViewApplication>) viewApplicationMap.values();
        if (viewApplications != null && viewApplications.size() > 0) {
            setViewApplicationName(viewApplications.iterator().next().getName());
        }
    }

    public void setViewApplicationsBean(ViewApplicationsBean viewApplicationsBean) {
        this.viewApplicationsBean = viewApplicationsBean;
    }

    public ViewApplicationsBean getViewApplicationsBean() {
        return viewApplicationsBean;
    }

    public void setPrivilegeBean(PrivilegeBean privilegeBean) {
        this.privilegeBean = privilegeBean;
    }

    public String getViewApplicationName() {
        if (getPrivilegeBean().getViewEntitlement().getViewApplication() == null) {
            resetViewApplication();
        }
        return getPrivilegeBean().getViewEntitlement().getViewApplication().getName();
    }

    public boolean isPolicyNameEditable() {
        return policyNameEditable;
    }

    public void setPolicyNameEditable(boolean policyNameEditable) {
        this.policyNameEditable = policyNameEditable;
    }

    public void setSubjectTypes(List<SubjectType> subjectTypes) {
        this.subjectTypes = subjectTypes;
    }

    public SubjectType getSubjectType(String name) {
        for (SubjectType st : subjectTypes) {
            if (st.getName().equals(name)) {
                return st;
            }
        }
        return null;
    }

    public void setConditionTypes(List<ConditionType> conditionTypes) {
        this.conditionTypes = conditionTypes;
    }

    public ConditionType getConditionType(String name) {
        for (ConditionType ct : conditionTypes) {
            if (ct.getName().equals(name)) {
                return ct;
            }
        }
        return null;
    }

    public PolicySummary getNamePolicySummary() {
        return namePolicySummary;
    }

    public PolicySummary getDescriptionPolicySummary() {
        return descriptionPolicySummary;
    }

    public PolicySummary getApplicationPolicySummary() {
        return applicationPolicySummary;
    }

    public PolicySummary getResourcesPolicySummary() {
        return resourcesPolicySummary;
    }

    public PolicySummary getSubjectsPolicySummary() {
        return subjectsPolicySummary;
    }

    public PolicySummary getConditionsPolicySummary() {
        return conditionsPolicySummary;
    }

    public PolicySummary getActionsPolicySummary() {
        return actionsPolicySummary;
    }

    public PolicySummary getStaticAttributesPolicySummary() {
        return staticAttributesPolicySummary;
    }

    public PolicySummary getUserAttributesPolicySummary() {
        return userAttributesPolicySummary;
    }

    private String getAdvancedTabPanelLabel(PolicyWizardAdvancedTabIndex i) {
        Resources r = new Resources();
        String val;

        switch (i) {
            case ACTIONS:
                val = actionsPolicySummary.getValue();
                break;
            case CONDITIONS:
                val = conditionsPolicySummary.getValue();
                break;
            case RESOURCE_ATTRIBUTES:
                val = staticAttributesPolicySummary.getValue();
                break;
            case USER_ATTRIBUTES:
                val = userAttributesPolicySummary.getValue();
                break;
            default:
                throw new AssertionError("unhandled tab index: " + i);
        }

        String label = r.getString(this, i + ".tabPanelLabel", val);
        return label;
    }

    public String getActionsTabPanelLabel() {
        return getAdvancedTabPanelLabel(PolicyWizardAdvancedTabIndex.ACTIONS);
    }

    public String getConditionsTabPanelLabel() {
        return getAdvancedTabPanelLabel(PolicyWizardAdvancedTabIndex.CONDITIONS);
    }

    public String getResourceAttributesTabPanelLabel() {
        return getAdvancedTabPanelLabel(PolicyWizardAdvancedTabIndex.RESOURCE_ATTRIBUTES);
    }

    public String getUserAttributesTabPanelLabel() {
        return getAdvancedTabPanelLabel(PolicyWizardAdvancedTabIndex.USER_ATTRIBUTES);
    }

    private String getPanelLabel(PolicyWizardStep pws) {
        Resources r = new Resources();
        String label;

        switch (pws) {
            case NAME:
                label = r.getString(this, "namePanelLabel");

                break;

            case RESOURCES:
                int resourceCount = Functions.size(privilegeBean.getViewEntitlement().getResources());
                label = r.getString(this, "resourcesPanelLabel", resourceCount);

                break;

            case SUBJECTS:
                int subjectCount = new Tree(privilegeBean.getViewSubject()).sizeLeafs();
                label = r.getString(this, "subjectsPanelLabel", subjectCount);

                break;

            case ADVANCED:
                label = r.getString(this, "advancedPanelLabel");

                break;

            case SUMMARY:
                label = r.getString(this, "summaryPanelLabel");

                break;
            default:
                throw new AssertionError("unhandled policy wizard step: " + pws);
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

    public String getAdvancedPanelLabel() {
        return getPanelLabel(ADVANCED);
    }

    public String getSummaryPanelLabel() {
        return getPanelLabel(SUMMARY);
    }
}
