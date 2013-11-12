/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PWResetUserValidationModel.java,v 1.2 2008/06/25 05:43:43 qcheng Exp $
 *
 */

package com.sun.identity.password.ui.model;

/**
 * <code>PWResetUserValidationModel</code> defines a set of methods that
 * are required by password reset user validation viewbean.
 */
public interface PWResetUserValidationModel extends PWResetModel {   
    /**
     * Returns user attribute configures in password reset service.
     *
     * @param orgDN location distinguished name to search.
     */
    String getUserAttr(String orgDN);

    /**
     * Returns <code>true</code> if the user exists. If more than one users is
     * found then it will return false and view bean will display an error 
     * message.
     *
     * @param userAttrValue User enter data for user validation.
     * @param userAttrName User attribute name to search for.
     * @param orgDN location distingushed name.
     * @return <code>true</code> if user exists.
     */
    boolean isUserExists(
        String userAttrValue, 
        String userAttrName,
        String orgDN);

    /**
     * Returns user validation title.
     *
     * @return user validation title.
     */
    String getUserValidateTitleString();

    /**
     * Returns next button label.
     *
     * @return next button label.
     */
    String getNextBtnLabel();

    /**
     * Sets the realm flag.
     *
     * @param value realm flag.
     */
    void setRealmFlag(boolean value);

    /**
     * Returns <code>true</code> if realm name is valid.
     *
     * @return <code>true</code> if the realm name is valid.
     */
    boolean isValidRealm();

    /**
     * Returns the localized string for attribute name in the user
     * service.
     *
     * @param userAttr attribute name
     * @return localized string for the attribute
     */
    String getLocalizedStrForAttr(String userAttr);

    /**
     * Returns missing user attribute message.
     *
     * @param userAttrName user attribute name.
     * @return missing user attribute message.
     */
    String getMissingUserAttrMessage(String userAttrName);

    /**
     * Returns <code>true</code> if the user is active and account is not 
     * expired. This method will use the user distinguished name stored in the 
     * model to determine if the user's account is active or has expired.
     *
     * @param orgDN organization DN.
     * @return <code>true</code> if user is active and account is not expired.
     */
    boolean isUserActive(String orgDN);

    /**
     * Returns realm name. If the given realm name is null or blank. 
     * then root realm will be returned.
     *
     * @param realm Realm name.
     * @return Realm Name
     * @throws PWResetException if unable to get realm or realm does not exists
     */
    String getRealm(String realm) throws PWResetException;

    /**
     * Sets the valid realm flag.
     *
     * @param orgDN Realm name.
     */
    void setValidRealm(String orgDN);

    /**
     * Returns the realm for the user reseting password.
     *
     * @return the realm for the user reseting password.
     */
    String getUserRealm();
}
