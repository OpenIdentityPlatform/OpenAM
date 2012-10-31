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
 * $Id: ImportEntityViewBean.java,v 1.7 2009/08/21 20:09:23 veiming Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.ImportEntityModel;
import com.sun.identity.console.federation.model.ImportEntityModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;

public class ImportEntityViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/ImportEntity.jsp";
    
    private AMPropertySheetModel psModel;
    private CCPageTitleModel ptModel;
    private static final String PROPERTIES = "propertyAttributes";
    private static final String RADIO_META = "radioMeta";
    private static final String RADIO_EXTENDED = "radioExtended";


    /**
     * Creates a authentication domains view bean.
     */
    public ImportEntityViewBean() {
        super("ImportEntity");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);                   
        createPageTitleModel();
        createPropertyModel();
        registerChildren();    
    }

    protected void registerChildren() {       
        ptModel.registerChildren(this);
        registerChild(PROPERTIES, AMPropertySheet.class);
        psModel.registerChildren(this);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals("pgtitle")) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTIES)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if ((psModel != null) && psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
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
        populateRealmData();

        String value = (String)getDisplayFieldValue(RADIO_META);
        if ((value == null) || value.equals("")){
            setDisplayFieldValue(RADIO_META, "url");
        }

        value = (String)getDisplayFieldValue(RADIO_EXTENDED);
        if ((value == null) || value.equals("")){
            setDisplayFieldValue(RADIO_EXTENDED, "url");
        }

    }    
    
    private void populateRealmData() {
         Set realmNames = Collections.EMPTY_SET;
         ImportEntityModel model = (ImportEntityModel)getModel();
         try{
             realmNames = model.getRealmNames("/", "*");         
             CCDropDownMenu menu =
                 (CCDropDownMenu)getChild(ImportEntityModel.REALM_NAME);
             OptionList sortedList = createOptionList(realmNames);
             OptionList optList =  new OptionList();
             int size = sortedList.size();
             for (int i = 0; i < size; i++) {
                 String name = sortedList.getValue(i);
                 optList.add(getPath(name), name);
             }
             menu.setOptions(optList);
         } catch (AMConsoleException e) {             
             debug.warning("ImportEntityViewBean.populateRealmData ",  
                 e);
             setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                 "import.entity.populaterealmdata.error");
         }
     }
     
    private void createPropertyModel() {
        psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/importEntityPropertySheet.xml"));
        psModel.clear();
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
        HttpServletRequest req = rc.getRequest();
        return new ImportEntityModelImpl(req, getPageSessionAttributes());
    }

    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        int idx = html.indexOf("tfMetadataFile\"");
        idx = html.lastIndexOf("<input ", idx);
        html = html.substring(0, idx) +
            "<span id=\"metadatafilename\"></span>" +
            html.substring(idx);

        idx = html.indexOf("tfExtendeddataFile\"");
        idx = html.lastIndexOf("<input ", idx);
        html = html.substring(0, idx) +
            "<span id=\"extendeddatafilename\"></span>" +
            html.substring(idx);
        return html;
    }

    /**
     * Handles upload entity button request. There are two fields on this page:
     * one for standard metadata and the other for extended. The standard is
     * required.
     *
     * @param event Request invocation event
     * @throws ModelControlException
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        ImportEntityModel model = (ImportEntityModel)getModel();
        Map data = new HashMap(6);
              
        String realm = (String)getDisplayFieldValue(model.REALM_NAME);
        data.put(model.REALM_NAME, realm);
        
        String radioMeta = (String)getDisplayFieldValue("radioMeta");
        String meta = (radioMeta.equals("url")) ? 
            (String)getDisplayFieldValue("tfMetadataFileURL") :
            (String)getDisplayFieldValue("tfMetadataFile");
        
        String radioExtended = (String)getDisplayFieldValue("radioExtended");
        String extended = (radioExtended.equals("url")) ? 
            (String)getDisplayFieldValue("tfExtendeddataFileURL") :
            (String)getDisplayFieldValue("tfExtendeddataFile");
        
        if ((meta == null) || (meta.length() == 0)) {
            psModel.setErrorProperty("standardFileNameProperty", true);            
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.input.error",
                "import.entity.missing.metadata");
            forwardTo();
        } else {
            data.put(ImportEntityModel.STANDARD_META, meta);
            if ((extended != null) || (extended.trim().length() > 0)) {
                data.put(ImportEntityModel.EXTENDED_META, extended);
            }            
            
            try {
                model.importEntity(data);
               
                StringBuilder buff = new StringBuilder();

                // build the success message.
                // don't need the realm name in the message so remove it first.
                data.remove(ImportEntityModel.REALM_NAME);                
                for (Iterator i = data.keySet().iterator(); i.hasNext();) {
                    String key = (String)i.next();  
                    String value = (String)data.get(key);

                    if ((value != null) && (value.length() > 0)) {
                        String val = (String)data.get(key);
                        if (val.startsWith("http")) {
                            if (buff.length() > 0) {
                                buff.append(", ");
                            }
                            buff.append(val);
                        } else {
                            int idx = val.lastIndexOf("<!-- ");
                            if (idx != -1) {
                                int idx1 = val.lastIndexOf(" -->");
                                if (idx1 != -1) {
                                    val = val.substring(idx+5, idx1);
                                }
                            }
                            if (buff.length() > 0) {
                                buff.append(", ");
                            }
                            buff.append(val);
                        }
                    }
                }

                String message = "";

                if (buff.length() > 0) {
                    Object[] params = {buff.toString()};
                    message = MessageFormat.format(model.getLocalizedString(
                        "import.entity.metadata.success"), params);
                }
                
                // set the message in the main view
                setPageSessionAttribute(
                    FederationViewBean.MESSAGE_TEXT, message);                
                FederationViewBean vb = 
                    (FederationViewBean)getViewBean(FederationViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext()); 
                    
            } catch (AMConsoleException ame) {
                debug.warning("ImportEntityViewBean.handleButton1req ", ame);             
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                 "import.entity.error");
                forwardTo();
            }
        }
    }
    
    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        FederationViewBean vb = 
            (FederationViewBean)getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());       
    }
}
