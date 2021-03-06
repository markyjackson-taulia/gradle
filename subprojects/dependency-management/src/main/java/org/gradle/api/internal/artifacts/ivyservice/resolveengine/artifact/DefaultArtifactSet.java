/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.ModuleExclusion;
import org.gradle.api.internal.artifacts.transform.VariantSelector;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.AttributesSchemaInternal;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.specs.Spec;
import org.gradle.internal.Factory;
import org.gradle.internal.component.model.ComponentArtifactMetadata;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.internal.component.model.VariantMetadata;
import org.gradle.internal.resolve.resolver.ArtifactResolver;
import org.gradle.internal.resolve.result.DefaultBuildableArtifactResolveResult;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DefaultArtifactSet implements ArtifactSet {
    private final ComponentIdentifier componentIdentifier;
    private final ModuleVersionIdentifier moduleVersionIdentifier;
    private final ModuleSource moduleSource;
    private final ModuleExclusion exclusions;
    private final Set<? extends VariantMetadata> variants;
    private final AttributesSchemaInternal schema;
    private final ArtifactResolver artifactResolver;
    private final Map<ComponentArtifactIdentifier, ResolvedArtifact> allResolvedArtifacts;
    private final long id;
    private final ImmutableAttributesFactory attributesFactory;

    public DefaultArtifactSet(ComponentIdentifier componentIdentifier, ModuleVersionIdentifier ownerId, ModuleSource moduleSource, ModuleExclusion exclusions, Set<? extends VariantMetadata> variants, AttributesSchemaInternal schema,  ArtifactResolver artifactResolver, Map<ComponentArtifactIdentifier, ResolvedArtifact> allResolvedArtifacts, long id, ImmutableAttributesFactory attributesFactory) {
        this.componentIdentifier = componentIdentifier;
        this.moduleVersionIdentifier = ownerId;
        this.moduleSource = moduleSource;
        this.exclusions = exclusions;
        this.variants = variants;
        this.schema = schema;
        this.artifactResolver = artifactResolver;
        this.allResolvedArtifacts = allResolvedArtifacts;
        this.id = id;
        this.attributesFactory = attributesFactory;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public ResolvedArtifactSet select(Spec<? super ComponentIdentifier> componentFilter, VariantSelector selector) {
        return snapshot().select(componentFilter, selector);
    }

    @Override
    public ArtifactSet snapshot() {
        ImmutableSet.Builder<ResolvedVariant> result = ImmutableSet.builder();
        for (VariantMetadata variant : variants) {
            Set<? extends ComponentArtifactMetadata> artifacts = variant.getArtifacts();
            Set<ResolvedArtifact> resolvedArtifacts = new LinkedHashSet<ResolvedArtifact>(artifacts.size());

            // Add artifact format as an implicit attribute when all artifacts have the same format
            AttributeContainerInternal attributes = variant.getAttributes();
            if (!attributes.contains(ArtifactAttributes.ARTIFACT_FORMAT)) {
                String format = null;
                for (ComponentArtifactMetadata artifact : artifacts) {
                    String candidateFormat = artifact.getName().getType();
                    if (format == null) {
                        format = candidateFormat;
                    } else if (!format.equals(candidateFormat)) {
                        format = null;
                        break;
                    }
                }
                if (format != null) {
                    attributes = attributesFactory.concat(attributes.asImmutable(), ArtifactAttributes.ARTIFACT_FORMAT, format);
                }
            }

            for (ComponentArtifactMetadata artifact : artifacts) {
                IvyArtifactName artifactName = artifact.getName();
                if (exclusions.excludeArtifact(moduleVersionIdentifier.getModule(), artifactName)) {
                    continue;
                }

                ResolvedArtifact resolvedArtifact = allResolvedArtifacts.get(artifact.getId());
                if (resolvedArtifact == null) {
                    Factory<File> artifactSource = new LazyArtifactSource(artifact, moduleSource, artifactResolver);
                    resolvedArtifact = new DefaultResolvedArtifact(moduleVersionIdentifier, artifactName, artifact.getId(), artifact.getBuildDependencies(), artifactSource);
                    allResolvedArtifacts.put(artifact.getId(), resolvedArtifact);
                }
                resolvedArtifacts.add(resolvedArtifact);
            }
            result.add(ArtifactBackedResolvedVariant.create(attributes, resolvedArtifacts));
        }
        return new ArtifactSetSnapshot(id, componentIdentifier, result.build(), schema);
    }

    private static class ArtifactSetSnapshot implements ArtifactSet {
        private final long id;
        private final ComponentIdentifier componentIdentifier;
        private final Set<ResolvedVariant> variants;
        private final AttributesSchemaInternal schema;

        ArtifactSetSnapshot(long id, ComponentIdentifier componentIdentifier, Set<ResolvedVariant> variants, AttributesSchemaInternal schema) {
            this.id = id;
            this.componentIdentifier = componentIdentifier;
            this.variants = variants;
            this.schema = schema;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public ArtifactSet snapshot() {
            return this;
        }

        @Override
        public ResolvedArtifactSet select(Spec<? super ComponentIdentifier> componentFilter, VariantSelector selector) {
            if (!componentFilter.isSatisfiedBy(componentIdentifier)) {
                return ResolvedArtifactSet.EMPTY;
            } else {
                return selector.select(variants, schema);
            }
        }
    }

    private static class LazyArtifactSource implements Factory<File> {
        private final ArtifactResolver artifactResolver;
        private final ModuleSource moduleSource;
        private final ComponentArtifactMetadata artifact;

        private LazyArtifactSource(ComponentArtifactMetadata artifact, ModuleSource moduleSource, ArtifactResolver artifactResolver) {
            this.artifact = artifact;
            this.artifactResolver = artifactResolver;
            this.moduleSource = moduleSource;
        }

        public File create() {
            DefaultBuildableArtifactResolveResult result = new DefaultBuildableArtifactResolveResult();
            artifactResolver.resolveArtifact(artifact, moduleSource, result);
            return result.getResult();
        }
    }

}
