package com.sun.identity.console.task;

import java.text.MessageFormat;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.OAuth2Model;
import com.sun.identity.console.task.model.OAuth2ModelImpl;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

public class ConfigureOAuth2ViewBean
        extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
            "/console/task/ConfigureOAuth2.jsp";
    private static final String PAGETITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String REALM = "tfRealm";
    private static final String TITLE_MESSAGE = "configure.oauth2profile.title.message";

    private CCPageTitleModel ptModel;
    private CCActionTableModel tableModel;
    private AMPropertySheetModel propertySheetModel;


    //public void forwardTo(RequestContext rc) {
    //}

    public ConfigureOAuth2ViewBean() {
        super("ConfigureOAuth2");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
        //ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PAGETITLE, CCPageTitle.class);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.create");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/propertyConfigureOAuth2.xml"));
        propertySheetModel.clear();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new OAuth2ModelImpl(req, getPageSessionAttributes());
    }

    public void beginDisplay(DisplayEvent e) {
        try {
            OAuth2Model model = (OAuth2Model) getModel();
            Set realms = model.getRealms();
            CCDropDownMenu menuRealm = (CCDropDownMenu) getChild(REALM);
            menuRealm.setOptions(createOptionList(realms));

            String realm = getRequestContext().getRequest().getParameter("realm");
            if (realm != null && !realm.trim().isEmpty()) {
                setDisplayFieldValue(REALM, realm);
            }
            final String titleMessage = model.getLocalizedString(TITLE_MESSAGE);
            ptModel.setPageTitleText(MessageFormat.format(titleMessage, model.getDisplayName()));
            ptModel.setPageTitleHelpMessage(model.getLocalizedHelpMessage());
        } catch (AMConsoleException ex) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    ex.getMessage());
        }
    }

    public String endPropertyAttributesDisplay(
            ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        return html;
    }

    static String removeSortHref(String html) {
        return html;
    }
}
