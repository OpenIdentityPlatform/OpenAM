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
 * $Id: PatternMatcher.java,v 1.2 2008/06/25 05:51:41 qcheng Exp $
 *
 */

package com.sun.identity.agents.common;



import java.util.ArrayList;

import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.agents.util.PatternRule;


/**
 * The class provides pattern matching for agent
 */
public class PatternMatcher extends SurrogateBase implements IPatternMatcher {
    
    public PatternMatcher(Module module) {
        super(module);
    }
    
    public void initialize(String[] patternList) {
        ArrayList patternRules = new ArrayList();

        if((patternList != null) && (patternList.length > 0)) {
            for(int i = 0; i < patternList.length; i++) {
                patternRules.add(new PatternRule(patternList[i]));
            }
        }
        setPatternRules(patternRules);
    }

    public boolean match(String str) {

        boolean result = false;
        ArrayList  rules  = getPatternRules();

        for(int i = 0; i < rules.size(); i++) {
            PatternRule nextRule = (PatternRule) rules.get(i);

            if(nextRule.matchString(str)) {
                if(isLogMessageEnabled()) {
                    logMessage("PatternMatcher.match(" + str + "): matched "
                               + "by rule: " + nextRule.getPatternString());
                }
                result = true;
                break;
            }
        }

        return result;
    }

    private ArrayList getPatternRules() {
        return _patternRules;
    }

    private void setPatternRules(ArrayList patternRules) {
        _patternRules = patternRules;
    }

    private ArrayList _patternRules;
}
