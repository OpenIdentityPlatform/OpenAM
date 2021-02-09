package org.forgerock.openam.headers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(filterName = "DisableSameSiteCookiesFilter", urlPatterns = {"/*"}, asyncSupported = true)
public class DisableSameSiteCookiesFilter implements Filter
{
  private static final Logger logger = LoggerFactory.getLogger(DisableSameSiteCookiesFilter.class);

  private static final String headerName = "Set-Cookie";

  public void init(FilterConfig filterConfig) throws ServletException {
    logger.debug("Initialized SameSite filter.");
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      logger.trace("Filtering response to automatically convert cookies to SameSite=none");
      DisableSameSiteResponseWrapper disableSameSiteResponseWrapper = new DisableSameSiteResponseWrapper((HttpServletResponse)response);
      chain.doFilter(request, (ServletResponse)disableSameSiteResponseWrapper);
    }
    else {
      chain.doFilter(request, response);
    }
  }

  public void destroy() {
    logger.debug("Destroying filter");
  }

  @VisibleForTesting
  static class DisableSameSiteResponseWrapper extends HttpServletResponseWrapper {
    DisableSameSiteResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public void addCookie(Cookie cookie) {
      // For non-secure cookies, there's nothing we can do
      if (!cookie.getSecure()) {
        DisableSameSiteCookiesFilter.logger.debug("Cookie {} not marked as secure, skipping automatic SameSite=None", cookie.getName());
        super.addCookie(cookie);
        return;
      }

      // This is a secure cookie, we can set the SameSite policy to None
      DisableSameSiteCookiesFilter.logger.debug("Marking cookie {} as SameSite=none", cookie.getName());

      // Copy the cookie to the CHF object
      org.forgerock.http.protocol.Cookie chfCookie = new org.forgerock.http.protocol.Cookie();
      chfCookie.setName(cookie.getName());
      chfCookie.setValue(cookie.getValue());
      chfCookie.setComment(cookie.getComment());
      chfCookie.setDomain(cookie.getDomain());
      chfCookie.setPath(cookie.getPath());
      chfCookie.setMaxAge(Integer.valueOf(cookie.getMaxAge()));
      chfCookie.setHttpOnly(Boolean.valueOf(cookie.isHttpOnly()));
      chfCookie.setSecure(Boolean.valueOf(cookie.getSecure()));
      chfCookie.setVersion(Integer.valueOf(cookie.getVersion()));

      Date expires = null;
      if (cookie.getMaxAge() == 0) {
        expires = new Date(0L);
      } else if (cookie.getMaxAge() > 0) {
        expires = Time.newDate(Time.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cookie.getMaxAge()));
      }
      chfCookie.setExpires(expires);

      // Force SameSite = None
      chfCookie.setSameSite(org.forgerock.http.protocol.Cookie.SameSite.NONE);

      // Do not call super.addCookie, rather, add the equivalent generated Header
      SetCookieHeader header = new SetCookieHeader(Collections.singletonList(chfCookie));
      super.addHeader(header.getName(), header.getFirstValue());
    }

    @Override
    public void addHeader(String name, String value) {
      // Are we processing a Set-Cookie header?
      if (!headerName.equalsIgnoreCase(name)) {
        // No, we have nothing to do
        super.addHeader(name, value);
        return;
      }

      // Yes, we are processing a Set-Cookie header
      SetCookieHeader header = SetCookieHeader.valueOf(value);

      // For each cookie in the header
      for (org.forgerock.http.protocol.Cookie cookie : header.getCookies())
      {
        // Does it already have a SameSite policy?
        if (cookie.getSameSite() == null)
        {
          // No. Is it secure?
          if (cookie.isSecure().booleanValue()) {
            // Yes, we can set it as SameSite=None!
            DisableSameSiteCookiesFilter.logger.debug("Marking cookie {} as SameSite=None", cookie.getName());
            cookie.setSameSite(org.forgerock.http.protocol.Cookie.SameSite.NONE);
          } else {
            // No, there's nothing we can do: SameSite=None attribute is only for secure cookies
            DisableSameSiteCookiesFilter.logger.debug("Cookie {} is not secure, can't set SameSite policy", cookie.getName());
          }
        }
        else
        {
          // Yes, nothing to do
          DisableSameSiteCookiesFilter.logger.debug("Cookie {} already marked as SameSite, nothing to do", cookie.getName());
        }

        // Add the cookie header to the response
        super.addHeader(headerName, (new SetCookieHeader(Collections.singletonList(cookie))).getFirstValue());
      }
    }
  }
}
