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
 * $Id: IConstants.java,v 1.1 2009/11/04 22:09:38 leiming Exp $
 *
 */

package com.sun.identity.agents.install.domino;

/**
 * Interface Domino server's specific constants,
 * which gets reused throughout installation interactions.
 */
public interface IConstants {
    
    /**  IBM Lotus DOMINO string **/
    public static String STR_DOMINO = "DOMINO";

    /** Field STR_SPS_GROUP **/
    public static String STR_DOMINO_GROUP = "dominoTools";
    
    public static final String STR_DOMINO_NOTES_INI_FILE = "notes.ini";
    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";

    public static final String STR_DSAME_CONF_FILE = "dsame.conf";
    public static final String STR_DSAME_FILE_TEMPLATE = "dsame.conf.template";
}


