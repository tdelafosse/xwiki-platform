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

import java.security.PrivateKey;
import java.security.Signature;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.signedScripts.KeyPairHandler;
import org.xwiki.signedScripts.SignatureGenerator;

/**
 * The default signature generator.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Singleton
public class DefaultSignatureGenerator implements SignatureGenerator
{   
    /**
     * Logger.
     */
    @Inject
    private Logger logger;
    
    /**
     * Component used for handling public and private key.
     */
    @Inject
    private KeyPairHandler keyHandler;
    
    /**
     * Used to find the signing user.
     */
    @Inject
    private org.xwiki.bridge.DocumentAccessBridge documentAccessBridge;
    
    @Override
    public String computeSignature(String content, String filename)
    {
        try {
            PrivateKey privateKey = keyHandler.getPrivateKey(filename);
            if (privateKey == null) {
                logger.warn("No keys there, generating a new pair");
                keyHandler.generateKeyPair(filename);
                privateKey = keyHandler.getPrivateKey(filename);
            }
            String user = documentAccessBridge.getCurrentUserReference().toString();
            String contentToSign = user + " : " + content;
            Signature instance = Signature.getInstance("SHA256withRSA");
            instance.initSign(privateKey);
            instance.update(contentToSign.getBytes());
            byte[] signature = instance.sign();
            return new String(Base64.encodeBase64(signature));
        } catch (Exception e) {
            logger.warn("Exception encountered while trying to sign the content : " + e.getMessage());
            return null;
        }
    }
}
