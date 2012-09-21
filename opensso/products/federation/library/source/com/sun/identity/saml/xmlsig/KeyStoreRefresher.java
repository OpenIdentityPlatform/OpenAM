/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: KeyStoreRefresher.java,v 1.1 2008/08/05 22:18:09 weisun2 Exp $
 */ 

package com.sun.identity.saml.xmlsig;

import java.io.File; 
import java.util.Collections;
import java.util.Date; 
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * This class is used to update the key store freqently.
 */

public class KeyStoreRefresher extends GeneralTaskRunnable {
    private long timeStamp;
    private File file;
    private long runPeriod;
    
    /**
     * Constructor.
     * @param runPeriod The period for the clean up to run.
     */
    public KeyStoreRefresher(long runPeriod) {
        this.runPeriod = runPeriod;
        String keystoreFile = SystemConfigurationUtil.getProperty(
              "com.sun.identity.saml.xmlsig.keystore");
        file = new File(keystoreFile); 
        this.timeStamp = file.lastModified();
    }
    
    public boolean addElement(Object obj) {
        return true; 
    }
    
    public boolean removeElement(Object obj) {
        return true; 
    }
    
    public boolean isEmpty() {
        return false;
    }
    
    public long getRunPeriod() {
        return runPeriod;
    }
    
    private void onChange() {
         String classMethod = "KeyStoreRefresher.onChange :" ;
         try {
             String kprovider = SystemConfigurationUtil.getProperty(
                 SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                 SAMLConstants.JKS_KEY_PROVIDER);
             KeyProvider newKeyStore= (KeyProvider)
                 Class.forName(kprovider).newInstance(); 
             XMLSignatureManager.getInstance().getSignatureProvider().
                 initialize(newKeyStore); 
             SAMLUtils.debug.message(classMethod + "Updating Key Store...");     
         } catch (ClassNotFoundException ce) {
             SAMLUtils.debug.error(classMethod, ce); 
         } catch (InstantiationException ie) {
             SAMLUtils.debug.error(classMethod, ie); 
         } catch (IllegalAccessException le) {
             SAMLUtils.debug.error(classMethod, le); 
         }  
    }
    
    public void run() {
        long timeStamp = file.lastModified();

        if( this.timeStamp != timeStamp ) {
            this.timeStamp = timeStamp;
            onChange();
        }   
    }  
}
