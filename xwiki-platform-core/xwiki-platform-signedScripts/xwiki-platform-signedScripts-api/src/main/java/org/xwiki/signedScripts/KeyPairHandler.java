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

import java.security.PrivateKey;
import java.security.PublicKey;

import org.xwiki.component.annotation.Role;

/**
 * This component is used to generate a key pair that would then be used for signing scripts. 
 * @version $Id$
 * @since 5.1RC1
 */
@Role
public interface KeyPairHandler
{
    /**
     * Generate the key pair.
     * 
     * @param filename Name of the file the generated keys should be stored
     */
    void generateKeyPair(String filename);
    
    /**
     * Retrieve a public key.
     * 
     * @param filename Name of the file the key is stored in
     * @return the public key
     */
    PublicKey getPublicKey(String filename);
    
    /**
     * Retrieve a private key.
     * 
     * @param filename Name of the file the key is stored in
     * @throws Exception if an error occurred while trying to retrieve the key
     * @return the private key
     */
    PrivateKey getPrivateKey(String filename)throws Exception;
}
