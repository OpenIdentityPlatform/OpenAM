/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Provider.cs,v 1.1 2009/12/22 23:24:49 robertis Exp $
 *
 *
 */


using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Web;
using System.Web.Security;
using System.Xml;
using System.Configuration;
using System.Configuration.Provider;
using System.IO;
using System.Security.Principal;
using System.Net;
using System.Collections;
using System.Collections.Specialized;
using Microsoft.Win32;


namespace OpenSSOProvider
{
    public class customUser : MembershipProvider
    {

        private string pApplicationName;
        private bool pEnablePasswordReset;
        private bool pEnablePasswordRetrieval;
        private bool pRequiresQuestionAndAnswer;
        private bool pRequiresUniqueEmail;
        private int pMaxInvalidPasswordAttempts;
        private int pPasswordAttemptWindow;
        private MembershipPasswordFormat pPasswordFormat;

        // Code in here.

        public override bool EnablePasswordRetrieval
        {
            get { return pEnablePasswordRetrieval; }
        }
        public override bool EnablePasswordReset
        {
            get { return pEnablePasswordReset; }
        }
        public override bool RequiresQuestionAndAnswer
        {
            get { return pRequiresQuestionAndAnswer; }
        }
        public override bool RequiresUniqueEmail
        {
            get { return pRequiresUniqueEmail; }
        }
        public override int MaxInvalidPasswordAttempts
        {
            get { return pMaxInvalidPasswordAttempts; }
        }
        public override int PasswordAttemptWindow
        {
            get { return pPasswordAttemptWindow; }
        }
        public override string ApplicationName
        {
            get { return pApplicationName; }
            set { pApplicationName = value; }
        }
        public override MembershipPasswordFormat PasswordFormat
        {
            get { return pPasswordFormat; }
        }
        private int pMinRequiredNonAlphanumericCharacters;
        public override int MinRequiredNonAlphanumericCharacters
        {
            get { return pMinRequiredNonAlphanumericCharacters; }
        }
        private int pMinRequiredPasswordLength;
        public override int MinRequiredPasswordLength
        {
            get { return pMinRequiredPasswordLength; }
        }
        private string pPasswordStrengthRegularExpression;
        public override string PasswordStrengthRegularExpression
        {
            get { return pPasswordStrengthRegularExpression; }
        }
        public override bool ChangePassword(string username, string oldPassword, string newPassword)
        {
            throw new NotSupportedException();
        }
        public override MembershipUser CreateUser(string username,
             string password,
             string email,
             string passwordQuestion,
             string passwordAnswer,
             bool isApproved,
             object providerUserKey,
             out MembershipCreateStatus status)
        {
            throw new NotSupportedException();
        }

        public override bool ChangePasswordQuestionAndAnswer(string username,
                      string password,
                      string newPwdQuestion,
                      string newPwdAnswer)
        {
            throw new NotSupportedException();
        }
        public override string GetPassword(string username, string answer)
        {
            throw new NotSupportedException();
        }
        public override string ResetPassword(string username, string answer)
        {
            throw new NotSupportedException();
        }
        public override void UpdateUser(MembershipUser user)
        {
            throw new NotSupportedException();
        }
        public override bool UnlockUser(string username)
        {
            throw new NotSupportedException();
        }
        public override bool DeleteUser(string username, bool deleteAllRelatedData)
        {
            throw new NotSupportedException();
        }
        public override MembershipUserCollection GetAllUsers(int pageIndex, int pageSize, out int totalRecords)
        {
            throw new NotSupportedException();
        }
        public override int GetNumberOfUsersOnline()
        {
            throw new NotSupportedException();
        }


