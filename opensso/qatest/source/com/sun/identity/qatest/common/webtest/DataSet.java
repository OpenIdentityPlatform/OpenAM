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
 * $Id: DataSet.java,v 1.2 2007/12/19 16:28:40 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.webtest;

import com.sun.identity.shared.xml.XMLHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class DataSet {
    private static final String FORM_ELT = "form";
    private static final String FORM_NAME_ATTR = "name";
    private static final String BUTTON_NAME_ATTR = "buttonName";
    private static final String INPUT_ELT = "input";
    private static final String TEXTAREA_ELT = "textarea";
    private static final String CHECKBOX_ELT = "checkbox";
    private static final String DYN_INPUT_ELT = "dynamicinput";
    private static final String INPUT_NAME_ATTR = "name";
    private static final String INPUT_VALUE_ATTR = "value";
    private static final String INPUT_VALUES_ATTR = "values";
    private static final String RESULT_ELT = "result";
    private static final String TEXT_ATTR = "text";
    private static final String ANCHOR_PATTERN_ATTR = "anchorpattern";
    private static final String URL_ATTR = "url";
    
    private String fileName;
    private String kickoffURL;
    private List<DataEntry> entries = new ArrayList<DataEntry>();
    
    public DataSet(String xml)
        throws Exception {
        this.fileName = xml;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        DocumentBuilder  builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ValidationErrorHandler());
        builder.setEntityResolver(new XMLHandler());
        Document doc = builder.parse(new File(xml));
        
        Element topElement = doc.getDocumentElement();
        kickoffURL = topElement.getAttribute("href");
        getEntries(topElement);
    }
    
    public List<DataEntry> getEntries() {
        return entries;
    }
    
    public String getKickOffURL() {
        return kickoffURL;
    }
    
    private void getEntries(Element root) {
        NodeList childElements = root.getElementsByTagName(FORM_ELT);
        int len = childElements.getLength();
        
        for (int i = 0; i < len; i++) {
            Element elt = (Element)childElements.item(i);
            String anchor = elt.getAttribute(ANCHOR_PATTERN_ATTR);
            
            if ((anchor != null) && (anchor.trim().length() > 0)) {
                entries.add(new DataEntry(anchor));
            } else {
                String url = elt.getAttribute(URL_ATTR);
                if ((url != null) && (url.trim().length() > 0)) {
                    try {
                        entries.add(new DataEntry(new URL(url)));
                    } catch (MalformedURLException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    String formName = elt.getAttribute(FORM_NAME_ATTR);

                    if ((formName != null) && (formName.trim().length() > 0)) {
                        String btnName = elt.getAttribute(BUTTON_NAME_ATTR);
                        Map<String, String> inputs = getInputElements(elt);
                        Map<String, String> textareas = getTextAreaElements(
                            elt);
                        Map<String, String> checkboxes = getCheckboxElements(
                            elt);
                        Map<String, String[]> dynInputs =
                            getDynamicInputElements(elt);

                        NodeList resultNodes = elt.getElementsByTagName(
                            RESULT_ELT);
                        String expectedResult = (resultNodes.getLength() > 0) ?
                            ((Element)resultNodes.item(0)).getAttribute(
                            TEXT_ATTR) : null;
                        entries.add(new DataEntry(formName, btnName, inputs,
                            textareas, checkboxes, dynInputs, expectedResult));
                    } else {
                        // this node is required.
                        NodeList resultNodes = 
                                elt.getElementsByTagName(RESULT_ELT);
                        String expectedResult = ((Element)resultNodes.item(0))
                            .getAttribute(TEXT_ATTR);
                        DataEntry e = new DataEntry();
                        e.setExpectedResult(expectedResult);
                        entries.add(e);
                    }
                }
            }
        }
    }
    
    private static Map<String, String> getInputElements(Element elt) {
        Map<String, String> inputs = new HashMap<String, String>();
        NodeList inputNodes = elt.getElementsByTagName(INPUT_ELT);
        int len = inputNodes.getLength();
        for (int i = 0; i < len; i++) {
            Element inputElm = (Element)inputNodes.item(i);
            inputs.put(inputElm.getAttribute(INPUT_NAME_ATTR),
                inputElm.getAttribute(INPUT_VALUE_ATTR));
        }
        return inputs;
    }
    
    private static Map<String, String> getTextAreaElements(Element elt) {
        Map<String, String> textareas = new HashMap<String, String>();
        NodeList inputNodes = elt.getElementsByTagName(TEXTAREA_ELT);
        int len = inputNodes.getLength();
        for (int i = 0; i < len; i++) {
            Element inputElm = (Element)inputNodes.item(i);
            textareas.put(inputElm.getAttribute(INPUT_NAME_ATTR),
                inputElm.getAttribute(INPUT_VALUE_ATTR));
        }
        return textareas;
    }
    
    private static Map<String, String> getCheckboxElements(Element elt) {
        Map<String, String> checkboxes = new HashMap<String, String>();
        NodeList inputNodes = elt.getElementsByTagName(CHECKBOX_ELT);
        int len = inputNodes.getLength();
        for (int i = 0; i < len; i++) {
            Element inputElm = (Element)inputNodes.item(i);
            checkboxes.put(inputElm.getAttribute(INPUT_NAME_ATTR),
                inputElm.getAttribute(INPUT_VALUE_ATTR));
        }
        return checkboxes;
    }
    
    private static Map<String, String[]> getDynamicInputElements(
        Element elt
        ) {
        Map<String, String[]> inputs = new HashMap<String, String[]>();
        NodeList inputNodes = elt.getElementsByTagName(DYN_INPUT_ELT);
        int len = inputNodes.getLength();
        for (int i = 0; i < len; i++) {
            Element inputElm = (Element)inputNodes.item(i);
            String name = inputElm.getAttribute(INPUT_NAME_ATTR);

            // The constant should be set to INPUT_VALUES_ATTR but
            // for some reason webtest is unable to retrive values
            // if its set for multiple values. Need to debug this 
            // more and fix it later. For now this will ONLY work
            // with list having single strings as list members.
            String values = inputElm.getAttribute(INPUT_VALUE_ATTR);
            StringTokenizer st = new StringTokenizer(values, "|");
            String[] arrayVal = new String[st.countTokens()];
            int counter = 0;
            while (st.hasMoreTokens()) {
                arrayVal[counter++] = st.nextToken();
            }
            inputs.put(name, arrayVal);
        }
        return inputs;
    }
    
    class ValidationErrorHandler implements ErrorHandler {
        
        // ignore fatal errors(an exception is guaranteed)
        public void fatalError(SAXParseException spe)
        throws SAXParseException {
            System.err.println(fileName + ": " + spe.getMessage() +
                "\nLine Number in XML file : " + spe.getLineNumber() +
                "\nColumn Number in XML file : " + spe.getColumnNumber());
            throw spe;
        }
        
        //treat validation errors also as fatal error
        public void error(SAXParseException spe)
        throws SAXParseException {
            System.err.println(fileName + ": " + spe.getMessage() +
                "\nLine Number in XML file : " + spe.getLineNumber() +
                "\nColumn Number in XML file : " + spe.getColumnNumber());
            throw spe;
        }
        
        // dump warnings too
        public void warning(SAXParseException err)
        throws SAXParseException {
            System.err.println(fileName + ": " + err.getMessage() +
                "\nLine Number in XML file : " + err.getLineNumber() +
                "\nColumn Number in XML file : " + err.getColumnNumber());
        }
    }
}
