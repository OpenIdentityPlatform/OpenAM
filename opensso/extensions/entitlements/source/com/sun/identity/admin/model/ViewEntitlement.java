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
 * $Id: ViewEntitlement.java,v 1.24 2009/08/05 14:37:15 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.DeepCloneableArrayList;
import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.ViewApplicationDao;
import com.sun.identity.admin.handler.BooleanActionsHandler;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.ValidateResourceResult;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.model.SelectItem;

public class ViewEntitlement implements Serializable {

    private List<Resource> resources = new ArrayList<Resource>();
    private List<Resource> exceptions = new ArrayList<Resource>();
    private BooleanActionsBean booleanActionsBean = new BooleanActionsBean();
    private ViewApplication viewApplication;
    private BooleanActionsHandler booleanActionsHandler = new BooleanActionsHandler();
    ;
    private List<Resource> availableResources = new ArrayList<Resource>();

    public ViewEntitlement() {
        booleanActionsHandler.setBooleanActionsBean(booleanActionsBean);
    }

    public ViewEntitlement(Entitlement e, Map<String, ViewApplication> viewApplications) {
        this();

        if (e == null) {
            return;
        }

        // application
        setViewApplication(viewApplications.get(e.getApplicationName()));

        // resources
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        Map<String, ResourceDecorator> resourceDecorators = (Map<String, ResourceDecorator>) mbr.resolve("resourceDecorators");
        for (String rs : e.getResourceNames()) {
            String resourceClassName = viewApplication.getViewApplicationType().getResourceClassName();
            Resource r;
            try {
                r = (Resource) Class.forName(resourceClassName).newInstance();
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            } catch (InstantiationException ie) {
                throw new RuntimeException(ie);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            r.setName(rs);

            // decorate resource (optionally)
            ResourceDecorator rd = resourceDecorators.get(r.getClass().getName());
            if (rd != null) {
                rd.decorate(r);
            }

            resources.add(r);
        }

        resetAvailableResources();

        // exceptions
        for (String rs : e.getExcludedResourceNames()) {
            String resourceClassName = viewApplication.getViewApplicationType().getResourceClassName();
            Resource r;
            try {
                r = (Resource) Class.forName(resourceClassName).newInstance();
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            } catch (InstantiationException ie) {
                throw new RuntimeException(ie);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }
            r.setName(rs);
            exceptions.add(r);
        }


        // actions
        for (String actionName : e.getActionValues().keySet()) {
            Boolean actionValue = e.getActionValues().get(actionName);
            BooleanAction ba = new BooleanAction();
            ba.setName(actionName);
            ba.setAllow(actionValue.booleanValue());
            booleanActionsBean.getActions().add(ba);
        }
    }

    private void resetAvailableResources() {
        if (viewApplication == null) {
            availableResources = Collections.emptyList();
        } else {
            availableResources = new DeepCloneableArrayList<Resource>(viewApplication.getResources()).deepClone();
            for (Resource r : resources) {
                if (!availableResources.contains(r)) {
                    availableResources.add(r);
                }
            }
        }
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<Resource> getExceptions() {
        return exceptions;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Resource[] getResourceArray() {
        return resources.toArray(new Resource[0]);
    }

    public void setResourceArray(Resource[] resourceArray) {
        resources = Arrays.asList(resourceArray);
    }

    public Entitlement getEntitlement() {
        Entitlement e = new Entitlement();

        e.setResourceNames(getResourceSet());
        e.setExcludedResourceNames(getExceptionSet());
        e.setActionValues(getActionMap());
        e.setApplicationName(viewApplication.getName());

        return e;
    }

    private Set<String> getResourceSet() {
        Set<String> resourceSet = new HashSet<String>();

        for (Resource r : resources) {
            resourceSet.add(r.getName());
        }

        return resourceSet;
    }

    private Set<String> getExceptionSet() {
        Set<String> exceptionSet = new HashSet<String>();

        for (Resource r : exceptions) {
            exceptionSet.add(r.getName());
        }

        return exceptionSet;
    }

    private Map<String, Boolean> getActionMap() {
        Map<String, Boolean> actionMap = new HashMap<String, Boolean>();

        for (Action a : booleanActionsBean.getActions()) {
            actionMap.put(a.getName(), (Boolean) a.getValue());
        }

        return actionMap;
    }

    public ViewApplication getViewApplication() {
        return viewApplication;
    }

    public void setViewApplication(ViewApplication viewApplication) {
        this.viewApplication = viewApplication;
        resetAvailableResources();
    }

    public String getResourcesToString() {
        return new ListFormatter(resources).toString();
    }

    public String getExceptionsToString() {
        return new ListFormatter(exceptions).toString();
    }

    public String getResourcesToFormattedString() {
        return new ListFormatter(resources).toFormattedString();
    }

    public String getExceptionsToFormattedString() {
        return new ListFormatter(exceptions).toFormattedString();
    }

    public BooleanActionsBean getBooleanActionsBean() {
        return booleanActionsBean;
    }

    public BooleanActionsHandler getBooleanActionsHandler() {
        return booleanActionsHandler;
    }

    public List<Resource> getAvailableResources() {
        return availableResources;
    }

    public List<SelectItem> getAvailableResourceItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (Resource r : getAvailableResources()) {
            items.add(new SelectItem(r, r.getName()));
        }

        return items;
    }

    public ValidateResourceResult validateResource(Resource r) {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        ViewApplicationDao vad = (ViewApplicationDao) mbr.resolve("viewApplicationDao");
        Application a = vad.getApplication(viewApplication);
        ValidateResourceResult vrr = a.validateResourceName(r.getName());
        return vrr;
    }
}