        public override MembershipUser GetUser(object providerUserKey, bool userIsOnline)
        {
            MembershipUser ret = null;

            try
            {
                string url = AgentHelper.serverUrl;
                url += "/identity/read";
                string token = "uid";

                if (AgentHelper.IsAgentTokenValid() == false)
                {
                    AgentHelper.AuthenticateAgent();
                }
                string data = "name=" + HttpUtility.UrlEncode(providerUserKey.ToString()) + "&admin=" + HttpUtility.UrlEncode(AgentHelper.ssoToken.Trim());

                string response = AgentHelper.MakePostRestCall(url, data);
                string name = AgentHelper.GetDataFromResponse(response, token, "identitydetails");
                string email = AgentHelper.GetDataFromResponse(response, "mail", "identitydetails");

                ret = new MembershipUser(Membership.Provider.Name,
                                name, name, email,
                                string.Empty, string.Empty, true, false,
                    //FIXME instead of the MinValue, should be the creation time
                                DateTime.MinValue,
                                DateTime.Today, DateTime.Today, DateTime.Today,
                                DateTime.MinValue);
            }
            catch (Exception ex)
            {
                //Log this exception
            }

            return ret;
        }

        public override MembershipUser GetUser(string username, bool userIsOnline)
        {

            //Here the SSO token is that of the user, not the amadmin.

            MembershipUser ret = null;
            try
            {


                string url = AgentHelper.serverUrl;
                url += "/identity/read";
                string token = "uid";

                if (AgentHelper.IsAgentTokenValid() == false)
                {
                    AgentHelper.AuthenticateAgent();
                }
                string data = "name=" + HttpUtility.UrlEncode(username) + "&admin=" + HttpUtility.UrlEncode(AgentHelper.ssoToken.Trim());

                string response = AgentHelper.MakePostRestCall(url, data);
                string name = AgentHelper.GetDataFromResponse(response, token, "identitydetails");
                string email = AgentHelper.GetDataFromResponse(response, "mail", "identitydetails");

                ret = new MembershipUser(Membership.Provider.Name,
                                name, name, email,
                                string.Empty, string.Empty, true, false,
                    //FIXME instead of MinValue, should be the user creation time
                                DateTime.MinValue,
                                DateTime.Today, DateTime.Today, DateTime.Today,
                                DateTime.MinValue);

            }
            catch (Exception ex)
            {
            }

            return ret;
        }


        public override string GetUserNameByEmail(string email)
        {

            string ret = string.Empty;

            try
            {
                string url = AgentHelper.serverUrl;
                url += "/identity/search";


                if (AgentHelper.IsAgentTokenValid() == false)
                {
                    AgentHelper.AuthenticateAgent();
                }
                string data = "attributes_names=mail&attributes_values_mail=" + HttpUtility.UrlEncode(email) + "&admin=" + HttpUtility.UrlEncode(AgentHelper.ssoToken.Trim());

                string response = AgentHelper.MakePostRestCall(url, data);

                int i1 = response.IndexOf("=");
                ret = response.Substring(i1 + 1);
            }
            catch (Exception ex)
            {
            }

            return ret;
        }


        public override bool ValidateUser(string username, string password)
        {
            bool ret = false;
            ret = true;
            return ret;
        }


        /*
         * For example, if the emailToMatch parameter is set to "address@example.com," then users with the e-mail addresses "address1@example.com," 
         * "address2@example.com," and so on are returned. Wildcard support is included based on the data source. 
         * Users are returned in alphabetical order by user name.
         * For now just do exact match, later, retrive the userid from the email and put it inside *userid*, by a separate routine and test the scenarios,
         * 
         * FIXME:
         * when typed user*, it shows only user2, in the people picker 
         */


