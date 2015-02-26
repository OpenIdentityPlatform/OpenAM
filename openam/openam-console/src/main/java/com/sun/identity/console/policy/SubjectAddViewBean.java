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
 * $Id: SubjectAddViewBean.java,v 1.2 2008/06/25 05:43:06 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.NameAlreadyExistsException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Set;

public class SubjectAddViewBean
    extends SubjectOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SubjectAdd.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    SubjectAddViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    /**
     * Creates a policy creation view bean.
     */
    public SubjectAddViewBean() {
        super("SubjectAdd", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        boolean wizard =
            getPageSessionAttribute(PolicyOpViewBeanBase.WIZARD) != null;
        String ptTitle = "page.title.policy.subject.create";
        if (!wizard) {
            ptTitle = "page.title.policy.subject.create.shortcut";
            disableButton("button1", true);
        }

        PolicyModel model = (PolicyModel)getModel();
        String i18nName = (String)propertySheetModel.getValue(
            SUBJECT_TYPE_NAME);
        String title = model.getLocalizedString(ptTitle);
        String[] param = {i18nName};
        ptModel.setPageTitleText(MessageFormat.format(title, (Object[])param));
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return "com/sun/identity/console/propertyPMSubjectAdd.xml";
    }

    /**
     * Handles create policy's subject request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        boolean forwarded = false;
        submitCycle = true;
        bFilter = true;

        try {
            Subject subject = createSubject();
            if (subject != null) {
                CachedPolicy cachedPolicy = getCachedPolicy();
                Policy policy = cachedPolicy.getPolicy();
                String name = (String)propertySheetModel.getValue(
                    SUBJECT_NAME);
                policy.addSubject(name, subject, isExclusive());
                backTrail();
                forwardToPolicyViewBean();
                forwarded = true;
            }
        } catch (NameAlreadyExistsException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (InvalidNameException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        } finally {
            if (!forwarded) {
                forwardTo();
            }
        }
    }

    protected Set getDefaultValues() {
        return null;
    }

    protected boolean isSubjectExclusive() {
        return false;
    }

    protected boolean hasValues() {
        return true;
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.addSubject";
    }

    protected boolean startPageTrail() {
        return false;
    }

}
