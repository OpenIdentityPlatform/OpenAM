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
 * $Id: HTMLConstants.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test.tools;

/**
 * Defines a set of commonly used string.
 */
interface HTMLConstants {
    String HREF = "<a href=\"{0}\">{1}</a>\n";
    String TBL_ENTRY = "<td id=\"{0}\">{1}</td>\n";
    String TBL_NUM_ENTRY = "<td id=\"{0}\" class=\"number\">{1}</td>\n";

    String TEST_TABLE = 
        "<tr>\n" +
        "<th colspan=\"4\" id=\"tblTest\">{0}</th>\n</tr>\n" +
        "<tr>\n<th id=\"tblTestTitle\">Method Name</th>\n" +
        "<th id=\"tblTestTitle\">Status</th>\n" +
        "<th id=\"tblTestTitle\">Time (secs)</th>\n" +
        "<th id=\"tblTestTitle\">Exception</th>\n" +
        "</tr>\n" +
        "<tr>\n{1}</tr>\n";
}
