/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact

import org.gradle.api.Buildable
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.internal.attributes.AttributeContainerInternal
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.progress.TestBuildOperationExecutor
import spock.lang.Specification

class ArtifactBackedResolvedVariantTest extends Specification {
    def variant = Mock(AttributeContainerInternal)
    def queue = new TestBuildOperationExecutor.TestBuildOperationQueue()
    def artifact1 = Mock(TestArtifact)
    def artifact2 = Mock(TestArtifact)

    def "visits empty variant"() {
        def visitor = Mock(ArtifactVisitor)
        def listener = Mock(ResolvedArtifactSet.AsyncArtifactListener)
        def set1 = of([])

        when:
        set1.artifacts.startVisit(queue, listener).visit(visitor)

        then:
        0 * _
    }

    def "visits artifacts and retains order when artifact files are not required"() {
        def visitor = Mock(ArtifactVisitor)
        def listener = Mock(ResolvedArtifactSet.AsyncArtifactListener)
        def set1 = of([artifact1, artifact2])
        def set2 = of([artifact1])

        when:
        set1.artifacts.startVisit(queue, listener).visit(visitor)

        then:
        _ * listener.requireArtifactFiles() >> false
        1 * visitor.visitArtifact(variant, artifact1)

        then:
        1 * visitor.visitArtifact(variant, artifact2)
        0 * _

        when:
        set2.artifacts.startVisit(queue, listener).visit(visitor)

        then:
        _ * listener.requireArtifactFiles() >> false
        1 * visitor.visitArtifact(variant, artifact1)
        0 * _
    }

    def "visits artifacts when artifact files are required"() {
        def visitor = Mock(ArtifactVisitor)
        def listener = Mock(ResolvedArtifactSet.AsyncArtifactListener)
        def f1 = new File("f1")
        def f2 = new File("f2")
        def set1 = of([artifact1, artifact2])
        def set2 = of([artifact1])

        when:
        def result = set1.artifacts.startVisit(queue, listener)

        then:
        _ * listener.requireArtifactFiles() >> true
        _ * artifact1.id >> Stub(ComponentArtifactIdentifier)
        _ * artifact2.id >> Stub(ComponentArtifactIdentifier)
        1 * artifact1.file >> f1
        1 * listener.artifactAvailable(artifact1)
        1 * artifact2.file >> f2
        1 * listener.artifactAvailable(artifact2)
        0 * _

        when:
        result.visit(visitor)

        then:
        1 * visitor.visitArtifact(variant, artifact1)

        then:
        1 * visitor.visitArtifact(variant, artifact2)
        0 * _

        when:
        def result2 = set2.artifacts.startVisit(queue, listener)

        then:
        _ * listener.requireArtifactFiles() >> true
        _ * artifact1.id >> Stub(ComponentArtifactIdentifier)
        1 * artifact1.file >> f1
        1 * listener.artifactAvailable(artifact1)
        0 * _

        when:
        result2.visit(visitor)

        then:
        1 * visitor.visitArtifact(variant, artifact1)
        0 * _
    }

    def "collects build dependencies"() {
        def deps1 = Stub(TaskDependency)
        def deps2 = Stub(TaskDependency)
        def set1 = of([artifact1, artifact2])
        def set2 = of([artifact1])

        given:
        artifact1.buildDependencies >> deps1
        artifact2.buildDependencies >> deps2

        when:
        def buildDeps = []
        set1.artifacts.collectBuildDependencies(buildDeps)

        then:
        buildDeps == [deps1, deps2]

        when:
        buildDeps = []
        set2.artifacts.collectBuildDependencies(buildDeps)

        then:
        buildDeps == [deps1]
    }

    ResolvedVariant of(artifacts) {
        return ArtifactBackedResolvedVariant.create(variant, artifacts)
    }

    interface TestArtifact extends ResolvedArtifact, Buildable { }

}
