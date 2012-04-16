using System;
using System.Text;
using System.Security.Cryptography;
using System.Collections;
using System.Collections.Generic;

namespace Sun.Identity
{

public class SecureAttrs
{

    public static int dbg = 0;

    // Configuration parameters 
    // Crypto type
    public const string SAE_CONFIG_CRYPTO_TYPE = "saecryptotype";
    public const string SAE_CRYPTO_ASYMMETRIC = "asymmetric";
    public const string SAE_CRYPTO_SYMMETRIC = "symmetric";

    // provider to be used for Asymmetric signing
    public const string SAE_CONFIG_ASYM_SIGN_PROVIDER = "saesigningprovider";
    // provider to be used for Asymmetric signature verification
    public const string SAE_CONFIG_ASYM_VERIFY_PROVIDER = "saeverifyingprovider";

    // Signature validity : accepted duration since timestamp.
    public const string SAE_CONFIG_SIG_VALIDITY_DURATION = 
                                              "saesigvalidityduration";

    private const int SYMMETRIC  = 0;
    private const int ASYMMETRIC = 1;

    private static int cryptoType       = SYMMETRIC;
    private static AsymmetricAlgorithm  asymSignProvider = null;
    private static AsymmetricAlgorithm  asymVerifyProvider = null;
    private static long tsDuration = 120000; // 2 minutes.

    ///<summary>
    ///    Init params for SecureAttrs class. Please see SAE_* params above.
    ///</summary>
    ///<param name="props">Init params.</param>
    public static void init(Hashtable props)
    {
        string type = (string) props[SAE_CONFIG_CRYPTO_TYPE]; 
        if (type != null) {
            if (SAE_CRYPTO_ASYMMETRIC.Equals(type))
                cryptoType = ASYMMETRIC; 
            if (SAE_CRYPTO_SYMMETRIC.Equals(type))
                cryptoType = SYMMETRIC; 
        }
        object provider = props[SAE_CONFIG_ASYM_SIGN_PROVIDER]; 
        if (provider != null)
            asymSignProvider = (AsymmetricAlgorithm) provider;
        provider = props[SAE_CONFIG_ASYM_VERIFY_PROVIDER]; 
        if (provider != null)
            asymVerifyProvider = (AsymmetricAlgorithm) provider;
 
        string duration = (string) props[SAE_CONFIG_SIG_VALIDITY_DURATION];
        if (duration != null)
            tsDuration = long.Parse(duration);
    }

    ///<summary>
    ///    Returns a Base64 encoded string comprising a signed set of 
    ///    attributes.
    ///</summary>
    ///<param name="str">Base64 encoded string containing secure attrs.</param>
    ///<param name="secret">Symmetric : Shared secret. Asymmetric : ignored</param>
    ///<return>Hashtable of attributes parsed if authentic, else null </return>
    public static string getEncodedString(Hashtable attrs, string secret)
    {
        System.Text.ASCIIEncoding  encoding=new System.Text.ASCIIEncoding();
        return Convert.ToBase64String(
               encoding.GetBytes(getSignature(attrs, secret)));

    }

    ///<summary>
    ///    Verifies a Base64 encoded string for authenticity 
    ///    based on the shared secret supplied.</summary>
    ///<param name="str">Base64 encoded string containing secure attrs.</param>
    ///<param name="secret">Shared secret.</param>
    ///<return>Hashtable of attributes parsed if authentic, else null </return>
    static public Hashtable verifyEncodedString(string str, string secret)
    {
        // Base64 decode...
        byte[] decValueBytes = Convert.FromBase64String(str);
        //string decValue = Convert.ToString(decValueBytes);
        string decValue = Encoding.ASCII.GetString(decValueBytes);
        if (dbg == 1)
           Console.WriteLine("verifyEncoded:1st b64 dec: {0}", decValue);
        return verifySignedString(decValue, secret);
    }

