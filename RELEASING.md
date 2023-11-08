Releasing
========

NOTE: this process requires you to have a git remote named `upstream` pointing to the main repo (https://github.com/spotify/mobius). 
You also need to have a GPG key set up locally and made available to the Gradle
signing plugin as per https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials,
and to configure Nexus server credentials in `~/.gradle/gradle.properties?` as per
https://github.com/Codearte/gradle-nexus-staging-plugin#server-credentials.

 1. Checkout latest master
 1. Make sure you are on a clean master and everything is pushed to upstream.
 1. Run `./gradlew clean test` and make sure everything passes.
 1. Run `./gradlew publish -PSONATYPE_NEXUS_USERNAME=<user> -PSONATYPE_NEXUS_PASSWORD=<password>`
    and follow the instructions.
 1. Enter the release version when prompted or press Enter for default (Please double check the version if you do so).
 1. Enter the next development version when prompted or press Enter for default (Please double check the version if you do so).
 1. When the build has successfully completed, run `./gradlew closeAndReleaseRepo`. If this times out
    or fails some other way, log in manually to https://oss.sonatype.org/ and either complete the
    release through closing and releasing the staging repository, or by cleaning up and rerunning.
 1. Add a description of the new release at https://github.com/spotify/mobius/releases.
 1. Once the new set of artifacts is available on https://repo.maven.apache.org/maven2/com/spotify/mobius/, update the
    `baselineVersion` property in [binary_compatibility.gradle](gradle/binary_compatibility.gradle) to the
    version that was just released.
