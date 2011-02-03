/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Resources.java,v 1.7 2009/06/04 11:49:11 veiming Exp $
 */

package com.sun.identity.admin;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

public class Resources {

    private Locale locale = null;

    public Resources() {
        locale = getLocaleFromFaces();
    }

    public Resources(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public Resources(ServletRequest request) {
        this.locale = getLocaleFromRequest(request);
    }

    private Locale getLocaleFromFaces() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            throw new RuntimeException("faces context is not available");
        }

        Locale l = fc.getViewRoot().getLocale();
        return l;
    }

    private Locale getLocaleFromRequest(ServletRequest request) {
        return request.getLocale();
    }

    public ResourceBundle getResourceBundle() {
        ResourceBundle rb = ResourceBundle.getBundle("com.sun.identity.admin.Messages", locale);
        return rb;
    }

    public String getString(String key) {
        ResourceBundle rb = getResourceBundle();
        try {
            return rb.getString(key);
        } catch (MissingResourceException mre) {
            return null;
        }
    }

    public String getString(String key, Object... params) {
        ResourceBundle rb = getResourceBundle();
        String msg;
        try {
            msg = rb.getString(key);
            msg = MessageFormat.format(msg, params);
        } catch (MissingResourceException mre) {
            msg = null;
        }

        return msg;
    }

    public String getString(Object o, String key) {
        return getString(o.getClass().getName() + "." + key);
    }

    public String getString(Class c, String key) {
        return getString(c.getName() + "." + key);
    }

    public String getString(Object o, String key, Object... params) {
        return getString(o.getClass().getName() + "." + key, params);
    }

    public String getString(Class c, String key, Object... params) {
        return getString(c.getName() + "." + key, params);
    }
}
