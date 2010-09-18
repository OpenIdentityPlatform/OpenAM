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
 * $Id: Properties.php,v 1.1 2007/03/09 21:13:20 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Properties class. Similar to Java Properties, deals with multi-line
 * properties files.
 */
class Properties {

	var $properties;
	var $keyValueSeparators = "=: \t\r\n";
	var $whiteSpaceChars = " \t\r\n";

	public function __construct($file) {
		$this->properties = array ();
		$this->load($file);
	}

	private function load($file) {
		$lines = file($file);
		$lc = 0;
		$cont = false;
		$key = null;
		foreach ($lines as $line) {
			$line = ltrim($line, $this->whiteSpaceChars);
			if (strlen($line) == 0 || substr($line, 0, 2) === "/*" || substr($line, 0, 1) === "*" || substr($line, 0, 1) === "#")
				continue;

			if (!$cont) {
				$key = $this->findFirstIn($line, $this->keyValueSeparators);

				if ($key === false)
					continue;

				$value = substr($line, $key +1);
				$value = trim($value, $this->whiteSpaceChars);

				$key = substr($line, 0, $key);
				$key = trim($key, $this->whiteSpaceChars);

				if (substr($value, strlen($value) - 1, 1) === '\\') {
					$value = substr($value, 0, strlen($value) - 1);
					$cont = true;
				} else
					$this->properties[$key] = $value;
			} else {
				$line = trim($line, $this->whiteSpaceChars);
				if (substr($line, strlen($line) - 1, 1) === '\\') {
					$value .= substr($line, 0, strlen($line) - 1);
				} else {
					$cont = false;
					$value .= $line;
					$this->properties[$key] = $value;
				}
			}
		}
	}

	private function continueLine($line) {
		$slashCount = 0;
		$index = strlen($line) - 1;
		while (($index >= 0) && (substr($line, $index--, 1) == '\\'))
			$slashCount++;
		return ($slashCount % 2 == 1);
	}

	/**
	  * Finds the first occurance of any character of $choices in $txt
	  */
	private function findFirstIn($txt, $choices, $start = null) {
		$pos = -1;
		$arr = array ();
		for ($i = 0; $i < strlen($choices); $i++) {
			array_push($arr, substr($choices, $i, 1));
		}
		foreach ($arr as $v) {
			$p = strpos($txt, $v, $start);
			if ($p === FALSE)
				continue;
			if (($p < $pos) || ($pos == -1))
				$pos = $p;
		}
		return $pos;
	}

	public function set_property($key, $value) {
		$this->properties[$key] = $value;
	}

	public function get_property($key) {
		return $this->properties[$key];
	}

	public function toArray() {
		return $this->properties;
	}

}
?>
