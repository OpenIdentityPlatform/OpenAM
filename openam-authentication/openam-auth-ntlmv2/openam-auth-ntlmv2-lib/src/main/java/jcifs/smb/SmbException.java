/* jcifs smb client library in Java
 * Copyright (C) 2000  "Michael B. Allen" <jcifs at samba dot org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.smb;

import jcifs.util.Hexdump;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * There are hundreds of error codes that may be returned by a CIFS
 * server. Rather than represent each with it's own <code>Exception</code>
 * class, this class represents all of them. For many of the popular
 * error codes, constants and text messages like "The device is not ready"
 * are provided.
 * <p>
 * The jCIFS client maps DOS error codes to NTSTATUS codes. This means that
 * the user may recieve a different error from a legacy server than that of
 * a newer varient such as Windows NT and above. If you should encounter
 * such a case, please report it to jcifs at samba dot org and we will
 * change the mapping.
 */

public class SmbException extends IOException implements NtStatus, DosError, WinError {

    static String getMessageByCode( int errcode ) {
        /* Note there's a signedness error here because 0xC0000000 based values are
         * negative so it with NT_STATUS_SUCCESS (0) the binary search will not be
         * performed properly. The effect is that the code at index 1 is never found
         * (NT_STATUS_UNSUCCESSFUL). So here we factor out NT_STATUS_SUCCESS
         * as a special case (which it is).
         */
        if (errcode == 0) {
            return "NT_STATUS_SUCCESS";
        }
        if(( errcode & 0xC0000000 ) == 0xC0000000 ) {
            int min = 1; /* Don't include NT_STATUS_SUCCESS */
            int max = NT_STATUS_CODES.length - 1;

            while( max >= min ) {
                int mid = (min + max) / 2;

                if( errcode > NT_STATUS_CODES[mid] ) {
                    min = mid + 1;
                } else if( errcode < NT_STATUS_CODES[mid] ) {
                    max = mid - 1;
                } else {
                    return NT_STATUS_MESSAGES[mid];
                }
            }
        } else {
            int min = 0;
            int max = DOS_ERROR_CODES.length - 1;

            while( max >= min ) {
                int mid = (min + max) / 2;

                if( errcode > DOS_ERROR_CODES[mid][0] ) {
                    min = mid + 1;
                } else if( errcode < DOS_ERROR_CODES[mid][0] ) {
                    max = mid - 1;
                } else {
                    return DOS_ERROR_MESSAGES[mid];
                }
            }
        }

        return "0x" + Hexdump.toHexString( errcode, 8 );
    }
    static int getStatusByCode( int errcode ) {
        if(( errcode & 0xC0000000 ) != 0 ) {
            return errcode;
        } else {
            int min = 0;
            int max = DOS_ERROR_CODES.length - 1;

            while( max >= min ) {
                int mid = (min + max) / 2;

                if( errcode > DOS_ERROR_CODES[mid][0] ) {
                    min = mid + 1;
                } else if( errcode < DOS_ERROR_CODES[mid][0] ) {
                    max = mid - 1;
                } else {
                    return DOS_ERROR_CODES[mid][1];
                }
            }
        }

        return NT_STATUS_UNSUCCESSFUL;
    }
    static String getMessageByWinerrCode( int errcode ) {
        int min = 0;
        int max = WINERR_CODES.length - 1;

        while( max >= min ) {
            int mid = (min + max) / 2;

            if( errcode > WINERR_CODES[mid] ) {
                min = mid + 1;
            } else if( errcode < WINERR_CODES[mid] ) {
                max = mid - 1;
            } else {
                return WINERR_MESSAGES[mid];
            }
        }

        return errcode + "";
    }


    private int status;
    private Throwable rootCause;

    SmbException() {
    }
    SmbException( int errcode, Throwable rootCause ) {
        super( getMessageByCode( errcode ));
        status = getStatusByCode( errcode );
        this.rootCause = rootCause;
    }
    SmbException( String msg ) {
        super( msg );
        status = NT_STATUS_UNSUCCESSFUL;
    }
    SmbException( String msg, Throwable rootCause ) {
        super( msg );
        this.rootCause = rootCause;
        status = NT_STATUS_UNSUCCESSFUL;
    }
    public SmbException( int errcode, boolean winerr ) {
        super( winerr ? getMessageByWinerrCode( errcode ) : getMessageByCode( errcode ));
        status = winerr ? errcode : getStatusByCode( errcode );
    }

    public int getNtStatus() {
        return status;
    }
    public Throwable getRootCause() {
        return rootCause;
    }
    public String toString() {
        if( rootCause != null ) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            rootCause.printStackTrace( pw );
            return super.toString() + "\n" + sw;
        } else {
            return super.toString();
        }
    }
}

