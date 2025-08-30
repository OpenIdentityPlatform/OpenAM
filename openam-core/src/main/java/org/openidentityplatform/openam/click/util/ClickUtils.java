/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.openidentityplatform.openam.click.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openidentityplatform.openam.click.Context;
import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.Page;
import org.openidentityplatform.openam.click.ActionResult;
import org.apache.click.Stateful;
import org.openidentityplatform.openam.click.control.AbstractControl;
import org.openidentityplatform.openam.click.control.AbstractLink;
import org.apache.click.control.ActionLink;
import org.openidentityplatform.openam.click.control.Container;
import org.openidentityplatform.openam.click.control.Field;
import org.openidentityplatform.openam.click.control.Form;

import org.apache.click.util.Format;
import org.apache.click.util.MessagesMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.openidentityplatform.openam.click.service.ConfigService;
import org.openidentityplatform.openam.click.service.LogService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;

/**
 * Provides miscellaneous Form, String and Stream utility methods.
 */
public class ClickUtils {

    // ------------------------------------------------------- Public Constants

    /**
     * The resource <tt>versioning</tt> request attribute: key: &nbsp;
     * <tt>enable-resource-version</tt>.
     * <p/>
     * If this attribute is set to <tt>true</tt> and Click is running in
     * <tt>production</tt> or <tt>profile</tt> mode, Click resources returned
     * from {@link org.apache.click.Control#getHeadElements()} will have a
     * <tt>version indicator</tt> added to their path.
     *
     * @see org.apache.click.Control#getHeadElements()
     * @see org.openidentityplatform.openam.click.util.ClickUtils#getResourceVersionIndicator(Context)
     */
    public static final String ENABLE_RESOURCE_VERSION = "enable-resource-version";

    /**
     * The default Click configuration filename: &nbsp;
     * "<tt>/WEB-INF/click.xml</tt>".
     */
    public static final String DEFAULT_APP_CONFIG = "/WEB-INF/click.xml";

    /** The version indicator separator string. */
    public static final String VERSION_INDICATOR_SEP = "_";

    /** The static web resource version number indicator string. */
    public static final String RESOURCE_VERSION_INDICATOR =
            VERSION_INDICATOR_SEP + getClickVersion();

    // ------------------------------------------------------ Private Constants

    /** The cached resource version indicator. */
    private static String cachedResourceVersionIndicator;

    /** The static application-wide resource version indicator. */
    private static String applicationVersion;

    /** The cached application version indicator string. */
    private static String cachedApplicationVersionIndicator;

    /**
     * Character used to separate username and password in persistent cookies.
     * 0x13 == "Device Control 3" non-printing ASCII char. Unlikely to appear
     * in a username
     */
    private static final char DELIMITER = 0x13;

    /*
     * "Tweakable" parameters for the cookie encoding. NOTE: changing these
     * and recompiling this class will essentially invalidate old cookies.
     */
    private final static char ENCODE_CHAR_OFFSET1 = 'C';
    private final static char ENCODE_CHAR_OFFSET2 = 'i';

    /** Hexadecimal characters for MD5 encoding. */
    private static final char[] HEXADECIMAL = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /** Ajax request header or parameter: "<tt>X-Requested-With</tt>". */
    private static final String X_REQUESTED_WITH = "X-Requested-With";

    /**
     * The array of escaped HTML character values, indexed on char value.
     * <p/>
     * HTML entities values were derived from Jakarta Commons Lang
     * <tt>org.apache.commons.lang.Entities</tt> class.
     */
    private static final String[] HTML_ENTITIES = new String[9999];

