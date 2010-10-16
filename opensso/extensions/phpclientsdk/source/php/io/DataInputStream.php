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
 * $Id: DataInputStream.php,v 1.1 2007/03/09 21:13:19 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
    DataInputStream class built to handle getting the binary data from the raw input stream.
*/
class DataInputStream {
	// holder for our raw data
	private $raw_data;
	// the seek head of our input stream
	private $current_byte;
	// the total size of the stream
	private $content_length;

	private $byteorder;

	/*
	    DataInputStream constructor
	    arguments    $rd    raw data stream
	*/
	public function __construct(& $rd) {
		$this->current_byte = 0;
		// store the stream in this object
		$this->raw_data = & $rd;
		// grab the total length of this stream
		$this->content_length = strlen($this->raw_data);
		// determine the multi-byte ordering of this machine
		// temporarily pack 1
		$tmp = pack("d", 1);
		// if the bytes are not reversed
		if ($tmp == "\0\0\0\0\0\0\360\77")
			$this->byteorder = 'big-endian';
		// the bytes are reversed
		else
			if ($tmp == "\77\360\0\0\0\0\0\0")
				$this->byteorder = 'little-endian';
	}

	// returns a single byte value.
	public function readByte() {
		// return the next byte
		return ord($this->raw_data[$this->current_byte++]);
	}

	// returns the value of 2 bytes
	public function readInt() {
		// read the next 2 bytes, shift and add
		return ((ord($this->raw_data[$this->current_byte++]) << 8) | ord($this->raw_data[$this->current_byte++]));
	}

	// returns the value of 4 bytes
	public function readLong() {
		// read the next 4 bytes, shift and add
		return ((ord($this->raw_data[$this->current_byte++]) << 24) | (ord($this->raw_data[$this->current_byte++]) << 16) | (ord($this->raw_data[$this->current_byte++]) << 8) | ord($this->raw_data[$this->current_byte++]));
	}

	// returns the value of 8 bytes
	public function readDouble() {
		if ($this->byteorder == "big-endian") {
			// container to store the reversed bytes
			$invertedBytes = "";
			// create a loop with a backwards index
			for ($i = 7; $i >= 0; $i--) {
				// grab the bytes in reverse order from the backwards index
				$invertedBytes .= $this->raw_data[$this->current_byte + $i];
			}
			// move the seek head forward 8 bytes
			$this->current_byte += 8;
		} else {
			// container to store the bytes
			$invertedBytes = "";
			// create a loop with a forwards index
			for ($i = 0; $i < 8; $i++) {
				// grab the bytes in forward order
				$invertedBytes .= $this->raw_data[$this->current_byte + $i];
			}
			// move the seek head forward
			$this->current_byte += 8;
		}
		// unpack the bytes
		$zz = unpack("dflt", $invertedBytes);
		// return the number from the associative array
		return $zz['flt'];
	}

	// returns a UTF string
	public function readUTF() {
		// get the length of the string (1st 2 bytes)
		$length = $this->readInt();
		// grab the string
		$val = utf8_decode(substr($this->raw_data, $this->current_byte, $length));
		// move the seek head to the end of the string
		$this->current_byte += $length;
		// return the string
		return $val;
	}

	// returns a UTF string with a LONG representing the length
	public function readLongUTF() {
		// get the length of the string (1st 4 bytes)
		$length = $this->readLong();
		// grab the string
		$val = utf8_decode(substr($this->raw_data, $this->current_byte, $length));
		// move the seek head to the end of the string
		$this->current_byte += $length;
		// return the string
		return $val;
	}

	public function eof() {
		return ($this->current_byte >= $this->content_length);
	}

}
?>

