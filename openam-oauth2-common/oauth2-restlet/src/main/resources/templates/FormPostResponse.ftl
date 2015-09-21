<!DOCTYPE html>
<!--
~ The contents of this file are subject to the terms of the Common Development and
~ Distribution License (the License). You may not use this file except in compliance with the
~ License.
~
~ You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
~ specific language governing permission and limitations under the License.
~
~ When distributing Covered Software, include this CDDL Header Notice in each file and include
~ the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
~ Header, with the fields enclosed by brackets [] replaced by your own identifying
~ information: "Portions copyright [year] [name of copyright owner]".
~
~ Copyright 2015 ForgeRock AS.
-->
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="description" content="OAuth 2.0 Form Post">
        <title>Submit This Form</title>
    </head>
    <body onload="javascript:document.forms[0].submit()">
        <form method="post" action="${redirectUri}">
            <#list formValues?keys as key><input type="hidden" name="${key}" value="${formValues[key]}"/></#list>
        </form>
    </body>
</html>