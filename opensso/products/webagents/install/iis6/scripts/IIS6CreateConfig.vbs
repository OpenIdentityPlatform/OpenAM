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
' $Id: IIS6CreateConfig.vbs,v 1.3 2009/07/28 18:41:45 robertis Exp $
'
' Copyright 2009 Sun Microsystems Inc. All Rights Reserved
'
'
'--------------------------------------------------------------------
Const ForReading = 1, ForWriting = 2
Dim installDir, setInstallDir, configInstallDir, portNumber,setPortNumber
Dim objWWW, Item, setIdentifier, protocol, setProtocol, agentDeploymentURI
Dim setAgentDeploymentURI, setPrimaryServerHost, primaryServerPort
Dim setPrimaryServerPort, primaryServerProtocol, setPrimaryServerProtocol
Dim primaryServerDeploymentURI, setPrimaryServerDeploymentURI, userName 
Dim setFailoverServerPort, failoverServerProtocol, setFailoverServerProtocol
Dim failoverServerDeploymentURI, setFailoverServerDeploymentURI, orgName
Dim failoverServerConsoleURI, setFailoverServerConsoleURI, failoverServerPort
Dim setOrgName, setUserName, setUserPassword, setPasswdKey, encryptFile, primaryServerURL
Dim encrypt, encryptedPwd, delEncryptedFile, cdssoEnabled, setCDSSOEnabled
Dim isVersion, correctVersion, setISVersionNumber, correctAgentPort
Dim correctIdentifier, correctAgentProtocol, correctPrimaryServerPort
Dim correctPrimaryServerProtocol, correctFailoverServerPort, tmpInstallDir
Dim correctFailoverServerProtocl, correctPassword, correctCDSSOFlag, dict
Dim setHostName, expHostName, encryptedPasswd, setFailoverServerHost, failoverServerURL
Dim scriptFullName, currDir, WshShell
Dim agentUrl, serverUrl, correctAgentUrl, correctServerUrl

Set Args = WScript.Arguments
if Args.Count < 1 Then
   WScript.Echo "Incorrect Number of arguments"
   WScript.Echo "Syntax: IIS6CreateConfig.vbs <config-filename>"
   WScript.Quit(1)
end if

WScript.Echo ""
WScript.Echo "Copyright c 2009 Sun Microsystems, Inc. All rights reserved"
WScript.Echo "Use is subject to license terms"

Set oFSO = CreateObject("Scripting.FileSystemObject")
Set dict = CreateObject("Scripting.Dictionary")
Set WshShell = CreateObject("WScript.Shell")

WScript.Echo "---------------------------------------------------------"
WScript.Echo "    Microsoft (TM) Internet Information Server (6.0)     "
WScript.Echo "---------------------------------------------------------"

'// Set the correct path where the script is located
scriptFullName = WScript.ScriptFullName
currDir = split(scriptFullName, "\IIS6CreateConfig.vbs")
WshShell.currentDirectory = currDir(0)

'// Load the locale specific resource file
Call LoadResourceFile(oFSO, dict)

'// Prompt user to enter Agent Details
Call GetAgentDetails(oFSO, dict)

'// Prompt user to enter Access Manager Details
Call GetAccessManagerDetails(oFSO, dict, WshShell)

'// Create config file
Call WriteConfigFile(oFSO, Args(0))

WScript.Quit(1)

'----------------------------------------------------------------------------
' Function Name : LoadResourceFile
' Input : oFSO, dict
' Output : None
' Description : The AgentConfigure() function performs the following tasks:
' 1. By default loads the English Resource file if the user does not specifies
'    any other resource file
' 2. Checks whether the resource file exists, if not will display an error and
'    prompts the user to re-enter the resource file name
' 3. Opens the resource file and reads the token as name value pairs and
'    populates the dictionary table to be used by subsequent functions.
'----------------------------------------------------------------------------
Function LoadResourceFile(oFSO, dict)

  Dim resourceFile, resFile, sLine, aLine, firstPart, secondPart
  Dim correctResourceFile

  correctResourceFile = false
  do 
    WScript.Echo "Enter the Agent Resource File Name [IIS6Resource.en] :"
    resourceFile = WScript.StdIn.ReadLine
    if (resourceFile = "") then
       resourceFile = "IIS6Resource.en"
       correctResourceFile = true
    elseif (oFSO.FileExists(resourceFile) = false) then
          WScript.Echo "Resource File specified does not exist"
        else 
	  correctResourceFile = true
    end if
  loop until (correctResourceFile = true)

  Set resFile = oFSO.OpenTextFile(resourceFile,ForReading,True)

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
End Function

