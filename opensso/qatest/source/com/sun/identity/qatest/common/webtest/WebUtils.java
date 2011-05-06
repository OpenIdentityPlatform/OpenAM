/* The contents of this file are subject to the terms
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
 * $Id: WebUtils.java,v 1.3 2009/01/31 00:38:24 mrudulahg Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.common.webtest;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.sun.identity.qatest.common.TestCommon;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class for web interaction.
 */
public class WebUtils extends TestCommon {
    static String XML_DIR = "built/classes";
    private static final String NULL_FORMNAME = "_none_";
    
    /**
     * Creates a new instance of <code>WebUtils</code>
     */
    private WebUtils() {
        super("WebUtils");
    }
    
    public static Page postForm(
        WebClient webClient,
        HtmlPage page,
        DataEntry data
    ) throws Exception {
        String formName = data.getFormName();
        if ((formName != null) && (formName.trim().length() > 0)) {
            return submitForm(page, data, formName);
        }
        
        String pattern = data.getVisitURLPattern();
        if ((pattern != null) && (pattern.trim().length() > 0)) {
            return visitURL(page, pattern);
        }
        
        URL url = data.getVisitURL();
        if (url != null) {
            return (HtmlPage)webClient.getPage(url);
        }
        
        data.validate(page);
        return null;
    }
    
    private static Page visitURL(HtmlPage page, String urlPattern)
        throws Exception {
        HtmlAnchor selected = null;
        List anchors = page.getAnchors();
        for (Iterator i = anchors.iterator(); i.hasNext(); ) {
            HtmlAnchor a = (HtmlAnchor)i.next();
            if (a.getHrefAttribute().indexOf(urlPattern) != -1) {
                selected = a;
                break;
            }
        }
        
        if (selected == null) {
            throw new Exception("Incorrect input XML, no action target.");
        }
        
        return selected.click();
    }

    private static Page submitForm(
        HtmlPage page,
        DataEntry data,
        String formName
    ) throws IOException, ElementNotFoundException {
        try {
        HtmlForm form;
        if (formName.equals(NULL_FORMNAME)) {
            form = (HtmlForm)page.getForms().get(0);
        } else {
            form = page.getFormByName(formName);
        }
        
        Map<String, String> inputs = data.getFormInputValues();
        for (String key : inputs.keySet()) {
            HtmlInput htmlInput = form.getInputByName(key);
            htmlInput.setValueAttribute(inputs.get(key));
        }

        Map<String, String> textareas = data.getFormTextAreaValues();
        for (String key : textareas.keySet()) {
            List list= form.getTextAreasByName(key);
            ((HtmlTextArea)list.get(0)).setText(textareas.get(key));
        }

        
        Map<String, String> checkboxes = data.getFormCheckboxValues();
        for (String key : checkboxes.keySet()) {
            HtmlCheckBoxInput cb = (HtmlCheckBoxInput)form.getInputByName(key);
            cb.setChecked(checkboxes.get(key).equals("true"));
        }
        
        Map<String, String[]> dInputs = data.getFormDynamicInputValues();
        for (String key : dInputs.keySet()) {
            HtmlSelect htmlSelect = form.getSelectByName(key);
            for (String attribute : dInputs.get(key)) {
                htmlSelect.setSelectedAttribute(attribute, true);
            }
        }

        Page result = null;
        String btnName = data.getButtonName();
        if (btnName.trim().length() == 0) {
            result = form.getInputByName("Submit").click();
        } else {
            if (btnName.equals("IDButton")){
                ScriptResult scriptResult = page.executeJavaScript("document.forms['Login'].submit();");
                result = scriptResult.getNewPage();
            } else {
                HtmlInput btn = form.getInputByName(btnName);
                result = btn.click();
            }
        }

        data.validate(result);
        return result;
        } catch (ElementNotFoundException e) {
            log(Level.SEVERE, "submitForm", "Got Exception while working on " +
                    "page : " + page.getWebResponse().getContentAsString());
            throw e;
        } catch (IOException e) {
            log(Level.SEVERE, "submitForm", "Got Exception while working on " +
                    "page : " + page.getWebResponse().getContentAsString());
            throw e;
        }
    }
    
    public static String getDataPropertiesFileName(Object obj) {
        String className = obj.getClass().getName();
        int index = className.lastIndexOf('.');
        if (index != -1) {
            className = className.substring(index+1);
        }
        return XML_DIR + "/" + className.toLowerCase() + ".xml";
    }
}
