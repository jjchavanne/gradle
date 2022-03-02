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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.reporting.Reporting
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf


class PrepareReportForCIPublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project.rootProject == project) { "This plugin should be applied to root project!" }
        project.gradle.taskGraph.whenReady {
            val prepareReportForCIPublishingBuildService = project.gradle.sharedServices.registerIfAbsent("prepareReportForCIPublishingBuildService", PrepareReportForCIPublishingBuildService::class.java) {
                parameters.rootBuildDir.set(project.buildDir)
                parameters.taskPathToHtmlReport.putAll(
                    this@whenReady.allTasks.filter { it is Reporting<*> }.associate { it.path to (it as Reporting<*>).reports["html"].outputLocation.get().asFile }
                )
            }
            project.gradle.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(prepareReportForCIPublishingBuildService)
        }
    }
}
