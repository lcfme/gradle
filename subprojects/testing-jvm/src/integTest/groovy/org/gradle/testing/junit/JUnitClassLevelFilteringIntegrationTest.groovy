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


package org.gradle.testing.junit

import org.gradle.integtests.fixtures.DefaultTestExecutionResult
import org.gradle.integtests.fixtures.TargetCoverage
import org.gradle.testing.fixture.JUnitMultiVersionIntegrationSpec
import org.gradle.util.VersionNumber

import static org.gradle.testing.fixture.JUnitCoverage.JUNIT_VINTAGE
import static org.gradle.testing.fixture.JUnitCoverage.LARGE_COVERAGE

@TargetCoverage({ LARGE_COVERAGE + JUNIT_VINTAGE })
class JUnitClassLevelFilteringIntegrationTest extends JUnitMultiVersionIntegrationSpec {
    def "runs all tests for class instead of method when runner is not filterable"() {
        given:
        buildFile << """
            apply plugin: 'java'
            ${mavenCentralRepository()}
            dependencies { testImplementation '${dependencyNotation}' }
            test { use${testFramework}() }
        """
        file("src/test/java/FooTest.java") << """
            import org.junit.*;
            import org.junit.runner.*;
            @RunWith(SomeRunner.class)
            public class FooTest {
            }
        """
        file("src/test/java/SomeRunner.java") << """
            import org.junit.*;
            import org.junit.runner.*;
            import org.junit.runner.notification.*;
            public class SomeRunner extends Runner {
                Class<?> c;

                public SomeRunner(Class<?> c) {
                    this.c = c;
                }

                public Description getDescription() {
                    Description suite = Description.createSuiteDescription(c.getName());
                    suite.addChild(Description.createTestDescription(c, "pass"));
                    suite.addChild(Description.createTestDescription(c, "other"));
                    return suite;
                }

                public void run(RunNotifier notifier) {
                    for(Description d: getDescription().getChildren()) {
                        notifier.fireTestStarted(d);
                        notifier.fireTestFinished(d);
                    }
                }
            }
        """

        when:
        succeeds("test", "--tests", "FooTest.pass")

        then:
        testResult()
            .assertTestClassesExecuted("FooTest")
            .testClass("FooTest").assertTestsExecuted("other", "pass")

        when:
        def runsEntireTestClassOnMismatch = vintage && vintageEngineSupportsMethodSelectorsForSpockMethods(dependencyVersion)
        if (runsEntireTestClassOnMismatch) {
            succeeds("test", "--tests", "FooTest.ignored")
        } else {
            fails("test", "--tests", "FooTest.ignored")
        }

        then:
        if (runsEntireTestClassOnMismatch) {
            testResult()
                .assertTestClassesExecuted("FooTest")
                .testClass("FooTest").assertTestsExecuted("other", "pass")
        } else {
            failure.assertHasCause("No tests found for given includes: [FooTest.ignored]")
        }

        when:
        fails("test", "--tests", "NotFooTest.pass")

        then:
        failure.assertHasCause("No tests found for given includes: [NotFooTest.pass]")
    }

    private DefaultTestExecutionResult testResult() {
        new DefaultTestExecutionResult(testDirectory)
    }

    private static boolean vintageEngineSupportsMethodSelectorsForSpockMethods(String version) {
        VersionNumber.parse(version).baseVersion >= VersionNumber.parse("5.7.0")
    }
}
