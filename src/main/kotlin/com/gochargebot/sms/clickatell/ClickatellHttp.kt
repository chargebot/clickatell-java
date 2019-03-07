package com.gochargebot.sms.clickatell

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.Map.Entry

/**
 * This is an example of how to use the Clickatell HTTP API. NOTE: this is not
 * the only way, this is just an example. This class can also be used as a
 * library if you wish.
 *
 * @date Dec 2, 2014
 * @author Dominic Schaff <dominic.schaff></dominic.schaff>@gmail.com>
 */
class ClickatellHttp
/**
 * Create a HTTP object, and set the auth, but not test the auth.
 */
(
    /**
     * @var The three private variables to use for authentication.
     */
    private val userName: String, private val apiId: String, private val password: String) {

  /**
   * This will attempt to get your current balance.
   *
   * @throws Exception
   * This will be thrown if your auth details were incorrect.
   *
   * @return Your balance.
   */
  // Build Parameters:
  // Send Request:
  // Check whether an auth failed happened:
  // We know the balance is the second part of the query:
  val balance: Double
    @Throws(Exception::class)
    get() {
      val urlParameters = ("user="
          + URLEncoder.encode(this.userName, "UTF-8") + "&api_id="
          + URLEncoder.encode(this.apiId, "UTF-8") + "&password="
          + URLEncoder.encode(this.password, "UTF-8"))
      val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "getbalance.php", urlParameters)
      if (result.contains("Authentication failed")) {
        throw Exception("Authentication Failed")
      }
      if (result.startsWith("ERR")) {
        throw Exception(result)
      }
      val a = result.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      return java.lang.Double.parseDouble(a[1])
    }

  /**
   * This tests whether your account details works.
   *
   * @return True if details were accepted, and false otherwise.
   * @throws UnknownHostException
   */
  @Throws(UnknownHostException::class)
  fun testAuth(): Boolean {
    try {
      // Build Parameters:
      val urlParameters = ("user="
          + URLEncoder.encode(this.userName, "UTF-8") + "&api_id="
          + URLEncoder.encode(this.apiId, "UTF-8") + "&password="
          + URLEncoder.encode(this.password, "UTF-8"))

      // Send Request:
      val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "auth.php", urlParameters)
      // Check whether an auth failed happened:
      return result.startsWith("OK: ")
    } catch (e: UnsupportedEncodingException) {
    }

    return false
  }

  /**
   * This sends a single message.
   *
   * @param number
   * The number that you wish to send to. This should be in
   * international format.
   * @param messageContent
   * The message you want to send,
   *
   * @throws Exception
   * This gets thrown on an auth failure.
   * @return A Message object that contains the resulting information.
   */
  @Throws(Exception::class)
  fun sendMessage(number: String, messageContent: String): Message {
    // Build Parameters:
    val urlParameters = ("user="
        + URLEncoder.encode(this.userName, "UTF-8") + "&api_id="
        + URLEncoder.encode(this.apiId, "UTF-8") + "&password="
        + URLEncoder.encode(this.password, "UTF-8") + "&to="
        + URLEncoder.encode(number, "UTF-8") + "&text="
        + URLEncoder.encode(messageContent, "UTF-8"))

    // Send Request:
    val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "sendmsg.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }
    val a = result.split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val message = Message()
    message.number = number
    message.content = messageContent
    // Check whether there is no credit left in the account:
    if (result.toLowerCase().startsWith("err")) {
      message.error = a[1]
      return message
    }
    message.message_id = a[1].trim { it <= ' ' }
    return message
  }

  /**
   * This is to send the same message to multiple people. Only use this
   * function to send a maximum of 300 messages, and a minimum of 2.
   *
   * @param numbers
   * The array of numbers that are to be sent to.
   * @param messageContent
   * The message that you would like to send.
   *
   * @return The returned array contains the messages sent with their
   * resulting information.
   *
   * @throws Exception
   * This gets thrown on auth errors.
   */
  @Throws(Exception::class)
  fun sendMessage(numbers: Array<String>, messageContent: String): Array<Message> {
    if (numbers.size < 2 || numbers.size > 300) {
      throw Exception("Illegal arguments passed")
    }
    val messages = ArrayList<Message>()
    // Build Parameters:
    var urlParameters = ("user="
        + URLEncoder.encode(this.userName, "UTF-8") + "&api_id="
        + URLEncoder.encode(this.apiId, "UTF-8") + "&password="
        + URLEncoder.encode(this.password, "UTF-8") + "&text="
        + URLEncoder.encode(messageContent, "UTF-8") + "&to=")
    urlParameters += numbers[0]
    for (x in 1 until numbers.size) {
      urlParameters += "," + numbers[x]
    }

    // Send Request:
    val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "sendmsg.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }
    // We don't throw an exception here, as maybe only part of your
    // messages failed:
    val lines = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (l in lines) {
      val n = l.split(" To: ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      val message = Message()
      message.number = n[1].trim { it <= ' ' }
      val q = n[0].split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      if (q[0].equals("err", ignoreCase = true)) {
        message.error = q[1]
      } else {
        message.message_id = q[1]
      }
      messages.add(message)
    }
    return messages.toTypedArray()
  }

  /**
   * This will attempt to get the message status of a single message.
   *
   * @param messageId
   * This is the message ID that you received when sending the
   * message.
   *
   * @return The status of the message.
   *
   * @throws Exception
   * if there is an error with the request.
   */
  @Throws(Exception::class)
  fun getMessageStatus(messageId: String): Int {
    val urlParameters: String
    // Build Parameters:
    urlParameters = ("user=" + URLEncoder.encode(this.userName, "UTF-8")
        + "&api_id=" + URLEncoder.encode(this.apiId, "UTF-8")
        + "&password=" + URLEncoder.encode(this.password, "UTF-8")
        + "&apimsgid=" + URLEncoder.encode(messageId, "UTF-8"))

    // Send Request:
    val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "querymsg.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }
    // If there was an error, throw it.
    if (result.startsWith("ERR:")) {
      throw Exception(result)
    }
    // We know the status will always be the fourth part:
    // Syntax: ID: xxx Status: xxx
    val a = result.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    return Integer.parseInt(a[3].trim { it <= ' ' })
  }

  /**
   * This will get the status and charge of the message given by the
   * messageId.
   *
   * @param messageId
   * The message ID that should be searched for.
   *
   * @return A Message object with the requested data will be returned.
   *
   * @throws Exception
   * If there was an error with the request.
   */
  @Throws(Exception::class)
  fun getMessageCharge(messageId: String): Message {
    val urlParameters: String
    // Build Parameters:
    urlParameters = ("user=" + URLEncoder.encode(this.userName, "UTF-8")
        + "&api_id=" + URLEncoder.encode(this.apiId, "UTF-8")
        + "&password=" + URLEncoder.encode(this.password, "UTF-8")
        + "&apimsgid=" + URLEncoder.encode(messageId, "UTF-8"))

    // Send Request:
    val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "getmsgcharge.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }
    val message = Message(messageId)
    // If there was an error, throw it.
    if (result.startsWith("ERR:")) {
      message.error = result.substring(4)
    } else {
      val a = result.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      message.status = a[5].trim { it <= ' ' }
      message.charge = a[3].trim { it <= ' ' }
    }
    return message
  }

  /**
   * This will try to stop a message that has been sent. Note that only
   * messages that are going to be sent in the future can be stopped. Or if by
   * some luck you message has not been sent to the operator yet.
   *
   * @param messageId
   * The message ID that is to be stopped.
   *
   * @return The status after requesting the message to be stopped.
   *
   * @throws Exception
   * If there was something wrong with the request.
   */
  @Throws(Exception::class)
  fun stopMessage(messageId: String): Int {
    val urlParameters: String
    // Build Parameters:
    urlParameters = ("user=" + URLEncoder.encode(this.userName, "UTF-8")
        + "&api_id=" + URLEncoder.encode(this.apiId, "UTF-8")
        + "&password=" + URLEncoder.encode(this.password, "UTF-8")
        + "&apimsgid=" + URLEncoder.encode(messageId, "UTF-8"))

    // Send Request:
    val result = this.excutePost(
        CLICKATELL_HTTP_BASE_URL + "delmsg.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }
    // If there was an error, throw it.
    if (result.startsWith("ERR")) {
      throw Exception(result)
    }
    // Split the result we know that the status will always the fourth
    // part:
    // Format: ID: xxx Status: xxx
    val a = result.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    return Integer.parseInt(a[3].trim { it <= ' ' })
  }

  /**
   * This attempts to get coverage data for the given number. A -1 means no
   * coverage, all else is the minimum cost the message could charge.
   *
   * @param number
   * The number the lookup should be done on.
   * @return The minimum possible cost, or a -1 on error.
   * @throws Exception
   * If there was something wrong with the request.
   */
  @Throws(Exception::class)
  fun getCoverage(number: String): Double {
    // Build Parameters:
    val urlParameters = ("user="
        + URLEncoder.encode(this.userName, "UTF-8") + "&api_id="
        + URLEncoder.encode(this.apiId, "UTF-8") + "&password="
        + URLEncoder.encode(this.password, "UTF-8") + "&msisdn="
        + URLEncoder.encode(number, "UTF-8"))

    // Send Request:
    val result = this.excutePost(CLICKATELL_UTILS_BASE_URL + "routecoverage.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }
    if (result.startsWith("ERR")) {
      return -1.0
    }
    val a = result.split("Charge: ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    return java.lang.Double.parseDouble(a[1])
  }

  /**
   * This will allow you to use any feature of the API. Note that you can do
   * more powerful things with this function. And as such should only be used
   * once you have read the documentation, as the parameters are passed
   * directly to the API.
   *
   * @param numbers
   * The list of numbers that must be sent to.
   * @param messageContent
   * The message that is to be sent.
   * @param features
   * The extra features that should be included.
   *
   * @return An array of Messages which will contain the data for each message
   * sent.
   *
   * @throws Exception
   * If there is anything wrong with the submission this will get
   * thrown.
   */
  @Throws(Exception::class)
  fun sendAdvancedMessage(numbers: Array<String>,
                          messageContent: String, features: HashMap<String, String>): Array<Message> {
    val messages = ArrayList<Message>()
    var urlParameters: String
    // Build Parameters:
    urlParameters = ("user=" + URLEncoder.encode(this.userName, "UTF-8")
        + "&api_id=" + URLEncoder.encode(this.apiId, "UTF-8")
        + "&password=" + URLEncoder.encode(this.password, "UTF-8")
        + "&text=" + URLEncoder.encode(messageContent, "UTF-8"))
    var number = numbers[0]
    for (x in 1 until numbers.size) {
      number += "," + numbers[x]
    }
    urlParameters += "&to=$number"
    for ((key, value) in features) {
      urlParameters += ("&" + key + "="
          + URLEncoder.encode(value, "UTF-8"))
    }

    // Send Request:
    val result = this.excutePost(CLICKATELL_HTTP_BASE_URL + "sendmsg.php", urlParameters)
    // Check whether an auth failed happened:
    if (result.contains("Authentication failed")) {
      throw Exception("Authentication Failed")
    }

    // This does some fancy swapping:
    val lines = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (lines.size > 1) { // Sent more than one message
      for (l in lines) {
        val message = Message()
        val i = l.split(" To: ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        message.number = i[1]
        val n = i[0].split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (n[0].equals("err", ignoreCase = true)) {
          message.error = n[1]
        } else {
          message.message_id = n[1]
        }
      }
    } else { // Sent one message
      val n = lines[0].split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      val message = Message()
      message.number = numbers[0]
      if (n[0].equals("err", ignoreCase = true)) {
        message.error = n[1]
      } else {
        message.message_id = n[1]
      }
    }
    return messages.toTypedArray()
  }

  /**
   * This executes a POST query with the given parameters.
   *
   * @param targetURL
   * The URL that should get hit.
   * @param urlParameters
   * The data you want to send via the POST.
   *
   * @return The content of the request.
   * @throws UnknownHostException
   */
  @Throws(UnknownHostException::class)
  private fun excutePost(targetURL: String, urlParameters: String): String {
    val url: URL
    var connection: HttpURLConnection? = null
    try {
      // Create connection
      url = URL(targetURL)
      connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "POST"
      connection.setRequestProperty("Content-Type",
                                    "application/x-www-form-urlencoded")

      connection.setRequestProperty("Content-Length",
                                    "" + Integer.toString(urlParameters.toByteArray().size))
      connection.setRequestProperty("Content-Language", "en-US")

      connection.useCaches = false
      connection.doInput = true
      connection.doOutput = true

      // Send request
      val wr = DataOutputStream(
          connection.outputStream)
      wr.writeBytes(urlParameters)
      wr.flush()
      wr.close()

      // Get Response
      val `is` = connection.inputStream
      val rd = BufferedReader(InputStreamReader(`is`))
      var line: String
      val response = StringBuffer()
      //while ((line = rd.readLine()) != null) {
      for (line in rd.lines()) {
        response.append(line)
        response.append('\n')
      }
      rd.close()
      return response.toString()
    } catch (e: UnknownHostException) {
      throw e
    } catch (e: Exception) {
      return ""
    } finally {
      connection?.disconnect()
    }
  }

  /**
   * This is the Message class that gets used as return values for some of the
   * functions.
   *
   * @author Dominic Schaff <dominic.schaff></dominic.schaff>@gmail.com>
   */
  inner class Message {
    var number: String? = null
    var message_id: String? = null
    var content: String? = null
    var charge: String? = null
    var status: String? = null
    var error: String? = null

    constructor(message_id: String) {
      this.message_id = message_id
    }

    constructor() {}

    override fun toString(): String {
      return if (message_id != null) {
        "$number: $message_id"
      } else "$number: $error"
    }
  }

  companion object {

    /**
     * @var The URL to use for the base of the HTTP API.
     */
    private val CLICKATELL_HTTP_BASE_URL = "https://api.clickatell.com/http/"

    /**
     * @var The URL to use for the base of the HTTP/UTILS API.
     */
    private val CLICKATELL_UTILS_BASE_URL = "https://api.clickatell.com/utils/"
  }
}
