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
 * $Id: InteractionUtils.java,v 1.2 2008/06/25 05:47:18 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.interaction;

import com.sun.identity.liberty.ws.interaction.jaxb.InteractionResponseElement;
import com.sun.identity.liberty.ws.interaction.jaxb.UserInteractionElement;
import com.sun.identity.liberty.ws.interaction.jaxb.ParameterType;
import com.sun.identity.liberty.ws.soapbinding.Message;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

/**
 * Class that provides some utility methods that work with objects 
 * related to interaction
 *
 * @supported.all.api
 */
public class InteractionUtils {
 
    private InteractionUtils() {}

    /**
     * Returns user friendly <code>Map</code> representation of parameters
     * in interaction response element
     *
     * @param interactionResponseElement obtained from
     *                        <code>InteractionManager</code>
     * @return a Map of parameters. Keys of the map are parameter 
     *          name String objects.  Values in the map are parameter value
     *          String objects
     */
    public static Map getParameters(
            InteractionResponseElement interactionResponseElement) {
        List parameters = interactionResponseElement.getParameter();
        Map pm = new HashMap();
        Iterator iter = parameters.iterator();
        while (iter.hasNext()) {
            ParameterType pt = (ParameterType) iter.next();
            pm.put(pt.getName(), pt.getValue());
        }
        return pm;
    }

    /**
     * Returns languages listed for the language attribute of the 
     * <code>UserInteraction</code> header in the message. Returns empty list
     * if <code>UserInteraction</code> header is not included in the message
     *
     * @param message SOAP message from which to find out 
     *      interaction languages
     *
     * @return languages listed for the language attribute of the 
     *      <code>UserInteraction</code> header in the message.
     *      Returns empty list
     *      if <code>UserInteraction</code> header is not included
     *      in the message
     */
    public static List getInteractionLangauge(Message message) {
        List languages = null;
        UserInteractionElement ue 
                = InteractionManager.getUserInteractionElement(message);
        if (ue != null) {
            languages = ue.getLanguage();
        }
        if (languages == null) {
            languages = Collections.EMPTY_LIST;
        }
        return languages;
    }
}
