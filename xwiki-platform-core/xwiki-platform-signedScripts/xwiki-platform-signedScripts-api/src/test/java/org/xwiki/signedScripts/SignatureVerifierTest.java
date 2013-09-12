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

package org.xwiki.signedScripts;

import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.signedScripts.internal.DefaultSignatureVerifier;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
* Unit tests for {@link SignatureVerifier}.
*
* @version $Id$
*/
public class SignatureVerifierTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultSignatureVerifier> mocker =
        new MockitoComponentMockingRule<DefaultSignatureVerifier>(DefaultSignatureVerifier.class);
    
    /** Signature class. */
    private final DocumentReference classRef = new DocumentReference("xwiki", "SignedScripts", "SignatureClass");
    
    private final DocumentReference testRef = new DocumentReference("xwiki", "SignedScripts", "Test");
    
    private final DocumentReference adminRef = new DocumentReference("xwiki", "XWiki", "Admin");
    
    private DefaultSignatureVerifier signatureVerifier;
    
    private KeyPairHandler keyPairHandler;
    
    private SignedScriptsAuthorizationContext authorizationContext;
    
    private DocumentAccessBridge dab;
    
    private DocumentReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        this.signatureVerifier = this.mocker.getComponentUnderTest();
        
        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getCurrentDocumentReference()).thenReturn(testRef);
        int n = 0;
        when(dab.getObjectNumber(testRef, classRef, "id", "myContent")).thenReturn((Integer)n);
        int N = -1;
        when(dab.getObjectNumber(testRef, classRef, "id", "mySecondContent")).thenReturn((Integer)N);
        when(dab.getProperty(testRef, classRef, n, "certificate")).thenReturn("test");
        when(dab.getProperty(testRef, classRef, n, "signature")).thenReturn("N+G4lhFwEWfb5/2lZ+ebgvhTaAR6zq3hdwxj6Ba8kDbmBlUnrNp2H2uEMxOmOGeyoyDXCFbZDiIJlaVZR1oQim0JEqwM06F0UJQS7Z0Pc9MOgxvqcGXP8fTYX++e6ULzPyG1SY59p5nov9aymLsy34TWak5lXWxXk9tG4THCcbRNjCPwBR/o9RSPNLWWjN5wJYpdWdtjRKbPDuLpz7SEtXLeIVgJwBsPquzbryPMiRmR2TOm1vpJEOTTjejaAG5ofvsPFyUzO4KMKztpfxjMy7lfkMGdEmAXbl/6yg51gV5XQQXxwDHJzJZUdxgXtJwhZneXwfZN6MVU6e5/6OzAgg==");
        when(dab.getProperty(testRef, classRef, n, "author")).thenReturn("xwiki:XWiki.Admin");
        
        this.authorizationContext = this.mocker.getInstance(SignedScriptsAuthorizationContext.class);
        
        this.resolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve("xwiki:XWiki.Admin")).thenReturn(adminRef);
        
        this.keyPairHandler = this.mocker.getInstance(KeyPairHandler.class);
        PublicKey publicKey = getTestPublicKey();
        when(keyPairHandler.getPublicKey(Mockito.any(String.class))).thenReturn(publicKey);
        
        this.logger = this.mocker.getMockedLogger();
    }

    @Test
    public void signatureScriptVerificationTest()
    {
        String content = "My test content";
        boolean result = signatureVerifier.verifyScriptSignature("myContent", content, "", true);
        Mockito.verify(authorizationContext).pushEntry(adminRef);
        Assert.assertTrue(result);
    }
    
    @Test
    public void signatureScriptVerificationFailTest()
    {
        String content = "My second test content";
        boolean result = signatureVerifier.verifyScriptSignature("mySecondContent", content, "", true);
        Assert.assertFalse(result);
    }
    
    @Test
    public void signatureScriptVerificationWithoutPutTest()
    {
        String content = "My test content";
        boolean result = signatureVerifier.verifyScriptSignature("myContent", content, "", false);
        Mockito.verify(authorizationContext, Mockito.never()).pushEntry(Mockito.any(DocumentReference.class));
        Assert.assertTrue(result);
    }
    
    @Test
    public void signatureMacroSignVerificationTest()
    {
        String content = "My test content";
        boolean result = signatureVerifier.verifySignMacroSignature("myContent", content, "", true);
        Mockito.verify(authorizationContext).enteringSignMacro(testRef, adminRef);
        Assert.assertTrue(result);
    }
    
    @Test
    public void signatureMacroSignVerificationWithoutPutTest()
    {
        String content = "My test content";
        boolean result = signatureVerifier.verifySignMacroSignature("myContent", content, "", false);
        Mockito.verify(authorizationContext, Mockito.never()).enteringSignMacro(
            Mockito.any(DocumentReference.class), Mockito.any(DocumentReference.class));
        Assert.assertTrue(result);
    }
    
    @Test
    public void getSignerTest()
    {
        String content = "My test content";
        DocumentReference signer = signatureVerifier.getSigner("myContent", content, "");
        Assert.assertEquals(adminRef, signer);
    }
    
    private PublicKey getTestPublicKey()
    {
        try {
            String path = "src/test/resources/public/test";
            File publicKeyFile = new File(path);
            InputStream in = new FileInputStream(publicKeyFile);
            ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            oin.close();
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
            KeyFactory fact = KeyFactory.getInstance("RSA");    
            return fact.generatePublic(keySpec);
        } catch (Exception e) {
            return null;
        }
    }
}
