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
 * $Id: TemplatedPage.java,v 1.10 2009/01/05 23:17:10 veiming Exp $
 *
 */
package com.sun.identity.config.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public abstract class TemplatedPage extends AjaxPage {
    
    public static final String STATUS_MESSAGE_CODES_SESSION_KEY = "statusMessageCodes";

    public int currentYear = Calendar.getInstance().get( Calendar.YEAR );

    public List statusMessages = new ArrayList(); //of Strings

    public String getTemplate() {
        return "assets/templates/main.html";
    }

    /**
     * Return the i18n code of the page title.
     * @return the i18n code of the page title;
     */
    protected abstract String getTitle();


    protected List getStatusMessageCodes() {
        return (List)getContext().getSessionAttribute(
            STATUS_MESSAGE_CODES_SESSION_KEY);
    }

    protected void addStatusMessageCode(String statusMessageCode) {
        List codes = getStatusMessageCodes();
        if (codes == null) {
            codes = new ArrayList();
        }
        codes.add(statusMessageCode);
        getContext().setSessionAttribute(
            STATUS_MESSAGE_CODES_SESSION_KEY, codes);
    }

    protected void clearStatusMessageCodes() {
        getContext().removeSessionAttribute(STATUS_MESSAGE_CODES_SESSION_KEY);
    }

    public void onInit() {
	super.onInit();
        addModel("title", getLocalizedString(getTitle()));
        List sessionStatusMessages = getStatusMessageCodes();
        if (sessionStatusMessages != null && !sessionStatusMessages.isEmpty()) {
            Iterator i = sessionStatusMessages.iterator();
            while ( i.hasNext() ) {
                String messageCode = (String)i.next();
                statusMessages.add(getLocalizedString(messageCode) );
            }
            clearStatusMessageCodes();
        }
        doInit();
    }

    protected void doInit() {
        //subclass implementation hook.
    }

}
