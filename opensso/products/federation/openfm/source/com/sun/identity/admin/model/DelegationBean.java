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
 * $Id: DelegationBean.java,v 1.10 2009/12/16 18:16:32 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectImplementation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.comparators.NullComparator;

public class DelegationBean implements Serializable {

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<ViewSubject> getViewSubjects() {
        return viewSubjects;
    }

    public void setViewSubjects(List<ViewSubject> viewSubjects) {
        this.viewSubjects = viewSubjects;
    }

    public DelegationAction getAction() {
        return action;
    }

    public void setAction(DelegationAction action) {
        this.action = action;
    }

    public static class NameComparator extends TableColumnComparator {

        public NameComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean db1 = (DelegationBean) o1;
            DelegationBean db2 = (DelegationBean) o2;

            if (!isAscending()) {
                return db1.getName().compareTo(db2.getName());
            } else {
                return db2.getName().compareTo(db1.getName());
            }
        }
    }

    public static class DescriptionComparator extends TableColumnComparator {

        public DescriptionComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean db1 = (DelegationBean) o1;
            DelegationBean db2 = (DelegationBean) o2;

            if (!isAscending()) {
                return db1.getDescription().compareTo(db2.getDescription());
            } else {
                return db2.getDescription().compareTo(db1.getDescription());
            }
        }
    }

    public static class BirthComparator extends TableColumnComparator {

        public BirthComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean db1 = (DelegationBean) o1;
            DelegationBean db2 = (DelegationBean) o2;

            if (!isAscending()) {
                return NULL_COMPARATOR.compare(db1.getBirth(), db2.getBirth());
            } else {
                return NULL_COMPARATOR.compare(db2.getBirth(), db1.getBirth());
            }
        }
    }

    public static class ModifiedComparator extends TableColumnComparator {

        public ModifiedComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean db1 = (DelegationBean) o1;
            DelegationBean db2 = (DelegationBean) o2;

            if (!isAscending()) {
                return NULL_COMPARATOR.compare(db1.getModified(), db2.getModified());
            } else {
                return NULL_COMPARATOR.compare(db2.getModified(), db1.getModified());
            }
        }
    }

    public static class AuthorComparator extends TableColumnComparator {

        public AuthorComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean db1 = (DelegationBean) o1;
            DelegationBean db2 = (DelegationBean) o2;

            if (!isAscending()) {
                return NULL_COMPARATOR.compare(db1.getAuthor(), db2.getAuthor());
            } else {
                return NULL_COMPARATOR.compare(db2.getAuthor(), db1.getAuthor());
            }
        }
    }

    public static class ModifierComparator extends TableColumnComparator {

        public ModifierComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean db1 = (DelegationBean) o1;
            DelegationBean db2 = (DelegationBean) o2;

            if (!isAscending()) {
                return NULL_COMPARATOR.compare(db1.getModifier(), db2.getModifier());
            } else {
                return NULL_COMPARATOR.compare(db2.getModifier(), db1.getModifier());
            }
        }
    }
    private String name;
    private String description;
    private Date birth;
    private Date modified;
    private String author;
    private String modifier;
    private boolean selected;
    private List<Resource> resources;
    private List<ViewSubject> viewSubjects = new ArrayList<ViewSubject>();
    private DelegationAction action = DelegationAction.READ;

    public DelegationBean() {
        // nothing
    }

    public DelegationBean(ApplicationPrivilege ap) {

        // name
        name = ap.getName();

        // description
        description = ap.getDescription();

        // birth
        birth = new Date(ap.getCreationDate());

        // author
        author = ap.getCreatedBy();

        // modified
        modified = new Date(ap.getLastModifiedDate());

        // modifier
        modifier = ap.getLastModifiedBy();

        // applications, resources
        resources = new ArrayList<Resource>();
        for (String applicationName : ap.getApplicationNames()) {
            ApplicationResource ar = new ApplicationResource();
            ar.setName(applicationName);

            String resourceClassName = ar.getViewEntitlement().getViewApplication().getViewApplicationType().getResourceClassName();
            Class resourceClass;
            try {
                resourceClass = Class.forName(resourceClassName);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }

            ManagedBeanResolver mbr = new ManagedBeanResolver();
            Map<String, ResourceDecorator> resourceDecorators = (Map<String, ResourceDecorator>) mbr.resolve("resourceDecorators");
            Set<String> resourceNames = ap.getResourceNames(applicationName);
            for (String resourceName : resourceNames) {
                try {
                    Resource r = (Resource) resourceClass.newInstance();
                    r.setName(resourceName);

                    // decorate resource (optionally)
                    ResourceDecorator rd = resourceDecorators.get(r.getClass().getName());
                    if (rd != null) {
                        rd.decorate(r);
                    }

                    ar.getViewEntitlement().getResources().add(r);

                    // if AP contains a resource for the given app that is
                    // *not* in the application itself, add that resource
                    // to the set of available resources
                    if (!ar.getViewEntitlement().getAvailableResources().contains(r)) {
                        ar.getViewEntitlement().getAvailableResources().add(r);
                    }
                } catch (InstantiationException ie) {
                    throw new RuntimeException(ie);
                } catch (IllegalAccessException iae) {
                    throw new RuntimeException(iae);
                }
            }

            resources.add(ar);
        }

        // subjects
        for (SubjectImplementation si : ap.getSubjects()) {
            viewSubjects.add(SubjectFactory.getInstance().getViewSubject(si));
        }

        // action
        action = DelegationAction.valueOf(ap.getActionValues().toString());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ApplicationPrivilege toApplicationPrivilege() {
        ApplicationPrivilege ap = new ApplicationPrivilege(name);

        // description
        ap.setDescription(description);

        // applications, resources
        for (Resource r : resources) {
            ApplicationResource ar = (ApplicationResource) r;
            String an = ar.getViewEntitlement().getViewApplication().getName();
            Set<String> rrs = new HashSet<String>();
            for (Resource rr : ar.getViewEntitlement().getResources()) {
                rrs.add(rr.getName());
            }
            ap.addApplicationResource(an, rrs);
        }

        // subjects
        Set<SubjectImplementation> eSubjects = new HashSet<SubjectImplementation>();
        for (ViewSubject vs : viewSubjects) {
            eSubjects.add((SubjectImplementation) vs.getEntitlementSubject());
        }
        try {
            ap.setSubject(eSubjects);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }

        // action
        ap.setActionValues(ApplicationPrivilege.PossibleAction.valueOf(action.toString()));

        return ap;
    }

    public String getResourcesToString() {
        return new ListFormatter(resources, true).toString();
    }

    public String getResourcesToFormattedString() {
        StringBuffer b = new StringBuffer();
        for (Iterator<Resource> i = resources.iterator(); i.hasNext();) {
            ApplicationResource rr = (ApplicationResource) i.next();
            List<Resource> rs = rr.getViewEntitlement().getResources();

            b.append(rr.getTitle());
            b.append("\n");
            if (rs != null) {
                for (Resource r : rs) {
                    b.append("    ");
                    b.append(r.getTitle());
                    b.append("\n");
                }
            }
        }

        return b.toString();
    }

    public String getViewSubjectsToString() {
        return new ListFormatter(viewSubjects).toString();
    }

    public String getViewSubjectsToFormattedString() {
        ListFormatter lf = new ListFormatter(viewSubjects);
        return lf.toFormattedString();
    }

    public Date getBirth() {
        return birth;
    }

    public Date getModified() {
        return modified;
    }

    public String getAuthor() {
        return author;
    }

    public String getModifier() {
        return modifier;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
