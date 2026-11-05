# Deferred wiki migration for OpenAM #1156

Apply `wiki-how-to.patch` to the OpenAM wiki **only after** the OpenAM documentation deployment and the companion documentation-site navigation change have been published. Check the target pages and images on the live site first.

The patch is a review artifact; it does not change the wiki during the OpenAM build or deployment. It replaces migrated articles with links, updates the existing Home entries, and points How-To-Guides to the new guide. Existing wiki image files remain available for historical links.

Wiki repository: https://github.com/OpenIdentityPlatform/OpenAM.wiki.git

Source revision: `c68baf2f36df4bb0d3c0b3c7f96a43832b80e4f3`

In a clean wiki checkout containing that revision:

```sh
git apply --check /path/to/wiki-how-to.patch
git apply /path/to/wiki-how-to.patch
git diff --check
git diff
```

If the wiki has changed since this revision, review the intervening edits and refresh the corresponding article and patch before applying. Commit and publish the wiki change separately after review.

## Article inventory

| Wiki page | Documentation source in `src/main/asciidoc/how-to/` |
| --- | --- |
| `Adaptive-Authentication:-How-to-Improve-Security-Without-Annoying-Users.md` | `adaptive-authentication-how-to-improve-security-without-annoying-users.adoc` |
| `Authentication-in-a-Single‐Page-Application-via-OpenAM-using-OAuth2-OIDC-Protocol.md` | `authentication-in-a-single-page-application-via-openam-using-oauth2-oidc-protocol.adoc` |
| `Authentication-modules.md` | `authentication-modules.adoc` |
| `Captcha-module.md` | `captcha-module.adoc` |
| `Config-OpenAM-as-OAuth2-Service-Provider.md` | `config-openam-as-oauth2-service-provider.adoc` |
| `Configuring-authentication-in-OpenAM-via-Yandex-Cloud-using-the-SAML-protocol.md` | `configuring-authentication-in-openam-via-yandex-cloud-using-the-saml-protocol.adoc` |
| `How-OpenAM-Works-Simple-Explanation.md` | `how-openam-works-simple-explanation.adoc` |
| `How-To-Run-OpenAM-in-Kubernetes.md` | `how-to-run-openam-in-kubernetes.adoc` |
| `How-To-Setup-Active-Directory-Authentication-In-OpenAM.md` | `how-to-setup-active-directory-authentication-in-openam.adoc` |
| `How-to-Add-Authorization-and-Protect-Your-Application-With-OpenAM-and-OpenIG-Stack.md` | `how-to-add-authorization-and-protect-your-application-with-openam-and-openig-stack.adoc` |
| `How-to-Create-Custom-OpenAM-DataStore-Integration.md` | `how-to-create-custom-openam-datastore-integration.adoc` |
| `How-to-Customise-OpenAM.md` | `how-to-customise-openam.adoc` |
| `How-to-Devleop-a-Custom-Authentication-Module.md` | `how-to-develop-a-custom-authentication-module.adoc` |
| `How-to-Protect-Model-Context-Protocol-(MCP)-Servers-with-OpenAM-and-OpenIG.md` | `how-to-protect-model-context-protocol-mcp-servers-with-openam-and-openig.adoc` |
| `How-to-Setup-2FA-with-Google-Authenticator-in-OpenAM.md` | `how-to-setup-2fa-with-google-authenticator-in-openam.adoc` |
| `How-to-Setup-Active-Directory-Authentication-in-a-Spring-Boot-Application.md` | `how-to-setup-active-directory-authentication-in-a-spring-boot-application.adoc` |
| `How-to-Setup-OAuth2-OIDC-Federation-in-OpenAM.md` | `how-to-setup-oauth2-oidc-federation-in-openam.adoc` |
| `How-to-Setup-SAMLv2-Federation-in-OpenAM.md` | `how-to-setup-samlv2-federation-in-openam.adoc` |
| `How-to-Setup-Two-Factor-Authentication-in-OpenAM.md` | `how-to-setup-two-factor-authentication-in-openam.adoc` |
| `How-to-Setup-WebAuthn-Authentication-in-OpenAM.md` | `how-to-setup-webauthn-authentication-in-openam.adoc` |
| `How-to-Start-OpenAM-and-OpenDJ-in-Separate-Docker-Contaners.md` | `how-to-start-openam-and-opendj-in-separate-docker-containers.adoc` |
| `How-to-Use-Apache-Cassandra-as-User-and-Session-DataStore-in-OpenAM.md` | `how-to-use-apache-cassandra-as-user-and-session-datastore-in-openam.adoc` |
| `How-to-disable-XUI-by-default.md` | `how-to-disable-xui-by-default.adoc` |
| `How-to-make-OpenAM-log-more-verbose.md` | `how-to-make-openam-log-more-verbose.adoc` |
| `How-to-setup-Kerberos-Authentication-with-OpenAM.md` | `how-to-setup-kerberos-authentication-with-openam.adoc` |
| `Login-and-Password-Authentication.md` | `login-and-password-authentication.adoc` |
| `Login-via-"Госуслуги-РФ"-OAuth2-Identity-Provider.md` | `login-via-gosuslugi-oauth2-identity-provider.adoc` |
| `Login-via-Facebook-OAuth2-Identity-Provider.md` | `login-via-facebook-oauth2-identity-provider.adoc` |
| `Login-via-Odnoklassniki-OAuth2-Identity-Provider.md` | `login-via-odnoklassniki-oauth2-identity-provider.adoc` |
| `Login-via-VK-OAuth2-Identity-Provider.md` | `login-via-vk-oauth2-identity-provider.adoc` |
| `Migrate-OpenAM-to-Apache-Cassandra-without-Single-Point-of-Failure.md` | `migrate-openam-to-apache-cassandra-without-single-point-of-failure.adoc` |
| `OpenAM-Monitoring-Using-Prometheus.md` | `openam-monitoring-using-prometheus.adoc` |
| `OpenAM-QR-Authentication-Module.md` | `openam-qr-authentication-module.adoc` |
| `OpenAM-and-Spring-Boot-3-Integration-via-OIDC-OAuth2-Protocol.md` | `openam-and-spring-boot-3-integration-via-oidc-oauth2-protocol.adoc` |
| `OpenAM-and-Spring-Boot-3-Integration-via-OpenAM-Cookie.md` | `openam-and-spring-boot-3-integration-via-openam-cookie.adoc` |
| `OpenAM-and-Spring-Boot-3-Integration-via-SAMLv2-Protocol.md` | `openam-and-spring-boot-3-integration-via-samlv2-protocol.adoc` |
| `OpenAM-and-Zero-Trust:-Confirming-Critical-Operations.md` | `openam-and-zero-trust-confirming-critical-operations.adoc` |
| `OpenAM-vs-Keycloak.md` | `openam-vs-keycloak.adoc` |
| `Passwordless-Authenticaion-Methods.md` | `passwordless-authentication-methods.adoc` |
| `Persistent-Cookie-module.md` | `persistent-cookie-module.adoc` |
| `Quick-Start-Guide.md` | `quick-start-guide.adoc` |
| `SAML-Authentication-in-WordPress-via-OpenAM.md` | `saml-authentication-in-wordpress-via-openam.adoc` |
| `SAML-Authentication-in-Yandex-Cloud-via-OpenAM.md` | `saml-authentication-in-yandex-cloud-via-openam.adoc` |
| `SSO-Configuration:-OpenIG-as-SAML-Service-Provider-for-OpenAM.md` | `sso-configuration-openig-as-saml-service-provider-for-openam.adoc` |
| `Setup-Tracing-with--OpenTelemetry-and-Jaeger.md` | `setup-tracing-with-opentelemetry-and-jaeger.adoc` |
| `Stateful-vs-Stateless-Authentication.md` | `stateful-vs-stateless-authentication.adoc` |
| `TIP:-Bootstrap-OpenAM-and-OpenDJ-Docker-Containers.md` | `bootstrap-openam-and-opendj-docker-containers.adoc` |
| `TIP:-How-to-Enable-CORS.md` | `how-to-enable-cors.adoc` |
| `TIP:-How-to-Pass-Additional-System-Property-to-CATALINA_OPTS-to-OpenAM-Docker-Container.md` | `how-to-pass-additional-system-property-to-catalina-opts-to-openam-docker-container.adoc` |
| `TIP:-How-to-Persist-OpenAM-Docker-Container-Data-Between-Restarts.md` | `how-to-persist-openam-docker-container-data-between-restarts.adoc` |
| `TIP:-Quick-OpenAM-Docker-Configuration-From-a-Command-Line.md` | `quick-openam-docker-configuration-from-a-command-line.adoc` |
| `Using-Google-Authenticator-with-OpenAM.md` | `using-google-authenticator-with-openam.adoc` |
| `Using-Microsoft-Authenticator-with-OpenAM.md` | `using-microsoft-authenticator-with-openam.adoc` |
| `What-is-Single-Sign-On-and-How-does-it-Work.md` | `what-is-single-sign-on-and-how-does-it-work.adoc` |

The Russian Yandex Cloud SP article points to the English SAML Authentication in Yandex Cloud tutorial covering the same flow. The separate English Yandex Cloud IdP tutorial remains a separate article.

Home, Documentation, How-To-Guides, sidebar/footer, and existing guide landing pages are navigation rather than new tutorials. The one-line external benchmark link remains on Home; no third-party article is copied. The two authenticator tutorials and the older 2FA tutorials remain separate because they describe distinct workflows.

## Migration details

- Preserves code listings and the HTML tables with merged cells; prefixes section anchors by article to avoid duplicate PDF identifiers.
- Copies only referenced wiki images into `images/how-to/` and rewrites links between migrated articles.
- Retains labels that previously linked to nonexistent wiki pages as plain text; links ssoadm references to the existing CLI reference. Removes the unfinished Russian-version TODO link.
- Fixes the Kubernetes service lookup (`openam`) and a `.pngg` image URL typo. Marks the customization example as targeting OpenAM 14.7.1.
- Keeps existing example versions unless explicitly noted; publication does not imply every external integration was retested.

The combined guide contains 222 screenshots (391 million pixels). PDF generation was verified with `MAVEN_OPTS=-Xmx4g`; the build, deployment, and release workflows use this heap limit because 2 GB was insufficient.
