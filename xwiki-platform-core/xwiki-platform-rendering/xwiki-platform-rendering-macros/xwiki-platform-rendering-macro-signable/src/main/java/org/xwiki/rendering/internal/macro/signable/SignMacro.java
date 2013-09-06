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
package org.xwiki.rendering.internal.macro.signable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.signable.AbstractSignableMacro;
import org.xwiki.rendering.macro.signable.SignableMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.signedScripts.SignatureVerifier;
import org.xwiki.signedScripts.SignedScriptsAuthorizationContext;

/**
 * Macro for signing content outside of scripts, or even signing a bunch of script in one shot.
 * 
 * @version $Id$
 * @since 5.3
 */
@Component
@Named("sign")
@Singleton
public class SignMacro extends AbstractSignableMacro<SignableMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Sign all the included content";
    
    @Inject 
    private SignatureVerifier signatureVerifier;
    
    @Inject
    private SignedScriptsAuthorizationContext authorizationContext;
    
    @Inject
    private Logger logger;
    
    @Inject
    private MacroContentParser contentParser;
    
    /**
     * Default constructor.
     */
    public SignMacro()
    {
        super("Sign", DESCRIPTION, SignableMacroParameters.class);

        // The sign macro must execute first since it should give PR to the macros it could possibly include.
        setPriority(10);
        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }
    @Override
    public List<Block> execute(SignableMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        logger.warn("Successfully entered sign macro");
        //If the content comes from another document, we should find this "source" in the context.
        String source;
        DocumentReference guestRef = new DocumentReference("xwiki", "XWiki", "Guest");
        if (context.getXDOM().getMetaData().getMetaData().containsKey(MetaData.SOURCE)) {
            source = context.getXDOM().getMetaData().getMetaData(MetaData.SOURCE).toString();
        } else {
            source = "";
        }
        String id = parameters.getId();
        if (id == null || !signatureVerifier.verifySignature(id, content, source)) {
            authorizationContext.pushEntry(guestRef);
        }
        List<Block> blocks = this.contentParser.parse(content, context, true, context.isInline()).getChildren();
        authorizationContext.popEntry();
        return blocks;
    }
}
