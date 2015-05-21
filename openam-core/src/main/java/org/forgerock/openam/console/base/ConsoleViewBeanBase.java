/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.util.Encoder;
import com.iplanet.jato.view.ViewBeanBase;
import org.forgerock.openam.utils.IOUtils;

import java.util.Map;

/**
 * Base class for to all OpenAM console pages that need to extend the JATO ViewBeanBase.
 */
public class ConsoleViewBeanBase extends ViewBeanBase {

    public ConsoleViewBeanBase() {
        super();
    }

    public ConsoleViewBeanBase(String pageName) {
        super(pageName);
    }

    @Override
    protected void deserializePageAttributes() {
        {
            if (isPageSessionDeserialized()) {
                return;
            }
            RequestContext context = getRequestContext();
            if (context == null) {
                context = RequestManager.getRequestContext();
            }
            final String pageAttributesParam = context.getRequest().getParameter("jato.pageSession");
            if (pageAttributesParam != null && !pageAttributesParam.trim().isEmpty()) {
                try {
                    ClassLoader classLoader = null;
                    Object refObject = RequestManager.getHandlingServlet();
                    if (refObject != null) {
                        classLoader = refObject.getClass().getClassLoader();
                    }
                    setPageSessionAttributes(
                            IOUtils.<Map>deserialise(Encoder.decodeHttp64(pageAttributesParam), false, classLoader));
                } catch (Exception e) {
                    handleDeserializePageAttributesException(e);
                }
            }
            setPageSessionDeserialized();
        }
    }

    private boolean isPageSessionDeserialized() {
        return RequestManager.getRequestContext().getRequest().getAttribute("jato.pageSessionDeserialized") != null;
    }

    private void setPageSessionDeserialized() {
        RequestManager.getRequestContext().getRequest().setAttribute("jato.pageSessionDeserialized", Boolean.TRUE);
    }
}
