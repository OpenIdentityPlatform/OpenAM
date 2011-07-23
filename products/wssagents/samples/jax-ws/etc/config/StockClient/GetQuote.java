/**
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
 * $Id: GetQuote.java,v 1.1 2009/10/05 06:09:16 mrudul_uchil Exp $
 *
 */
package com.sun.stockquote;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPFaultException;
import javax.jws.HandlerChain;

public class GetQuote extends HttpServlet {
    
    @WebServiceRef(wsdlLocation =
        "http://localhost:8080/StockService/StockService?wsdl")
    @HandlerChain(file="client_handlers.xml")
    private com.sun.stockquote.StockService service;
    
    /**
     * Get Stock quote from WSP
     */
    public QuoteResponseType getStockQuote(String symbol) throws
            SOAPFaultException {

        StockQuote port = service.getStockQuotePort();
        QuoteRequestType body = new QuoteRequestType();
        body.setSymbol(symbol);
        return(port.getStockQuote(body));
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request,
        HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String symbol = request.getParameter("symbol");
        if ((symbol == null) || (symbol.length() == 0)) {
            out.println("<h1>Invalid Stock Symbol</h1>");
            out.close();
            return;
        }
        try {
            // Get StockQuote
            QuoteResponseType result = getStockQuote(symbol);
            PriceType price = result.getPrice();
            
            // Display the page
            out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 " +
                    "Transitional//EN\"\n");
            out.write("\"http://www.w3.org/TR/html4/loose.dtd\">\n");
            out.write("<html>\n");
            out.write("<head>\n");
            out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; "
                    + "charset=UTF-8\" />\n");
            out.write("<title>Display Quote</title>\n");
            addJavaScript(out);
            out.write("</head>\n");
            out.write("<body>\n");
            out.write("<h1>Stock Quote Display</h1>\n");
            out.write("<hr/>\n");
            out.write("<table border=\"1\" width=\"400\" cellspacing=\"1\" " +
                    "cellpadding=\"1\" bgcolor=\"#BBDDFF\">\n");
            out.write("<thead>\n");
            out.write("<tr>\n<th>");
            out.print(result.getCompany());
            out.write("(");
            out.print( result.getSymbol());
            out.write(")</th>\n");
            out.write("<th>");
            out.print(result.getMessage());
            out.write("</th>\n");
            out.write("</tr>\n");
            out.write("</thead>\n");
            out.write("<tbody>\n");
            out.write("<tr>\n");
            out.write("<td width=\"60%\">Last Trade:</td>\n");
            out.write("<td width=\"40%\">");
            out.print( (price != null) ? price.getLast() : "N/A");
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td><font color=\"#CC0000\">DELAY:</font> </td>\n");
            out.write("<td><font color=\"#CC0000\">");
            out.print( result.getDelay() );
            out.write("</font></td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>Trade Time:</td>\n");
            out.write("<td>");
            out.print( result.getTime() );
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>Change:</td>\n");
            out.write("<td> ");
            out.print( result.getChange() );
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>Open:</td>\n");
            out.write("<td>");
            out.print((price != null)? price.getOpen() : "N/A");
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>Day's Range:</td>\n");
            out.write("<td>");
            out.print((price != null)? price.getDayLow() : "N/A");
            out.write(" - ");
            out.print((price != null)? price.getDayHigh() : "N/A");
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>52wk Range:</td>\n");
            out.write("<td>");
            out.print( (price != null)? price.getYearRange() : "N/A");
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>Volume:</td>\n");
            out.write("<td>");
            out.print( result.getVolume() );
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("<tr>\n");
            out.write("<td>Market Cap:</td>\n");
            out.write("<td>");
            out.print(result.getMarketCap());
            out.write("</td>\n");
            out.write("</tr>\n");
            out.write("</tbody>\n");
            out.write("</table>\n");
            out.write("<hr/>\n");
            
            // Image for checking request and response XML
            out.write("<h3>View SOAP Messages</h3>");
            out.write("<img name=\"Communications\" src=\"communication.gif\" " 
                    + "width=\"421\" height=\"203\" border=\"0\" "
                    + "usemap=\"#m_arviimg\"><map name=\"m_arviimg\">\n");
            out.write("<area shape=\"rect\" coords=\"180,120,245,144\" " +
                "href=\"javascript:DoRemote('");
            out.write("SOAPMessage?dir=response");
            out.write("',843,897)\">");            
            out.write("<area shape=\"rect\" coords=\"179,74,245,100\" " +
                "href=\"javascript:DoRemote('");
            out.write("SOAPMessage?dir=request");
            out.write("',843,897)\">");
            out.write("</map>\n ");
            
            // Link to try again
            out.write("<hr/>\n");
            out.write("<a href=\"index.jsp\">Try again</a>");
            // Image for checking request and response XML

            out.write("</body>\n");
            out.write("</html>\n");
        } catch (SOAPFaultException sfe) {
           out.write("SOAP Fault exception occured: Fault string" 
               + sfe.getFault().getFaultString()); 
        } catch (Exception ex) {
            ex.printStackTrace(out);
        }
        out.close();
    }
    
    private void addJavaScript(PrintWriter out) {
        out.write("<SCRIPT LANGUAGE=\"JavaScript\">\n");
        out.write("function DoRemote(url,w,h) {\n");
        out.write("remote= window.open(\"\",\"remotewin\"," +
            "'toolbar=0,location=0,directories=0,status=0,menubar=0," +
            "scrollbars=1,resizable=1,alwaysRaised=1,width='+w+',height='+h);");
        out.write("\nremote.resizeTo(w,h);\n");
        out.write("remote.location.href = url;\n");
        out.write("if (remote.opener == null) remote.opener = window;\n");
        out.write("remote.opener.name = \"opener\";");
        out.write("remote.focus();");
        out.write("}\n</SCRIPT>\n");
    }
    
    
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    
}
