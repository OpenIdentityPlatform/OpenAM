import java.security.MessageDigest;


public final class Encoder {
    static private final int DEFAULTMAXQUARTET = 19;
    static private final int BASELENGTH = 255;
    static private final int LOOKUPLENGTH = 64;
    static private final int TWENTYFOURBITGROUP = 24;
    static private final int EIGHTBIT = 8;
    static private final int SIXTEENBIT = 16;
    static private final int FOURBYTE = 4;
    static private final int SIGN = -128;
    static private final char PAD = '=';
    static final private byte[] base64Alphabet = new byte[BASELENGTH];
    static final private char[] lookUpBase64Alphabet = new char[LOOKUPLENGTH];


    static {
         for (int i = 0; i < BASELENGTH; i++) {
              base64Alphabet[i] = -1;
         }
         for (int i = 'Z'; i >= 'A'; i--) {
              base64Alphabet[i] = (byte) (i - 'A');
         }
         for (int i = 'z'; i >= 'a'; i--) {
              base64Alphabet[i] = (byte) (i - 'a' + 26);
         }


         for (int i = '9'; i >= '0'; i--) {
              base64Alphabet[i] = (byte) (i - '0' + 52);
         }


         base64Alphabet['+'] = 62;
         base64Alphabet['/'] = 63;


         for (int i = 0; i <= 25; i++)
              lookUpBase64Alphabet[i] = (char) ('A' + i);


         for (int i = 26, j = 0; i <= 51; i++, j++)
              lookUpBase64Alphabet[i] = (char) ('a' + j);


         for (int i = 52, j = 0; i <= 61; i++, j++)
              lookUpBase64Alphabet[i] = (char) ('0' + j);
         lookUpBase64Alphabet[62] = '+';
         lookUpBase64Alphabet[63] = '/';
    }


    private static String hash(String string) {
         try {




          MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
          sha1.update(string.getBytes("UTF-8"));
          return encode(sha1.digest());
     } catch (Exception ex) {
          ex.printStackTrace();
          return null;
     }
}


public static void main(String[] args) {
     if (args.length == 1) {
          // Hash the first argument and return
          System.out.println(hash(args[0]));
     } else {
          System.err.println(
               "Please provide string to be hashed and encoded.");
     }
     System.exit(0);
}
private static boolean isWhiteSpace(char octect) {
     return (octect == 0x20 || octect == 0xd || octect == 0xa
               || octect == 0x9);
}


private static boolean isPad(char octect) {
     return (octect == PAD);
}


private static boolean isData(char octect) {
     return (base64Alphabet[octect] != -1);
}


private static boolean isBase64(char octect) {
     return (isWhiteSpace(octect) || isPad(octect) || isData(octect));
}


/**
  * Encodes hex octects into Base64
  *
  * @param binaryData
  *               Array containing binaryData
  * @return Encoded Base64 array
  */
private static String encode(byte[] binaryData) {
     return encode(binaryData, 0);
}



/**
  * Encodes hex octects into Base64
  *
  * @param binaryData
  *               Array containing binaryData
  * @param maxCharsPerLine
  *               the max characters per line If 0, the encoded result appears
  *               on a single line.
  * @return Encoded Base64 array
  */
private static String encode(byte[] binaryData, int maxCharsPerLine) {


     // check maxCharsPerLine can be divided by
     int numQuartetPerLine = DEFAULTMAXQUARTET;
     if (maxCharsPerLine == 0) {
          numQuartetPerLine = DEFAULTMAXQUARTET;
     } else if (maxCharsPerLine % FOURBYTE == 0) {
          numQuartetPerLine = maxCharsPerLine / FOURBYTE;
     } else {
          // Illegal input. Take default 76 chars per line.
          numQuartetPerLine = DEFAULTMAXQUARTET;
     }


     if (binaryData == null)
          return null;


     int lengthDataBits = binaryData.length * EIGHTBIT;
     if (lengthDataBits == 0) {
          return "";
     }


     int fewerThan24bits = lengthDataBits % TWENTYFOURBITGROUP;
     int numberTriplets = lengthDataBits / TWENTYFOURBITGROUP;
     int numberQuartet = fewerThan24bits != 0 ? numberTriplets + 1
               : numberTriplets;
     int numberLines = (numberQuartet - 1) / numQuartetPerLine + 1;
     char encodedData[] = null;


     if (maxCharsPerLine == 0) {
          encodedData = new char[numberQuartet * 4];
     } else {
          encodedData = new char[numberQuartet * 4 + numberLines];
     }
     byte k = 0, l = 0, b1 = 0, b2 = 0, b3 = 0;


     int encodedIndex = 0;
     int dataIndex = 0;

int i = 0;


for (int line = 0; line < numberLines - 1; line++) {
     for (int quartet = 0; quartet < numQuartetPerLine; quartet++) {
          b1 = binaryData[dataIndex++];
          b2 = binaryData[dataIndex++];
          b3 = binaryData[dataIndex++];


          l = (byte) (b2 & 0x0f);
          k = (byte) (b1 & 0x03);


          byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2)
                    : (byte) ((b1) >> 2 ^ 0xc0);


          byte val2 = ((b2 & SIGN) == 0) ? (byte) (b2 >> 4)
                    : (byte) ((b2) >> 4 ^ 0xf0);
          byte val3 = ((b3 & SIGN) == 0) ? (byte) (b3 >> 6)
                    : (byte) ((b3) >> 6 ^ 0xfc);


          encodedData[encodedIndex++] = lookUpBase64Alphabet[val1];
          encodedData[encodedIndex++] = lookUpBase64Alphabet[val2
                    | (k << 4)];
          encodedData[encodedIndex++] = lookUpBase64Alphabet[(l << 2)
                    | val3];
          encodedData[encodedIndex++] = lookUpBase64Alphabet[b3 & 0x3f];


          i++;
     }


     if (maxCharsPerLine != 0) {
          encodedData[encodedIndex++] = 0xa;
     }
}


for (; i < numberTriplets; i++) {
     b1 = binaryData[dataIndex++];
     b2 = binaryData[dataIndex++];
     b3 = binaryData[dataIndex++];


     l = (byte) (b2 & 0x0f);
     k = (byte) (b1 & 0x03);


     byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2)
               : (byte) ((b1) >> 2 ^ 0xc0);


