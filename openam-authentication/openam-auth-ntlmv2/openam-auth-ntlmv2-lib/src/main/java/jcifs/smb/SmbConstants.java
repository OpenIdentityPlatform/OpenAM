package jcifs.smb;

import jcifs.Config;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.TimeZone;

interface SmbConstants {

    static final int DEFAULT_PORT = 445;

    static final int DEFAULT_MAX_MPX_COUNT = 10;
    static final int DEFAULT_RESPONSE_TIMEOUT = 30000;
    static final int DEFAULT_SO_TIMEOUT = 35000;
    static final int DEFAULT_RCV_BUF_SIZE = 60416;
    static final int DEFAULT_SND_BUF_SIZE = 16644;
    static final int DEFAULT_SSN_LIMIT = 250;
    static final int DEFAULT_CONN_TIMEOUT = 35000;

    static final InetAddress LADDR = Config.getLocalHost();
    static final int LPORT = Config.getInt( "jcifs.smb.client.lport", 0 );
    static final int MAX_MPX_COUNT = Config.getInt( "jcifs.smb.client.maxMpxCount", DEFAULT_MAX_MPX_COUNT );
    static final int SND_BUF_SIZE = Config.getInt( "jcifs.smb.client.snd_buf_size", DEFAULT_SND_BUF_SIZE );
    static final int RCV_BUF_SIZE = Config.getInt( "jcifs.smb.client.rcv_buf_size", DEFAULT_RCV_BUF_SIZE );
    static final boolean USE_UNICODE = Config.getBoolean( "jcifs.smb.client.useUnicode", true );
    static final boolean FORCE_UNICODE = Config.getBoolean( "jcifs.smb.client.useUnicode", false );
    static final boolean USE_NTSTATUS = Config.getBoolean( "jcifs.smb.client.useNtStatus", true );
    static final boolean SIGNPREF = Config.getBoolean("jcifs.smb.client.signingPreferred", false );
    static final boolean USE_NTSMBS = Config.getBoolean( "jcifs.smb.client.useNTSmbs", true );
    static final boolean USE_EXTSEC = Config.getBoolean( "jcifs.smb.client.useExtendedSecurity", true );

    static final String NETBIOS_HOSTNAME = Config.getProperty( "jcifs.netbios.hostname", null );
    static final int LM_COMPATIBILITY = Config.getInt( "jcifs.smb.lmCompatibility", 3);

    static final int FLAGS_NONE                           = 0x00;
    static final int FLAGS_LOCK_AND_READ_WRITE_AND_UNLOCK = 0x01;
    static final int FLAGS_RECEIVE_BUFFER_POSTED          = 0x02;
    static final int FLAGS_PATH_NAMES_CASELESS            = 0x08;
    static final int FLAGS_PATH_NAMES_CANONICALIZED       = 0x10;
    static final int FLAGS_OPLOCK_REQUESTED_OR_GRANTED    = 0x20;
    static final int FLAGS_NOTIFY_OF_MODIFY_ACTION        = 0x40;
    static final int FLAGS_RESPONSE                       = 0x80;

    static final int FLAGS2_NONE                          = 0x0000;
    static final int FLAGS2_LONG_FILENAMES                = 0x0001;
    static final int FLAGS2_EXTENDED_ATTRIBUTES           = 0x0002;
    static final int FLAGS2_SECURITY_SIGNATURES           = 0x0004;
    static final int FLAGS2_EXTENDED_SECURITY_NEGOTIATION = 0x0800;
    static final int FLAGS2_RESOLVE_PATHS_IN_DFS          = 0x1000;
    static final int FLAGS2_PERMIT_READ_IF_EXECUTE_PERM   = 0x2000;
    static final int FLAGS2_STATUS32                      = 0x4000;
    static final int FLAGS2_UNICODE                       = 0x8000;

    static final int CAP_NONE             = 0x0000;
    static final int CAP_RAW_MODE         = 0x0001;
    static final int CAP_MPX_MODE         = 0x0002;
    static final int CAP_UNICODE          = 0x0004;
    static final int CAP_LARGE_FILES      = 0x0008;
    static final int CAP_NT_SMBS          = 0x0010;
    static final int CAP_RPC_REMOTE_APIS  = 0x0020;
    static final int CAP_STATUS32         = 0x0040;
    static final int CAP_LEVEL_II_OPLOCKS = 0x0080;
    static final int CAP_LOCK_AND_READ    = 0x0100;
    static final int CAP_NT_FIND          = 0x0200;
    static final int CAP_DFS              = 0x1000;
    static final int CAP_EXTENDED_SECURITY = 0x80000000;

    // file attribute encoding
    static final int ATTR_READONLY   = 0x01;
    static final int ATTR_HIDDEN     = 0x02;
    static final int ATTR_SYSTEM     = 0x04;
    static final int ATTR_VOLUME     = 0x08;
    static final int ATTR_DIRECTORY  = 0x10;
    static final int ATTR_ARCHIVE    = 0x20;

    // extended file attribute encoding(others same as above)
    static final int ATTR_COMPRESSED = 0x800;
    static final int ATTR_NORMAL     = 0x080;
    static final int ATTR_TEMPORARY  = 0x100;

