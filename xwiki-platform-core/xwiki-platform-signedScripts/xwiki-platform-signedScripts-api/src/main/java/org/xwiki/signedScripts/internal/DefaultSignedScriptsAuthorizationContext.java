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

import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.signedScripts.SignedScriptsAuthorizationContext;

/**
 * The default authorization context.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Singleton
public class DefaultSignedScriptsAuthorizationContext implements SignedScriptsAuthorizationContext,
             ExecutionContextInitializer
{   
    /** Execution. */
    @Inject
    private Execution execution;
    
    /** Logger. */
    @Inject
    private Logger logger;
    
    @Override
    public void initialize(ExecutionContext executionContext)
    {
        if (!executionContext.hasProperty(SignedScriptsAuthorizationContext.EXECUTION_CONTEXT_KEY)) {
            executionContext.newProperty(SignedScriptsAuthorizationContext.EXECUTION_CONTEXT_KEY)
                .makeFinal().inherited().initial(new Stack<DocumentReference>()).declare();
        }
        if (!executionContext.hasProperty(SignedScriptsAuthorizationContext.EXECUTION_CONTEXT_SIGN_MACRO_KEY)) {
            executionContext.newProperty(SignedScriptsAuthorizationContext.EXECUTION_CONTEXT_SIGN_MACRO_KEY)
                .makeFinal().inherited().initial(new Stack<MacroSignEntry>()).declare();
        }
    }
    
    @Override
    public void pushEntry(DocumentReference userRef)
    {
        logger.debug("Pushing entry : " + userRef.toString());
        Stack<DocumentReference> stackEntry = getStackEntry();
        stackEntry.push(userRef);
    }
    
    @Override
    public void popEntry()
    {
        logger.debug("Popping entry");
        Stack<DocumentReference> stackEntry = getStackEntry();
        if (!stackEntry.isEmpty()) {
            stackEntry.pop();
        }
    }
    
    @Override
    public DocumentReference getLastEntry()
    {
        Stack<DocumentReference> stackEntry = getStackEntry();
        return stackEntry.peek();
    }
    
    @Override
    public boolean hasEntry()
    {
        Stack<DocumentReference> stackEntry = getStackEntry();
        return !stackEntry.isEmpty();
    }
    
    @Override
    public void enteringSignMacro(DocumentReference docRef, DocumentReference userRef)
    {
        Stack<MacroSignEntry> signMacroStack = getSignMacroStackEntry();
        MacroSignEntry entry = new MacroSignEntry(docRef, userRef);
        signMacroStack.push(entry);
    }
    
    @Override
    public void exitingSignMacro()
    {
        Stack<MacroSignEntry> signMacroStack = getSignMacroStackEntry();
        signMacroStack.pop();
    }
    
    @Override
    public MacroSignEntry getLastMacroSign()
    {
        Stack<MacroSignEntry> signMacroStack = getSignMacroStackEntry();
        return signMacroStack.peek();
    }
    
    @Override
    public boolean isInsideMacroSign()
    {
        Stack<MacroSignEntry> signMacroStack = getSignMacroStackEntry();
        return !signMacroStack.isEmpty();
    }
    
    /**
     * Retrieving the stack entry.
     * 
     * @return the stack entry.
     */
    private Stack<DocumentReference> getStackEntry()
    {
        return (Stack<DocumentReference>) execution.getContext().getProperty(
            SignedScriptsAuthorizationContext.EXECUTION_CONTEXT_KEY);
    }
    
    /**
     * Retrieving the document stack entry.
     * 
     * @return the document stack entry.
     */
    private Stack<MacroSignEntry> getSignMacroStackEntry()
    {
        return (Stack<MacroSignEntry>) execution.getContext().getProperty(
            SignedScriptsAuthorizationContext.EXECUTION_CONTEXT_SIGN_MACRO_KEY);
    }
}
