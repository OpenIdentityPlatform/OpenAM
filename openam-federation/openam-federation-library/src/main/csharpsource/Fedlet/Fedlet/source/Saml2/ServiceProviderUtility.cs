/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2009-2010 Sun Microsystems Inc. All Rights Reserved
 * 
 * The contents of this file are subject to the terms
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
 * $Id: ServiceProviderUtility.cs,v 1.9 2010/01/26 01:20:14 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

using System;
using System.Collections;
using System.Collections.Specialized;
using System.Globalization;
using System.IO;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Web;
using System.Xml;
using Sun.Identity.Common;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;
using System.Collections.Generic;
using System.Configuration;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography.Xml;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Utility class to encapsulate configuration and metadata management
    /// along with convenience methods for retrieveing SAML2 objects.
    /// </summary>
    public class ServiceProviderUtility
    {
        #region Members

        /// <summary>
        /// Home folder containing configuration and metadata.
        /// </summary>
        private string homeFolder;

        #endregion

        #region Constructors

        /// <summary>
        /// Initializes a new instance of the ServiceProviderUtility class
        /// using the App_Data folder for the application as the default home
        /// folder for configuration and metadata.
        /// </summary>
        /// <param name="context">HttpContext used for reading application data.</param>
        public ServiceProviderUtility(HttpContext context)
        {
             DirectoryInfo dirInfoCheck = new DirectoryInfo(context.Server.MapPath(@"/App_Data"));
             if (dirInfoCheck.Exists)
             {
                 this.Initialize(context.Server.MapPath(@"/App_Data"));
             }
             else
             {
                 this.Initialize(context.Server.MapPath(@"App_Data"));
             }
        }

        /// <summary>
        /// Initializes a new instance of the ServiceProviderUtility class
        /// using the given home folder for configuration and metadata.
        /// </summary>
        /// <param name="homeFolder">Home folder containing configuration and metadata.</param>
        public ServiceProviderUtility(string homeFolder)
        {
            this.Initialize(homeFolder);
        }

        #endregion

        #region Properties
        /// <summary>
        /// Gets the service provider configured for the hosted application.
        /// </summary>
        public ServiceProvider ServiceProvider { get; private set; }

        /// <summary>
        /// Gets the collection of identity providers configured for the
        /// hosted application where the key is the identity provider's
        /// entity ID.
        /// </summary>
        public Hashtable IdentityProviders { get; private set; }

        /// <summary>
        /// Gets the collection of circle-of-trusts configured for the
        /// hosted application where the key is the circle-of-trust's
        /// "cot-name".
        /// </summary>
        public Hashtable CircleOfTrusts { get; private set; }
        #endregion

        #region Public Retrieval Methods

        /// <summary>
        /// Retrieve the ArtifactResponse object with the given SAMLv2 
        /// artifact.
        /// </summary>
        /// <param name="artifact">SAMLv2 artifact</param>
        /// <returns>ArtifactResponse object</returns>
        public ArtifactResponse GetArtifactResponse(Artifact artifact)
        {
            ArtifactResolve artifactResolve = new ArtifactResolve(this.ServiceProvider, artifact);
            ArtifactResponse artifactResponse = null;

            IdentityProvider idp = this.GetIdpFromArtifact(artifact);
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpNotDeterminedFromArtifact);
            }

            string artifactResolutionSvcLoc = idp.GetArtifactResolutionServiceLocation(Saml2Constants.HttpSoapProtocolBinding);
            if (artifactResolutionSvcLoc == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpArtifactResSvcLocNotDefined);
            }

            HttpWebRequest request = null;
            HttpWebResponse response = null;
            try
            {
                Uri artifactResolutionSvcUri = new Uri(artifactResolutionSvcLoc);
                if (artifactResolutionSvcUri.Scheme == "https")
                {
                    System.Net.ServicePointManager.ServerCertificateValidationCallback +=
                    delegate(object sender, System.Security.Cryptography.X509Certificates.X509Certificate certificate,
                    System.Security.Cryptography.X509Certificates.X509Chain chain,
                    System.Net.Security.SslPolicyErrors sslPolicyErrors)
                    {
                        if (ServiceProvider.TrustAllCerts
                            || sslPolicyErrors.HasFlag(System.Net.Security.SslPolicyErrors.None))
                            {
                                return true;
                            }

                        StringBuilder logErrorMessage = new StringBuilder();
                        logErrorMessage.Append("SSLPolicyError: ").Append(sslPolicyErrors);
                        FedletLogger.Error(logErrorMessage.ToString());
                        return false;
                    };
                }

                request = (HttpWebRequest)WebRequest.Create(artifactResolutionSvcUri);

                string authCertAlias = ConfigurationManager.AppSettings[Saml2Constants.MutualAuthCertAlias];
                if (artifactResolutionSvcUri.Scheme == "https" && !string.IsNullOrWhiteSpace(authCertAlias))
                {
                    X509Certificate2 cert = FedletCertificateFactory.GetCertificateByFriendlyName(authCertAlias);
                    if (cert != null)
                    {
                        request.ClientCertificates.Add(cert);
                    }
                }

                XmlDocument artifactResolveXml = (XmlDocument)artifactResolve.XmlDom;

                if (idp.WantArtifactResolveSigned)
                {
                    if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                    {
                        throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                    }
                    else
                    {
                        Saml2Utils.SignXml(
                            this.ServiceProvider.SigningCertificateAlias,
                            artifactResolveXml,
                            artifactResolve.Id,
                            true,
                            idp.RequestedSignatureSigningAlgorithm,
                            idp.RequestedDigestMethod,
                            this.ServiceProvider);
                    }
                }

                string soapMessage = Saml2Utils.CreateSoapMessage(artifactResolveXml.InnerXml);

                byte[] byteArray = Encoding.UTF8.GetBytes(soapMessage);
                request.ContentType = "text/xml";
                request.ContentLength = byteArray.Length;
                request.AllowAutoRedirect = false;
                request.Method = "POST";

                Stream requestStream = request.GetRequestStream();
                requestStream.Write(byteArray, 0, byteArray.Length);
                requestStream.Close();

                StringBuilder logMessage = new StringBuilder();
                logMessage.Append("ArtifactResolve:\r\n").Append(artifactResolveXml.OuterXml);
                FedletLogger.Info(logMessage.ToString());

                response = (HttpWebResponse)request.GetResponse();
                StreamReader streamReader = new StreamReader(response.GetResponseStream());
                string responseContent = streamReader.ReadToEnd();
                streamReader.Close();

                XmlDocument soapResponse = new XmlDocument();
                soapResponse.PreserveWhitespace = true;
                soapResponse.LoadXml(responseContent);

                XmlNamespaceManager soapNsMgr = new XmlNamespaceManager(soapResponse.NameTable);
                soapNsMgr.AddNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
                soapNsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                soapNsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                soapNsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");

                XmlElement root = soapResponse.DocumentElement;
                XmlNode responseXml = root.SelectSingleNode("/soap:Envelope/soap:Body/samlp:ArtifactResponse", soapNsMgr);
                string artifactResponseXml = responseXml.OuterXml;

                artifactResponse = new ArtifactResponse(artifactResponseXml);

                if (artifactResolve.Id != artifactResponse.InResponseTo)
                {
                    throw new Saml2Exception(Resources.ArtifactResolutionInvalidInResponseTo);
                }
            }
            catch (WebException we)
            {
                throw new ServiceProviderUtilityException(Resources.ArtifactResolutionWebException, we);
            }
            finally
            {
                if (response != null)
                {
                    response.Close();
                }
            }

            return artifactResponse;
        }

        /// <summary>
        /// Retrieve the AuthnResponse object found within the HttpRequest
        /// in the context of the HttpContext, performing validation of
        /// the AuthnResponse prior to returning to the user.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <returns>AuthnResponse object</returns>
        public AuthnResponse GetAuthnResponse(HttpContext context)
        {
            ArtifactResponse artifactResponse = null;
            AuthnResponse authnResponse = null;
            ICollection authnRequests = AuthnRequestCache.GetSentAuthnRequests(context);
            HttpRequest request = context.Request;

            // Check if a saml response was received...
            if (string.IsNullOrEmpty(request[Saml2Constants.ResponseParameter])
                && string.IsNullOrEmpty(request[Saml2Constants.ArtifactParameter]))
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityNoSamlResponseReceived);
            }

            // Obtain AuthnResponse object from either HTTP-POST or HTTP-Artifact
            if (request[Saml2Constants.ResponseParameter] != null)
            {
                string samlResponse = Saml2Utils.ConvertFromBase64(request[Saml2Constants.ResponseParameter]);
                authnResponse = new AuthnResponse(samlResponse);

                XmlDocument xmlDoc = (XmlDocument)authnResponse.XmlDom;
                StringBuilder logMessage = new StringBuilder();
                logMessage.Append("AuthnResponse:\r\n").Append(xmlDoc.OuterXml);
                FedletLogger.Info(logMessage.ToString());
            }
            else if (request[Saml2Constants.ArtifactParameter] != null)
            {
                Artifact artifact = new Artifact(request[Saml2Constants.ArtifactParameter]);
                artifactResponse = this.GetArtifactResponse(artifact);
                authnResponse = artifactResponse.AuthnResponse;

                XmlDocument xmlDoc = (XmlDocument)artifactResponse.XmlDom;
                StringBuilder logMessage = new StringBuilder();
                logMessage.Append("ArtifactResponse:\r\n").Append(xmlDoc.OuterXml);
                FedletLogger.Info(logMessage.ToString());
            }

            string prevAuthnRequestId = authnResponse.InResponseTo;

            try
            {
                if (artifactResponse != null)
                {
                    this.ValidateForArtifact(artifactResponse, authnRequests);
                }
                else
                {
                    this.ValidateForPost(authnResponse, authnRequests);
                }
            }
            catch (Saml2Exception se)
            {
                // log and throw again...
                XmlDocument authnResponseXml = (XmlDocument)authnResponse.XmlDom;
                StringBuilder logMessage = new StringBuilder();
                logMessage.Append(se.Message).Append("\r\n").Append(authnResponseXml.InnerXml);
                FedletLogger.Warning(logMessage.ToString());
                throw;
            }
            finally
            {
                AuthnRequestCache.RemoveSentAuthnRequest(context, prevAuthnRequestId);
            }

            // If we already decrypted the XML because of an encrypted assertion,
            // then this method won't be executed
            if (authnResponse.IsEncrypted())
            {
                authnResponse.Decrypt(ServiceProvider);
            }
            return authnResponse;
        }

        /// <summary>
        /// Retrieve the LogoutRequest object found within the HttpRequest
        /// in the context of the HttpContext, performing validation of
        /// the LogoutRequest prior to returning to the user.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <returns>LogoutRequest object</returns>
        public LogoutRequest GetLogoutRequest(HttpContext context)
        {
            HttpRequest request = context.Request;
            string samlRequest = null;

            // Obtain the LogoutRequest object...
            if (request.HttpMethod == "GET")
            {
                samlRequest = Saml2Utils.ConvertFromBase64Decompress(request[Saml2Constants.RequestParameter]);
            }
            else if (request.HttpMethod == "POST")
            {
                // something posted...check if soap vs form post
                if (!String.IsNullOrEmpty(request[Saml2Constants.RequestParameter]))
                {
                    samlRequest = Saml2Utils.ConvertFromBase64(request[Saml2Constants.RequestParameter]);
                }
                else
                {
                    StreamReader reader = new StreamReader(request.InputStream);
                    string requestContent = reader.ReadToEnd();

                    XmlDocument soapRequest = new XmlDocument();
                    soapRequest.PreserveWhitespace = true;
                    soapRequest.LoadXml(requestContent);

                    XmlNamespaceManager soapNsMgr = new XmlNamespaceManager(soapRequest.NameTable);
                    soapNsMgr.AddNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
                    soapNsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                    soapNsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                    soapNsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");

                    XmlElement root = soapRequest.DocumentElement;
                    XmlNode requestXml = root.SelectSingleNode("/soap:Envelope/soap:Body/samlp:LogoutRequest", soapNsMgr);
                    samlRequest = requestXml.OuterXml;
                }
            }

            // Check if a saml request was received...
            if (samlRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityNoSamlRequestReceived);
            }

            LogoutRequest logoutRequest = new LogoutRequest(samlRequest);

            XmlDocument xmlDoc = (XmlDocument)logoutRequest.XmlDom;
            StringBuilder logMessage = new StringBuilder();
            logMessage.Append("LogoutRequest:\r\n").Append(xmlDoc.OuterXml);
            FedletLogger.Info(logMessage.ToString());

            try
            {
                if (request.HttpMethod == "GET")
                {
                    string queryString
                        = request.RawUrl.Substring(request.RawUrl.IndexOf("?", StringComparison.Ordinal) + 1);
                    FedletLogger.Info("LogoutRequest query string:\r\n" + queryString);
                    this.ValidateForRedirect(logoutRequest, queryString);
                }
                else
                {
                    this.ValidateForPost(logoutRequest);
                }
            }
            catch (Saml2Exception se)
            {
                // log and throw again...
                logMessage = new StringBuilder();
                logMessage.Append(se.Message).Append("\r\n").Append(xmlDoc.InnerXml);
                FedletLogger.Warning(logMessage.ToString());
                throw;
            }

            return logoutRequest;
        }

        /// <summary>
        /// Retrieve the LogoutResponse object found within the HttpRequest
        /// in the context of the HttpContext, performing validation of
        /// the LogoutResponse prior to returning to the user.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <returns>LogoutResponse object</returns>
        public LogoutResponse GetLogoutResponse(HttpContext context)
        {
            LogoutResponse logoutResponse = null;
            HttpRequest request = context.Request;

            // Check if a saml response was received...
            if (String.IsNullOrEmpty(request[Saml2Constants.ResponseParameter]))
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityNoSamlResponseReceived);
            }

            // Obtain the LogoutRequest object...
            if (request.HttpMethod == "GET")
            {
                string samlResponse = Saml2Utils.ConvertFromBase64Decompress(request[Saml2Constants.ResponseParameter]);
                logoutResponse = new LogoutResponse(samlResponse);
            }
            else
            {
                string samlResponse = Saml2Utils.ConvertFromBase64(request[Saml2Constants.ResponseParameter]);
                logoutResponse = new LogoutResponse(samlResponse);
            }

            XmlDocument xmlDoc = (XmlDocument)logoutResponse.XmlDom;
            StringBuilder logMessage = new StringBuilder();
            logMessage.Append("LogoutResponse:\r\n").Append(xmlDoc.OuterXml);
            FedletLogger.Info(logMessage.ToString());

            string prevLogoutRequestId = logoutResponse.InResponseTo;
            try
            {
                if (request.HttpMethod == "GET")
                {
                    string queryString
                        = request.RawUrl.Substring(request.RawUrl.IndexOf("?", StringComparison.Ordinal) + 1);
                    FedletLogger.Info("LogoutResponse query string:\r\n" + queryString);
                    this.ValidateForRedirect(logoutResponse, LogoutRequestCache.GetSentLogoutRequests(context), queryString);
                }
                else
                {
                    this.ValidateForPost(logoutResponse, LogoutRequestCache.GetSentLogoutRequests(context));
                }
            }
            catch (Saml2Exception se)
            {
                // log and throw again...
                logMessage = new StringBuilder();
                logMessage.Append(se.Message).Append("\r\n").Append(xmlDoc.InnerXml);
                FedletLogger.Warning(logMessage.ToString());
                throw;
            }
            finally
            {
                LogoutRequestCache.RemoveSentLogoutRequest(context, prevLogoutRequestId);
            }

            return logoutResponse;
        }

        /// <summary>
        /// Gets the HTML for use of submitting the AuthnRequest with POST.
        /// </summary>
        /// <param name="authnRequest">
        /// AuthnRequest to packaged for a POST.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameVallueCollection of additional parameters.
        /// </param>
        /// <returns>
        /// HTML with auto-form submission with POST of the AuthnRequest
        /// </returns>
        public string GetAuthnRequestPostHtml(AuthnRequest authnRequest, string idpEntityId, NameValueCollection parameters)
        {
            if (authnRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.AuthnRequestIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            string ssoPostLocation = idp.GetSingleSignOnServiceLocation(Saml2Constants.HttpPostProtocolBinding);
            if (ssoPostLocation == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleSignOnSvcLocNotDefined);
            }

            string relayState = null;
            if (parameters != null && !string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                relayState = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(relayState, this.ServiceProvider.RelayStateUrlList);
            }


            XmlDocument authnRequestXml = (XmlDocument)authnRequest.XmlDom;
            if (this.ServiceProvider.AuthnRequestsSigned || idp.WantAuthnRequestsSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    Saml2Utils.SignXml(
                        this.ServiceProvider.SigningCertificateAlias,
                        authnRequestXml,
                        authnRequest.Id,
                        true,
                        idp.RequestedSignatureSigningAlgorithm,
                        idp.RequestedDigestMethod,
                        this.ServiceProvider);
                    FedletLogger.Info("Signed AuthnRequest:\r\n" + authnRequestXml.InnerXml);
                }
            }

            string packagedAuthnRequest = Saml2Utils.ConvertToBase64(authnRequestXml.InnerXml);
            string inputFieldFormat = "<input type=\"hidden\" name=\"{0}\" value=\"{1}\" />";

            StringBuilder html = new StringBuilder();
            html.Append("<html><head><title>OpenAM - SP initiated SSO</title></head>");
            html.Append("<body onload=\"document.forms[0].submit();\">");
            html.Append("<form method=\"post\" action=\"");
            html.Append(ssoPostLocation);
            html.Append("\">");
            html.Append("<input type=\"hidden\" name=\"");
            html.Append(Saml2Constants.RequestParameter);
            html.Append("\" value=\"");
            html.Append(packagedAuthnRequest);
            html.Append("\" />");

            if (!string.IsNullOrEmpty(relayState))
            {
                html.Append(string.Format(
                                          CultureInfo.InvariantCulture,
                                          inputFieldFormat,
                                          Saml2Constants.RelayState,
                                          HttpUtility.HtmlEncode(relayState)));
            }

            html.Append("</form>");
            html.Append("</body>");
            html.Append("</html>");

            return html.ToString();
        }

        /// <summary>
        /// Gets the AuthnRequest location along with querystring parameters 
        /// to be used for actual browser requests.
        /// </summary>
        /// <param name="authnRequest">
        /// AuthnRequest to packaged for a redirect.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameVallueCollection of additional parameters.
        /// </param>
        /// <returns>
        /// URL with query string parameter for the specified IDP.
        /// </returns>
        public string GetAuthnRequestRedirectLocation(AuthnRequest authnRequest, string idpEntityId, NameValueCollection parameters)
        {
            if (authnRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.AuthnRequestIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            string ssoRedirectLocation = idp.GetSingleSignOnServiceLocation(Saml2Constants.HttpRedirectProtocolBinding);
            if (ssoRedirectLocation == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleSignOnSvcLocNotDefined);
            }

            string packagedAuthnRequest = Saml2Utils.CompressConvertToBase64UrlEncode(authnRequest.XmlDom);
            string queryString = Saml2Constants.RequestParameter + "=" + packagedAuthnRequest;

            if (parameters != null && !string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                string relayState = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(relayState, this.ServiceProvider.RelayStateUrlList);
                queryString += "&" + Saml2Constants.RelayState;
                queryString += "=" + HttpUtility.UrlEncode(relayState);
            }

            if (this.ServiceProvider.AuthnRequestsSigned || idp.WantAuthnRequestsSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    queryString += "&" + Saml2Constants.SignatureAlgorithm;
                    queryString += "=" + HttpUtility.UrlEncode(idp.RequestedSignatureSigningAlgorithm);
                    queryString = Saml2Utils.SignQueryString(this.ServiceProvider.SigningCertificateAlias, queryString);
                }
            }

            StringBuilder redirectUrl = new StringBuilder();
            redirectUrl.Append(ssoRedirectLocation);
            redirectUrl.Append(Saml2Utils.GetQueryStringDelimiter(ssoRedirectLocation));
            redirectUrl.Append(queryString);

            FedletLogger.Info("AuthnRequest via Redirect:\r\n" + redirectUrl);

            return redirectUrl.ToString();
        }

        /// <summary>
        /// Gets the HTML for use of submitting the LogoutRequest with POST.
        /// </summary>
        /// <param name="logoutRequest">
        /// LogoutRequest to packaged for a POST.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameVallueCollection of additional parameters.
        /// </param>
        /// <returns>
        /// HTML with auto-form submission with POST of the LogoutRequest
        /// </returns>
        public string GetLogoutRequestPostHtml(LogoutRequest logoutRequest, string idpEntityId, NameValueCollection parameters)
        {
            if (logoutRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityLogoutRequestIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            string sloPostLocation = idp.GetSingleLogoutServiceLocation(Saml2Constants.HttpPostProtocolBinding);
            if (sloPostLocation == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleLogoutSvcLocNotDefined);
            }

            string relayState = null;
            if (parameters != null && !string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                relayState = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(relayState, this.ServiceProvider.RelayStateUrlList);
            }

            XmlDocument logoutRequestXml = (XmlDocument)logoutRequest.XmlDom;

            if (idp.WantLogoutRequestSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    Saml2Utils.SignXml(
                        this.ServiceProvider.SigningCertificateAlias,
                        logoutRequestXml,
                        logoutRequest.Id,
                        true,
                        idp.RequestedSignatureSigningAlgorithm,
                        idp.RequestedDigestMethod,
                        this.ServiceProvider);
                }
            }

            string packagedLogoutRequest = Saml2Utils.ConvertToBase64(logoutRequestXml.InnerXml);
            string inputFieldFormat = "<input type=\"hidden\" name=\"{0}\" value=\"{1}\" />";

            StringBuilder html = new StringBuilder();
            html.Append("<html><head><title>OpenAM - SP initiated SLO</title></head>");
            html.Append("<body onload=\"document.forms[0].submit();\">");
            html.Append("<form method=\"post\" action=\"");
            html.Append(sloPostLocation);
            html.Append("\">");
            html.Append("<input type=\"hidden\" name=\"");
            html.Append(Saml2Constants.RequestParameter);
            html.Append("\" value=\"");
            html.Append(packagedLogoutRequest);
            html.Append("\" />");

            if (!string.IsNullOrEmpty(relayState))
            {
                html.Append(string.Format(
                                          CultureInfo.InvariantCulture,
                                          inputFieldFormat,
                                          Saml2Constants.RelayState,
                                          HttpUtility.HtmlEncode(relayState)));
            }

            html.Append("</form>");
            html.Append("</body>");
            html.Append("</html>");

            return html.ToString();
        }

        /// <summary>
        /// Gets the LogoutRequest location along with querystring parameters 
        /// to be used for actual browser requests.
        /// </summary>
        /// <param name="logoutRequest">
        /// LogoutRequest to packaged for a redirect.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameVallueCollection of additional parameters.
        /// </param>
        /// <returns>
        /// URL with query string parameter for the specified IDP.
        /// </returns>
        public string GetLogoutRequestRedirectLocation(LogoutRequest logoutRequest, string idpEntityId, NameValueCollection parameters)
        {
            if (logoutRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityLogoutRequestIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            string sloRedirectLocation = idp.GetSingleLogoutServiceLocation(Saml2Constants.HttpRedirectProtocolBinding);
            if (sloRedirectLocation == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleLogoutSvcLocNotDefined);
            }

            string packagedLogoutRequest = Saml2Utils.CompressConvertToBase64UrlEncode(logoutRequest.XmlDom);
            string queryString = Saml2Constants.RequestParameter + "=" + packagedLogoutRequest;

            if (parameters != null && !string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                string relayState = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(relayState, this.ServiceProvider.RelayStateUrlList);
                queryString += "&" + Saml2Constants.RelayState;
                queryString += "=" + HttpUtility.UrlEncode(relayState);
            }

            if (idp.WantLogoutRequestSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    queryString += "&" + Saml2Constants.SignatureAlgorithm;
                    queryString += "=" + HttpUtility.UrlEncode(idp.RequestedSignatureSigningAlgorithm);
                    queryString = Saml2Utils.SignQueryString(this.ServiceProvider.SigningCertificateAlias, queryString);
                }
            }

            StringBuilder redirectUrl = new StringBuilder();
            redirectUrl.Append(sloRedirectLocation);
            redirectUrl.Append(Saml2Utils.GetQueryStringDelimiter(sloRedirectLocation));
            redirectUrl.Append(queryString);

            FedletLogger.Info("LogoutRequest via Redirect:\r\n" + redirectUrl);

            return redirectUrl.ToString();
        }

        /// <summary>
        /// Gets the HTML for use of submitting the LogoutResponse with POST.
        /// </summary>
        /// <param name="logoutResponse">
        /// LogoutResponse to packaged for a POST.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameVallueCollection of additional parameters.
        /// </param>
        /// <returns>
        /// HTML with auto-form submission with POST of the LogoutRequest
        /// </returns>
        public string GetLogoutResponsePostHtml(LogoutResponse logoutResponse, string idpEntityId, NameValueCollection parameters)
        {
            if (logoutResponse == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityLogoutResponseIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            string sloPostResponseLocation = idp.GetSingleLogoutServiceResponseLocation(Saml2Constants.HttpPostProtocolBinding);
            if (sloPostResponseLocation == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleLogoutSvcResLocNotDefined);
            }

            string relayState = null;
            if (parameters != null && !string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                relayState = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(relayState, this.ServiceProvider.RelayStateUrlList);
            }

            XmlDocument logoutResponseXml = (XmlDocument)logoutResponse.XmlDom;

            if (idp.WantLogoutResponseSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    Saml2Utils.SignXml(
                        this.ServiceProvider.SigningCertificateAlias,
                        logoutResponseXml,
                        logoutResponse.Id,
                        true,
                        idp.RequestedSignatureSigningAlgorithm,
                        idp.RequestedDigestMethod,
                        this.ServiceProvider);
                }
            }

            string packagedLogoutResponse = Saml2Utils.ConvertToBase64(logoutResponseXml.InnerXml);
            string inputFieldFormat = "<input type=\"hidden\" name=\"{0}\" value=\"{1}\" />";

            StringBuilder html = new StringBuilder();
            html.Append("<html><head><title>OpenAM - IDP initiated SLO</title></head>");
            html.Append("<body onload=\"document.forms[0].submit();\">");
            html.Append("<form method=\"post\" action=\"");
            html.Append(sloPostResponseLocation);
            html.Append("\">");
            html.Append("<input type=\"hidden\" name=\"");
            html.Append(Saml2Constants.ResponseParameter);
            html.Append("\" value=\"");
            html.Append(packagedLogoutResponse);
            html.Append("\" />");

            if (!string.IsNullOrEmpty(relayState))
            {
                html.Append(string.Format(
                                          CultureInfo.InvariantCulture,
                                          inputFieldFormat,
                                          Saml2Constants.RelayState,
                                          HttpUtility.HtmlEncode(relayState)));
            }

            html.Append("</form>");
            html.Append("</body>");
            html.Append("</html>");

            return html.ToString();
        }

        /// <summary>
        /// Gets the LogoutResponse location along with querystring parameters 
        /// to be used for actual browser requests.
        /// </summary>
        /// <param name="logoutResponse">
        /// LogoutResponse to packaged for a redirect.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameVallueCollection of additional parameters.
        /// </param>
        /// <returns>
        /// URL with query string parameter for the specified IDP.
        /// </returns>
        public string GetLogoutResponseRedirectLocation(LogoutResponse logoutResponse, string idpEntityId, NameValueCollection parameters)
        {
            if (logoutResponse == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityLogoutResponseIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            string sloRedirectResponseLocation = idp.GetSingleLogoutServiceResponseLocation(Saml2Constants.HttpRedirectProtocolBinding);
            if (sloRedirectResponseLocation == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleLogoutSvcLocNotDefined);
            }

            string packagedLogoutResponse = Saml2Utils.CompressConvertToBase64UrlEncode(logoutResponse.XmlDom);
            string queryString = Saml2Constants.ResponseParameter + "=" + packagedLogoutResponse;

            if (parameters != null && !string.IsNullOrEmpty(parameters[Saml2Constants.RelayState]))
            {
                string relayState = parameters[Saml2Constants.RelayState];
                Saml2Utils.ValidateRelayState(relayState, this.ServiceProvider.RelayStateUrlList);
                queryString += "&" + Saml2Constants.RelayState;
                queryString += "=" + HttpUtility.UrlEncode(relayState);
            }

            if (idp.WantLogoutResponseSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    queryString += "&" + Saml2Constants.SignatureAlgorithm;
                    queryString += "=" + HttpUtility.UrlEncode(idp.RequestedSignatureSigningAlgorithm);
                    queryString = Saml2Utils.SignQueryString(this.ServiceProvider.SigningCertificateAlias, queryString);
                }
            }

            StringBuilder redirectUrl = new StringBuilder();
            redirectUrl.Append(sloRedirectResponseLocation);
            redirectUrl.Append(Saml2Utils.GetQueryStringDelimiter(sloRedirectResponseLocation));
            redirectUrl.Append(queryString);

            FedletLogger.Info("LogoutResponse via Redirect:\r\n" + redirectUrl);

            return redirectUrl.ToString();
        }

        #endregion

        #region Public Send Methods

        /// <summary>
        /// Sends an AuthnRequest to the specified IDP with the given 
        /// parameters.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameValueCollection of varying parameters for use in the 
        /// construction of the AuthnRequest.
        /// </param>
        public void SendAuthnRequest(HttpContext context, string idpEntityId, NameValueCollection parameters)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            if (parameters == null)
            {
                parameters = new NameValueCollection();
            }

            AuthnRequest authnRequest = new AuthnRequest(idp, this.ServiceProvider, parameters);
            XmlDocument xmlDoc = (XmlDocument)authnRequest.XmlDom;
            StringBuilder logMessage = new StringBuilder();
            logMessage.Append("AuthnRequest:\r\n").Append(xmlDoc.OuterXml);
            FedletLogger.Info(logMessage.ToString());

            // Add this AuthnRequest for this user for validation on AuthnResponse
            AuthnRequestCache.AddSentAuthnRequest(context, authnRequest);

            // Send with Redirect or Post based on the 'reqBinding' parameter.
            if (parameters[Saml2Constants.RequestBinding] == Saml2Constants.HttpPostProtocolBinding)
            {
                string postHtml = this.GetAuthnRequestPostHtml(authnRequest, idpEntityId, parameters);
                context.Response.Write(postHtml);
                context.Response.End();
            }
            else
            {
                string redirectUrl = this.GetAuthnRequestRedirectLocation(authnRequest, idpEntityId, parameters);
                context.Response.Redirect(redirectUrl.ToString(), true);
            }
        }

        /// <summary>
        /// Sends a LogoutRequest to the specified IDP with the given 
        /// parameters.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameValueCollection of varying parameters for use in the 
        /// construction of the LogoutRequest.
        /// </param>
        public void SendLogoutRequest(HttpContext context, string idpEntityId, NameValueCollection parameters)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            if (parameters == null)
            {
                parameters = new NameValueCollection();
            }

            LogoutRequest logoutRequest = new LogoutRequest(idp, this.ServiceProvider, parameters);
            XmlDocument xmlDoc = (XmlDocument)logoutRequest.XmlDom;
            StringBuilder logMessage = new StringBuilder();
            logMessage.Append("LogoutRequest:\r\n").Append(xmlDoc.OuterXml);
            FedletLogger.Info(logMessage.ToString());

            // Send with Redirect, POST, or SOAP based on the 'Binding' parameter.
            if (parameters[Saml2Constants.Binding] == Saml2Constants.HttpPostProtocolBinding)
            {
                LogoutRequestCache.AddSentLogoutRequest(context, logoutRequest);
                string postHtml = this.GetLogoutRequestPostHtml(logoutRequest, idpEntityId, parameters);
                context.Response.Write(postHtml);
                context.Response.End();
            }
            else if (parameters[Saml2Constants.Binding] == Saml2Constants.HttpRedirectProtocolBinding)
            {
                LogoutRequestCache.AddSentLogoutRequest(context, logoutRequest);
                string redirectUrl = this.GetLogoutRequestRedirectLocation(logoutRequest, idpEntityId, parameters);
                context.Response.Redirect(redirectUrl.ToString(), true);
            }
            else if (parameters[Saml2Constants.Binding] == Saml2Constants.HttpSoapProtocolBinding)
            {
                this.SendSoapLogoutRequest(logoutRequest, idpEntityId);
            }
            else
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityUnsupportedLogoutBinding);
            }
        }

        /// <summary>
        /// Sends a SOAP LogoutRequest to the specified IDP.
        /// </summary>
        /// <param name="logoutRequest">
        /// LogoutRequest object.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        public void SendSoapLogoutRequest(LogoutRequest logoutRequest, string idpEntityId)
        {
            HttpWebRequest request = null;
            HttpWebResponse response = null;
            LogoutResponse logoutResponse = null;
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];

            if (logoutRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityLogoutRequestIsNull);
            }
            else if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }
            else if (idp.GetSingleLogoutServiceLocation(Saml2Constants.HttpSoapProtocolBinding) == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleLogoutSvcLocNotDefined);
            }

            try
            {
                Uri soapLogoutSvcUri = new Uri(idp.GetSingleLogoutServiceLocation(Saml2Constants.HttpSoapProtocolBinding));
                if (soapLogoutSvcUri.Scheme == "https")
                {
                    System.Net.ServicePointManager.ServerCertificateValidationCallback +=
                    delegate(object sender, System.Security.Cryptography.X509Certificates.X509Certificate certificate,
                    System.Security.Cryptography.X509Certificates.X509Chain chain,
                    System.Net.Security.SslPolicyErrors sslPolicyErrors)
                    {
                        if (ServiceProvider.TrustAllCerts
                            || sslPolicyErrors.HasFlag(System.Net.Security.SslPolicyErrors.None))
                            {
                                return true;
                            }
                        
                        StringBuilder logErrorMessage = new StringBuilder();
                        logErrorMessage.Append("SSLPolicyError: ").Append(sslPolicyErrors);
                        FedletLogger.Error(logErrorMessage.ToString());
                        return false;
                    };
                }

                request = (HttpWebRequest)WebRequest.Create(soapLogoutSvcUri);

                string authCertAlias = ConfigurationManager.AppSettings[Saml2Constants.MutualAuthCertAlias];
                if (soapLogoutSvcUri.Scheme == "https" && !string.IsNullOrWhiteSpace(authCertAlias))
                {
                    X509Certificate2 cert = FedletCertificateFactory.GetCertificateByFriendlyName(authCertAlias);
                    if (cert != null)
                    {
                        request.ClientCertificates.Add(cert);
                    }
                }

                XmlDocument logoutRequestXml = (XmlDocument)logoutRequest.XmlDom;

                if (idp.WantLogoutRequestSigned)
                {
                    if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                    {
                        throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                    }
                    else
                    {
                        Saml2Utils.SignXml(
                            this.ServiceProvider.SigningCertificateAlias,
                            logoutRequestXml,
                            logoutRequest.Id,
                            true,
                            idp.RequestedSignatureSigningAlgorithm,
                            idp.RequestedDigestMethod,
                            this.ServiceProvider);
                    }
                }

                string soapMessage = Saml2Utils.CreateSoapMessage(logoutRequestXml.InnerXml);

                byte[] byteArray = Encoding.UTF8.GetBytes(soapMessage);
                request.ContentType = "text/xml";
                request.ContentLength = byteArray.Length;
                request.AllowAutoRedirect = false;
                request.Method = "POST";

                Stream requestStream = request.GetRequestStream();
                requestStream.Write(byteArray, 0, byteArray.Length);
                requestStream.Close();

                response = (HttpWebResponse)request.GetResponse();
                StreamReader streamReader = new StreamReader(response.GetResponseStream());
                string responseContent = streamReader.ReadToEnd();
                streamReader.Close();

                XmlDocument soapResponse = new XmlDocument();
                soapResponse.PreserveWhitespace = true;
                soapResponse.LoadXml(responseContent);

                XmlNamespaceManager soapNsMgr = new XmlNamespaceManager(soapResponse.NameTable);
                soapNsMgr.AddNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
                soapNsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                soapNsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                soapNsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");

                XmlElement root = soapResponse.DocumentElement;
                XmlNode responseXml = root.SelectSingleNode("/soap:Envelope/soap:Body/samlp:LogoutResponse", soapNsMgr);
                string logoutResponseXml = responseXml.OuterXml;

                logoutResponse = new LogoutResponse(logoutResponseXml);
                StringBuilder logMessage = new StringBuilder();
                logMessage.Append("LogoutResponse:\r\n").Append(logoutResponseXml);
                FedletLogger.Info(logMessage.ToString());

                ArrayList logoutRequests = new ArrayList();
                logoutRequests.Add(logoutRequest);
                this.Validate(logoutResponse, logoutRequests);
            }
            catch (WebException we)
            {
                throw new ServiceProviderUtilityException(Resources.LogoutRequestWebException, we);
            }
            finally
            {
                if (response != null)
                {
                    response.Close();
                }
            }
        }

        /// <summary>
        /// Send the SAML LogoutResponse message based on the received
        /// LogoutRequest.  POST (default) or Redirect is supported.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="logoutRequest">
        /// LogoutRequest corresponding to the ensuing LogoutResponse to send.
        /// </param>
        public void SendLogoutResponse(HttpContext context, LogoutRequest logoutRequest)
        {
            if (logoutRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityLogoutRequestIsNull);
            }

            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[logoutRequest.Issuer];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpNotDeterminedFromLogoutRequest);
            }

            // send logout response based on how it was received
            if (context.Request.HttpMethod == "GET")
            {
                NameValueCollection parameters = new NameValueCollection();
                parameters[Saml2Constants.Binding] = Saml2Constants.HttpRedirectProtocolBinding;
                LogoutResponse logoutResponse = new LogoutResponse(idp, this.ServiceProvider, logoutRequest, parameters);

                StringBuilder logMessage = new StringBuilder();
                XmlDocument xmlDoc = (XmlDocument)logoutResponse.XmlDom;
                logMessage.Append("LogoutResponse:\r\n").Append(xmlDoc.OuterXml);
                FedletLogger.Info(logMessage.ToString());

                parameters = Saml2Utils.GetRequestParameters(context.Request);
                string redirectUrl = this.GetLogoutResponseRedirectLocation(logoutResponse, idp.EntityId, parameters);
                context.Response.Redirect(redirectUrl.ToString(), true);
            }
            else
            {
                NameValueCollection parameters = new NameValueCollection();
                parameters[Saml2Constants.Binding] = Saml2Constants.HttpPostProtocolBinding;
                LogoutResponse logoutResponse = new LogoutResponse(idp, this.ServiceProvider, logoutRequest, parameters);

                StringBuilder logMessage = new StringBuilder();
                XmlDocument xmlDoc = (XmlDocument)logoutResponse.XmlDom;
                logMessage.Append("LogoutResponse:\r\n").Append(xmlDoc.OuterXml);
                FedletLogger.Info(logMessage.ToString());

                parameters = Saml2Utils.GetRequestParameters(context.Request);
                string postHtml = this.GetLogoutResponsePostHtml(logoutResponse, idp.EntityId, parameters);
                context.Response.Write(postHtml);
                context.Response.End();
            }
        }

        /// <summary>
        /// Writes a SOAP LogoutResponse to the Response object found within
        /// the given HttpContext based on the given logout request.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="logoutRequest">
        /// LogoutRequest object.
        /// </param>
        public void SendSoapLogoutResponse(HttpContext context, LogoutRequest logoutRequest)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[logoutRequest.Issuer];

            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            NameValueCollection parameters = new NameValueCollection();
            parameters[Saml2Constants.Binding] = Saml2Constants.HttpSoapProtocolBinding;

            LogoutResponse logoutResponse = new LogoutResponse(idp, this.ServiceProvider, logoutRequest, parameters);
            XmlDocument logoutResponseXml = (XmlDocument)logoutResponse.XmlDom;

            if (idp.WantLogoutResponseSigned)
            {
                if (string.IsNullOrEmpty(this.ServiceProvider.SigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    Saml2Utils.SignXml(
                        this.ServiceProvider.SigningCertificateAlias,
                        logoutResponseXml,
                        logoutResponse.Id,
                        true,
                        idp.RequestedSignatureSigningAlgorithm,
                        idp.RequestedDigestMethod,
                        this.ServiceProvider);
                }
            }

            StringBuilder logMessage = new StringBuilder();
            logMessage.Append("LogoutResponse:\r\n").Append(logoutResponseXml.OuterXml);
            FedletLogger.Info(logMessage.ToString());

            string soapMessage = Saml2Utils.CreateSoapMessage(logoutResponseXml.OuterXml);

            context.Response.ContentType = "text/xml";
            context.Response.Write(soapMessage);
        }

        /// <summary>
        /// Sends an AttributeQueryRequest to the specified IDP with the given 
        /// parameters.
        /// </summary>
        /// <param name="context">
        /// HttpContext containing session, request, and response objects.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        /// <param name="parameters">
        /// NameValueCollection of varying parameters for use in the 
        /// construction of the AttributeQueryRequest.
        /// </param>
        /// <param name="attributes">
        /// A list of SamlAttributes for use in the 
        /// construction of the AttributeQueryRequest.
        /// </param>
        public AttributeQueryResponse SendAttributeQueryRequest(HttpContext context, string idpEntityId, NameValueCollection parameters,
             List<SamlAttribute> attributes)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
            if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }

            if (parameters == null)
            {
                parameters = new NameValueCollection();
            }

            AttributeQueryRequest aqRequest = new AttributeQueryRequest(idp, this.ServiceProvider, parameters, attributes);
            XmlDocument xmlDoc = (XmlDocument)aqRequest.XmlDom;
            StringBuilder logMessage = new StringBuilder();
            logMessage.Append("AttributeQueryRequest:\r\n").Append(xmlDoc.OuterXml);
            FedletLogger.Info(logMessage.ToString());
            return this.SendSoapAttributeQueryRequest(aqRequest, idpEntityId);
        }

        /// <summary>
        /// Sends a SOAP AttributeQueryRequest to the specified IDP.
        /// </summary>
        /// <param name="attrQueryRequest">
        /// AttributeQueryRequest object.
        /// </param>
        /// <param name="idpEntityId">Entity ID of the IDP.</param>
        public AttributeQueryResponse SendSoapAttributeQueryRequest(AttributeQueryRequest attrQueryRequest, string idpEntityId)
        {
            XmlNodeList soapFault = null;
            HttpWebRequest request = null;
            HttpWebResponse response = null;
            AttributeQueryResponse attributeQueryResponse = null;
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[idpEntityId];

            if (attrQueryRequest == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityAttributeQueryRequestIsNull);
            }
            else if (idp == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProviderNotFound);
            }
            else if (idp.GetSingleAttributeServiceLocation(Saml2Constants.HttpSoapProtocolBinding, attrQueryRequest.X509SubjectName) == null)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdpSingleAttributeQuerySvcLocNotDefined);
            }

            try
            {

                Uri soapAttributeQuerySvcUri = new Uri(idp.GetSingleAttributeServiceLocation(Saml2Constants.HttpSoapProtocolBinding, attrQueryRequest.X509SubjectName));
                if (soapAttributeQuerySvcUri.Scheme == "https")
                {
                    System.Net.ServicePointManager.ServerCertificateValidationCallback +=
                    delegate(object sender, System.Security.Cryptography.X509Certificates.X509Certificate certificate,
                    System.Security.Cryptography.X509Certificates.X509Chain chain,
                    System.Net.Security.SslPolicyErrors sslPolicyErrors)
                    {
                        if (ServiceProvider.TrustAllCerts
                            || sslPolicyErrors.HasFlag(System.Net.Security.SslPolicyErrors.None))
                            {
                                return true;
                            }

                        StringBuilder logErrorMessage = new StringBuilder();
                        logErrorMessage.Append("SSLPolicyError: ").Append(sslPolicyErrors);
                        FedletLogger.Error(logErrorMessage.ToString());
                        return false;
                    };
                }

                request = (HttpWebRequest)WebRequest.Create(soapAttributeQuerySvcUri);

                string authCertAlias = ConfigurationManager.AppSettings[Saml2Constants.MutualAuthCertAlias];
                if (soapAttributeQuerySvcUri.Scheme == "https" && !string.IsNullOrWhiteSpace(authCertAlias))
                {
                    X509Certificate2 cert = FedletCertificateFactory.GetCertificateByFriendlyName(authCertAlias);
                    if (cert != null)
                    {
                        request.ClientCertificates.Add(cert);
                    }
                }

                XmlDocument attrQueryRequestXml = (XmlDocument)attrQueryRequest.XmlDom;

                if (this.ServiceProvider.WantNameIDEncryptedAttributeQuery)
                {
                    if (string.IsNullOrWhiteSpace(idp.EncodedEncryptionCertificate))
                    {
                        throw new ServiceProviderUtilityException(Resources.EncryptedXmlCertNotFound);
                    }

                    if (string.IsNullOrWhiteSpace(idp.EncryptionMethodAlgorithm))
                    {
                        throw new ServiceProviderUtilityException(Resources.EncryptedXmlInvalidEncrAlgorithm);
                    }

                    Saml2Utils.EncryptAttributeQueryNameID(
                        idp.EncodedEncryptionCertificate,
                        idp.EncryptionMethodAlgorithm,
                        attrQueryRequestXml);
                }

                if (string.IsNullOrWhiteSpace(this.ServiceProvider.AttributeQuerySigningCertificateAlias))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySignFailedNoCertAlias);
                }
                else
                {
                    Saml2Utils.SignXml(
                         this.ServiceProvider.AttributeQuerySigningCertificateAlias,
                         attrQueryRequestXml,
                         attrQueryRequest.Id,
                         true,
                         idp.RequestedSignatureSigningAlgorithm,
                         idp.RequestedDigestMethod,
                         this.ServiceProvider);
                }

                string soapMessage = Saml2Utils.CreateSoapMessage(attrQueryRequestXml.InnerXml);
                StringBuilder logMessageSoap = new StringBuilder();
                logMessageSoap.Append("AttributeQuerySOAPRequest:\r\n").Append(soapMessage);
                FedletLogger.Info(logMessageSoap.ToString());

                byte[] byteArray = Encoding.UTF8.GetBytes(soapMessage);
                request.ContentType = "text/xml";
                request.ContentLength = byteArray.Length;
                request.AllowAutoRedirect = false;
                request.Method = "POST";

                Stream requestStream = request.GetRequestStream();
                requestStream.Write(byteArray, 0, byteArray.Length);
                requestStream.Close();

                response = (HttpWebResponse)request.GetResponse();
                StreamReader streamReader = new StreamReader(response.GetResponseStream());
                string responseContent = streamReader.ReadToEnd();
                streamReader.Close();

                XmlDocument soapResponse = new XmlDocument();
                soapResponse.PreserveWhitespace = true;
                soapResponse.LoadXml(responseContent);

                XmlNamespaceManager soapNsMgr = new XmlNamespaceManager(soapResponse.NameTable);
                soapNsMgr.AddNamespace("soap", "http://schemas.xmlsoap.org/soap/envelope/");
                soapNsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                soapNsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                soapNsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");

                XmlElement root = soapResponse.DocumentElement;

                if ((soapFault = root.GetElementsByTagName("Fault", "http://schemas.xmlsoap.org/soap/envelope/")).Count > 0)
                {
                    StringBuilder faultMessage = new StringBuilder();
                    faultMessage.Append("AttributeQueryResponse Error:\r\n");
                    faultMessage.Append(soapFault[0].InnerText);
                    FedletLogger.Info(faultMessage.ToString());
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilitySoapFault, new SoapException(soapFault[0].InnerText));
                }

                XmlNode responseXml = root.SelectSingleNode("/soap:Envelope/soap:Body/samlp:Response", soapNsMgr);
                string attrQueryResponseXml = responseXml.OuterXml;

                attributeQueryResponse = new AttributeQueryResponse(attrQueryResponseXml);
                StringBuilder logMessage = new StringBuilder();
                logMessage.Append("AttributeQueryResponse:\r\n").Append(attrQueryResponseXml);
                FedletLogger.Info(logMessage.ToString());

                if (attributeQueryResponse != null && !string.IsNullOrWhiteSpace(attributeQueryResponse.StatusCode)
                    && !attributeQueryResponse.StatusCode.Equals(Saml2Constants.Success))
                {
                    throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityErrorSamlResponseReceived,
                        new Saml2Exception(attributeQueryResponse.StatusMessage));
                }

                ArrayList attributeQueryRequests = new ArrayList();
                attributeQueryRequests.Add(attrQueryRequest);
                this.Validate(attributeQueryResponse, attributeQueryRequests);

            }
            catch (WebException we)
            {
                throw new ServiceProviderUtilityException(Resources.AttributeQueryRequestWebException, we);
            }
            finally
            {
                if (response != null)
                {
                    response.Close();
                }
            }
            return attributeQueryResponse;
        }

        #endregion

        #region Public Validation Methods

        /// <summary>
        /// Validates the given ArtifactResponse object.
        /// </summary>
        /// <param name="artifactResponse">ArtifactResponse object.</param>
        /// <param name="authnRequests">
        /// Collection of previously sent authnRequests used to compare with
        /// the InResponseTo attribute (if present) of the embedded 
        /// AuthnResponse within the ArtifactResponse.
        /// </param>
        /// <see cref="ServiceProviderUtility.Validate(AuthnResponse, ICollection)"/>
        public void ValidateForArtifact(ArtifactResponse artifactResponse, ICollection authnRequests)
        {
            if (artifactResponse.XmlSignature == null && artifactResponse.AuthnResponse.isAssertionEncrypted())
            {
                artifactResponse.Decrypt(ServiceProvider);
                artifactResponse.AuthnResponse.Decrypt(ServiceProvider);
            }
            this.CheckSignature(artifactResponse);
            if (artifactResponse.AuthnResponse.isAssertionEncrypted())
            {
                artifactResponse.AuthnResponse.Decrypt(ServiceProvider);
            }
            this.Validate(artifactResponse.AuthnResponse, authnRequests);
        }

        /// <summary>
        /// Validates the given AuthnResponse object.
        /// </summary>
        /// <param name="authnResponse">AuthnResponse object.</param>
        /// <param name="authnRequests">
        /// Collection of previously sent authnRequests used to compare with
        /// the InResponseTo attribute (if present) of the AuthnResponse.
        /// </param>
        /// <see cref="ServiceProviderUtility.Validate(AuthnResponse, ICollection)"/>
        public void ValidateForPost(AuthnResponse authnResponse, ICollection authnRequests)
        {
            if (authnResponse.XmlResponseSignature == null && authnResponse.isAssertionEncrypted())
            {
                authnResponse.Decrypt(ServiceProvider);
            }
            this.CheckSignature(authnResponse);
            if (authnResponse.isAssertionEncrypted())
            {
                authnResponse.Decrypt(ServiceProvider);
            }
            this.Validate(authnResponse, authnRequests);
        }

        /// <summary>
        /// Validates the given LogoutRequest.
        /// </summary>
        /// <param name="logoutRequest">LogoutRequest object.</param>
        public void ValidateForPost(LogoutRequest logoutRequest)
        {
            this.CheckIssuer(logoutRequest.Issuer);

            if (this.ServiceProvider.WantLogoutRequestSigned)
            {
                this.CheckSignature(logoutRequest);
            }
        }

        /// <summary>
        /// Validates the given LogoutRequest.
        /// </summary>
        /// <param name="logoutRequest">LogoutRequest object.</param>
        /// <param name="queryString">
        /// Raw query string that contains the request and possible signature
        /// </param>
        public void ValidateForRedirect(LogoutRequest logoutRequest, string queryString)
        {
            this.CheckIssuer(logoutRequest.Issuer);

            if (this.ServiceProvider.WantLogoutRequestSigned)
            {
                this.CheckSignature(logoutRequest, queryString);
            }
        }

        /// <summary>
        /// Validates the given LogoutResponse object obtained from a POST. If
        /// this service provider desires the logout respone to be signed, XML
        /// signature checking will be performed.
        /// </summary>
        /// <param name="logoutResponse">LogoutResponse object.</param>
        /// <param name="logoutRequests">
        /// Collection of previously sent logoutRequests used to compare with
        /// the InResponseTo attribute of the LogoutResponse (if present).
        /// </param>
        public void ValidateForPost(LogoutResponse logoutResponse, ICollection logoutRequests)
        {
            if (logoutResponse == null)
            {
                throw new Saml2Exception(Resources.ServiceProviderUtilityLogoutResponseIsNull);
            }

            if (this.ServiceProvider.WantLogoutResponseSigned)
            {
                this.CheckSignature(logoutResponse);
            }

            this.Validate(logoutResponse, logoutRequests);
        }

        /// <summary>
        /// Validates the given LogoutResponse object obtained from a
        /// Redirect. If this service provider desires the logout respone to 
        /// be signed, XML signature checking will be performed.
        /// </summary>
        /// <param name="logoutResponse">LogoutResponse object.</param>
        /// <param name="logoutRequests">
        /// Collection of previously sent logoutRequests used to compare with
        /// the InResponseTo attribute of the LogoutResponse (if present).
        /// </param>
        /// <param name="queryString">
        /// Raw query string that contains the request and possible signature
        /// </param>
        public void ValidateForRedirect(LogoutResponse logoutResponse, ICollection logoutRequests, string queryString)
        {
            if (logoutResponse == null)
            {
                throw new Saml2Exception(Resources.ServiceProviderUtilityLogoutResponseIsNull);
            }

            if (this.ServiceProvider.WantLogoutResponseSigned)
            {
                this.CheckSignature(logoutResponse, queryString);
            }

            this.Validate(logoutResponse, logoutRequests);
        }

        #endregion

        #region Static Private Methods

        /// <summary>
        /// Checks the time condition of the given AuthnResponse.
        /// </summary>
        /// <param name="authnResponse">SAMLv2 AuthnResponse.</param>
        private static void CheckConditionWithTime(AuthnResponse authnResponse)
        {
            DateTime utcNow = DateTime.UtcNow;
            DateTime utcBefore = TimeZoneInfo.ConvertTimeToUtc(authnResponse.ConditionNotBefore);
            DateTime utcOnOrAfter = TimeZoneInfo.ConvertTimeToUtc(authnResponse.ConditionNotOnOrAfter);

            if (utcNow < utcBefore || utcNow >= utcOnOrAfter)
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidConditionTime);
            }
        }

        /// <summary>
        /// Checks the InResponseTo field of the given AuthnResponse to
        /// see if it is one of the managed authn requests.
        /// </summary>
        /// <param name="authnResponse">SAMLv2 AuthnResponse.</param>
        /// <param name="authnRequests">
        /// Collection of previously sent AuthnRequests.
        /// </param>
        private static void CheckInResponseTo(AuthnResponse authnResponse, ICollection authnRequests)
        {
            if (authnRequests != null && authnResponse.InResponseTo != null)
            {
                IEnumerator i = authnRequests.GetEnumerator();
                while (i.MoveNext())
                {
                    AuthnRequest authnRequest = (AuthnRequest)i.Current;
                    if (authnRequest.Id == authnResponse.InResponseTo)
                    {
                        // Found one, return quietly.
                        return;
                    }
                }
            }

            // Didn't find one, complain loudly.
            throw new Saml2Exception(Resources.AuthnResponseInvalidInResponseTo);
        }

        /// <summary>
        /// Checks the InResponseTo field of the given LogoutResponse to
        /// see if it is one of the managed logout requests.
        /// </summary>
        /// <param name="logoutResponse">SAMLv2 LogoutResponse.</param>
        /// <param name="logoutRequests">
        /// Collection of previously sent LogoutRequests.
        /// </param>
        private static void CheckInResponseTo(LogoutResponse logoutResponse, ICollection logoutRequests)
        {
            if (logoutRequests != null && logoutResponse.InResponseTo != null)
            {
                IEnumerator i = logoutRequests.GetEnumerator();
                while (i.MoveNext())
                {
                    LogoutRequest logoutRequest = (LogoutRequest)i.Current;
                    if (logoutRequest.Id == logoutResponse.InResponseTo)
                    {
                        // Found one, return quietly.
                        return;
                    }
                }
            }

            // Didn't find one, complain loudly.
            throw new Saml2Exception(Resources.LogoutResponseInvalidInResponseTo);
        }

        /// <summary>
        /// Checks the InResponseTo field of the given AttributeQueryResponse to
        /// see if it is one of the managed attribute query requests.
        /// </summary>
        /// <param name="attributeQueryResponse">SAMLv2 AttributeQueryResponse.</param>
        /// <param name="attributeQueryRequests">
        /// Collection of previously sent AttributeQueryRequests.
        /// </param>
        private static void CheckInResponseTo(AttributeQueryResponse attributeQueryResponse, ICollection attributeQueryRequests)
        {
            if (attributeQueryResponse != null && attributeQueryResponse.InResponseTo != null)
            {
                IEnumerator i = attributeQueryRequests.GetEnumerator();
                while (i.MoveNext())
                {
                    AttributeQueryRequest request = (AttributeQueryRequest)i.Current;
                    if (request.Id == attributeQueryResponse.InResponseTo)
                    {
                        // Found one, return quietly.
                        return;
                    }
                }
            }

            // Didn't find one, complain loudly.
            throw new Saml2Exception(Resources.AttributeQueryResponseInvalidInResponseTo);
        }

        /// <summary>
        /// Checks for a SAML "success" status code in a SAML message, 
        /// otherwise a Saml2Exception is thrown.
        /// </summary>
        /// <param name="statusCode">StatusCode to check</param>
        private static void CheckStatusCode(string statusCode)
        {
            if (string.IsNullOrEmpty(statusCode) || statusCode != Saml2Constants.Success)
            {
                throw new Saml2Exception(Resources.InvalidStatusCode);
            }
        }

        #endregion

        #region Non-static Private Methods

        /// <summary>
        /// Internal method to load configuration information and metadata
        /// for the hosted service provider and associated identity providers.
        /// </summary>
        /// <param name="homeFolder">Home folder containing configuration and metadata.</param>
        private void Initialize(string homeFolder)
        {
            DirectoryInfo dirInfo = new DirectoryInfo(homeFolder);
            if (!dirInfo.Exists)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityHomeFolderNotFound);
            }

            this.homeFolder = homeFolder;

            // Load the metadata for this service provider.
            this.ServiceProvider = new ServiceProvider(this.homeFolder);

            // Load the configuration for one or more circle of trusts.
            this.CircleOfTrusts = new Hashtable();
            this.InitializeCircleOfTrusts();

            // Load metadata for one or more identity providers.
            this.IdentityProviders = new Hashtable();
            this.InitializeIdentityProviders();
        }

        /// <summary>
        /// Internal method to load all configuration information for all
        /// circle of trusts found in the home folder.
        /// </summary>
        private void InitializeCircleOfTrusts()
        {
            DirectoryInfo dirInfo = new DirectoryInfo(this.homeFolder);
            FileInfo[] files = dirInfo.GetFiles("fedlet*.cot");

            foreach (FileInfo file in files)
            {
                CircleOfTrust cot = new CircleOfTrust(file.FullName);
                string key = cot.Attributes["cot-name"];
                this.CircleOfTrusts.Add(key, cot);
            }

            if (this.CircleOfTrusts.Count <= 0)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtiltyCircleOfTrustsNotFound);
            }
        }

        /// <summary>
        /// Internal method to load all configuration information for all
        /// identity providers' metadata founds in the home folder.
        /// </summary>
        private void InitializeIdentityProviders()
        {
            DirectoryInfo dirInfo = new DirectoryInfo(this.homeFolder);
            FileInfo[] files = dirInfo.GetFiles("idp*.xml");

            string metadataFilePattern = "idp(.*).xml";         // for regex.match
            string extendedFilePattern = "idp{0}-extended.xml"; // for string.format
            string fileIndex = null;

            foreach (FileInfo metadataFile in files)
            {
                Match m = Regex.Match(metadataFile.Name, metadataFilePattern);

                // determine index
                if (m.Success)
                {
                    fileIndex = m.Groups[1].Value;
                }

                string extendedFileName;
                if (fileIndex == null)
                {
                    extendedFileName = string.Format(CultureInfo.InvariantCulture, extendedFilePattern, string.Empty);
                }
                else
                {
                    extendedFileName = string.Format(CultureInfo.InvariantCulture, extendedFilePattern, fileIndex);
                }

                FileInfo extendedFile = new FileInfo(this.homeFolder + @"/" + extendedFileName);
                if (metadataFile.Exists && extendedFile.Exists)
                {
                    IdentityProvider identityProvider = new IdentityProvider(metadataFile.FullName, extendedFile.FullName);
                    this.IdentityProviders.Add(identityProvider.EntityId, identityProvider);
                }
            }

            if (this.IdentityProviders.Count <= 0)
            {
                throw new ServiceProviderUtilityException(Resources.ServiceProviderUtilityIdentityProvidersNotFound);
            }
        }

        /// <summary>
        /// Checks if the provided entity ID matches one of the known entity
        /// Identity Provider ID's, otherwise a Saml2Exception is thrown..
        /// </summary>
        /// <param name="idpEntityId">IDP entity ID</param>
        private void CheckIssuer(string idpEntityId)
        {
            if (string.IsNullOrEmpty(idpEntityId) || !this.IdentityProviders.ContainsKey(idpEntityId))
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }
        }

        /// <summary>
        /// Checks the audience condition of the given AuthnResponse.
        /// </summary>
        /// <param name="authnResponse">SAMLv2 AuthnResponse.</param>
        private void CheckConditionWithAudience(AuthnResponse authnResponse)
        {
            if (!authnResponse.ConditionAudiences.Contains(this.ServiceProvider.EntityId))
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidConditionAudience);
            }
        }

        /// <summary>
        /// Checks the signature of the given ArtifactResponse and embedded
        /// AuthnResponse used for the Artifact profile.
        /// </summary>
        /// <param name="artifactResponse">ArtifactResponse object.</param>
        /// <seealso cref="ServiceProviderUtility.ValidateForArtifact"/>
        private void CheckSignature(ArtifactResponse artifactResponse)
        {
            AuthnResponse authnResponse = artifactResponse.AuthnResponse;

            IdentityProvider identityProvider = (IdentityProvider)this.IdentityProviders[authnResponse.Issuer];
            if (identityProvider == null)
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }

            XmlElement artifactResponseSignature = (XmlElement)artifactResponse.XmlSignature;
            XmlElement responseSignature = (XmlElement)authnResponse.XmlResponseSignature;
            XmlElement assertionSignature = (XmlElement)authnResponse.XmlAssertionSignature;

            XmlElement validationSignature = null;
            string validationSignatureCert = null;
            string validationReferenceId = null;

            if (this.ServiceProvider.WantArtifactResponseSigned && artifactResponseSignature == null)
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureMissingOnArtifactResponse);
            }
            else if (this.ServiceProvider.WantPostResponseSigned && responseSignature == null && artifactResponseSignature == null)
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureMissingOnResponse);
            }
            else if (this.ServiceProvider.WantAssertionsSigned && assertionSignature == null && responseSignature == null && artifactResponseSignature == null)
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureMissing);
            }

            // pick the ArtifactResponse, Response or the Assertion for further validation...
            if (artifactResponseSignature != null)
            {
                validationSignature = artifactResponseSignature;
                validationSignatureCert = artifactResponse.SignatureCertificate;
                validationReferenceId = artifactResponse.Id;
            }
            else if (responseSignature != null)
            {
                validationSignature = responseSignature;
                validationSignatureCert = authnResponse.ResponseSignatureCertificate;
                validationReferenceId = authnResponse.Id;
            }
            else
            {
                validationSignature = assertionSignature;
                validationSignatureCert = authnResponse.AssertionSignatureCertificate;
                validationReferenceId = authnResponse.AssertionId;
            }

            if (validationSignatureCert != null)
            {
                string idpCert = Regex.Replace(identityProvider.EncodedSigningCertificate, @"\s", string.Empty);
                validationSignatureCert = Regex.Replace(validationSignatureCert, @"\s", string.Empty);
                if (idpCert != validationSignatureCert)
                {
                    throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureCertsDontMatch);
                }
            }

            // check the signature of the xml document (optional for artifact)
            if (validationSignature != null)
            {
                Saml2Utils.ValidateSignedXml(
                                             identityProvider.SigningCertificate,
                                             (XmlDocument)artifactResponse.XmlDom,
                                             validationSignature,
                                             validationReferenceId);
            }
        }

        /// <summary>
        /// Checks the signature of the given AuthnResponse used for the POST
        /// profile.
        /// </summary>
        /// <param name="authnResponse">AuthnResponse object.</param>
        /// <seealso cref="ServiceProviderUtility.ValidateForPost(AuthnResponse, ICollection)"/>
        private void CheckSignature(AuthnResponse authnResponse)
        {
            IdentityProvider identityProvider = (IdentityProvider)this.IdentityProviders[authnResponse.Issuer];
            if (identityProvider == null)
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }

            XmlElement responseSignature = (XmlElement)authnResponse.XmlResponseSignature;
            XmlElement assertionSignature = (XmlElement)authnResponse.XmlAssertionSignature;
            XmlElement validationSignature = null;
            string validationSignatureCert = null;
            string validationReferenceId = null;

            if (responseSignature == null && assertionSignature == null)
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureMissing);
            }
            else if (this.ServiceProvider.WantPostResponseSigned && responseSignature == null)
            {
                throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureMissingOnResponse);
            }

            // pick the Response or the Assertion for further validation...
            if (responseSignature != null)
            {
                validationSignature = responseSignature;
                validationSignatureCert = authnResponse.ResponseSignatureCertificate;
                validationReferenceId = authnResponse.Id;
            }
            else
            {
                validationSignature = assertionSignature;
                validationSignatureCert = authnResponse.AssertionSignatureCertificate;
                validationReferenceId = authnResponse.AssertionId;
            }

            if (validationSignatureCert != null)
            {
                string idpCert = Regex.Replace(identityProvider.EncodedSigningCertificate, @"\s", string.Empty);
                validationSignatureCert = Regex.Replace(validationSignatureCert, @"\s", string.Empty);
                if (idpCert != validationSignatureCert)
                {
                    throw new Saml2Exception(Resources.AuthnResponseInvalidSignatureCertsDontMatch);
                }
            }

            // check the signature of the xml document (always for post)
            Saml2Utils.ValidateSignedXml(
                                         identityProvider.SigningCertificate,
                                         (XmlDocument)authnResponse.XmlDom,
                                         validationSignature,
                                         validationReferenceId);
        }

        /// <summary>
        /// Checks the signature of the given LogoutRequest assuming
        /// the signature is within the XML.
        /// </summary>
        /// <param name="logoutRequest">SAMLv2 LogoutRequest object.</param>
        private void CheckSignature(LogoutRequest logoutRequest)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[logoutRequest.Issuer];

            if (idp == null)
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }

            Saml2Utils.ValidateSignedXml(
                idp.SigningCertificate,
                logoutRequest.XmlDom,
                logoutRequest.XmlSignature,
                logoutRequest.Id);
        }

        /// <summary>
        /// Checks the signature of the given LogoutRequest with
        /// the raw query string.
        /// </summary>
        /// <param name="logoutRequest">SAMLv2 LogoutRequest object.</param>
        /// <param name="queryString">
        /// Raw query string that contains the request and possible signature.
        /// </param>
        private void CheckSignature(LogoutRequest logoutRequest, string queryString)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[logoutRequest.Issuer];

            if (idp == null)
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }

            Saml2Utils.ValidateSignedQueryString(idp.SigningCertificate, queryString);
        }

        /// <summary>
        /// Checks the signature of the given logoutResponse assuming
        /// the signature is within the XML.
        /// </summary>
        /// <param name="logoutResponse">SAMLv2 LogoutRequest object.</param>
        private void CheckSignature(LogoutResponse logoutResponse)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[logoutResponse.Issuer];

            if (idp == null)
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }

            Saml2Utils.ValidateSignedXml(
                idp.SigningCertificate,
                logoutResponse.XmlDom,
                logoutResponse.XmlSignature,
                logoutResponse.Id);
        }

        /// <summary>
        /// Checks the signature of the given LogoutResponse with
        /// the raw query string.
        /// </summary>
        /// <param name="logoutResponse">SAMLv2 LogoutResponse object.</param>
        /// <param name="queryString">
        /// Raw query string that contains the response and possible signature.
        /// </param>
        private void CheckSignature(LogoutResponse logoutResponse, string queryString)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[logoutResponse.Issuer];

            Saml2Utils.ValidateSignedQueryString(idp.SigningCertificate, queryString);
        }

        /// <summary>
        /// Checks to confirm the issuer and hosted service provider are in
        /// the same circle of trust, otherwise a Saml2Exception is thrown.
        /// </summary>
        /// <param name="idpEntityId">IDP entity ID</param>
        private void CheckCircleOfTrust(string idpEntityId)
        {
            string spEntityId = this.ServiceProvider.EntityId;

            foreach (string cotName in this.CircleOfTrusts.Keys)
            {
                CircleOfTrust cot = (CircleOfTrust)this.CircleOfTrusts[cotName];
                if (cot.AreProvidersTrusted(spEntityId, idpEntityId))
                {
                    return;
                }
            }

            throw new Saml2Exception(Resources.InvalidIdpEntityIdNotInCircleOfTrust);
        }

        /// <summary>
        /// Gets the Identity Provider associated with the specified artifact.
        /// The currently maintained list of IDPs each have their entity ID
        /// hashed and compared with the given artifact's source ID to make
        /// the correct determination.
        /// </summary>
        /// <param name="artifact">SAML artifact.</param>
        /// <returns>
        /// Identity Provider who's entity ID matches the source ID
        /// within the artifact, null if not found.
        /// </returns>
        private IdentityProvider GetIdpFromArtifact(Artifact artifact)
        {
            SHA1 sha1 = new SHA1CryptoServiceProvider();
            IdentityProvider idp = null;
            string idpEntityIdHashed = null;

            foreach (string idpEntityId in this.IdentityProviders.Keys)
            {
                idpEntityIdHashed = BitConverter.ToString(sha1.ComputeHash(Encoding.UTF8.GetBytes(idpEntityId)));
                idpEntityIdHashed = idpEntityIdHashed.Replace("-", string.Empty);

                if (idpEntityIdHashed == artifact.SourceId)
                {
                    idp = (IdentityProvider)this.IdentityProviders[idpEntityId];
                    break;
                }
            }

            return idp;
        }

        /// <summary>
        /// Validates the given AuthnResponse object except for xml signature.
        /// XML signature checking is expected to be done prior to calling
        /// this method based on the appropriate profile.
        /// </summary>
        /// <param name="authnResponse">AuthnResponse object.</param>
        /// <param name="authnRequests">
        /// Collection of previously sent authnRequests used to compare with
        /// the InResponseTo attribute of the AuthnResponse (if present).
        /// </param>
        /// <see cref="ServiceProviderUtility.ValidateForArtifact"/>
        /// <see cref="ServiceProviderUtility.ValidateForPost(AuthnResponse, ICollection)"/>
        private void Validate(AuthnResponse authnResponse, ICollection authnRequests)
        {
            if (authnResponse.InResponseTo != null)
            {
                ServiceProviderUtility.CheckInResponseTo(authnResponse, authnRequests);
            }

            this.CheckIssuer(authnResponse.Issuer);
            ServiceProviderUtility.CheckStatusCode(authnResponse.StatusCode);
            ServiceProviderUtility.CheckConditionWithTime(authnResponse);
            this.CheckConditionWithAudience(authnResponse);
            this.CheckCircleOfTrust(authnResponse.Issuer);
        }

        /// <summary>
        /// Validates the given LogoutResponse object except for xml signature.
        /// XML signature checking is expected to be done prior to calling
        /// this method based on the appropriate profile.
        /// </summary>
        /// <param name="logoutResponse">LogoutResponse object.</param>
        /// <param name="logoutRequests">
        /// Collection of previously sent logoutRequests used to compare with
        /// the InResponseTo attribute of the LogoutResponse (if present).
        /// </param>
        private void Validate(LogoutResponse logoutResponse, ICollection logoutRequests)
        {
            if (logoutResponse == null)
            {
                throw new Saml2Exception(Resources.ServiceProviderUtilityLogoutResponseIsNull);
            }

            ServiceProviderUtility.CheckInResponseTo(logoutResponse, logoutRequests);
            this.CheckIssuer(logoutResponse.Issuer);
            this.CheckCircleOfTrust(logoutResponse.Issuer);
            ServiceProviderUtility.CheckStatusCode(logoutResponse.StatusCode);
        }

        /// <summary>
        /// Validates the given AttributeQueryResponse object except for xml signature.
        /// XML signature checking is expected to be done prior to calling
        /// this method based on the appropriate profile.
        /// </summary>
        /// <param name="attributeQueryResponse">AttributeQueryResponse object.</param>
        /// <param name="attributeQueryRequests">
        /// Collection of previously sent attributeQueryRequests used to compare with
        /// the InResponseTo attribute of the AttributeQueryResponse (if present).
        /// </param>
        private void Validate(AttributeQueryResponse attributeQueryResponse, ICollection attributeQueryRequests)
        {
            if (attributeQueryResponse == null)
            {
                throw new Saml2Exception(Resources.ServiceProviderUtilityAttributeQueryResponseIsNull);
            }

            ServiceProviderUtility.CheckInResponseTo(attributeQueryResponse, attributeQueryRequests);
            this.CheckIssuer(attributeQueryResponse.Issuer);
            this.CheckCircleOfTrust(attributeQueryResponse.Issuer);
            ServiceProviderUtility.CheckStatusCode(attributeQueryResponse.StatusCode);
        }

        /// <summary>
        /// Checks the signature of the given attributeQueryResponse assuming
        /// the signature is within the XML.
        /// </summary>
        /// <param name="attributeQueryResponse">SAMLv2 AttributeQueryResponse object.</param>
        private void CheckSignature(AttributeQueryResponse attributeQueryResponse)
        {
            IdentityProvider idp = (IdentityProvider)this.IdentityProviders[attributeQueryResponse.Issuer];

            if (idp == null)
            {
                throw new Saml2Exception(Resources.InvalidIssuer);
            }

            Saml2Utils.ValidateSignedXml(
                idp.SigningCertificate,
                attributeQueryResponse.XmlDom,
                attributeQueryResponse.XmlSignature,
                attributeQueryResponse.Id);
        }

        #endregion
    }
}
