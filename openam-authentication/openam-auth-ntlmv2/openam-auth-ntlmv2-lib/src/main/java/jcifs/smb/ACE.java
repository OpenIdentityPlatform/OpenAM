package jcifs.smb;

import jcifs.util.Hexdump;

/**
 * An Access Control Entry (ACE) is an element in a security descriptor
 * such as those associated with files and directories. The Windows OS
 * determines which users have the necessary permissions to access objects
 * based on these entries.
 * <p>
 * To fully understand the information exposed by this class a description
 * of the access check algorithm used by Windows is required. The following
 * is a basic description of the algorithm. For a more complete description
 * we recommend reading the section on Access Control in Keith Brown's
 * "The .NET Developer's Guide to Windows Security" (which is also
 * available online).
 * <p>
 * Direct ACEs are evaluated first in order. The SID of the user performing
 * the operation and the desired access bits are compared to the SID
 * and access mask of each ACE. If the SID matches, the allow/deny flags
 * and access mask are considered. If the ACE is a "deny"
 * ACE and <i>any</i> of the desired access bits match bits in the access
 * mask of the ACE, the whole access check fails. If the ACE is an "allow"
 * ACE and <i>all</i> of the bits in the desired access bits match bits in
 * the access mask of the ACE, the access check is successful. Otherwise,
 * more ACEs are evaluated until all desired access bits (combined)
 * are "allowed". If all of the desired access bits are not "allowed"
 * the then same process is repeated for inherited ACEs.
 * <p>
 * For example, if user <tt>WNET\alice</tt> tries to open a file
 * with desired access bits <tt>0x00000003</tt> (<tt>FILE_READ_DATA |
 * FILE_WRITE_DATA</tt>) and the target file has the following security
 * descriptor ACEs:
 * <pre>
 * Allow WNET\alice     0x001200A9  Direct
 * Allow Administrators 0x001F01FF  Inherited
 * Allow SYSTEM         0x001F01FF  Inherited
 * </pre>
 * the access check would fail because the direct ACE has an access mask
 * of <tt>0x001200A9</tt> which doesn't have the
 * <tt>FILE_WRITE_DATA</tt> bit on (bit <tt>0x00000002</tt>). Actually, this isn't quite correct. If
 * <tt>WNET\alice</tt> is in the local <tt>Administrators</tt> group the access check
 * will succeed because the inherited ACE allows local <tt>Administrators</tt>
 * both <tt>FILE_READ_DATA</tt> and <tt>FILE_WRITE_DATA</tt> access.
 */

public class ACE {

    public static final int FILE_READ_DATA        = 0x00000001; // 1
    public static final int FILE_WRITE_DATA       = 0x00000002; // 2
    public static final int FILE_APPEND_DATA      = 0x00000004; // 3
    public static final int FILE_READ_EA          = 0x00000008; // 4
    public static final int FILE_WRITE_EA         = 0x00000010; // 5
    public static final int FILE_EXECUTE          = 0x00000020; // 6
    public static final int FILE_DELETE           = 0x00000040; // 7
    public static final int FILE_READ_ATTRIBUTES  = 0x00000080; // 8
    public static final int FILE_WRITE_ATTRIBUTES = 0x00000100; // 9
    public static final int DELETE                = 0x00010000; // 16
    public static final int READ_CONTROL          = 0x00020000; // 17
    public static final int WRITE_DAC             = 0x00040000; // 18
    public static final int WRITE_OWNER           = 0x00080000; // 19
    public static final int SYNCHRONIZE           = 0x00100000; // 20
    public static final int GENERIC_ALL           = 0x10000000; // 28
    public static final int GENERIC_EXECUTE       = 0x20000000; // 29
    public static final int GENERIC_WRITE         = 0x40000000; // 30
    public static final int GENERIC_READ          = 0x80000000; // 31

    public static final int FLAGS_OBJECT_INHERIT    = 0x01;
    public static final int FLAGS_CONTAINER_INHERIT = 0x02;
    public static final int FLAGS_NO_PROPAGATE      = 0x04;
    public static final int FLAGS_INHERIT_ONLY      = 0x08;
    public static final int FLAGS_INHERITED         = 0x10;

    boolean allow;
    int flags;
    int access;
    SID sid;

    /**
     * Returns true if this ACE is an allow ACE and false if it is a deny ACE.
     */
    public boolean isAllow() {
        return allow;
    }
    /**
     * Returns true if this ACE is an inherited ACE and false if it is a direct ACE.
     * <p>
     * Note: For reasons not fully understood, <tt>FLAGS_INHERITED</tt> may
     * not be set within all security descriptors even though the ACE was in
     * face inherited. If an inherited ACE is added to a parent the Windows
     * ACL editor will rebuild all children ACEs and set this flag accordingly.
     */
    public boolean isInherited() {
        return (flags & FLAGS_INHERITED) != 0;
    }
    /**
     * Returns the flags for this ACE. The </tt>isInherited()</tt>
     * method checks the <tt>FLAGS_INHERITED</tt> bit in these flags.
     */
    public int getFlags() {
        return flags;
    }
    /**
     * Returns the 'Apply To' text for inheritance of ACEs on
     * directories such as 'This folder, subfolder and files'. For
     * files the text is always 'This object only'.
     */
    public String getApplyToText() {
        switch (flags & (FLAGS_OBJECT_INHERIT | FLAGS_CONTAINER_INHERIT | FLAGS_INHERIT_ONLY)) {
            case 0x00:
                return "This folder only";
            case 0x03:
                return "This folder, subfolders and files";
            case 0x0B:
                return "Subfolders and files only";
            case 0x02:
                return "This folder and subfolders";
            case 0x0A:
                return "Subfolders only";
            case 0x01:
                return "This folder and files";
            case 0x09:
                return "Files only";
        }
        return "Invalid";
    }
    /**
     * Returns the access mask accociated with this ACE. Use the
     * constants for <tt>FILE_READ_DATA</tt>, <tt>FILE_WRITE_DATA</tt>,
     * <tt>READ_CONTROL</tt>, <tt>GENERIC_ALL</tt>, etc with bitwise
     * operators to determine which bits of the mask are on or off.
     */
    public int getAccessMask() {
        return access;
    }

    /**
     * Return the SID associated with this ACE.
     */
    public SID getSID() {
        return sid;
    }

    int decode( byte[] buf, int bi ) {
        allow = buf[bi++] == (byte)0x00;
        flags = buf[bi++] & 0xFF;
        int size = ServerMessageBlock.readInt2(buf, bi);
        bi += 2;
        access = ServerMessageBlock.readInt4(buf, bi);
        bi += 4;
        sid = new SID(buf, bi);
        return size;
    }

    void appendCol(StringBuffer sb, String str, int width) {
        sb.append(str);
        int count = width - str.length();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
    }
    /**
     * Return a string represeting this ACE.
     * <p>
     * Note: This function should probably be changed to return SDDL
     * fragments but currently it does not.
     */
    public String toString() {
        int count, i;
        String str;

        StringBuffer sb = new StringBuffer();
        sb.append( isAllow() ? "Allow " : "Deny  " );
        appendCol(sb, sid.toDisplayString(), 25);
        sb.append( " 0x" ).append( Hexdump.toHexString( access, 8 )).append(' ');
        sb.append(isInherited() ? "Inherited " : "Direct    ");
        appendCol(sb, getApplyToText(), 34);
        return sb.toString();
    }
}
