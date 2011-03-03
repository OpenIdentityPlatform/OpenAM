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
 * $Id: ViewApplication.java,v 1.9 2010/01/13 18:41:54 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.dao.ViewApplicationDao;
import com.sun.identity.admin.handler.BooleanActionsHandler;
import com.sun.identity.entitlement.Application;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.model.SelectItem;
import javax.security.auth.Subject;
import org.apache.commons.collections.comparators.NullComparator;

public class ViewApplication implements Serializable {

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public static class NameComparator extends TableColumnComparator {
        public NameComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getName().compareTo(va2.getName());
            } else {
                return va2.getName().compareTo(va1.getName());
            }
        }
    }

    public static class DescriptionComparator extends TableColumnComparator {
        private static final NullComparator nullComparator = new NullComparator();

        public DescriptionComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return nullComparator.compare(va1.getDescription(), va2.getDescription());
            } else {
                return nullComparator.compare(va2.getDescription(), va1.getDescription());
            }
        }
    }

    public static class ApplicationTypeComparator extends TableColumnComparator {
        public ApplicationTypeComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getViewApplicationType().getName().compareTo(va2.getViewApplicationType().getName());
            } else {
                return va2.getViewApplicationType().getName().compareTo(va1.getViewApplicationType().getName());
            }
        }
    }

    public static class OverrideRuleComparator extends TableColumnComparator {
        public OverrideRuleComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getOverrideRule().compareTo(va2.getOverrideRule());
            } else {
                return va2.getOverrideRule().compareTo(va1.getOverrideRule());
            }
        }
    }

    public static class BirthComparator extends TableColumnComparator {

        public BirthComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getBirth().compareTo(va2.getBirth());
            } else {
                return va2.getBirth().compareTo(va1.getBirth());
            }
        }
    }

    public static class ModifiedComparator extends TableColumnComparator {

        public ModifiedComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getModified().compareTo(va2.getModified());
            } else {
                return va2.getModified().compareTo(va1.getModified());
            }
        }
    }

    public static class AuthorComparator extends TableColumnComparator {

        public AuthorComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getAuthor().compareTo(va2.getAuthor());
            } else {
                return va2.getAuthor().compareTo(va1.getAuthor());
            }
        }
    }

    public static class ModifierComparator extends TableColumnComparator {

        public ModifierComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            ViewApplication va1 = (ViewApplication) o1;
            ViewApplication va2 = (ViewApplication) o2;

            if (!isAscending()) {
                return va1.getModifier().compareTo(va2.getModifier());
            } else {
                return va2.getModifier().compareTo(va1.getModifier());
            }
        }
    }

    private String name;
    private String description;
    private ViewApplicationType viewApplicationType;
    private List<Resource> resources = new ArrayList<Resource>();
    private BooleanActionsBean booleanActionsBean = new BooleanActionsBean();
    private BooleanActionsHandler booleanActionsHandler = new BooleanActionsHandler();
    private List<ConditionType> conditionTypes = new ArrayList<ConditionType>();
    private List<SubjectType> subjectTypes = new ArrayList<SubjectType>();
    private OverrideRule overrideRule = OverrideRule.DENY_OVERRIDE;
    private boolean selected;
    private boolean writable = true;
    private Date birth;
    private Date modified;
    private String author;
    private String modifier;
    private boolean inUse = false;

    public ViewApplication() {
        booleanActionsHandler.setBooleanActionsBean(booleanActionsBean);
    }

    public ViewApplication(Application a) {
        this();
        
        ManagedBeanResolver mbr = new ManagedBeanResolver();

        name = a.getName();
        description = a.getDescription();


        // application type
        Map<String, ViewApplicationType> entitlementApplicationTypeToViewApplicationTypeMap = (Map<String, ViewApplicationType>) mbr.resolve("entitlementApplicationTypeToViewApplicationTypeMap");
        viewApplicationType = entitlementApplicationTypeToViewApplicationTypeMap.get(a.getApplicationType().getName());

        // birth, modified, author, modifier
        birth = new Date(a.getCreationDate());
        modified = new Date(a.getLastModifiedDate());
        author = a.getCreatedBy();
        modifier = a.getLastModifiedBy();

        // resources
        for (String resourceString : a.getResources()) {
            Resource r;
            try {
                r = (Resource) Class.forName(viewApplicationType.getResourceClassName()).newInstance();
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            } catch (InstantiationException ie) {
                throw new RuntimeException(ie);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            r.setName(resourceString);
            resources.add(r);
        }

        // actions
        for (String actionName : a.getActions().keySet()) {
            Boolean value = a.getActions().get(actionName);
            BooleanAction ba = new BooleanAction();
            ba.setName(actionName);
            ba.setAllow(value.booleanValue());
            booleanActionsBean.getActions().add(ba);
        }

        // conditions
        ConditionFactory ctf = (ConditionFactory) mbr.resolve("conditionFactory");
        Map<String,ConditionType> conditionTypeNameMap = ctf.getConditionTypeNameMap();
        for (String conditionTypeName : a.getConditions()) {
            ConditionType ct = conditionTypeNameMap.get(conditionTypeName);
            assert (ct != null);
            conditionTypes.add(ct);
        }

        // subjects
        SubjectFactory sf = (SubjectFactory) mbr.resolve("subjectFactory");
        for (String viewSubjectClassName : a.getSubjects()) {
            SubjectType st = sf.getSubjectType(viewSubjectClassName);
            assert (st != null);
            subjectTypes.add(st);
        }

        // override rule
        Class ecClass = a.getEntitlementCombinerClass();
        overrideRule = OverrideRule.valueOf(ecClass);
    }

    public List<SubjectContainer> getSubjectContainers() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        SubjectFactory sf = (SubjectFactory) mbr.resolve("subjectFactory");
        List<SubjectContainer> subjectContainers = new ArrayList<SubjectContainer>();
        for (SubjectType st : getSubjectTypes()) {
            SubjectContainer sc = sf.getSubjectContainer(st);
            if (sc != null && sc.isVisible()) {
                subjectContainers.add(sc);
            }
        }

        return subjectContainers;
    }

    public List<SubjectType> getExpressionSubjectTypes() {
        List<SubjectType> ests = new ArrayList<SubjectType>();
        for (SubjectType st: getSubjectTypes()) {
            if (st.isExpression()) {
                ests.add(st);
            }
        }
        return ests;
    }

    public List<ConditionType> getExpressionConditionTypes() {
        List<ConditionType> ects = new ArrayList<ConditionType>();
        for (ConditionType ct: conditionTypes) {
            if (ct.isExpression()) {
                ects.add(ct);
            }
        }
        return ects;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "title." + name);
        if (title == null) {
            title = name;
        }
        return title;
    }

    public ViewApplicationType getViewApplicationType() {
        return viewApplicationType;
    }

    public void setViewApplicationType(ViewApplicationType viewApplicationType) {
        this.viewApplicationType = viewApplicationType;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Application toApplication() {
        //
        // this is really just modifies the applications.
        //
        Subject adminSubject = new Token().getAdminSubject();
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();

        Application app = ViewApplicationDao.getInstance().newApplication(name, viewApplicationType);
        app.setDescription(description);

        // resources
        Set<String> resourceStrings = new HashSet<String>();
        for (Resource r : resources) {
            resourceStrings.add(r.getName());
        }
        app.addResources(resourceStrings);

        // actions
        Map<String,Boolean> actionMap = booleanActionsBean.getActionMap();
        app.setActions(actionMap);

        // conditions
        Set<String> conditions = new HashSet<String>();
        for (ConditionType ct: conditionTypes) {
            String ctName = ct.getName();
            conditions.add(ctName);
        }
        app.setConditions(conditions);

        // subjects
        Set<String> subjects = new HashSet<String>();
        for (SubjectType st: subjectTypes) {
            String viewClassName = st.newViewSubject().getClass().getName();
            subjects.add(viewClassName);
        }
        app.setSubjects(subjects);

        // override rule
        if (overrideRule != null) {
            Class ecClass = overrideRule.getEntitlementCombinerClass();
            app.setEntitlementCombiner(ecClass);
        }

        return app;
    }

    public List<ConditionType> getConditionTypes() {
        return conditionTypes;
    }

    public void setConditionTypes(List<ConditionType> conditionTypes) {
        this.conditionTypes = conditionTypes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SubjectType> getSubjectTypes() {
        return subjectTypes;
    }

    public BooleanActionsBean getBooleanActionsBean() {
        return booleanActionsBean;
    }

    public BooleanActionsHandler getBooleanActionsHandler() {
        return booleanActionsHandler;
    }

    public int getResourcesSize() {
        int size = 0;
        for (Resource r: resources) {
            if (r.getName() != null && r.getName().length() > 0) {
                size++;
            }
        }
        return size;
    }

    public OverrideRule getOverrideRule() {
        return overrideRule;
    }

    public void setOverrideRule(OverrideRule overrideRule) {
        this.overrideRule = overrideRule;
    }

    public List<SelectItem> getOverrideRuleNameItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (OverrideRule or: OverrideRule.values()) {
            SelectItem si = new SelectItem(or.toString(), or.getTitle());
            items.add(si);
        }

        return items;
    }

    public String getOverrideRuleName() {
        return overrideRule == null ? null : overrideRule.toString();
    }

    public void setOverrideRuleName(String name) {
        if (name != null) {
            overrideRule = OverrideRule.valueOf(name);
        }
    }

    public List<OverrideRule> getOverrideRuleValues() {
        return Arrays.asList(OverrideRule.values());
    }

    public String getResourcesToString() {
        return new ListFormatter(resources).toString();
    }

    public String getSubjectTypesToString() {
        return new ListFormatter(subjectTypes).toString();
    }

    public String getConditionTypesToString() {
        return new ListFormatter(conditionTypes).toString();
    }

    public String getResourcesToFormattedString() {
        return new ListFormatter(resources).toFormattedString();
    }

    public String getSubjectTypesToFormattedString() {
        return new ListFormatter(subjectTypes).toFormattedString();
    }

    public String getConditionTypesToFormattedString() {
        return new ListFormatter(conditionTypes).toFormattedString();
    }

    @Override
    public String toString() {
        return name;
    }
}
