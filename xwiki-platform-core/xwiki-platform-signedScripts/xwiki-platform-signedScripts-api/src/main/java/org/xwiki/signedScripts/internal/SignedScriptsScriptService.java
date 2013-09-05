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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.signedScripts.KeyPairHandler;
import org.xwiki.signedScripts.ScriptParser;
import org.xwiki.signedScripts.SignatureGenerator;
import org.xwiki.signedScripts.SignatureVerifier;
import org.xwiki.signedScripts.SignedScriptsAuthorizationContext;

/**
 * Service for accessing signature generator.
 * 
 * @version $Id$
 *
 */
@Component
@Named("signedScripts")
@Singleton
public class SignedScriptsScriptService implements ScriptService
{
    /** The signature generator. */
    @Inject
    private SignatureGenerator signatureGenerator;
    
    /** The key pair tool. */ 
    @Inject
    private KeyPairHandler keyPairHandler;
    
    /** Signature verifier. */
    @Inject
    private SignatureVerifier signatureVerifier;
    
    /** Authorization context. */
    @Inject
    private SignedScriptsAuthorizationContext authorizationContext;
    
    /** Script Parser. */
    @Inject 
    private ScriptParser scriptParser;
    
    /**
     * Compute signature.
     * 
     * @param content Content
     * @param filename Filename
     * @return signature
     */
    public String computeSignature(String content, String filename)
    {
        return signatureGenerator.computeSignature(content, filename);
    }
    
    /**
     * Generate a public and a private key.
     * 
     * @param filename Name of the file where these keys should be stored
     */
    public void generateKeyPair(String filename)
    {
        keyPairHandler.generateKeyPair(filename);
    }
    
    /**
     * Finding scripts.
     * 
     * @return Map of the scripts to sign
     */
    public Map<String, String> findScripts()
    {
        return scriptParser.findScripts();
    }
}
