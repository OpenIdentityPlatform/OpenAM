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
 * $Id: database.php,v 1.1 2007/05/22 05:38:41 andreas1980 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

mysql_connect(
	$LIGHTBULB_CONFIG['userdatabase']['host'], 
	$LIGHTBULB_CONFIG['userdatabase']['username'],
	$LIGHTBULB_CONFIG['userdatabase']['password']) or die("Not connected : " . mysql_error() . "\n");


mysql_select_db($LIGHTBULB_CONFIG['userdatabase']['database']) or die( "Unable to select database : " . mysql_error() . "\n");

error_log("Connecting to the database...");

function spi_namemapping_nameIdToLocalId($idp, $sp, $nameID) {

	$query="SELECT localid FROM nameidmapping WHERE (idp='$idp' AND sp='$sp' AND nameid='$nameID')";
	error_log("query...NameID... $query ...");
	$result=mysql_query($query);
	if (!$result) {
	   echo 'MySQL Error: ' . mysql_error() . "\n";
	   exit;
	}
	
	$num=mysql_numrows($result);
	
	
	$i=0;
	while ($i < $num) {
		return mysql_result($result,$i,"localid");;
	}
}

function spi_namemapping_mapNameIdToLocalId($idp, $sp, $nameID, $localId) {


	$query="INSERT INTO nameidmapping (idp, sp, nameid, localid) VALUES ('$idp', '$sp', '$nameID', '$localId')";
	$result=mysql_query($query);
	if (!$result) {
	   echo 'MySQL Error: ' . mysql_error() . "\n";
	   exit;
	}
}
?>
