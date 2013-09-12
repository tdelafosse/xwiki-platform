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
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.signedScripts.internal.DefaultSignatureGenerator;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
* Unit tests for {@link SignatureGenerator}.
*
* @version $Id$
*/
public class SignatureGeneratorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultSignatureGenerator> mocker =
        new MockitoComponentMockingRule<DefaultSignatureGenerator>(DefaultSignatureGenerator.class);
    
    /** Reference to the admin user. */
    private final DocumentReference adminRef = new DocumentReference("xwiki", "XWiki", "Admin");
    
    private DefaultSignatureGenerator signatureGenerator;
    
    private KeyPairHandler keyPairHandler;
    
    private DocumentAccessBridge dab;

    @Before
    public void setUp() throws Exception
    {
        this.signatureGenerator = this.mocker.getComponentUnderTest();
        
        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getCurrentUserReference()).thenReturn(adminRef);
        
        this.keyPairHandler = this.mocker.getInstance(KeyPairHandler.class);
        PrivateKey privateKey = getTestPrivateKey();
        when(keyPairHandler.getPrivateKey(Mockito.any(String.class))).thenReturn(privateKey);
    }

    @Test
    public void signatureGeneratorTest()
    {
        String content = "My test content";
        String result = signatureGenerator.computeSignature(content, "test");
        Assert.assertEquals(result, "N+G4lhFwEWfb5/2lZ+ebgvhTaAR6zq3hdwxj6Ba8kDbmBlUnrNp2H2uEMxOmOGeyoyDXCFbZDiIJlaVZR1oQim0JEqwM06F0UJQS7Z0Pc9MOgxvqcGXP8fTYX++e6ULzPyG1SY59p5nov9aymLsy34TWak5lXWxXk9tG4THCcbRNjCPwBR/o9RSPNLWWjN5wJYpdWdtjRKbPDuLpz7SEtXLeIVgJwBsPquzbryPMiRmR2TOm1vpJEOTTjejaAG5ofvsPFyUzO4KMKztpfxjMy7lfkMGdEmAXbl/6yg51gV5XQQXxwDHJzJZUdxgXtJwhZneXwfZN6MVU6e5/6OzAgg==");
    }
    
    private PrivateKey getTestPrivateKey()
    {
        try {
            String path = "src/test/resources/private/test";
            File privateKeyFile = new File(path);
            InputStream in = new FileInputStream(privateKeyFile);
            ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            oin.close();
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePrivate(keySpec);
        } catch (Exception e) {
            return null;
        }
    }
}
