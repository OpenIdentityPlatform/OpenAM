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
 * $Id: ConfigureSalesForceAppsWarningViewBean.java,v 1.1 2009/07/01 23:27:21 babysunil Exp $
 *
 */

package com.sun.identity.console.task;

import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.identity.console.task.model.TaskModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import javax.servlet.http.HttpServletRequest;

/**
 * Create Warning for salesforce UI.
 */
public class ConfigureSalesForceAppsWarningViewBean
        extends AMPrimaryMastHeadViewBean {

    public static final String DEFAULT_DISPLAY_URL =
            "/console/task/ConfigureSalesForceAppsWarning.jsp";

    public ConfigureSalesForceAppsWarningViewBean() {
        super("ConfigureSalesForceAppsWarning");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent e) {
        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "create.salesforce.apps.missing.cot");
    }

    public String endIalertCommonDisplay(
            ChildContentDisplayEvent event) {
        TaskModel model = (TaskModel) getModel();
        String html = event.getContent();
        int idx = html.lastIndexOf("</div>");
        html = html.substring(0, idx + 6) +
                "<p>&nbsp;<p><center><div class=\"TtlBtnDiv\">" +
                "<input name=\"button1\" type=\"submit\" " + "" +
                "class=\"Btn1\" value=\"  " +
                model.getLocalizedString("button.ok") +
                "  \" onmouseover=\"javascript: this.className='Btn1Hov'\" " +
                "onmouseout=\"javascript: this.className='Btn1'\" " +
                "onblur=\"javascript: this.className='Btn1'\" " +
                "onfocus=\"javascript: this.className='Btn1Hov'\" " +
                "onClick=\"top.location.replace('../task/Home'); return false;\" " +
                "/></div></center>" + html.substring(idx + 6);
        return html;
    }
}
