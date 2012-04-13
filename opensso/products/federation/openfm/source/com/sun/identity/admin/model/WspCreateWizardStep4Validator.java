/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: WspCreateWizardStep4Validator.java,v 1.1 2009/10/19 22:51:25 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.net.URI;
import java.net.URISyntaxException;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;


public class WspCreateWizardStep4Validator extends WspCreateWizardStepValidator
{
    public WspCreateWizardStep4Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {

        if( !validNameIdMapper() ) {
            return false;
        }
        
        if( !validAttributeNamespace() ) {
            return false;
        }
        
        return true;
    }

    private boolean validNameIdMapper() {
        WspCreateWizardBean wizardBean = getWspCreateWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();
        
        String nameIdMapper = profileBean.getNameIdMapper();
        boolean valid = true;
        
        if( nameIdMapper != null && nameIdMapper.length() > 0 ) {
            try {
                Class.forName(nameIdMapper);
            } catch (ClassNotFoundException cnfe) {
                Effect e;
                e = new InputFieldErrorEffect();
                profileBean.setNameIdMapperInputEffect(e);
                
                e = new MessageErrorEffect();
                profileBean.setNameIdMapperMessageEffect(e);
                
                showErrorMessage("invalidNameIdMapperSummary", 
                                 "invalidNameIdMapperDetail");
                valid = false;
            }
        } 
        
        return valid;
    }
    
    private boolean validAttributeNamespace() {
        WspCreateWizardBean wizardBean = getWspCreateWizardBean();
        WspProfileBean profileBean = wizardBean.getWspProfileBean();

        String namespace = profileBean.getAttributeNamespace();
        boolean valid = true;
        
        try {
            new URI(namespace);
        } catch (URISyntaxException urise) {
            Effect e;
            e = new InputFieldErrorEffect();
            profileBean.setAttributeNamespaceInputEffect(e);
            
            e = new MessageErrorEffect();
            profileBean.setAttributeNamespaceMessageEffect(e);

            showErrorMessage("invalidNamespaceSummary",
                             "invalidNamespaceDetail");
            valid = false;
        }

        return valid;
    }

}