        public override MembershipUserCollection FindUsersByEmail(string emailToMatch, int pageIndex, int pageSize, out int totalRecords)
        {
            MembershipUserCollection ret = null;
            totalRecords = 0;

            try
            {

                string url = AgentHelper.serverUrl;
                url += "/identity/search";


                if (AgentHelper.IsAgentTokenValid() == false)
                {
                    AgentHelper.AuthenticateAgent();
                }

                if (emailToMatch[emailToMatch.Length - 1] == '%')
                {
                    emailToMatch = emailToMatch.Substring(0, emailToMatch.Length - 1);
                }
                string email = emailToMatch;
                string data = "attributes_names=mail&attributes_values_mail=" + email + "&admin=" + HttpUtility.UrlEncode(AgentHelper.ssoToken.Trim());

                string response = AgentHelper.MakePostRestCall(url, data);

                ret = new MembershipUserCollection();
                while (response.IndexOf("=") != -1)
                {
                    int i1 = response.IndexOf("=");
                    response = response.Substring(i1 + 1);
                    int i2 = response.IndexOf("string");
                    string username;

                    if (i2 > 0)
                        username = (response.Substring(0, i2 - 1)).Trim();
                    else
                        username = (response.Substring(0)).Trim();

                    MembershipUser muser = GetUser(username, true);
                    if (muser != null)
                        ret.Add(muser);
                    totalRecords++;

                }

            }
            catch (Exception ex)
            {
            }

            return ret;
        }


        public override MembershipUserCollection FindUsersByName(string usernameToMatch, int pageIndex, int pageSize, out int totalRecords)
        {

            MembershipUserCollection ret = null;
            totalRecords = 0;

            try
            {
                // Initialize the number of records found.
                totalRecords = 0;

                string url = AgentHelper.serverUrl;
                url += "/identity/search";

                if (AgentHelper.IsAgentTokenValid() == false)
                {
                    AgentHelper.AuthenticateAgent();
                }

                if (usernameToMatch[usernameToMatch.Length - 1] == '%')
                {
                    usernameToMatch = usernameToMatch.Substring(0, usernameToMatch.Length - 1);
                }
                string name = usernameToMatch;
                string nameAttr = "uid";
                string data = "attributes_names=" + HttpUtility.UrlEncode(nameAttr) + "&attributes_values_" + nameAttr + "=" + name + "&admin=" + HttpUtility.UrlEncode(AgentHelper.ssoToken.Trim());

                string response = AgentHelper.MakePostRestCall(url, data);

                ret = new MembershipUserCollection();
                while (response.IndexOf("=") != -1)
                {
                    int i1 = response.IndexOf("=");
                    response = response.Substring(i1 + 1);
                    int i2 = response.IndexOf("string");
                    string username;

                    if (i2 > 0)
                        username = (response.Substring(0, i2 - 1)).Trim();
                    else
                        username = (response.Substring(0)).Trim();

                    MembershipUser muser = GetUser(username, true);
                    if (muser != null)
                        ret.Add(muser);
                    totalRecords++;

                }
            }
            catch (Exception ex)
            {
            }

            return ret;
        }


    }



    public sealed class AgentConfSingleton
    {
        private static volatile AgentConfSingleton instance;
        private static object syncRoot = new Object();

        private AgentConfSingleton()
        {
            LoadPropertiesFile();
            LoadVariables();
        }

        public static AgentConfSingleton Instance
        {
            get
            {
                if (instance == null)
                {
                    lock (syncRoot)
                    {
                        if (instance == null)
                            instance = new AgentConfSingleton();
                    }
                }

                return instance;
            }
        }


        private static string filePath;
        private static StringDictionary values = new StringDictionary();
        private static string serverUrl;
        private static string loginUrl;
        private static string username;
        private static string password;
        private static string debugFileName;
        private static string debugLevel;
        private static string cookieName;
        private static string loginAttribute;

        public string ServerUrl
        {
            get { return serverUrl; }
        }

        public string LoginUrl
        {
            get { return loginUrl; }
        }

        public string Username
        {
            get
            {
                return username;
            }
        }

        public string Password
        {
            get { return password; }
        }

        public string DebugFileName
        {
            get { return debugFileName; }
        }

        public string DebugLevel
        {
            get { return debugLevel; }
        }

        public string CookieName
        {
            get { return cookieName; }
        }