    ///<summary>Turns debug trace on/off. </summary>
    ///<param name="flag"0 turns off, non-0 turns debug on.</param>
    ///<return>Debug state</return>
    static public int setDebug(int flag)
    {
        dbg = flag;
        return dbg;
    }
    static public Hashtable verifySignedString(string decValue, string secret)
    {
        string sig = null;
        Hashtable attrs = new Hashtable();
        // Tokenize : attr=value pairs delimited by "|" 
        // Put "Signature" in sig string;
        char[] delim = {'|'};

        string[] attrlist = decValue.Split(delim);

        if (attrlist == null || attrlist.GetLength(0) == 0)
            return null;

        foreach (string ava in attrlist)
        { 
            int idx = ava.IndexOf("=");
            if (dbg == 1)
                Console.WriteLine("verifyEncoded:idx={0}", idx);
            string key = ava.Substring(0, idx);
            string val = ava.Substring(idx+1, ava.Length-idx-1);
            if (dbg == 1)
                Console.WriteLine("verifyEncoded:key={0} val={1}", key, val);
           
            if (key.Equals("Signature")) {
                 sig = val;
            }
            else {
                 attrs.Add(key, val);
            }
        }
        if (dbg == 1)
           Console.WriteLine("verifyEncoded:Signature={0}", sig);
        if ( verifyAttrs(attrs, sig, secret))
            return attrs;
        else 
            return null;
    }


    public static string getSignature(Hashtable attrs, string secret)
    {
        string normalizedStr = normalize(attrs);
        string hash = ComputeHash(normalizedStr, secret);
        if (dbg == 1)
           Console.WriteLine("getSignature: normalized={0} hash={1}", 
                              normalizedStr, hash);
        StringBuilder sb = new StringBuilder();
        sb.Append(normalizedStr).Append("Signature="+hash);
        return (sb.ToString());
    }
    public static bool verifyAttrs(Hashtable attrs, string token, string secret)
    {
        // Retrive the timestamp
        
        int idx = token.IndexOf("TS", 2);
        string ts = token.Substring(2, idx-2);
        long signts = long.Parse(ts);
        TimeSpan t = (DateTime.UtcNow - new DateTime(1970, 1, 1));
        long nowts = (long) t.TotalMilliseconds;
        if ((nowts - signts) > tsDuration) {
            return false;
        }

        string signature = token.Substring(idx+2, token.Length - idx -2);
        if (dbg == 1)
           Console.WriteLine("verifyAttrs: ts={0} signature={1}", ts, signature);
        if (cryptoType == ASYMMETRIC) {
            return VerifyAsym(normalize(attrs)+ts, signature, ts); 
        } else {
            string seed = String.Concat(secret, ts);
            string nhash = Encrypt(normalize(attrs), seed);
            return(signature.Equals(nhash));
        }
    }
    public static string normalize(Hashtable attrs)
    {
        SortedList sl = new SortedList(attrs);
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < sl.Count; i++) {
            sb.Append(sl.GetKey(i)).Append("=").Append(sl.GetByIndex(i)).Append("|");
        }
        return (sb.ToString());
    }

