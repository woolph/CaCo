package at.charlemagne.libs.util

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun String.encodeUrl(charset: Charset = StandardCharsets.UTF_8) = URLEncoder.encode(this, charset.name())

fun String.decodeUrl(charset: Charset = StandardCharsets.UTF_8) = URLDecoder.decode(this, charset.name())