        public string LoginAttribute
        {
            get { return loginAttribute; }
        }

        public static void LoadVariables()
        {
            serverUrl = values["com.sun.identity.agents.config.server.url"];
            loginUrl = values["com.sun.identity.agents.config.login.url"];
            username = values["com.sun.identity.agents.config.username"];
            password = values["com.sun.identity.agents.config.password"];
            debugFileName = values["com.sun.identity.agents.config.debug.file"];
            debugLevel = values["com.sun.identity.agents.config.debug.level"];
            cookieName = values["com.sun.identity.agents.config.cookie.name"];
            loginAttribute = values["com.sun.identity.agents.config.login.attribute"];
        }


        public static void LoadPropertiesFile()
        {

            RegistryKey hklm = Registry.LocalMachine;
            hklm = hklm.OpenSubKey("SOFTWARE\\Sun Microsystems\\OpenSSO Sharepoint Agent");
            Object obp = hklm.GetValue("Path");
            filePath = obp.ToString();
            filePath += "\\agents.config";


            FileStream fs = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            char[] NameValueSeparator = new char[] { '=' };


            try
            {
                StreamReader reader = new StreamReader(fs);

                string line;
                while ((line = reader.ReadLine()) != null)
                {
                    bool lineProcessed = false;

                    try
                    {
                        string trimmedLine = line.Trim();
                        if (trimmedLine.Length == 0) continue;

                        if (line.StartsWith("#")) continue;

                        string[] pair = line.Split(NameValueSeparator, 2);

                        string name = pair[0].Trim();
                        string val = string.Empty;
                        if (pair.Length > 1) val = pair[1].Trim();

                        values[name] = val;
                    }
                    finally
                    {
                        //do cleanups here
                    }
                }
            }
            finally
            {
                fs.Close();
            }
        }


    }


    internal class AgentHelper
    {
        private static volatile AgentConfSingleton agentConf = AgentConfSingleton.Instance;
        public static string ssoToken = "";

        public static string serverUrl = agentConf.ServerUrl;
        public static string loginUrl = agentConf.LoginUrl;
        private static string username = agentConf.Username;
        private static string password = agentConf.Password;
        public static string debugFile = agentConf.DebugFileName;
        public static string debugLevel = agentConf.DebugLevel;
        public static string cookieName = agentConf.CookieName;
        public static string loginAttribute = agentConf.LoginAttribute;


        public static bool AuthenticateAgent()
        {
            string response = "";
            bool successflag = true;
            string authnurl = serverUrl + "/identity/authenticate";
            string contentdata = "username=" + HttpUtility.UrlEncode(username) +
                "&password=" + HttpUtility.UrlEncode(password);

            try
            {
                response = MakePostRestCall(authnurl, contentdata);
                int i1 = response.IndexOf("=");
                ssoToken = (response.Substring(i1 + 1)).Trim();
            }
            catch (Exception ex)
            {
                successflag = false;
            }

            return successflag;
        }

        public static bool IsAgentTokenValid()
        {
            bool isValid = false;
            if (ssoToken.Equals(""))
            {
                return isValid;
            }

            string response = "";
            string url = serverUrl + "/identity/isTokenValid";
            string queryparams = "tokenid=";
            queryparams += ssoToken;
            response = MakePostRestCall(url, queryparams);
            response = response.Trim();
            int i1 = response.IndexOf("=");
            string boolresponse = response.Substring(i1 + 1, 4);
            if (boolresponse.Equals("true"))
            {
                isValid = true;
            }

            return isValid;
        }

        public static string GetUserInfo()
        {
            string userInfo = "";

            return userInfo;
        }