    static {
        HTML_ENTITIES[34] = "&quot;"; // " - double-quote
        HTML_ENTITIES[38] = "&amp;"; // & - ampersand
        HTML_ENTITIES[60] = "&lt;"; // < - less-than
        HTML_ENTITIES[62] = "&gt;"; // > - greater-than
        HTML_ENTITIES[160] = "&nbsp;"; // non-breaking space
        HTML_ENTITIES[161] = "&iexcl;"; // inverted exclamation mark
        HTML_ENTITIES[162] = "&cent;";  // cent sign
        HTML_ENTITIES[163] = "&pound;"; // pound sign
        HTML_ENTITIES[164] = "&curren;"; // currency sign
        HTML_ENTITIES[165] = "&yen;";  // yen sign = yuan sign
        HTML_ENTITIES[166] = "&brvbar;"; // broken bar = broken vertical bar
        HTML_ENTITIES[167] = "&sect;"; // section sign
        HTML_ENTITIES[168] = "&uml;"; // diaeresis = spacing diaeresis
        HTML_ENTITIES[169] = "&copy;"; // © - copyright sign
        HTML_ENTITIES[170] = "&ordf;"; // feminine ordinal indicator
        HTML_ENTITIES[171] = "&laquo;"; // left-pointing double angle quotation mark = left pointing guillemet
        HTML_ENTITIES[172] = "&not;";   //not sign
        HTML_ENTITIES[173] = "&shy;";   //soft hyphen = discretionary hyphen
        HTML_ENTITIES[174] = "&reg;";   // ® - registered trademark sign
        HTML_ENTITIES[175] = "&macr;";   //macron = spacing macron = overline = APL overbar
        HTML_ENTITIES[176] = "&deg;";   //degree sign
        HTML_ENTITIES[177] = "&plusmn;";   //plus-minus sign = plus-or-minus sign
        HTML_ENTITIES[178] = "&sup2;";   //superscript two = superscript digit two = squared
        HTML_ENTITIES[179] = "&sup3;";   //superscript three = superscript digit three = cubed
        HTML_ENTITIES[180] = "&acute;";   //acute accent = spacing acute
        HTML_ENTITIES[181] = "&micro;";   //micro sign
        HTML_ENTITIES[182] = "&para;";   //pilcrow sign = paragraph sign
        HTML_ENTITIES[183] = "&middot;";   //middle dot = Georgian comma = Greek middle dot
        HTML_ENTITIES[184] = "&cedil;";   //cedilla = spacing cedilla
        HTML_ENTITIES[185] = "&sup1;";   //superscript one = superscript digit one
        HTML_ENTITIES[186] = "&ordm;";   //masculine ordinal indicator
        HTML_ENTITIES[187] = "&raquo;";   //right-pointing double angle quotation mark = right pointing guillemet
        HTML_ENTITIES[188] = "&frac14;";   //vulgar fraction one quarter = fraction one quarter
        HTML_ENTITIES[189] = "&frac12;";   //vulgar fraction one half = fraction one half
        HTML_ENTITIES[190] = "&frac34;";   //vulgar fraction three quarters = fraction three quarters
        HTML_ENTITIES[191] = "&iquest;";   //inverted question mark = turned question mark
        HTML_ENTITIES[192] = "&Agrave;";   // À - uppercase A, grave accent
        HTML_ENTITIES[193] = "&Aacute;";   // Á - uppercase A, acute accent
        HTML_ENTITIES[194] = "&Acirc;";   // Â - uppercase A, circumflex accent
        HTML_ENTITIES[195] = "&Atilde;";   // Ã - uppercase A, tilde
        HTML_ENTITIES[196] = "&Auml;";   // Ä - uppercase A, umlaut
        HTML_ENTITIES[197] = "&Aring;";   // Å - uppercase A, ring
        HTML_ENTITIES[198] = "&AElig;";   // Æ - uppercase AE
        HTML_ENTITIES[199] = "&Ccedil;";   // Ç - uppercase C, cedilla
        HTML_ENTITIES[200] = "&Egrave;";   // È - uppercase E, grave accent
        HTML_ENTITIES[201] = "&Eacute;";   // É - uppercase E, acute accent
        HTML_ENTITIES[202] = "&Ecirc;";   // Ê - uppercase E, circumflex accent
        HTML_ENTITIES[203] = "&Euml;";   // Ë - uppercase E, umlaut
        HTML_ENTITIES[204] = "&Igrave;";   // Ì - uppercase I, grave accent
        HTML_ENTITIES[205] = "&Iacute;";   // Í - uppercase I, acute accent
        HTML_ENTITIES[206] = "&Icirc;";   // Î - uppercase I, circumflex accent
        HTML_ENTITIES[207] = "&Iuml;";   // Ï - uppercase I, umlaut
        HTML_ENTITIES[208] = "&ETH;";   // Ð - uppercase Eth, Icelandic
        HTML_ENTITIES[209] = "&Ntilde;";   // Ñ - uppercase N, tilde
        HTML_ENTITIES[210] = "&Ograve;";   // Ò - uppercase O, grave accent
        HTML_ENTITIES[211] = "&Oacute;";   // Ó - uppercase O, acute accent
        HTML_ENTITIES[212] = "&Ocirc;";   // Ô - uppercase O, circumflex accent
        HTML_ENTITIES[213] = "&Otilde;";   // Õ - uppercase O, tilde
        HTML_ENTITIES[214] = "&Ouml;";   // Ö - uppercase O, umlaut
        HTML_ENTITIES[215] = "&times;";   //multiplication sign
        HTML_ENTITIES[216] = "&Oslash;";   // Ø - uppercase O, slash
        HTML_ENTITIES[217] = "&Ugrave;";   // Ù - uppercase U, grave accent
        HTML_ENTITIES[218] = "&Uacute;";   // Ú - uppercase U, acute accent
        HTML_ENTITIES[219] = "&Ucirc;";   // Û - uppercase U, circumflex accent
        HTML_ENTITIES[220] = "&Uuml;";   // Ü - uppercase U, umlaut
        HTML_ENTITIES[221] = "&Yacute;";   // Ý - uppercase Y, acute accent
        HTML_ENTITIES[222] = "&THORN;";   // Þ - uppercase THORN, Icelandic
        HTML_ENTITIES[223] = "&szlig;";   // ß - lowercase sharps, German
        HTML_ENTITIES[224] = "&agrave;";   // à - lowercase a, grave accent
        HTML_ENTITIES[225] = "&aacute;";   // á - lowercase a, acute accent
        HTML_ENTITIES[226] = "&acirc;";   // â - lowercase a, circumflex accent
        HTML_ENTITIES[227] = "&atilde;";   // ã - lowercase a, tilde
        HTML_ENTITIES[228] = "&auml;";   // ä - lowercase a, umlaut
        HTML_ENTITIES[229] = "&aring;";   // å - lowercase a, ring
        HTML_ENTITIES[230] = "&aelig;";   // æ - lowercase ae
        HTML_ENTITIES[231] = "&ccedil;";   // ç - lowercase c, cedilla
        HTML_ENTITIES[232] = "&egrave;";   // è - lowercase e, grave accent
        HTML_ENTITIES[233] = "&eacute;";   // é - lowercase e, acute accent
        HTML_ENTITIES[234] = "&ecirc;";   // ê - lowercase e, circumflex accent
        HTML_ENTITIES[235] = "&euml;";   // ë - lowercase e, umlaut
        HTML_ENTITIES[236] = "&igrave;";   // ì - lowercase i, grave accent
        HTML_ENTITIES[237] = "&iacute;";   // í - lowercase i, acute accent
        HTML_ENTITIES[238] = "&icirc;";   // î - lowercase i, circumflex accent
        HTML_ENTITIES[239] = "&iuml;";   // ï - lowercase i, umlaut
        HTML_ENTITIES[240] = "&eth;";   // ð - lowercase eth, Icelandic
        HTML_ENTITIES[241] = "&ntilde;";   // ñ - lowercase n, tilde
        HTML_ENTITIES[242] = "&ograve;";   // ò - lowercase o, grave accent
        HTML_ENTITIES[243] = "&oacute;";   // ó - lowercase o, acute accent
        HTML_ENTITIES[244] = "&ocirc;";   // ô - lowercase o, circumflex accent
        HTML_ENTITIES[245] = "&otilde;";   // õ - lowercase o, tilde
        HTML_ENTITIES[246] = "&ouml;";   // ö - lowercase o, umlaut
        HTML_ENTITIES[247] = "&divide;";   // division sign
        HTML_ENTITIES[248] = "&oslash;";   // ø - lowercase o, slash
        HTML_ENTITIES[249] = "&ugrave;";   // ù - lowercase u, grave accent
        HTML_ENTITIES[250] = "&uacute;";   // ú - lowercase u, acute accent
        HTML_ENTITIES[251] = "&ucirc;";   // û - lowercase u, circumflex accent
        HTML_ENTITIES[252] = "&uuml;";   // ü - lowercase u, umlaut
        HTML_ENTITIES[253] = "&yacute;";   // ý - lowercase y, acute accent
        HTML_ENTITIES[254] = "&thorn;";   // þ - lowercase thorn, Icelandic
        HTML_ENTITIES[255] = "&yuml;";   // ÿ - lowercase y, umlaut
        // http://www.w3.org/TR/REC-html40/sgml/entities.html
        // <!-- Latin Extended-B -->
        HTML_ENTITIES[402] = "&fnof;";   //latin small f with hook = function= florin, U+0192 ISOtech -->
        // <!-- Greek -->
        HTML_ENTITIES[913] = "&Alpha;";   //greek capital letter alpha, U+0391 -->
        HTML_ENTITIES[914] = "&Beta;";   //greek capital letter beta, U+0392 -->
        HTML_ENTITIES[915] = "&Gamma;";   //greek capital letter gamma,U+0393 ISOgrk3 -->
        HTML_ENTITIES[916] = "&Delta;";   //greek capital letter delta,U+0394 ISOgrk3 -->
        HTML_ENTITIES[917] = "&Epsilon;";   //greek capital letter epsilon, U+0395 -->
        HTML_ENTITIES[918] = "&Zeta;";   //greek capital letter zeta, U+0396 -->
        HTML_ENTITIES[919] = "&Eta;";   //greek capital letter eta, U+0397 -->
        HTML_ENTITIES[920] = "&Theta;";   //greek capital letter theta,U+0398 ISOgrk3 -->
        HTML_ENTITIES[921] = "&Iota;";   //greek capital letter iota, U+0399 -->
        HTML_ENTITIES[922] = "&Kappa;";   //greek capital letter kappa, U+039A -->
        HTML_ENTITIES[923] = "&Lambda;";   //greek capital letter lambda,U+039B ISOgrk3 -->
        HTML_ENTITIES[924] = "&Mu;";   //greek capital letter mu, U+039C -->
        HTML_ENTITIES[925] = "&Nu;";   //greek capital letter nu, U+039D -->
        HTML_ENTITIES[926] = "&Xi;";   //greek capital letter xi, U+039E ISOgrk3 -->
        HTML_ENTITIES[927] = "&Omicron;";   //greek capital letter omicron, U+039F -->
        HTML_ENTITIES[928] = "&Pi;";   //greek capital letter pi, U+03A0 ISOgrk3 -->
        HTML_ENTITIES[929] = "&Rho;";   //greek capital letter rho, U+03A1 -->
        // <!-- there is no Sigmaf, and no U+03A2 character either -->
        HTML_ENTITIES[931] = "&Sigma;";   //greek capital letter sigma,U+03A3 ISOgrk3 -->
        HTML_ENTITIES[932] = "&Tau;";   //greek capital letter tau, U+03A4 -->
        HTML_ENTITIES[933] = "&Upsilon;";   //greek capital letter upsilon,U+03A5 ISOgrk3 -->
        HTML_ENTITIES[934] = "&Phi;";   //greek capital letter phi,U+03A6 ISOgrk3 -->
        HTML_ENTITIES[935] = "&Chi;";   //greek capital letter chi, U+03A7 -->
        HTML_ENTITIES[936] = "&Psi;";   //greek capital letter psi,U+03A8 ISOgrk3 -->
        HTML_ENTITIES[937] = "&Omega;";   //greek capital letter omega,U+03A9 ISOgrk3 -->
        HTML_ENTITIES[945] = "&alpha;";   //greek small letter alpha,U+03B1 ISOgrk3 -->
        HTML_ENTITIES[946] = "&beta;";   //greek small letter beta, U+03B2 ISOgrk3 -->
        HTML_ENTITIES[947] = "&gamma;";   //greek small letter gamma,U+03B3 ISOgrk3 -->
        HTML_ENTITIES[948] = "&delta;";   //greek small letter delta,U+03B4 ISOgrk3 -->
        HTML_ENTITIES[949] = "&epsilon;";   //greek small letter epsilon,U+03B5 ISOgrk3 -->
        HTML_ENTITIES[950] = "&zeta;";   //greek small letter zeta, U+03B6 ISOgrk3 -->
        HTML_ENTITIES[951] = "&eta;";   //greek small letter eta, U+03B7 ISOgrk3 -->
        HTML_ENTITIES[952] = "&theta;";   //greek small letter theta,U+03B8 ISOgrk3 -->
        HTML_ENTITIES[953] = "&iota;";   //greek small letter iota, U+03B9 ISOgrk3 -->
        HTML_ENTITIES[954] = "&kappa;";   //greek small letter kappa,U+03BA ISOgrk3 -->
        HTML_ENTITIES[955] = "&lambda;";   //greek small letter lambda,U+03BB ISOgrk3 -->
        HTML_ENTITIES[956] = "&mu;";   //greek small letter mu, U+03BC ISOgrk3 -->
        HTML_ENTITIES[957] = "&nu;";   //greek small letter nu, U+03BD ISOgrk3 -->
        HTML_ENTITIES[958] = "&xi;";   //greek small letter xi, U+03BE ISOgrk3 -->
        HTML_ENTITIES[959] = "&omicron;";   //greek small letter omicron, U+03BF NEW -->
        HTML_ENTITIES[960] = "&pi;";   //greek small letter pi, U+03C0 ISOgrk3 -->
        HTML_ENTITIES[961] = "&rho;";   //greek small letter rho, U+03C1 ISOgrk3 -->
        HTML_ENTITIES[962] = "&sigmaf;";   //greek small letter final sigma,U+03C2 ISOgrk3 -->
        HTML_ENTITIES[963] = "&sigma;";   //greek small letter sigma,U+03C3 ISOgrk3 -->
        HTML_ENTITIES[964] = "&tau;";   //greek small letter tau, U+03C4 ISOgrk3 -->
        HTML_ENTITIES[965] = "&upsilon;";   //greek small letter upsilon,U+03C5 ISOgrk3 -->
        HTML_ENTITIES[966] = "&phi;";   //greek small letter phi, U+03C6 ISOgrk3 -->
        HTML_ENTITIES[967] = "&chi;";   //greek small letter chi, U+03C7 ISOgrk3 -->
        HTML_ENTITIES[968] = "&psi;";   //greek small letter psi, U+03C8 ISOgrk3 -->
        HTML_ENTITIES[969] = "&omega;";   //greek small letter omega,U+03C9 ISOgrk3 -->
        HTML_ENTITIES[977] = "&thetasym;";   //greek small letter theta symbol,U+03D1 NEW -->
        HTML_ENTITIES[978] = "&upsih;";   //greek upsilon with hook symbol,U+03D2 NEW -->
        HTML_ENTITIES[982] = "&piv;";   //greek pi symbol, U+03D6 ISOgrk3 -->
        // <!-- General Punctuation -->
        HTML_ENTITIES[8226] = "&bull;";   //bullet = black small circle,U+2022 ISOpub  -->
        // <!-- bullet is NOT the same as bullet operator, U+2219 -->
        HTML_ENTITIES[8230] = "&hellip;";   //horizontal ellipsis = three dot leader,U+2026 ISOpub  -->
        HTML_ENTITIES[8242] = "&prime;";   //prime = minutes = feet, U+2032 ISOtech -->
        HTML_ENTITIES[8243] = "&Prime;";   //double prime = seconds = inches,U+2033 ISOtech -->
        HTML_ENTITIES[8254] = "&oline;";   //overline = spacing overscore,U+203E NEW -->
        HTML_ENTITIES[8260] = "&frasl;";   //fraction slash, U+2044 NEW -->
        // <!-- Letterlike Symbols -->
        HTML_ENTITIES[8472] = "&weierp;";   //script capital P = power set= Weierstrass p, U+2118 ISOamso -->
        HTML_ENTITIES[8465] = "&image;";   //blackletter capital I = imaginary part,U+2111 ISOamso -->
        HTML_ENTITIES[8476] = "&real;";   //blackletter capital R = real part symbol,U+211C ISOamso -->
        HTML_ENTITIES[8482] = "&trade;";   //trade mark sign, U+2122 ISOnum -->
        HTML_ENTITIES[8501] = "&alefsym;";   //alef symbol = first transfinite cardinal,U+2135 NEW -->
        // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the same glyph could be used to depict both characters -->
        // <!-- Arrows -->
        HTML_ENTITIES[8592] = "&larr;";   //leftwards arrow, U+2190 ISOnum -->
        HTML_ENTITIES[8593] = "&uarr;";   //upwards arrow, U+2191 ISOnum-->
        HTML_ENTITIES[8594] = "&rarr;";   //rightwards arrow, U+2192 ISOnum -->
        HTML_ENTITIES[8595] = "&darr;";   //downwards arrow, U+2193 ISOnum -->
        HTML_ENTITIES[8596] = "&harr;";   //left right arrow, U+2194 ISOamsa -->
        HTML_ENTITIES[8629] = "&crarr;";   //downwards arrow with corner leftwards= carriage return, U+21B5 NEW -->
        HTML_ENTITIES[8656] = "&lArr;";   //leftwards double arrow, U+21D0 ISOtech -->
        // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by' arrowbut also does not have any other character for that function. So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
        HTML_ENTITIES[8657] = "&uArr;";   //upwards double arrow, U+21D1 ISOamsa -->
        HTML_ENTITIES[8658] = "&rArr;";   //rightwards double arrow,U+21D2 ISOtech -->
        // <!-- ISO 10646 does not say this is the 'implies' character but does not have another character with this function so ?rArr can be used for 'implies' as ISOtech suggests -->
        HTML_ENTITIES[8659] = "&dArr;";   //downwards double arrow, U+21D3 ISOamsa -->
        HTML_ENTITIES[8660] = "&hArr;";   //left right double arrow,U+21D4 ISOamsa -->
        // <!-- Mathematical Operators -->
        HTML_ENTITIES[8704] = "&forall;";   //for all, U+2200 ISOtech -->
        HTML_ENTITIES[8706] = "&part;";   //partial differential, U+2202 ISOtech  -->
        HTML_ENTITIES[8707] = "&exist;";   //there exists, U+2203 ISOtech -->
        HTML_ENTITIES[8709] = "&empty;";   //empty set = null set = diameter,U+2205 ISOamso -->
        HTML_ENTITIES[8711] = "&nabla;";   //nabla = backward difference,U+2207 ISOtech -->
        HTML_ENTITIES[8712] = "&isin;";   //element of, U+2208 ISOtech -->
        HTML_ENTITIES[8713] = "&notin;";   //not an element of, U+2209 ISOtech -->
        HTML_ENTITIES[8715] = "&ni;";   //contains as member, U+220B ISOtech -->
        // <!-- should there be a more memorable name than 'ni'? -->
        HTML_ENTITIES[8719] = "&prod;";   //n-ary product = product sign,U+220F ISOamsb -->
        // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi' though the same glyph might be used for both -->
        HTML_ENTITIES[8721]  = "&sum;";   //n-ary summation, U+2211 ISOamsb -->
        // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma' though the same glyph might be used for both -->
        HTML_ENTITIES[8722]  = "&minus;";   //minus sign, U+2212 ISOtech -->
        HTML_ENTITIES[8727]  = "&lowast;";   //asterisk operator, U+2217 ISOtech -->
        HTML_ENTITIES[8730]  = "&radic;";   //square root = radical sign,U+221A ISOtech -->
        HTML_ENTITIES[8733]  = "&prop;";   //proportional to, U+221D ISOtech -->
        HTML_ENTITIES[8734]  = "&infin;";   //infinity, U+221E ISOtech -->
        HTML_ENTITIES[8736] = "&ang;";   //angle, U+2220 ISOamso -->
        HTML_ENTITIES[8743] = "&and;";   //logical and = wedge, U+2227 ISOtech -->
        HTML_ENTITIES[8744] = "&or;";   //logical or = vee, U+2228 ISOtech -->
        HTML_ENTITIES[8745] = "&cap;";   //intersection = cap, U+2229 ISOtech -->
        HTML_ENTITIES[8746] = "&cup;";   //union = cup, U+222A ISOtech -->
        HTML_ENTITIES[8747] = "&int;";   //integral, U+222B ISOtech -->
        HTML_ENTITIES[8756] = "&there4;";   //therefore, U+2234 ISOtech -->
        HTML_ENTITIES[8764] = "&sim;";   //tilde operator = varies with = similar to,U+223C ISOtech -->
        // <!-- tilde operator is NOT the same character as the tilde, U+007E,although the same glyph might be used to represent both  -->
        HTML_ENTITIES[8773] = "&cong;";   //approximately equal to, U+2245 ISOtech -->
        HTML_ENTITIES[8776] = "&asymp;";   //almost equal to = asymptotic to,U+2248 ISOamsr -->
        HTML_ENTITIES[8800] = "&ne;";   //not equal to, U+2260 ISOtech -->
        HTML_ENTITIES[8801] = "&equiv;";   //identical to, U+2261 ISOtech -->
        HTML_ENTITIES[8804] = "&le;";   //less-than or equal to, U+2264 ISOtech -->
        HTML_ENTITIES[8805] = "&ge;";   //greater-than or equal to,U+2265 ISOtech -->
        HTML_ENTITIES[8834] = "&sub;";   //subset of, U+2282 ISOtech -->
        HTML_ENTITIES[8835] = "&sup;";   //superset of, U+2283 ISOtech -->
        // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the Symbol font encoding and is not included. Should it be, for symmetry?It is in ISOamsn  --> <!ENTITY nsub"; 8836   //not a subset of, U+2284 ISOamsn -->
        HTML_ENTITIES[8838] = "&sube;";   //subset of or equal to, U+2286 ISOtech -->
        HTML_ENTITIES[8839] = "&supe;";   //superset of or equal to,U+2287 ISOtech -->
        HTML_ENTITIES[8853] = "&oplus;";   //circled plus = direct sum,U+2295 ISOamsb -->
        HTML_ENTITIES[8855] = "&otimes;";   //circled times = vector product,U+2297 ISOamsb -->
        HTML_ENTITIES[8869] = "&perp;";   //up tack = orthogonal to = perpendicular,U+22A5 ISOtech -->
        HTML_ENTITIES[8901] = "&sdot;";   //dot operator, U+22C5 ISOamsb -->
        // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
        // <!-- Miscellaneous Technical -->
        HTML_ENTITIES[8968] = "&lceil;";   //left ceiling = apl upstile,U+2308 ISOamsc  -->
        HTML_ENTITIES[8969] = "&rceil;";   //right ceiling, U+2309 ISOamsc  -->
        HTML_ENTITIES[8970] = "&lfloor;";   //left floor = apl downstile,U+230A ISOamsc  -->
        HTML_ENTITIES[8971] = "&rfloor;";   //right floor, U+230B ISOamsc  -->
        HTML_ENTITIES[9001] = "&lang;";   //left-pointing angle bracket = bra,U+2329 ISOtech -->
        // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation mark' -->
        HTML_ENTITIES[9002]  = "&rang;";   //right-pointing angle bracket = ket,U+232A ISOtech -->
        // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A 'single right-pointing angle quotation mark' -->
        // <!-- Geometric Shapes -->
        HTML_ENTITIES[9674] = "&loz;";   //lozenge, U+25CA ISOpub -->
        // <!-- Miscellaneous Symbols -->
        HTML_ENTITIES[9824] = "&spades;";   //black spade suit, U+2660 ISOpub -->
        // <!-- black here seems to mean filled as opposed to hollow -->
        HTML_ENTITIES[9827] = "&clubs;";   //black club suit = shamrock,U+2663 ISOpub -->
        HTML_ENTITIES[9829] = "&hearts;";   //black heart suit = valentine,U+2665 ISOpub -->
        HTML_ENTITIES[9830] = "&diams;";   //black diamond suit, U+2666 ISOpub -->
        // <!-- Latin Extended-A -->
        HTML_ENTITIES[338] = "&OElig;";   //  -- latin capital ligature OE,U+0152 ISOlat2 -->
        HTML_ENTITIES[339] = "&oelig;";   //  -- latin small ligature oe, U+0153 ISOlat2 -->
        // <!-- ligature is a misnomer, this is a separate character in some languages -->
        HTML_ENTITIES[352] = "&Scaron;";   //  -- latin capital letter S with caron,U+0160 ISOlat2 -->
        HTML_ENTITIES[353] = "&scaron;";   //  -- latin small letter s with caron,U+0161 ISOlat2 -->
        HTML_ENTITIES[376] = "&Yuml;";   //  -- latin capital letter Y with diaeresis,U+0178 ISOlat2 -->
        // <!-- Spacing Modifier Letters -->
        HTML_ENTITIES[710] = "&circ;";   //  -- modifier letter circumflex accent,U+02C6 ISOpub -->
        HTML_ENTITIES[732] = "&tilde;";   //small tilde, U+02DC ISOdia -->
        // <!-- General Punctuation -->
        HTML_ENTITIES[8194] = "&ensp;";   //en space, U+2002 ISOpub -->
        HTML_ENTITIES[8195] = "&emsp;";   //em space, U+2003 ISOpub -->
        HTML_ENTITIES[8201] = "&thinsp;";   //thin space, U+2009 ISOpub -->
        HTML_ENTITIES[8204] = "&zwnj;";   //zero width non-joiner,U+200C NEW RFC 2070 -->
        HTML_ENTITIES[8205] = "&zwj;";   //zero width joiner, U+200D NEW RFC 2070 -->
        HTML_ENTITIES[8206] = "&lrm;";   //left-to-right mark, U+200E NEW RFC 2070 -->
        HTML_ENTITIES[8207] = "&rlm;";   //right-to-left mark, U+200F NEW RFC 2070 -->
        HTML_ENTITIES[8211] = "&ndash;";   //en dash, U+2013 ISOpub -->
        HTML_ENTITIES[8212] = "&mdash;";   //em dash, U+2014 ISOpub -->
        HTML_ENTITIES[8216] = "&lsquo;";   //left single quotation mark,U+2018 ISOnum -->
        HTML_ENTITIES[8217] = "&rsquo;";   //right single quotation mark,U+2019 ISOnum -->
        HTML_ENTITIES[8218] = "&sbquo;";   //single low-9 quotation mark, U+201A NEW -->
        HTML_ENTITIES[8220] = "&ldquo;";   //left double quotation mark,U+201C ISOnum -->
        HTML_ENTITIES[8221] = "&rdquo;";   //right double quotation mark,U+201D ISOnum -->
        HTML_ENTITIES[8222] = "&bdquo;";   //double low-9 quotation mark, U+201E NEW -->
        HTML_ENTITIES[8224] = "&dagger;";   //dagger, U+2020 ISOpub -->
        HTML_ENTITIES[8225] = "&Dagger;";   //double dagger, U+2021 ISOpub -->
        HTML_ENTITIES[8240] = "&permil;";   //per mille sign, U+2030 ISOtech -->
        HTML_ENTITIES[8249] = "&lsaquo;";   //single left-pointing angle quotation mark,U+2039 ISO proposed -->
        // <!-- lsaquo is proposed but not yet ISO standardized -->
        HTML_ENTITIES[8250] = "&rsaquo;";   //single right-pointing angle quotation mark,U+203A ISO proposed -->
        // <!-- rsaquo is proposed but not yet ISO standardized -->
        HTML_ENTITIES[8364] = "&euro;";   //  -- euro sign, U+20AC NEW -->
    };

