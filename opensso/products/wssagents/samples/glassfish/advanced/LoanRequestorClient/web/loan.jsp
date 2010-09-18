<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
  
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
  
   $Id: loan.jsp,v 1.2 2008/08/15 01:05:42 veiming Exp $
--%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Loan Requestor Client</title>       
<style type="text/css">
.label { width: 30em;
text-align: left
}
</style> 
</head>
   <body>
    <h1>Loan Application</h1>
   
    <form name="LoanApplication" action="LoanApplication" method="GET">
        <div class="label">Applicant Name: <input type="text" name="applicantname" size="30" /></div>
        <br/>
        <div class="label"> Social Security Number: <input type="text" name="ssn" size="30" /></div>
        <br/>
        <div class="label">Gender: <input type="text" name="gender" size="8" /></div>
        <br/>
        <div class="label">Age: <input type="text" name="age" size="4" /></div>
        <br/>
        <div class="label">Address: <input type="text" name="address" size="40" /></div>
        <br/>
        <div class="label">Email: <input type="text" name="email" size="30" /></div>
        <br/>
        <div class="label">Annual Salary: <input type="text" name="salary" size="20" /></div>
        <br/>
        <div class="label">Loan requested: <input type="text" name="loanamount" size="20" /></div>
        <br/>
        <div class="label"><input type="submit" value="Apply Loan" name="loan" /></div>
        <br/>
        <br/>
    </form>


    </body>
</html>