        public static string MakePostRestCall(string url, string content)
        {
            string result = "";
            Uri address = new Uri(url);
            HttpWebRequest request = WebRequest.Create(address) as HttpWebRequest;
            request.Method = "POST";
            request.ContentType = "application/x-www-form-urlencoded";

            StringBuilder data = new StringBuilder();
            data.Append(content);

            // Create a byte array of the data we want to send  
            byte[] byteData = UTF8Encoding.UTF8.GetBytes(data.ToString());

            request.ContentLength = byteData.Length;

            using (Stream postStream = request.GetRequestStream())
            {
                postStream.Write(byteData, 0, byteData.Length);
            }

            //right now, the 401 REST responses, which throws an exception, are handled in the callers
            using (HttpWebResponse response = request.GetResponse() as HttpWebResponse)
            {
                StreamReader reader = new StreamReader(response.GetResponseStream());
                result = reader.ReadToEnd();
            }

            return result;
        }


        public static string GetDataFromResponse(string data, string token, string marker)
        {
            string result = "";

            int i1 = data.IndexOf(token);
            string s1 = data.Substring(i1);
            int i2 = s1.IndexOf("=");
            string s2 = s1.Substring(i2 + 1);
            int i3 = s2.IndexOf(marker);
            result = s2.Substring(0, i3 - 1);
            return result;

        }
    }


    public class openssoModule : IHttpModule
    {
        public openssoModule()
        {
        }

        public String ModuleName
        {
            get { return "openssoModule"; }
        }

        public void Init(HttpApplication application)
        {
            application.AuthenticateRequest += (new EventHandler(this.Application_AuthenticateRequest));
            application.EndRequest += (new EventHandler(this.Application_EndRequest));

        }

        private void Application_AuthenticateRequest(Object source,
            EventArgs e)
        {
            string[] roles = null;
            string userName;
            string CookieName = AgentHelper.cookieName;
            string token = AgentHelper.loginAttribute;


            HttpCookie Cookie = HttpContext.Current.Request.Cookies[CookieName];

            LogModuleMsg("Inside Authenticate request");

            //if iplanetDirPro cookie is present ..
            //unpack the cookie and set the username in the context
            if (Cookie != null)
            {
                HttpApplication application = (HttpApplication)source;
                HttpContext context = application.Context;

                string url = AgentHelper.serverUrl;
                url += "/identity/attributes";

                //string data = "attributes_names=uid&subjectid=" + HttpUtility.UrlEncode(Cookie.Value);
                string data = "attributes_names=uid&subjectid=" + Cookie.Value;

                string restResponse = AgentHelper.MakePostRestCall(url, data);
                userName = GetUserId(restResponse, token);

                GenericIdentity userIdentity = new GenericIdentity(userName);
                GenericPrincipal principal = new GenericPrincipal(userIdentity, roles);

                context.User = principal;

            }

        }

        private string GetUserId(string data, string token)
        {
            string result = "";
            int i1 = data.IndexOf(token);
            string s1 = data.Substring(i1);
            int i2 = s1.IndexOf("=");
            string s2 = s1.Substring(i2 + 1);
            int i3 = s2.IndexOf("userdetails");
            result = s2.Substring(0, i3 - 1);
            return result;

        }

        private void Application_EndRequest(Object source, EventArgs e)
        {
            HttpApplication application = (HttpApplication)source;
            HttpContext context = application.Context;
            string status = context.Response.Status;
            int statusCode = HttpContext.Current.Response.StatusCode;
            string incomingUrl = HttpContext.Current.Request.Url.AbsoluteUri;
            LogModuleMsg("Inside end request");
            if (statusCode == 401)
            {
                string redirectUrl = AgentHelper.loginUrl;
                redirectUrl += "?goto=" + incomingUrl;
                HttpContext.Current.Response.Redirect(redirectUrl, true);

            }
        }

        public void Dispose()
        {
        }


        public void LogModuleMsg(string message)
        {
            string filePath = AgentHelper.debugFile;

            try
            {
                StreamWriter sw = File.AppendText(filePath);
                sw.WriteLine(message);
                sw.Flush();
                sw.Close();
            }
            catch (Exception e)
            {

            }
        }
    }


}

