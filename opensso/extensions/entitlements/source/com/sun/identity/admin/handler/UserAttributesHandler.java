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
 * $Id: UserAttributesHandler.java,v 1.3 2009/06/24 23:47:01 farble1670 Exp $
 */

package com.sun.identity.admin.handler;

import com.icesoft.faces.component.dragdrop.DndEvent;
import com.icesoft.faces.component.dragdrop.DropEvent;
import com.sun.identity.admin.model.AttributesBean;
import com.sun.identity.admin.model.UserAttributesBean;
import com.sun.identity.admin.model.ViewAttribute;
import java.util.Collections;
import java.util.List;
import javax.faces.event.ActionEvent;

public class UserAttributesHandler extends AttributesHandler {
    public UserAttributesHandler(AttributesBean ab) {
        super(ab);
    }

    public void dropListener(DropEvent dropEvent) {
        int type = dropEvent.getEventType();
        if (type == DndEvent.DROPPED) {
            Object dragValue = dropEvent.getTargetDragValue();
            assert (dragValue != null);
            ViewAttribute va = (ViewAttribute)dragValue;

            List<ViewAttribute> availableViewAttributes = ((UserAttributesBean)getAttributesBean()).getAvailableViewAttributes();
            getAttributesBean().getViewAttributes().add(va);
            availableViewAttributes.remove(va);
            Collections.sort((List)availableViewAttributes);
        }
    }

    @Override
    public void removeListener(ActionEvent event) {
        super.removeListener(event);
        ViewAttribute va = getViewAttribute(event);
        List<ViewAttribute> availableViewAttributes = ((UserAttributesBean)getAttributesBean()).getAvailableViewAttributes();
        availableViewAttributes.add(va);
        Collections.sort(availableViewAttributes);
    }

}
