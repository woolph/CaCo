package at.woolph.caco

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.GregorianCalendar

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

class M11DedicatedApp
/**
 * Constructor. Fill parameters according to given MKM profile app parameters.
 *
 * @param appToken
 * @param appSecret
 * @param accessToken
 * @param accessSecret
 */
(private val mkmAppToken: String, private val mkmAppSecret: String, private val mkmAccessToken: String, private val mkmAccessTokenSecret: String) {

    private var _lastError: Throwable? = null
    private var _lastCode: Int = 0
    private var _lastContent: String? = null
    private var debug: Boolean = false

    /**
     * Activates the console debug messages
     * @param flag true if you want to enable console messages; false to disable any notification.
     */
    fun setDebug(flag: Boolean) {
        debug = flag
    }

    /**
     * Encoding function. To avoid deprecated version, the encoding used is UTF-8.
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    private fun rawurlencode(str: String): String {
        return URLEncoder.encode(str, "UTF-8")
    }

    private fun _debug(msg: String) {
        if (debug) {
            print(GregorianCalendar.getInstance().time)
            print(" > ")
            println(msg)
        }
    }

    /**
     * Get last Error exception.
     * @return null if no errors; instead the raised exception.
     */
    fun lastError(): Throwable? {
        return _lastError
    }

    /**
     * Perform the request to given url with OAuth 1.0a API.
     *
     * @param requestURL url to be requested. Ex. https://api.cardmarket.com/ws/v1.1/products/island/1/1/false
     * @return true if request was successfully executed. You can retrieve the content with responseContent();
     */
    fun request(requestURL: String): Boolean {
        _lastError = null
        _lastCode = 0
        _lastContent = ""
        try {

            _debug("Requesting $requestURL")

            val oauth_version = "1.0"
            val oauth_consumer_key = mkmAppToken
            val oauth_token = mkmAccessToken
            val oauth_signature_method = "HMAC-SHA1"
            // String oauth_timestamp = "" + (System.currentTimeMillis()/1000) ;
            val oauth_timestamp = "1407917892"
            // String oauth_nonce = "" + System.currentTimeMillis() ;
            val oauth_nonce = "53eb1f44909d6"


            val encodedRequestURL = rawurlencode(requestURL)

            var baseString = "GET&$encodedRequestURL&"

            val paramString = "oauth_consumer_key=" + rawurlencode(oauth_consumer_key) + "&" +
                    "oauth_nonce=" + rawurlencode(oauth_nonce) + "&" +
                    "oauth_signature_method=" + rawurlencode(oauth_signature_method) + "&" +
                    "oauth_timestamp=" + rawurlencode(oauth_timestamp) + "&" +
                    "oauth_token=" + rawurlencode(oauth_token) + "&" +
                    "oauth_version=" + rawurlencode(oauth_version)

            baseString = baseString + rawurlencode(paramString)

            val signingKey = rawurlencode(mkmAppSecret) +
                    "&" +
                    rawurlencode(mkmAccessTokenSecret)

            val mac = Mac.getInstance("HmacSHA1")
            val secret = SecretKeySpec(signingKey.toByteArray(), mac.algorithm)
            mac.init(secret)
            val digest = mac.doFinal(baseString.toByteArray())
            val oauth_signature = DatatypeConverter.printBase64Binary(digest)    //Base64.encode(digest) ;

            val authorizationProperty = "OAuth " +
                    "realm=\"" + requestURL + "\", " +
                    "oauth_version=\"" + oauth_version + "\", " +
                    "oauth_timestamp=\"" + oauth_timestamp + "\", " +
                    "oauth_nonce=\"" + oauth_nonce + "\", " +
                    "oauth_consumer_key=\"" + oauth_consumer_key + "\", " +
                    "oauth_token=\"" + oauth_token + "\", " +
                    "oauth_signature_method=\"" + oauth_signature_method + "\", " +
                    "oauth_signature=\"" + oauth_signature + "\""

            val connection = URL(requestURL).openConnection() as HttpURLConnection
            connection.addRequestProperty("Authorization", authorizationProperty)
            connection.connect()

            // from here standard actions...
            // read response code... read input stream.... close connection...

            _lastCode = connection.responseCode

            _debug("Response Code is $_lastCode")


            if (200 == _lastCode || 401 == _lastCode || 404 == _lastCode || 206 == _lastCode) {

                _lastContent = (if (_lastCode == 200 || 206 == _lastCode) connection.inputStream else connection.errorStream).bufferedReader().useLines {
                    it.joinToString("\n")
                }
                _debug("Response Content is \n" + _lastContent!!)
            }

            return _lastCode == 200

        } catch (e: Exception) {
            _debug("(!) Error while requesting $requestURL")
            _lastError = e
        }

        return false
    }

    /**
     * Get response code from last request.
     * @return
     */
    fun responseCode(): Int {
        return _lastCode
    }

    /**
     * Get response content from last request.
     * @return
     */
    fun responseContent(): String? {
        return _lastContent
    }
}

fun main(args: Array<String>) {
    // USAGE EXAMPLE
    val mkmAppToken = "KJ12mz7orXpPewT5"
    val mkmAppSecret = "L7RU2wixWUvb4E1QAMjXem3Rk0ZAwDkB"
    val mkmAccessToken = "UrpRHL0QmdFy4AOMhNjHiT52WGXAW07o"
    val mkmAccessTokenSecret = "Bp1DmxGrRdUJYGGKXM4xgoxxEg2T5036"


    val app = M11DedicatedApp(mkmAppToken, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret)

    //if (app.request("https://api.cardmarket.com/ws/v1.1/account")) { println(app.responseContent()) }

    // test with active console debug
    app.setDebug(true)
    /*
    if (app.request("https://api.cardmarket.com/ws/v1.1/products/island/1/1/false")) {
        println(app.responseContent())
        // .. process(app.responseContent());
    }*/

    //if (app.request("https://api.cardmarket.com/ws/v1.1/products/serra_angel/1/1/false")) { println(app.responseContent()) }

    //if (app.request("https://api.cardmarket.com/ws/v1.1/user/Rarehuntershop")) { println(app.responseContent()) }

    //if (app.request("https://api.cardmarket.com/ws/v2.0/games/1/expansions")) { println(app.responseContent()) }
    //if (app.request("https://api.cardmarket.com/ws/v2.0/expansions/2439/singles")) { println(app.responseContent()) } // 2439 = WAR
    //if (app.request("https://api.cardmarket.com/ws/v2.0/products/371952")) { println(app.responseContent()) }// 371952 = Merfolk Skydiver => get sell pricing
    if (app.request("https://api.cardmarket.com/ws/v2.0/articles/371952")) { println(app.responseContent()) }// 371952 = Merfolk Skydiver

    // etc....
}
