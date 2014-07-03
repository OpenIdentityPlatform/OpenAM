
/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.idm;

/**
 * An exception type thrown when an {@link com.sun.identity.idm.IdRepo} is asked to
 * create an object with a name that is already used.
 *
 * @supported.all.api
 */
public class IdRepoDuplicateObjectException extends IdRepoException {

    /**
     * This constructor is used to pass the localized error message At this
     * level, the locale of the caller is not known and it is not possible to
     * throw localized error message at this level. Instead this constructor
     * provides Resource Bundle name and error code for correctly locating the
     * error message. The default <code>getMessage()</code> will always return
     * English messages only. This is in consistent with current JRE.
     *
     * @param rbName
     *            Resource bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use <code>ResourceBundle rb =
     *        ResourceBunde.getBundle(rbName,locale);
     *        String localizedStr = rb.getString(errorCode)</code>.
     * @param args
     *            arguments to message. If it is not present pass the as null.
     */
    private IdRepoDuplicateObjectException(String rbName, String errorCode, Object[] args) {
        super(rbName, errorCode, args);
    }

    /**
     * Create an instance using the localized {@link IdRepoBundle#NAME_ALREADY_EXISTS }
     * error message populated with the provided name.
     *
     * @param name An identity name that is already taken
     * @return exception with localized error message
     */
    public static IdRepoDuplicateObjectException nameAlreadyExists(String name) {
        return new IdRepoDuplicateObjectException(IdRepoBundle.BUNDLE_NAME, IdRepoBundle.NAME_ALREADY_EXISTS,
                new String[] { name });
    }

    /**
     * Create an instance using the localized {@link IdRepoBundle#IDENTITY_OF_TYPE_ALREADY_EXISTS }
     * error message populated with the provided name and type.
     *
     * @param name An identity name that is already taken
     * @param type The identity type
     * @return exception with localized error message
     */
    public static IdRepoDuplicateObjectException identityOfTypeAlreadyExists(String name, String type) {
        return new IdRepoDuplicateObjectException(IdRepoBundle.BUNDLE_NAME,
                IdRepoBundle.IDENTITY_OF_TYPE_ALREADY_EXISTS, new String[] { name, type });
    }

}
