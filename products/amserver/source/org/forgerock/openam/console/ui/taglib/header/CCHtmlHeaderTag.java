/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.console.ui.taglib.header;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.view.View;
import com.sun.identity.shared.Constants;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import org.forgerock.openam.utils.StringUtils;

/**
 *
 * @author Peter Major
 */
public class CCHtmlHeaderTag extends com.sun.web.ui.taglib.header.CCHtmlHeaderTag {

    private static final String CONTEXT_ROOT =
            SystemProperties.get(Constants.AM_CONSOLE_DEPLOYMENT_DESCRIPTOR);

    @Override
    protected String getHTMLStringInternal(Tag parent, PageContext pageContext, View view) throws JspException {
        String ret = super.getHTMLStringInternal(parent, pageContext, view);
        return StringUtils.insertContent(ret, ret.indexOf("</head>"),
                "<link id=\"styleSheet\" rel=\"stylesheet\" type=\"text/css\" href=\""
                + CONTEXT_ROOT
                + "/console/css/help.css\">"
                + "<script type=\"text/javascript\" src=\""
                + CONTEXT_ROOT
                + "/console/js/help.js\" ></script>"
                + "<script type=\"text/javascript\" language=\"javascript\">"
                + "var amContextRoot = \"" + CONTEXT_ROOT + "\";"
                + "</script>");
    }
}
