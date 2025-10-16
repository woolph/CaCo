plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "CaCo"

include(
  ":lib",
  ":cli",
  ":gui",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
