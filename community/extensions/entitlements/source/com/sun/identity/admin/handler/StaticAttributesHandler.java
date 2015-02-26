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
 * $Id: StaticAttributesHandler.java,v 1.3 2009/06/04 11:49:13 veiming Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.model.AttributesBean;
import com.sun.identity.admin.model.StaticViewAttribute;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

public class StaticAttributesHandler extends AttributesHandler {
    public StaticAttributesHandler(AttributesBean ab) {
        super(ab);
    }

    public void editValueListener(ActionEvent event) {
        StaticViewAttribute sva = (StaticViewAttribute)getViewAttribute(event);
        sva.setValueEditable(true);
    }

    public void valueEditedListener(ValueChangeEvent event) {
        StaticViewAttribute sva = (StaticViewAttribute)getViewAttribute(event);
        sva.setValueEditable(false);
    }
}
