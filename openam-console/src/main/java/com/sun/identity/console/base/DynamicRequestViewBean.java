/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package com.sun.identity.console.base;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.shared.datastruct.CollectionHelper;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * This view bean handles dynamic requests made from the client.
 *
 * @since 13.0.0
 */
public abstract class DynamicRequestViewBean extends AMPrimaryMastHeadViewBean {

    protected boolean dynamicRequest = false;
    protected Map<String, Set<String>> unsavedAttributeValues;
    protected AMPropertySheetModel propertySheetModel;

    /**
     * Creates an instance of {@link DynamicRequestViewBean}.
     *
     * @param name Name of page.
     */
    public DynamicRequestViewBean(String name) {
        super(name);
    }

    /**
     * Extract the URL for the dynamic link and pass it to subclass to handle the link request.
     *
     * @param event Request invocation event.
     */
    public void handleDynLinkRequest(RequestInvocationEvent event) {
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();
        setDynamicRequest(false);
        if (model == null) {
            forwardTo();
        } else {
            HttpServletRequest request = event.getRequestContext().getRequest();
            String url = appendPgSession(model.getPropertiesViewBean(request.getParameter("attrname")));
            handleDynamicLinkRequest(url);
        }
    }

    /**
     * Populate the requested URL with the realm and pass it to subclass to handle the link request.
     *
     * @param event Request invocation event.
     */
    public void handleCreateDynLinkRequest(RequestInvocationEvent event) {
        HttpServletRequest request = event.getRequestContext().getRequest();
        handleDynamicLinkRequest(MessageFormat.format(request.getParameter("url"), getCurrentRealmEncoded(), ""));
    }

    /**
     * Populate the requested URL with the realm and ID of the attribute to edit, then pass it to subclass
     * to handle the link request.
     *
     * @param event Request invocation event.
     */
    public void handleEditDynLinkRequest(RequestInvocationEvent event) {
        HttpServletRequest request = event.getRequestContext().getRequest();
        String attrValue = CollectionHelper.getMapAttr(getAttributeValueMap(), request.getParameter("attrname"));
        attrValue = "[Empty]".equals(attrValue) ? "" : attrValue;
        handleDynamicLinkRequest(MessageFormat.format(request.getParameter("url"), getCurrentRealmEncoded(), attrValue));
    }

    /**
     * Store the unsaved attributes and reload the page.
     *
     * @param event Request invocation event.
     */
    public void handleRefreshDynLinkRequest(RequestInvocationEvent event) {
        setDynamicRequest(true);
        forwardTo();
    }

    private void setDynamicRequest(boolean dynamic) {
        // Store the current attribute values from the UI to render when beginDisplay is called
        unsavedAttributeValues = getAttributeValueMap();
        dynamicRequest = dynamic;
    }

    /**
     * Handle the appropriate dynamic link request.
     *
     * @param url The URL to link to.
     */
    protected abstract void handleDynamicLinkRequest(String url);

    /**
     * Get the current (could be unsaved) attribute values for the view bean.
     *
     * @return the attribute values
     */
    protected abstract Map<String, Set<String>> getAttributeValueMap();

}
