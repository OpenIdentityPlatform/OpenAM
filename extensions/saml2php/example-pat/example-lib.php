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
 * $Id: example-lib.php,v 1.1 2007/05/22 05:38:38 andreas1980 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

session_start();


/*
 * Inizialize database used for user storage...
 */






/*
 * This is a function that returns the URL that you are on right now.
 */
function selfURL() {
	$s = empty($_SERVER["HTTPS"]) ? ''
		: ($_SERVER["HTTPS"] == "on") ? "s"
		: "";
	$protocol = strleft(strtolower($_SERVER["SERVER_PROTOCOL"]), "/").$s;
	$port = ($_SERVER["SERVER_PORT"] == "80") ? ""
		: (":".$_SERVER["SERVER_PORT"]);
	return $protocol."://".$_SERVER['SERVER_NAME'].$port.$_SERVER['REQUEST_URI'];
}
// Helper function used in selfURL()
function strleft($s1, $s2) {
	return substr($s1, 0, strpos($s1, $s2));
}




function federatedLogin()
{
    return isset($_SESSION['SamlResponse']);
}

function authenticateLocalUser($userid, $password) {

    //$passwordhash = sha1( $password );

    $query="SELECT username FROM users WHERE (userid='$userid' AND password='$password')";
    $result=mysql_query($query);
    if (!$result) {
        echo 'MySQL Error: ' . mysql_error() . "\n";
        exit;
    }



    $username=NULL;

    $num=mysql_numrows($result);
    if ( $num == 1 )
    {
        $username = mysql_result($result,0,"username");
    }

    return $username;
}

function getUserName($userid)
{

    $query="SELECT username FROM users WHERE (userid='$userid')";
    $result=mysql_query($query);
    if (!$result) {
        echo 'MySQL Error: ' . mysql_error() . "\n";
        exit;
    }



    $username=NULL;

    $num=mysql_numrows($result);
    if ( $num == 1 )
    {
        $username = mysql_result($result,0,"username");
    }

    return $username;
}
?>
