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

package com.facebook.buck.core.rulekey;

import com.facebook.buck.core.util.immutables.BuckStyleImmutable;
import com.facebook.buck.log.views.JsonViews;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@BuckStyleImmutable
interface AbstractBuildRuleKeys {

  @Value.Parameter
  @JsonView(JsonViews.MachineReadableLog.class)
  RuleKey getRuleKey();

  @JsonView(JsonViews.MachineReadableLog.class)
  Optional<RuleKey> getInputRuleKey();

  Optional<RuleKey> getDepFileRuleKey();

  Optional<RuleKey> getManifestRuleKey();
}
