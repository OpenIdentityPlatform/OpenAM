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
 * $Id: DelegationWizardBean.java,v 1.12 2009/12/16 18:16:32 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Functions;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.DelegationDao;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilege.PossibleAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;
import static com.sun.identity.admin.model.DelegationWizardStep.*;

public abstract class DelegationWizardBean extends WizardBean {

    private boolean nameEditable = false;
    private Effect nameInputEffect;
    private DelegationBean delegationBean = new DelegationBean();
    private List<Resource> availableResources;
    private ViewApplicationsBean viewApplicationsBean;
    private SubjectType subjectType;
    private List<SubjectType> subjectTypes;
    private Map<SubjectType, SubjectContainer> subjectTypeToSubjectContainerMap;
    private List<ViewSubject> selectedAvailableViewSubjects;
    private List<ViewSubject> selectedSelectedViewSubjects;
    private DelegationSummary nameSummary = new NameDelegationSummary(this);
    private DelegationSummary resourcesSummary = new ResourcesDelegationSummary(this);
    private DelegationSummary subjectsSummary = new SubjectsDelegationSummary(this);
    private DelegationSummary actionSummary = new ActionDelegationSummary(this);

    protected abstract void resetDelegationBean();

    @Override
    public void reset() {
        super.reset();
        reset(true, null);
    }

    private void reset(boolean resetName, ViewApplicationType vat) {
        resetDelegationBean();
        resetAvailableResources();
        resetSubjectType();
    }

    private void resetSubjectType() {
        subjectTypes = DelegationDao.getInstance().getSubjectTypes();
        if (subjectTypes.size() > 0) {
            subjectType = subjectTypes.get(0);
        }
    }

    public List<DelegationAction> getActions() {
        return Arrays.asList(DelegationAction.values());
    }