    /**
     * The array of escaped XML character values, indexed on char value.
     * <p/>
     * XML entities values were derived from Jakarta Commons Lang
     * <tt>org.apache.commons.lang.Entities</tt> class.
     */
    private static final String[] XML_ENTITIES = new String[63];

    static {
        XML_ENTITIES[34] = "&quot;"; // " - double-quote
        XML_ENTITIES[38] = "&amp;"; // & - ampersand
        XML_ENTITIES[39] = "&#039;"; // ' - quote
        XML_ENTITIES[60] = "&lt;"; // < - less-than
        XML_ENTITIES[62] = "&gt;"; // > - greater-than
    };

    // --------------------------------------------------------- Public Methods

    /**
     * Perform an auto post redirect to the specified target using the given
     * response. If the params Map is defined then the form will post these
     * values as name value pairs. If the compress value is true, this method
     * will attempt to gzip compress the response content if requesting
     * browser accepts "gzip" encoding.
     * <p/>
     * Once this method has returned you should not attempt to write to the
     * servlet response.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param target the target URL to send the auto post redirect to
     * @param params the map of parameter values to post
     * @param compress the flag to specify whether to attempt gzip compression
     *         of the response content
     */
    public static void autoPostRedirect(HttpServletRequest request,
                                        HttpServletResponse response, String target, Map<?, ?> params,
                                        boolean compress) {

        Validate.notNull(request, "Null response parameter");
        Validate.notNull(response, "Null response parameter");
        Validate.notNull(target, "Null target parameter");

        HtmlStringBuffer buffer = new HtmlStringBuffer(1024);
        buffer.append("<html><body onload=\"document.forms[0].submit();\">");
        buffer.append("<form name=\"form\" method=\"post\" style=\"{display:none;}\" action=\"");
        buffer.append(target);
        buffer.append("\">");
        for (Map.Entry<?, ?> entry : params.entrySet()) {
            buffer.elementStart("textarea");
            buffer.appendAttribute("name", entry.getKey());
            buffer.elementEnd();
            buffer.append(entry.getValue());
            buffer.elementEnd("textarea");
        }
        buffer.append("</form></body></html>");

        // Determine whether browser will accept gzip compression
        if (compress) {
            compress = false;
            Enumeration<?> e = request.getHeaders("Accept-Encoding");

            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                if (name.indexOf("gzip") != -1) {
                    compress = true;
                    break;
                }
            }
        }

