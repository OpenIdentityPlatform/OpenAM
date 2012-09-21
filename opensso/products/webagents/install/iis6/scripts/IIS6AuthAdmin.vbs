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
' $Id: IIS6AuthAdmin.vbs,v 1.1 2007/06/05 19:36:44 subbae Exp $
'
' Copyright 2007 Sun Microsystems Inc. All Rights Reserved
'
'---------------------------------------
' Installs the SJS Access Manager IIS Agent into IIS
'
' Requires:
'    -a Action - ADDFILTER, REMOVEFILTER
'    -p Path to Agent dll
'    -i Web-site identifier
'
' Usage: IIS6AuthAdmin.vbs -a <action> -i <website-identifier> -p <filter-path> 
'
' Example:
'	 cscript.exe IIS6AuthAdmin.vbs -a ADDFILTER -i 1 -p C:\web_agents\iis6_agent\bin\amiis6auth.dll
'    
'---------------------------------------

Dim FiltersObj
Dim LoadOrder
Dim Args, ArgNum
Dim FilterPath
Dim Action
Dim FilterName
Dim FilterDesc
Dim FiltSepr
Dim FiltFound
Dim FiltTemp
Dim Identifier

Set Args = WScript.Arguments
if Args.Count < 2 Then
    WScript.Echo "Incorrect number of arguments"
    WScript.Quit(1)
End IF

FilterName = "amiis6auth"
FilterDesc = "DSAME Agent"

ArgNum = 0
While ArgNum < Args.Count
	Select Case LCase(Args(ArgNum))
		Case "-a":
			ArgNum = ArgNum + 1
			Action = Args(ArgNum)
		Case "-p": 
			ArgNum = ArgNum + 1
			FilterPath = Args(ArgNum)
		Case "-i":
			ArgNum = ArgNum + 1
			Identifier = Args(ArgNum)
		Case Else:
			WScript.Echo "Unknown argument "& Args(ArgNum)
			WScript.Quit(1)
	End Select	
	ArgNum = ArgNum + 1
Wend


If Action <> "ADDFILTER" and Action <> "REMOVEFILTER" Then
    WScript.Echo "Unknown argument "& Args(0)
    WScript.Quit(1)
End If

If Action="" Then
    WScript.Echo "No action specified -  ADDFILTER, REMOVEFILTER"
    WScript.Quit(1)
End If

If Action="ADDFILTER" Then
		If (FilterPath="") Then
		    WScript.Echo "Missing Filter path"
		    WScript.Quit(1)
		End If
		If (Identifier="") Then
		    WScript.Echo "Missing Web site identifier"
		    WScript.Quit(1)
		End If
				
		Set FiltersObj = GetObject("IIS://localhost/W3SVC/" & Identifier & "/Filters")
		LoadOrder = FiltersObj.FilterLoadOrder
		
		'Check if the filter is already loaded
		FiltFound = Instr(LoadOrder, FilterName)
		If FiltFound > 0 Then
			WScript.Echo "Filter amiis6auth already loaded"
			WScript.Quit(1)
		End If	
		
		If LoadOrder <> "" Then
		  FiltSepr = ","
		End If
		LoadOrder = FilterName & FiltSepr & LoadOrder
		FiltersObj.FilterLoadOrder = LoadOrder
		FiltersObj.SetInfo
	
		Dim FilterObj
		
		Set FilterObj = FiltersObj.Create("IIsFilter", FilterName)
		FilterObj.FilterPath = FilterPath
		FilterObj.FilterDescription = FilterDesc
		FilterObj.SetInfo
		
End If

If Action="REMOVEFILTER" Then

		If (FilterName="") Then
		    WScript.Echo "Missing Filter Name"
		    WScript.Quit(1)
		End If
		
		Set FiltersObj = GetObject("IIS://localhost/W3SVC/" & Identifier & "/Filters")
		LoadOrder = FiltersObj.FilterLoadOrder
				
		'If the filter is not present, quit.
		If Instr(LoadOrder, FilterName) = 0 Then
			WScript.Echo "Filter amiis6auth not loaded."
			WScript.Quit(1)
		End If
		
		FiltersObj.Delete "IIsFilter", FilterName
		FiltersObj.SetInfo
		FiltTemp = FilterName
		If Instr(LoadOrder, FilterName & ",") > 0 Then
		  FiltTemp = FilterName & "," 
		End If
		
		LoadOrder = Replace(LoadOrder,FiltTemp,"")
		FiltersObj.FilterLoadOrder = LoadOrder
		FiltersObj.SetInfo
End If

WScript.Quit(0)
