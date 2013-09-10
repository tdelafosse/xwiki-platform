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

import java.security.PublicKey;
import java.security.Signature;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.codec.binary.Base64; 
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.signedScripts.KeyPairHandler;
import org.xwiki.signedScripts.SignatureVerifier;
import org.xwiki.signedScripts.SignedScriptsAuthorizationContext;

/**
 * The default signature verifier.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Singleton
public class DefaultSignatureVerifier implements SignatureVerifier
{
    /** Separator. */
    private static final String SEPARATOR = " : ";
    
    /** String indicating that we are checking the signature of a script. */
    private static final String SCRIPT_MODE = "script";
    
    /** String indicating that we are checking the signature of a sign macro. */
    private static final String SIGN_MACRO_MODE = "signMacro";
    
    /** Name of the property where the id is stored. */
    private static final String ID_PROP = "id";
    
    /** Name of the property where the name of the certificate is stored. */
    private static final String CERTIFICATE_PROP = "certificate";
    
    /** Name of the property where the signature is stored. */
    private static final String SIGNATURE_PROP = "signature";
    
    /** Name of the property where the author of the signature is stored. */
    private static final String AUTHOR_PROP = "author";
    
    /** Signature class. */
    private final DocumentReference classRef = new DocumentReference("xwiki", "SignedScripts", "SignatureClass");
    
    /** Logger. */
    @Inject
    private Logger logger;
    
    /** Key handler. */
    @Inject
    private KeyPairHandler keyHandler;
    
    /** Authorization context. */
    @Inject
    private SignedScriptsAuthorizationContext authorizationContext;
    
    /**
     * Used to find signature objects.
     */
    @Inject
    private org.xwiki.bridge.DocumentAccessBridge documentAccessBridge;

    /**
     * To access the script context.
     */
    @Inject
    private Execution execution;
    
    /**
     * To get doc reference.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;
    
    @Override
    public DocumentReference getSigner(String id, String content, String contentDoc)
    {
        try {
            // Reference to the document containing the script.
            DocumentReference scriptDoc;
            if (contentDoc.equals("")) {
                scriptDoc = documentAccessBridge.getCurrentDocumentReference();
            } else {
                scriptDoc = resolver.resolve(contentDoc);
            }
            int n = documentAccessBridge.getObjectNumber(scriptDoc, classRef, ID_PROP, id);
            // If there is no corresponding object, it means the script hasn't been signed.
            if (n == -1) {
                return null;
            }
            String certificate = documentAccessBridge.getProperty(scriptDoc, classRef, n, CERTIFICATE_PROP).toString();
            String signature = documentAccessBridge.getProperty(scriptDoc, classRef, n, SIGNATURE_PROP).toString();
            String author = documentAccessBridge.getProperty(scriptDoc, classRef, n, AUTHOR_PROP).toString();
            DocumentReference authorRef = resolver.resolve(author);
            String signedContent = authorRef.toString() + SEPARATOR + content;
            if (verifyPureSignature(signature, signedContent, certificate)) {
                return authorRef;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public boolean verifyScriptSignature(String id, String content, String contentDoc, boolean putInAuthContext)
    {
        return verifySignature(id, content, contentDoc, SCRIPT_MODE, putInAuthContext);
    }
    
    @Override
    public boolean verifySignMacroSignature(String id, String content, String contentDoc, boolean putInAuthContext)
    {
        return verifySignature(id, content, contentDoc, SIGN_MACRO_MODE, putInAuthContext);
    }
    
    protected boolean verifySignature(
        String id, String content, String contentDoc, String mode, boolean putInAuthContext)
    {
        try {
            logger.debug("Verifying signature for content : " + content);
            // Reference to the document containing the script.
            DocumentReference scriptDoc;
            if (contentDoc.equals("")) {
                scriptDoc = documentAccessBridge.getCurrentDocumentReference();
            } else {
                scriptDoc = resolver.resolve(contentDoc);
            }
            logger.debug("Content doc is : " + scriptDoc.toString());
            int n = documentAccessBridge.getObjectNumber(scriptDoc, classRef, ID_PROP, id);
            // If there is no corresponding object, it means the script hasn't been signed.
            if (n == -1) {
                return false;
            }
            String certificate = documentAccessBridge.getProperty(scriptDoc, classRef, n, CERTIFICATE_PROP).toString();
            String signature = documentAccessBridge.getProperty(scriptDoc, classRef, n, SIGNATURE_PROP).toString();
            String author = documentAccessBridge.getProperty(scriptDoc, classRef, n, AUTHOR_PROP).toString();
            DocumentReference authorRef = resolver.resolve(author);
            logger.debug("Author ref is : " + authorRef.toString());
            String signedContent = authorRef.toString() + SEPARATOR + content;
            if (verifyPureSignature(signature, signedContent, certificate)) {
                if (!putInAuthContext) {
                    return true;
                }
                if (SCRIPT_MODE == mode) {
                    authorizationContext.pushEntry(authorRef);
                } else if (SIGN_MACRO_MODE == mode) {
                    authorizationContext.enteringSignMacro(scriptDoc, authorRef);
                }
                return true;
            } else {
                logger.warn("The signature is incorrect !");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifying the signature of a content.
     * 
     * @param signature Signature to verify
     * @param content Content that has been signed
     * @param filename Name of the file where the public key is stored
     * @return true if the signature is correct
     */
    protected boolean verifyPureSignature(String signature, String content, String filename)
    {
        logger.debug("Found signature object");
        byte[] decodedSignature = Base64.decodeBase64(signature);
        try {
            PublicKey publicKey = keyHandler.getPublicKey(filename);
            Signature verifySignature = Signature.getInstance("SHA256withRSA");
            verifySignature.initVerify(publicKey);  

            //update signature with signature data.  
            verifySignature.update(content.getBytes());  

            //verify signature  
            return verifySignature.verify(decodedSignature);
        } catch (Exception e) {
            logger.warn("An exception occured while trying to verify signature");
            return false;
        }
    }
}
