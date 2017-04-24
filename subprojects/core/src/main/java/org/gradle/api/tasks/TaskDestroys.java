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

package org.gradle.api.tasks;

import org.gradle.api.file.FileCollection;

/**
 * Represents the files or directories that a {@link org.gradle.api.Task} destroys.
 */
public interface TaskDestroys {
    /**
     * Registers some files that this task destroys.
     *
     * @param paths The files that will be destroyed. The given paths are evaluated as per {@link org.gradle.api.Project#files(Object...)}.
     * @return this
     */
    TaskDestroys files(Object... paths);

    /**
     * Registers some input file for this task.
     *
     * @param path A file that will be destroyed. The given path is evaluated as per {@link org.gradle.api.Project#files(Object...)}.
     * @return this
     */
    TaskDestroys file(Object path);

    /**
     * Returns the files this task destroys.
     *
     * @return The files that will be destroyed. Returns an empty collection if this task has no input files.
     */
    FileCollection getFiles();
}
