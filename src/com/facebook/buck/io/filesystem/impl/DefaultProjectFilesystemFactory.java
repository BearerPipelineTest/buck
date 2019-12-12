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

package com.facebook.buck.io.filesystem.impl;

import com.facebook.buck.core.cell.name.CanonicalCellName;
import com.facebook.buck.core.exceptions.HumanReadableException;
import com.facebook.buck.io.filesystem.BuckPaths;
import com.facebook.buck.io.filesystem.EmbeddedCellBuckOutInfo;
import com.facebook.buck.io.filesystem.GlobPatternMatcher;
import com.facebook.buck.io.filesystem.PathMatcher;
import com.facebook.buck.io.filesystem.ProjectFilesystemFactory;
import com.facebook.buck.io.filesystem.RecursiveFileMatcher;
import com.facebook.buck.io.windowsfs.WindowsFS;
import com.facebook.buck.util.BuckConstant;
import com.facebook.buck.util.config.Config;
import com.facebook.buck.util.environment.Platform;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class DefaultProjectFilesystemFactory implements ProjectFilesystemFactory {

  @VisibleForTesting public static final String BUCK_BUCKD_DIR_KEY = "buck.buckd_dir";

  // A non-exhaustive list of characters that might indicate that we're about to deal with a glob.
  private static final Pattern GLOB_CHARS = Pattern.compile("[*?{\\[]");

  @Nullable
  private static final WindowsFS winFSInstance =
      Platform.detect() == Platform.WINDOWS ? new WindowsFS() : null;

  /** @return the WindowsFS singleton. */
  @Nullable
  public static WindowsFS getWindowsFSInstance() {
    return winFSInstance;
  }

  @Override
  public DefaultProjectFilesystem createProjectFilesystem(
      CanonicalCellName cellName,
      Path root,
      Config config,
      Optional<EmbeddedCellBuckOutInfo> embeddedCellBuckOutInfo) {
    BuckPaths buckPaths = getConfiguredBuckPaths(cellName, root, config, embeddedCellBuckOutInfo);
    return new DefaultProjectFilesystem(
        root,
        extractIgnorePaths(root, config, buckPaths, embeddedCellBuckOutInfo),
        buckPaths,
        ProjectFilesystemDelegateFactory.newInstance(root, config),
        getWindowsFSInstance());
  }

  @Override
  public DefaultProjectFilesystem createProjectFilesystem(
      CanonicalCellName cellName, Path root, Config config) {
    return createProjectFilesystem(cellName, root, config, Optional.empty());
  }

  @Override
  public DefaultProjectFilesystem createProjectFilesystem(CanonicalCellName cellName, Path root) {
    return createProjectFilesystem(cellName, root, new Config());
  }

  private static ImmutableSet<PathMatcher> extractIgnorePaths(
      Path root,
      Config config,
      BuckPaths buckPaths,
      Optional<EmbeddedCellBuckOutInfo> embeddedCellBuckOutInfo) {
    ImmutableSet.Builder<PathMatcher> builder = ImmutableSet.builder();

    FileSystem rootFs = root.getFileSystem();

    builder.add(RecursiveFileMatcher.of(rootFs.getPath(".idea")));

    String projectKey = "project";
    String ignoreKey = "ignore";

    String buckdDirProperty = System.getProperty(BUCK_BUCKD_DIR_KEY, ".buckd");
    if (!Strings.isNullOrEmpty(buckdDirProperty)) {
      addPathMatcherRelativeToRepo(root, builder, rootFs.getPath(buckdDirProperty));
    }

    Path cacheDir =
        DefaultProjectFilesystem.getCacheDir(root, config.getValue("cache", "dir"), buckPaths);
    addPathMatcherRelativeToRepo(root, builder, cacheDir);

    Optional<Path> mainCellBuckOut = getMainCellBuckOut(root, embeddedCellBuckOutInfo);

    config
        .getListWithoutComments(projectKey, ignoreKey)
        .forEach(
            input -> {
              // We don't really want to ignore the output directory when doing things like
              // filesystem walks, so return null
              if (buckPaths.getBuckOut().toString().equals(input)) {
                return; // root.getFileSystem().getPathMatcher("glob:**");
              }

              // For the same reason as above, we don't really want to ignore the buck-out of other
              // cells either. They may contain artifacts that we want to use in this cell.
              // TODO: The below does this for the root cell, but this should be true of all cells.
              if (mainCellBuckOut.isPresent() && mainCellBuckOut.get().toString().equals(input)) {
                return;
              }

              if (GLOB_CHARS.matcher(input).find()) {
                builder.add(GlobPatternMatcher.of(input));
                return;
              }
              addPathMatcherRelativeToRepo(root, builder, rootFs.getPath(input));
            });

    return builder.build();
  }

  private static void addPathMatcherRelativeToRepo(
      Path root, Builder<PathMatcher> builder, Path pathToAdd) {
    if (!pathToAdd.isAbsolute()
        || pathToAdd.normalize().startsWith(root.toAbsolutePath().normalize())) {
      if (pathToAdd.isAbsolute()) {
        pathToAdd = root.relativize(pathToAdd);
      }
      builder.add(RecursiveFileMatcher.of(pathToAdd));
    }
  }

  private static Path getConfiguredBuckOut(
      Optional<String> configuredBuckOut, Path buckOutPath, Path rootPath) {
    // You currently cannot truly configure the BuckOut directory today.
    // The use of ConfiguredBuckOut and project.buck_out here is IMHO
    // confusingly "partial" support for this feature.
    //
    // Language Services chose to hack around this limitation so we could run
    // Buck in an isolated, separate BuckOut. But that requires
    // us to ensure any ConfiguredBuckOut is a relative path underneath our
    // top-level BuckOut (which happily enough, FBCode already does for their current
    // use: "buck-out/dev"). We enforce that relativity here in a disgusting way.
    String buckOut = buckOutPath.toString();
    if (configuredBuckOut.isPresent()) {
      String value = configuredBuckOut.get();
      if (value.startsWith(BuckConstant.DEFAULT_BUCK_OUT_DIR_NAME)
          && buckOut != BuckConstant.DEFAULT_BUCK_OUT_DIR_NAME) {
        value = value.replace(BuckConstant.DEFAULT_BUCK_OUT_DIR_NAME, buckOut);
      }

      return rootPath.getFileSystem().getPath(value);
    }

    return buckOutPath;
  }

  private static BuckPaths getConfiguredBuckPaths(
      CanonicalCellName cellName,
      Path rootPath,
      Config config,
      Optional<EmbeddedCellBuckOutInfo> embeddedCellBuckOutInfo) {
    boolean buckOutIncludeTargetConfigHash =
        config.getBooleanValue("project", "buck_out_include_target_config_hash", false);
    BuckPaths buckPaths =
        BuckPaths.createDefaultBuckPaths(cellName, rootPath, buckOutIncludeTargetConfigHash);
    Path buckOut =
        (embeddedCellBuckOutInfo.isPresent())
            ? embeddedCellBuckOutInfo.get().getCellBuckOut()
            : buckPaths.getBuckOut();
    Optional<String> configuredProjectBuckOut = config.get("project", "buck_out");

    Path configuredBuckOut = getConfiguredBuckOut(configuredProjectBuckOut, buckOut, rootPath);

    if (configuredBuckOut.isAbsolute()) {
      configuredBuckOut = rootPath.relativize(configuredBuckOut);
    }

    if (buckOut.isAbsolute()) {
      buckOut = rootPath.relativize(buckOut);
    }

    return buckPaths.withConfiguredBuckOut(configuredBuckOut).withBuckOut(buckOut);
  }

  /** Returns a root-relative path to the main cell's buck-out when using embedded cell buck out */
  private static Optional<Path> getMainCellBuckOut(
      Path root, Optional<EmbeddedCellBuckOutInfo> embeddedCellBuckOutInfoOptional) {
    if (!embeddedCellBuckOutInfoOptional.isPresent()) {
      return Optional.empty();
    }

    EmbeddedCellBuckOutInfo embeddedCellBuckOutInfo = embeddedCellBuckOutInfoOptional.get();

    Path mainCellBuckOut =
        embeddedCellBuckOutInfo
            .getMainCellRoot()
            .resolve(embeddedCellBuckOutInfo.getMainCellBuckPaths().getBuckOut());

    return Optional.of(root.relativize(mainCellBuckOut));
  }

  @Override
  public DefaultProjectFilesystem createOrThrow(CanonicalCellName cellName, Path path) {
    try {
      // toRealPath() is necessary to resolve symlinks, allowing us to later
      // check whether files are inside or outside of the project without issue.
      return createProjectFilesystem(cellName, path.toRealPath().normalize());
    } catch (IOException e) {
      throw new HumanReadableException(
          String.format(
              ("Failed to resolve project root [%s]."
                  + "Check if it exists and has the right permissions."),
              path.toAbsolutePath()),
          e);
    }
  }
}
