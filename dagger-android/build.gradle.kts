import dagger.gradle.build.SoftwareType

plugins {
  alias(libs.plugins.daggerBuild)
  id(libs.plugins.android.library.get().pluginId)
  id(libs.plugins.kotlinAndroid.get().pluginId)
}

dependencies {
  api(project(":dagger"))
  implementation(project(":dagger-lint-aar"))
  api(libs.androidx.annotations)
  compileOnly(libs.errorprone.annotations)
}

daggerBuild {
  type = SoftwareType.ANDROID_LIBRARY
  isPublished = true
}

android { namespace = "dagger.android" }
