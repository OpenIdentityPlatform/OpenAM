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
 * $Id: SSOTokenID.php,v 1.1 2007/03/09 21:13:16 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * The <code>SSOTokenID</code> is an interface that is used to identify a single
 * sign on token object. It contains a random string and the name of the server.
 * The random string in the <code>SSOTokenID</code> is unique on a given server.
 *
 * @see com.iplanet.sso.SSOToken
 */
interface SSOTokenID {

	/**
	 * This method returns the encrypted SSO token string.
	 *
	 * @return An encrypted SSO token string
	 */
	public function __toString();

	/**
	 * Compares this <code>SSOTokenID</code> to the specified object. The
	 * result is true if and only if the argument is not null and the random
	 * string and server name are the same in both objects.
	 *
	 * @param object to compare against this <code>SSOTokenID</code>.
	 * @return true if the <code>SSOTokenID</code> are equal.
	 */
	public function equals($object);

	/**
	 * Returns a hash code for this object.
	 *
	 * @return a hash code value for this object.
	 */
	public function hashCode();
}
?>
