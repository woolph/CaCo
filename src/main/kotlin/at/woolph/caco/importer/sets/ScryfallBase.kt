package at.woolph.caco.importer.sets


interface ScryfallBase {
    fun isValid(): Boolean

    fun checkValid() {
        if (!isValid())
            throw IllegalStateException("object is not valid")
    }
}
