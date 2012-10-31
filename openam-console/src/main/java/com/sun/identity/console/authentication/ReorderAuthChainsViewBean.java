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
 * $Id: ReorderAuthChainsViewBean.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AuthConfigurationEntry;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.authentication.model.AuthConfigurationModelImpl;
import com.sun.web.ui.model.CCOrderableListModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.orderablelist.CCOrderableList;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.ArrayList;
import java.util.List;

public class ReorderAuthChainsViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/authentication/ReorderAuthChains.jsp";
    static final String PG_SESSION_TRACKING = "acReorderTrack";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String REORDER_LIST = "reorderList";

    private CCPageTitleModel ptModel;

    /**
     * Creates a realm creation view bean.
     */
    public ReorderAuthChainsViewBean() {
        super("ReorderAuthChains");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(REORDER_LIST, CCOrderableList.class);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(REORDER_LIST)) {
            CCOrderableListModel model = new CCOrderableListModel();
            model.setSelectedLabel(
                "authentication.module.config.reorder.label");
            view = new CCOrderableList(this, model, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        CCOrderableList list = (CCOrderableList)getChild(REORDER_LIST);
        CCOrderableListModel model = (CCOrderableListModel)list.getModel();

        String xml = (String)getPageSessionAttribute(
            AuthConfigViewBean.ENTRY_LIST);
        List chains = new ArrayList(
            AMAuthConfigUtils.xmlToAuthConfigurationEntry(xml));
        OptionList optList = new OptionList();
        int sz = chains.size();

        for (int i = 0; i < sz; i++) {
            AuthConfigurationEntry entry =(AuthConfigurationEntry)chains.get(i);
            String name = entry.getLoginModuleName();
            String flag = entry.getControlFlag();
            String options = entry.getOptions();

            String displayName = name + " - " + flag;
            if ((options != null) && (options.trim().length() > 0)) {
                displayName += " - " + options;
            }

            optList.add(displayName, Integer.toString(i));
        }
        model.setSelectedOptionList(optList);
    }

    /**
     * Handles change request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        String xml = (String)getPageSessionAttribute(
            AuthConfigViewBean.ENTRY_LIST);
        List chains = new ArrayList(
            AMAuthConfigUtils.xmlToAuthConfigurationEntry(xml));
        List newChains = new ArrayList();

        CCOrderableList list = (CCOrderableList)getChild(REORDER_LIST);
        list.restoreStateData();
        CCOrderableListModel model = (CCOrderableListModel)list.getModel();
        OptionList optList = model.getSelectedOptionList();
        int sz = optList.size();

        for (int i = 0; i < sz; i++) {
            String idx = optList.getValue(i);
            int num = Integer.parseInt(idx);
            newChains.add(chains.get(num));
        }

        setPageSessionAttribute(AuthConfigViewBean.ENTRY_LIST,
            AMAuthConfigUtils.authConfigurationEntryToXMLString(newChains));
        forwardToAuthConfigViewBean();
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        forwardToAuthConfigViewBean();
    }

    private void forwardToAuthConfigViewBean() {
        AuthConfigViewBean vb = (AuthConfigViewBean)
            getViewBean(AuthConfigViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        return new AuthConfigurationModelImpl(
            rc.getRequest(), getPageSessionAttributes());
    }
}