     byte val2 = ((b2 & SIGN) == 0) ? (byte) (b2 >> 4)
               : (byte) ((b2) >> 4 ^ 0xf0);
     byte val3 = ((b3 & SIGN) == 0) ? (byte) (b3 >> 6)

                        : (byte) ((b3) >> 6 ^ 0xfc);


              encodedData[encodedIndex++] = lookUpBase64Alphabet[val1];
              encodedData[encodedIndex++] = lookUpBase64Alphabet[val2 | (k << 4)];
              encodedData[encodedIndex++] = lookUpBase64Alphabet[(l << 2) | val3];
              encodedData[encodedIndex++] = lookUpBase64Alphabet[b3 & 0x3f];
         }


         // form integral number of 6-bit groups
         if (fewerThan24bits == EIGHTBIT) {
              b1 = binaryData[dataIndex];
              k = (byte) (b1 & 0x03);


              byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2)
                        : (byte) ((b1) >> 2 ^ 0xc0);
              encodedData[encodedIndex++] = lookUpBase64Alphabet[val1];
              encodedData[encodedIndex++] = lookUpBase64Alphabet[k << 4];
              encodedData[encodedIndex++] = PAD;
              encodedData[encodedIndex++] = PAD;
         } else if (fewerThan24bits == SIXTEENBIT) {
              b1 = binaryData[dataIndex];
              b2 = binaryData[dataIndex + 1];
              l = (byte) (b2 & 0x0f);
              k = (byte) (b1 & 0x03);


              byte val1 = ((b1 & SIGN) == 0) ? (byte) (b1 >> 2)
                        : (byte) ((b1) >> 2 ^ 0xc0);
              byte val2 = ((b2 & SIGN) == 0) ? (byte) (b2 >> 4)
                        : (byte) ((b2) >> 4 ^ 0xf0);


              encodedData[encodedIndex++] = lookUpBase64Alphabet[val1];
              encodedData[encodedIndex++] = lookUpBase64Alphabet[val2 | (k << 4)];
              encodedData[encodedIndex++] = lookUpBase64Alphabet[l << 2];
              encodedData[encodedIndex++] = PAD;
         }


         if (maxCharsPerLine != 0) {
              encodedData[encodedIndex] = 0xa;
         }
         return new String(encodedData);
    }
}


