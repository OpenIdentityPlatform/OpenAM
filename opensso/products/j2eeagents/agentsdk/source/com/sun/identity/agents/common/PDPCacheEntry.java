/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.Manager;
import java.util.Map;

/**
 *
 * @author mad
 */
public class PDPCacheEntry implements IPDPCacheEntry {
    public PDPCacheEntry(Manager manager) {

    }
    
    public PDPCacheEntry(String originalURL,
                         Map parameterMap,
                         long creationTime) {
        _originalURL = originalURL;
        _parameterMap = parameterMap;
        _creationTime = creationTime;
    }

    public void initialize() {
    }

    public long getCreationTime() {
        return _creationTime;
    }

    public void setCreationTime(long _creationTime) {
        this._creationTime = _creationTime;
    }

    public String getOriginalURL() {
        return _originalURL;
    }

    public void setOriginalURL(String _originalURL) {
        this._originalURL = _originalURL;
    }

    public Map getParameterMap() {
        return _parameterMap;
    }

    public void setParameterMap(Map _parameterMap) {
        this._parameterMap = _parameterMap;
    }

    private String _originalURL;
    private Map _parameterMap;
    private long _creationTime;
}
