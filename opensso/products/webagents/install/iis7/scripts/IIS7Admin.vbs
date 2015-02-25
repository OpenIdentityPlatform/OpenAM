' The contents of this file are subject to the terms
' of the Common Development and Distribution License
' (the License). You may not use this file except in
' compliance with the License.
'
' You can obtain a copy of the License at
' https://opensso.dev.java.net/public/CDDLv1.0.html or
' opensso/legal/CDDLv1.0.txt
' See the License for the specific language governing
' permission and limitations under the License.
'
' When distributing Covered Code, include this CDDL
' Header Notice in each file and include the License file
' at opensso/legal/CDDLv1.0.txt.
' If applicable, add the following below the CDDL Header,
' with the fields enclosed by brackets [] replaced by
' your own identifying information:
' "Portions Copyrighted [year] [name of copyright owner]"
'
' $Id: IIS7Admin.vbs,v 1.3 2009/07/28 18:42:33 robertis Exp $
'
' Copyright 2007 Sun Microsystems Inc. All Rights Reserved
'
' Portions Copyrighted 2014 ForgeRock AS
'
'---------------------------------------------------------------------------
' Configures/UnConfigures the IIS7 module for a Web Site
'
' Requires:
'    -config/-unconfig 
'
' Usage: IIS7Admin.vbs -config/-unconfig <config-filename>
'
' Example:
'    IIS7Admin -config agentConfig
'
'
'--------------------------------------------------------------------------

Dim WshShell, installDir, iis7ConfigDir, iis7LogsDir, iis7AuditDir, iis7DebugDir, identifier, newIdentifier
Dim origBootstrapFile, origConfigFile, newConfigDir, newLogsDir, newAuditDir, newDebugDir, moduleDir, modulePath
Dim newBootstrapFile, newConfigFile, newConfigFileTmp, agentName
Dim regKey, responseFile, sLine, aLine, wildCardMap
Dim NewMaps(), count, objWebRoot, IIsWebServiceObj, FSO, dict, dict1
Dim scriptFullName, currDir
Dim iis7InstanceDir
Const ForReading = 1, ForWriting = 2

Set Args = WScript.Arguments
if Args.Count < 2 Then
   WScript.Echo "Incorrect Number of arguments"
   WScript.Echo "Syntax: IIS7Admin.vbs -config/-unconfig <config-filename>"
   WScript.Quit(1)
end if

Set dict1 = CreateObject("Scripting.Dictionary")
Set WshShell = CreateObject("WScript.Shell")

'// Set the correct path where the script is located
scriptFullName = WScript.ScriptFullName
currDir = split(scriptFullName, "\IIS7Admin.vbs")
WshShell.currentDirectory = currDir(0)

' Entry point
Call Init()

'// Load the locale specific resource file
Call LoadResourceFile(FSO, dict1)

if (Args(0) = "-config") then
   Call AgentConfigure(WshShell, FSO, responseFile, objWebRoot, dict1)
elseif (Args(0) = "-unconfig") then
   Call AgentUnconfigure(WshShell, FSO, dict1)
end if

