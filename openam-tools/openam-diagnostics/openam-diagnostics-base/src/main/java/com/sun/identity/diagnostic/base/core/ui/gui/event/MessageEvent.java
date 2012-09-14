/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MessageEvent.java,v 1.1 2008/11/22 02:19:56 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.event;

public class MessageEvent {
    
    public static final int RESULT_MESSAGE = 0;
    public static final int INFO_MESSAGE = 1;
    public static final int WARNING_MESSAGE = 2;
    public static final int ERROR_MESSAGE = 3;

    private int type;
    private String message;
    
    /** Creates a new instance of MessageEvent */
    public MessageEvent(int type, String message) {
        this.type = type;
        this.message = message;
    }
    
    public int getMessageType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
}
