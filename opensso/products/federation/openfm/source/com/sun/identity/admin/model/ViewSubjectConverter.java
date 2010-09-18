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
 * $Id: ViewSubjectConverter.java,v 1.1 2009/10/19 17:54:06 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

public class ViewSubjectConverter implements Converter {

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        String[] vals = value.split(":");
        String subjectTypeName = vals[0];
        String viewSubjectName = vals[1];

        // so, so terrible
        SubjectFactory sf = SubjectFactory.getInstance();
        Map<String,SubjectType> subjectTypeNameMap = sf.getSubjectTypeNameMap();
        SubjectType st = subjectTypeNameMap.get(subjectTypeName);
        SubjectContainer sc = sf.getSubjectContainer(st);
        for (ViewSubject vs: sc.getViewSubjects()) {
            if (vs.getName().equals(viewSubjectName)) {
                return vs;
            }
        }
        return null;
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        ViewSubject vs = (ViewSubject)value;
        return vs.getSubjectType().getName() + ":" + vs.getName();
    }

}
