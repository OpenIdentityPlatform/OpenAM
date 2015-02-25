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
 * $Id: PrivilegeBean.java,v 1.37 2009/08/09 06:04:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.model.AttributesBean.AttributeType;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrivilegeBean implements Serializable {

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public List<AttributesBean> getAttributesBeans() {
        return attributesBeans;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public static class NameComparator extends TableColumnComparator {

        public NameComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            if (!isAscending()) {
                return pb1.getName().compareTo(pb2.getName());
            } else {
                return pb2.getName().compareTo(pb1.getName());
            }
        }
    }

    public static class DescriptionComparator extends TableColumnComparator {

        public DescriptionComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            String d1 = (pb1.getDescription() == null) ? "" : pb1.getDescription();
            String d2 = (pb2.getDescription() == null) ? "" : pb2.getDescription();

            if (!isAscending()) {
                return d1.compareTo(d2);
            } else {
                return d2.compareTo(d1);
            }
        }
    }

    public static class ApplicationComparator extends TableColumnComparator {

        public ApplicationComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            if (!isAscending()) {
                return pb1.getViewEntitlement().getViewApplication().getName().compareTo(pb2.getViewEntitlement().getViewApplication().getName());
            } else {
                return pb2.getViewEntitlement().getViewApplication().getName().compareTo(pb1.getViewEntitlement().getViewApplication().getName());
            }
        }
    }

    public static class BirthComparator extends TableColumnComparator {

        public BirthComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            if (!isAscending()) {
                return pb1.getBirth().compareTo(pb2.getBirth());
            } else {
                return pb2.getBirth().compareTo(pb1.getBirth());
            }
        }
    }

    public static class ModifiedComparator extends TableColumnComparator {

        public ModifiedComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            if (!isAscending()) {
                return pb1.getModified().compareTo(pb2.getModified());
            } else {
                return pb2.getModified().compareTo(pb1.getModified());
            }
        }
    }

    public static class AuthorComparator extends TableColumnComparator {

        public AuthorComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            if (!isAscending()) {
                return pb1.getAuthor().compareTo(pb2.getAuthor());
            } else {
                return pb2.getAuthor().compareTo(pb1.getAuthor());
            }
        }
    }

    public static class ModifierComparator extends TableColumnComparator {

        public ModifierComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            PrivilegeBean pb1 = (PrivilegeBean) o1;
            PrivilegeBean pb2 = (PrivilegeBean) o2;

            if (!isAscending()) {
                return pb1.getModifier().compareTo(pb2.getModifier());
            } else {
                return pb2.getModifier().compareTo(pb1.getModifier());
            }
        }
    }


    private String name = null;
    private String description = null;
    private ViewEntitlement viewEntitlement = new ViewEntitlement();
    private ViewCondition viewCondition = null;
    private ViewSubject viewSubject = null;
    private List<AttributesBean> attributesBeans = new ArrayList<AttributesBean>();
    private Date birth;
    private Date modified;
    private String author;
    private String modifier;
    private boolean selected;

    public PrivilegeBean() {
        AttributesBean ab;
        ab = new StaticAttributesBean();
        attributesBeans.add(ab);
        ab = new UserAttributesBean();
        attributesBeans.add(ab);
    }

    public PrivilegeBean(
                Privilege p,
                Map<String,ViewApplication> viewApplications,
                SubjectFactory subjectFactory) {

        name = p.getName();
        description = p.getDescription();

        // entitlement
        viewEntitlement = new ViewEntitlement(p.getEntitlement(), viewApplications);

        // subjects
        viewSubject = subjectFactory.getViewSubject(p.getSubject());

        // conditions
        viewCondition = ConditionFactory.getInstance().getViewCondition(p.getCondition());

        // attributes
        AttributesBean ab;
        ab = new StaticAttributesBean(p.getResourceAttributes());
        attributesBeans.add(ab);
        ab = new UserAttributesBean(p.getResourceAttributes());
        attributesBeans.add(ab);

        // created, modified
        birth = new Date(p.getCreationDate());
        author = p.getCreatedBy();
        modified = new Date(p.getLastModifiedDate());
        modifier = p.getLastModifiedBy();
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

    public Privilege toPrivilege() {
        // subjects
        EntitlementSubject eSubject = null;
        if (viewSubject != null) {
            eSubject = viewSubject.getEntitlementSubject();
        }

        // resources / actions
        Entitlement entitlement = viewEntitlement.getEntitlement();

        // conditions
        EntitlementCondition condition = null;
        if (getViewCondition() != null) {
            condition = getViewCondition().getEntitlementCondition();
        }

        // attrs
        Set<ResourceAttribute> attributes = new HashSet<ResourceAttribute>();
        for (AttributesBean ab: attributesBeans) {
            Set<ResourceAttribute> attrs = ab.toResourceAttributesSet();
            attributes.addAll(attrs);
        }

        try {
            Privilege p = Privilege.getNewInstance();
            p.setName(name);
            p.setEntitlement(entitlement);
            p.setSubject(eSubject);
            p.setCondition(condition);
            p.setResourceAttributes(attributes);
            p.setDescription(description);
            return p;
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    public ViewCondition getViewCondition() {
        return viewCondition;
    }

    public void setViewCondition(ViewCondition viewCondition) {
        this.viewCondition = viewCondition;
    }

    public ViewSubject getViewSubject() {
        return viewSubject;
    }

    public void setViewSubject(ViewSubject viewSubject) {
        this.viewSubject = viewSubject;
    }

    public ViewEntitlement getViewEntitlement() {
        return viewEntitlement;
    }

    public String getViewSubjectToString() {
        if (viewSubject == null) {
            return "";
        }
        return viewSubject.toString();
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PrivilegeBean)) {
            return false;
        }
        PrivilegeBean other = (PrivilegeBean)o;

        return other.getName().equals(name);
    }

    public AttributesBean getStaticAttributesBean() {
        return getAttributesBean(AttributeType.STATIC);
    }

    public AttributesBean getUserAttributesBean() {
        return getAttributesBean(AttributeType.USER);
    }

    private AttributesBean getAttributesBean(AttributeType attributeType) {
        for (AttributesBean ab: attributesBeans) {
            if (ab.getAttributeType().equals(attributeType)) {
                return ab;
            }
        }
        return null;
    }
}
