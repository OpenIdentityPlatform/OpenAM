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
 * $Id: makedb.php,v 1.1 2007/05/22 05:38:39 andreas1980 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

$user="dbuser";
$password="dbpassword";
$database="lightbulb";

$link = mysql_connect("localhost",$user,$password);
if (!$link) {
    die("Not connected : " . mysql_error() . "\n");
}
$sql = "SHOW TABLES FROM $database";
$result = mysql_query($sql);

mysql_free_result($result);
@mysql_select_db($database) or die( "Unable to select database : " . mysql_error() . "\n");

$query="DROP TABLE IF EXISTS users";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query="CREATE TABLE users (userid varchar(80) NOT NULL, passwordhash varchar(80) NOT NULL, username varchar(80) NOT NULL,PRIMARY KEY (userid))";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$passwordhash = sha1( "password" );

$query = "INSERT INTO users VALUES ('johns', '".$passwordhash."', 'John Smith')";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query = "INSERT INTO users VALUES ('admin', '".$passwordhash."', 'Administrator')";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query="SELECT * FROM users";
$result=mysql_query($query);

$num=mysql_numrows($result);

$i=0;
while ($i < $num) {

$userid=mysql_result($result,$i,"userid");
$passwordhash=mysql_result($result,$i,"passwordhash");
$username=mysql_result($result,$i,"username");

echo "$userid $passwordhash $username\n";

$i++;
}


$query="DROP TABLE IF EXISTS nameidmapping";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query="CREATE TABLE nameidmapping (idp varchar(80) NOT NULL,sp varchar(80) NOT NULL,nameid varchar(80) NOT NULL,localid varchar(80) NOT NULL,PRIMARY KEY (idp,sp,localid))";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query = "INSERT INTO nameidmapping VALUES ('http://amfmdemo.example.com', 'http://patlinux.red.iplanet.com', 'YgolvKBPsL4ABSrdOpilovLnVq+X', 'johns')";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query = "INSERT INTO nameidmapping VALUES ('http://amfmdemo.example.com', 'http://patlinux.red.iplanet.com', 'GsIcQLU2JvgDJ0ov2+SXVf29ncGF', 'admin')";
$result = mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

$query="SELECT * FROM nameidmapping";
$result=mysql_query($query);

$num=mysql_numrows($result);

$i=0;
while ($i < $num) {

$idp=mysql_result($result,$i,"idp");
$sp=mysql_result($result,$i,"sp");
$nameid=mysql_result($result,$i,"nameid");
$localid=mysql_result($result,$i,"localid");

echo "$idp $sp $nameid $localid\n";

$i++;
}

mysql_close();

?>
