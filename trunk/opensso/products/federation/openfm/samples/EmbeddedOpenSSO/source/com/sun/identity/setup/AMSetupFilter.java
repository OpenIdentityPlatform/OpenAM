
package com.sun.identity.setup;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This filter brings administrator to a configuration page
 * where the product can be configured if the product is not
 * yet configured.
*/
public final class AMSetupFilter implements Filter {
    private boolean initialized;

    public void doFilter(
        ServletRequest request, 
        ServletResponse response, 
        FilterChain filterChain
    ) throws IOException, ServletException 
    {
        filterChain.doFilter(request, response);
    }

    /**
     * Destroy the filter config on sever shutdowm 
     */
    public void destroy() {
    }
    
    /**
     * Initializes the filter.
     *
     * @param filterConfig Filter Configuration.
     */
    public void init(FilterConfig filterConfig) {
        ServletContext cxt = filterConfig.getServletContext();
        Map<String, String> configData = new HashMap<String, String>();
        ResourceBundle res = ResourceBundle.getBundle("configparam");
        for (Enumeration e = res.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String val = res.getString(key);
            configData.put(key, val);
        }
        EmbeddedOpenSSO embOpenSSO = new EmbeddedOpenSSO(
            cxt, System.getProperty("user.home") + "/" + cxt.getContextPath(),
            configData);
        initialized = embOpenSSO.isConfigured();
        if (!initialized) {

            embOpenSSO.configure();
        }
        embOpenSSO.startup();
    }
   
}
