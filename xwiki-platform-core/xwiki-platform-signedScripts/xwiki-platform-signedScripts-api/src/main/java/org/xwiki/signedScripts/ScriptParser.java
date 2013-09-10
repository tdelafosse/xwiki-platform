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

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * This component is used to parse the DOM for finding scripts to sign.
 * @version $Id$
 * @since 5.1RC1
 */
@Role
public interface ScriptParser
{
    /**
     * Find the scripts to sign from a page content.
     * 
     * @param currentDocRef Reference of the document to find scripts in
     * @return A map containing the id and the content of every script to sign 
     */
    Map<String, String> findScripts(DocumentReference currentDocRef);
    
    /**
     * Find the scripts to sign from a page content.
     * 
     * @param content Content to find scripts in
     * @param syntaxId Id of the content syntax
     * @param docRef Reference to the document containing the scripts and their signatures
     * @return A map containing the id and the content of every script to sign 
     */
    Map<String, String> findScripts(String content, String syntaxId, DocumentReference docRef);
    
}
