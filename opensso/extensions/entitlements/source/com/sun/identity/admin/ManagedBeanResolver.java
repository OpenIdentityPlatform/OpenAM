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
 * $Id: ManagedBeanResolver.java,v 1.5 2009/07/22 20:32:16 farble1670 Exp $
 */

package com.sun.identity.admin;

import javax.el.ELResolver;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ManagedBeanResolver {

    public ManagedBeanResolver() {
    }

    public ManagedBeanResolver(ServletContext context, ServletRequest request, ServletResponse response) {
        ServletFacesContext.getInstance(context, request, response);
    }

    public Object resolve(String name) {
        FacesContext fcontext = FacesContext.getCurrentInstance();
        ELResolver resolver = fcontext.getELContext().getELResolver();
        Object o = resolver.getValue(fcontext.getELContext(), null, name);

        assert(o != null);
        
        return o;
    }

    /*
    public Object resolve(String name) {
        FacesContext fc = FacesContext.getCurrentInstance();
        final StringBuffer valueBinding = new StringBuffer();  
        valueBinding.append('#');  
        valueBinding.append('{');  
        valueBinding.append(name);  
        valueBinding.append('}');  
        
        final Object o = fc.getApplication().createValueBinding(valueBinding.toString()).getValue(fc);  

        return o;
    }
    */
}
