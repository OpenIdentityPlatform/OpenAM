<!doctype html>
<html lang="en">
<head>
    <title>OAuth2 Authorization Server</title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" charset="utf-8"/>
    <meta name="description" content="OAuth2 Error">
    <link rel="stylesheet" href="resources/styles.css?v=1.0" type="text/css" media="screen" charset="utf-8">
    <!--[if lt IE 9]>
    <![endif]-->
</head>
<body>
<div id="container">
    <div id="header">
        <h2>OAuth authorization page</h2>
        <section id="intro">
            <h2>Application requesting scope</h2>

            <p>Client ClientId = ${clientId?if_exists} CB = ${clientDescription?if_exists} wants to get access to your
                information.</p>
        </section>
    </div>
    <aside>
        <form action="${target!'.'}" method="post">
            <h4>The following private info is requested</h4>
        <#if requestingScopes??>
            <#list requestingScopes as r><input type="checkbox" name="scope" value="${r}" checked/>
                <b>${r}</b><br/>
            </#list>
        </#if>
        <#if grantedScopes??>
            <#if grantedScopes?has_content>
                <hr/>
                <h4>Previously approved scopes</h4>
                <#list grantedScopes as g><input type="checkbox" name="scope" value="${g}" checked/>
                    <b>${g}</b><br/>
                </#list>
            </#if>
        </#if>
            <br/>
            <!-- Optional parameters -->
        <#if realm??>
            <input type="hidden" name="realm" value="${realm}"/>
        </#if>
        <#if redirect_uri??>
            <input type="hidden" name="redirect_uri" value="${redirect_uri}"/>
        </#if>
        <#if scope??>
            <input type="hidden" name="scope" value="${scope}"/>
        </#if>
        <#if state??>
            <input type="hidden" name="state" value="${state}"/>
        </#if>
            <!-- Required parameters -->
            <input type="hidden" name="response_type" value="${response_type}"/>
            <input type="hidden" name="client_id" value="${client_id}"/>
            <!-- Custom parameters -->
            <!--input type="hidden" name="decision" value="allow"/>
            <input type="submit" value="Allow access" class="button save"/>
            <input type="hidden" name="decision" value="deny"/>
            <input type="submit" value="No Thanks" class="button"/-->
            <input type="submit" name="decision" class="button gray" value="Allow"/>
            <input type="submit" name="decision" class="button gray" value="Deny"/>
        </form>
    </aside>
</div>
</body>
</html>