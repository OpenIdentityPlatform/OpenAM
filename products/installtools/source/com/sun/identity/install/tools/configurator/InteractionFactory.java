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
 * $Id: InteractionFactory.java,v 1.2 2008/06/25 05:51:21 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * @author krishc
 *
 * Factory class to instantiate different interaction objects
 * depending on InteractionInfo. Examples of interaction objects
 * are install interaction, password interaction, summary interaction 
 * etc.
 *
 */
public class InteractionFactory implements InteractionConstants {
    
    public static UserDataInteraction createInteraction (InteractionInfo info) 
        throws InstallException {
        
       UserDataInteraction inter = null;
       
       if (info.getType().equalsIgnoreCase(STR_IN_INSTALL_INTER_TYPE)) {
           Debug.log("Creating Install Interaction");
           inter = new InstallInteraction(info);
       } 
       
       if (inter == null) {
           throw new InstallException(
               LocalizedMessage.get(LOC_IN_ERR_FAILED_TO_CREATE_INTER));
       }
       
       return inter;
    }

}
