package at.woolph.utils.csv

class IntentionallySkippedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
