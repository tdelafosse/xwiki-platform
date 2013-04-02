/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.user.impl.xwiki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.securityfilter.authenticator.persistent.DefaultPersistentLoginManager;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.web.Utils;

/**
 * Class responsible for remembering the login information between requests. It uses (encrypted) cookies for this. The
 * encryption key is stored in xwiki.cfg, xwiki.authentication.encryptionKey parameter. The cookies used are:
 * <dl>
 * <dt>username</dt>
 * <dd>The logged in username</dd>
 * <dt>password</dt>
 * <dd>The password</dd>
 * <dt>rememberme</dt>
 * <dd>Whether or not the authentication information should be preserved across sessions</dd>
 * <dt>validation</dt>
 * <dd>Token used for validating the cookie information. It contains hashed information about the other cookies and a
 * secret paramete, optionally binding with the current IP of the user (so that the cookie cannot be reused on another
 * computer). This binding is enabled by the parameter xwiki.authentication.useip . The secret parameter is specified in
 * xwiki.authentication.validationKey</dd>
 * </dl>
 * 
 * @version $Id$
 */
public class MyPersistentLoginManager extends DefaultPersistentLoginManager
{
    /**
     * Just for information
     */
    protected String encryptionAlgorithm = "AES";
    protected String encryptionMode = "CBC";
    
    /**
     * Use the environment component in order to get the wiki permanent directory where the encryptions keys should be stored
     */
    private Environment environment = Utils.getComponent(Environment.class);
    
    /**
     * Name of the file where the encryption keys are stored.
     */
    private static final String ENCRYPTION_KEY_FILE_NAME = "AuthenticationEncryptionKey.txt";
    
    /**
     * Password protecting the keystore.
     */
    private static final String KEYSTORE_PASSWORD = "asinvESdl9csw";
    
    /**
     * Password protecting the encryption key.
     */
    private static final String ENCRYPTION_KEY_PROTECTION = "ad6TdolIkx9I";
    
    /**
     * The string used to separate the fields in the hashed validation message.
     */
    private static final String FIELD_SEPARATOR = ":";

    /**
     * Log4J logger object to log messages in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyPersistentLoginManager.class);

    /**
     * Default value to use when getting the authentication cookie values.
     */
    private static final String DEFAULT_VALUE = "false";

