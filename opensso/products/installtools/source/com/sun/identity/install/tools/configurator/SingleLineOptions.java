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
 * $Id: SingleLineOptions.java,v 1.2 2008/06/25 05:51:23 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.Console;

public class SingleLineOptions extends BaseOptions {

    public SingleLineOptions() {
        super();
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        if (separator == null || separator.trim().length() == 0) {
            separator = STR_COMMA_DELIMITER;
        }
        return separator;
    }

    public void display() {
        StringBuffer sb = new StringBuffer(256);

        sb.append(STR_OPTIONS_START_DELIMITER);
        sb.append(STR_SPACE_DELIMITER);

        int count = getResponseOptions().size();
        for (int i = 0; i < count; i++) {
            UserOptionItem option = (UserOptionItem)
                getResponseOptions().get(i);
            sb.append(option.getDisplayMessage());
            if (i != count - 1) { // Not the last one
                sb.append(getSeparator());
            }
            sb.append(STR_SPACE_DELIMITER);
        }

        sb.append(STR_OPTIONS_END_DELIMITER);
        Console.println(sb.toString());
    }

    private String separator;

    public static final String STR_OPTIONS_START_DELIMITER = "[";

    public static final String STR_COMMA_DELIMITER = ",";

    public static final String STR_SPACE_DELIMITER = " ";

    public static final String STR_FORWARD_SLASH_DELIMITER = "/";

    public static final String STR_OPTIONS_END_DELIMITER = "]";
}
