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
 * $Id: ActionSchemaGUI.java,v 1.2 2008/06/25 05:43:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewInterface;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.policy.ActionSchema;
import com.sun.web.ui.common.CCTagClass;
import com.sun.web.ui.taglib.common.CCTagBase;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

public class ActionSchemaGUI {
    private static ActionSchemaGUI instance = new ActionSchemaGUI();

    private ActionSchemaGUI() {
    }

    public static ActionSchemaGUI getInstance() {
        return instance;
    }

    public String getHTMLString(
        AMViewBeanBase viewBean,
        PageContext pageContext,
        ActionSchema actionSchema,
        String actionTableName
    ) throws JspException {
        StringBuilder buff = new StringBuilder(1000);
        int syntax = AMDisplayType.getInstance().getDisplaySyntax(actionSchema);
        HttpServletRequest req = viewBean.getRequestContext().getRequest();
        CCTagBase parentTag = getCCTag(req, CCTagClass.ACTIONTABLE);
        parentTag.setName(actionTableName);

        switch (syntax) {
        case AMDisplayType.SYNTAX_RADIO:
            String viewName = AMViewInterface.PREFIX_RADIO_BUTTON +
                actionSchema.getName();
            View view = viewBean.getChild(viewName);
            CCTagBase tag = getCCTag(req, CCTagClass.RADIOBUTTON);
            tag.setName(viewName);
            buff.append(tag.getHTMLString(parentTag, pageContext, view));
            break;
        }

        return buff.toString();
    }

    private CCTagBase getCCTag(HttpServletRequest req, String name)
        throws JspException {
        CCTagBase tag = null;
        Class tagclass = null;
                                                                                
        try {
            ClassLoader classLoader = getClassLoader(req);
            tagclass = classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new JspException(e.getMessage());
        }
                                                                                
        if (CCTagBase.class.isAssignableFrom(tagclass)) {
            try {
                tag = (CCTagBase)tagclass.newInstance();
            } catch (InstantiationException e) {
                throw new JspException(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new JspException(e.getMessage());
            }
        } else {
            throw new IllegalArgumentException(
                "CCTagBase is not a superclass of: " + tagclass);
        }
                                                                                
        return tag;
    }

    private ClassLoader getClassLoader(HttpServletRequest request) {
        ClassLoader classLoader = RequestManager.getHandlingServlet().
            getClass().getClassLoader();
        return (classLoader != null)
            ? classLoader
            : this.getClass().getClassLoader();
    }
}