'----------------------------------------------------------------------------
' Function Name : GetAgentDetails
' Input : oFSO, dict
' Output : None
' Description : The GetAgentDetails() function performs the following tasks:
' 1. Using the dictionary table prompts the user to enter Agent details and
'    stores the value in the variables
'----------------------------------------------------------------------------
Function GetAgentDetails(oFSO, dict)

  Set WshNetwork = WScript.CreateObject("WScript.Network")

  installDir = oFSO.GetAbsolutePathName("..\")
  tmpInstallDir = installDir

  setInstallDir = Replace(tmpInstallDir,"\","/")

  configInstallDir = Replace(installDir,"/","\")
  computerName = WshNetwork.ComputerName


  correctAgentUrl = false
  do 
    WScript.Echo dict("101")
    agentUrl = LCase(WScript.StdIn.ReadLine)
    
    if (IsValidUrl(agentUrl,true)) then
       correctAgentUrl = true
    else
       WScript.Echo ""
       WScript.Echo dict("105")
       WScript.Echo ""
    end if
  loop until (correctAgentUrl = true)
  WScript.Echo ""


  'Displaying the web sites instances
  WScript.Echo dict("102")
  WScript.Echo dict("103")
  Set objWWW = GetObject("IIS://" & computerName & "/W3SVC")
  For Each Item in objWWW
      if (Item.Class = "IIsWebServer") then
         Wscript.Echo Item.ServerComment + " (" + Item.name + ")"
      end if
  Next

  WScript.Echo ""
  correctIdentifier = false
  do 
      WScript.Echo dict("104")
      setIdentifier = WScript.StdIn.ReadLine
      Set objWWW = GetObject("IIS://" & computerName & "/W3SVC")
      For Each Item in objWWW
        if (Item.Class = "IIsWebServer") then
          if (setIdentifier = Item.name) then
            Exit Do
          end if
        end if
      Next
      if (correctIdentifier = false) then
        WScript.Echo dict("121")
      end if
  loop until (correctIdentifier = true)

  setAgentDeploymentURI="/amagent"

End Function

'----------------------------------------------------------------------------
' Function Name : GetAccessManagerDetails
' Input : oFSO, dict, WshShell
' Output : None
' Description : The GetAccessManagerDetails() function performs the following 
' tasks:
' 1. Using the dictionary table prompts the user to enter Access Manager
'    details and stores the value in the variables
' 2. Encrypts the shared secret password
'----------------------------------------------------------------------------
Function GetAccessManagerDetails(oFSO, dict, WshShell)

  WScript.Echo "------------------------------------------------"
  WScript.Echo "ForgeRock OpenAM" 
  WScript.Echo "------------------------------------------------"

  correctServerUrl = false
  do 
    WScript.Echo dict("108")
    serverUrl = LCase(WScript.StdIn.ReadLine)
    
    if (IsValidUrl(serverUrl,false)) then
       correctServerUrl = true
    else
       WScript.Echo ""
       WScript.Echo dict("109")
       WScript.Echo ""
    end if
  loop until (correctServerUrl = true)


  WScript.Echo ""
  WScript.Echo dict("145")
  setUserName = WScript.StdIn.ReadLine
  WScript.Echo ""
  do 
    WScript.Echo dict("106")
    passwdFile = WScript.StdIn.ReadLine
    WScript.Echo
    'First check if the file exist.
    if(oFSO.FileExists(passwdFile)) then
        Set passwdFile = oFSO.GetFile(passwdFile)
        if(passwdFile.Size > 0) then
            Set passwd = oFSO.OpenTextFile(passwdFile,ForReading,True)
            setUserPassword = passwd.ReadLine
            passwd.Close
            if (Trim(setUserPassword) = "") then
                WScript.Echo ""
                WScript.Echo dict("119")
                WScript.Echo ""
            end if
        else
            WScript.Echo dict("119")
        end if
    else
        WScript.Echo dict("107")
    end if
  loop until (Trim(setUserPassword) <> "")

  setUserPassword = Trim(setUserPassword)

  '// Encrypt the password
  encryptFile = configInstallDir + "\encryptPasswd"
  setPasswdKey = GetRandomString(10)
  WshShell.Run "cmd /c cd " + chr(34) + configInstallDir + chr(34) + "\bin" + "& cryptit.exe " + setUserPassword + " "+ setPasswdKey + " > " + chr(34) + encryptFile + chr(34), 0, true
  
  Set encrypt = oFSO.OpenTextFile(encryptFile,ForReading,True)
  encryptedPasswd = encrypt.ReadLine
  encrypt.Close
  WScript.Sleep(100)

  oFSO.DeleteFile(encryptFile)

  Set WshShell = nothing

End Function

'----------------------------------------------------------------------------
' Function Name : WriteConfigFile
' Input : oFSO, configFile
' Output : None
' Description : The WriteConfigFile() function performs the following tasks:
' 1.Creates the config file
' 2 Populates the name and value pairs to be used by the IIS6Admin.vbs
'----------------------------------------------------------------------------
Function WriteConfigFile(oFSO, configFile)

  '// Create the agentConfig file
  Set wTF = oFSO.OpenTextFile(configFile,ForWriting,True)
  wTF.WriteLine "INSTALL_DIRECTORY = " + configInstallDir
  wTF.WriteLine "IDENTIFIER = " + setIdentifier
  wTF.WriteLine "@AGENT_HOST@ = " + setHostName
  wTF.WriteLine "@AGENT_PREF_PORT@ = " + setPortNumber
  wTF.WriteLine "@AGENT_PREF_PROTO@ = " + setProtocol
  wTF.WriteLine "@AGENT_DEPLOY_URI@ = " + setAgentDeploymentURI

  primaryServerURL = setPrimaryServerProtocol + "://" + setPrimaryServerHost + ":"+ setPrimaryServerPort 

  wTF.WriteLine "@AM_SERVICES_HOST@ = " + setPrimaryServerHost
  wTF.WriteLine "@AM_SERVICES_PROTO@ = " + setPrimaryServerProtocol
  wTF.WriteLine "@AM_SERVICES_PORT@ = " + setPrimaryServerPort
  wTF.WriteLine "@AM_SERVICES_DEPLOY_URI@ = " + setPrimaryServerDeploymentURI
                                      
  wTF.WriteLine "@AGENT_PROFILE_NAME@ = " + setUserName
  wTF.WriteLine "@AGENT_ENCRYPTED_PASSWORD@ = " + encryptedPasswd
  wTF.WriteLine "@AGENT_ENCRYPT_KEY@ = " + setPasswdKey


  wTF.WriteLine "AGENT_URL_PREFIX = " + setProtocol + "://" + setHostName + ":" + setPortNumber + setAgentDeploymentURI

  wTF.WriteLine "@DEBUG_LOGS_DIR@ = " + setInstallDir + "/Identifier_" + setIdentifier + "/logs/debug"
  wTF.WriteLine "TEMP_DIR_PREFIXDEBUG_DIR_PREFIX = " + setInstallDir 

  wTF.WriteLine "@AUDIT_LOGS_DIR@ = " + setInstallDir + "/Identifier_" + setIdentifier + "/logs/audit"

  expHostName = Replace(setHostName,".","_")
  wTF.WriteLine "@AUDIT_LOG_FILENAME@ = " + "amAgent_"+ expHostName + ".log"

  wTF.WriteLine "SERVER_DIR =  "  + setInstallDir + "/iis6/cert"

  wTF.WriteLine "@NOTIFICATION_ENABLE@ = true" 
  wTF.WriteLine "@LOG_ROTATION@ = true" 
  wTF.WriteLine "AGENT_CERT_PREFIX =  " 

  WScript.Echo "-----------------------------------------------------"
  WScript.Echo dict("110") + " " + configFile
  WScript.Echo "-----------------------------------------------------"
  wTF.Close

  Set oFSO = nothing

End Function

' Return a random string 
Function GetRandomString(len) 
  dim i, s, a, rndm, rndm1, tmp, tmp1
  a=Array("a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","0","1","2","3","4","5","6","7","8","9")

  const startChr ="a", range = 36 
  Randomize 
  s = "" 
  for i = 0 to len-1   
     rndm1 = Rnd()
     rndm = rndm1 * range
     rndm = rndm Mod 35
     s = s + a(rndm)
  next 
  GetRandomString = s 
end function  
' 

Function IsValidUrl(url, isAgent)

  Dim urlArray,i,protocol,hostname,portnum,deployuri,currStr
  IsValidUrl = true

  urlArray=split(url,":", -1, 1)
  bound=UBound(urlArray)
  Erase urlArray
  if(bound <> 2) then
    IsValidUrl = false
    Exit Function
  end if 

  urlArray=split(url,"://", -1, 1)
  bound=UBound(urlArray)
  if (bound <> 1) then
    IsValidUrl = false
    Exit Function
  else
    protocol = urlArray(0)
    currStr = urlArray(1)
    if ((Len(protocol)=0) or (Len(currStr)=0)) then
      IsValidUrl = false
      Exit Function
    end if 
    if ((protocol <> "http") and (protocol <> "https")) then
      IsValidUrl = false
      Exit Function
    end if
  end if
  Erase urlArray
  
  urlArray=split(currStr,":", -1, 1)
  bound=UBound(urlArray)
  if (bound <> 1) then
    IsValidUrl = false
    Exit Function
  else
    hostname = urlArray(0)
    currStr = urlArray(1)
    if ((Len(hostname)=0) or (Len(currStr)=0)) then
      IsValidUrl = false
      Exit Function
    end if 
  end if
  Erase urlArray

  if(isAgent = true)then
    portnum = currStr
    if (IsNumeric(portnum) = false) then
      IsValidUrl = false
      Exit Function
    end if
    setProtocol=protocol
    setHostName=hostname
    setPortNumber=portnum
  else
    urlArray=split(currStr,"/", -1, 1)
    bound=UBound(urlArray)
    if (bound <> 1) then
      IsValidUrl = false
      Exit Function
    else
      portnum = urlArray(0)
      deployuri = urlArray(1)
      if ((Len(portnum)=0) or (Len(deployuri)=0)) then
        IsValidUrl = false
        Exit Function
      end if 
    end if
    Erase urlArray
    setPrimaryServerProtocol=protocol
    setPrimaryServerHost=hostname
    setPrimaryServerPort=portnum
    setPrimaryServerDeploymentURI="/"+deployuri
  end if

End Function


