/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PPInteractionHelper.java,v 1.2 2008/06/25 05:47:14 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.idpp;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.math.BigInteger;
import java.util.MissingResourceException;
import com.sun.identity.liberty.ws.idpp.plugin.AttributeMapper;
import com.sun.identity.liberty.ws.idpp.common.*;

/**
 * The class <code>PPInteractionHelper</code> is a helper class for the
 * Personal Profile Service for creating questions for Interaction for value 
 * and interaction for consent.
 * The question key will always be of the following form in a properties file.
 * Question: <PPAttribute>_Value_Question for interact for value
 * Question: <PPAttribute>_Consent_Question for interfact for consent
 * For e.g. CN_Value_Question or CN_Consent_Question.
 * There will be a default question key for Consent if there's not one for the
 * rquested attribute., but it's not true for interact for the value.
 */

public class PPInteractionHelper {
  
    private ResourceBundle props = null;
    private static final String TEXT_MIN_CHARS = "textMinChars";
    private static final String TEXT_MAX_CHARS = " textMaxChars";
    private static String defaultMinChars = null; 
    private static String defaultMaxChars =  null;
    private static String idppProps = "libPersonalProfile";


    /**
     * Constructor
     * @param lang Language for the properties file.
     */
    public PPInteractionHelper(String lang) {
        if(lang != null) {
            props = ResourceBundle.getBundle(idppProps,
                com.sun.identity.shared.locale.Locale.
                getLocaleObjFromAcceptLangHeader(lang));
        } else {
            props = IDPPUtils.bundle;
        }
        try {
            defaultMinChars = props.getString("defaultMinTextChars");
            defaultMaxChars = props.getString("defaultMaxTextChars");
        } catch (MissingResourceException mre) {
            IDPPUtils.debug.error("PPInteractHelper.Static: Could not find min"+
                " or maximum text characters.", mre);
        }
    }

