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
 * $Id: IXMLUtilsConstants.java,v 1.2 2008/06/25 05:51:31 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

/**
 * Defines constants that are frequently used by the XMLUtils subsystem of
 * classes.
 */
public interface IXMLUtilsConstants {

    public static final String TOKEN_TYPE_BOUNDED = "bounded";

    public static final String TOKEN_TYPE_UNBOUNDED = "unbounded";

    public static final String TOKEN_TYPE_WHITESPACE = "whitespace";

    public static final String TOKEN_TYPE_META = "meta";

    public static final String TOKEN_TYPE_COMMENT = "comment";

    public static final String TOKEN_TYPE_DOCTYPE = "doctype";

    public static final String NEW_LINE = System.getProperty("line.separator",
            "\n");

    public static final String DOCTYPE = "DOCTYPE";
}
