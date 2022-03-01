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
package org.gradle.integtests.tooling.r75

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.TestLauncher

@TargetGradleVersion(">=7.5")
@ToolingApiVersion(">=7.5")
class TestLauncherTaskExecutionCrossVersionSpec extends ToolingApiSpecification {

    def setup() {
        file('build.gradle') << """
            plugins {
                id 'java-library'
            }

            ${mavenCentralRepository()}

            dependencies {
                testImplementation 'junit:junit:4.13'
            }
        """
        file("src/test/java/MyTest.java") << '''
            public class MyTest {

                @org.junit.Test
                public void pass() {
                }

                // if the test task is executed without a filter then the build fails
                @org.junit.Test
                public void fail() {
                    throw new RuntimeException();
                }
            }
        '''
    }

    @TargetGradleVersion("<7.5")
    def "old Gradle version ignores task execution request"() {
        when:
        launchTestWithTestFilter { tl ->
            tl.forTasks(':help')
        }

        then:
        !taskExecuted(":help")
    }

    def "can execute a build with TestLauncher"() {
        when:
        launchTestWithTestFilter { tl ->
            tl.forTasks(':help')
        }

        then:
        taskExecuted(":help")
    }

    def "can use task selectors"() {
        setup:
        settingsFile << '''
            rootProject.name = 'root'
            include 'a'
            include 'a:aa'
        '''
        buildFile << '''
            allprojects {
                tasks.register('foo')
            }
        '''
        file('a/aa').mkdirs()

        when:
        launchTestWithTestFilter { tl ->
            tl.forTasks('foo', 'test')
        }

        then:
        taskExecuted(":foo")
        taskExecuted(":a:foo")
        taskExecuted(":a:aa:foo")
    }

    def "can exclude tasks"() {
        setup:
        buildFile << '''
            def foo = tasks.register('foo')
            tasks.register('bar') {
                dependsOn foo
            }
        '''

        when:
        launchTestWithTestFilter { tl ->
            tl.forTasks('bar', 'test').addArguments('-x', 'foo')
        }

        then:
        taskExecuted(":bar")
        !taskExecuted(":foo")
    }

    def "if selected test overlaps with tasks then the filter still applies"() {
        when:
        launchTestWithTestFilter { tl ->
            tl.forTasks(':help', ':test')
        }

        then:
        taskExecuted(":help")
    }

    private def launchTestWithTestFilter(@DelegatesTo(TestLauncher) @ClosureParams(value = SimpleType, options = ["org.gradle.tooling.TestLauncher"]) Closure testLauncherSpec) {
        withConnection { connection ->
            def testLauncher = connection.newTestLauncher()
            testLauncher.withTaskAndTestMethods(':test', 'MyTest', ['pass'])
            collectOutputs(testLauncher)
            testLauncherSpec(testLauncher)
            testLauncher.run()
        }
    }

    def taskExecuted(String path) {
        stdout.toString().contains("Task ${path}")
    }
}
