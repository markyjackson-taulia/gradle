/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.api.internal.project;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.internal.progress.BuildOperationDescriptor;

public interface ProjectConfigurator {
    Project project(Project project, Closure<? super Project> configureClosure);

    Project project(Project project, Action<? super Project> configureAction);

    void projectBuildOperation(ConfigureProjectBuildOperation configureProjectBuildOperation);

    void subprojects(Iterable<Project> projects, Closure<? super Project> configureClosure);

    void subprojects(Iterable<Project> projects, Action<? super Project> configureAction);

    void allprojects(Iterable<Project> projects, Closure<? super Project> configureClosure);

    void allprojects(Iterable<Project> projects, Action<? super Project> configureAction);

    Project rootProject(Project project, Action<Project> buildOperationExecutor);

    abstract class ConfigureProjectBuildOperation implements RunnableBuildOperation {
        private Project project;

        public ConfigureProjectBuildOperation(Project project) {
            this.project = project;
        }

        @Override
        public BuildOperationDescriptor.Builder description() {
            String name = "Configure project " + ((ProjectInternal) project).getIdentityPath().toString();
            return BuildOperationDescriptor.displayName(name);
        }
    }
}
