/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PageTrail.java,v 1.2 2008/06/25 05:42:48 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base;

import com.sun.identity.console.base.model.AMConsoleException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class captures a chain of markers where each marker contains information
 * on the page session needed to activate view bean.
 */
public class PageTrail
    implements Serializable
{
    private List trail = new ArrayList();

    /**
     * Default constructor that is required for serialization.
     */
    public PageTrail() {
    }

    /**
     * Creates a page trail.
     *
     * @param displayName Display Name.
     * @param viewBeanClassName Class Name of View Bean.
     * @param pageSessionAttributeValues Map of attribute name (String) to 
     *        a value (Serializable).
     */
    public PageTrail(
        String displayName,
        String viewBeanClassName,
        Map pageSessionAttributeValues
    ) {
        add(displayName, viewBeanClassName, pageSessionAttributeValues);
    }

    /**
     * Set trail.
     *
     * @param displayName Display Name.
     * @param viewBeanClassName Class Name of View Bean.
     * @param pageSessionAttributeValues Map of attribute name (String) to 
     *        a value (Serializable).
     */
    public void set(
        String displayName,
        String viewBeanClassName,
        Map pageSessionAttributeValues
    ) {
        trail.clear();
        add(displayName, viewBeanClassName, pageSessionAttributeValues);
    }

    /**
     * Swap the last item of trail.
     *
     * @param displayName Display Name.
     * @param viewBeanClassName Class Name of View Bean.
     * @param pageSessionAttributeValues Map of attribute name (String) to 
     *        a value (Serializable).
     */
    public void swap(
        String displayName,
        String viewBeanClassName,
        Map pageSessionAttributeValues
    ) {
        trail.remove(trail.size()-1);
        trail.add(new Marker(
            displayName, viewBeanClassName, pageSessionAttributeValues));
    }

    /**
     * Adds item to trail.
     *
     * @param displayName Display Name.
     * @param viewBeanClassName Class Name of View Bean.
     * @param pageSessionAttributeValues Map of attribute name (String) to 
     *        a value (Serializable).
     */
    public void add(
        String displayName,
        String viewBeanClassName,
        Map pageSessionAttributeValues
    ) {
        if (!trail.isEmpty()) {
            Marker lastMarker = (Marker)trail.get(trail.size() -1);
            if (lastMarker.getViewBeanClassName().equals(viewBeanClassName)) {
                trail.remove(trail.size() -1);
            }
        }
        trail.add(new Marker(
            displayName, viewBeanClassName, pageSessionAttributeValues));
    }

    /**
     * Pops the last item in the list.
     *
     * @return the Marker of the second last item.
     */
    public Marker pop() {
        try {
            int idx = (trail.size() -2);
            return (idx < 0) ? backTo(0) : backTo(idx);
        } catch (AMConsoleException e) {
            // NO-OP
            // This exception sbould not occur because checks are already
            // done before calling backTo methid.
            return null;
        }
    }

    /**
     * Discards items from a given index (exclusive) to the end.
     *
     * @param idx Index.
     * @return the Marker for <code>idx</code>.
     * @throws AMConsoleException if cannot be discard items in page trail.
     */
    public Marker backTo(int idx)
        throws AMConsoleException
    {
        int size = trail.size();

        if (idx >= size) {
            throw new AMConsoleException(
                "cannot be discard items in page trail");
        }

        for (int i = size -1; i > idx; --i) {
            trail.remove(i);
        }
        return (Marker)trail.get(idx);
    }

    /**
     * Returns a list of marker in this trail.
     *
     * @return a list of marker in this trail.
     */
    public List getMarkers() {
        return trail;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        for (Iterator iter = trail.iterator(); iter.hasNext(); ) {
            Marker m = (Marker)iter.next();
            buff.append(m.toString());
        }
        return buff.toString();
    }

    /**
     * An item in a chain of bread crumb.
     */
    public class Marker
        implements Serializable
    {
        private String displayName;
        private String viewBeanClassName;
        private Map pageSessionAttributeValues;

        public Marker() {
        }

        public Marker(
            String displayName,
            String viewBeanClassName,
            Map pageSessionAttributeValues
        ) {
            if (displayName.startsWith("//")) {
                displayName = displayName.substring(1);
            }
            this.displayName = displayName;
            this.viewBeanClassName = viewBeanClassName;
            this.pageSessionAttributeValues = pageSessionAttributeValues;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getViewBeanClassName() {
            return viewBeanClassName;
        }

        public Map getPageSessionAttributeValues() {
            return pageSessionAttributeValues;
        }
    
        public String toString() {
            return "\ndisplayName=" + displayName +
                "\nviewBeanClassName=" + viewBeanClassName +
                "\npageSessionAttributeValues=" + pageSessionAttributeValues;
        }
    }
}
