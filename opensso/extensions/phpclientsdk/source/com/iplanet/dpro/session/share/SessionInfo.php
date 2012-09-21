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
 * $Id: SessionInfo.php,v 1.1 2007/03/09 21:13:10 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


/**
 * <code>SessionInfo</code> class holds all the information about the
 * <code>Session</code>
 *
 */
class SessionInfo {

	/** <code>Session</code> id */
	public $sid;

	/** <code>Session</code> type */
	public $type;

	/** <code>Cookie</code> id */
	public $cid;

	/** <code> Cookie</code> domain */
	public $cdomain;

	/** Max <code>Session</code> Time */
	public $maxtime;

	/** Max <code>Session</code> Idle time */
	public $maxidle;

	/** Max <code>Session</code> Cache */
	public $maxcaching;

	/** <code>Session</code> idle time */
	public $timeidle;

	/** Time left for <code>Session</code> to become inactive */
	public $timeleft;

	/** <code>Session</code> state */
	public $state;

	public $properties = array ();

	const QUOTE = "\"";

	const NL = "\n";

	/**
	* translates the <code>Session</code> Information to an XML document
	* String based
	* @return An XML String representing the information
	*/
	public function toXMLString() {
		$xml = "<Session sid=" . SessionInfo::QUOTE . $this->sid . SessionInfo::QUOTE . " stype=" . SessionInfo::QUOTE . $this->stype . SessionInfo::QUOTE . " cid=" . SessionInfo::QUOTE .
		htmlentities($this->cid, ENT_COMPAT, "UTF-8") . SessionInfo::QUOTE . " cdomain=" . SessionInfo::QUOTE .
		htmlentities($this->cdomain, ENT_COMPAT, "UTF-8") . SessionInfo::QUOTE . " maxtime=" . SessionInfo::QUOTE . $this->maxtime . SessionInfo::QUOTE . " maxidle=" . SessionInfo::QUOTE . $this->maxidle . SessionInfo::QUOTE .
		" maxcaching=" . SessionInfo::QUOTE . $this->maxcaching . SessionInfo::QUOTE . " timeidle=" . SessionInfo::QUOTE .
		$this->timeidle . SessionInfo::QUOTE . " timeleft=" .
		SessionInfo::QUOTE . $this->timeleft . SessionInfo::QUOTE . " state=" . SessionInfo::QUOTE . $this->state . SessionInfo::QUOTE . ">" .
		$this->NL;

		foreach ($this->properties as $name => $value) {
			$xml = $xml . "<Property name=" . SessionInfo::QUOTE .
			htmlentities($name, ENT_COMPAT, "UTF-8") . SessionInfo::QUOTE . " value=" . SessionInfo::QUOTE .
			htmlentities($value, ENT_COMPAT, "UTF-8") . SessionInfo::QUOTE . ">" . "</Property>" . SessionInfo::NL;
		}
		$xml .= "</Session>";
		return $xml;
	}
}
?>
