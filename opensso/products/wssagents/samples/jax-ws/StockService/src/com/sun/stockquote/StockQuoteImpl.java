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
 * $Id: StockQuoteImpl.java,v 1.2 2009/07/27 21:43:54 mrudul_uchil Exp $
 *
 */

package com.sun.stockquote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import javax.jws.WebService;
import javax.jws.HandlerChain;

@WebService(portName = "StockQuotePort", 
            serviceName="StockService",
            targetNamespace = "http://sun.com/stockquote.wsdl",
            wsdlLocation = "WEB-INF/wsdl/stockservice.wsdl",
            endpointInterface = "com.sun.stockquote.StockQuote")
@HandlerChain(file="server_handlers.xml")
public class StockQuoteImpl implements StockQuote{

	public QuoteResponseType getStockQuote(QuoteRequestType body) {
		 System.out.println("Before processing stock quote request");
   	     QuoteResponseType retVal =
   	                new QuoteResponseType();
   	        try {
   	            String symbol = body.getSymbol();
   	            System.out.println("StockQuoteImpl.getStockQuote Symbol: " +
                    symbol);
   	            Map data = getYahooQuote(symbol);
   	            if (data == null) {
   	                // Unable to obtain from Yahoo! Get from local cache
   	                data = getCachedQuote(symbol);
   	            }

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
            BufferedReader in = new BufferedReader
                    (new InputStreamReader(url.openStream()));
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
        stockValues.put("realValue", "9.50");
        stockValues.put("volume", "22451284");
        stockValues.put("open", "9.50");
        stockValues.put("change", "0.00");
        stockValues.put("dayHigh", "9.50");
        stockValues.put("dayLow","9.50");
        stockValues.put("yearRange", "2.60 - 10.94");
        stockValues.put("marketCap", "6.843B");
        stockValues.put("message", "Quote AUTO Generated");
        stockValues.put("time", getTime());
        stockData.put("JAVA", stockValues);

        stockValues = new HashMap();
        stockValues.put("realValue", "21.51");
        stockValues.put("company", "");
        stockValues.put("volume", "32308872");
        stockValues.put("open", "20.83");
        stockValues.put("change", "+0.88");
        stockValues.put("dayHigh", "20.77");
        stockValues.put("dayLow","21.53");
        stockValues.put("yearRange", "13.80 - 23.62");
        stockValues.put("marketCap", "107.7B");
        stockValues.put("message", "Quote AUTO Generated");
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

