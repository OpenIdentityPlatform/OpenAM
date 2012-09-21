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
 * $Id: AMViewBeanBase.java,v 1.15 2009/10/19 18:17:33 asyhuang Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base;

import com.iplanet.am.util.BrowserEncoding;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.util.Encoder;
import com.iplanet.jato.view.DisplayField;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.ViewBeanBase;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Button;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.SSOTokenEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMI18NUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSSchema;
import com.sun.web.ui.model.CCActionTableModelInterface;
import com.sun.web.ui.common.CCPrivateConfiguration;
import com.sun.web.ui.view.alert.CCAlertInline;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCCheckBox;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.html.CCHref;
import com.sun.web.ui.view.html.CCLabel;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.common.CCI18N;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.server.UID;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.owasp.esapi.ESAPI;

/**
 * This is the base class for all view beans in Console.
 */
public abstract class AMViewBeanBase
    extends ViewBeanBase
{
    public static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);
        
    private static final int MAX_PG_SESSION_SIZE = 1024-32;
    private static final String TXT_LOCATION = "txtLocation";
    private static final String TXT_RANDOM_STR = "txtRandomStr";
    private static final String PG_SESSION = "pgSession";
    private static final String IALERT_COMMON = "ialertCommon";
    protected static final String PG_SESSION_ATTR_ID = "dsame.pgSessionID";
    protected static final String PG_SESSION_SSO_ID = "dsame.pgSessionSSOID";
    protected static final String PG_SESSION_ATTR = "dsame.pgSession";
    protected static final String SZ_CACHE = "szCache";
    protected static final String BTN_GHOST = "g";
    protected boolean initialized = false;

    private final String vbUID = new UID().toString();
    private boolean pageSessionInSessionStore = false;
    private AMModel dataModel;
    private Set blankTextFields = new HashSet();

    static {
        String consoleRemote = SystemProperties.get(
            Constants.AM_CONSOLE_REMOTE);
        boolean remote = (consoleRemote != null) &&
            consoleRemote.equalsIgnoreCase("true");

        String host = (remote) ?
            SystemProperties.get(Constants.AM_CONSOLE_HOST) :
            SystemProperties.get(Constants.AM_SERVER_HOST);
        System.setProperty("com.sun.web.console.securehost", host);
        System.setProperty("com.sun.web.console.unsecurehost", host);

        String port = (remote) ?
            SystemProperties.get(Constants.AM_CONSOLE_PORT) :
            SystemProperties.get(Constants.AM_SERVER_PORT);
        System.setProperty("com.sun.web.console.secureport", port);
        System.setProperty("com.sun.web.console.unsecureport", port);
        
        String protocol = (remote) ?
            SystemProperties.get(Constants.AM_CONSOLE_PROTOCOL) :
            SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
        CCPrivateConfiguration.setSecureHelp(protocol.equals("https")); 
    }

    /**
     * Creates an instance of base view bean object
     *
     * @param name of page
     */
    public AMViewBeanBase(String name) {
        super(name);
    }

    public void forwardTo(RequestContext rc)
        throws NavigationException {
        initialize();
        super.forwardTo(rc);
    }

    public void setRequestContext(RequestContext context) {
        super.setRequestContext(context);
        handlePageSessionThruURL(context);
        setRequestContentInitialize(context);
    }
    
    protected void setRequestContentInitialize(RequestContext context) {
        initialize();
    }

    protected void initialize() {
    }

    /**
     * Registers user interface components used by this view bean.
     */
    protected void registerChildren() {
        registerChild(SZ_CACHE, SerializedField.class);
        registerChild(BTN_GHOST, Button.class);
    }

    /**
     * Creates user interface components used by this view bean.
     *
     * @param name of component
     * @return child component
     */
    protected View createChild(String name) {
        View view = null;

        if (name.equals(PG_SESSION)) {
            view = new StaticTextField(this, name, "");
        } else if (name.equals(BTN_GHOST)) {
            view = new Button(this, name, "");
        } else if (name.startsWith(AMViewInterface.PREFIX_STATIC_TXT)) {
            view = new CCStaticTextField(this, name, null);
        } else if ( name.startsWith(AMViewInterface.PREFIX_SERIALIZABLE)) {
            view = new SerializedField(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_CHECKBOX)) {
            view = new CCCheckBox(this, name, "true", "false", false);
        } else if (name.startsWith(AMViewInterface.PREFIX_TEXTFIELD)) {
            view = new CCTextField(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_LABEL)) {
            view = new CCLabel(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_BUTTON)) {
            view = new CCButton(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_SINGLE_CHOICE)) {
            view = new CCDropDownMenu(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_HREF)) {
            view = new CCHref(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_RADIO_BUTTON)) {
            view = new CCRadioButton(this, name, null);
        } else if (name.startsWith(AMViewInterface.PREFIX_INLINE_ALERT)) {
            view = new CCAlertInline(this, name, null);
        } else {
            throw new IllegalArgumentException(
                "Invalid child name [" + name + "]");
        }

        return view;
    }

    /**
     * Picks up page session information for request and update page session
     * map.
     *
     * @param context request context
     */
    private void handlePageSessionThruURL(RequestContext context) {
        HttpServletRequest req = context.getRequest();
        String pgSession = req.getParameter(PG_SESSION_ATTR);

        if (pgSession != null) {
            try {
                Map map = (Map) Encoder.deserialize(
                    Encoder.decodeHttp64(pgSession), false);

                if (map != null) {
                    for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                        String k = (String) i.next();
                        setPageSessionAttribute(k, (Serializable)map.get(k));
                    }
                }
            } catch (ClassNotFoundException e) {
                debug.warning("AMModelBase.handlePageSessionThruURL", e);
            } catch (IOException e) {
                debug.warning("AMModelBase.handlePageSessionThruURL", e);
            }
        }
    }

    /**
     * Gets current charset
     *
     * @param model object
     * @return current charset
     */
    public String getCharset(AMModel model) {
        Locale locale = model.getUserLocale();
        String agentType = model.getClientType();
        String charset = AMI18NUtils.getCharset(agentType, locale);
        String jCharset = BrowserEncoding.mapHttp2JavaCharset(charset);
        return jCharset;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        AMModel model = getModel();
        setPageEncoding(model);
        setDisplayFieldValue(TXT_RANDOM_STR, model.getRandomString());
        setDisplayFieldValue(PG_SESSION, getPageSessionAttributeString());
        setDisplayFieldValue(TXT_LOCATION,
            getModel().getLocalizedString("label.location"));
        performDelegationTasks();
    }

    /**
     * Pass session attribute map to other view bean
     *
     * @param other view bean
     */
    public void passPgSessionMap(ViewBean other) {
        passPgSessionMap(other, getPageSessionAttributes());
    }

    /**
     * Pass session attribute map to other view bean
     *
     * @param other view bean
     * @param attributes Map of attribute name to values.
     */
    public void passPgSessionMap(ViewBean other, Map attributes) {
        if ((attributes != null) && (attributes.size() > 0)) {
            Iterator iter = attributes.keySet().iterator();

            while (iter.hasNext()) {
                String key = (String) iter.next();
                other.setPageSessionAttribute(
                    key, (Serializable)attributes.get(key));
            }
        }
    }

    /**
     * Returns model for this view bean.
     * Dervived class need to implement this method.
     *
     * @return model for this view bean.
     */
    public AMModel getModel() {
        if (dataModel == null) {
            dataModel = getModelInternal();
        }

        return dataModel;
    }

    /**
     * Set inline alert message.
     *
     * @param type Type of message such as Error, Warning, etc.
     * @param title Title of message.
     * @param message Message.
     */
    protected void setInlineAlertMessage(
        String type,
        String title,
        String message
    ) {
        CCAlertInline alert = (CCAlertInline)getChild(IALERT_COMMON);
        org.owasp.esapi.Encoder enc = ESAPI.encoder();
        alert.setType(type);
        alert.setSummary(enc.encodeForHTML(title));
        alert.setDetail(enc.encodeForHTML(message));
    }

    /**
     * Returns true if inline alert message is set.
     *
     * @return true if inline alert message is set.
     */
    protected boolean isInlineAlertMessageSet() {
        CCAlertInline alert = (CCAlertInline)getChild(IALERT_COMMON);
        String detail = alert.getDetail();
        return (detail != null) && (detail.length() > 0);
    }

    // Hacks -------------------------------------------------------------------

    /**
     * Overwriting this map so that we correct the set value for
     * OpenSSO's proprietory taglib component,
     * <code>SerializedField</code>.  Without doing this, string value
     * will be set to the value of this component, this is incorrect
     * because the string value of this component is actually
     * a serialized string of the value. Doing a <code>setValue</code>
     * on this component will serialize the original twice and thus
     * this component returns incorrect value in the execution cycle.
     *
     * @param field Display field.
     * @param childValues Values of child component.
     */
    protected void mapRequestParameter(DisplayField field, Object[] childValues)
    {
        if (field instanceof SerializedField) {
            if ((childValues != null) && (childValues.length > 0)) {
                ((SerializedField)field).setSerializedString(
                    (String)childValues[0]);
            }
        } else {
            super.mapRequestParameter(field, childValues);
        }
    }


    // Abstract Methods --------------------------------------------------------

    /**
     * Returns model for this view bean.
     * Dervived class need to implement this method.
     *
     * @return model for this view bean.
     */
    protected abstract AMModel getModelInternal();

    // End of Abstract Methods ------------------------------------------------

    /**
     * Returns a option list object that contains options for a given map
     * of value to its localized string.
     *
     * @param map Map of value to its localized string.
     * @return a option list object that contains options for a given map
     *        of value to its localized string.
     */
    public OptionList createOptionList(Map map) {
        OptionList optionList = new OptionList();

        if ((map != null) && !map.isEmpty()) {
            List sorted = AMFormatUtils.sortItems(
                map.values(), getModel().getUserLocale());
            Map reversed = AMFormatUtils.reverseStringMap(map);

            for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
                String label = (String)iter.next();
                optionList.add(label, (String)reversed.get(label));
            }
        }

        return optionList;
    }

    /**
     * Returns a option list object that contains options for a given map
     * of value to its localized string.
     *
     * @param map Map of value to its localized string.
     * @return a option list object that contains options for a given map
     *        of value to its localized string.
     */
    public static OptionList createOptionList(Map map, Locale locale) {
        OptionList optionList = new OptionList();

        if ((map != null) && !map.isEmpty()) {
            Map reverseMap = AMFormatUtils.reverseStringMap(map);
            List list = AMFormatUtils.sortKeyInMap(reverseMap, locale);

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                String label = (String)iter.next();
                optionList.add(label, (String)reverseMap.get(label));
            }
        }

        return optionList;
    }

    /**
     * Returns an option list object that contains options for a given 
     * collection of string.
     *
     * @param collection Collection of strings to be included in option list.
     * @param locale Locale defining how the entries should be sorted.
     * @return a option list object that contains options for a given 
     *        collection of string.
     */
    public static OptionList createOptionList(
        Collection collection,
        Locale locale
    ) {
        return createOptionList(collection, locale, true); 
    }

    /**
     * Returns an option list object that contains options for a given
     * collection of string.
     *
     * @param collection Collection of strings to be included in option list.
     * @return a option list object that contains options for a given
     *        collection of string.
     */
    public OptionList createOptionList(Collection collection) {
        OptionList optionList = new OptionList();
        
        if ((collection != null) && !collection.isEmpty()) {
            // first sort the entries in the collection
            collection = AMFormatUtils.sortItems(
                collection, getModel().getUserLocale());

            for (Iterator iter = collection.iterator(); iter.hasNext(); ) {
                String value = (String)iter.next();
                optionList.add(value, value);
            }
        }
        
        return optionList;
    }

     /**
      * Returns an option list object that contains options for a given
      * collection of string.
      *
      * @param collection Collection of strings to be included in option list.
      * @param locale Locale defining how the entries should be sorted.
      * @param bSort <code>true</code> to sort the options.
      * @return a option list object that contains options for a given
      *        collection of string.
      */
    public static OptionList createOptionList(
        Collection collection,
        Locale locale,
        boolean bSort
    ) {        
        OptionList optionList = new OptionList();

        if ((collection != null) && !collection.isEmpty()) {
            if (bSort) {
                collection = AMFormatUtils.sortItems(collection, locale);
            }
            for (Iterator iter = collection.iterator(); iter.hasNext(); ) {
                  String value = (String)iter.next();
                  optionList.add(value, value); 
            }
        }
        return optionList; 
    }
    
    /**
     * Returns a set of string from a option list items.
     *
     * @param optList Option list that contains the items.
     * @return Set of string from a option list items.
     */
    public static Set getValues(OptionList optList) {
        Set set = null;
        if ((optList != null) && (optList.size() > 0)) {
            int sz = optList.size();
            set = new OrderedSet();

            for (int i = 0; i < sz; i++) {
                set.add(optList.getValue(i));
            }
        }
        return (set != null) ? set : Collections.EMPTY_SET;
    }

    /**
     * Returns a list of string from a option list items.
     *
     * @param optList Option list that contains the items.
     * @return list of string from a option list items.
     */
    public static List getList(OptionList optList) {
        int sz = optList.size();
        List list = new ArrayList(sz);

        for (int i = 0; i < sz; i++) {
            list.add(optList.getValue(i));
        }

        return list;
    }

    /**
     * Appends page session string to URL
     *
     * @param url Target URL
     */
    protected String appendPgSession(String url) {
        StringBuilder sb = new StringBuilder();
        sb.append(url);

        if (url.indexOf("?") == -1) {
            sb.append("?");
        } else {
            sb.append("&");
        }

        sb.append(PG_SESSION_ATTR)
          .append("=")
          .append(encodePageSessionMap());
        return sb.toString();
    }

    private String encodePageSessionMap() {
        String encoded = "";
                                                                                
        try {
            HashMap map = new HashMap(getPageSessionAttributes());
            encoded = Encoder.encodeHttp64(Encoder.serialize(map, false), 800);
        } catch (IOException e) {
            debug.error("AMViewBeanBase.encodePageSessionMap", e);
        }

        return encoded;
    }

    protected void resetButtonState(String btnName) {
        CCButton btn = (CCButton)getChild(btnName);
        btn.resetStateData();
    }

    public void disableButton(String btnName, boolean disable) {
        CCButton btn = (CCButton)getChild(btnName);
        btn.setDisabled(disable);
    }

    protected String getCharset() {
        AMModel model = getModel();
        Locale locale = model.getUserLocale();
        String agentType = model.getClientType();
        String charset = AMI18NUtils.getCharset(agentType, locale);
        return BrowserEncoding.mapHttp2JavaCharset(charset);
    }

    protected String getContentType() {
        AMModel model = getModel();
        Locale locale = model.getUserLocale();
        String agentType = model.getClientType();
        String content_type = AMI18NUtils.getContentType(agentType);
        return content_type;
    }

    private void setPageEncoding(AMModel model) {
        HttpServletResponse res = getRequestContext().getResponse();
        HttpServletRequest req = getRequestContext().getRequest();
        String content_type = getContentType();
        String charset = getCharset();
        String jCharset = BrowserEncoding.mapHttp2JavaCharset(charset);
        res.setContentType(content_type+";charset="+jCharset);
        CCI18N.initContentType(req, res);
    }

    /**
     * Does nothing, just forwards to the same view bean
     *
     * @param event - request invocation event
     */
    public void handleGRequest(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Reset the view bean.
     */
    public void resetView() {
    }

    protected void bypassForwardTo(RequestContext rc) {
        super.forwardTo(rc);
    }

    /**
     * Set location DN
     *
     * @param dn
     */
    public void setCurrentLocation(String dn) {
        if (dn != null) {
            setPageSessionAttribute(
                AMAdminConstants.CONSOLE_LOCATION_DN, dn);
        }
    }

    /**
     * Gets the current location DN.
     *
     * @return location DN
     */
    public String getCurrentLocationDN() {
        return (String)getPageSessionAttribute(
            AMAdminConstants.CONSOLE_LOCATION_DN);
    }

    /**
     * Gets the name of the current location.
     *
     * @return name of location
     */
    public String getCurrentLocation() {
        String tmp = (String)getPageSessionAttribute(
            AMAdminConstants.CONSOLE_LOCATION_DN);

        if (tmp != null && tmp.length() > 0) {
            int x = tmp.lastIndexOf("/");
            tmp = tmp.substring(x+1);
        }

        return tmp;
    }

    /**
     * Hides the selection checkboxes in table.
     *
     * @param tableName Name of table.
     */
    public void hideTableSelectionIcons(String tableName) {
        CCActionTable table = (CCActionTable)getChild(tableName);
        CCActionTableModelInterface model = table.getModel();
        model.setSelectionType(CCActionTableModelInterface.NONE);
    }

    /**
     * Returns a set of static text field' names that should be blanked.
     * This field is a a action table as a label for a hyperlink. Blanking it
     * out will hide the hyperlink accordingly.
     *
     * @return Set of static text field' names that should be blanked.
     */
    public Set getBlankTextFields() {
        return blankTextFields;
    }

    /**
     * Includes a static text field that should be blanked. This field
     * is a a action table as a label for a hyperlink. Blanking it out
     * will hide the hyperlink accordingly.
     *
     * @param name Name of static text field.
     */
    public void addBlankTextField(String name) {
        blankTextFields.add(name);
    }



    /* Delegation *************************************************************/
    protected void performDelegationTasks() {
        DelegationConfig config = DelegationConfig.getInstance();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        config.configureButtonsAndTables(realmName,
            getServiceNameForAccessControl(), getModel(), this);
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        boolean display = false;
        Method method = getBeginDisplayMethod(event.getChildName());

        if (method != null) {
            Object[] param = {event};
            try {
                Boolean results = (Boolean)method.invoke(this, param);
                display = results.booleanValue();
            } catch (IllegalAccessException e) {
                debug.warning("AMViewBeanBase.beginChildDisplay", e);
            } catch (InvocationTargetException e) {
                debug.warning("AMViewBeanBase.beginChildDisplay", e);
            }
        } else {
            super.endDisplay(event);
            display = !blankTextFields.contains(event.getChildName());
        }

        return display;
    }

    private Method getBeginDisplayMethod(String childName) {
        char nameChars[] = childName.toCharArray();
        nameChars[0] = Character.toUpperCase(nameChars[0]);
        String methodName = "begin" + new String(nameChars) + "Display";
        Class[] param = {ChildDisplayEvent.class};
        Method method = null;

        try {
            method = getClass().getMethod(methodName, param);
        } catch (NoSuchMethodException e) {
            // Do nothing. OK to not find a begin display method for the child.
        } catch (SecurityException e) {
            // Do nothing. OK to not find a begin display method for the child.
        }

        return method;
    }

    protected String getServiceNameForAccessControl() {
        return null;
    }

    public AMViewBeanBase getCallingView() {
        String returnVB = (String)removePageSessionAttribute(
            AMAdminConstants.SAVE_VB_NAME);

        if (returnVB == null) {
            return null;
        } else {
            try {
                Class clazz = Class.forName(returnVB);
                AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
                return vb;
            } catch (ClassNotFoundException cnfe) {
                return null;
            }
        }
    }

    public Locale getUserLocale() {
        AMModel model = getModel();
        return (model != null) ? model.getUserLocale() : java.util.Locale.US;
    }

    public String getPageSessionAttributeString() {
        String strAttr;
        if (pageSessionInSessionStore) {
            pageSessionInSessionStore = false;
            strAttr = super.getPageSessionAttributeString();
            pageSessionInSessionStore = true;
        } else {
            strAttr = super.getPageSessionAttributeString();
            if (strAttr.length()>=MAX_PG_SESSION_SIZE) {
                SSOToken ssoToken = getModel().getUserSSOToken();
                String ssoTokenID = ssoToken.getTokenID().toString();
                Map store = SessionStore.getSessionStore(ssoToken);
                Map attributes = getPageSessionAttributes();
                store.put(vbUID, attributes);

                /* 
                 * We are storing the SSOToken ID because it is needed to 
                 * retrieve the page session map later on. We cannot call
                 * getModel().getUserSSOToken() because the model may depend on
                 * the page session map for initialization. This avoids getting 
                 * into a chicken or the egg problem.
                 */                
                Map tmp = new HashMap(4);
                tmp.put(PG_SESSION_ATTR_ID, vbUID);
                tmp.put(PG_SESSION_SSO_ID, ssoTokenID);
                super.setPageSessionAttributes(tmp);
                
                strAttr = super.getPageSessionAttributeString();
                pageSessionInSessionStore = true;
            }
        }
        return strAttr;
    }

    protected void deserializePageAttributes() {
        super.deserializePageAttributes();
        pageSessionInSessionStore = false;
        String ssoid = (String)super.getPageSessionAttribute(PG_SESSION_SSO_ID);
        if (ssoid != null) {
            Map store = SessionStore.getSessionStore(ssoid);

            // store can be null, if sso token has expired.
            if (store != null) {
                Map savedAttr = (Map)store.get(
                    (String)super.getPageSessionAttribute(PG_SESSION_ATTR_ID));
                super.setPageSessionAttributes(savedAttr);
            }
        }
    }

    protected void setPageSessionAttributes(Map value) {
        if (pageSessionInSessionStore) {
            String ssoTokenID = getModel().getUserSSOToken()
                .getTokenID().toString();
            Map store = SessionStore.getSessionStore(ssoTokenID);
            store.put(vbUID, value);
            super.getPageSessionAttributes().put(PG_SESSION_ATTR_ID, vbUID);
        } else {
            super.setPageSessionAttributes(value);
        }
    }

    protected Map getPageSessionAttributes() {
        Map attributes;
        if (pageSessionInSessionStore) {
            String ssoTokenID = getModel().getUserSSOToken()
                .getTokenID().toString();
            Map store = SessionStore.getSessionStore(ssoTokenID);
            attributes = (Map)store.get(vbUID);
        } else {
            attributes = super.getPageSessionAttributes();
        }
        return attributes;
    }

    protected void redirectToStartURL() {
        AMAdminFrameViewBean vb = (AMAdminFrameViewBean)getViewBean(
            AMAdminFrameViewBean.class);
        vb.forwardTo(getRequestContext());
    }

    /**
     * The Page Session Store will be created if a ViewBean's page session
     * size exceeds MAX_PG_SESSION_SIZE, which is currently set to little
     * less than 2KB.
     */
    private static class SessionStore {
        static private Map stores = new HashMap();

        static private SSOTokenListener listener = new SSOTokenListener() {
            public void ssoTokenChanged(SSOTokenEvent event) {
                try {
                    switch(event.getType())  {
                        case SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT:
                        case SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT:
                        case SSOTokenEvent.SSO_TOKEN_DESTROY:
                            SSOToken token = event.getToken();
                            stores.remove(token.getTokenID().toString());
                    }
                } catch(SSOException ssoe) {
                    debug.warning("SessionStore.ssoTokenChanged", ssoe);
                }
            }
        };

        static Map getSessionStore(SSOToken ssoToken) {
            String storeKey = ssoToken.getTokenID().toString();
            Map store =  getSessionStore(storeKey);
            if (store == null) {
                store = new HashMap();
                stores.put(storeKey, store);
                try {
                    ssoToken.addSSOTokenListener(listener);
                } catch(SSOException ssoe) {
                    debug.warning("SessionStore.getSessionStore", ssoe);
                }
            }
            return store;
        }

        static Map getSessionStore(String storeKey) {
            return (Map)stores.get(storeKey);
        }
    }

    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales. If the label passed in cannot be found it will
     * use the default value of "previous view".
     */
    protected String getBackButtonLabel(String label) {
        String localizedLabel = getModel().getLocalizedString(label);
        if (localizedLabel.equals(label)) {
            localizedLabel = getModel().getLocalizedString("previous.view");
        }
        String[] arg = { localizedLabel };
        return MessageFormat.format(
            getModel().getLocalizedString("back.button"), (Object[])arg);
    }

    protected String getCurrentRealm() {
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        if (curRealm == null) {
            curRealm = AMModelBase.getStartDN(getRequestContext().getRequest());
            setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, curRealm);
        }
        return curRealm;
    }

    // this builds a display string only
    public String getPath(String child) {
        AMModel model = getModel();
        StringBuilder path = new StringBuilder();
        String startDN = model.getStartDN();
        if (startDN.charAt(0) != '/') {
            startDN = "/" + startDN;
        }
        if (child.charAt(0) != '/') {
            child = "/" + child;
        }
        path.append(startDN);

        if (!child.equals(startDN)) {
            int idx = child.indexOf(startDN);
            String subRealm = (idx == 0) ?
                child.substring(startDN.length()) : child;

            StringTokenizer st = new StringTokenizer(subRealm, "/");
            while (st.hasMoreTokens()) {
                path.append(" > ")
                    .append(SMSSchema.unescapeName(st.nextToken()));
            }
        }

        return path.toString();
    }

    protected String getRequestURL() {
        HttpServletRequest req = this.getRequestContext().getRequest();
        String uri = req.getRequestURI().toString();
        int idx = uri.indexOf('/', 1);
        uri = uri.substring(0, idx);
        return req.getScheme() + "://" + req.getServerName() +
            ":" + req.getServerPort() + uri;
    }
    
    public static String stringToHex(String str) {
        StringBuilder buff = new StringBuilder();
        str = str.replaceAll("\\\\u", "\\\\\\\\u");
        int len = str.length();
        for (int i = 0; i < len; i++) {
            buff.append(charToHex(str.charAt(i)));
        }
        return buff.toString();
    }
    
    public static String charToHex(char c) {
        StringBuilder buffer = new StringBuilder();
        if (c <= 0x7E) {
            buffer.append(c);
        } else {
            buffer.append("\\u");
            String hex = Integer.toHexString(c);
            for (int j = hex.length(); j < 4; j++ ) {
                buffer.append('0');
            }
            buffer.append(hex);
        }
        return buffer.toString();
    }
    
    public static String hexToString(String str) {
        StringBuilder buff = new StringBuilder();
        int idx = str.indexOf("\\u");
        while (idx != -1) {
            boolean done = false;
            if (idx > 0) {
                if (str.charAt(idx -1) == '\\') {
                    buff.append(str.substring(0, idx-1))
                        .append(str.substring(idx, idx+2));
                    str = str.substring(idx+2);
                    done = true;
                }
            }

            if (!done) {
                buff.append(str.substring(0, idx))
                    .append(
                    (char)Integer.parseInt(str.substring(idx+2, idx+6), 16));
                str = str.substring(idx+6);
            }
            idx = str.indexOf("\\u");
        }

        buff.append(str);
        return buff.toString();
    }
}
