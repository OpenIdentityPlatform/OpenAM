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
 * "Portions Copyrighted [year] [name of copyright owner]
 *
 * $Id: WSFedGeneralBase.java,v 1.6 2008/06/25 05:49:38 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.WSFedPropertiesModel;
import com.sun.identity.console.federation.model.WSFedPropertiesModelImpl;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;

public abstract class WSFedGeneralBase extends EntityPropertiesBase {
    
    public WSFedGeneralBase(String name) {
        super(name);
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new WSFedPropertiesModelImpl(req, getPageSessionAttributes());
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
    }
    
    protected String getProfileName() {
        return EntityModel.WSFED;
    }
    
    protected abstract void createPropertyModel();
    
    /**
     * Converts the List to Set.
     *
     * @param list the list to be converted.
     * @return the corresponding Set.
     */
    protected Set convertListToSet(List list) {
        Set s = new HashSet();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            s.add(it.next());
        }
        return s;
    }
    
    /**
     * Return empty set if value is null
     *
     * @param set the set to be checked for null.
     * @return the EMPTY_SET if value is null.
     */
    protected Set returnEmptySetIfValueIsNull(Set set) {
        return (set != null) ? set : Collections.EMPTY_SET;
    }
}
