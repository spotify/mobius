Releasing
========

## Prerequisites

- Java 17+ (required by Gradle 8.2 and the build toolchain)
- A GPG key set up locally and made available to the Gradle signing plugin as per
  https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
- A Central Portal token from https://central.sonatype.com (go to Account → Generate User Token)

## Steps

 1. Checkout latest master.
 1. Make sure you are on a clean master and everything is pushed.
 1. Run `./gradlew clean test` and make sure everything passes.
 1. Update the version in `gradle.properties` and the `baselineVersion` in
    [binary_compatibility.gradle](gradle/binary_compatibility.gradle) to the version being released.
    Commit, push via PR, and tag the merge commit.
 1. Run `./gradlew publishAggregationToCentralPortal -PSONATYPE_NEXUS_USERNAME=<token-username> -PSONATYPE_NEXUS_PASSWORD=<token-password>`.
 1. Log in to https://central.sonatype.com and verify the deployment. Since `publishingType` is
    `USER_MANAGED`, you need to manually publish the deployment from the portal UI.
 1. Add a description of the new release at https://github.com/spotify/mobius/releases.
 1. Once the new set of artifacts is available on https://repo.maven.apache.org/maven2/com/spotify/mobius/,
    update the `baselineVersion` property in [binary_compatibility.gradle](gradle/binary_compatibility.gradle)
    to the version that was just released.
 1. Update the version in `gradle.properties` in the master branch to the next version.
