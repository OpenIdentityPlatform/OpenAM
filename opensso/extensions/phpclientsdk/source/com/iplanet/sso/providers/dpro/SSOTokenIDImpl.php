<?php
/* The contents of this file are subject to the terms
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
 * $Id: SSOTokenIDImpl.php,v 1.1 2007/03/09 21:13:17 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * This class <code>SSOTokenIDImpl</code> implements the interface
 * <code>SSOTokenID</code> and is used to
 * identify a <code>SSOToken</code> object.
 * It contains a random String and which is unique on a given
 * SSOToken server.
 *
 * @see com.iplanet.sso.SSOToken
 */
class SSOTokenIDImpl implements SSOTokenID {

    /** single sign on Session id */
     private $SSOSessionID;

    /** hashcode for the object*/
     private $hashCode = -1;

    /**
     * Creates a SSOTokenIDImpl object
     * @param sid The SessionID
     * @see com.iplanet.dpro.session.SessionID
     */
    function __construct(SessionID $sid) {
        $this->SSOSessionID = $sid;
        $hashCode = $this->SSOSessionID->hashCode();
    }

    /**
     * This method returns the encrypted sso token string.
     *
     * @return An encrypted sso token string.
     */
    public function __toString() {
        return $this->SSOSessionID->__toString();
    }

    /**
     * Compares this SessionID to the specified object. The result is true if
     * and only if the argument is not null and the random string and server
     * name are the same in both objects.
     *
     * @param an
     *            Object - the object to compare this SessionID against.
     * @return true if the SessionID are equal; false otherwise.
     */
    public function equals($object) {
        if ($this === $object)
            return true;
        else if ($object == null)
            return false;

        return $this->SSOSessionID->equals($object);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code value for this object.
     */
    public function hashCode() {
        // Since SSOTokenID is immutable, it's hashCode doesn't change.
        return $this->hashCode;
    }
}
?>
