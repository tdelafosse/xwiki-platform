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
package org.xwiki.display.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;

/**
 * Default {@link DocumentDisplayer} implementation.
 * 
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Singleton
public class DefaultDocumentDisplayer implements DocumentDisplayer
{
    /**
     * The component used to display the document title.
     */
    @Inject
    @Named("title")
    private DocumentDisplayer titleDisplayer;

    /**
     * The component used to display the document content.
     */
    @Inject
    @Named("content")
    private DocumentDisplayer contentDisplayer;

    /**
     * The component used to lookup a syntax specific implementation for content and title displayer.
     */
    @Inject
    private ComponentManager componentManager;
    
    /**
     * Logger.
     */
    @Inject
    private Logger logger;
    
    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters,
                        DocumentModelBridge contentDocument)
    {
        String syntaxId = document.getSyntax().toIdString();
        DocumentDisplayer displayer;
        if (parameters.isTitleDisplayed()) {
            logger.warn("Entering title display");
            try {
                displayer = componentManager.getInstance(DocumentDisplayer.class, "title/" + syntaxId);
                logger.warn("Displayer : " + displayer.toString());
            } catch (ComponentLookupException e) {
                displayer = titleDisplayer;
            }
        } else {
            try {
                displayer = componentManager.getInstance(DocumentDisplayer.class, "content/" + syntaxId);
            } catch (ComponentLookupException e) {
                displayer = contentDisplayer;
            }
        }
        return displayer.display(document, parameters, contentDocument);
    }

    @Override
    public XDOM display(DocumentModelBridge document, DocumentDisplayerParameters parameters)
    {
        return display(document, parameters, null);
    }

}