    // access mask encoding
    static final int FILE_READ_DATA        = 0x00000001; // 1
    static final int FILE_WRITE_DATA       = 0x00000002; // 2
    static final int FILE_APPEND_DATA      = 0x00000004; // 3
    static final int FILE_READ_EA          = 0x00000008; // 4
    static final int FILE_WRITE_EA         = 0x00000010; // 5
    static final int FILE_EXECUTE          = 0x00000020; // 6
    static final int FILE_DELETE           = 0x00000040; // 7
    static final int FILE_READ_ATTRIBUTES  = 0x00000080; // 8
    static final int FILE_WRITE_ATTRIBUTES = 0x00000100; // 9
    static final int DELETE                = 0x00010000; // 16
    static final int READ_CONTROL          = 0x00020000; // 17
    static final int WRITE_DAC             = 0x00040000; // 18
    static final int WRITE_OWNER           = 0x00080000; // 19
    static final int SYNCHRONIZE           = 0x00100000; // 20
    static final int GENERIC_ALL           = 0x10000000; // 28
    static final int GENERIC_EXECUTE       = 0x20000000; // 29
    static final int GENERIC_WRITE         = 0x40000000; // 30
    static final int GENERIC_READ          = 0x80000000; // 31


    // flags for move and copy
    static final int FLAGS_TARGET_MUST_BE_FILE         = 0x0001; 
    static final int FLAGS_TARGET_MUST_BE_DIRECTORY    = 0x0002; 
    static final int FLAGS_COPY_TARGET_MODE_ASCII      = 0x0004; 
    static final int FLAGS_COPY_SOURCE_MODE_ASCII      = 0x0008;
    static final int FLAGS_VERIFY_ALL_WRITES           = 0x0010; 
    static final int FLAGS_TREE_COPY                   = 0x0020; 

    // open function
    static final int OPEN_FUNCTION_FAIL_IF_EXISTS      = 0x0000;
    static final int OPEN_FUNCTION_OVERWRITE_IF_EXISTS = 0x0020;

    static final int PID = (int)( Math.random() * 65536d );

    static final int SECURITY_SHARE = 0x00;
    static final int SECURITY_USER  = 0x01;

    static final int CMD_OFFSET        = 4;
    static final int ERROR_CODE_OFFSET = 5;
    static final int FLAGS_OFFSET      = 9;
    static final int SIGNATURE_OFFSET  = 14;
    static final int TID_OFFSET        = 24;
    static final int HEADER_LENGTH     = 32;

    static final long MILLISECONDS_BETWEEN_1970_AND_1601 = 11644473600000L;
    static final TimeZone TZ = TimeZone.getDefault();

    static final boolean USE_BATCHING = Config.getBoolean( "jcifs.smb.client.useBatching", true );
    static final String OEM_ENCODING = Config.getProperty( "jcifs.encoding", Config.DEFAULT_OEM_ENCODING );
    static final String UNI_ENCODING = "UTF-16LE";
    static final int DEFAULT_FLAGS2 =
                FLAGS2_LONG_FILENAMES |
                FLAGS2_EXTENDED_ATTRIBUTES |
                ( USE_EXTSEC ? FLAGS2_EXTENDED_SECURITY_NEGOTIATION : 0 ) |
                ( SIGNPREF ? FLAGS2_SECURITY_SIGNATURES : 0 ) |
                ( USE_NTSTATUS ? FLAGS2_STATUS32 : 0 ) |
                ( USE_UNICODE ? FLAGS2_UNICODE : 0 );
    static final int DEFAULT_CAPABILITIES =
                ( USE_NTSMBS ? CAP_NT_SMBS : 0 ) |
                ( USE_NTSTATUS ? CAP_STATUS32 : 0 ) |
                ( USE_UNICODE ? CAP_UNICODE : 0 ) |
                CAP_DFS;
    static final int FLAGS2 = Config.getInt( "jcifs.smb.client.flags2", DEFAULT_FLAGS2 );
    static final int CAPABILITIES = Config.getInt( "jcifs.smb.client.capabilities", DEFAULT_CAPABILITIES );
    static final boolean TCP_NODELAY = Config.getBoolean( "jcifs.smb.client.tcpNoDelay", false );
    static final int RESPONSE_TIMEOUT =
                Config.getInt( "jcifs.smb.client.responseTimeout", DEFAULT_RESPONSE_TIMEOUT );

    static final LinkedList CONNECTIONS = new LinkedList();

    static final int SSN_LIMIT =
            Config.getInt( "jcifs.smb.client.ssnLimit", DEFAULT_SSN_LIMIT );
    static final int SO_TIMEOUT =
            Config.getInt( "jcifs.smb.client.soTimeout", DEFAULT_SO_TIMEOUT );
    static final int CONN_TIMEOUT =
            Config.getInt( "jcifs.smb.client.connTimeout", DEFAULT_CONN_TIMEOUT );
    static final String NATIVE_OS =
            Config.getProperty( "jcifs.smb.client.nativeOs", System.getProperty( "os.name" ));
    static final String NATIVE_LANMAN =
            Config.getProperty( "jcifs.smb.client.nativeLanMan", "jCIFS" );
    static final int VC_NUMBER = 1;
    static final SmbTransport NULL_TRANSPORT = new SmbTransport( null, 0, null, 0 );
}
