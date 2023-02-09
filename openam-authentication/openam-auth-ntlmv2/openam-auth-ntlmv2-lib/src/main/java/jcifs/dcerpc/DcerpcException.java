/* jcifs msrpc client library in Java
 * Copyright (C) 2006  "Michael B. Allen" <jcifs at samba dot org>
 *                     "Eric Glass" <jcifs at samba dot org>
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

package jcifs.dcerpc;

import jcifs.smb.WinError;
import jcifs.util.Hexdump;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DcerpcException extends IOException implements DcerpcError, WinError {

    static String getMessageByDcerpcError(int errcode) {
        int min = 0;
        int max = DCERPC_FAULT_CODES.length;

        while (max >= min) {
            int mid = (min + max) / 2;

            if (errcode > DCERPC_FAULT_CODES[mid]) {
                min = mid + 1;
            } else if (errcode < DCERPC_FAULT_CODES[mid]) {
                max = mid - 1;
            } else {
                return DCERPC_FAULT_MESSAGES[mid];
            }
        }

        return "0x" + Hexdump.toHexString(errcode, 8);
    }

    private int error;
    private Throwable rootCause;

    DcerpcException(int error) {
        super(getMessageByDcerpcError(error));
        this.error = error;
    }
    public DcerpcException(String msg) {
        super(msg);
    }
    public DcerpcException(String msg, Throwable rootCause) {
        super(msg);
        this.rootCause = rootCause;
    }
    public int getErrorCode() {
        return error;
    }
    public Throwable getRootCause() {
        return rootCause;
    }
    public String toString() {
        if (rootCause != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            rootCause.printStackTrace(pw);
            return super.toString() + "\n" + sw;
        }
        return super.toString();
    }
}
