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
 * $Id: MessageViewBean.java,v 1.2 2008/06/25 05:42:48 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.web.ui.view.alert.CCAlert;
import javax.servlet.http.HttpServletRequest;

/**
 * This view just display a message.
 */
public class MessageViewBean
    extends AMViewBeanBase
{
    private static final String PAGE_NAME = "Message";
    private static final String DEFAULT_DISPLAY_URL 
        = "/console/base/Message.jsp";
    private String message = "";
    private String messageType = CCAlert.TYPE_INFO;
    private String messageTitle = "";

    /**
     * Constructs a message view bean
     */
    public MessageViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    /**
     * This is to bypass the authentication check.
     *
     * @param rc Request Context.
     */
    public void forwardTo(RequestContext rc) {
        super.bypassForwardTo(rc);
    }

    /**
     * Sets message type, title and message.
     *
     * @param messageType Message type.
     * @param messageTitle Message title.
     * @param message Message .
     */
    public void setMessage(
        String messageType,
        String messageTitle,
        String message
    ) {
        this.messageType = messageType;
        this.messageTitle = messageTitle;
        this.message = message;
    }

    /**
     * Sets values to the tags.
     *
     * @param e Display event.
     * throws ModelControlException if model is not accessible.
     */
    public void beginDisplay(DisplayEvent e)
        throws ModelControlException {
        super.beginDisplay(e);
        setInlineAlertMessage(messageType, messageTitle, message);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }

}
