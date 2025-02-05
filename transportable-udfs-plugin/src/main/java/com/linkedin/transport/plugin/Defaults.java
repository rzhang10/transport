/**
 * Copyright 2019 LinkedIn Corporation. All rights reserved.
 * Licensed under the BSD-2 Clause license.
 * See LICENSE in the project root for license information.
 */
package com.linkedin.transport.plugin;

import com.google.common.collect.ImmutableList;
import com.linkedin.transport.codegen.HiveWrapperGenerator;
import com.linkedin.transport.codegen.SparkWrapperGenerator;
import com.linkedin.transport.codegen.TrinoWrapperGenerator;
import com.linkedin.transport.plugin.packaging.DistributionPackaging;
import com.linkedin.transport.plugin.packaging.ShadedJarPackaging;
import com.linkedin.transport.plugin.packaging.ThinJarPackaging;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import static com.linkedin.transport.plugin.ConfigurationType.*;


/**
 * Stores default configurations for the Transport UDF plugin
 */
class Defaults {

  private Defaults() {
  }

  // The versions of the Transport and supported platforms to apply corresponding versions of the platform dependencies
  private static final Properties DEFAULT_VERSIONS = loadDefaultVersions();

  private static Properties loadDefaultVersions() {
    try (InputStream is =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("version-info.properties")) {
      Properties defaultVersions = new Properties();
      defaultVersions.load(is);
      return defaultVersions;
    } catch (IOException e) {
      throw new RuntimeException("Error loading version-info.properties", e);
    }
  }

  static final List<DependencyConfiguration> MAIN_SOURCE_SET_DEPENDENCY_CONFIGURATIONS = ImmutableList.of(
      getDependencyConfiguration(IMPLEMENTATION, "com.linkedin.transport:transportable-udfs-api", "transport"),
      getDependencyConfiguration(ANNOTATION_PROCESSOR, "com.linkedin.transport:transportable-udfs-annotation-processor",
          "transport"),
      // the idea plugin needs a scala-library on the classpath when the scala plugin is applied even when there are no
      // scala sources
      getDependencyConfiguration(COMPILE_ONLY, "org.scala-lang:scala-library", "scala")
  );

  static final List<DependencyConfiguration> TEST_SOURCE_SET_DEPENDENCY_CONFIGURATIONS = ImmutableList.of(
      getDependencyConfiguration(IMPLEMENTATION, "com.linkedin.transport:transportable-udfs-test-api", "transport"),
      getDependencyConfiguration(RUNTIME_ONLY, "com.linkedin.transport:transportable-udfs-test-generic", "transport")
  );

  static final List<Platform> DEFAULT_PLATFORMS = ImmutableList.of(
      new Platform(
          "trino",
          Language.JAVA,
          TrinoWrapperGenerator.class,
          JavaLanguageVersion.of(11),
          ImmutableList.of(
              getDependencyConfiguration(IMPLEMENTATION, "com.linkedin.transport:transportable-udfs-trino",
                  "transport"),
              getDependencyConfiguration(COMPILE_ONLY, "io.trino:trino-main", "trino")
          ),
          ImmutableList.of(
              getDependencyConfiguration(RUNTIME_ONLY, "com.linkedin.transport:transportable-udfs-test-trino",
                  "transport"),
              // trino-main:tests is a transitive dependency of transportable-udfs-test-trino, but some POM -> IVY
              // converters drop dependencies with classifiers, so we apply this dependency explicitly
              getDependencyConfiguration(RUNTIME_ONLY, "io.trino:trino-main", "trino", "tests")
          ),
          ImmutableList.of(new ThinJarPackaging(), new DistributionPackaging())),
      new Platform(
          "hive",
          Language.JAVA,
          HiveWrapperGenerator.class,
          JavaLanguageVersion.of(8),
          ImmutableList.of(
              getDependencyConfiguration(IMPLEMENTATION, "com.linkedin.transport:transportable-udfs-hive", "transport"),
              getDependencyConfiguration(COMPILE_ONLY, "org.apache.hive:hive-exec", "hive")
          ),
          ImmutableList.of(
              getDependencyConfiguration(RUNTIME_ONLY, "com.linkedin.transport:transportable-udfs-test-hive",
                  "transport")
          ),
          ImmutableList.of(new ShadedJarPackaging(ImmutableList.of("org.apache.hadoop", "org.apache.hive"), null))),
      new Platform(
          "spark",
          Language.SCALA,
          SparkWrapperGenerator.class,
          JavaLanguageVersion.of(8),
          ImmutableList.of(
              getDependencyConfiguration(IMPLEMENTATION, "com.linkedin.transport:transportable-udfs-spark",
                  "transport"),
              getDependencyConfiguration(COMPILE_ONLY, "org.apache.spark:spark-sql_2.11", "spark")
          ),
          ImmutableList.of(
              getDependencyConfiguration(RUNTIME_ONLY, "com.linkedin.transport:transportable-udfs-test-spark",
                  "transport")
          ),
          ImmutableList.of(new ShadedJarPackaging(
              ImmutableList.of("org.apache.hadoop", "org.apache.spark"),
              ImmutableList.of("com.linkedin.transport.spark.**")))
      )
  );

  private static DependencyConfiguration getDependencyConfiguration(ConfigurationType configurationType,
      String module, String platform) {
    return getDependencyConfiguration(configurationType, module, platform, null);
  }

  private static DependencyConfiguration getDependencyConfiguration(ConfigurationType configurationType,
      String module, String platform, String classifier) {
    return new DependencyConfiguration(configurationType, module
        + ":" + DEFAULT_VERSIONS.getProperty(platform + "-version")
        + (classifier != null ? (":" + classifier) : ""));
  }
}
