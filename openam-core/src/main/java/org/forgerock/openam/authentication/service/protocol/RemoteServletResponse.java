/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.authentication.service.protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;

/**
 * Encapsulates the transferable state of a ServletResponse object.
 */
public class RemoteServletResponse implements ServletResponse, Serializable {
    public static final long serialVersionUID = 42L;

    /* The response being wrapped.
     */
    private transient ServletResponse response;

    /* The transferable state.
     */
    private String characterEncoding = null;
    private String charSet = null;
    private String contentType = null;
    private Locale locale = null;

    /**
     * Creates a ServletResponse adaptor wrapping the given response object.
     *
     * @param response The response to wrap.
     * @throws java.lang.IllegalArgumentException if the response is null.
     */
    public RemoteServletResponse(ServletResponse response) {

        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        
        this.response = response;
        processResponse();
    }
    
    protected void processResponse() {
        characterEncoding = getCharacterEncoding();
        contentType = getContentType();
    }

    /**
     * Used by deserialization.
     */
    public RemoteServletResponse() {

    }

    /**
     * Return the wrapped ServletResponse object.
     * 
     * @return The encapsulated response object.
     */
    public ServletResponse getResponse() {
        return this.response;
    }	

    /**
     * Sets the response being wrapped. 
     * 
     * @throws java.lang.IllegalArgumentException if the response is null.
     */
    public void setResponse(ServletResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        
        this.response = response;
        processResponse();
    }

    /**
     * The default behavior of this method is to return getCharacterEncoding()
     * on the wrapped response object. Serialized.
     * 
     * @return The character encoding of the response.
     */
    public String getCharacterEncoding() {
        return response != null ? this.response.getCharacterEncoding() : characterEncoding;
    }

    /**
     * Returns the MIME type for the content response
     *
     * @return MIME type of the content response
     * @since 2.4
     */
    public String getContentType() {
        return response != null ? this.response.getContentType() : contentType;
    }

    /**
     * The default behavior of this method is to return getOutputStream()
     * on the wrapped response object. Not serialized.
     *
     * @return The output stream associated with the response, null if unavailable.
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return response != null ? this.response.getOutputStream() : null;
    }

    /**
     * The default behavior of this method is to return getWriter()
     * on the wrapped response object. Not serialized.
     *
     * @return The writer associated with the response, null if unavailable.
     */
    public PrintWriter getWriter() throws IOException {
        return response != null ? this.response.getWriter() : null;
    }

    /**
     * The default behavior of this method is to call setContentLength(int len)
     * on the wrapped response object. Not serialized.
     *
     * @param len The new content length of the response.
     */
    public void setContentLength(int len) {
        if (response != null) {
            this.response.setContentLength(len);
        }
    }

    public void setContentLengthLong(long len) {
        if (response != null) {
            this.response.setContentLengthLong(len);
        }
    }

    /**
     * The default behavior of this method is to call setContentType(String type)
     * on the wrapped response object. Not Serialized.
     *
     * @param type The new content type of the response.
     */
    public void setContentType(String type) {
        if (response != null) {
            this.response.setContentType(type);
        }
    }

    /**
     * The default behavior of this method is to call setBufferSize(int size)
     * on the wrapped response object. Not serailzed.
     *
     * @param size The new buffer size of the request.
     */
    public void setBufferSize(int size) {
        if (response != null) {
            this.response.setBufferSize(size);
        }
    }

    /**
     * The default behavior of this method is to return getBufferSize()
     * on the wrapped response object. Not serialized.
     *
     * @return The buffer size of the response, -1 if unavailable.
     */
    public int getBufferSize() {
        return response != null ? this.response.getBufferSize(): -1;
    }

    /**
     * The default behavior of this method is to call flushBuffer()
     * on the wrapped response object. Not serialized.
     */
    public void flushBuffer() throws IOException {
        if (response != null) {
            this.response.flushBuffer();
        }
    }

    /**
     * The default behavior of this method is to return isCommitted()
     * on the wrapped response object. Not serialized.
     *
     * @return True if the response has been committed, false otherwise.
     */
    public boolean isCommitted() {
        return response != null && this.response.isCommitted();
    }

    /**
     * The default behavior of this method is to call reset()
     * on the wrapped response object. Not serialized.
     */
    public void reset() {
        if (response != null) {
            this.response.reset();
        }
    }

    /**
     * The default behavior of this method is to call resetBuffer()
     * on the wrapped response object. Not serialized.
     */
    public void resetBuffer() {
        if (response != null) {
            this.response.resetBuffer();
        }
    }

    /**
     * The default behavior of this method is to call setLocale(Locale loc)
     * on the wrapped response object. Serialized.
     *
     * @param loc Sets the new locale of the response.
     */
    public void setLocale(Locale loc) {
        if (response != null) {
            this.response.setLocale(loc);
        } else {
            locale = loc;
        }
    }

    /**
     * Sets the character encoding of the response
     *
     * @param charSet Sets the new charSet of the response.
     * @since 2.4
     */
    public void setCharacterEncoding(String charSet) {
        if (response != null) {
            this.response.setCharacterEncoding(charSet);
        } else {
            this.charSet = charSet;
        }
    }

    /**
     * The default behavior of this method is to return getLocale()
     * on the wrapped response object. Serialized.
     *
     * @return The locale of the response
     */
    public Locale getLocale() {
	    return response != null ? this.response.getLocale() : locale;
    }
}
