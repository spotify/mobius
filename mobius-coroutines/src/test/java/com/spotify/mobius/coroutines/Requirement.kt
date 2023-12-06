package com.spotify.mobius.coroutines

import java.lang.annotation.Inherited

/** Annotation for defining Given-When-Then tests */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
@Inherited
annotation class Requirement(val given: String, val `when`: String = "", val whenever: String = "", val then: String)
