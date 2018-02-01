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
 1. Run `./gradlew release -PSONATYPE_NEXUS_USERNAME=<user> -PSONATYPE_NEXUS_PASSWORD=<password>`
    and follow the instructions.
 1. Enter the release version when prompted or press Enter for default (Please double check the version if you do so).
 1. Enter the next development version when prompted or press Enter for default (Please double check the version if you do so).
 1. When the build has successfully completed, run `./gradlew closeAndReleaseRepo`