'----------------------------------------------------------------------------
' Function Name : Init
' Input : None
' Output : None
' Description : The Init() function performs the following tasks
' 1. Opens and reads the configuration file generated from IIS7CreateConfig.vbs
' 2. Populates few of the tokens request for agent configuration
'----------------------------------------------------------------------------
Function Init()
  Dim correctConfigFile

  WScript.Echo ""

  Set WshShell = WScript.CreateObject("WScript.Shell")
  Set FSO = CreateObject("Scripting.FileSystemObject")
  Set dict = CreateObject("Scripting.Dictionary")

  'Response file which has all the tokens defined.
  correctConfigFile = false
  responseFile = Args(1)
  do 
    if (FSO.FileExists(responseFile) = false) then
       WScript.Echo ""
       WScript.Echo "Config file specified does not exist. Re-enter the correct config file name"
       responseFile = WScript.StdIn.ReadLine
    else 
       correctConfigFile = true
    end if
  loop until (correctConfigFile = true)
  Set resFile = FSO.OpenTextFile(responseFile,ForReading,True)

  Do While resFile.AtEndOfStream <> True
     '//Read a line of the file
     sLine = resFile.ReadLine
     if (instr(sLine, "=") > 0) then
        'split the line on the = sign
        aLine = split(sLine, "=")
        firstPart = Trim(aline(0))
        secondPart = Trim(aline(1))
        'Read all the tokens and add to the dictionary
        dict.add firstPart, secondPart
     end if
  loop

  if dict.Exists("INSTALL_DIRECTORY") then
     installDir = dict("INSTALL_DIRECTORY")
  end if
  if dict.Exists("IDENTIFIER") then
      identifier = dict("IDENTIFIER")
      newIdentifier = "Identifier_" + identifier
  end if

  ' Generate the location of properties file
  iis7InstanceDir = installDir + "\" + newIdentifier 
  iis7ConfigDir = iis7InstanceDir + "\config"
  iis7LogsDir = iis7InstanceDir + "\logs"
  iis7AuditDir = iis7LogsDir + "\audit"
  iis7DebugDir = iis7LogsDir + "\debug"

  newConfigDir = iis7ConfigDir  
  newLogsDir = iis7LogsDir
  newAuditDir = iis7AuditDir
  newDebugDir = iis7DebugDir 

  moduleDir = installDir + "\bin"
  modulePath = moduleDir + "\amiis7module.dll"
  agentName = "iis7agent"

End Function

'----------------------------------------------------------------------------
' Function Name : LoadResourceFile
' Input : FSO, dict1
' Output : None
' Description : The AgentConfigure() function performs the following tasks:
' 1. By default loads the English Resource file if the user does not specifies
'    any other resource file
' 2. Checks whether the resource file exists, if not will display an error and
'    prompts the user to re-enter the resource file name
' 3. Opens the resource file and reads the token as name value pairs and
'    populates the dictionary table to be used by subsequent functions.
'----------------------------------------------------------------------------
Function LoadResourceFile(FSO, dict1)

  Dim resourceFile, resFile, sLine, aLine, firstPart, secondPart
  Dim correctResourceFile

  correctResourceFile = false
  do 
    WScript.Echo ""
    WScript.Echo "Enter the Agent Resource File Name [IIS7Resource.en] :"
    resourceFile = WScript.StdIn.ReadLine
    if (resourceFile = "") then
       resourceFile = "IIS7Resource.en"
       correctResourceFile = true
    elseif (FSO.FileExists(resourceFile) = false) then
          WScript.Echo "Resource File specified does not exist"
        else
          correctResourceFile = true
    end if
  loop until (correctResourceFile = true)

  Set resFile = FSO.OpenTextFile(resourceFile,ForReading,True)

  Do While resFile.AtEndOfStream <> True
     '//Read a line of the file
     sLine = resFile.ReadLine
     if (instr(sLine, "=") > 0) then
        'split the line on the = sign
        aLine = split(sLine, "=")
        firstPart = Trim(aline(0))
        secondPart = Trim(aline(1))
        'Read all the tokens and add to the dictionary
        dict1.add firstPart, secondPart
     end if
  loop
End Function

