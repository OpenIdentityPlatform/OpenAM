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
 * $Id: UMUserPasswordResetOptionsViewBean.java,v 1.4 2008/09/22 20:17:37 veiming Exp $
 *
 */

package com.sun.identity.console.user;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.CloseWindowViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.idm.EntityEditViewBean;
import com.sun.identity.console.realm.RMRealmViewBeanBase;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsData;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsModel;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class UMUserPasswordResetOptionsViewBean
    extends RMRealmViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/user/UMUserPasswordResetOptions.jsp";

    public static final String TBL_SEARCH = "tblSearch";
    public static final String TBL_COL_QUESTION = "tblColQuestion";
    public static final String TBL_COL_ANSWER = "tblColAnswer";
    public static final String TBL_DATA_QUESTION = "tblDataQuestion";
    public static final String TBL_DATA_PERSONAL_QUESTION =
        "tblDataPersonalQuestion";
    public static final String TBL_DATA_ANSWER = "tblDataAnswer";
    public static final String PAGETITLE = "pgtitle";
    public static final String CHILD_USER_PWD_RESET_TILED_VIEW =
        "pwdResetTiledView";
    public static final String CB_FORCE_RESET_PWD = "cbForceResetPwd";

    private CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel;
    private boolean tblModelPopulated = false;

    public UMUserPasswordResetOptionsViewBean() {
        super("UMUserPasswordResetOptions");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        createPageTitleModel();
        registerChildren();
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.close");
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        registerChild(CB_FORCE_RESET_PWD, CCCheckBox.class);
        registerChild(CHILD_USER_PWD_RESET_TILED_VIEW,
            UMUserPasswordResetOptionsTiledView.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(CHILD_USER_PWD_RESET_TILED_VIEW)) {
            view = new UMUserPasswordResetOptionsTiledView(
                this, tblModel, name);
        } else if (name.equals(TBL_SEARCH)) {
            populateTableModelEx();
            CCActionTable child = new CCActionTable(this, tblModel, name);
            child.setTiledView((ContainerView)getChild(
                CHILD_USER_PWD_RESET_TILED_VIEW));
            view = child;
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        if (!tblModelPopulated) {
            getQuestions();
            CCCheckBox cbForceResetPwd = (CCCheckBox)getChild(
                CB_FORCE_RESET_PWD);
            UMUserPasswordResetOptionsModel model =
                (UMUserPasswordResetOptionsModel)getModel();
            String userId = (String)getPageSessionAttribute(
                EntityEditViewBean.UNIVERSAL_ID);
            cbForceResetPwd.setChecked(model.isForceReset(userId));
        }
    }
    
    public boolean beginForceResetDisplay(ChildDisplayEvent event) {        
        UMUserPasswordResetOptionsModel model =
                (UMUserPasswordResetOptionsModel)getModel();
        return model.isRealmAdmin();
    }

    public boolean beginQuestionsDisplay(ChildDisplayEvent event) {
        boolean display = false;
        UMUserPasswordResetOptionsModel model =
            (UMUserPasswordResetOptionsModel)getModel();
        String userId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);

        if (model.isLoggedInUser(userId)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            List data = (List)szCache.getSerializedObj();
            display = (data != null) && !data.isEmpty();
        }

        return display;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new UMUserPasswordResetOptionsModelImpl(
            req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblUMUserPasswordResetOptions.xml"));
        tblModel.setTitleLabel("label.question");
        tblModel.setActionValue(TBL_COL_QUESTION,
            "table.user.password.reset.name.column.question");
        tblModel.setActionValue(TBL_COL_ANSWER,
            "table.user.password.reset.name.column.answer");
    }

    private void getQuestions() {
        UMUserPasswordResetOptionsModel model =
            (UMUserPasswordResetOptionsModel)getModel();
        String userId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);

        try {
            List questionAnswers = model.getUserAnswers(userId);
            populateTableModel(questionAnswers);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateTableModelEx() {
        if (!tblModelPopulated) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            List data = (List)szCache.getSerializedObj();

            if (data != null) {
                populateTableModel(data);
                tblModelPopulated = true;
            }
        }
    }

    public UMUserPasswordResetOptionsData getUserPasswordResetOptionsData(
        int i
    ) {
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        return (UMUserPasswordResetOptionsData)
            ((List)szCache.getSerializedObj()).get(i);
    }

    private void populateTableModel(List questionAnswers) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        szCache.setValue(questionAnswers);

        if ((questionAnswers != null) && !questionAnswers.isEmpty()) {
            boolean firstEntry = true;

            for (Iterator iter = questionAnswers.iterator(); iter.hasNext(); ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }

                UMUserPasswordResetOptionsData data =
                    (UMUserPasswordResetOptionsData)iter.next();

                String question = data.getQuestionLocalizedName();
                if (data.isPersonalQuestion()) {
                    tblModel.setValue(TBL_DATA_QUESTION, null);
                    tblModel.setValue(TBL_DATA_PERSONAL_QUESTION, question);
                } else {
                    tblModel.setValue(TBL_DATA_QUESTION, question);
                    tblModel.setValue(TBL_DATA_PERSONAL_QUESTION, null);
                }

                tblModel.setRowSelected(data.isSelected());
                tblModel.setValue(TBL_DATA_ANSWER, data.getAnswer());
            }
        }
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        tblModelPopulated = false;
        forwardTo();
    }

    /**
     * Handles save password options request.
     *
     * @param event Request invocation event.
     * @throws ModelControlException if action table model state cannot be 
     *               restored.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        List optionData = restoreOptionsData();
        CCCheckBox cbForceResetPwd = (CCCheckBox)getChild(CB_FORCE_RESET_PWD);
        boolean forceResetPwd = cbForceResetPwd.isChecked();

        UMUserPasswordResetOptionsModel model =
            (UMUserPasswordResetOptionsModel)getModel();
        String userId = (String)getPageSessionAttribute(
            EntityEditViewBean.UNIVERSAL_ID);

        try {
            model.modifyUserOption(optionData, userId, forceResetPwd);

            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("profile.updated"));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles close browser window request.
     *
     * @param event Request invocation event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        CloseWindowViewBean vb = (CloseWindowViewBean)getViewBean(
            CloseWindowViewBean.class);
        vb.forwardTo(getRequestContext());
    }

    private List restoreOptionsData() throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();

        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List optionData = (List)szCache.getSerializedObj();
        int sz = optionData.size();

        for (int i = 0; i <sz; i++) {
            UMUserPasswordResetOptionsData data =
                (UMUserPasswordResetOptionsData)optionData.get(i);
            tblModel.setRowIndex(i);
            data.setSelected(tblModel.isRowSelected());
            data.setAnswer((String)tblModel.getValue(TBL_DATA_ANSWER));

            if (data.isPersonalQuestion()) {
                data.setQuestion((String)tblModel.getValue(
                    TBL_DATA_PERSONAL_QUESTION));
            }
        }

        return optionData;
    }
}
