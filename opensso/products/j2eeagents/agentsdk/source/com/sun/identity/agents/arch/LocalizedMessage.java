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
 * $Id: LocalizedMessage.java,v 1.2 2008/06/25 05:51:36 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Represents a localized message in the system. A <code>LocalizedMessage</code>
 * is strongly tied to the <code>Module</code> from where it originates from
 * and is uses a <code>Module</code> specific resource <code>id</code> to 
 * locate the actual resource. It also facilitates the localization into a
 * different locale other than the <code>Module</code>'s configured locale.
 */
public class LocalizedMessage {
    
   /**
    * Returns the string representation of this <code>LocalizedMessage</code>
    * in the given <code>locale</code>.
    * 
    * @param locale the Locale to be used when rendering this message.
    * @return the localized String representing this message.
    */
    public String toString(Locale locale) {

        return getFormattedMessage(
            ModuleList.getRegisteredModule(getModuleCode()).getResource(
                getId(), locale));
    }

   /**
    * Returns the string representation of this <code>LocalizedMessage</code>
    * in the Locale of the originating <code>Module</code>.
    * 
    * @return the localized String representing this message.
    */
    public String toString() {

        String result = null;

        if(getLocale() != null) {
            result = toString(getLocale());
        } else {
            result = getFormattedMessage(
                ModuleList.getRegisteredModule(getModuleCode()).getResource(
                    getId()));
        }

        return result;
    }

   /**
    * Allows the caller to set a locale different from the default locale
    * associated with the <code>Module</code> from where the message originated.
    * 
    * @param locale the Locale to be used when rendering the message.
    */
    public void setLocale(Locale locale) {
        _locale = locale;
    }
    
   /**
    * Constructs a <code>LocalizedMessage</code> associated with the 
    * <code>Module</code> corresponding to the given <code>moduleCode</code>, 
    * and the <code>id</code> of the localized resource. 
    * 
    * @param moduleCode the code of the <code>Module</code> from where the 
    * message originates.
    * @param id the <code>id</code> of the resource.
    */    
    LocalizedMessage(byte moduleCode, int id) {
        this(moduleCode, id, null);
    }
    
   /**
    * Constructs a <code>LocalizedMessage</code> associated with the 
    * <code>Module</code> corresponding to the given <code>moduleCode</code>, 
    * and the <code>id</code> of the localized resource. The optional 
    * <code>params</code> argument allows any special values to be inserted
    * into the final message using the <code>MessageFormat</code> semantics.
    * 
    * @param moduleCode the code of the <code>Module</code> from where the 
    * message originates.
    * @param id the <code>id</code> of the resource.
    * @param params optional parameters that will be inserted into the final
    * message using the <code>MessageFormat</code> semantics.
    */
    LocalizedMessage(byte moduleCode, int id, Object[] params) {

        setModuleCode(moduleCode);
        setId(id);
        setParams(params);
    }
    
    private String getFormattedMessage(String resourceString) {
        String message = null;
        if((getParams() == null) || (getParams().length == 0)) {
            message = resourceString;
        } else {
            message = MessageFormat.format(resourceString, getParams());
        }
        return message;
    }
    
    private void setModuleCode(byte moduleCode) {
        _moduleCode = moduleCode;
    }
    
    private byte getModuleCode() {
        return _moduleCode;
    }
    
    private int getId() {
        return _id;
    }
    
    private void setId(int id) {
        _id = id;
    }
    
    private Object[] getParams() {
        return _params;
    }

    private void setParams(Object[] params) {
        _params = params;
    }
        
    private Locale getLocale() {
        return _locale;
    }

    private byte     _moduleCode;
    private int      _id;
    private Object[] _params;
    private Locale   _locale;
}
