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
package org.xwiki.rendering.macro.descriptor;

import java.util.Map;

import org.xwiki.rendering.macro.parameter.MacroParameters;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;

/**
 * 
 * @param <P>
 * @version $Id$
 * @since 1.6M1
 */
public interface MacroDescriptor<P extends MacroParameters>
{
    /**
     * @return the description of the macro.
     */
    String getDescription();

    /**
     * @param <D> the type of MacroParameterClass child class to return.
     * @param name the name of the parameter.
     * @return the parameter class.
     */
    <D extends MacroParameterDescriptor< ? >> D getParameterDescriptor(String name);

    /**
     * Create a new {@link MacroParameters} which parse provided parameters to transform it in more java usable types
     * (line int, boolean, etc.).
     * 
     * @param parameters the prameters to parse.
     * @return a new {@link MacroParameters}.
     */
    P createMacroParameters(Map<String, String> parameters);
}
