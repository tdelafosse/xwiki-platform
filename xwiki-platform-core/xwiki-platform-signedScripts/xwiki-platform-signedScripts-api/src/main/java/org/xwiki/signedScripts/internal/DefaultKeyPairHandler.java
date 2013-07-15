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

package org.xwiki.signedScripts.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.signedScripts.KeyPairHandler;

/**
 * The default key pair handler.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Singleton
public class DefaultKeyPairHandler implements KeyPairHandler
{
    /**
     * RSA string.
     */
    private static final String RSA = "RSA";
    
    /**
     * Public string.
     */
    private static final String PUBLIC = "public";
    
    /**
     * Private string.
     */
    private static final String PRIVATE = "private";
    
    /**
     * Logger.
     */
    @Inject
    private Logger logger;
    
    /**
     * Environment, used to access the filesystem.
     */
    @Inject
    private Environment environment;
    
    @Override
    public void generateKeyPair(String filename)
    {
        try {
            logger.debug("Start generating key pair");
            KeyPair keyPair = generateRandomKeyPair();
            Key publicKey = keyPair.getPublic();
            Key privateKey = keyPair.getPrivate();
            
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            
            RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
            saveToFile(filename, PUBLIC, publicKeySpec.getModulus(), publicKeySpec.getPublicExponent());
            
            RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);
            saveToFile(filename, PRIVATE, privateKeySpec.getModulus(), privateKeySpec.getPrivateExponent());
            
            logger.debug("Key pair generated.");
        } catch (Exception e) {
            logger.warn("Exception encountered while trying to store the key pair : " + e.getMessage());
        }
    }

    @Override
    public PublicKey getPublicKey(String filename)
    {
        try {
            File file = this.getFile(filename, PUBLIC);
            if (!file.exists()) {
                return null;
            }
            InputStream in = new FileInputStream(file);
            ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
            try {
                BigInteger m = (BigInteger) oin.readObject();
                BigInteger e = (BigInteger) oin.readObject();
                RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
                KeyFactory fact = KeyFactory.getInstance(RSA);
                PublicKey publicKey = fact.generatePublic(keySpec);
                return publicKey;
            } catch (Exception e) {
                logger.warn("Error encountered while trying to retrieve public key");
                return null;
            } finally {
                oin.close();
            }
        } catch (Exception e) {
            logger.warn("Error encountered while trying to access the file where the public key is stored : "
                + e.getMessage());
            return null;
        }
    }
   
    @Override
    public PrivateKey getPrivateKey(String filename) throws Exception
    {
        File file = this.getFile(filename, PRIVATE);
        if (!file.exists()) {
            File publicFile = this.getFile(filename, PUBLIC);
            // If the public file exists, it probably means that this certificate is an import.
            if (!publicFile.exists()) {
                return null;
            }
        }
        InputStream in = new FileInputStream(file);
        ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
        try {
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
            KeyFactory fact = KeyFactory.getInstance(RSA);
            PrivateKey privateKey = fact.generatePrivate(keySpec);
            return privateKey;
        } catch (Exception e) {
            logger.warn("Error encountered while trying to retrieve private key");
            throw e;
        } finally {
            oin.close();
        }
    }
   
    /**
     * Generating a random key pair.
     * 
     * @return the pair key
     */
    private KeyPair generateRandomKeyPair()
    {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA);
            kpg.initialize(2048);
            KeyPair kp = kpg.genKeyPair();
            return kp;
        } catch (Exception e) {
            logger.warn("Exception encountered while generating the key pair : " + e.getMessage());
            return null;
        }
    }
   
    /**
     * Get the file where the key is supposed to be stored.  
     * 
     * @param filename Name of the file 
     * @param hint Specify whether it's a private or a public key
     * @return The file where the key is to be stored
     */
    private File getFile(String filename, String hint)
    {
        File permDir = environment.getPermanentDirectory();
        String path = permDir.getAbsolutePath() + File.separator + hint;
        File hintDir = new File(path);
        // If the private (resp. public) repository doesn't exist yet, let's create it.
        if (!hintDir.exists()) {
            hintDir.mkdir();
        }
        path = path + File.separator + filename;
        File encryptionFile = new File(path);
        return encryptionFile;
    }
    
    /**
     * Saving a private or public key.
     *  
     * @param filename Name of the file to save the key in
     * @param hint Specify whether this is a private or a public key
     * @param mod Modulus of the key
     * @param exp Exponent of the key
     * @throws Exception exception encountered while storing the key
     */
    private void saveToFile(String filename, String hint, BigInteger mod, BigInteger exp) throws Exception
    {
        File file = this.getFile(filename, hint);
        if (!file.exists()) {
            file.createNewFile();
        }
        ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            oout.writeObject(mod);
            oout.writeObject(exp);
        } catch (Exception e) {
            throw e;
        } finally {
            oout.close();
        }
    }
    
}
