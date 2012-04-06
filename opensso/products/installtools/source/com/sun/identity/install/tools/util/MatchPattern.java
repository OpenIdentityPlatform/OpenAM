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
 * $Id: MatchPattern.java,v 1.2 2008/06/25 05:51:29 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

public class MatchPattern {

    public MatchPattern(String pattern, int matchType) {
        setPattern(pattern);
        setIsActiveFlag(true);
        setMatchType(matchType);
        setIgnoreCaseFlag(false);
        setLastOccurranceInFile(false);
    }

    public boolean isPresent(String lineData) {

        boolean matchFound = false;
        int patternLength = getPattern().length();

        lineData = lineData.trim();

        int lineLength = lineData.length();

        if (lineLength >= patternLength) {
            if (isMatchFromStart()) {
                matchFound = lineData.regionMatches(isIgnoreCase(), 0,
                        getPattern(), 0, patternLength);
            } else if (isMatchFromEnd()) {
                int frmOff = lineLength - patternLength;
                matchFound = lineData.regionMatches(isIgnoreCase(), frmOff,
                        getPattern(), 0, patternLength);
            } else if (isIgnoreCase()) {
                String tempLine = new String(lineData);
                String tempPattern = new String(getPattern());
                tempLine.toLowerCase();
                tempPattern.toLowerCase();
                if (tempLine.indexOf(tempPattern) >= 0) {
                    matchFound = true;
                }
            } else if (lineData.indexOf(getPattern()) >= 0) {
                matchFound = true;
            }
        }

        return matchFound;
    }

    public boolean isMatchForLastOccurranceInFile() {
        return lastOccurranceInFile;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActiveFlag(boolean flag) {
        isActive = flag;
    }

    public String getPattern() {
        return pattern;
    }

    public int getMatchType() {
        return matchType;
    }

    public void setIgnoreCaseFlag(boolean flag) {
        ignoreCase = flag;
    }

    public void setLastOccurranceInFile(boolean flag) {
        lastOccurranceInFile = flag;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("[ MatchPattern: pattern='").append(
                getPattern()).append("'");
        sb.append(", isActive=").append(isActive());
        sb.append(", matchFromStart=").append(isMatchFromStart());
        sb.append(", matchFromEnd=").append(isMatchFromEnd());
        sb.append(", ignoreCase=").append(isIgnoreCase());
        sb.append(", lastOccurranceInFile");
        sb.append(isMatchForLastOccurranceInFile());
        sb.append(" ]");

        return sb.toString();
    }

    private boolean isMatchFromStart() {
        return (getMatchType() == INT_MATCH_FROM_START);
    }

    private boolean isMatchFromEnd() {
        return (getMatchType() == INT_MATCH_FROM_END);
    }

    private boolean isIgnoreCase() {
        return ignoreCase;
    }

    private void setPattern(String pattern) {
        this.pattern = pattern;
    }

    private void setMatchType(int type) {
        if ((type == INT_MATCH_FROM_START) || (type == INT_MATCH_FROM_END)
                || (type == INT_MATCH_OCCURRANCE)) {
            matchType = type;
        } else {
            throw new IllegalArgumentException("Invalid argument for match"
                    + " type");
        }
    }

    // Intially will be true. Once found it becomes inactive.
    private boolean isActive;

    // If true only the last occurrance will be removed. Otherwise the first.
    private boolean lastOccurranceInFile;

    private boolean ignoreCase;

    private int matchType;

    private String pattern;

    public static final int INT_MATCH_OCCURRANCE = 0;

    public static final int INT_MATCH_FROM_START = 1;

    public static final int INT_MATCH_FROM_END = 2;
}
