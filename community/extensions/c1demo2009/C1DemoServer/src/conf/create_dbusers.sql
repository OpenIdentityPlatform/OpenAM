#
#  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#  Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
#
#  The contents of this file are subject to the terms
#  of the Common Development and Distribution License
#  (the License). You may not use this file except in
#  compliance with the License.
#
#  You can obtain a copy of the License at
#  opensso/legal/CDDLv1.0.txt
#  See the License for the specific language governing
#  permission and limitations under the License.
#
#  When distributing Covered Code, include this CDDL
#  Header Notice in each file and include the License file
#  at opensso/legal/CDDLv1.0.txt.
#  If applicable, add the following below the CDDL Header,
#  with the fields enclosed by brackets [] replaced by
#  your own identifying information:
#  "Portions Copyrighted [year] [name of copyright owner]"
#
#  $Id: create_dbusers.sql,v 1.1 2009/06/18 00:56:21 superpat7 Exp $
#

#
# SQL Script file to set up users for demo
# Import using
# mysql -uroot -p<password> -Dj1demodb < create_dbusers.sql
#

GRANT ALL PRIVILEGES ON j1demodb.* TO 'j1demouser' IDENTIFIED BY 'j1demouser';
GRANT ALL PRIVILEGES ON j1demodb.* TO 'j1demouser'@'%' IDENTIFIED BY 'j1demouser';
GRANT ALL PRIVILEGES ON j1demodb.* TO 'j1demouser'@'localhost' IDENTIFIED BY 'j1demouser';