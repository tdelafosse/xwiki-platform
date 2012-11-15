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
package org.xwiki.extension.xar.internal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.internal.RepositoryUtils;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.extension.xar.internal.util.XarExtensionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Local repository proxy for XAR extensions.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
@Named(XarExtensionHandler.TYPE)
public class XarInstalledExtensionRepository extends AbstractExtensionRepository implements
    InstalledExtensionRepository, Initializable
{
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ExtensionInstalledEvent(),
        new ExtensionUninstalledEvent(), new ExtensionUpgradedEvent());

    @Inject
    private transient InstalledExtensionRepository installedRepository;

    @Inject
    private transient Packager packager;

    @Inject
    private transient ObservationManager observation;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    private Map<ExtensionId, XarInstalledExtension> extensions =
        new ConcurrentHashMap<ExtensionId, XarInstalledExtension>();

    private Map<String, Map<XarEntry, Set<ExtensionId>>> documentExtensions =
        new ConcurrentHashMap<String, Map<XarEntry, Set<ExtensionId>>>();

    @Override
    public void initialize() throws InitializationException
    {
        setDescriptor(new DefaultExtensionRepositoryDescriptor(XarExtensionHandler.TYPE, XarExtensionHandler.TYPE,
            this.installedRepository.getDescriptor().getURI()));

        loadExtensions();

        this.observation.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object arg1, Object arg2)
            {
                ExtensionEvent extensionEvent = (ExtensionEvent) event;
                InstalledExtension extension = (InstalledExtension) arg1;

                try {
                    if (extension.getType().equals(XarExtensionHandler.TYPE)) {
                        if (extensionEvent instanceof ExtensionUninstalledEvent) {
                            removeXarExtension(XarExtensionUtils.toWiki(extensionEvent.getNamespace()),
                                extensionEvent.getExtensionId());
                        } else if (extensionEvent instanceof ExtensionInstalledEvent) {
                            addXarExtension(XarExtensionUtils.toWiki(extensionEvent.getNamespace()), extension);
                        } else {
                            InstalledExtension previousExtension = (InstalledExtension) arg2;
                            removeXarExtension(XarExtensionUtils.toWiki(extensionEvent.getNamespace()),
                                previousExtension.getId());
                            addXarExtension(XarExtensionUtils.toWiki(extensionEvent.getNamespace()), extension);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to update XAR extensions index", e);
                }
            }

            @Override
            public String getName()
            {
                return XarInstalledExtensionRepository.class.getName();
            }

            @Override
            public List<Event> getEvents()
            {
                return EVENTS;
            }
        });
    }

    private void addXarExtension(InstalledExtension installedExtension) throws IOException
    {
        if (installedExtension.getNamespaces() != null) {
            addXarExtension("", installedExtension);
        } else {
            for (String namespace : installedExtension.getNamespaces()) {
                addXarExtension(XarExtensionUtils.toWiki(namespace), installedExtension);
            }
        }
    }

    private void addXarExtension(String wiki, InstalledExtension installedExtension) throws IOException
    {
        XarInstalledExtension xarExtension = this.extensions.get(installedExtension.getId());

        if (xarExtension == null) {
            xarExtension = new XarInstalledExtension(installedExtension, this, this.packager);
            this.extensions.put(installedExtension.getId(), xarExtension);
        }

        addXarExtension(wiki, xarExtension);
    }

    private void addXarExtension(String wiki, XarInstalledExtension xarExtension)
    {
        Map<XarEntry, Set<ExtensionId>> documents = this.documentExtensions.get(wiki);

        if (documents == null) {
            documents = new ConcurrentHashMap<XarEntry, Set<ExtensionId>>();
            this.documentExtensions.put(wiki, documents);
        }

        for (XarEntry entry : xarExtension.getPages()) {
            Set<ExtensionId> extensions = documents.get(entry);

            if (extensions == null) {
                extensions = Collections.newSetFromMap(new ConcurrentHashMap<ExtensionId, Boolean>());
                documents.put(entry, extensions);
            }

            extensions.add(xarExtension.getId());
        }
    }

    private void removeXarExtension(String wiki, ExtensionId extensionId)
    {
        XarInstalledExtension xarExtension = this.extensions.get(extensionId);

        if (!xarExtension.isInstalled()) {
            this.extensions.remove(extensionId);
        }

        Map<XarEntry, Set<ExtensionId>> documents = this.documentExtensions.get(wiki);

        if (documents != null) {
            for (XarEntry entry : xarExtension.getPages()) {
                Set<ExtensionId> extensions = documents.get(entry);

                if (extensions != null) {
                    extensions.remove(extensionId);
                }
            }
        }
    }

    private void loadExtensions()
    {
        for (InstalledExtension localExtension : this.installedRepository.getInstalledExtensions()) {
            if (localExtension.getType().equalsIgnoreCase(XarExtensionHandler.TYPE)) {
                try {
                    addXarExtension(localExtension);
                } catch (IOException e) {
                    this.logger.error("Failed to parse extension [{}]", localExtension.getId(), e);
                }
            }
        }
    }

    /**
     * @param documentReference the document reference
     * @return the XAR extensions associated to the provided document reference
     */
    public List<XarInstalledExtension> getExtensions(DocumentReference documentReference)
    {
        List<XarInstalledExtension> extensions;

        WikiReference wikiReference = documentReference.getWikiReference();

        Map<XarEntry, Set<ExtensionId>> documents = this.documentExtensions.get(wikiReference.getName());

        if (documents != null) {
            EntityReference localReference = documentReference.removeParent(wikiReference);

            Locale locale = documentReference.getLocale();
            if (locale == null) {
                locale = Locale.ROOT;
            }

            Set<ExtensionId> extensionIds = documents.get(new XarEntry(localReference, locale));

            extensions = new ArrayList<XarInstalledExtension>(extensionIds.size());
            for (ExtensionId extensionId : extensionIds) {
                XarInstalledExtension xarExtension = this.extensions.get(extensionId);

                extensions.add(xarExtension);
            }
        } else {
            extensions = Collections.emptyList();
        }

        return extensions;
    }

    // ExtensionRepository

    @Override
    public InstalledExtension resolve(ExtensionId extensionId) throws ResolveException
    {
        InstalledExtension extension = this.extensions.get(extensionId);

        if (extension == null) {
            throw new ResolveException("Extension [" + extensionId + "] does not exists or is not a xar extension");
        }

        return extension;
    }

    @Override
    public InstalledExtension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.resolve(extensionDependency);
        extension = this.extensions.get(extension.getId());

        if (extension == null) {
            throw new ResolveException("Extension [" + extensionDependency
                + "] does not exists or is not a xar extension");
        }

        return extension;
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return this.extensions.containsKey(extensionId);
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        return this.installedRepository.resolveVersions(id, offset, nb);
    }

    // LocalExtensionRepository

    @Override
    public int countExtensions()
    {
        return this.extensions.size();
    }

    @Override
    public Collection<InstalledExtension> getInstalledExtensions(String namespace)
    {
        List<InstalledExtension> installedExtensions = new ArrayList<InstalledExtension>(extensions.size());
        for (InstalledExtension localExtension : this.extensions.values()) {
            if (localExtension.isInstalled(namespace)) {
                installedExtensions.add(localExtension);
            }
        }

        return installedExtensions;
    }

    @Override
    public Collection<InstalledExtension> getInstalledExtensions()
    {
        return (Collection) this.extensions.values();
    }

    @Override
    public InstalledExtension getInstalledExtension(ExtensionId extensionId)
    {
        return this.extensions.get(extensionId);
    }

    @Override
    public InstalledExtension getInstalledExtension(String id, String namespace)
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        if (extension.getType().equals(XarExtensionHandler.TYPE)) {
            extension = this.extensions.get(extension.getId());
        } else {
            extension = null;
        }

        return extension;
    }

    @Override
    public InstalledExtension installExtension(LocalExtension extension, String namespace, boolean dependency)
        throws InstallException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void uninstallExtension(InstalledExtension extension, String namespace) throws UninstallException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<InstalledExtension> getBackwardDependencies(String id, String namespace) throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.getInstalledExtension(id, namespace);

        return extension.getType().equals(XarExtensionHandler.TYPE) ? this.installedRepository.getBackwardDependencies(
            id, namespace) : null;
    }

    @Override
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(ExtensionId extensionId)
        throws ResolveException
    {
        InstalledExtension extension = this.installedRepository.resolve(extensionId);

        return extension.getType().equals(XarExtensionHandler.TYPE) ? this.installedRepository
            .getBackwardDependencies(extensionId) : null;
    }

    @Override
    public IterableResult<Extension> search(String pattern, int offset, int nb) throws SearchException
    {
        return RepositoryUtils.searchInCollection(pattern, offset, nb, this.extensions.values());
    }
}