    public static string ComputeHash(string   plainText, string secret)
    {
        TimeSpan t = (DateTime.UtcNow - new DateTime(1970, 1, 1));
        long ts = (long) t.TotalMilliseconds;
        // Check kind of crypto requested
        string hashValue;
        if (cryptoType == ASYMMETRIC) {
            hashValue = SignAsym(plainText, ts);
        } else  {
            string saltBuf = string.Concat(secret, ts);
            hashValue =  Encrypt(plainText, saltBuf);
        }
        // Include Timestamp....
        StringBuilder result = new StringBuilder("TS");
        result.Append(ts).Append("TS").Append(hashValue);
        
        // Return the result.
        return result.ToString();
    }
    private static string Encrypt(string plainText, string saltBuf)
    {
        if (dbg == 1)
           Console.WriteLine("Encrypt: plain={0} salt={1}",plainText, saltBuf);
        byte[] saltBytes = Encoding.UTF8.GetBytes(saltBuf);

        // Convert plain text into a byte array.
        byte[] plainTextBytes = Encoding.UTF8.GetBytes(plainText);
        
        // Allocate array, which will hold plain text and salt.
        byte[] plainTextWithSaltBytes = 
                new byte[plainTextBytes.Length + saltBytes.Length];

        // Copy plain text bytes into resulting array.
        for (int i=0; i < plainTextBytes.Length; i++)
            plainTextWithSaltBytes[i] = plainTextBytes[i];
        
        // Append salt bytes to the resulting array.
        for (int i=0; i < saltBytes.Length; i++)
            plainTextWithSaltBytes[plainTextBytes.Length + i] = saltBytes[i];

        HashAlgorithm hash  = new SHA1Managed();

        
        // Compute hash value of our plain text with appended salt.
        byte[] hashBytes = hash.ComputeHash(plainTextWithSaltBytes);
        
        // Create array which will hold hash and original salt bytes.
        byte[] hashWithSaltBytes = new byte[hashBytes.Length ];
        
        // Copy hash bytes into resulting array.
        for (int i=0; i < hashBytes.Length; i++)
            hashWithSaltBytes[i] = hashBytes[i];
            
        // Convert result into a base64-encoded string.
        string hashValue = Convert.ToBase64String(hashWithSaltBytes);
        if (dbg == 1)
           Console.WriteLine("Encrypt: hash={0}", hashValue);
        return hashValue;
    }
    private static string SignAsym(string plainText, long ts)
    {
        if (asymSignProvider == null)
            return null;
        SHA1 sha1 = SHA1.Create();
        byte[] cleartext = ASCIIEncoding.ASCII.GetBytes(plainText+ts);
        byte[] hash = sha1.ComputeHash(cleartext);
        object[] arr= new object[2];
        arr[0] = hash;
        arr[1] = CryptoConfig.MapNameToOID("SHA1" );
        byte[] sign1= (byte[]) 
        asymSignProvider.GetType().GetMethod("SignHash")
                        .Invoke(asymSignProvider, arr );
        string b64signature = Convert.ToBase64String( sign1);
        return b64signature;
    }
    private static bool VerifyAsym(string plainText, string signature, string ts)
    {
        SHA1 sha1 = SHA1.Create();
        byte[] bsignature = ASCIIEncoding.ASCII.GetBytes(signature);
        byte[] cleartext = ASCIIEncoding.ASCII.GetBytes(plainText+ts);
        byte[] hash = sha1.ComputeHash(cleartext);

        object[] arr= new object[3];
        arr[0] = hash;
        arr[1] = CryptoConfig.MapNameToOID("SHA1" );
        arr[2] = bsignature;
        bool verify = (bool) asymVerifyProvider.GetType()
                        .GetMethod("VerifyHash")
                        .Invoke(asymVerifyProvider, arr );
        return verify;
    }

}

public class SecureAttrsTest
{
    [STAThread]
    static void Main(string[] args)
    {

        Console.WriteLine("TEST 1 : Verify input string :  =====");
        string encString = "c3VuLnVzZXJpZD11dXxzdW4uc3BhcHB1cmw9YXBhcHB8YnJhbmNoPWJifG1haWw9bW18U2lnbmF0dXJlPVRTMTE3NDI3ODY0NzM5OVRTQ0ozSHNmTStLR1dYd3hEZnBGT2hWVUNLbFpzPQ==";
        if (args.Length > 0)
            encString = args[0];
        Hashtable result = SecureAttrs.verifyEncodedString(encString, "secret");
        if (result == null)
           Console.WriteLine("Verify enc str FAILED");
        else
           Console.WriteLine("Verify enc str PASSED: {0}", SecureAttrs.normalize(result));
        Console.WriteLine("TEST 1 END ==============");
        Console.WriteLine("TEST 2 : Simple signing and verification .=====");
        Hashtable ht4in = new Hashtable();
        ht4in.Add("branch","005"); 
        ht4in.Add("mail","user5@mail.com"); 
        ht4in.Add("sun.userid","user5"); 
        string encString4 = SecureAttrs.getEncodedString(ht4in, "secret");
        Console.WriteLine("Encoded {0}", encString4);
        Hashtable ht4out = SecureAttrs.verifyEncodedString(encString4, "secret");
        Console.WriteLine("Verify result {0}", ht4out);
        if (ht4out != null) {
            Console.WriteLine("Verify PASSED : {0}" , SecureAttrs.normalize(ht4out));
        }
        else {
            Console.WriteLine("Verify FAILED");
        }
        Console.WriteLine("TEST 1 END ==============");

      
    }
}
}
