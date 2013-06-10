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
package org.xwiki.url.internal.standard;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration options specific to the "Standard" URL implementation scheme.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Role
@Unstable
public interface StandardURLConfiguration
{
    /**
     * @return true if Entity URLs define the wiki they point to as part of the URL path (aka path-based) or false if
     *         the wiki is contained in the URL host (aka domain-based, e.g. "mywiki.server.com")
     */
    boolean isPathBasedMultiWiki();

    /**
     * @return the path prefix used when using path-based URLs for multiwiki (e.g. for a wiki named {@code mywiki}:
     *         {@code http://server/xwiki/&lt;prefix&gt;/mywiki/...})
     */
    String getWikiPathPrefix();
}