'----------------------------------------------------------------------------
' Function Name : AgentConfigure
' Input : WshShell, FSO, responseFile, objWebRoot, dict1
' Output : None
' Description : The AgentConfigure() function performs the following tasks:
' 1. Opens the properties file 
' 2. Perfoms token replacement in properties file using the agent
'    configuration file created from IIS7CreateConfig.vbs
' 3. Under "iis_v7_WINNT_agent_3\web_agents\iis7_agent directory", creates a 
'    sub-directory "Identifier_<id number>"
' 4. Updates the windows registry with the location of properties file
' 5. Adds the IIS7 agent http module to the web site for which the agent
'    is configured.
'----------------------------------------------------------------------------
Function AgentConfigure(WshShell, FSO, responseFile, objWebRoot, dict1)
   WScript.Echo dict1("129")

   if (FSO.FolderExists(iis7InstanceDir) = false) then
      FSO.CreateFolder(iis7InstanceDir)
   end if
   if (FSO.FolderExists(newConfigDir) = false) then
      FSO.CreateFolder(newConfigDir)
   end if
   if (FSO.FolderExists(newLogsDir) = false) then
      FSO.CreateFolder(newLogsDir)
   end if
   if (FSO.FolderExists(newAuditDir) = false) then
      FSO.CreateFolder(newAuditDir)
   end if
   if (FSO.FolderExists(newDebugDir) = false) then
      FSO.CreateFolder(newDebugDir)
   end if

   WScript.Echo dict1("130")
   origBootstrapFile = installDir + "\config\OpenSSOAgentBootstrap.template"
   origConfigFile = installDir + "\config\OpenSSOAgentConfiguration.template"
   newBootstrapFile = newConfigDir + "\OpenSSOAgentBootstrap.properties"
   newConfigFile = newConfigDir + "\OpenSSOAgentConfiguration.properties"
   FSO.CopyFile origBootstrapFile, newBootstrapFile
   FSO.CopyFile origConfigFile, newConfigFile

   'Perform token replacement in properties
   With New RegExp
     .Pattern = "^(.*?) = (.*?)$"
     .Multiline = True
     .Global = True
     Set Tokens = .Execute(FSO.OpenTextFile(responseFile, ForReading, False).ReadAll)
   End With

   B = FSO.OpenTextFile(newBootstrapFile, ForReading, True).ReadAll

   For Each Token In Tokens
     B = Replace(B, Token.Submatches(0), Token.Submatches(1))
   Next

   FSO.OpenTextFile(newBootstrapFile, ForWriting, False).Write B


   B = FSO.OpenTextFile(newConfigFile, ForReading, True).ReadAll

   For Each Token In Tokens
     B = Replace(B, Token.Submatches(0), Token.Submatches(1))
   Next

   FSO.OpenTextFile(newConfigFile, ForWriting, False).Write B

   WScript.Sleep(100)

   Call ConfigureDll()
   WSCript.Echo dict1("133")
End Function

Function ConfigureDll()
    Dim oExecInst, strCmdInst, oExecAdd, strCmdAdd, siteName, siteId, xmlElm
    Set xmlDoc = CreateObject("Microsoft.XMLDOM")
    xmlDoc.async = false
    xmlDoc.resolveExternals = false
    xmlDoc.validateOnParse = false
    strCmd = "%systemroot%\system32\inetsrv\appcmd.exe list sites /xml" 
    Set objExecObject = WshShell.Exec(strCmd)
    xmlDoc.loadXML(objExecObject.StdOut.ReadAll)
    Set objNodeList = xmlDoc.getElementsByTagName("SITE")
    If objNodeList.length > 0 then
     For each xmlElm in objNodeList
      siteId = xmlElm.getAttribute("SITE.ID")
      If siteId = identifier then
       siteName = xmlElm.getAttribute("SITE.NAME")
       Exit For
      End If
     Next
    Else
     WScript.Echo "Error fetching SITE name."
    End If
    If siteName <> "" then  
     strCmdInst = "%systemroot%\system32\inetsrv\appcmd.exe install module /name:" + agentName + " /image:" + modulePath + " /add:false"
     Set oExecInst = WshShell.Exec(strCmdInst)
     WScript.Echo "Installing policy web agent module in IIS (status: " & oExecInst.Status & ")"
     WScript.Sleep(1000)
     strCmdAdd = "%systemroot%\system32\inetsrv\appcmd.exe add module /name:" + agentName + " /app.name:" + chr(34) + siteName + "/" + chr(34)
     Set oExecAdd = WshShell.Exec(strCmdAdd)
     WScript.Echo "Adding policy web agent module to " & chr(34) & siteName & chr(34) & " (status: " & oExecAdd.Status & ")"
    Else
     WScript.Echo "Policy web agent module installation failed."
     WScript.Quit(0)
    End If
