/* jcifs smb client library in Java
 * Copyright (C) 2007  "Michael B. Allen" <jcifs at samba dot org>
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

public class SmbShareInfo implements FileEntry {

    protected String netName;
    protected int type;
    protected String remark;

    public SmbShareInfo() {
    }
    public SmbShareInfo(String netName, int type, String remark)
    {
        this.netName = netName;
        this.type = type;
        this.remark = remark;
    }
    public String getName() {
        return netName;
    }
    public int getType() {
        /* 0x80000000 means hidden but SmbFile.isHidden() checks for $ at end
         */
        switch (type & 0xFFFF) {
            case 1:
                return SmbFile.TYPE_PRINTER;
            case 3:
                return SmbFile.TYPE_NAMED_PIPE;
        }
        return SmbFile.TYPE_SHARE;
    }
    public int getAttributes() {
        return SmbFile.ATTR_READONLY | SmbFile.ATTR_DIRECTORY;
    }
    public long createTime() {
        return 0L;
    }
    public long lastModified() {
        return 0L;
    }
    public long length() {
        return 0L;
    }

    public boolean equals(Object obj) {
        if (obj instanceof SmbShareInfo) {
            SmbShareInfo si = (SmbShareInfo)obj;
            return netName.equals(si.netName);
        }
        return false;
    }
    public int hashCode() {
        int hashCode = netName.hashCode();
        return hashCode;
    }

    public String toString() {
        return new String( "SmbShareInfo[" +
                "netName=" + netName +
                ",type=0x" + Hexdump.toHexString( type, 8 ) +
                ",remark=" + remark + "]" );
    }
}