    public List<SelectItem> getActionItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (DelegationAction da : DelegationAction.values()) {
            items.add(new SelectItem(da, da.getTitle()));
        }
        return items;
    }

    public List<ViewSubject> getAvailableViewSubjects() {
        SubjectContainer sc = subjectTypeToSubjectContainerMap.get(subjectType);
        if (sc == null) {
            return Collections.EMPTY_LIST;
        }
        List<ViewSubject> vss = new ArrayList<ViewSubject>(sc.getViewSubjects());
        if (delegationBean.getViewSubjects() != null) {
            for (ViewSubject vs : delegationBean.getViewSubjects()) {
                vss.remove(vs);
            }
        }

        return vss;
    }

    public List<SelectItem> getAvailableViewSubjectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (ViewSubject vs : getAvailableViewSubjects()) {
            items.add(new SelectItem(vs, vs.getTitle()));
        }
        return items;
    }

    public List<ViewSubject> getSelectedViewSubjects() {
        return delegationBean.getViewSubjects();
    }

    public List<SelectItem> getSelectedViewSubjectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (delegationBean.getViewSubjects() != null) {
            for (ViewSubject vs : delegationBean.getViewSubjects()) {
                items.add(new SelectItem(vs, vs.getTitle()));
            }
        }
        return items;
    }

    public String getSubjectTypeName() {
        return subjectType.getName();
    }

    public void setSubjectTypeName(String name) {
        for (SubjectType st : subjectTypes) {
            if (st.getName().equals(name)) {
                subjectType = st;
                break;
            }
        }
    }

    public List<SelectItem> getSubjectTypeNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (SubjectType st : subjectTypes) {
            items.add(new SelectItem(st.getName(), st.getTitle()));
        }
        return items;
    }

    public List<Resource> getResources() {
        return delegationBean.getResources();
    }

    public void setResources(List<Resource> resources) {
        delegationBean.setResources(new ArrayList<Resource>());
        for (Resource r : resources) {
            int i = getAvailableResources().indexOf(r);
            assert (i != -1);
            r = getAvailableResources().get(i);
            delegationBean.getResources().add(r);
        }
    }

    public List<SelectItem> getAvailableResourceItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (availableResources != null) {
            for (Resource r : availableResources) {
                ApplicationResource ar = (ApplicationResource) r;
                items.add(new SelectItem(ar, ar.getTitle()));
            }
        }

        return items;
    }

    private void resetAvailableResources() {
        availableResources = new ArrayList<Resource>();
        for (ViewApplication va : viewApplicationsBean.getViewApplications().values()) {
            ApplicationResource ar = new ApplicationResource();
            ar.setName(va.getName());

            List<Resource> resources = delegationBean.getResources();
            int i = -1;
            if (resources != null) {
                i = delegationBean.getResources().indexOf(ar);
            }

            if (i == -1) {
                // new delegation
                // set all resources selected
                ar.getViewEntitlement().setResources(ar.getViewEntitlement().getAvailableResources());
            } else {
                // existing delegation
                // set selected resources according to delegation bean settings
                ApplicationResource selectedAr = (ApplicationResource) delegationBean.getResources().get(i);
                List<Resource> selectedResources = selectedAr.getViewEntitlement().getResources();
                ar.getViewEntitlement().setResources(selectedResources);
                for (Resource r: selectedResources) {
                    if (!ar.getViewEntitlement().getAvailableResources().contains(r)) {
                        ar.getViewEntitlement().getAvailableResources().add(r);
                    }
                }
            }
            availableResources.add(ar);
        }
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

    public int getResourcesSize() {
        int size = 0;
        if (delegationBean.getResources() != null) {
            for (Resource r : delegationBean.getResources()) {
                ApplicationResource arr = (ApplicationResource) r;
                size += arr.getResourceCount();
            }
        }
        return size;
    }

    private String getPanelLabel(DelegationWizardStep aws) {
        Resources r = new Resources();
        String label;

        // TODO: add counts to labal strings

        switch (aws) {
            case NAME:
                label = r.getString(this, "namePanelLabel");
                break;

            case RESOURCES:
                int applicationCount = Functions.size(delegationBean.getResources());
                int resourceCount = getResourcesSize();
                label = r.getString(this, "resourcesPanelLabel", applicationCount, resourceCount);
                break;

            case SUBJECTS:
                int subjectCount = Functions.size(delegationBean.getViewSubjects());
                label = r.getString(this, "subjectsPanelLabel", subjectCount);
                break;

            case ACTION:
                label = r.getString(this, "actionPanelLabel");
                break;

            case SUMMARY:
                label = r.getString(this, "summaryPanelLabel");
                break;

            default:
                throw new AssertionError("unhandled delegation wizard step: " + aws);
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
        return getPanelLabel(ACTION);
    }

    public String getSummaryPanelLabel() {
        return getPanelLabel(SUMMARY);
    }

    public DelegationBean getDelegationBean() {
        return delegationBean;
    }

    public void setDelegationBean(DelegationBean delegationBean) {
        this.delegationBean = delegationBean;
    }

    public List<Resource> getAvailableResources() {
        return availableResources;
    }

    public void setViewApplicationsBean(ViewApplicationsBean viewApplicationsBean) {
        this.viewApplicationsBean = viewApplicationsBean;
    }

    public void setSubjectTypeToSubjectContainerMap(Map<SubjectType, SubjectContainer> subjectTypeToSubjectContainerMap) {
        this.subjectTypeToSubjectContainerMap = subjectTypeToSubjectContainerMap;
    }

    public List<ViewSubject> getSelectedAvailableViewSubjects() {
        return selectedAvailableViewSubjects;
    }

    public void setSelectedAvailableViewSubjects(List<ViewSubject> selectedAvailableViewSubjects) {
        this.selectedAvailableViewSubjects = selectedAvailableViewSubjects;
    }

    public List<ViewSubject> getSelectedSelectedViewSubjects() {
        return selectedSelectedViewSubjects;
    }

    public void setSelectedSelectedViewSubjects(List<ViewSubject> selectedSelectedViewSubjects) {
        this.selectedSelectedViewSubjects = selectedSelectedViewSubjects;
    }

    public String getViewSubjectFilter() {
        SubjectContainer sc = subjectTypeToSubjectContainerMap.get(subjectType);
        return sc.getFilter();
    }

    public void setViewSubjectFilter(String viewSubjectFilter) {
        SubjectContainer sc = subjectTypeToSubjectContainerMap.get(subjectType);
        sc.setFilter(viewSubjectFilter);
    }

    public DelegationSummary getNameSummary() {
        return nameSummary;
    }

    public DelegationSummary getResourcesSummary() {
        return resourcesSummary;
    }

    public DelegationSummary getSubjectsSummary() {
        return subjectsSummary;
    }

    public DelegationSummary getActionSummary() {
        return actionSummary;
    }
}
