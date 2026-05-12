/* Copyright 2026 Wolfgang Mayer */
package at.woolph.utils.csv

class IntentionallySkippedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
