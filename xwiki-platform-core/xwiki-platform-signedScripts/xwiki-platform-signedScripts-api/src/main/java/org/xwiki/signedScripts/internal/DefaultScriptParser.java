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
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.script.AbstractScriptMacro;
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
     * Parser to find macro in the document content.
     */
    @Inject
    @Named("xwiki/2.1")
    private Parser parser;
    
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
    
    @Override
    public Map<String, String> findScripts()
    {
        Map<String, String> scriptsFound = new HashMap<String, String>();
        try {
            DocumentReference currentDocRef = documentAccessBridge.getCurrentDocumentReference();
            // Let's get the content of the actual document, where we should look for scripts to sign
            String content = documentAccessBridge.getDocument(currentDocRef).getContent();
            logger.warn("Current content is : " + content);
            XDOM xdom = parser.parse(new StringReader(content));
            List<MacroBlock> macroBlocks = 
                xdom.getBlocks(new ClassBlockMatcher(MacroBlock.class), Block.Axes.DESCENDANT);
            logger.warn("We have " + macroBlocks.size() + " macro blocks");
            // For each macro in the content, let's find the ones corresponding to scripts
            for (MacroBlock block : macroBlocks) {
                if (couldBeSigned(block)) {
                    scriptsFound.put(block.getParameter(ID), block.getContent());
                }
            }
        } catch (ParseException e) {
            logger.warn("Failed to parse content : " + e.toString());
        } catch (Exception e) {
            logger.warn("Failed to get content : " + e.toString());
        }
        return scriptsFound;
    }
    
    /**
     * Method to determine whether a given script should asked to be signed.
     * 
     * @param block The block containing the script
     * @return true if we should propose the user to sign this script
     */
    private boolean couldBeSigned(MacroBlock block)
    {
        try {
            // First let's verify that this macro is a script macro and that it contains an id parameter.
            Macro< ? > macro = this.macroManager.getMacro(new MacroId(block.getId()));
            if (macro instanceof AbstractScriptMacro && block.getParameters().containsKey(ID)) {
                DocumentReference currentDocRef = documentAccessBridge.getCurrentDocumentReference();
                String id = block.getParameter(ID);
                String content = block.getContent();
                // If the script is already signed, no need to ask to sign it again
                // TODO : We should be able to resign a script if it's someone else who signed it.
                return !signatureVerifier.verifySignature(id, content, currentDocRef.toString());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

