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
package org.xwiki.rendering.macro.signable;

import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;

/**
 * Base Class for signable macros.
 * <p>
 * It concerns every script macro and the special global 'sign' macro
 * </p>
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$
 * @since 5.3
 */
public abstract class AbstractSignableMacro<P extends SignableMacroParameters> extends AbstractMacro<P>
{
    /**
     * @param macroName the name of the macro (eg "groovy")
     */
    public AbstractSignableMacro(String macroName)
    {
        super(macroName, null, SignableMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     */
    public AbstractSignableMacro(String macroName, String macroDescription)
    {
        super(macroName, macroDescription, SignableMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     */
    public AbstractSignableMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor)
    {
        super(macroName, macroDescription, contentDescriptor, SignableMacroParameters.class);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractSignableMacro(String macroName, String macroDescription,
        Class< ? extends SignableMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, parametersBeanClass);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }

    /**
     * @param macroName the name of the macro (eg "groovy")
     * @param macroDescription the text description of the macro.
     * @param contentDescriptor the description of the macro content.
     * @param parametersBeanClass class of the parameters bean for this macro.
     */
    public AbstractSignableMacro(String macroName, String macroDescription, ContentDescriptor contentDescriptor,
        Class< ? extends SignableMacroParameters> parametersBeanClass)
    {
        super(macroName, macroDescription, contentDescriptor, parametersBeanClass);

        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
    }
}
