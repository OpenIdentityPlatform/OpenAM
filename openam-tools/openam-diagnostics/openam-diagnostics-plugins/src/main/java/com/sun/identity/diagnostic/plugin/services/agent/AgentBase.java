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
 * $Id: AgentBase.java,v 1.1 2008/11/22 02:41:19 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.agent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.SimpleTimeZone;

import com.sun.identity.diagnostic.plugin.services.common.ClientBase;

/**
 * This is the base class for <code>agent</code> service.
 * Any class that needs agent specific methods can use this base
 * class.
 */
public abstract class AgentBase extends ClientBase implements AgentConstants {
    

 protected boolean isCDSSOenabled(Properties agtProp){
     return Boolean.valueOf((String)agtProp.get(
         AGENT_CDSSO_ENABLE)).booleanValue();
 }

 protected String getLocalDateAsGMTString(){
     SimpleDateFormat dateFormat =
         new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
     dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
     Date date = new Date();
     return dateFormat.format(date);
 }
 
}