    /**
     * Gets the inquiry question for interaction for consent. 
     * common question if it does'nt have one.
     * @param isQuery true if this is a <code>PP</code> query request,
     *                false if this is a <code>PP</code> modify request.
     * @param ppElement leaf element in <code>PP</code> select expresssion.
     * @return String inquiry question.
     */
    public String getInteractForConsentQuestion(boolean isQuery, 
        String ppElement) {
        try {
            if (isQuery) {
                return  props.getString(ppElement + "_" + 
                        IDPPConstants.QUERY_TYPE + "_" + 
                        IDPPConstants.CONSENT + "_" + IDPPConstants.QUESTION);
            } else {
                return  props.getString(ppElement + "_" + 
                        IDPPConstants.MODIFY_TYPE + "_" + 
                        IDPPConstants.CONSENT + "_" + IDPPConstants.QUESTION);
            }
        } catch(MissingResourceException mre) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PPRequestHandler.getInquiryQuestion:"+
               "can not find question for:" + ppElement);
            }
        }

        if (isQuery) {
            return props.getString(IDPPConstants.COMMON_QUERY_CONSENT_QUESTION);
        } else {
            return 
                props.getString(IDPPConstants.COMMON_MODIFY_CONSENT_QUESTION);
        }
    }

    /**
     * Gets Interact For value questions for an idpp element. There will be
     * mutiple questions for a non leaf element.
     * @param isQuery true if this is a <code>PP</code> query request,
     *                false if this is a <code>PP</code> modify request.
     * @param ppElement leaf element in <code>PP</code> select expression.
     * @return Map map of <code>PP</code> element and question.
     *             key is the ppelement for e.g. CN
     *             value is the question for e.g. "What's the CN value?";
     */ 
    public Map getInteractForValueQuestions(boolean isQuery, String ppElement) {

        Map queries = new HashMap();
        Set ppElements = getPPLeafElements(ppElement);
        if(ppElements == null || ppElements.size() == 0) {
           return queries;
        }

        if(ppElements.size() == 1) {
           String question = getInteractForValueQuestion(isQuery, ppElement);
           if(question == null) {
              return queries;
           } 
           queries.put(ppElement, question);
           return queries;
        }

        Iterator iter = ppElements.iterator();
        while(iter.hasNext()) {
           String element = (String)iter.next();
           String question = getInteractForValueQuestion(isQuery, element);
           if(question != null) {
              queries.put(element, question);
           }
        }
        
        return queries;
    }

    /**
     * Gets the Personal Profile DS Attribute from the attribute mapper.
     * @param key PP Attribute
     * @return String DS Attribute for the respective PP Attribute
     */ 
    public String getPPAttribute(String key) {
        AttributeMapper mapper = IDPPServiceManager.getInstance().
                        getAttributeMapper();
        return mapper.getDSAttribute(key);
    }

    /**
     * Gets the text minimum characters
     * @param resource a resource that's looking for minimum text chars.
     * @return BigInteger text minimum characters in BigInteger format
     * @exception NumberFormatException if it can not parse the number.
     */
    public BigInteger getTextMinChars(String resource) 
    throws NumberFormatException {
        String minChars = null;
        try {
            minChars = props.getString(resource + "_" + TEXT_MIN_CHARS);
        } catch (MissingResourceException mre) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PPInteractionHelper.getTextMinChars:"+
               "Could not find min chars for " + resource);
            }
            minChars = defaultMinChars;
        }
        return new BigInteger(minChars);
    }

    /**
     * Gets the text maxmimum characters for a given attribute.
     * @param resource an attribute that's looking for the max text chars.
     * @return BigInteger text maximum characters in BigInteger format
     * @exception NumberFormatException if it can not parse the number.
     */
    public BigInteger getTextMaxChars(String resource) 
    throws NumberFormatException {
        String maxChars = null;
        try {
            maxChars = props.getString(resource + "_" + TEXT_MAX_CHARS);
        } catch (MissingResourceException mre) {
            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("PPInteractionHelper.getTextMinChars:"+
               "Could not find min chars for " + resource);
            }
            maxChars = defaultMaxChars;
        }
        return new BigInteger(maxChars);
    }

    /**
     * Gets the interact for value for a leaf element
     * @param isQuery true if this is a <code>PP</code> query request,
     *                false if this is a <code>PP</code> modify request.
     * @param ppElement leaf element in <code>PP</code> select expression.
     * @return String question for the pp attribute element,
     *                null if does not find one.
     */
    private String getInteractForValueQuestion(boolean isQuery, 
        String ppElement) {
        try {
            if (isQuery) {
                return props.getString( ppElement + "_" + 
                    IDPPConstants.QUERY_TYPE + "_" +
                    IDPPConstants.VALUE + "_" + IDPPConstants.QUESTION);
            } else {
                return props.getString( ppElement + "_" + 
                    IDPPConstants.MODIFY_TYPE + "_" +
                    IDPPConstants.VALUE + "_" + IDPPConstants.QUESTION);
            }
        } catch (MissingResourceException mre) {
            IDPPUtils.debug.error("PPInteractionHelper.getInteractForValue" +
            "Question: No question found for " + ppElement, mre);
            return null;
        }
    }

    /**
     * Gets the Personal Profile Service Leaf Element if there are any for
     * the given PP Attribute element.
     * @param ppElement PP Attribute element
     * @return Set set of leaf elements.
     */
    private Set getPPLeafElements(String ppElement) {
        Set set = new HashSet();
        if(ppElement == null) {
           return set;
        }
        if(ppElement.equals(IDPPConstants.COMMON_NAME_ELEMENT)) {
           return getCommonNameElements();
        } else if(ppElement.equals(IDPPConstants.INFORMAL_NAME_ELEMENT)) {
           return getInformalNameElements();
        } else if(ppElement.equals(IDPPConstants.EMPLOYMENT_IDENTITY_ELEMENT)) {
           return getEmploymentIdentityElements();
        } else if(ppElement.equals(IDPPConstants.LEGAL_IDENTITY_ELEMENT)) {
           return getLegalIdentityElements();
        } else if(ppElement.equals(IDPPConstants.EXTENSION_ELEMENT)) {
           return getExtensionElements();
        } else { 
        //if-else loops are followed
        }
        //It comes here only if it's a leaf element
        set.add(ppElement);
        return set; 
    }

    // Static method to get the common name elements
    private static Set getCommonNameElements() {
        Set set = getAnalyzedNameElements();
        set.add(IDPPConstants.CN_ELEMENT);
        set.add(IDPPConstants.ALT_CN_ELEMENT);
        return set;
    }

    // Static method to get analyzed name elements
    private static Set getAnalyzedNameElements() {
        Set set = new HashSet();
        set.add(IDPPConstants.FN_ELEMENT);
        set.add(IDPPConstants.MN_ELEMENT);
        set.add(IDPPConstants.SN_ELEMENT);
        set.add(IDPPConstants.PT_ELEMENT);
        return set;
    }

    // Static method to get informal name elements
    private static Set getInformalNameElements() {
        Set set = new HashSet();
        set.add(IDPPConstants.INFORMAL_NAME_ELEMENT);
        return set;
    }

    // Static method to get employment identity elements
    private static Set getEmploymentIdentityElements() {
        Set set = new HashSet();
        set.add(IDPPConstants.JOB_TITLE_ELEMENT);
        set.add(IDPPConstants.O_ELEMENT);
        set.add(IDPPConstants.ALT_O_ELEMENT);
        return set;
    }

    // Static method to get legal identity elements
    private static Set getLegalIdentityElements() {
         Set set = getAnalyzedNameElements();
         set.add(IDPPConstants.LEGAL_NAME_ELEMENT);
         set.add(IDPPConstants.ALT_ID_TYPE_ELEMENT);
         set.add(IDPPConstants.ALT_ID_VALUE_ELEMENT);
         set.add(IDPPConstants.GENDER_ELEMENT);
         set.add(IDPPConstants.DOB_ELEMENT);
         set.add(IDPPConstants.MARITAL_STATUS_ELEMENT);
         set.add(IDPPConstants.ID_TYPE_ELEMENT);
         set.add(IDPPConstants.ID_VALUE_ELEMENT);
         return set;
    }

    // Static method to get the extension elements
    private static Set getExtensionElements() {
         return  IDPPServiceManager.getInstance().getExtensionAttributes();
    }

}
