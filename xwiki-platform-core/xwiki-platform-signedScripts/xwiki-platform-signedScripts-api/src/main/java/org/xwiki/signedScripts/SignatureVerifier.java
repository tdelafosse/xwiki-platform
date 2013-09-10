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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * This component is used to verify signatures. 
 * 
 * @version $Id$
 * @since 5.3
 */
@Role
public interface SignatureVerifier
{   
    /**
     * Get the user who signed the passed element.
     * 
     * @param id Id of the signed element
     * @param content Content signed
     * @param contentDoc Document containing the content
     * @return the user who signed the content passed if the signature is valid. Returns null otherwise.
     */
    DocumentReference getSigner(String id, String content, String contentDoc);
    
    /**
     * Verify the signature of the passed content.
     * 
     * @param id Id of the scripting macro
     * @param content Content of the macro
     * @param contentDoc Document containing the script and against where the signature objects are stored
     * @param pushInAuthorizationContext true if the corresponding object should be put in the authorization context 
     * @return true if this script has been correctly signed
     */
    boolean verifyScriptSignature(String id, String content, String contentDoc, boolean pushInAuthorizationContext);
    
    /**
     * Verify the signature of the passed content.
     * 
     * @param id Id of the sign macro
     * @param content Content of the macro
     * @param contentDoc Document containing the macro and against where the signature objects are stored
     * @param pushInAuthorizationContext true if the corresponding object should be put in the authorization context 
     * @return true if this macro has been correctly signed
     */
    boolean verifySignMacroSignature(String id, String content, String contentDoc, boolean pushInAuthorizationContext);
}
