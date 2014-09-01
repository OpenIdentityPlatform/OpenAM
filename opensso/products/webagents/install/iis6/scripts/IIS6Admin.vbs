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
' $Id: IIS6Admin.vbs,v 1.2 2009/07/21 19:15:31 robertis Exp $
'
' Copyright 2007 Sun Microsystems Inc. All Rights Reserved
'
'---------------------------------------------------------------------------
' Configures/UnConfigures the WildCard Application Map for a Web Site
'
' Requires:
'    -config/-unconfig 
'
' Usage: IIS6admin.vbs -config/-unconfig <config-filename>
'
' Example:
'    IIS6admin -config agentConfig
'
'
'Reference Section present at the end of the file lists the url's that was
'used to develop this script
'--------------------------------------------------------------------------

Dim WshShell, installDir, iis6ConfigDir, iis6LogsDir, iis6AuditDir, iis6DebugDir, identifier, newIdentifier
Dim origBootstrapFile, origConfigFile, newConfigDir, newLogsDir, newAuditDir, newDebugDir
Dim newBootstrapFile, newConfigFile
Dim regKey, responseFile, sLine, aLine, wildCardMap
Dim NewMaps(), count, objWebRoot, IIsWebServiceObj, FSO, dict, dict1
Dim scriptFullName, currDir
Dim iis6InstanceDir
Const ForReading = 1, ForWriting = 2

Set Args = WScript.Arguments
if Args.Count < 2 Then
   WScript.Echo "Incorrect Number of arguments"
   WScript.Echo "Syntax: IIS6Admin.vbs -config/-unconfig <config-filename>"
   WScript.Quit(1)
end if

Set dict1 = CreateObject("Scripting.Dictionary")
Set WshShell = CreateObject("WScript.Shell")

'// Set the correct path where the script is located
scriptFullName = WScript.ScriptFullName
currDir = split(scriptFullName, "\IIS6Admin.vbs")
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
' 1. Opens and reads the configuration file generated from IIS6CreateConfig.vbs
' 2. Populates few of the tokens request for agent configuration
' 3. Enables "All Unknown ISAPI Extensions"
' 4. Reads all the application mapping for the site on which the agent
'    will be configured
'----------------------------------------------------------------------------
Function Init()
  Dim correctConfigFile

  WScript.Echo "Copyright c 2009 Sun Microsystems, Inc. All rights reserved"
  WScript.Echo "Use is subject to license terms"

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
  iis6InstanceDir = installDir + "\" + newIdentifier 
  iis6ConfigDir = iis6InstanceDir + "\config"
  iis6LogsDir = iis6InstanceDir + "\logs"
  iis6AuditDir = iis6LogsDir + "\audit"
  iis6DebugDir = iis6LogsDir + "\debug"

  newConfigDir = iis6ConfigDir  
  newLogsDir = iis6LogsDir  
  newAuditDir = iis6AuditDir  
  newDebugDir = iis6DebugDir 

  'Enable "All Unknown ISAPI Extensions"
  Set IIsWebServiceObj = GetObject("IIS://localhost/W3SVC") 
  IIsWebServiceObj.EnableExtensionFile "*.dll"
  IIsWebServiceObj.SetInfo

  'Get all the application mappings
  count = 0
  Set objWebRoot = GetObject("IIS://localhost/W3SVC/" & identifier & "/ROOT")

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
    WScript.Echo "Enter the Agent Resource File Name [IIS6Resource.en] :"
    resourceFile = WScript.StdIn.ReadLine
    if (resourceFile = "") then
       resourceFile = "IIS6Resource.en"
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
'    configuration file created from IIS6CreateConfig.vbs
' 3. Under "iis_v6_WINNT_agent_3\web_agents\iis6_agent" directory, creates a 
'    sub-directory "Identifier_<id number>"
' 4. Updates the windows registry with the location of properties file
' 5. Adds the wild card application map to the web site for which the agent
'    is configured.
'----------------------------------------------------------------------------
Function AgentConfigure(WshShell, FSO, responseFile, objWebRoot, dict1)
   WScript.Echo dict1("129")

   if (FSO.FolderExists(iis6InstanceDir) = false) then
      FSO.CreateFolder(iis6InstanceDir)
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

   'Install the wild card application into this web site
   WScript.Echo dict1("132")
   For Each Item in objWebRoot.ScriptMaps
       ReDim Preserve NewMaps(count)
       NewMaps(count) = Item
       count = count + 1
   Next

   ReDim Preserve NewMaps(count)
   wildcardMap = "*," + installDir + "\bin\amiis6.dll" + ",0,All"
   NewMaps(count) = wildcardMap

   'Put all the mappings
   objWebRoot.PutEx 2, "ScriptMaps", NewMaps
   objWebRoot.SetInfo
   WSCript.Echo dict1("133")
End Function

'----------------------------------------------------------------------------
' Function Name : AgentUnConfigure
' Input : WshShell, FSO, dict1
' Output : None
' Description : The AgentUnConfigure() function performs the following tasks:
' 1.Removes both the properties file under the "Identifier_<id number>"
'   and the "Identifier_<id number>" directory itself
' 2.Removes the windows registry entry which had information about the 
'   location of properties file
' 3.Removes the wild card application map to the web site for which the agent
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

      'Remove the wild card application from this Site
      WScript.Echo dict1("137")
      For Each Item in objWebRoot.ScriptMaps
        if (instr(Item, "amiis6.dll") = 0) then
          ReDim Preserve NewMaps(count)
          NewMaps(count) = Item
          count = count + 1
        end if
      Next

      'Put the rest of the mappings
      objWebRoot.PutEx 2, "ScriptMaps", NewMaps
      objWebRoot.SetInfo

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
'--------------------------------------------------------------------------
'Reference
'
'The following is the list of url's that was referenced:
' 1. http://msdn.microsoft.com/library/default.asp?url=/library/en-us/script56/html/vsgrpFeatures.asp
' 2. http://msdn.microsoft.com/library/en-us/iissdk/iis/iis_reference.asp 
' 3. http://msdn.microsoft.com/library/default.asp?url=/library/en-us/iissdk/iis/ref_mb_scriptmaps.asp
'--------------------------------------------------------------------------

