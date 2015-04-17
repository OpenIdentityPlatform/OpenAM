<!doctype html>
<!--
  ~ DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  ~
  ~ Copyright 2012-2015 ForgeRock AS.
  ~
  ~ The contents of this file are subject to the terms
  ~ of the Common Development and Distribution License
  ~ (the License). You may not use this file except in
  ~ compliance with the License.
  ~
  ~ You can obtain a copy of the License at
  ~ http://forgerock.org/license/CDDLv1.0.html
  ~ See the License for the specific language governing
  ~ permission and limitations under the License.
  ~
  ~ When distributing Covered Code, include this CDDL
  ~ Header Notice in each file and include the License file
  ~ at http://forgerock.org/license/CDDLv1.0.html
  ~ If applicable, add the following below the CDDL Header,
  ~ with the fields enclosed by brackets [] replaced by
  ~ your own identifying information:
  ~ "Portions Copyrighted [year] [name of copyright owner]"
  ~
  ~ Portions Copyrighted 2014 Nomura Research Institute, Ltd
  -->
<html lang="en">
    <head>
        <title>OAuth2 Authorization Server</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" charset="utf-8"/>
        <meta name="description" content="OAuth2 Authorization">
        <#if xui>
            <style>
                .hidden {
                    display: none;
                }
                ul.scopes {
                    margin-top: -10px;
                }
                ul.scopes li {
                    padding-top: 10px;
                    padding-left: 20px;
                    font-size: 0.9em;
                    list-style-position: inside;
                    list-style-type: disc;
                }
                hr {
                    margin: 15px 0;
                }
            </style>
            <script type="text/javascript">
                realm = "${(realm!"/")?js_string}";
                less = {
                rootPath: "${baseUrl?js_string}/XUI"
            };
            </script>
            <script data-main="${baseUrl?html}/XUI/main-authorize" src="${baseUrl?html}/XUI/libs/requirejs-2.1.14-min.js"></script>
        </#if>
    </head>
    <body>
        <div id="wrapper">
            <div id="login-base" class="container hidden">
                <#if xui>
                <div id="header" class="clearfix">
                    <div id="logo" class="main-logo-holder">
                        <a href="#" title="ForgeRock">
                            <img src="${baseUrl?html}/XUI/images/login-logo.png" style="width:210px; height:104px;" alt="ForgeRock">
                        </a>
                    </div>
                </div>
              </#if>
              <div id="content" class="content">
                  <div id="login-container" class="col-sm-6 col-sm-offset-3">
                      <div id="oauth-header" class="page-header">
                          <section id="intro" class="text-center">
                              <h1>${display_name?if_exists}</h1>
                              <div>${display_description?if_exists} </div>
                          </section>
                      </div>
                      <form action="${target!'.'}" method="post" class="clearfix">
                          <div>This application is requesting the following private information:</div>
                          <#if display_scope??>
                          <br/>
                          <ul class="scopes">
                              <#list display_scope as r>
                              <li>${r}</li>
                              </#list>
                          </ul>
                          </#if>
                          <br/>
                          <!-- Optional parameters -->
                          <#if realm??>
                              <input type="hidden" name="realm" value="${realm?html}"/>
                          </#if>
                          <#if redirect_uri??>
                              <input type="hidden" name="redirect_uri" value="${redirect_uri?html}"/>
                          </#if>
                          <#if scope??>
                              <input type="hidden" name="scope" value="${scope?html}"/>
                          </#if>
                          <#if state??>
                              <input type="hidden" name="state" value="${state?html}"/>
                          </#if>
                          <#if nonce??>
                              <input type="hidden" name="nonce" value="${nonce?html}"/>
                          </#if>
                          <#if acr??>
                              <input type="hidden" name="acr" value="${acr?html}"/>
                          </#if>
                          <!-- Required parameters -->
                          <input type="hidden" name="response_type" value="${response_type}"/>
                          <input type="hidden" name="client_id" value="${client_id}"/>

                          <div class="checkbox">
                              <label>
                                  <input type="checkbox" name="save_consent"/> Save Consent
                              </label>
                          </div>

                          <div class="pull-right">
                              <input type="submit" name="decision" class="btn btn-primary" value="Allow"/>
                              <input type="submit" name="decision" class="btn btn-default" value="Deny"/>
                          </div>
                      </form>
                    </div>
                </div>
            </div>
        </div>
        <#if xui>
        <footer id="footer" class="footer">
            <div class="container">
                <p>
                    <a href="mailto: info@forgerock.com">info@forgerock.com</a>
                    <br>
                    Copyright © 2010-15 ForgeRock AS, all rights reserved.
                </p>
            </div>
        </footer>
        </#if>
    </body>
</html>
