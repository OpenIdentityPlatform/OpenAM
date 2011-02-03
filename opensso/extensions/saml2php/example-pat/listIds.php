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
 * $Id: listIds.php,v 1.1 2007/05/22 05:38:38 andreas1980 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
?>
<html>
    <head>
        <title>Database Content</title>
    </head>
    <body>
        <h3>Users</h3>
        <table border="1">
<?php
$user="dbuser";
$password="dbpassword";
$database="lightbulb";

mysql_connect("localhost",$user,$password) or die("Not connected : " . mysql_error() . "\n");

mysql_select_db($database) or die( "Unable to select database : " . mysql_error() . "\n");

$query="SELECT * FROM users";
$result=mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

echo "<tr><th>userid</th><th>passwordhash</th><th>username</th></tr>";

$num=mysql_numrows($result);

$i=0;
while ($i < $num) {
    echo "<tr><td>" . mysql_result($result,$i,"userid") . "</td><td>" . mysql_result($result,$i,"passwordhash") . "</td><td>" . mysql_result($result,$i,"username") . "</td></tr>";
    $i++;
}
?>
        </table>
        <h3>Name ID Mapping</h3>
        <table border="1">
<?php
$query="SELECT * FROM nameidmapping";
$result=mysql_query($query);
if (!$result) {
   echo 'MySQL Error: ' . mysql_error() . "\n";
   exit;
}

echo "<tr><th>idp</th><th>sp</th><th>nameid</th><th>localid</th></tr>";

$num=mysql_numrows($result);

$i=0;
while ($i < $num) {
    echo "<tr><td>" . mysql_result($result,$i,"idp") . "</td><td>" . mysql_result($result,$i,"sp") . "</td><td>" . mysql_result($result,$i,"nameid") . "</td><td>" . mysql_result($result,$i,"localid") . "</td></tr>";
    $i++;
}

mysql_close();
?>
        </table>
    </body>
</html>
