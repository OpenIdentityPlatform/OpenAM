/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SearchTemplate.java,v 1.3 2008/06/25 05:41:46 qcheng Exp $
 *
 */

package com.iplanet.ums;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;

/**
 * Represents templates for searching functionality. SearchTemplate serves the
 * purpose of defining guidelines in a search. It defines the search filter and
 * attributes to be returned in a search query. Reusability and flexibility are
 * serving goals in SearchTemplate.
 * <P>
 * 
 * @see Template
 * @see CreationTemplate
 * @supported.api
 */
public class SearchTemplate extends Template {
    /**
     * Default constructor for deserialization
     * 
     * @supported.api
     */
    public SearchTemplate() {
        super();
    }

    /**
     * Creates a template with an attribute set and a search filter. The
     * Attribute set contains attributes to be returned on a search. If the
     * search filter is null, then "objectclass=*" is assumed (return all
     * objects).
     * 
     * @param name
     *            Template name
     * @param attrSet
     *            set of attributes
     * @param filter
     *            search filter
     */
    public SearchTemplate(String name, AttrSet attrSet, String filter) {
        super(name);
        setAttributeSet(attrSet);
        setSearchFilter(filter);
    }

    /**
     * Creates a template with an array of attributes and a search filter. The
     * array of attributes contains attributes to be returned on a search. If
     * the search filter is null, then "objectclass=*" is assumed (return all
     * objects).
     * 
     * @param name
     *            Template name
     * @param attributeNames
     *            an array of attribute names
     * @param filter
     *            search filter
     * 
     * @supported.api
     */
    public SearchTemplate(String name, String[] attributeNames, String filter) {
        super(name);
        setAttributeNames(attributeNames);
        setSearchFilter(filter);
    }

    /**
     * Sets the filter expression used to search for objects of this type, for
     * example, "objectclass=inetorgperson" or
     * "(&(objectclass=inetorgperson)(ou=accounting))"
     * 
     * @param filter
     *            A UMS search expression (LDAP syntax)
     * 
     * @supported.api
     */
    public void setSearchFilter(String filter) {
        m_searchFilter = (filter != null) ? filter : "objectclass=*";
    }

    /**
     * Gets the filter expression used to search for objects of this type.
     * 
     * @return a UMS search expression (LDAP syntax)
     * 
     * @supported.api
     */
    public String getSearchFilter() {
        return m_searchFilter;
    }

    /**
     * Sets the attributes to be returned on a search.
     * 
     * @param attributeNames
     *            The attribute names to be returned
     * 
     * @supported.api
     */
    public void setAttributeNames(String[] attributeNames) {
        if (attributeNames != null) {
            m_attrSet = new AttrSet();
            addAttributeNames(attributeNames);
        }
    }

    /**
     * Adds the attribute name to the list of attributes to be returned on a
     * search.
     * 
     * @param attributeName
     *            The attribute name to be added
     * 
     * @supported.api
     */
    public void addAttributeName(String attributeName) {
        if (attributeName != null) {
            if (m_attrSet == null) {
                m_attrSet = new AttrSet();
            }
            m_attrSet.add(new Attr(attributeName));
        }
    }

    /**
     * Adds the attribute names to the list of attributes to be returned on a
     * search.
     * 
     * @param attributeNames
     *            The attribute names to be added
     * 
     * @supported.api
     */
    public void addAttributeNames(String[] attributeNames) {
        if (attributeNames != null) {
            for (int i = 0; i < attributeNames.length; i++) {
                addAttributeName(attributeNames[i]);
            }
        }
    }

    /**
     * Removes the attribute name from the list of attributes to be returned on
     * a search.
     * 
     * @param attributeName
     *            The attribute name to be removed
     * 
     * @supported.api
     */
    public void removeAttributeName(String attributeName) {
        if (attributeName != null && m_attrSet != null) {
            m_attrSet.remove(attributeName);
        }
    }

    /**
     * Removes the attribute names from the list of attributes to be returned on
     * a search.
     * 
     * @param attributeNames
     *            The attribute names to be removed
     * 
     * @supported.api
     */
    public void removeAttributeNames(String[] attributeNames) {
        if (attributeNames != null && m_attrSet != null) {
            for (int i = 0; i < attributeNames.length; i++) {
                removeAttributeName(attributeNames[i]);
            }
        }
    }

    /**
     * Gets a list of attribute names defined in the object.
     * 
     * @return Names of all attributes defined
     * 
     * @supported.api
     */
    public String[] getAttributeNames() {
        return (m_attrSet == null) ? new String[0] : m_attrSet
                .getAttributeNames();
    }

    /**
     * Gets the attributes to be returned on a search.
     * 
     * @return set of attributes to be returned on a search
     */
    AttrSet getAttributeSet() {
        return m_attrSet;
    }

    /**
     * Sets the attributes to be returned on a search.
     * 
     * @param attrSet
     *            set of attributes
     */
    void setAttributeSet(AttrSet attrSet) {
        // ??? Should we clone attrSet instead of keeping a reference?
        m_attrSet = attrSet;
    }

    /**
     * Returns a copy of the template.
     * 
     * @return A copy of the Template
     * 
     * @supported.api
     */
    public Object clone() {
        SearchTemplate t = (SearchTemplate) super.clone();
        if (m_attrSet != null) {
            t.setAttributeSet((AttrSet) m_attrSet.clone());
        }
        if (m_searchFilter != null) {
            t.setSearchFilter(m_searchFilter);
        }
        return t;
    }

    /**
     * Render the object.
     * 
     * @return The object in printable form
     * 
     * @supported.api
     */
    public String toString() {
        return "SearchTemplate: " + getName() + " { " + m_attrSet + " }";
    }

    private AttrSet m_attrSet = null;

    private String m_searchFilter = null;
}