        OutputStream os = null;
        GZIPOutputStream gos = null;
        try {
            response.setContentType("text/html");

            if (compress) {
                response.setHeader("Content-Encoding", "gzip");

                os = response.getOutputStream();
                gos = new GZIPOutputStream(os);
                gos.write(buffer.toString().getBytes());

            } else {
                response.setContentLength(buffer.length());

                os = response.getOutputStream();
                os.write(buffer.toString().getBytes());
            }

        } catch (IOException ex) {
            org.apache.click.util.ClickUtils.getLogService().error(ex.getMessage(), ex);

        } finally {
            org.apache.click.util.ClickUtils.close(gos);
            org.apache.click.util.ClickUtils.close(os);
        }
    }

    /**
     * A helper method that binds the submitted request value to the Field's
     * value. Since Field values are only bound during the <tt>"onProcess"</tt>
     * event, this method can be used to bind a submitted Field value during
     * the <tt>"onInit"</tt> event, which occurs <b>before</b> the
     * <tt>"onProcess"</tt> event.
     * <p/>
     * This is especially useful for dynamic Form and Page behavior where Field
     * values are inspected during the <tt>"onInit"</tt> event to add or remove
     * specific Fields.
     * <p/>
     * <b>Please note</b>: this method won't bind disabled fields, unless the
     * field has an incoming request parameter matching its name. If an incoming
     * request parameter is present, this method will switch off the Field's
     * disabled property.
     * <p/>
     * This method delegates to
     * {@link #canBind(org.openidentityplatform.openam.click.Control, org.openidentityplatform.openam.click.Context)}
     * to check if the Field value can be bound.
     * <p/>
     * <pre class="prettyprint">
     * public void onInit() {
     *     Form form = new Form("form");
     *     Select select = new Select("select");
     *     select.setAttribute("onchange", "Click.submit(form, false)");
     *
     *     // Bind the select Field request value
     *     ClickUtils.bind(select);
     *
     *     if (select.getValue() == COMPANY) {
     *         form.add(new TextField("companyName"));
     *     } else {
     *         form.add(new TextField("fullname"));
     *         form.add(new TextField("age"));
     *     }
     * } </pre>
     *
     * @param field the Field to bind
     */
    public static void bind(Field field) {
        Context context = Context.getThreadLocalContext();
        if (canBind(field, context)) {
            bindField(field, context);
        }
    }

    /**
     * A helper method that binds the submitted request value to the Link's
     * value. See {@link #bind(org.openidentityplatform.openam.click.control.Field)} for a detailed
     * description.
     * <p/>
     * This method delegates to
     * {@link #canBind(org.openidentityplatform.openam.click.Control, org.openidentityplatform.openam.click.Context)}
     * to check if the Link value can be bound.
     *
     * @param link the AbstractLink to bind
     */
    public static void bind(AbstractLink link) {
        Context context = Context.getThreadLocalContext();
        if (canBind(link, context)) {
            link.bindRequestValue();
        }
    }

    /**
     * A helper method that binds the submitted request values of all Fields
     * and Links inside the given container or child containers. See
     * {@link #bind(org.openidentityplatform.openam.click.control.Field)} for a detailed description.
     * <p/>
     * This method delegates to
     * {@link #canBind(org.openidentityplatform.openam.click.Control, org.openidentityplatform.openam.click.Context)}
     * to check if the Container Fields and Links can be bound.
     * <p/>
     * Below is an example to bind Form Field's during the onInit event:
     *
     * <pre class="prettyprint">
     * public void onInit() {
     *     Form form = new Form("form");
     *     Checkbox commentChk = new Checkbox("comment");
     *     Select select = new Select("select");
     *     select.setAttribute("onchange", "Click.submit(form, false)");
     *
     *     // Bind all Form Field request values
     *     ClickUtils.bind(form);
     *
     *     if (select.getValue() == COMPANY) {
     *         form.add(new TextField("companyName"));
     *     } else {
     *         form.add(new TextField("fullname"));
     *         form.add(new TextField("age"));
     *     }
     *
     *     if (commentChk.isChecked()) {
     *         form.add(new TextArea("feedback"));
     *     }
     * } </pre>
     *
     * @param container the container which Fields and Links to bind
     */
    public static void bind(Container container) {
        Context context = Context.getThreadLocalContext();
        if (canBind(container, context)) {
            bind(container, context);
        }
    }

    /**
     * A helper method that binds and validates the Field's submitted request
     * value. This method will return true if the validation succeeds, false
     * otherwise. See {@link #bind(org.openidentityplatform.openam.click.control.Field)} for a
     * detailed description.
     * <p/>
     * This method delegates to
     * {@link #canBind(org.openidentityplatform.openam.click.Control, org.openidentityplatform.openam.click.Context)}
     * to check if the Field value can be bound and validated.
     * <p/>
     * <b>Please note</b>: this method won't bind and validate disabled fields,
     * unless the field has an incoming request parameter matching its name.
     * If an incoming request parameter is present, this method will switch off
     * the Field's disabled property.
     * <p/>
     * <pre class="prettyprint">
     * public void onInit() {
     *     Form form = new Form("form");
     *     Select select = new Select("select", true);
     *     select.addOption(Option.EMPTY_OPTION);
     *
     *     select.setAttribute("onchange", "Click.submit(form, false)");
     *
     *     // Bind the Field request value and validate it before continuing
     *     if (ClickUtils.bindAndValidate(select)) {
     *         if (select.getValue() == COMPANY) {
     *             form.add(new TextField("companyName"));
     *         } else {
     *             form.add(new TextField("fullname"));
     *             form.add(new TextField("age"));
     *         }
     *     }
     * } </pre>
     *
     * @param field the Field to bind and validate
     * @return true if field was bound and valid, or false otherwise
     */
    public static boolean bindAndValidate(Field field) {
        Context context = Context.getThreadLocalContext();
        if (canBind(field, context)) {
            return bindAndValidate(field, context);
        }
        return false;
    }

    /**
     * A helper method that binds and validates the submitted request values
     * of all Fields and Links inside the given container or child containers.
     * This method will return true if the validation succeeds, false
     * otherwise.
     * <p/>
     * See {@link #bindAndValidate(org.openidentityplatform.openam.click.control.Form)} for a
     * detailed description.
     *
     * @param container the container which Fields and Links to bind and
     * validate
     * @return true if all Fields are valid, false otherwise
     */
    public static boolean bindAndValidate(Container container) {
        Context context = Context.getThreadLocalContext();
        if (canBind(container, context)) {
            return bindAndValidate(container, context);
        }
        return false;
    }

    /**
     * * A helper method that binds and validates the submitted request values
     * of all Fields and Links inside the given Form or child containers. Note,
     * the Form itself is also validated.
     * <p/>
     * This method will return true if the validation succeeds, false otherwise.
     * See {@link #bind(org.openidentityplatform.openam.click.control.Field)} for a detailed
     * description.
     * <p/>
     * This method delegates to
     * {@link #canBind(org.openidentityplatform.openam.click.Control, org.openidentityplatform.openam.click.Context)}
     * to check if the Form Fields and Links can be bound and validated.
     *
     * <pre class="prettyprint">
     * public void onInit() {
     *     Form form = new Form("form");
     *     Checkbox commentChk = new Checkbox("comment");
     *     Select select = new Select("select", true);
     *     select.addOption(Option.EMPTY_OPTION);
     *
     *     select.setAttribute("onchange", "Click.submit(form, false)");
     *
     *     // Bind all Form field request values and validate it before continuing
     *     if (ClickUtils.bindAndValidate(form)) {
     *
     *         if (select.getValue() == COMPANY) {
     *             form.add(new TextField("companyName"));
     *         } else {
     *             form.add(new TextField("fullname"));
     *             form.add(new TextField("age"));
     *         }
     *
     *         if (commentChk.isChecked()) {
     *             form.add(new TextArea("feedback"));
     *         }
     *     }
     * } </pre>
     *
     * @param form the form which Fields and Links to bind and validate
     * @return true if the form, it's fields and links was bound and valid, false
     * otherwise
     */
    public static boolean bindAndValidate(Form form) {
        Context context = Context.getThreadLocalContext();
        if (canBind(form, context)) {
            return bindAndValidate(form, context);
        }
        return false;
    }

    /**
     * Return a new XML Document for the given input stream.
     *
     * @param inputStream the input stream
     * @return new XML Document
     * @throws RuntimeException if a parsing error occurs
     */
    public static Document buildDocument(InputStream inputStream) {
        return buildDocument(inputStream, null);
    }

    /**
     * Return a new XML Document for the given input stream and XML entity
     * resolver.
     *
     * @param inputStream the input stream
     * @param entityResolver the XML entity resolver
     * @return new XML Document
     * @throws RuntimeException if a parsing error occurs
     */
    public static Document buildDocument(InputStream inputStream,
                                         EntityResolver entityResolver) {
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            DocumentBuilder builder = factory.newDocumentBuilder();

            if (entityResolver != null) {
                builder.setEntityResolver(entityResolver);
            }

            return builder.parse(inputStream);

        } catch (Exception ex) {
            throw new RuntimeException("Error parsing XML", ex);
        }
    }

    /**
     * Return true if the given control's request value can be bound, false
     * otherwise.
     * <p/>
     * The following algorithm is used to determine if the Control can be
     * bound to a request value or not.
     * <ul>
     * <li>return false if the request is a forward.
     * See {@link org.apache.click.Context#isForward()}</li>
     * <li>return true if the request is an Ajax request.
     * See {@link org.apache.click.Context#isAjaxRequest()}</li>
     * <li>return true if the control has no parent Form</li>
     * <li>return true if the control's parent Form was submitted, false otherwise.
     * See {@link org.apache.click.control.Form#isFormSubmission()}</li>
     * </ul>
     *
     * @param control the control to check if it can be bound or not
     * @param context the request context
     * @return true if the given control request value be bound, false otherwise
     */
    public static boolean canBind(Control control, Context context) {

        if (context.isForward()) {
            return false;
        }

        // This can cause issue with two fields inside a form with the same name.
        if (context.isAjaxRequest()) {
            return true;
        }

        Form form = ContainerUtils.findForm(control);
        if (form == null && control instanceof Form) {
            form = (Form) control;
        }
        if (form != null) {
            return form.isFormSubmission();
        }
        return true;
    }

    /**
     * Returns the <code>Class</code> object associated with the class or
     * interface with the given string name, using the current Thread context
     * class loader.
     *
     * @param classname the name of the class to load
     * @return the <tt>Class</tt> object
     * @throws ClassNotFoundException if the class cannot be located
     */
    public static Class classForName(String classname)
            throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return Class.forName(classname, true, classLoader);
    }

    /**
     * Close the given closeable (Reader, Writer, Stream) and ignore any
     * exceptions thrown.
     *
     * @param closeable the closeable (Reader, Writer, Stream) to close.
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ioe) {
                // Ignore
            }
        }
    }

    /**
     * Creates a template model of key/value pairs which can be used by template
     * engines such as Velocity and Freemarker.
     * <p/>
     * The following objects will be added to the model:
     * <ul>
     * <li>the Page {@link org.openidentityplatform.openam.click.Page#model model} Map key/value
     * pairs
     * </li>
     * <li>context - the Servlet context path, e.g. <span class="">/mycorp</span>
     * </li>
     * <li>format - the Page {@link Format} object for formatting the display
     * of objects.
     * </li>
     * <li>messages - the {@link MessagesMap} adaptor for the
     * {@link org.apache.click.Page#getMessages()} method.
     * </li>
     * <li>path - the {@link org.openidentityplatform.openam.click.Page#path path} of the <tt>page</tt>
     * template.
     * </li>
     * <li>request - the page {@link jakarta.servlet.http.HttpServletRequest}
     * object.
     * </li>
     * <li>response - the page {@link jakarta.servlet.http.HttpServletResponse}
     * object.
     * </li>
     * <li>session - the {@link SessionMap} adaptor for the users
     * {@link jakarta.servlet.http.HttpSession}.
     * </li>
     * </ul>
     *
     * @param page the page to populate the template model from
     * @param context the request context
     * @return a template model as a map
     */
    public static Map<String, Object> createTemplateModel(final Page page, Context context) {

        ConfigService configService = ClickUtils.getConfigService(context.getServletContext());
        LogService logger = configService.getLogService();

        final Map<String, Object> model = new HashMap<String, Object>(page.getModel());

        final HttpServletRequest request = context.getRequest();

        Object pop = model.put("request", request);
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"request\". The page model object "
                    + pop + " has been replaced with the request object";
            logger.warn(msg);
        }

        pop = model.put("response", context.getResponse());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"response\". The page model object "
                    + pop + " has been replaced with the response object";
            logger.warn(msg);
        }

        SessionMap sessionMap = new SessionMap(request.getSession(false));
        pop = model.put("session", sessionMap);
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"session\". The page model object "
                    + pop + " has been replaced with the request "
                    + " session";
            logger.warn(msg);
        }

        pop = model.put("context", request.getContextPath());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"context\". The page model object "
                    + pop + " has been replaced with the request "
                    + " context path";
            logger.warn(msg);
        }

        Format format = page.getFormat();
        if (format != null) {
            pop = model.put("format", format);
            if (pop != null && !page.isStateful()) {
                String msg = page.getClass().getName() + " on "
                        + page.getPath()
                        + " model contains an object keyed with reserved "
                        + "name \"format\". The page model object " + pop
                        + " has been replaced with the format object";
                logger.warn(msg);
            }
        }

        String path = page.getPath();
        if (path != null) {
            pop = model.put("path", path);
            if (pop != null && !page.isStateful()) {
                String msg = page.getClass().getName() + " on "
                        + page.getPath()
                        + " model contains an object keyed with reserved "
                        + "name \"path\". The page model object " + pop
                        + " has been replaced with the page path";
                logger.warn(msg);
            }
        }

        pop = model.put("messages", page.getMessages());
        if (pop != null && !page.isStateful()) {
            String msg = page.getClass().getName() + " on " + page.getPath()
                    + " model contains an object keyed with reserved "
                    + "name \"messages\". The page model object "
                    + pop + " has been replaced with the request "
                    + " messages";
            logger.warn(msg);
        }

        return model;
    }

    /**
     * Invalidate the specified cookie and delete it from the response object.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param cookieName The name of the cookie you want to delete
     * @param path of the path the cookie you want to delete
     */
    public static void invalidateCookie(HttpServletRequest request,
                                        HttpServletResponse response, String cookieName, String path) {

        setCookie(request, response, cookieName, null, 0, path);
    }

    /**
     * Return true is this is an Ajax request, false otherwise.
     * <p/>
     * An Ajax request is identified by the presence of the request <tt>header</tt>
     * or request <tt>parameter</tt>: "<tt>X-Requested-With</tt>".
     * "<tt>X-Requested-With</tt>" is the de-facto standard identifier used by
     * Ajax libraries.
     * <p/>
     * <b>Note:</b> incoming requests that contains a request <tt>parameter</tt>
     * "<tt>X-Requested-With</tt>" will result in this method returning true, even
     * though the request itself was not initiated through a <tt>XmlHttpRequest</tt>
     * object. This allows one to programmatically enable Ajax requests. A common
     * use case for this feature is when uploading files through an IFrame element.
     * By specifying "<tt>X-Requested-With</tt>" as a request parameter the IFrame
     * request will be handled like a normal Ajax request.
     *
     * @param request the servlet request
     * @return true if this is an Ajax request, false otherwise
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        return request.getHeader(X_REQUESTED_WITH) != null
                || request.getParameter(X_REQUESTED_WITH) != null;
    }

    /**
     * Return true if the request is a multi-part content type POST request.
     *
     * @param request the page servlet request
     * @return true if the request is a multi-part content type POST request
     */
    public static boolean isMultipartRequest(HttpServletRequest request) {
        //return ServletFileUpload.isMultipartContent(request);
        //throw new RuntimeException("not implemented");
        return false;
    }

    /**
     * Invalidate the specified cookie and delete it from the response object. Deletes only cookies mapped
     * against the root "/" path. Otherwise use
     * {@link #invalidateCookie(HttpServletRequest, HttpServletResponse, String, String)}
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @see #invalidateCookie(HttpServletRequest, HttpServletResponse, String, String)
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param cookieName The name of the cookie you want to delete.
     */
    public static void invalidateCookie(HttpServletRequest request,
                                        HttpServletResponse response, String cookieName) {

        invalidateCookie(request, response, cookieName, "/");
    }

    /**
     * Return a resource bundle using the specified base name.
     *
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @return a resource bundle for the given base name
     * @throws MissingResourceException if no resource bundle for the specified base name can be found
     */
    public static ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, Locale.getDefault());
    }

    /**
     * Return a resource bundle using the specified base name and locale.
     *
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param locale the locale for which a resource bundle is desired
     * @return a resource bundle for the given base name and locale
     * @throws MissingResourceException if no resource bundle for the specified base name can be found
     */
    public static ResourceBundle getBundle(String baseName, Locale locale) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return ResourceBundle.getBundle(baseName, locale, classLoader);
    }

    /**
     * Return the first XML child Element for the given parent Element and child
     * Element name.
     *
     * @param parent the parent element to get the child from
     * @param name the name of the child element
     * @return the first child element for the given name and parent
     */
    public static Element getChild(Element parent, String name) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals(name)) {
                    return (Element) node;
                }
            }
        }
        return null;
    }

    /**
     * Return the list of XML child Element elements with the given name from
     * the given parent Element.
     *
     * @param parent the parent element to get the child from
     * @param name the name of the child element
     * @return the list of XML child elements for the given name
     */
    public static List<Element> getChildren(Element parent, String name) {
        List<Element> list = new ArrayList<Element>();
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals(name)) {
                    list.add((Element) node);
                }
            }
        }
        return list;
    }

    /**
     * Return the InputStream for the Click configuration file <tt>click.xml</tt>.
     * This method will first lookup the <tt>click.xml</tt> under the
     * applications <tt>WEB-INF</tt> directory, and then if not found it will
     * attempt to find the configuration file on the classpath root.
     *
     * @param servletContext the servlet context to obtain the Click configuration
     *     from
     * @return the InputStream for the Click configuration file
     * @throws RuntimeException if the resource could not be found
     */
    public static InputStream getClickConfig(ServletContext servletContext) {
        InputStream inputStream =
                servletContext.getResourceAsStream(DEFAULT_APP_CONFIG);

        if (inputStream == null) {
            inputStream = org.apache.click.util.ClickUtils.getResourceAsStream("/click.xml", org.apache.click.util.ClickUtils.class);
            if (inputStream == null) {
                String msg =
                        "could not find click app configuration file: "
                                + DEFAULT_APP_CONFIG + " or click.xml on classpath";
                throw new RuntimeException(msg);
            }
        }

        return inputStream;
    }

    /**
     * Return the application configuration service instance from the given
     * servlet context.
     *
     * @param servletContext the servlet context to get the config service instance
     * @return the application config service instance
     */
    public static ConfigService getConfigService(ServletContext servletContext) {
        ConfigService configService = (ConfigService)
                servletContext.getAttribute(ConfigService.CONTEXT_NAME);

        if (configService != null) {
            return configService;

        } else {
            String msg =
                    "could not find ConfigService in the SerlvetContext under the"
                            + " name '" + ConfigService.CONTEXT_NAME + "'.\nThis can occur"
                            + " if ClickUtils.getConfigService() is called before"
                            + " ClickServlet is initialized by the servlet container.\n"
                            + "To fix ensure that ClickServlet is loaded at startup by"
                            + " editing your web.xml and setting the load-on-startup to 0:\n\n"
                            + " <servlet>\n"
                            + "   <servlet-name>ClickServlet</servlet-name>\n"
                            + "   <servlet-class>org.apache.click.ClickServlet</servlet-class>\n"
                            + "   <load-on-startup>0</load-on-startup>\n"
                            + " </servlet>\n";

            throw new RuntimeException(msg);
        }
    }

    /**
     * Returns the specified Cookie object, or null if the cookie does not exist.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param request the servlet request
     * @param name the name of the cookie
     * @return the Cookie object if it exists, otherwise null
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie cookies[] = request.getCookies();

        if (cookies == null || name == null || name.length() == 0) {
            return null;
        }

        //Otherwise, we have to do a linear scan for the cookie.
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }

        return null;
    }

    /**
     * Sets the given cookie values in the servlet response.
     * <p/>
     * This will also put the cookie in a list of cookies to send with this request's response
     * (so that in case of a redirect occurring down the chain, the first filter
     * will always try to set this cookie again)
     * <p/>
     * The cookie secure flag is set if the request is secure.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param name the cookie name
     * @param value the cookie value
     * @param maxAge the maximum age of the cookie in seconds. A negative
     * value will expire the cookie at the end of the session, while 0 will delete
     * the cookie.
     * @param path the cookie path
     * @return the Cookie object created and set in the response
     */
    public static Cookie setCookie(HttpServletRequest request, HttpServletResponse response,
                                   String name, String value, int maxAge, String path) {

        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);

        return cookie;
    }

    /**
     * Returns the value of the specified cookie as a String. If the cookie
     * does not exist, the method returns null.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param request the servlet request
     * @param name the name of the cookie
     * @return the value of the cookie, or null if the cookie does not exist.
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie cookie = getCookie(request, name);

        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }

    /**
     * Return the Click Framework version string.
     *
     * @return the Click Framework version string
     */
    public static String getClickVersion() {
        ResourceBundle bundle = getBundle("click-control");
        return bundle.getString("click-version");
    }

    /**
     * Return the web application version string.
     *
     * @return the web application version string
     */
    public static String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * Set the web application version string.
     *
     * @param applicationVersion the web application version string
     */
    public static void setApplicationVersion(String applicationVersion) {
        ClickUtils.applicationVersion = applicationVersion;
        cachedApplicationVersionIndicator = null;
    }

    /**
     * Return Click's version indicator for static web resources
     * (eg css, js and image files) if resource versioning is active,
     * otherwise this method will return an empty string.
     * <p/>
     * Click's resource versioning becomes active under the following
     * conditions:
     * <ul>
     * <li>the {@link #ENABLE_RESOURCE_VERSION} request attribute must be set
     * to <tt>true</tt></li>
     * <li>the application mode must be either "production" or "profile"</li>
     * </ul>
     *
     * The version indicator is based on the current Click release version.
     * For example when using Click 1.4 this method will return the string
     * <tt>"_1.4"</tt>.
     *
     * @param context the request context
     * @return a version indicator for web resources
     */
    public static String getResourceVersionIndicator(Context context) {
        if (cachedResourceVersionIndicator != null) {
            return cachedResourceVersionIndicator;
        }

        ConfigService configService = getConfigService(context.getServletContext());

        boolean isProductionModes = configService.isProductionMode()
                || configService.isProfileMode();

        if (isProductionModes
                && isEnableResourceVersion(context)) {

            cachedResourceVersionIndicator = RESOURCE_VERSION_INDICATOR;
            return cachedResourceVersionIndicator;

        } else {
            return "";
        }
    }

    /**
     * If resource versioning is active this method will return the
     * application version indicator for static web resources
     * (eg JavaScript and Css) otherwise this method will return an empty string.
     * <p/>
     * Application resource versioning becomes active under the following
     * conditions:
     * <ul>
     * <li>the {@link #ENABLE_RESOURCE_VERSION} request attribute must be set
     * to <tt>true</tt></li>
     * <li>the application mode must be either "production" or "profile"</li>
     * </ul>
     *
     * The version indicator is based on the application version.
     * For example if the application version is 1.2 this method will
     * return the string <tt>"_1.2"</tt>.
     * <p/>
     * The application version can be set through the static method
     * {@link #setApplicationVersion(java.lang.String)}.
     *
     * @return an application version indicator for web resources
     */
    public static String getApplicationResourceVersionIndicator() {
        // Return the cached version first
        if (cachedApplicationVersionIndicator != null) {
            return cachedApplicationVersionIndicator;
        }

        // Check if the Context has been set
        if (Context.hasThreadLocalContext()) {

            Context context = Context.getThreadLocalContext();
            ConfigService configService = ClickUtils.getConfigService(context.getServletContext());

            boolean isProductionModes = configService.isProductionMode()
                    || configService.isProfileMode();

            if (isProductionModes && ClickUtils.isEnableResourceVersion(context)) {
                String version = getApplicationVersion();
                if (StringUtils.isNotBlank(version)) {
                    cachedApplicationVersionIndicator = VERSION_INDICATOR_SEP
                            + version;
                    return cachedApplicationVersionIndicator;
                }
            }
        }
        return "";
    }

    /**
     * Return the given control CSS selector or null if no selector can be found.
     * <p/>
     * <b>Please note:</b> it is highly recommended to set a control's ID
     * attribute when dealing with Ajax requests.
     * <p/>
     * The CSS selector is calculated as follows:
     * <ol>
     *   <li>if control.getId() is set, prepend it with a '#' char
     *       and return the value. An example selector will be: <tt>#field-id</tt>
     *   </li>
     *
     *   <li>if control.getName() is set do the following:
     *     <ol>
     *       <li>if the control is of type {@link org.apache.click.control.ActionLink},
     *       it's "<tt>class</tt>" attribute selector will be returned. For example:
     *       <tt>a[class=red]</tt>. <b>Please note:</b> if the link class attribute is
     *       not set, the class attribute will be set to its name, prefixed with
     *       a dash, '-'. For example: <tt>a[class=-my-link]</tt>.
     *       </li>
     *
     *       <li>if the control is not an ActionLink, it is assumed the control
     *       will render its "<tt>name</tt>" attribute and the name attribute
     *       selector will be returned. For example: <tt>input[name=my-button]</tt>.
     *       </li>
     *     </ol>
     *   </li>
     *
     *   <li>otherwise return null.
     *   </li>
     * </ol>
     *
     * @param control the control which CSS selector to return
     * @return the control CSS selector or null if no selector can be found
     * @throws IllegalArgumentException if control is null
     */
    public static String getCssSelector(Control control) {
        if (control == null) {
            throw new IllegalArgumentException("Control cannot be null");
        }

        String id = control.getId();
        String name = control.getName();
        String cssSelector = null;

        if (StringUtils.isNotBlank(id)) {
            cssSelector = '#' + id;
        } else if (StringUtils.isNotBlank(name)) {
            String tag = null;

            // Try and create a more specific selector by retrieving the
            // control's tag
            if (control instanceof AbstractControl) {
                tag = StringUtils.defaultString(((AbstractControl) control).getTag());
            }

            HtmlStringBuffer buffer = new HtmlStringBuffer(20);

            // Handle ActionLink (perhaps other link controls too?) differently
            // as it doesn't render the "name" attribute. The "name" attribute
            // is used by links for bookmarking purposes. Instead set the class
            // attribute to the link's name and use that as the selector.
            if (control instanceof ActionLink) {
                ActionLink link = (ActionLink) control;
                if (!link.hasAttribute("class")) {
                    link.setAttribute("class", '-' + name);
                }
                buffer.append(tag).append("[class*=");
                buffer.append(link.getAttribute("class")).append("]");

            } else {
                buffer.append(tag).append("[name=");
                buffer.append(name).append("]");
            }
            cssSelector = buffer.toString();
        }
        return cssSelector;
    }

    /**
     * Populate the given object's attributes with the Form's field values.
     * <p/>
     * The specified Object can either be a POJO (plain old java object) or
     * a {@link java.util.Map}. If a POJO is specified, its attributes are
     * populated from  matching form fields. If a map is specified, its
     * key/value pairs are populated from matching form fields.
     *
     * @param form the Form to obtain field values from
     * @param object the object to populate with field values
     * @param debug log debug statements when populating the object
     */
    public static void copyFormToObject(Form form, Object object,
                                        boolean debug) {

        ContainerUtils.copyContainerToObject(form, object);
    }

    /**
     * Populate the given Form field values with the object's attributes.
     * <p/>
     * The specified Object can either be a POJO (plain old java object) or
     * a {@link java.util.Map}. If a POJO is specified, its attributes are
     * copied to matching form fields. If a map is specified, its key/value
     * pairs are copied to matching form fields.
     *
     * @param object the object to obtain attribute values from
     * @param form the Form to populate
     * @param debug log debug statements when populating the form
     */
    public static void copyObjectToForm(Object object, Form form,
                                        boolean debug) {

        ContainerUtils.copyObjectToContainer(object, form);
    }

    /**
     * Deploy the specified classpath resource to the given target directory
     * under the web application root directory.
     * <p/>
     * This method will <b>not</b> override any existing resources found in the
     * target directory.
     * <p/>
     * If an IOException or SecurityException occurs this method will log a
     * warning message.
     *
     * @param servletContext the web applications servlet context
     * @param resource the classpath resource name
     * @param targetDir the target directory to deploy the resource to
     */
    public static void deployFile(ServletContext servletContext,
                                  String resource, String targetDir) {

        if (servletContext == null) {
            throw new IllegalArgumentException("Null servletContext parameter");
        }

        if (StringUtils.isBlank(resource)) {
            String msg = "Null resource parameter not defined";
            throw new IllegalArgumentException(msg);
        }

        String realTargetDir = servletContext.getRealPath("/") + File.separator;

        if (StringUtils.isNotBlank(targetDir)) {
            realTargetDir = realTargetDir + targetDir;
        }


        LogService logger = getConfigService(servletContext).getLogService();

        try {

            // Create files deployment directory
            File directory = new File(realTargetDir);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    String msg =
                            "could not create deployment directory: " + directory;
                    throw new IOException(msg);
                }
            }

            String destination = resource;
            int index = resource.lastIndexOf('/');
            if (index != -1) {
                destination = resource.substring(index + 1);
            }

            destination = realTargetDir + File.separator + destination;

            File destinationFile = new File(destination);

            if (!destinationFile.exists()) {
                InputStream inputStream =
                        getResourceAsStream(resource, org.apache.click.util.ClickUtils.class);

                if (inputStream != null) {
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(destinationFile);
                        byte[] buffer = new byte[1024];
                        while (true) {
                            int length = inputStream.read(buffer);
                            if (length <  0) {
                                break;
                            }
                            fos.write(buffer, 0, length);
                        }

                        if (logger.isTraceEnabled()) {
                            int lastIndex =
                                    destination.lastIndexOf(File.separatorChar);
                            if (lastIndex != -1) {
                                destination =
                                        destination.substring(lastIndex + 1);
                            }
                            String msg =
                                    "deployed " + targetDir + "/" + destination;
                            logger.trace(msg);
                        }

                    } finally {
                        close(fos);
                        close(inputStream);
                    }
                } else {
                    String msg =
                            "could not locate classpath resource: " + resource;
                    throw new IOException(msg);
                }
            }

        } catch (IOException ioe) {
            String msg =
                    "error occurred deploying resource " + resource
                            + ", error " + ioe;
            logger.warn(msg);

        } catch (SecurityException se) {
            String msg =
                    "error occurred deploying resource " + resource
                            + ", error " + se;
            logger.warn(msg);
        }
    }

    /**
     * Deploy the specified classpath resources to the given target directory
     * under the web application root directory.
     *
     * @param servletContext the web applications servlet context
     * @param resources the array of classpath resource names
     * @param targetDir the target directory to deploy the resource to
     */
    public static void deployFiles(ServletContext servletContext,
                                   String[] resources, String targetDir) {

        if (resources == null) {
            throw new IllegalArgumentException("Null resources parameter");
        }

        for (String resource : resources) {
            ClickUtils.deployFile(servletContext, resource, targetDir);
        }
    }

    /**
     * Deploys required files (from a file list) for a control that repsects a specific convention.
     * <p/>
     * <b>Convention:</b>
     * <p/>
     * There's a descriptor file generated by the <code>tools/standalone/dev-tasks/ListFilesTask</code>.
     * The files to deploy are all in a subdirectory placed in the same directory with the control.
     * See documentation for more details. <p/>
     *
     * <b>Usage:</b><p/>
     * In your Control simply use the code below, and everything should work automatically.
     * <pre class="prettyprint">
     * public void onDeploy(ServletContext servletContext) {
     *    ClickUtils.deployFileList(servletContext, HeavyControl.class, "click");
     * } </pre>
     *
     * @param servletContext the web applications servlet context
     * @param controlClass the class of the Control that has files for deployment
     * @param targetDir target directory where to deploy the files to. In most cases this
     * is only the reserved directory <code>click</code>
     */
    public static void deployFileList(ServletContext servletContext,
                                      Class<? extends Control> controlClass, String targetDir) {

        String packageName = ClassUtils.getPackageName(controlClass);
        packageName = StringUtils.replaceChars(packageName, '.', '/');
        packageName = "/" + packageName;
        String controlName = ClassUtils.getShortClassName(controlClass);

        ConfigService configService = getConfigService(servletContext);
        LogService logService = configService.getLogService();
        String descriptorFile = packageName + "/" + controlName + ".files";
        logService.debug("Use deployment descriptor file:" + descriptorFile);

        InputStream is = getResourceAsStream(descriptorFile, org.apache.click.util.ClickUtils.class);
        List fileList = IOUtils.readLines(is);
        if (fileList == null || fileList.isEmpty()) {
            logService.info("there are no files to deploy for control " + controlClass.getName());
            return;
        }

        // a target dir list is required cause the ClickUtils.deployFile() is too inflexible to autodetect
        // required subdirectories.
        List<String> targetDirList = new ArrayList<String>(fileList.size());
        for (int i = 0; i < fileList.size(); i++) {
            String filePath = (String) fileList.get(i);
            String destination = "";
            int index = filePath.lastIndexOf('/');
            if (index != -1) {
                destination = filePath.substring(0, index + 1);
            }
            targetDirList.add(i, targetDir + "/" + destination);
            fileList.set(i, packageName + "/" + filePath);
        }

        for (int i = 0; i < fileList.size(); i++) {
            String source = (String) fileList.get(i);
            String targetDirName = targetDirList.get(i);
            ClickUtils.deployFile(servletContext, source, targetDirName);
        }

    }

    /**
     * Return an encoded version of the <tt>Serializable</tt> object. The object
     * will be serialized, compressed and Base 64 encoded.
     *
     * @param object the object to encode
     * @return a serialized, compressed and Base 64 string encoding of the
     * given object
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the object parameter is null, or if
     *      the object is not Serializable
     */
    public static String encode(Object object) throws IOException {
        if (object == null) {
            throw new IllegalArgumentException("null object parameter");
        }
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("parameter not Serializable");
        }

        ByteArrayOutputStream bos = null;
        GZIPOutputStream gos = null;
        ObjectOutputStream oos = null;

        try {
            bos = new ByteArrayOutputStream();
            gos = new GZIPOutputStream(bos);
            oos = new ObjectOutputStream(gos);

            oos.writeObject(object);

        } finally {
            close(oos);
            close(gos);
            close(bos);
        }

        Base64 base64 = new Base64();

        try {
            byte[] byteData = base64.encode(bos.toByteArray());

            return new String(byteData);

        } catch (Throwable t) {
            String message =
                    "error occurred Base64 encoding: " + object + " : " + t;
            throw new IOException(message);
        }
    }

    /**
     * Return an object from the {@link #encode(Object)} string.
     *
     * @param string the encoded string
     * @return an object from the encoded
     * @throws ClassNotFoundException if the class could not be instantiated
     * @throws IOException if an data I/O error occurs
     */
    public static Object decode(String string)
            throws ClassNotFoundException, IOException {

        Base64 base64 = new Base64();
        byte[] byteData = null;

        try {
            byteData = base64.decode(string.getBytes());

        } catch (Throwable t) {
            String message =
                    "error occurred Base64 decoding: " + string + " : " + t;
            throw new IOException(message);
        }

        ByteArrayInputStream bis = null;
        GZIPInputStream gis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(byteData);
            gis = new GZIPInputStream(bis);
            ois = new ObjectInputStream(gis);

            return ois.readObject();

        } finally {
            close(ois);
            close(gis);
            close(bis);
        }
    }

    /**
     * Builds a cookie string containing a username and password.
     * <p/>
     * Note: with open source this is not really secure, but it prevents users
     * from snooping the cookie file of others and by changing the XOR mask and
     * character offsets, you can easily tweak results.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param username the username
     * @param password the password
     * @param xorMask the XOR mask to encrypt the value with, must be same as
     *      as the value used to decrypt the cookie password
     * @return String encoding the input parameters, an empty string if one of
     *      the arguments equals <code>null</code>
     */
    public static String encodePasswordCookie(String username, String password, int xorMask) {
        String encoding = new String(new char[]{DELIMITER, ENCODE_CHAR_OFFSET1, ENCODE_CHAR_OFFSET2});

        return encodePasswordCookie(username, password, encoding, xorMask);
    }

    /**
     * Builds a cookie string containing a username and password, using offsets
     * to customize the encoding.
     * <p/>
     * Note: with open source this is not really secure, but it prevents users
     * from snooping the cookie file of others and by changing the XOR mask and
     * character offsets, you can easily tweak results.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param username the username
     * @param password the password
     * @param encoding a String used to customize cookie encoding (only the first 3 characters are used)
     * @param xorMask the XOR mask to encrypt the value with, must be same as
     *      as the value used to decrypt the cookie password
     * @return String encoding the input parameters, an empty string if one of
     *      the arguments equals <code>null</code>.
     */
    public static String encodePasswordCookie(String username, String password, String encoding, int xorMask) {
        StringBuilder buf = new StringBuilder();

        if (username != null && password != null) {

            char offset1 = (encoding != null && encoding.length() > 1)
                    ? encoding.charAt(1) : ENCODE_CHAR_OFFSET1;

            char offset2 = (encoding != null && encoding.length() > 2)
                    ? encoding.charAt(2) : ENCODE_CHAR_OFFSET2;

            byte[] bytes = (username + DELIMITER + password).getBytes();
            int b;

            for (int n = 0; n < bytes.length; n++) {
                b = bytes[n] ^ (xorMask + n);
                buf.append((char) (offset1 + (b & 0x0F)));
                buf.append((char) (offset2 + ((b >> 4) & 0x0F)));
            }
        }

        return buf.toString();
    }

    /**
     * Decodes a cookie string containing a username and password.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param cookieVal the encoded cookie username and password value
     * @param xorMask the XOR mask to decrypt the value with, must be same as
     *      as the value used to encrypt the cookie password
     * @return String[] containing the username at index 0 and the password at
     *      index 1, or <code>{ null, null }</code> if cookieVal equals
     *      <code>null</code> or the empty string.
     */
    public static String[] decodePasswordCookie(String cookieVal, int xorMask) {
        String encoding = new String(new char[]{DELIMITER, ENCODE_CHAR_OFFSET1, ENCODE_CHAR_OFFSET2});

        return decodePasswordCookie(cookieVal, encoding, xorMask);
    }

    /**
     * Decodes a cookie string containing a username and password.
     * <p/>
     * This method was derived from Atlassian <tt>CookieUtils</tt> method of
     * the same name, release under the BSD License.
     *
     * @param cookieVal the encoded cookie username and password value
     * @param encoding  a String used to customize cookie encoding (only the first 3 characters are used)
     *      - should be the same string you used to encode the cookie!
     * @param xorMask the XOR mask to decrypt the value with, must be same as
     *      as the value used to encrypt the cookie password
     * @return String[] containing the username at index 0 and the password at
     *      index 1, or <code>{ null, null }</code> if cookieVal equals
     *      <code>null</code> or the empty string.
     */
    public static String[] decodePasswordCookie(String cookieVal, String encoding,
                                                int xorMask) {

        // Check that the cookie value isn't null or zero-length
        if (cookieVal == null || cookieVal.length() <= 0) {
            return null;
        }

        char offset1 = (encoding != null && encoding.length() > 1)
                ? encoding.charAt(1) : ENCODE_CHAR_OFFSET1;

        char offset2 = (encoding != null && encoding.length() > 2)
                ? encoding.charAt(2) : ENCODE_CHAR_OFFSET2;

        // Decode the cookie value
        char[] chars = cookieVal.toCharArray();
        byte[] bytes = new byte[chars.length / 2];
        int b;

        for (int n = 0, m = 0; n < bytes.length; n++) {
            b = chars[m++] - offset1;
            b |= (chars[m++] - offset2) << 4;
            bytes[n] = (byte) (b ^ (xorMask + n));
        }

        cookieVal = new String(bytes);
        int pos = cookieVal.indexOf(DELIMITER);

        String username = (pos < 0) ? "" : cookieVal.substring(0, pos);
        String password = (pos < 0) ? "" : cookieVal.substring(pos + 1);

        return new String[]{username, password};
    }

    /**
     * URL encode the specified value using the "UTF-8" encoding scheme.
     * <p/>
     * For example <tt>(http://host?name=value with spaces)</tt> will become
     * <tt>(http://host?name=value+with+spaces)</tt>.
     * <p/>
     * This method uses {@link URLEncoder#encode(java.lang.String, java.lang.String)}
     * internally.
     *
     * @param value the value to encode using "UTF-8"
     * @return an encoded URL string
     */
    public static String encodeURL(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Null object parameter");
        }

        try {
            return URLEncoder.encode(value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * URL decode the specified value using the "UTF-8" encoding scheme.
     * <p/>
     * For example <tt>(http://host?name=value+with+spaces)</tt> will become
     * <tt>(http://host?name=value with spaces)</tt>.
     * <p/>
     * This method uses {@link URLDecoder#decode(java.lang.String, java.lang.String)}
     * internally.
     *
     * @param value the value to decode using "UTF-8"
     * @return an encoded URL string
     */
    public static String decodeURL(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Null object parameter");
        }

        try {
            return URLDecoder.decode(value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return an encoded URL value for the given object using the context
     * request character encoding or "UTF-8" if the request character encoding
     * is not specified.
     * <p/>
     * For example <tt>(http://host?name=value with spaces)</tt> will become
     * <tt>(http://host?name=value+with+spaces)</tt>.
     * <p/>
     * This method uses
     * {@link URLEncoder#encode(java.lang.String, java.lang.String)} internally.
     *
     * @param object the object value to encode as a URL string
     * @param context the context providing the request character encoding
     * @return an encoded URL string
     */
    public static String encodeUrl(Object object, Context context) {
        if (object == null) {
            throw new IllegalArgumentException("Null object parameter");
        }
        if (context == null) {
            throw new IllegalArgumentException("Null context parameter");
        }

        String charset = context.getRequest().getCharacterEncoding();

        try {
            if (charset == null) {
                return URLEncoder.encode(object.toString(), "UTF-8");

            } else {
                return URLEncoder.encode(object.toString(), charset);
            }

        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    /**
     * Return a HTML escaped string for the given string value.
     *
     * @param value the string value to escape
     * @return the HTML escaped string value
     */
    public static String escapeHtml(String value) {
        if (requiresHtmlEscape(value)) {

            HtmlStringBuffer buffer = new HtmlStringBuffer(value.length() * 2);

            buffer.appendHtmlEscaped(value);

            return buffer.toString();

        } else {
            return value;
        }
    }

    /**
     * Return an escaped string for the given string value. The following
     * characters are escaped: &lt;, &gt;, &quot;, &#039;, &amp;.
     *
     * @param value the string value to escape
     * @return the escaped string value
     */
    public static String escape(String value) {
        if (requiresEscape(value)) {

            HtmlStringBuffer buffer = new HtmlStringBuffer(value.length() * 2);

            buffer.appendEscaped(value);

            return buffer.toString();

        } else {
            return value;
        }
    }

    /**
     * Return true if the control has a submitted request value, false otherwise.
     *
     * @param control the control which request parameter to check
     * @return true if the control has a submitted request value, false otherwise
     */
    public static boolean hasRequestParameter(Control control) {
        Context context = Context.getThreadLocalContext();
        if (canBind(control, context)) {
            return context.hasRequestParameter(control.getName());
        }
        return false;
    }

    /**
     * Invoke the named method on the given object and return the boolean
     * result.
     *
     * @see org.apache.click.Control#setListener(Object, String)
     *
     * @param listener the object with the method to invoke
     * @param method the name of the method to invoke
     * @return true if the listener method returned true
     */
    public static boolean invokeListener(Object listener, String method) {

        Object result = invokeMethod(listener, method);

        if (result instanceof Boolean) {
            return (Boolean) result;

        } else {

            Method targetMethod = null;
            try {
                targetMethod = listener.getClass().getMethod(method);

                String msg =
                        "Invalid listener method, missing boolean return type: "
                                + targetMethod;
                throw new RuntimeException(msg);
            } catch (Exception e) {
                String msg = "Exception occurred invoking public method: " + targetMethod;
                throw new RuntimeException(msg, e);
            }
        }
    }

    /**
     * Invoke the named method on the given target and return the Object result.
     *
     * @param target the target object with the method to invoke
     * @param method the name of the method to invoke
     * @return an ActionResult instance
     */
    public static ActionResult invokeAction(Object target, String method) {

        Object result = invokeMethod(target, method);

        if (result == null || result instanceof ActionResult) {
            return (ActionResult) result;

        } else {

            Method targetMethod = null;
            try {
                targetMethod = target.getClass().getMethod(method);

                String msg =
                        "Invalid target method, missing ActionResult return type: "
                                + targetMethod;
                throw new RuntimeException(msg);
            } catch (Exception e) {
                String msg = "Exception occurred invoking public method: " + targetMethod;
                throw new RuntimeException(msg, e);
            }
        }
    }

    /**
     * Return true if static web content resource versioning is enabled.
     *
     * @param context the request context
     * @return true if static web content resource versioning is enabled
     */
    public static boolean isEnableResourceVersion(Context context) {
        return "true".equals(context.getRequestAttribute(ENABLE_RESOURCE_VERSION));
    }

    /**
     * Return the value string limited to maxlength characters. If the string
     * gets curtailed, "..." is appended to it.
     * <p/>
     * Adapted from Velocity Tools Formatter.
     *
     * @param value the string value to limit the length of
     * @param maxlength the maximum string length
     * @return a length limited string
     */
    public static String limitLength(String value, int maxlength) {
        return limitLength(value, maxlength, "...");
    }

    /**
     * Return the value string limited to maxlength characters. If the string
     * gets curtailed and the suffix parameter is appended to it.
     * <p/>
     * Adapted from Velocity Tools Formatter.
     *
     * @param value the string value to limit the length of
     * @param maxlength the maximum string length
     * @param suffix the suffix to append to the length limited string
     * @return a length limited string
     */
    public static String limitLength(String value, int maxlength, String suffix) {
        String ret = value;
        if (value.length() > maxlength) {
            ret = value.substring(0, maxlength - suffix.length()) + suffix;
        }
        return ret;
    }

    /**
     * Return the application LogService instance using thread local Context
     * to perform the lookup.
     *
     * @return the application LogService instance
     */
    public static LogService getLogService() {
        Context context = Context.getThreadLocalContext();
        ServletContext servletContext = context.getServletContext();
        ConfigService configService = getConfigService(servletContext);
        LogService logService = configService.getLogService();
        return logService;
    }

    /**
     * Return the list of Fields for the given Form, including any Fields
     * contained in FieldSets. The list of returned fields will exclude any
     * <tt>Button</tt>, <tt>FieldSet</tt> or <tt>Label</tt> fields.
     *
     * @param form the form to obtain the fields from
     * @return the list of contained form fields
     */
    public static List<Field> getFormFields(Form form) {
        if (form == null) {
            throw new IllegalArgumentException("Null form parameter");
        }
        return ContainerUtils.getInputFields(form);
    }

    /**
     * Return the mime-type or content-type for the given filename/extension.
     * <p/>
     * Example:
     * <pre class="prettyprint">
     * // Lookup mimetype for file
     * String mimeType = ClickUtils.getMimeType("hello-world.pdf");
     *
     * // Lookup mimetype for extension
     * mimeType = ClickUtils.getMimeType("pdf");
     * </pre>
     *
     * @param value the filename or extension to obtain the mime-type for
     * @return the mime-type for the given filename/extension, or null if not
     * found
     */
    public static String getMimeType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("null filename/extension parameter");
        }

        String ext = value;

        int index = value.lastIndexOf(".");
        if (index != -1) {
            ext = value.substring(index + 1);
        }

        try {
            ResourceBundle bundle = getBundle("org/apache/click/util/mime-type");

            return bundle.getString(ext.toLowerCase());

        } catch (MissingResourceException mre) {
            return null;
        }
    }

    /**
     * Return the given control's top level parent's localized messages Map.
     * <p/>
     * This method will walk up to the control's parent page object and
     * return pages messages. If the control's top level parent is a control
     * then the parent's messages map will be returned. If the top level
     * parent is not a Page or Control instance an empty map will be returned.
     *
     * @param control the control to get the parent messages Map for
     * @return the top level parent's Map of localized messages
     */
    public static Map<String, String> getParentMessages(Control control) {
        if (control == null) {
            throw new IllegalArgumentException("Null control parameter");
        }

        Object parent = control.getParent();
        if (parent == null) {
            return Collections.emptyMap();

        } else {
            while (parent != null) {
                if (parent instanceof Control) {
                    control = (Control) parent;
                    parent = control.getParent();

                    if (parent == null) {
                        return control.getMessages();
                    }

                } else if (parent instanceof Page) {
                    Page page = (Page) parent;
                    return page.getMessages();

                } else if (parent != null) {
                    // Unknown parent class
                    return Collections.emptyMap();
                }
            }
        }

        return Collections.emptyMap();
    }

    /**
     * Return the given control's top level parent's localized message for the
     * specified name.
     * <p/>
     * This method will walk up to the control's parent page object and for each
     * parent control found, look for a message of the specified name. A
     * message found in a parent control will override the message of a child
     * control.
     * <p/>
     * Given the following property files:
     * <p/>
     * MyPage.properties
     * <pre class="prettyprint">
     * myfield.label=Page </pre>
     *
     * and MyForm.properties
     * <pre class="prettyprint">
     * myfield.label=Form </pre>
     *
     * and a the following snippet:
     *
     * <pre class="prettyprint">
     * public MyPage extends Page {
     *     public void onInit() {
     *         MyForm form = new MyForm("form");
     *         TextField field = new TextField("myfield");
     *         form.add(field);
     *
     *         //1.
     *         System.out.println(ClickUtils.getParentMessage(field, "myfield.label"));
     *
     *         addControl(form);
     *
     *         //2.
     *         System.out.println(ClickUtils.getParentMessage(field, "myfield.label"));
     *     }
     * }
     * </pre>
     *
     * The first (1.) println statement above will output <tt>Form</tt> because
     * at that stage MyForm is the highest level parent of field.
     * <tt>getParentMessage</tt> will find the property <tt>myfield.label</tt>
     * in the MyForm message properties and return <tt>Form</tt>
     * <p/>
     * The second (2.) println statement will output <tt>Page</tt> as now
     * MyPage is the highest level parent. On its first pass up the hierarchy,
     * <tt>getParentMessage</tt> will find the property <tt>myfield.label</tt>
     * in the MyForm message properties and on its second pass will find the
     * same property in MyPage message properties. As MyPage is higher up the
     * hierarchy than MyForm, MyPage will override MyForm and the property value
     * will be <tt>Page</tt>.
     *
     * @param control the control to get the parent message for
     * @param name the specific property name to find
     * @return the top level parent's Map of localized messages
     */
    public static String getParentMessage(Control control, String name) {
        if (control == null) {
            throw new IllegalArgumentException("Null control parameter");
        }
        if (name == null) {
            throw new IllegalArgumentException("Null name parameter");
        }

        Object parent = control.getParent();
        if (parent == null) {
            return null;

        } else {
            String message = null;
            while (parent != null) {
                if (parent instanceof Control) {
                    control = (Control) parent;
                    if (control != null) {
                        if (control.getMessages().containsKey(name)) {
                            message = control.getMessages().get(name);
                        }
                    }

                    parent = control.getParent();
                    if (parent == null) {
                        return message;
                    }

                } else if (parent instanceof Page) {
                    Page page = (Page) parent;
                    if (page.getMessages().containsKey(name)) {
                        message = page.getMessages().get(name);
                    }
                    return message;

                } else if (parent != null) {
                    // Unknown parent class
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get the parent page of the given control or null if the control has no
     * parent. This method will walk up the control's parent hierarchy to
     * find its parent page.
     *
     * @param control the control to get the parent page from
     * @return the parent page of the control or null if the control has no
     * parent
     */
    public static Page getParentPage(Control control) {
        Object parent = control.getParent();

        while (parent != null) {
            if (parent instanceof Control) {
                control = (Control) parent;
                parent = control.getParent();

            } else if (parent instanceof Page) {
                return (Page) parent;

            } else if (parent != null) {
                throw new RuntimeException("Invalid parent class");
            }
        }

        return null;
    }

    /**
     * Return an ordered map of request parameters from the given request.
     *
     * @param request the servlet request to obtain request parameters from
     * @return the ordered map of request parameters
     */
    public static Map<String, Object> getRequestParameterMap(HttpServletRequest request) {

        TreeMap<String, Object> requestParams = new TreeMap<String, Object>();

        Enumeration<?> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement().toString();

            String[] values = request.getParameterValues(name);

            if (values.length == 1) {
                requestParams.put(name, values[0]);

            } else {
                requestParams.put(name, values);
            }
        }

        return requestParams;
    }

    /**
     * Return the page resource path from the request. For example:
     * <pre class="codeHtml">
     * <span class="blue">http://www.mycorp.com/banking/secure/login.htm</span>  ->  <span class="red">/secure/login.htm</span> </pre>
     *
     * @param request the page servlet request
     * @return the page resource path from the request
     */
    public static String getResourcePath(HttpServletRequest request) {
        // Adapted from VelocityViewServlet.handleRequest() method:

        // If we get here from RequestDispatcher.include(), getServletPath()
        // will return the original (wrong) URI requested.  The following
        // special attribute holds the correct path.  See section 8.3 of the
        // Servlet 2.3 specification.

        String path = (String)
                request.getAttribute("jakarta.servlet.include.servlet_path");

        // Also take into account the PathInfo stated on
        // SRV.4.4 Request Path Elements.
        String info = (String)
                request.getAttribute("jakarta.servlet.include.path_info");

        if (path == null) {
            path = request.getServletPath();
            info = request.getPathInfo();
        }

        if (info != null) {
            path += info;
        }

        return path;
    }

    /**
     * Return the requestURI from the request. For example:
     * <pre class="codeHtml">
     * <span class="blue">http://www.mycorp.com/banking/secure/login.htm</span>  ->  <span class="red">/banking/secure/login.htm</span> </pre>
     *
     * @param request the page servlet request
     * @return the requestURI from the request
     */
    public static String getRequestURI(HttpServletRequest request) {
        // CLK-334. Adapted from VelocityViewServlet.handleRequest() method:

        // If we get here from RequestDispatcher.include(), getServletPath()
        // will return the original (wrong) URI requested.  The following
        // special attribute holds the correct path.  See section 8.3 of the
        // Servlet 2.3 specification.

        String requestURI = (String) request.getAttribute("jakarta.servlet.include.request_uri");

        if (requestURI == null) {
            requestURI = request.getRequestURI();
        }

        if (requestURI != null && requestURI.endsWith(".jsp")) {
            requestURI = StringUtils.replace(requestURI, ".jsp", ".htm");
        }

        return requestURI;
    }

    /**
     * Finds a resource with a given name. This method returns null if no
     * resource with this name is found.
     * <p>
     * This method uses the current <tt>Thread</tt> context <tt>ClassLoader</tt> to find
     * the resource. If the resource is not found the class loader of the given
     * class is then used to find the resource.
     *
     * @param name the name of the resource
     * @param aClass the class lookup the resource against, if the resource is
     *     not found using the current <tt>Thread</tt> context <tt>ClassLoader</tt>.
     * @return the input stream of the resource if found or null otherwise
     */
    public static InputStream getResourceAsStream(String name, Class<?> aClass) {
        Validate.notNull(name, "Parameter name is null");
        Validate.notNull(aClass, "Parameter aClass is null");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        InputStream inputStream = classLoader.getResourceAsStream(name);
        if (inputStream == null) {
            inputStream = aClass.getResourceAsStream(name);
        }

        return inputStream;
    }

    /**
     * Finds a resource with a given name. This method returns null if no
     * resource with this name is found.
     * <p>
     * This method uses the current <tt>Thread</tt> context <tt>ClassLoader</tt> to find
     * the resource. If the resource is not found the class loader of the given
     * class is then used to find the resource.
     *
     * @param name the name of the resource
     * @param aClass the class lookup the resource against, if the resource is
     *     not found using the current <tt>Thread</tt> context <tt>ClassLoader</tt>.
     * @return the URL of the resource if found or null otherwise
     */
    public static URL getResource(String name, Class<?> aClass) {
        Validate.notNull(name, "Parameter name is null");
        Validate.notNull(aClass, "Parameter aClass is null");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        URL url = classLoader.getResource(name);
        if (url == null) {
            url = aClass.getResource(name);
        }

        return url;
    }

    /**
     * Remove the control state from the session for the given stateful control,
     * control name and request context.
     *
     * @param control the stateful control which state to remove
     * @param controlName the name of the control which state to remove
     * @param context the request context
     */
    public static void removeState(Stateful control, String controlName, Context context) {
        if (control == null) {
            throw new IllegalStateException("Control cannot be null.");
        }
        if (controlName == null) {
            throw new IllegalStateException(ClassUtils.getShortClassName(control.getClass())
                    + " name has not been set. State cannot be removed until the name is set");
        }
        if (context == null) {
            throw new IllegalStateException("Context cannot be null.");
        }

        String resourcePath = context.getResourcePath();
        Map pageMap = ClickUtils.getPageState(resourcePath, context);
        if (pageMap != null) {
            Object pop = pageMap.remove(controlName);

            if (pageMap.isEmpty()) {
                // If this was the last state for the page, remove the page state map
                context.removeSessionAttribute(resourcePath);
            } else {
                // Check if control state was emoved
                if (pop != null) {
                    // If control state was removed, set session attribute to force
                    // session replication in a cluster
                    context.setSessionAttribute(resourcePath, pageMap);
                }
            }
        }
    }

    /**
     * Restore the control state from the session for the given stateful control,
     * control name and request context.
     * <p/>
     * This method delegates to {@link org.apache.click.Stateful#setState(java.lang.Object)}
     * to restore the control state.
     *
     * @param control the stateful control which state to restore
     * @param controlName the name of the control which state to restore
     * @param context the request context
     */
    public static void restoreState(Stateful control, String controlName, Context context) {
        if (control == null) {
            throw new IllegalStateException("Control cannot be null.");
        }
        if (controlName == null) {
            throw new IllegalStateException(ClassUtils.getShortClassName(control.getClass())
                    + " name has not been set. State cannot be restored until the name is set");
        }
        if (context == null) {
            throw new IllegalStateException("Context cannot be null.");
        }

        String resourcePath = context.getResourcePath();
        Map pageMap = ClickUtils.getPageState(resourcePath, context);
        if (pageMap != null) {
            control.setState(pageMap.get(controlName));
        }
    }

    /**
     * Save the control state in the session for the given stateful control,
     * control name and request context.
     * <p/>
     * * This method delegates to {@link org.apache.click.Stateful#getState()}
     * to retrieve the control state to save.
     *
     * @param control the stateful control which state to save
     * @param controlName the name of the control control which state to save
     * @param context the request context
     */
    public static void saveState(Stateful control, String controlName, Context context) {
        if (control == null) {
            throw new IllegalStateException("Control cannot be null.");
        }
        if (controlName == null) {
            throw new IllegalStateException(ClassUtils.getShortClassName(control.getClass())
                    + " name has not been set. State cannot be saved until the name is set");
        }
        if (context == null) {
            throw new IllegalStateException("Context cannot be null.");
        }

        String resourcePath = context.getResourcePath();
        Map pageMap = getOrCreatePageState(resourcePath, context);
        Object state = control.getState();
        if (state == null) {
            // Set null state to see if it differs from previous state
            Object pop = pageMap.put(controlName, state);
            if (pop != null) {
                // Previous state differs from current state, so set the
                // session attribute to force session replication in a cluster
                context.setSessionAttribute(resourcePath, pageMap);
            }
        } else {
            pageMap.put(controlName, state);
            // After control state has been added to the page state, set the
            // session attribute to force session replication in a cluster
            context.setSessionAttribute(resourcePath, pageMap);
        }
    }

    /**
     * Return the getter method name for the given property name.
     *
     * @param property the property name
     * @return the getter method name for the given property name.
     */
    public static String toGetterName(String property) {
        HtmlStringBuffer buffer = new HtmlStringBuffer(property.length() + 3);

        buffer.append("get");
        buffer.append(Character.toUpperCase(property.charAt(0)));
        buffer.append(property.substring(1));

        return buffer.toString();
    }

    /**
     * Return the is getter method name for the given property name.
     *
     * @param property the property name
     * @return the is getter method name for the given property name.
     */
    public static String toIsGetterName(String property) {
        HtmlStringBuffer buffer = new HtmlStringBuffer(property.length() + 3);

        buffer.append("is");
        buffer.append(Character.toUpperCase(property.charAt(0)));
        buffer.append(property.substring(1));

        return buffer.toString();
    }

    /**
     * Return a field label string from the given field name. For example:
     * <pre class="codeHtml">
     * <span class="blue">faxNumber</span> &nbsp; -&gt; &nbsp; <span class="red">Fax Number</span> </pre>
     * <p/>
     * <b>Note</b> toLabel will return an empty String ("") if a <tt>null</tt>
     * String name is specified.
     *
     * @param name the field name
     * @return a field label string from the given field name
     */
    public static String toLabel(String name) {
        if (name == null) {
            return "";
        }

        HtmlStringBuffer buffer = new HtmlStringBuffer();

        for (int i = 0, size = name.length(); i < size; i++) {
            char aChar = name.charAt(i);

            if (i == 0) {
                buffer.append(Character.toUpperCase(aChar));

            } else {
                buffer.append(aChar);

                if (i < name.length() - 1) {
                    char nextChar = name.charAt(i + 1);
                    if (Character.isLowerCase(aChar)
                            && (Character.isUpperCase(nextChar)
                            || Character.isDigit(nextChar))) {

                        // Add space before digits or uppercase letters
                        buffer.append(" ");

                    } else if (Character.isDigit(aChar)
                            && (!Character.isDigit(nextChar))) {

                        // Add space after digits
                        buffer.append(" ");
                    }
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Return an 32 char MD5 encoded string from the given plain text.
     * The returned value is MD5 hash compatible with Tomcat catalina Realm.
     * <p/>
     * Adapted from <tt>org.apache.catalina.util.MD5Encoder</tt>
     *
     * @param plaintext the plain text value to encode
     * @return encoded MD5 string
     */
    public static String toMD5Hash(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Null plaintext parameter");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(plaintext.getBytes("UTF-8"));

            byte[] binaryData = md.digest();

            char[] buffer = new char[32];

            for (int i = 0; i < 16; i++) {
                int low = (binaryData[i] & 0x0f);
                int high = ((binaryData[i] & 0xf0) >> 4);
                buffer[i * 2] = HEXADECIMAL[high];
                buffer[i * 2 + 1] = HEXADECIMAL[low];
            }

            return new String(buffer);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a field name string from the given field label.
     * <p/>
     * A label of <tt>" OK do it!"</tt> is returned as <tt>"okDoIt"</tt>. Any <tt>&amp;nbsp;</tt>
     * characters will also be removed.
     * <p/>
     * A label of <tt>"customerSelect"</tt> is returned as <tt>"customerSelect"</tt>.
     *
     * @param label the field label or caption
     * @return a field name string from the given field label
     */
    public static String toName(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Null label parameter");
        }

        boolean doneFirstLetter = false;
        boolean lastCharBlank = false;
        boolean hasWhiteSpace = (label.indexOf(' ') != -1);

        HtmlStringBuffer buffer = new HtmlStringBuffer(label.length());
        for (int i = 0, size = label.length(); i < size; i++) {
            char aChar = label.charAt(i);

            if (aChar != ' ') {
                if (Character.isJavaIdentifierPart(aChar)) {
                    if (lastCharBlank) {
                        if (doneFirstLetter) {
                            buffer.append(Character.toUpperCase(aChar));
                            lastCharBlank = false;
                        } else {
                            buffer.append(Character.toLowerCase(aChar));
                            lastCharBlank = false;
                            doneFirstLetter = true;
                        }
                    } else {
                        if (doneFirstLetter) {
                            if (hasWhiteSpace) {
                                buffer.append(Character.toLowerCase(aChar));
                            } else {
                                buffer.append(aChar);
                            }
                        } else {
                            buffer.append(Character.toLowerCase(aChar));
                            doneFirstLetter = true;
                        }
                    }
                }
            } else {
                lastCharBlank = true;
            }
        }

        return buffer.toString();
    }

    /**
     * Return the setter method name for the given property name.
     *
     * @param property the property name
     * @return the setter method name for the given property name.
     */
    public static String toSetterName(String property) {
        HtmlStringBuffer buffer = new HtmlStringBuffer(property.length() + 3);

        buffer.append("set");
        buffer.append(Character.toUpperCase(property.charAt(0)));
        buffer.append(property.substring(1));

        return buffer.toString();
    }

    /**
     * Returns true if Click resources (JavaScript, CSS, images etc) packaged
     * in jars can be deployed to the root directory of the webapp, false
     * otherwise.
     * <p/>
     * This method will return false in restricted environments where write
     * access to the underlying file system is disallowed. Examples where
     * write access is not allowed include the WebLogic JEE server (this can be
     * changed though) and Google App Engine.
     *
     * @param servletContext the application servlet context
     * @return true if writes are allowed, false otherwise
     */
    public static boolean isResourcesDeployable(ServletContext servletContext) {
        try {
            boolean canWrite = (servletContext.getRealPath("/") != null);
            if (!canWrite) {
                return false;
            }

            // Since Google App Engine returns a value for getRealPath, check
            // SecurityManager if writes are allowed
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkWrite("/click");
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // -------------------------------------------------------- Package Methods

    /**
     * Append the escaped string for the given character value to the
     * buffer. The following characters are escaped: &lt;, &gt;, &quot;, &#039;,
     * &amp;.
     *
     * @param aChar the character value to escape
     * @param buffer the string buffer to append the escaped value to
     */
    static void appendEscapeChar(char aChar, HtmlStringBuffer buffer) {
        int index = aChar;

        if (index < XML_ENTITIES.length && XML_ENTITIES[index] != null) {
            buffer.append(XML_ENTITIES[index]);

        } else {
            buffer.append(aChar);
        }
    }

    /**
     * Append the escaped string for the given character value to the buffer.
     * The following characters are escaped: &lt;, &gt;, &quot;, &#039;, &amp;.
     *
     * @param value the String value to escape
     * @param buffer the string buffer to append the escaped value to
     */
    static void appendEscapeString(String value, HtmlStringBuffer buffer) {
        char aChar;
        for (int i = 0, size = value.length(); i < size; i++) {
            aChar = value.charAt(i);
            int index = aChar;

            if (index < XML_ENTITIES.length && XML_ENTITIES[index] != null) {
                buffer.append(XML_ENTITIES[index]);

            } else {
                buffer.append(aChar);
            }
        }
    }

    /**
     * Append the HTML escaped string for the given character value to the
     * buffer.
     *
     * @param aChar the character value to escape
     * @param buffer the string buffer to append the escaped value to
     */
    static void appendHtmlEscapeChar(char aChar, HtmlStringBuffer buffer) {
        int index = aChar;

        if (index < HTML_ENTITIES.length && HTML_ENTITIES[index] != null) {
            buffer.append(HTML_ENTITIES[index]);

        } else {
            buffer.append(aChar);
        }
    }

    /**
     * Append the HTML escaped string for the given character value to the
     * buffer.
     *
     * @param value the String value to escape
     * @param buffer the string buffer to append the escaped value to
     */
    static void appendHtmlEscapeString(String value, HtmlStringBuffer buffer) {
        char aChar;
        for (int i = 0, size = value.length(); i < size; i++) {
            aChar = value.charAt(i);
            int index = aChar;

            if (index < HTML_ENTITIES.length && HTML_ENTITIES[index] != null) {
                buffer.append(HTML_ENTITIES[index]);

            } else {
                buffer.append(aChar);
            }
        }
    }

    /**
     * Return true if the given character requires escaping. The following
     * characters are escaped: &lt;, &gt;, &quot;, &#039;, &amp;.
     *
     * @param aChar the character value to test
     * @return true if the given character requires escaping
     */
    static boolean requiresEscape(char aChar) {
        int index = aChar;

        if (index < XML_ENTITIES.length) {
            return XML_ENTITIES[index] != null;

        } else {
            return false;
        }
    }

    /**
     * Return true if the given string requires escaping of characters.
     * The following characters require escaping: &lt;, &gt;, &quot;, &#039;, &amp;.
     *
     * @param value the string value to test
     * @return true if the given string requires escaping of characters
     */
    static boolean requiresEscape(String value) {
        if (value == null) {
            return false;
        }

        int length = value.length();
        for (int i = 0; i < length; i++) {
            if (requiresEscape(value.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return true if the given character requires HTML escaping.
     *
     * @param aChar the character value to test
     * @return true if the given character requires HTML escaping
     */
    static boolean requiresHtmlEscape(char aChar) {
        int index = aChar;

        if (index < HTML_ENTITIES.length) {
            return HTML_ENTITIES[index] != null;

        } else {
            return false;
        }
    }

    /**
     * Return true if the given string requires HTML escaping of characters.
     *
     * @param value the string value to test
     * @return true if the given string requires HTML escaping of characters
     */
    static boolean requiresHtmlEscape(String value) {
        if (value == null) {
            return false;
        }

        int length = value.length();
        for (int i = 0; i < length; i++) {
            if (requiresHtmlEscape(value.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    // -------------------------------------------------------- Private Methods

    /**
     * A helper method that binds the submitted request values of all Fields
     * and Links inside the given container or child containers.
     * <p/>
     * For Field controls, this method delegates to
     * {@link #bindField(org.openidentityplatform.openam.click.control.Field, org.openidentityplatform.openam.click.Context)}.
     *
     * @param container the container which Fields and Links to bind
     * @param context the request context
     */
    private static void bind(Container container, Context context) {
        for (int i = 0; i < container.getControls().size(); i++) {
            Control control = container.getControls().get(i);
            if (control instanceof Container) {
                // Include fields but skip fieldSets
                if (control instanceof Field) {
                    Field field = (Field) control;
                    bindField(field, context);

                } else if (control instanceof AbstractLink) {
                    ((AbstractLink) control).bindRequestValue();
                }
                Container childContainer = (Container) control;
                bind(childContainer, context);

            } else if (control instanceof Field) {
                Field field = (Field) control;
                bindField(field, context);

            } else if (control instanceof AbstractLink) {
                ((AbstractLink) control).bindRequestValue();
            }
        }
    }

    /**
     * A helper method that binds and validates the submitted request values
     * of all Fields and Links inside the given container or child containers.
     * <p/>
     * For Field controls, this method delegates to
     * {@link #bindField(org.openidentityplatform.openam.click.control.Field, org.openidentityplatform.openam.click.Context)}.
     *
     * @param container the container which Fields and Links to bind and
     * validate
     * @param context the request context
     * @return true if container fields and links was bound and valid, false
     * otherwise
     */
    private static boolean bindAndValidate(Container container, Context context) {
        boolean valid = true;
        for (int i = 0; i < container.getControls().size(); i++) {
            Control control = container.getControls().get(i);
            if (control instanceof Container) {

                // Bind and validate fields only
                if (control instanceof Field) {
                    if (!bindAndValidate((Field) control, context)) {
                        valid = false;
                    }

                } else if (control instanceof AbstractLink) {
                    ((AbstractLink) control).bindRequestValue();
                }

                Container childContainer = (Container) control;
                if (!bindAndValidate(childContainer, context)) {
                    valid = false;
                }

            } else if (control instanceof Field) {
                if (!bindAndValidate((Field) control, context)) {
                    valid = false;
                }

            } else if (control instanceof AbstractLink) {
                ((AbstractLink) control).bindRequestValue();
            }
        }
        return valid;
    }

    /**
     * A helper method that binds and validates the submitted request values
     * of all Fields and Links inside the given Form or child containers. Note,
     * the Form itself is also validated.
     * <p/>
     * For Field controls, this method delegates to
     * {@link #bindField(org.openidentityplatform.openam.click.control.Field, org.openidentityplatform.openam.click.Context)}.
     *
     * @param form the Form which Fields and Links to bind and validate
     * @param context the request context
     * @return true if the form, it's fields and links was bound and valid, false
     * otherwise
     */
    private static boolean bindAndValidate(Form form, Context context) {
        if (!bindAndValidate((Container) form, context)) {
            return false;
        }

        if (form.getValidate()) {
            // Keep reference to current error
            String errorReference = form.getError();

            // Validate form. If validation fails the form error will be changed
            form.validate();

            boolean valid = form.isValid();

            // Revert back to original error
            form.setError(errorReference);

            return valid;
        }
        return true;
    }

    /**
     * A helper method that binds and validates the Field's submitted request
     * value.
     * <p/>
     * This method delegates to
     * {@link #bindField(org.openidentityplatform.openam.click.control.Field, org.openidentityplatform.openam.click.Context)}
     * to bind the field value.
     *
     * @param field the Field to bind and validate
     * @param context the request context
     * @return true if field was bound and valid, or false otherwise
     */
    private static boolean bindAndValidate(Field field, Context context) {
        boolean continueProcessing = bindField(field, context);
        if (!continueProcessing) {
            return true;
        }

        if (field.getValidate()) {
            // Keep reference to current error
            String errorReference = field.getError();

            // Validate field. If validation fails the field error will be changed
            field.validate();

            boolean valid = field.isValid();

            // Revert back to original error
            field.setError(errorReference);

            return valid;
        }
        return true;
    }

    /**
     * Bind the field to its incoming request parameter, returning true if the
     * field value was bound, false otherwise.
     * <p/>
     * <b>Please note</b>: this method won't bind disabled fields,
     * unless the field has an incoming request parameter matching its name.
     * If an incoming request parameter is present, this method will switch off
     * the Field's disabled property.
     *
     * @param field the field which value to bind to its request parameter
     * @param context the request context
     * @return true if the field was bound to its request parameter, false
     * otherwise
     */
    private static boolean bindField(Field field, Context context) {
        if (field.isDisabled()) {
            // Switch off disabled property if control has incoming request
            // parameter. Normally this means the field was enabled via JS
            if (context.hasRequestParameter(field.getName())) {
                field.setDisabled(false);
            } else {
                return false;
            }
        }
        field.bindRequestValue();
        return true;
    }

    /**
     * Retrieve or create the map where page state is stored in.
     *
     * @see #getPageState(java.lang.String, org.openidentityplatform.openam.click.Context)
     *
     * @param pagePath the path under which the page state is stored in the
     * session
     * @param context the request context
     * @return the map where page state is stored in
     */
    private static Map getOrCreatePageState(String pagePath, Context context) {
        Map pageMap = getPageState(pagePath, context);
        if (pageMap == null) {
            pageMap = new HashMap();
        }
        return pageMap;
    }

    /**
     * Retrieve the map for the given pagePath from the session where page state
     * is stored in.
     *
     * @param pagePath the path under which the page state is stored in the
     * session
     * @param context the request context
     * @return the map where page state is stored in
     */
    private static Map getPageState(String pagePath, Context context) {
        Object storedPageValue = context.getSessionAttribute(pagePath);
        Map pageMap = null;
        if (storedPageValue != null) {
            pageMap = (Map) storedPageValue;
        }
        return pageMap;
    }

    /**
     * Invoke the named method on the given target object and return the result.
     *
     * @param target the target object with the method to invoke
     * @param method the name of the method to invoke
     * @return Object the target method result
     */
    private static Object invokeMethod(Object target, String method) {
        if (target == null) {
            throw new IllegalArgumentException("Null target parameter");
        }
        if (method == null) {
            throw new IllegalArgumentException("Null method parameter");
        }

        Method targetMethod = null;
        boolean isAccessible = true;
        try {
            Class<?> targetClass = target.getClass();
            targetMethod = targetClass.getMethod(method);

            // Change accessible for anonymous inner classes public methods
            // only. Conditional checks:
            // #1 - Target method is not accessible
            // #2 - Anonymous inner classes are not public
            // #3 - Only modify public methods
            // #4 - Anonymous inner classes have no declaring class
            // #5 - Anonymous inner classes have $ in name
            if (!targetMethod.isAccessible()
                    && !Modifier.isPublic(targetClass.getModifiers())
                    && Modifier.isPublic(targetMethod.getModifiers())
                    && targetClass.getDeclaringClass() == null
                    && targetClass.getName().indexOf('$') != -1) {

                isAccessible = false;
                targetMethod.setAccessible(true);
            }

            return targetMethod.invoke(target);

        } catch (InvocationTargetException ite) {

            Throwable e = ite.getTargetException();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;

            } else if (e instanceof Exception) {
                String msg =
                        "Exception occurred invoking public method: " + targetMethod;

                throw new RuntimeException(msg, e);

            } else if (e instanceof Error) {
                String msg =
                        "Error occurred invoking public method: " + targetMethod;

                throw new RuntimeException(msg, e);

            } else {
                String msg =
                        "Error occurred invoking public method: " + targetMethod;

                throw new RuntimeException(msg, e);
            }

        } catch (Exception e) {
            String msg =
                    "Exception occurred invoking public method: " + targetMethod;

            throw new RuntimeException(msg, e);

        } finally {
            if (targetMethod != null && !isAccessible) {
                targetMethod.setAccessible(false);
            }
        }
    }
}
