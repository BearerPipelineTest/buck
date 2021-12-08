/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.buck.jvm.kotlin;

import com.facebook.buck.core.build.execution.context.IsolatedExecutionContext;
import com.facebook.buck.core.util.log.Logger;
import com.facebook.buck.event.AnnotationProcessorPerfStats;
import com.facebook.buck.event.AnnotationProcessorStatsEvent;
import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.event.KotlinPluginPerfStats;
import com.facebook.buck.event.KotlinPluginStatsEvent;
import com.facebook.buck.jvm.core.BuildTargetValue;
import com.facebook.buck.step.StepExecutionResult;
import com.facebook.buck.step.StepExecutionResults;
import com.facebook.buck.step.isolatedsteps.IsolatedStep;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Parses the performance stats report generated by Kapt, which contains data for all the annotation
 * processors that ran in the compilation.
 *
 * <p>The parsed data is posted to the supplied event bus using the AnnotationProcessorStatsEvent.
 *
 * <p>Example for a Kapt stats report:
 *
 * <pre>
 * Kapt Annotation Processing performance report:
 * com.udinic.MetagenProcessor: total: 1650 ms, init: 39 ms, 3 round(s): 1611 ms, 0 ms, 0 ms
 * com.udinic.ComponentsProcessor: total: 11 ms, init: 11 ms, 0 round(s):
 * com.udinic.InjectorProcessor: total: 390 ms, init: 17 ms, 3 round(s): 293 ms, 47 ms, 33 ms
 * General: totalTime: 3825 ms, initialAnalysis: 275 ms, stubs: 29 ms, annotationProcessing: 2104 ms
 * </pre>
 */
public class KaptStatsReportParseStep extends IsolatedStep {

  private static final Logger LOG = Logger.get(KaptStatsReportParseStep.class);

  private static final Pattern PATTERN_PROCESSOR_STATS_LINE =
      Pattern.compile(
          "([\\w.$]+)\\: total: (\\d+) ms, init: (\\d+) ms, (\\d+) round\\(s\\):([\\w\\d ,]*)");
  private static final Pattern PATTERN_ROUND_TIME = Pattern.compile(" (\\d+) ms");
  private static final Pattern PATTERN_GENERAL_STATS_LINE =
      Pattern.compile(
          "General\\: totalTime: (\\d+) ms, initialAnalysis: (\\d+) ms, stubs: (\\d+) ms, annotationProcessing: (\\d+) ms(.*)");

  final Path pathToReportFile;
  final BuildTargetValue invokingRule;
  final BuckEventBus eventBus;

  public KaptStatsReportParseStep(
      Path pathToReportFile, BuildTargetValue invokingRule, BuckEventBus eventBus) {
    this.pathToReportFile = pathToReportFile;
    this.invokingRule = invokingRule;
    this.eventBus = eventBus;
  }

  @Override
  public StepExecutionResult executeIsolatedStep(IsolatedExecutionContext context)
      throws IOException {
    List<String> lines = Files.readAllLines(pathToReportFile);

    LOG.debug(
        "Parsing Kapt stats report for rule[%s] reportPath[%s]",
        invokingRule.getFullyQualifiedName(), pathToReportFile);

    parseReport(lines);

    return StepExecutionResults.SUCCESS;
  }

  @VisibleForTesting
  void parseReport(List<String> lines) {
    // Starting from the second line, the first line is the report's title.
    for (int i = 1; i < lines.size(); i++) {
      AnnotationProcessorPerfStats annotationProcessorPerfStats = parseProcessorStats(lines.get(i));

      if (annotationProcessorPerfStats != null) {
        AnnotationProcessorStatsEvent statsEvent =
            new AnnotationProcessorStatsEvent(
                invokingRule.getFullyQualifiedName(), annotationProcessorPerfStats);
        eventBus.post(statsEvent);
      } else {
        KotlinPluginPerfStats kotlinPluginPerfStats = parseKaptStats(lines.get(i));

        if (kotlinPluginPerfStats != null) {
          KotlinPluginStatsEvent kaptStatsEvent =
              new KotlinPluginStatsEvent(
                  invokingRule.getFullyQualifiedName(), kotlinPluginPerfStats);
          eventBus.post(kaptStatsEvent);
        }
      }
    }
  }

  @Nullable
  private AnnotationProcessorPerfStats parseProcessorStats(String line) {
    Matcher matcher = PATTERN_PROCESSOR_STATS_LINE.matcher(line);

    if (matcher.find()) {
      // The first group is the entire line, so we start from the second group
      String processorName = matcher.group(1);
      String totalTime = matcher.group(2);
      String initTime = matcher.group(3);
      String roundTimesString = matcher.group(5);

      Matcher matcherRoundTimes = PATTERN_ROUND_TIME.matcher(roundTimesString);
      List<Long> roundTimesList = new ArrayList<>();
      while (matcherRoundTimes.find()) {
        String roundTimeStr = matcherRoundTimes.group(1);
        roundTimesList.add(Long.parseLong(roundTimeStr));
      }

      return new AnnotationProcessorPerfStats(
          processorName, Long.parseLong(initTime), Long.parseLong(totalTime), roundTimesList);
    }

    return null;
  }

  @Nullable
  private KotlinPluginPerfStats parseKaptStats(String line) {
    Matcher matcher = PATTERN_GENERAL_STATS_LINE.matcher(line);

    if (matcher.find()) {
      // The first group is the entire line, so we start from the second group
      String totalTime = matcher.group(1);
      String initialAnalysis = matcher.group(2);
      String stubs = matcher.group(3);
      String annotationProcessing = matcher.group(4);
      String extraData = matcher.group(5);

      return new KotlinPluginPerfStats(
          "KAPT",
          Long.parseLong(totalTime),
          Long.parseLong(initialAnalysis),
          Long.parseLong(stubs),
          Long.parseLong(annotationProcessing),
          extraData);
    }

    return null;
  }

  @Override
  public String getIsolatedStepDescription(IsolatedExecutionContext context) {
    return "Reading reports file in path[" + pathToReportFile + "] ";
  }

  @Override
  public String getShortName() {
    return "kaptStatsParser";
  }
}
