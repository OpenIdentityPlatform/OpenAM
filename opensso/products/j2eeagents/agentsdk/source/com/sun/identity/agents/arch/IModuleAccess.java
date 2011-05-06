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
 * $Id: IModuleAccess.java,v 1.2 2008/06/25 05:51:36 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import java.util.Locale;

/**
 * This interface defines all the access APIs available for a given
 * <code>Module</code>.
 */
public interface IModuleAccess extends IDebugAccess {
    
   /**
    * Returns the resource associated with the given <code>id</code>. This
    * method returns the resource from the <code>Module</code>'s locale.
    * 
    * @param id for which the resource is being requested.
    *
    * @return the resources associated with the given <code>id</code>.
    */
    public String getResource(int id);
    
   /**
    * Returns the resource associated with the given <code>id</code> using
    * the given <code>locale</code>.
    * 
    * @param id for which the resource is being requested.
    * @param locale in which the resource resides.
    *
    * @return the resources associated with the given <code>id</code>.
    *
    */
    public String getResource(int id, Locale locale);
    
   /**
    * Factory method for creating <code>LocalizedMessage</code> instances
    * associated with this instance of <code>Module</code>.
    *
    * @param id the resource identifier for localization purposes.
    *
    * @return the <code>LocalizedMessage</code> representing the message 
    * associated with the given <code>id</code> in the Locale associated 
    * with this <code>Module</code>.
    *
    */
    public LocalizedMessage makeLocalizableString(int id);
    
   /**
    * Factory method for creating <code>LocalizedMessage</code> instances
    * associated with this instance of <code>Module</code>.
    *
    * @param id the resource identifier for localization purposes.
    * @param params optional parameters to be used when rendering is done
    * by <code>LocalizedMessage</code> instance.
    * 
    * @return the <code>LocalizedMessage</code> representing the message 
    * associated with the given <code>id</code> in the Locale associated 
    * with this <code>Module</code>.
    *
    */
    public LocalizedMessage makeLocalizableString(int id, Object[] params);

}