End Function

Function UnconfigureDll()
    Dim  oExec, oExecDel, strCmd, strCmdDel, siteName, siteId, xmlElm
    Set xmlDoc = CreateObject("Microsoft.XMLDOM")
    xmlDoc.async = false
    xmlDoc.resolveExternals = false
    xmlDoc.validateOnParse = false
    strCmd = "%systemroot%\system32\inetsrv\appcmd.exe list sites /xml" 
    Set objExecObject = WshShell.Exec(strCmd)
    xmlDoc.loadXML(objExecObject.StdOut.ReadAll)
    Set objNodeList = xmlDoc.getElementsByTagName("SITE")
    If objNodeList.length > 0 then
     For each xmlElm in objNodeList
      siteId = xmlElm.getAttribute("SITE.ID")
      If siteId = identifier then
       siteName = xmlElm.getAttribute("SITE.NAME")
       Exit For
      End If
     Next
    Else
     WScript.Echo "Error fetching SITE name."
    End If
    If siteName <> "" then
     strCmdDel = "%systemroot%\system32\inetsrv\appcmd.exe delete module " + agentName + " /app.name:" + chr(34) + siteName + "/" + chr(34)
     Set oExecDel = WshShell.Exec(strCmdDel)
     WScript.Echo "Removing policy web agent module from " & chr(34) & siteName & chr(34) & " (status: " & oExecDel.Status & ")"
     strCmd = "%systemroot%\system32\inetsrv\appcmd.exe uninstall module " + agentName
     Set oExec = WshShell.Exec(strCmd)
     WScript.Echo "Uninstalling policy web agent module from IIS (status: " & oExec.Status & ")"
    Else
     WScript.Echo "Policy web agent module uninstallation failed."
     WScript.Quit(0)
    End If
End Function

'----------------------------------------------------------------------------
' Function Name : AgentUnConfigure
' Input : WshShell, FSO, dict1
' Output : None
' Description : The AgentUnConfigure() function performs the following tasks:
' 1.Removes both the OpenSSOAgentBootstrap.properties and OpenSSOAgentConfiguration.properties 
'   file under the "Identifier_<id number>" and the "Identifier_<id number>" directory itself
' 2.Removes the windows registry entry which had information about the 
'   location of properties file
' 3.Removes the IIS7 agent http module from  the web site for which the agent
'   was configured.
'----------------------------------------------------------------------------
Function AgentUnConfigure(WshShell, FSO, dict1)
   'Remove the Agent Config Directory
   delBootstrapFile = newConfigDir + "\OpenSSOAgentBootstrap.properties"
   delConfigFile = newConfigDir + "\OpenSSOAgentConfiguration.properties"

   if (FSO.FileExists(delBootstrapFile)) then
      WScript.Echo dict1("143")
      FSO.DeleteFile(delBootstrapFile)
   else
      WScript.Echo dict1("144")
   end if

   if (FSO.FileExists(delConfigFile)) then
      WScript.Echo dict1("134")
      FSO.DeleteFile(delConfigFile)
   else
      WScript.Echo dict1("139")
   end if

   if (FSO.FolderExists(newConfigDir)) then
      WScript.Echo dict1("135")
      WScript.Echo newConfigDir
      FSO.DeleteFolder(newConfigDir)

      Call UnconfigureDll()

      WScript.Echo dict1("138")
   else
      WScript.Echo dict1("140")
      WScript.Echo dict1("141")
      WScript.Echo dict1("142")
   end if


End Function

set resFile = nothing
Set FSO = nothing

WScript.Quit(0)

