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

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.signable.AbstractSignableMacro;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.signedScripts.ScriptParser;
import org.xwiki.signedScripts.SignatureVerifier;

/**
 * The default signature generator.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Singleton
public class DefaultScriptParser implements ScriptParser
{   
    /**
     * String representing the parameter "id" of the scripts to be signed.
     */
    private static final String ID = "id";
    
    /**
     * Logger.
     */
    @Inject
    private Logger logger;
    
    /**
     * Used to know whether a given script has already a correct signature or not.
     */
    @Inject
    private SignatureVerifier signatureVerifier;
    
    /**
     * Used to get the content to parse.
     */
    @Inject
    private org.xwiki.bridge.DocumentAccessBridge documentAccessBridge;
    
    /**
     * Handles macro registration and macro lookups. Injected by the Component Manager.
     */
    @Inject
    private MacroManager macroManager;
    
    @Inject
    private ComponentManager componentManager;
    
    @Override
    public Map<String, String> findScripts(DocumentReference currentDocRef)
    {
     // Let's get the content of the actual document, where we should look for scripts to sign
        try {
            String content = documentAccessBridge.getDocument(currentDocRef).getContent();
            String syntaxId = documentAccessBridge.getDocument(currentDocRef).getSyntax().toIdString();
            return findScripts(content, syntaxId, currentDocRef);
        } catch (Exception e) {
            logger.warn("Failed to get content : " + e.toString());
            return null;
        }
    }
    
    @Override
    public Map<String, String> findScripts(String content, String syntaxId, DocumentReference docRef)
    {
        //logger.warn("Content is : " + content);
        Map<String, String> scriptsFound = new HashMap<String, String>();
        String blockContent;
        try {
            Parser parser = componentManager.getInstance(Parser.class, syntaxId);
            XDOM xdom = parser.parse(new StringReader(content));
            List<MacroBlock> macroBlocks = 
                xdom.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT);
            // For each macro in the content, let's find the ones corresponding to scripts
            for (MacroBlock block : macroBlocks) {
                blockContent = block.getContent();
                if (couldBeSigned(block, docRef)) {
                    scriptsFound.put(block.getParameter(ID), blockContent);
                }
            }
        } catch (ParseException e) {
            logger.warn("Failed to parse content : " + e.toString());
        } catch (Exception e) {
            logger.warn("Got the following exception : " + e.toString());
        }
        logger.warn("Scripts found : " + scriptsFound.values());
        return scriptsFound;
    }
    
    /**
     * Method to determine whether a given script should asked to be signed.
     * 
     * @param block The block containing the script
     * @return true if we should propose the user to sign this script
     */
    private boolean couldBeSigned(MacroBlock block, DocumentReference docRef)
    {
        try {
            // First let's verify that this macro is a script macro and that it contains an id parameter.
            Macro< ? > macro = this.macroManager.getMacro(new MacroId(block.getId()));
            if (macro instanceof AbstractSignableMacro && block.getParameters().containsKey(ID)) {
                String id = block.getParameter(ID);
                String content = block.getContent();
                // If the script is already signed, no need to ask to sign it again
                // TODO : We should be able to resign a script if it's someone else who signed it.
                DocumentReference authorRef = signatureVerifier.getSigner(id, content, docRef.toString());
                DocumentReference currentUser = documentAccessBridge.getCurrentUserReference();
                if (null != authorRef || !currentUser.equals(authorRef)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

