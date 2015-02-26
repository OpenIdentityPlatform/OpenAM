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
 * $Id: PatternRule.java,v 1.2 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

import java.util.ArrayList;


/**
 * A string that contains wildcard for pattern matching
 */
public class PatternRule {

    public PatternRule(String patternString) {
        setPatternString(patternString);
        initializeTokens(patternString);
    }

    public boolean matchString(String data) {

        boolean match  = true;
        int     index  = 0;
        ArrayList  tokens = getTokens();

        for(int i = 0; i < tokens.size(); i++) {
            Token nextToken = (Token) tokens.get(i);

            switch(nextToken.getTokenType()) {

            case Token.TYPE_CONSTANT_TOKEN :
                int length = nextToken.getConsumptionLength();

                if(index + length > data.length()) {
                    match = false;
                } else {
                    match = nextToken.consume(data.substring(index,
                                                             index + length));
                }

                if(match) {
                    index += length;
                }
                break;

            case Token.TYPE_WILDCARD_TOKEN :
                Token lookahead = null;

                if(i < tokens.size() - 1) {
                    int j = i + 1;

                    while(j < tokens.size()) {
                        Token token = (Token) tokens.get(j);

                        if(token.getTokenType()
                                == Token.TYPE_CONSTANT_TOKEN) {
                            lookahead = token;

                            break;
                        }

                        j++;
                    }
                }

                if(lookahead == null) {
                    match = true;

                    nextToken.consume(data.substring(index));

                    index = data.length();
                } else {
                    String lookaheadString = lookahead.getTokenString();
                    String tmp = data.substring(index, data.length());

                    if(tmp.indexOf(lookaheadString) != -1) {
                        int charsToConsume = tmp.indexOf(lookaheadString);

                        nextToken.consume(data.substring(index,
                                                         index
                                                         + charsToConsume));

                        index += charsToConsume;
                    } else {
                        match = false;
                    }
                }
                break;

            default :
                match = false;
                break;
            }

            if( !match) {
                break;
            }
        }

        if(match) {
            if(index != data.length()) {
                match = false;
            }
        }

        return match;
    }

    public String getPatternString() {
        return _patternString;
    }

    private void setPatternString(String patternString) {
        _patternString = patternString;
    }

    private ArrayList getTokens() {
        return _tokens;
    }

    private void initializeTokens(String pattern) {
        StringBuffer buff = new StringBuffer();
        ArrayList list = new ArrayList();
        for(int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if(ch == '*') {
                if(buff.length() > 0) {
                    list.add(new ConstantToken(buff.toString()));
                    buff.delete(0, buff.length());
                }

                list.add(new WildCardToken());
            } else {
                buff.append(ch);
            }
        }

        if(buff.length() > 0) {
            list.add(new ConstantToken(buff.toString()));
        }

        _tokens = list;
    }
    
    private ArrayList _tokens;
    private String _patternString;    
}