    /** Date formatter for the cookie "Expires" value. */
    private static final DateFormat COOKIE_EXPIRE_FORMAT = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z",
        Locale.US);
    static {
        COOKIE_EXPIRE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /** For performance, cache the often used epoch date which forces a cookie to be removed. */
    private static final String COOKIE_EXPIRE_NOW = COOKIE_EXPIRE_FORMAT.format(new Date(0));

    /**
     * The domain generalization for which the cookies are active. Configured by the xwiki.authentication.cookiedomains
     * parameter. If a request comes from a host not in this list, then the cookie is valid only for the requested
     * domain. If a request comes from a host that partially matches a domain in this list (meaning that the value in
     * the list is contained in the requested domain), then the cookie is set for the more general value found in the
     * list. This is useful for using the same account across multiple virtual wikis, for example.
     */
    protected String[] cookieDomains;

    /**
     * The path for which the cookies are active. By default the cookie is active for all paths in the configured
     * domains.
     */
    protected String cookiePath = "/";

    /**
     * The prefix that should be used for cookie names.
     */
    protected String cookiePrefix = "";
    
    /**
     * Use SHA-512 instead of MD5
     */
    protected String valueBeforeSHA = "";
    
    /**
     * Use SHA-512 instead of MD5
     */
    protected String valueAfterSHA = "";
    
    protected static final String COOKIE_USERNAME_IV = "usernameiv";
    protected static final String COOKIE_PASSWORD_IV = "passwordiv";
    
    /**
     * Default constructor. The configuration is done outside, in
     * {@link XWikiAuthServiceImpl#getAuthenticator(com.xpn.xwiki.XWikiContext)}, so no parameters are needed at this
     * point.
     */
    public MyPersistentLoginManager()
    {
        super();
    }

    /**
     * Setter for the {@link #cookieDomains} parameter.
     * 
     * @param cdlist The new value for {@link #cookieDomains}. The list is processed, so that any value not starting
     *            with a dot is prefixed with one, to respect the cookie RFC.
     * @see #cookieDomains
     */
    public void setCookieDomains(String[] cdlist)
    {
        if (cdlist != null && cdlist.length > 0) {
            this.cookieDomains = new String[cdlist.length];
            for (int i = 0; i < cdlist.length; ++i) {
                if (cdlist[i] != null && !cdlist[i].startsWith(".")) {
                    this.cookieDomains[i] = ".".concat(cdlist[i]);
                } else {
                    this.cookieDomains[i] = cdlist[i];
                }
            }
        } else {
            this.cookieDomains = null;
        }
    }

    /**
     * Setter for the {@link #cookiePath} parameter.
     * 
     * @param cp The new value for {@link #cookiePath}.
     * @see #cookiePath
     */
    public void setCookiePath(String cp)
    {
        this.cookiePath = cp;
    }

    /**
     * Setup a cookie: expiration date, path, domain + send it to the response.
     * 
     * @param cookie The cookie to setup.
     * @param sessionCookie Whether the cookie is only for this session, or for a longer period.
     * @param cookieDomain The domain for which the cookie is set.
     * @param response The servlet response.
     */
    public void setupCookie(Cookie cookie, boolean sessionCookie, String cookieDomain, HttpServletResponse response)
    {
        if (!sessionCookie) {
            setMaxAge(cookie);
        }
        cookie.setPath(this.cookiePath);
        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }
        addCookie(response, cookie);
    }

    /**
     * Remember a specific login using cookies.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @param username The username that's being remembered.
     * @param password The password that's being remembered.
     */
    @Override
    public void rememberLogin(HttpServletRequest request, HttpServletResponse response, String username, String password)
    {
        String ivUsername = "";
        String protectedUsername = username;
        String ivPassword = "";
        String protectedPassword = password;
        if (this.protection.equals(PROTECTION_ALL) || this.protection.equals(PROTECTION_ENCRYPTION)) {
            String[] encryptedUsername = encryptText(protectedUsername);
            String[] encryptedPassword = encryptText(protectedPassword);
            ivUsername = encryptedUsername[0];
            protectedUsername = encryptedUsername[1];
            ivPassword = encryptedPassword[0];
            protectedPassword = encryptedPassword[1];
                
            if (protectedUsername == null || protectedPassword == null) {
                LOGGER.error("ERROR!!");
                LOGGER.error("There was a problem encrypting the username or password!!");
                LOGGER.error("Remember Me function will be disabled!!");
                return;
            }
        }

        // Let's check if the cookies should be session cookies or persistent ones.
        boolean sessionCookie = !(isTrue(request.getParameter("j_rememberme")));
        String cookieDomain = getCookieDomain(request);

        // Create client cookies to remember the login information.

        // Username
        Cookie usernameCookie = new Cookie(getCookiePrefix() + COOKIE_USERNAME, protectedUsername);
        setupCookie(usernameCookie, sessionCookie, cookieDomain, response);
        
        //Username initation vector
        Cookie usernameIVCookie = new Cookie(getCookiePrefix() + COOKIE_USERNAME_IV, ivUsername);
        setupCookie(usernameIVCookie, sessionCookie, cookieDomain, response);

        // Password
        Cookie passwdCookie = new Cookie(getCookiePrefix() + COOKIE_PASSWORD, protectedPassword);
        setupCookie(passwdCookie, sessionCookie, cookieDomain, response);
        
        //Password initiation vector
        Cookie passwordIVCookie = new Cookie(getCookiePrefix() + COOKIE_PASSWORD_IV, ivPassword);
        setupCookie(passwordIVCookie, sessionCookie, cookieDomain, response);

        // Remember me
        Cookie rememberCookie = new Cookie(getCookiePrefix() + COOKIE_REMEMBERME, !sessionCookie + "");
        setupCookie(rememberCookie, sessionCookie, cookieDomain, response);

        if (this.protection.equals(PROTECTION_ALL) || this.protection.equals(PROTECTION_VALIDATION)) {
            String validationHash = getValidationHash(protectedUsername, protectedPassword, getClientIP(request));
            if (validationHash != null) {
                // Validation
                Cookie validationCookie = new Cookie(getCookiePrefix() + COOKIE_VALIDATION, validationHash);
                setupCookie(validationCookie, sessionCookie, cookieDomain, response);
            } else {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("WARNING!!! WARNING!!!");
                    LOGGER.error("PROTECTION=ALL or PROTECTION=VALIDATION was specified");
                    LOGGER.error("but Validation Hash could NOT be generated");
                    LOGGER.error("Validation has been disabled!!!!");
                }
            }
        }
        return;
    }

    /**
     * Sets the maximum age for cookies. The maximum age is configured in xwiki.cfg using the
     * xwiki.authentication.cookielife parameter (number of days). The default age is 14 days.
     * 
     * @param cookie The cookie for which the expiration date is configured.
     */
    private void setMaxAge(Cookie cookie)
    {
        try {
            cookie.setMaxAge(Math.round(60 * 60 * 24 * Float.parseFloat(this.cookieLife)));
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed setting cookie Max age with duration " + this.cookieLife);
            }
        }
    }

    /**
     * Adds a cookie to the response.
     * 
     * @param response The servlet response.
     * @param cookie The cookie to be sent.
     */
    private void addCookie(HttpServletResponse response, Cookie cookie)
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adding cookie: " + cookie.getDomain() + cookie.getPath() + " " + cookie.getName() + "="
                + cookie.getValue());
        }
        // We don't use the container's response.addCookie, since the HttpOnly cookie flag was introduced only recently
        // in the servlet specification, and we're still using the older 2.4 specification as a minimal requirement for
        // compatibility with as many containers as possible. Instead, we write the cookie manually as a HTTP header.
        StringBuilder cookieValue = new StringBuilder(150);
        cookieValue.append(cookie.getName() + "=");
        if (StringUtils.isNotEmpty(cookie.getValue())) {
            cookieValue.append("\"" + cookie.getValue() + "\"");
        }
        cookieValue.append("; Version=1");
        if (cookie.getMaxAge() >= 0) {
            cookieValue.append("; Max-Age=" + cookie.getMaxAge());
            // IE is such a pain, it doesn't understand the modern, safer Max-Age
            cookieValue.append("; Expires=");
            if (cookie.getMaxAge() == 0) {
                cookieValue.append(COOKIE_EXPIRE_NOW);
            } else {
                cookieValue.append(COOKIE_EXPIRE_FORMAT.format(new Date(System.currentTimeMillis() + cookie.getMaxAge()
                    * 1000L)));
            }
        }
        if (StringUtils.isNotEmpty(cookie.getDomain())) {
            // IE needs toLowerCase for the domain name
            cookieValue.append("; Domain=" + cookie.getDomain().toLowerCase());
        }
        if (StringUtils.isNotEmpty(cookie.getPath())) {
            cookieValue.append("; Path=" + cookie.getPath());
        }
        // Protect cookies from being used from JavaScript, see http://www.owasp.org/index.php/HttpOnly
        cookieValue.append("; HttpOnly");

        // Session cookies should be discarded.
        // FIXME Safari 5 can't handle properly "Discard", as it really discards all the response header data after the
        // first "Discard" encountered, so it will only see the first such cookie. Disabled for the moment until Safari
        // gets fixed, or a better idea comes to mind.
        // Since we don't set a Max-Age, the rfc2109 behavior will kick in, and recognize this as a session cookie.
        // if (cookie.getMaxAge() < 0) {
        // cookieValue.append("; Discard");
        // }
        response.addHeader("Set-Cookie", cookieValue.toString());
    }

    /**
     * Compute the actual domain the cookie is supposed to be set for. Search through the list of generalized domains
     * for a partial match. If no match is found, then no specific domain is used, which means that the cookie will be
     * valid only for the requested domain.
     * 
     * @param request The servlet request.
     * @return The configured domain generalization that matches the request, or null if no match is found.
     */
    private String getCookieDomain(HttpServletRequest request)
    {
        String cookieDomain = null;
        if (this.cookieDomains != null) {
            String servername = request.getServerName();
            for (int i = 0; i < this.cookieDomains.length; i++) {
                if (servername.indexOf(this.cookieDomains[i]) != -1) {
                    cookieDomain = this.cookieDomains[i];
                    break;
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cookie domain is:" + cookieDomain);
        }
        return cookieDomain;
    }

    /**
     * Get validation hash for the specified parameters. The hash includes a secret password, and optionally binds the
     * cookie to the requester's IP. The hash secret is configured using the xwiki.authentication.validationKey
     * parameter. The IP binding is enabled using the xwiki.authentication.useip parameter.
     * 
     * @param username The remembered username.
     * @param password The remembered password.
     * @param clientIP The client IP of the request.
     * @return Validation hash.
     */
    private String getValidationHash(String username, String password, String clientIP)
    {
        if (this.validationKey == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("ERROR! >> validationKey not specified...");
                LOGGER.error("you are REQUIRED to specify the validatonkey in xwiki.cfg");
            }
            return null;
        }
        MessageDigest SHA = null;
        StringBuffer sbValueBeforeSHA = new StringBuffer();

        try {
            SHA = MessageDigest.getInstance("SHA-512");

            sbValueBeforeSHA.append(username);
            sbValueBeforeSHA.append(FIELD_SEPARATOR);
            sbValueBeforeSHA.append(password.toString());
            sbValueBeforeSHA.append(FIELD_SEPARATOR);
            if (isTrue(this.useIP)) {
                sbValueBeforeSHA.append(clientIP.toString());
                sbValueBeforeSHA.append(FIELD_SEPARATOR);
            }
            sbValueBeforeSHA.append(this.validationKey.toString());

            this.valueBeforeSHA = sbValueBeforeSHA.toString();
            SHA.update(this.valueBeforeSHA.getBytes());

            byte[] array = SHA.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }
            this.valueAfterSHA = sb.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to get [" + MessageDigest.class.getName() + "] instance", e);
        }

        return this.valueAfterSHA;
    }

    /**
     * Encrypt a string. The encryption is password-based. The password can be configured using the
     * xwiki.authentication.encryptionKey parameter.
     * 
     * @param clearText The text to be encrypted.
     * @return clearText, encrypted.
     * @todo Optimize this code by creating the Cipher only once.
     */
    public String[] encryptText(String clearText)
    {
        try {
            Cipher c1 = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec encryptionKey = getEncryptionKey();
            if (encryptionKey != null) {
                c1.init(Cipher.ENCRYPT_MODE, encryptionKey);
                IvParameterSpec iv = c1.getParameters().getParameterSpec(IvParameterSpec.class);
                byte[] ivBytes = iv.getIV();
                byte[] clearTextBytes;
                clearTextBytes = clearText.getBytes();
                byte[] encryptedText = c1.doFinal(clearTextBytes);
                String encryptedEncodedText = new String(Base64.encodeBase64(encryptedText));
                String ivParameters = new String(Base64.encodeBase64(ivBytes));
                // Since the cookie spec does not allow = in the cookie value, it must be replaced
                // with something else. Bas64 does not use _, and it is allowed in cookies, so
                // we're using that instead of =. In decryptText the reverse operation is perfomed.
                // See XWIKI-2211
                String[] couple = {ivParameters.replaceAll("=", "_"), encryptedEncodedText.replaceAll("=", "_")};
                return couple;
            }
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("ERROR! >> SecretKey not generated...");
            }
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to encrypt text: " + clearText, e);
            }
        }
        return null;
    }

    /**
     * Forget a login by removing the authentication cookies.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     */
    @Override
    public void forgetLogin(HttpServletRequest request, HttpServletResponse response)
    {
        ((SecurityRequestWrapper) request).setUserPrincipal(null);
        removeCookie(request, response, getCookiePrefix() + COOKIE_USERNAME);
        removeCookie(request, response, getCookiePrefix() + COOKIE_USERNAME_IV);
        removeCookie(request, response, getCookiePrefix() + COOKIE_PASSWORD);
        removeCookie(request, response, getCookiePrefix() + COOKIE_PASSWORD_IV);
        removeCookie(request, response, getCookiePrefix() + COOKIE_REMEMBERME);
        removeCookie(request, response, getCookiePrefix() + COOKIE_VALIDATION);
        return;
    }

    /**
     * Given an array of cookies and a name, this method tries to find and return the cookie from the array that has the
     * given name. If there is no cookie matching the name in the array, null is returned.
     * 
     * @param cookies The list of cookies sent by the client.
     * @param cookieName The name of the cookie to be retrieved.
     * @return The requested cookie, or null if no cookie with the given name was found.
     */
    private static Cookie getCookie(Cookie[] cookies, String cookieName)
    {
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookieName.equals(cookie.getName())) {
                    return (cookie);
                }
            }
        }
        return null;
    }

    /**
     * Remove a cookie.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @param cookieName The name of the cookie that must be removed.
     */
    private void removeCookie(HttpServletRequest request, HttpServletResponse response, String cookieName)
    {
        Cookie cookie = getCookie(request.getCookies(), cookieName);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setPath(this.cookiePath);
            addCookie(response, cookie);
            String cookieDomain = getCookieDomain(request);
            if (cookieDomain != null) {
                cookie.setDomain(cookieDomain);
                addCookie(response, cookie);
            }
        }
    }

    /**
     * Check if a text is supposed to be an affirmative value ("true", "yes" or "1").
     * 
     * @param text The text to check.
     * @return true if the text is one of "true", "yes" or "1", false otherwise.
     */
    private static boolean isTrue(String text)
    {
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    /**
     * Given an array of Cookies, a name, and a default value, this method tries to find the value of the cookie with
     * the given name. If there is no cookie matching the name in the array, then the default value is returned instead.
     * 
     * @param cookies The list of cookies to search.
     * @param cookieName The name of the cookie whose value should be returned.
     * @param defaultValue The default value that should be returned when no cookie with the given name was found.
     * @return The value of the cookie with the given name, or defaultValue if no such cookie was found.
     */
    private static String getCookieValue(Cookie[] cookies, String cookieName, String defaultValue)
    {
        String value = defaultValue;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookieName.equals(cookie.getName())) {
                    value = cookie.getValue();
                }
            }
        }
        return value;
    }

    /**
     * Checks if the cookies are valid.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @return True if the validation cookie holds a valid value or is not present, false otherwise.
     * @todo Don't ignore it when set to "false", check the validation method.
     */
    private boolean checkValidation(HttpServletRequest request, HttpServletResponse response)
    {
        if (this.protection.equals(PROTECTION_ALL) || this.protection.equals(PROTECTION_VALIDATION)) {
            String username = getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_USERNAME, DEFAULT_VALUE);
            String password = getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_PASSWORD, DEFAULT_VALUE);
            String cookieHash =
                getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_VALIDATION, DEFAULT_VALUE);
            String calculatedHash = getValidationHash(username, password, getClientIP(request));
            if (cookieHash.equals(calculatedHash)) {
                return true;
            } else {
                LOGGER.warn("Login cookie validation hash mismatch! Cookies have been tampered with");
                LOGGER.info("Login cookie is being deleted!");
                forgetLogin(request, response);
            }
        }
        return false;
    }

    /**
     * Get the username stored (in a cookie) in the request. Also checks the validity of the cookie.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @return The username value, or <tt>null</tt> if not found or the cookie isn't valid.
     * @todo Also use the URL, in case cookies are disabled [XWIKI-1071].
     */
    @Override
    public String getRememberedUsername(HttpServletRequest request, HttpServletResponse response)
    {
        String username = getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_USERNAME, DEFAULT_VALUE);
        String usernameIV = getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_USERNAME_IV, DEFAULT_VALUE);

        if (!username.equals(DEFAULT_VALUE)) {
            if (checkValidation(request, response)) {
                if (this.protection.equals(PROTECTION_ALL) || this.protection.equals(PROTECTION_ENCRYPTION)) {
                    username = decryptText(username, usernameIV);
                }
                return username;
            }
        }
        return null;
    }

    /**
     * Get the password stored (in a cookie) in the request. Also checks the validity of the cookie.
     * 
     * @param request The servlet request.
     * @param response The servlet response.
     * @return The password value, or <tt>null</tt> if not found or the cookie isn't valid.
     * @todo Also use the URL, in case cookies are disabled [XWIKI-1071].
     */
    @Override
    public String getRememberedPassword(HttpServletRequest request, HttpServletResponse response)
    {
        String password = getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_PASSWORD, DEFAULT_VALUE);
        String ivPassword = getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_PASSWORD_IV, DEFAULT_VALUE);
        if (!password.equals(DEFAULT_VALUE)) {
            if (checkValidation(request, response)) {
                if (this.protection.equals(PROTECTION_ALL) || this.protection.equals(PROTECTION_ENCRYPTION)) {
                    password = decryptText(password, ivPassword);
                }
                return password;
            }
        }
        return null;
    }

    @Override
    public boolean rememberingLogin(HttpServletRequest request)
    {
        if (getCookieValue(request.getCookies(), getCookiePrefix() + COOKIE_REMEMBERME, "false").equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Decrypt a string.
     * 
     * @param encryptedText The encrypted value.
     * @return encryptedText, decrypted
     */
    private String decryptText(String encryptedText, String ivString)
    {
        try {
            // Since the cookie spec does not allow = in the cookie value, it must be replaced
            // with something else. Bas64 does not use _, and it is allowed in cookies, so
            // we're using that instead of =. In encryptText the reverse operation was perfomed,
            // so here we must re-introduce the = sign needed by Base64.
            // See XWIKI-2211
            byte[] decodedEncryptedText =
                Base64.decodeBase64(encryptedText.replaceAll("_", "=").getBytes("ISO-8859-1"));
            Cipher c1 = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] ivBytes = Base64.decodeBase64(ivString.replaceAll("_", "=").getBytes("ISO-8859-1"));
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            c1.init(Cipher.DECRYPT_MODE, getEncryptionKey(), iv);
            byte[] decryptedText = c1.doFinal(decodedEncryptedText);
            String decryptedTextString = new String(decryptedText);
            return decryptedTextString;
        } catch (Exception e) {
            LOGGER.error("Error decrypting text: " + encryptedText, e);
            return null;
        }
    }

    /**
     * Returns the original client IP. Needed because request.getRemoteAddr returns the address of the last requesting
     * host, which can be either the real client, or a proxy. The original method prevents logging in when using a
     * cluster of reverse proxies in front of XWiki.
     * 
     * @param request The servlet request.
     * @return The IP of the actual client.
     */
    protected String getClientIP(HttpServletRequest request)
    {
        String remoteIP = request.getHeader("X-Forwarded-For");
        if (remoteIP == null || "".equals(remoteIP)) {
            remoteIP = request.getRemoteAddr();
        } else if (remoteIP.indexOf(',') != -1) {
            remoteIP = remoteIP.substring(0, remoteIP.indexOf(','));
        }
        return remoteIP;
    }

    /**
     * Setter for the {@link #cookiePrefix} parameter.
     * 
     * @param prefix The new value for {@link #cookiePrefix}.
     * @see #cookiePrefix
     */
    public void setCookiePrefix(String prefix)
    {
        this.cookiePrefix = prefix;
    }

    /**
     * Getter for the {@link #cookiePrefix} parameter.
     * 
     * @return The value for {@link #cookiePrefix}.
     * @see #cookiePrefix
     */
    public String getCookiePrefix()
    {
        return this.cookiePrefix;
    }
    
    private SecretKeySpec getEncryptionKey()
    {
        try
        {
            KeyStore ks = KeyStore.getInstance("JCEKS");
            char[] password = KEYSTORE_PASSWORD.toCharArray();
            File file = this.getEncryptionFile();
            if(!file.exists())
            {
                LOGGER.debug("The encryption file doesn't exist yet");
                ks.load(null, password);
                storeEncryptionKey(ks);
            }
            else
                ks.load(new FileInputStream(file), password);
            return retrieveEncryptionKey(ks);
        }
        catch (Exception e)
        {
            LOGGER.warn("Cannot retrieve encryption key : " + e.getMessage());
            return null;
        }
    }
    
    /**
     *  Get the file where the encryption key is supposed to be stored. 
     *  
     *  @return The file where the encryption key is stored
     *  */
    private File getEncryptionFile()
    {
        File permDir = environment.getPermanentDirectory();
        String path = permDir.getAbsolutePath() + permDir.pathSeparator + ENCRYPTION_KEY_FILE_NAME;
        File encryptionFile = new File(path) ;
        return encryptionFile ;
    }
    
    
    /**
     * Store a randomly generated encryption key in a keystore.
     * 
     * @param ks Keystore where the key should be kept
     */
    public void storeEncryptionKey(KeyStore ks)
    {
        try
        {
            String encryptionKey = generateRandomKey();
            String storePassword = KEYSTORE_PASSWORD;
            String protection = ENCRYPTION_KEY_PROTECTION;
            byte[] keyData = new BigInteger(encryptionKey, 16).toByteArray();
            SecretKeySpec key = new SecretKeySpec(keyData, "AES");
            KeyStore.SecretKeyEntry skEntry =
                new KeyStore.SecretKeyEntry(key);
            ks.setEntry("encryptionKey", skEntry, 
                new KeyStore.PasswordProtection(protection.toCharArray()));
            File file = this.getEncryptionFile();
            if(!file.exists())
            {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            ks.store(fos, storePassword.toCharArray());
        }
        catch(Exception e)
        {
            LOGGER.warn("Exception encountered while trying to store the key : " + e.getMessage());
        }
    }
    
    /**
     * Retrieve the encryption key.
     * 
     * @param ks Keystore where the key is stored
     * @return encryption key
     */
    public SecretKeySpec retrieveEncryptionKey(KeyStore ks)
    {
        String protection = ENCRYPTION_KEY_PROTECTION;
        try
        {
            KeyStore.SecretKeyEntry pkEntry = (KeyStore.SecretKeyEntry)
                ks.getEntry("encryptionKey", new KeyStore.PasswordProtection(protection.toCharArray()));
            SecretKeySpec mySecretKey = (SecretKeySpec) pkEntry.getSecretKey();
            return mySecretKey;
        }
        catch(Exception e)
        {
            LOGGER.warn("Exception encountered while trying to retrieve the encryption key : " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates a random encryption key.
     * 
     * @return the encryption key as a string
     */
    private String generateRandomKey()
    {
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey key = keyGenerator.generateKey();
            byte[] encoded = key.getEncoded(); 
            String data = new BigInteger(encoded).toString(16);
            return data;
        }
        catch(Exception e)
        {
            LOGGER.warn("Exception encountered while generating the encryption key : " + e.getMessage());
            return null;
        }
    }
}
