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
 * $Id: DataEntry.java,v 1.4 2008/06/26 20:05:14 rmisra Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.webtest;

import com.gargoylesoftware.htmlunit.Page;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DataEntry extends TestCommon {
    private String visitURLPattern;
    private URL url;
    private String formName;
    private String btnName;
    private String expectedResult;
    private Map<String, String> inputs = new HashMap<String, String>();
    private Map<String, String> textareas = new HashMap<String, String>();
    private Map<String, String> checkboxes = new HashMap<String, String>();
    private Map<String, String[]> dynamicInputs =
        new HashMap<String, String[]>();
    
    DataEntry() {
        super("DataEntry");
    }

    DataEntry(URL url) {
        super("DataEntry");
        this.url = url;
    }
    
    DataEntry(String url) {
        super("DataEntry");
        visitURLPattern = url;
    }
    
    DataEntry(
        String formName,
        String btnName,
        Map<String, String> inputs,
        Map<String, String> textareas,
        Map<String, String> checkboxes,
        Map<String, String[]> dynamicInputs,
        String expectedResult
    ) {
        super("DataEntry");
        this.formName = formName;
        this.btnName = btnName;
        this.inputs.putAll(inputs);
        this.textareas.putAll(textareas);
        this.checkboxes.putAll(checkboxes);
        this.dynamicInputs.putAll(dynamicInputs);
        this.expectedResult = expectedResult;
    }

    /**
     * Returns form name.
     *
     * @return form name.
     */
    public String getFormName() {
        return formName;
    }
    
    /**
     * Returns the URL to visit.
     *
     * @return the URL to visit.
     */
    public String getVisitURLPattern() {
        return visitURLPattern;
    }

    /**
     * Returns the URL to visit.
     *
     * @return the URL to visit.
     */
    public URL getVisitURL() {
        return url;
    }
    /**
     * Returns submit button name.
     *
     * @return submit button name.
     */
    public String getButtonName() {
        return btnName;
    }

    /**
     * Sets expected result.
     *
     * @param result Expected result.
     */
    public void setExpectedResult(String result) {
        expectedResult = result;
    }

    /**
     * Validate if the web response is correct.
     */
    public void validate(Page page) {
        if (expectedResult != null) {
            String content = page.getWebResponse().getContentAsString();
            if (content.indexOf(expectedResult) == -1) {
                log(Level.SEVERE, "validate", "The expected result did NOT" +
                        " match with the output");
                log(Level.SEVERE, "validate", "The expected result to match" +
                        " is: " + expectedResult);
                log(Level.SEVERE, "validate", "The actual html output is: \n" +
                        content);
            }
            assert(content.indexOf(expectedResult) != -1);
        }
    }
 
    /**
     * Returns form checkbox values.
     *
     * @return form checkbox values.
     */
    public Map<String, String> getFormCheckboxValues() {
        return checkboxes;
    }

    /**
     * Returns form input values.
     *
     * @return form input values.
     */
    public Map<String, String> getFormInputValues() {
        return inputs;
    }
    
    /**
     * Returns form textarea values.
     *
     * @return form textarea values.
     */
    public Map<String, String> getFormTextAreaValues() {
        return textareas;
    }

    /**
     * Returns form dynamic input values.
     *
     * @return form dynamic input values.
     */
    public Map<String, String[]> getFormDynamicInputValues() {
        return dynamicInputs;
    }
}