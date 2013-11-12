<!--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: readme.txt,v 1.3 2008/06/25 05:52:15 qcheng Exp $

   Portions Copyrighted [2013] [ForgeRock AS]

-->

need to use JAVA_HOME JDK1.5 or above.


replace these properties in the ant file build.xml
    REPLACE_WEB_LOGIC_10_SERVER_LIB  set it to your installation of \bea\wlserver_10.0\server\lib
    and 
    REPLACE_JAVA_HOME_LIB  set to something like \bea\jdk150_06\lib

Run 'ant'. 
The default target is "build" which will generate an amauthprovider.jar

There is another target called "rebuild" which does the build target plus copies into the ..\etc directory. So you can choose to copy existing ..\etc amauthprovider.jar or generate a new one for each weblogic 10 agent build


**********************************************************************

OPTIONAL STUFF BELOW
If you want to generate the amauthprovider.jar at the command line, 
and not use ant, then follow the instructions below.

**********************************************************************


C:\workspace\test3-bea10\bea\wlserver_10.0\server\lib
C:\workspace\test3-bea10\bea\jdk150_06

0) download and install BEA 10 or have some of the libraries and artifacts available

1)change to this directory ...
  opensso\products\j2eeagents\weblogic\v10\mbeans


2) set to JDK1.5 or above, note there is jdk in bea 10 downloads so consider using
JAVA_HOME=C:\workspace\test3-bea10\bea\jdk150_06

3) make a dist directory inside current directory
for example
 opensso\products\j2eeagents\weblogic\v10\mbeans\dist

3) make a build\ directory inside current directory
for example
 opensso\products\j2eeagents\weblogic\v10\mbeans\build

4) copy these two files into the build directory
AgentAuthenticator.xml(found in current directory)  
commo.dtd (found in weblogic 10 server\lib directory such as  
                     C:\workspace\test3-bea10\bea\wlserver_10.0\server\lib) 

4) run this java command
java -classpath
C:\workspace\test3-bea10\bea\wlserver_10.0\server\lib\weblogic.jar;
C:\workspace\test3-bea10\bea\jdk150_06\lib\tools.jar;
.\build  
 weblogic.management.commo.WebLogicMBeanMaker -files build 
 -MDF build\AgentAuthenticator.xml -createStubs -verbose

5)  run this java command
java -classpath
C:\workspace\test3-bea10\bea\wlserver_10.0\server\lib\weblogic.jar;
C:\workspace\test3-bea10\bea\jdk150_06\lib\tools.jar;
.\build 
 weblogic.management.commo.WebLogicMBeanMaker -files build 
 -MJF dist\amauthprovider.jar -createStubs -verbose



