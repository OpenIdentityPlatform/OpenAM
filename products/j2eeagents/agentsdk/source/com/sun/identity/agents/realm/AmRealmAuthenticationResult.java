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
 * $Id: AmRealmAuthenticationResult.java,v 1.2 2008/06/25 05:51:58 qcheng Exp $
 *
 */

package com.sun.identity.agents.realm;

import java.util.HashSet;
import java.util.Set;


/**
 * The agent realm authentication result
 */
public class AmRealmAuthenticationResult {
    public boolean isValid() {
        return _isValid;
    }
    
    public AmRealmAuthenticationResult() {
        this(false, null);
    }
    
    public AmRealmAuthenticationResult(boolean valid) {
        this(valid,null);
    }
    
    public AmRealmAuthenticationResult(boolean valid, Set attributes) {
        setValid(valid);
        setAttributes(attributes);
    }
    
    public Set getAttributes() {
        return _attributes;
    }
    
    private void setAttributes(Set attributes) {
        if (attributes != null) {
            _attributes = attributes;
        } else {
            _attributes = new HashSet();
        }
    }
    
    private void setValid(boolean valid) {
        _isValid = valid;
    }
    
    private boolean _isValid;
    private Set _attributes;
    
    public static final AmRealmAuthenticationResult FAILED 
                    = new AmRealmAuthenticationResult();
}
