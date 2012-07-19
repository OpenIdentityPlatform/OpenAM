<!doctype html>
<html lang="en">
<head>
    <title>OAuth2 Error Page</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" charset="utf-8"/>
    <meta name="description" content="OAuth2 Error">
    <link rel="stylesheet" href="resources/styles.css?v=1.0" type="text/css" media="screen" charset="utf-8">
    <!--[if lt IE 9]>
    <![endif]-->
</head>
<body>
<p><b>Error: </b>
<#if error??>
    <#if error_uri??>
        <a href="${error_uri}">${error}</a>
    <#else>
    ${error}
    </#if>
</#if>
</p>

<p><b>Description: </b>
<#if error_description??>${error_description}</#if>
</p>
</body>
</html>