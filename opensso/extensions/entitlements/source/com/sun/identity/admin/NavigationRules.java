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
 * $Id: NavigationRules.java,v 1.2 2009/06/04 11:49:11 veiming Exp $
 */

package com.sun.identity.admin;

import org.jdom.input.SAXBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

public class NavigationRules {

    private Map<String, NavigationRule> navigationRules = new HashMap<String, NavigationRule>();

    public Map<String, NavigationRule> getNavigationRules() {
        return navigationRules;
    }

    public void setNavigationRules(Map<String, NavigationRule> navigationRules) {
        this.navigationRules = navigationRules;
    }

    public static class NavigationRule {

        private String fromViewId;
        private Map<String, NavigationCase> navigationCases = new HashMap<String, NavigationCase>();

        public String getFromViewId() {
            return fromViewId;
        }

        public void setFromViewId(String fromViewId) {
            this.fromViewId = fromViewId;
        }

        public Map<String, NavigationCase> getNavigationCases() {
            return navigationCases;
        }

        public void setNavigationCases(Map<String, NavigationCase> navigationCases) {
            this.navigationCases = navigationCases;
        }
    }

    public static class NavigationCase {

        public String getFromOutcome() {
            return fromOutcome;
        }

        public void setFromOutcome(String fromOutcome) {
            this.fromOutcome = fromOutcome;
        }

        public String getToViewId() {
            return toViewId;
        }

        public void setToViewId(String toViewId) {
            this.toViewId = toViewId;
        }
        private String fromOutcome;
        private String toViewId;
    }

    public NavigationRules() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ServletContext sc = (ServletContext) ec.getContext();

        URL u;
        try {
            u = sc.getResource("/WEB-INF/faces-config.xml");
        } catch (MalformedURLException mfue) {
            throw new RuntimeException(mfue);
        }

        InputStream is;
        try {
            is = u.openStream();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        SAXBuilder parser = new SAXBuilder();
        Document d;

        try {
            d = parser.build(is);
        } catch (JDOMException je) {
            throw new RuntimeException(je);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        Namespace ns = Namespace.getNamespace("http://java.sun.com/xml/ns/javaee");
        Element rootElement = d.getRootElement();
        List<Element> nrElements = rootElement.getChildren("navigation-rule", ns);
        for (Element nrElement: nrElements) {
            NavigationRule nr = new NavigationRule();
            Element fvidElement = nrElement.getChild("from-view-id", ns);
            String fromViewId = null;
            if (fvidElement != null) {
                fromViewId = fvidElement.getText();
            }
            nr.setFromViewId(fromViewId);

            List<Element> ncElements = nrElement.getChildren("navigation-case", ns);
            for (Element ncElement: ncElements) {
                NavigationCase nc = new NavigationCase();
                Element foElement = ncElement.getChild("from-outcome", ns);
                String fromOutcome = foElement.getText();
                nc.setFromOutcome(fromOutcome);
                Element tvidElement = ncElement.getChild("to-view-id", ns);
                String toViewId = tvidElement.getText();
                nc.setToViewId(toViewId);

                nr.getNavigationCases().put(fromOutcome, nc);
            }

            navigationRules.put(fromViewId, nr);
        }
    }
}
