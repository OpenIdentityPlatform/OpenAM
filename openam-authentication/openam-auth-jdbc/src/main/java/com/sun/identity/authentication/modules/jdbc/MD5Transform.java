package com.sun.identity.authentication.modules.jdbc;

import java.security.MessageDigest;
import java.util.Formatter;

import com.sun.identity.authentication.spi.AuthLoginException;
   
/**
 * A very simple test implementation of the JDBC Password Syntax Transform MD5.
 */
public class MD5Transform implements JDBCPasswordSyntaxTransform  {
    /** 
     * Creates a new instance of <code>ClearTextTransform</code>. 
     */
    public MD5Transform() {
    }
    
    /** 
     * This simply returns the MD5 format of the password. 
     *
     * @param input Password before transform
     * @return MD5 Password after transform in this case the same thing.
     * @throws AuthLoginException
     */  
    public String transform(String input) throws AuthLoginException {
        if (input == null) {
            throw new AuthLoginException(
                "No input to the Clear Text Transform!");
        }
        final  Formatter fmt = new Formatter();
        try {
			for (byte b : MessageDigest.getInstance("MD5").digest(input.getBytes("UTF-8"))) {
			    fmt.format("%02x", b);
			}
			 return fmt.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally {
			if (fmt!=null) 
        		fmt.close();
		}
       
    }
}
