package at.charlemagne.libs.json

import java.io.InputStream
import java.io.StringReader
import javax.json.JsonObject
import javax.json.JsonReader

public fun JsonObject.getJsonObjectArray(name: String) = this.getJsonArray(name).getValuesAs(JsonObject::class.java)

public inline fun <R> InputStream.useJsonReader(block: (JsonReader) -> R): R {
    this.use {
        return javax.json.Json.createReader(it).use(block)
    }
}

public inline fun <R> String.useJsonReader(block: (JsonReader) -> R): R {
    return javax.json.Json.createReader(StringReader(this)).use(block)
}
