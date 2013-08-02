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
 * Authorization context.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Role
public interface SignedScriptsAuthorizationContext
{
    /** The execution context key where the authorization context must be stored. */
    String EXECUTION_CONTEXT_KEY = "signedScripts_authorization_context";
    
    /** The execution context key where the authorization context document must be stored. */
    String EXECUTION_CONTEXT_DOC_KEY = "signedScripts_script_document";
    
    /**
     * To push an entry.
     * 
     * @param userRef Reference to the user rights should be granted according to.
     */
    void pushEntry(DocumentReference userRef);
    
    /**
     * To pop the last entry.
     */
    void popEntry();
    
    /**
     * Get the top entry.
     * 
     * @return last entry pushed.
     */
    DocumentReference getLastEntry();
    
    /**
     * Indicates whether the stack is empty.
     * 
     * @return true if the stack contains an entry.
     */
    boolean hasEntry();
}