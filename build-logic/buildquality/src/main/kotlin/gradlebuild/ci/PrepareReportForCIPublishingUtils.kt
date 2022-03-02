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

package gradlebuild.testcleanup

import org.gradle.api.file.FileSystemOperations
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun prepareReportForCIPublishing(report: File, targetDir: File, projectName: String, fileSystemOperations: FileSystemOperations) {
    if (!report.exists()) {
        return
    }
    if (report.isDirectory) {
        val destFile = targetDir.resolve("report-$projectName-${report.name}.zip")
        zip(destFile, report)
    } else {
        fileSystemOperations.copy {
            from(report)
            into(targetDir)
            rename { "report-$projectName-${report.parentFile.name}-${report.name}" }
        }
    }
}


fun zip(destZip: File, srcDir: File) {
    destZip.parentFile.mkdirs()
    ZipOutputStream(FileOutputStream(destZip), StandardCharsets.UTF_8).use { zipOutput ->
        val srcPath = srcDir.toPath()
        Files.walk(srcPath).use { paths ->
            paths
                .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                .forEach { path ->
                    val zipEntry = ZipEntry(srcPath.relativize(path).toString())
                    zipOutput.putNextEntry(zipEntry)
                    Files.copy(path, zipOutput)
                    zipOutput.closeEntry()
                }
        }
    }
}
