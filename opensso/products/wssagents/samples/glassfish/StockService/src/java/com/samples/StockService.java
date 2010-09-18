/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StockService.java,v 1.6 2009/11/16 21:53:01 mallas Exp $
 *
 */
package com.samples;

import com.sun.stockquote.QuoteRequestType;
import com.sun.stockquote.QuoteResponseType;
import com.sun.stockquote.PriceType;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.security.auth.Subject;
import java.security.Principal;
import java.security.AccessController;
import javax.xml.ws.WebServiceContext;
import javax.annotation.Resource;
import javax.xml.ws.soap.Addressing;

@WebService(serviceName = "StockService", portName = "StockQuotePortTypePort", endpointInterface = "com.sun.stockquote.StockQuotePortType", targetNamespace = "http://sun.com/stockquote.wsdl", wsdlLocation = "WEB-INF/wsdl/StockService/stockservice.wsdl")
@HandlerChain( file="handlers.xml", name="" )
@Addressing(enabled=true, required=false)
public class StockService implements com.sun.stockquote.StockQuotePortType {

    @Resource
    protected WebServiceContext wscontext;
    
    /** Creates a new instance of StockService */
    public StockService() {
    }

    public QuoteResponseType getStockQuote(QuoteRequestType body) {
        com.sun.stockquote.QuoteResponseType retVal = 
               new com.sun.stockquote.QuoteResponseType();
        try {
            boolean userAuthenticated = false;
            String userName = null;
            String authMethod = null;
            Subject sub = (Subject)wscontext.getMessageContext().get(
                  "javax.security.auth.Subject");
            if(sub == null) {
               System.out.println(" Can not find the subject");
            } else {
               Iterator iter =  sub.getPrincipals().iterator();
               while(iter.hasNext()) {
                  Principal p = (Principal)iter.next();
                  userName = p.getName();
               }

               Iterator iter2 = sub.getPublicCredentials().iterator();
               while(iter2.hasNext()) {
                  Object obj = iter2.next();
                  if(obj instanceof Map) {
                     Map attrs = (Map)obj;
                     authMethod = (String)attrs.get("AuthMethod");
                  }
               }
            }

            if(userName != null) {
               System.out.println(" Authenticated user: " + userName);
               userAuthenticated = true;
            }

            String msg = "Principal: " + userName + " Authentication method: "+
                         authMethod;

            String symbol = body.getSymbol().trim();
            Map data = getYahooQuote(symbol);
            if (data == null) {
            // Unable to obtain from Yahoo! Get from local cache
             data = getCachedQuote(symbol);
            }
            data.put("message", msg);
            
            // Convert from Map to QuoteResponseType            
            retVal.setSymbol((String) data.get("symbol"));
            retVal.setCompany((String) data.get("company"));
            retVal.setMessage((String) data.get("message"));
            retVal.setTime((String) data.get("time"));
            retVal.setVolume((String)data.get("volume"));
            retVal.setDelay((String) data.get("delay"));
            retVal.setMarketCap((String) data.get("marketCap"));
            PriceType priceType = new PriceType();
            String last = null;
            last = (String)data.get("realValue");
            float lastft = Float.parseFloat(last);
            priceType.setLast(lastft);
            String open = (String)data.get("open");
            float openft = Float.parseFloat(open);
            priceType.setOpen(openft);
            String dayHigh = (String)data.get("dayHigh");
            float dayHighFt = Float.parseFloat(dayHigh);
            priceType.setDayHigh(dayHighFt);
            String dayLow = (String)data.get("dayLow");
            float dayLowFt = Float.parseFloat(dayLow);
            priceType.setDayLow(dayLowFt);
            priceType.setYearRange((String) data.get("yearRange"));
            retVal.setPrice(priceType);            
            String change = (String)data.get("change");
            retVal.setChange(change);
        } catch (Exception e) {
             e.printStackTrace();
            // Handle exception
        }
        return retVal;    
    }
   private Map getYahooQuote(String ticker) {
        URL url = null;
        try {
            // URL for the stock quote from Yahoo! service
            url = new URL("http://download.finance.yahoo.com/d/quotes.csv?s=" +
                ticker + "&d=t&f=sl1d1t1c1ohgvj1pp2wern");
            
            // Set the timeouts for connection and read
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            
            // Request for the stock quote
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String[] values = null;
            String str;
            if ((str = in.readLine()) != null) {
                values = str.split(",");
            }
            in.close();
            if ((values == null) || values.length < 16) {
                return (null);
            }
            // Populate stock values
            Map map = new HashMap();
            map.put("symbol", removeQuotes(values[0]));
            map.put("company", removeQuotes(values[15]));
            map.put("realValue", values[1]);
            map.put("time", removeQuotes(values[2]) + " " +
                removeQuotes(values[3]));
            map.put("volume", values[8]);
            map.put("open", values[5]);
            map.put("change", values[4]);
            map.put("dayHigh", values[6]);
            map.put("dayLow",values[7]);
            map.put("yearRange", removeQuotes(values[12]));
            map.put("marketCap", values[9]);
            map.put("message", "");
            return map;
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
   private Map getCachedQuote(String symbol) {
        if (stockData == null || stockData.isEmpty()) {
            init();
        }
        Map data = (Map) stockData.get(symbol);
        if (data == null) {
            data = (Map) stockData.get("ORCL");
        }
        data.put("symbol", symbol);
        data.put("company", symbol.toUpperCase());
        return (data);
    }

   /**
     * Assign static values for stock quotes.
     * Used as fall-back if quote cannot be
     * obtained from Yahoo!
     */
    private void init() {
        Map stockValues = new HashMap();
        stockValues.put("company", "");
        stockValues.put("realValue", "7.36");
        stockValues.put("volume", "31,793,369");
        stockValues.put("open", "7.37");
        stockValues.put("change", "-0.01");
        stockValues.put("dayHigh", "7.38");
        stockValues.put("dayLow","7.12");
        stockValues.put("yearRange", "N/A");
        stockValues.put("marketCap", "N/A");
        stockValues.put("message", "User not authenticated." +
                   " Quote AUTO Generated");
        stockValues.put("time", getTime());
        stockData.put("JAVA", stockValues);
        
        stockValues = new HashMap();
        stockValues.put("realValue", "16.35");
        stockValues.put("company", "");
        stockValues.put("volume", "38,544,715");
        stockValues.put("open", "16.35");
        stockValues.put("change", "-0.27");
        stockValues.put("dayHigh", "16.64");
        stockValues.put("dayLow","16.31");
        stockValues.put("yearRange", "N/A");
        stockValues.put("marketCap", "N/A");
        stockValues.put("message", "User not authenticated." +
                   " Quote AUTO Generated");
        stockValues.put("time", getTime());
        stockData.put("ORCL", stockValues);
    }
    
    private String getTime() {
        GregorianCalendar time = new GregorianCalendar();
        return (time.get(Calendar.MONTH) + "/" +
            time.get(Calendar.DAY_OF_MONTH) + "/" +
            time.get(Calendar.YEAR) + " " +
            time.get(Calendar.HOUR) + ":" +
            time.get(Calendar.MINUTE) +
            time.get(Calendar.AM_PM));
    }
    
    private String removeQuotes(String key) {
        return (key.replaceAll("\"", ""));
    }
    
    private static Map stockData = new HashMap();

    

}
