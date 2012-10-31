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
 * $Id: AMViewInterface.java,v 1.2 2008/06/25 05:42:48 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

public interface AMViewInterface
{
    /**
     * Hardcoded <code>Lockhart</code> URI.
     */
    String LOCKHART_HARDCODED_URI = "/com_sun_web_ui";

    /**
     * Tab index for Realm Management.
     */
    int TAB_REALM = 1;

    /**
     * Tab index for Configuration Management.
     */
    int TAB_CONFIGURATION = 4;
    int TAB_SERVICE_AUTHENTICATION = 41;
    int TAB_SERVICE_GENERAL = 42;
    int TAB_SERVICE_CONSOLE  = 43;
    int TAB_SERVICE_CORE = 44;
    int TAB_SERVICE_POLICY = 45;

    /**
     * Tab index for Web Services Management.
     */
    int TAB_WEBSERVICES = 3;

    /**
     * Tab index for Web Services - Personal Profile.
     */
    int TAB_WEBSERVICES_PERSONAL_PROFILE = 31;

    /**
     * Tab index for Web Services - Discovery Service.
     */
    int TAB_WEBSERVICES_DISCOVERY_SERVICE = 32;

    /**
     * Tab index for Session Management.
     */
    int TAB_SESSIONS = 5;
    int TAB_SESSIONS_CURRENT = 51;
    int TAB_SESSIONS_HA_CONFIGURATION = 52;
    int TAB_SESSIONS_HA_STATISTICS = 53;


    /**
     * Static Text Field Child Prefix.
     */
    String PREFIX_STATIC_TXT = "txt";

    /**
     * Serializable Field Child Prefix.
     */
    String PREFIX_SERIALIZABLE = "sz";

    /**
     * Checkbox Child Prefix.
     */
    String PREFIX_CHECKBOX = "cb";

    /**
     * Text Field Child Prefix.
     */
    String PREFIX_TEXTFIELD = "tf";

    /**
     * Label Child Prefix.
     */
    String PREFIX_LABEL = "lbl";

    /**
     * <code>HREF</code> Field Child Prefix.
     */
    String PREFIX_HREF = "href";

    /**
     * Button Field Child Prefix.
     */
    String PREFIX_BUTTON = "btn";

    /**
     * Radio Button Child Prefix.
     */
    String PREFIX_RADIO_BUTTON = "radio";

    /**
     * Single Choice Box Child Prefix.
     */
    String PREFIX_SINGLE_CHOICE = "singleChoice";

    /**
     * Inline Alert Child Prefix.
     */
    String PREFIX_INLINE_ALERT = "ialert";

    /**
     * Legend Child Prefix.
     */
    String PREFIX_LEGEND = "legend";
}
