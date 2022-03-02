/*
 * Copyright 2022 the original author or authors.
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

package gradlebuild.ci

import gradlebuild.testcleanup.prepareReportForCIPublishing
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import java.io.File
import javax.inject.Inject


/**
 * This build service collects all failed tasks' reports and puts them into rootProjectBuildDir,
 * so that TeamCity can publish them as artifacts.
 */
abstract class PrepareReportForCIPublishingBuildService @Inject constructor(
    private val fileSystemOperations: FileSystemOperations
) : BuildService<PrepareReportForCIPublishingBuildService.Params>, AutoCloseable, OperationCompletionListener {
    interface Params : BuildServiceParameters {
        val rootBuildDir: DirectoryProperty

        /**
         * Key is the path of a task, value is the HTML report file it generates.
         */
        val taskPathToHtmlReport: MapProperty<String, File>
    }

    /**
     * Key is the failed task path, value is the report file.
     */
    private
    val failedTaskPathToReportFile: MutableMap<String, File> = mutableMapOf()

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent || event.result !is TaskFailureResult) {
            return
        }
        val taskPath = event.descriptor.taskPath
        val htmlReport = parameters.taskPathToHtmlReport.get()[taskPath] ?: return
        failedTaskPathToReportFile[taskPath] = htmlReport
    }

    override fun close() {
        failedTaskPathToReportFile.forEach { (taskPath, reportFile) ->
            val projectName = taskPath.split(":").filter { it.isNotEmpty() }.first()
            prepareReportForCIPublishing(reportFile, parameters.rootBuildDir.get().asFile, projectName, fileSystemOperations)
        }
    }
}
