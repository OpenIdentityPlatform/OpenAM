/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: FilterBase.java,v 1.2 2008/11/28 12:36:21 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class FilterBase implements IConstants, IConfigKeys {
    protected boolean addFilterElement(XMLDocument xmldoc) {
        boolean result = true;

        try {
            if (!isAgentFilterPresent(xmldoc)) {
                int index = findElementIndexForFilter(xmldoc);

                addAgentFilterElement(
                    xmldoc,
                    index);
                addAgentFilterMappingElement(
                    xmldoc,
                    index + 1);

                xmldoc.store();
            }
        } catch (Exception ex) {
            Debug.log(
                "FilterBase.addFilterElement(): "
                + "Exception caught while adding <filter> to the " +
                "global web.xml file: "
                + ex.getMessage(),
                ex);
            result = false;
        }

        return result;
    }

    private boolean isAgentFilterPresent(XMLDocument xmldoc)
        throws Exception {
        boolean filterFound = false;

        ArrayList elements = xmldoc.getRootElement()
                                   .getNamedChildElements(ELEMENT_FILTER);

        Iterator elemItr = elements.iterator();

        while (elemItr.hasNext()) {
            if (hasAgentFilterNameElement((XMLElement) elemItr.next())) {
                // already contains the agent filter, so abort
                filterFound = true;

                break;
            }
        }

        return filterFound;
    }

    /*
     * This method attempts to find the ideal place for the Filter element. We
     * want to place our filter element before all other filter elements.
     * If the filter element is not found, then we try to discover if other
     * elements such as servlet are present and then place the filter
     * before those elements.
     */
    private int findElementIndexForFilter(XMLDocument xmldoc) {
        HashMap map = new HashMap();
        int index = 0;

        map.put(ELEMENT_FILTER, ELEMENT_FILTER);
        map.put(ELEMENT_LISTENER, ELEMENT_LISTENER);
        map.put(ELEMENT_SERVLET, ELEMENT_SERVLET);
        map.put(ELEMENT_SERVLET_MAPPING, ELEMENT_SERVLET_MAPPING);
        map.put(ELEMENT_MIME_MAPPING, ELEMENT_MIME_MAPPING);
        map.put(ELEMENT_WELCOME_FILE_LIST, ELEMENT_WELCOME_FILE_LIST);
        map.put(ELEMENT_SEC_CONSTRAINT, ELEMENT_SEC_CONSTRAINT);
        map.put(ELEMENT_LOGIN_CONFIG, ELEMENT_LOGIN_CONFIG);
        map.put(ELEMENT_SEC_ROLE, ELEMENT_SEC_ROLE);
        map.put(ELEMENT_ENV_ENTRY, ELEMENT_ENV_ENTRY);
        map.put(ELEMENT_EJB_REF, ELEMENT_EJB_REF);
        map.put(ELEMENT_EJB_LOCAL_REF, ELEMENT_EJB_LOCAL_REF);

        ArrayList appElements = xmldoc.getRootElement()
                                      .getChildElements();

        for (; index < appElements.size(); index++) {
            String name = ((XMLElement) appElements.get(index)).getName();
            Debug.log(
                "FilterBase.addFilterElement: Examining element " + name);

            if (map.containsKey(name)) {
                Debug.log(
                    "FilterBase.addFilterElement: Found element " + name
                    + " in web.xml");

                break;
            }
        }

        return index;
    }

    protected boolean hasAgentFilterNameElement(XMLElement element)
        throws Exception {
        boolean result = false;

        Debug.log(
            "FilterBase:hasAgentFilterNameElement(): Examining Filter "
            + element.getName());

        ArrayList childElements = element.getNamedChildElements(
                ELEMENT_FILTER_NAME);

        if ((childElements != null) && (childElements.size() == 1)) {
            XMLElement filterNameElement = (XMLElement) childElements.get(
                    0);

            if (filterNameElement.getValue()
                                     .equals(FILTER_NAME)) {
                result = true;
                Debug.log(
                    "FilterBase:hasAgentFilterNameElement(): " +
                    "SJS Tomcat Agent Filter has been configured for " +
                    "this web.xml file");
            }
        }

        return result;
    }

    private XMLElement addAgentFilterElement(
        XMLDocument xmldoc,
        int index) throws Exception {
        XMLElement filterElem = xmldoc.newElement(ELEMENT_FILTER);

        xmldoc.getRootElement()
              .addChildElementAt(
            filterElem,
            index,
            true);

        filterElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_FILTER_NAME,
                FILTER_NAME),
            true);
        filterElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_DISPLAY_NAME,
                FILTER_DISPLAY_NAME),
            true);
        filterElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_DESCRIPTION,
                FILTER_DESCRIPTION),
            true);
        filterElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_FILTER_CLASS,
                FILTER_CLASS),
            true);

        return filterElem;
    }

    private XMLElement addAgentFilterMappingElement(
        XMLDocument xmldoc,
        int index) throws Exception {
        XMLElement filterMappingElem = xmldoc.newElement(
                ELEMENT_FILTER_MAPPING);

        xmldoc.getRootElement()
              .addChildElementAt(
            filterMappingElem,
            index,
            true);

        filterMappingElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_FILTER_NAME,
                FILTER_NAME),
            true);
        filterMappingElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_URL_PATTERN,
                FILTER_URL_PATTERN),
            true);
        filterMappingElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_DISPATCHER,
                ELEMENT_DISPATCHER_VALUE_REQUEST),
            true);
        filterMappingElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_DISPATCHER,
                ELEMENT_DISPATCHER_VALUE_INCLUDE),
            true);
        filterMappingElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_DISPATCHER,
                ELEMENT_DISPATCHER_VALUE_FORWARD),
            true);
        filterMappingElem.addChildElement(
            xmldoc.newElement(
                ELEMENT_DISPATCHER,
                ELEMENT_DISPATCHER_VALUE_ERROR),
            true);

        return filterMappingElem;
    }

    protected boolean removeFilterElement(XMLDocument xmldoc) {
        boolean status = false;
        int index = 0;
        boolean filterFound = false;
        boolean match = false;
        XMLElement filterElement = null;

        try {
            ArrayList elements = xmldoc.getRootElement()
                                       .getNamedChildElements(
                    ELEMENT_FILTER);

            Iterator elemItr = elements.iterator();

            while (elemItr.hasNext()) {
                filterElement = (XMLElement) elemItr.next();

                if (hasAgentFilterNameElement(filterElement)) {
                    filterFound = true;

                    break;
                }
            }

            if (filterFound) {
                filterElement.delete();
            }

            status = true;
        } catch (Exception ex) {
            Debug.log(
                "FilterBase.removeFilterElement(): "
                + "Exception caught while removing Agent filter from " +
                "the global web.xml file: "
                + ex.getMessage(),
                ex);
        }

        return status;
    }

    protected boolean removeFilterMappingElement(XMLDocument xmldoc) {
        boolean status = false;
        int index = 0;
        boolean filterFound = false;
        boolean match = false;
        ArrayList filterNameElements = null;
        XMLElement filterMappingElement = null;
        XMLElement filterNameElement = null;

        try {
            ArrayList elements = xmldoc.getRootElement()
                                       .getNamedChildElements(
                    ELEMENT_FILTER_MAPPING);

            Iterator elemItr = elements.iterator();

            while (elemItr.hasNext()) {
                filterMappingElement = (XMLElement) elemItr.next();

                filterNameElements = filterMappingElement
                    .getNamedChildElements(ELEMENT_FILTER_NAME);

                if ((filterNameElements != null)
                        && (filterNameElements.size() > 0)) {
                    filterNameElement = (XMLElement) filterNameElements
                        .get(0);

                    if (filterNameElement.getValue()
                                             .equalsIgnoreCase(
                                FILTER_NAME)) {
                        filterFound = true;

                        break;
                    }
                }
            }

            if (filterFound) {
                Debug.log(
                    "FilterBase.removeFilterMappingElement(): "
                    + "Found Agent filter mapping element");

                filterMappingElement.delete();
            }

            status = true;
        } catch (Exception ex) {
            Debug.log(
                "FilterBase.removeFilterElement(): "
                + "Exception caught while removing Agent filter from " +
                "the global web.xml file: "
                + ex.getMessage(),
                ex);
        }

        return status;
    }
}
