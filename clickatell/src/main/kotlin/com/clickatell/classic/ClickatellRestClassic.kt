package com.clickatell.classic

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.Map.Entry

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue

/**
 * This is an example of how to use the Clickatell REST API. NOTE: this is not
 * the only way, this is just an example. This class can also be used as a
 * library if you wish.
 *
 * @date Dec 2, 2014
 * @author Dominic Schaff <dominic.schaff></dominic.schaff>@gmail.com>
 */
class ClickatellRestClassic
/**
 * Create a REST object, and set the auth, but not test the auth.
 */
(
    /**
     * @var The three private variables to use for authentication.
     */
    private val apiKey: String) {

  /**
   * This will attempt to get your current balance.
   *
   * @throws Exception
   * This will be thrown if your auth details were incorrect.
   *
   * @return Your balance.
   */
  // Send Request:
  val balance: Double
    @Throws(Exception::class)
    get() {
      val response = this.excute("account/balance", GET, null)
      val obj = JSONValue.parse(response) as JSONObject

      CheckForError(obj)

      val objData = obj["data"] as JSONObject
      val balance = objData["balance"] as String
      return java.lang.Double.parseDouble(balance)
    }

  /**
   * This sends a single message.
   *
   * @param number
   * The number that you wish to send to. This should be in
   * international format.
   * @param message
   * The message you want to send,
   *
   * @throws Exception
   * This gets thrown on an auth failure.
   * @return A Message object which will contain the information from the
   * request.
   */
  @Throws(Exception::class)
  fun sendMessage(number: String, message: String): Message {
    // Send Request:
    val response = this.excute("message", POST, "{\"to\":[\"" + number
        + "\"],\"text\":\"" + message + "\"}")
    val obj = JSONValue.parse(response) as JSONObject

    CheckForError(obj)
    val msg = Message()
    val objData = obj["data"] as JSONObject
    val msgArray = objData["message"] as JSONArray
    val firstMsg = msgArray[0] as JSONObject
    msg.number = firstMsg["to"] as String
    if (!(firstMsg["accepted"] as Boolean)) {
      try {
        CheckForError(firstMsg)
      } catch (e: Exception) {
        msg.error = e.message
      }

    } else {
      msg.message_id = firstMsg["apiMessageId"] as String
    }
    return msg
  }

  /**
   * This is to send the same message to multiple people.
   *
   * @param numbers
   * The array of numbers that are to be sent to.
   * @param message
   * The message that you would like to send.
   *
   * @return An Array of Message objects which will contain the information
   * from the request.
   *
   * @throws Exception
   * This gets thrown on auth errors.
   */
  @Throws(Exception::class)
  fun sendMessage(numbers: Array<String>, message: String): Array<Message> {
    var number = numbers[0]
    for (x in 1 until numbers.size) {
      number += "\",\"" + numbers[x]
    }
    val messages = ArrayList<Message>()

    // Send Request:
    val response = this.excute("message", POST, "{\"to\":[\"" + number
        + "\"],\"text\":\"" + message + "\"}")
    val obj = JSONValue.parse(response) as JSONObject

    CheckForError(obj)
    val objData = obj["data"] as JSONObject
    val msgArray = objData["message"] as JSONArray
    for (i in msgArray.indices) {
      val msg = Message()
      val firstMsg = msgArray[i] as JSONObject
      msg.number = firstMsg["to"] as String
      if (!(firstMsg["accepted"] as Boolean)) {
        try {
          CheckForError(firstMsg)
        } catch (e: Exception) {
          msg.error = e.message
        }

      } else {
        msg.message_id = firstMsg["apiMessageId"] as String
      }
      messages.add(msg)
    }
    return messages.toTypedArray()
  }

  /**
   * This will get the status and charge of the message given by the
   * messageId.
   *
   * @param messageId
   * The message ID that should be searched for.
   *
   * @return A Message object which will contain the information from the
   * request.
   *
   * @throws Exception
   * If there was an error with the request.
   */
  @Throws(Exception::class)
  fun getMessageStatus(messageId: String): Message {
    val response = this.excute("message/$messageId", GET, null)
    val obj = JSONValue.parse(response) as JSONObject

    CheckForError(obj)
    val msg = Message()
    val objData = obj["data"] as JSONObject
    msg.message_id = objData["apiMessageId"] as String
    msg.charge = objData["charge"].toString()
    msg.status = objData["messageStatus"] as String
    msg.statusString = objData["description"] as String

    return msg
  }

  /**
   * This will try to stop a message that has been sent. Note that only
   * messages that are going to be sent in the future can be stopped. Or if by
   * some luck you message has not been sent to the operator yet.
   *
   * @param messageId
   * The message ID that is to be stopped.
   *
   * @return A Message object which will contain the information from the
   * request.
   *
   * @throws Exception
   * If there was something wrong with the request.
   */
  @Throws(Exception::class)
  fun stopMessage(messageId: String): Message {
    // Send Request:
    val response = this.excute("message/$messageId", DELETE, null)
    val obj = JSONValue.parse(response) as JSONObject

    CheckForError(obj)
    val msg = Message()
    val objData = obj["data"] as JSONObject
    msg.message_id = objData["apiMessageId"] as String
    msg.status = objData["messageStatus"] as String
    msg.statusString = objData["description"] as String

    return msg
  }

  /**
   * This will allow you to use any feature of the API. Note that you can do
   * more powerful things with this function. And as such should only be used
   * once you have read the documentation, as the parameters are passed
   * directly to the API.
   *
   * @param numbers
   * The list of numbers that must be sent to.
   * @param message
   * The message that is to be sent.
   * @param features
   * The extra features that should be included.
   *
   * @return An Array of Message objects which will contain the information
   * from the request.
   *
   * @throws Exception
   * If there is anything wrong with the submission this will get
   * thrown.
   */
  @Throws(Exception::class)
  fun sendAdvancedMessage(numbers: Array<String>, message: String,
                          features: HashMap<String, String>): Array<Message> {
    val messages = ArrayList<Message>()
    var dataPacket = "{\"to\":[\"" + numbers[0]
    for (x in 1 until numbers.size) {
      dataPacket += "\",\"" + numbers[x]
    }
    dataPacket += "\"],\"text\":\"$message\""
    for ((key, value) in features) {
      dataPacket += (",\"" + key + "\":\"" + value
          + "\"")
    }
    dataPacket += "}"

    // Send Request:
    val response = this.excute("message", POST, dataPacket)
    val obj = JSONValue.parse(response) as JSONObject

    CheckForError(obj)
    val objData = obj["data"] as JSONObject
    val msgArray = objData["message"] as JSONArray
    for (i in msgArray.indices) {
      val msg = Message()
      val firstMsg = msgArray[i] as JSONObject
      msg.number = firstMsg["to"] as String
      if (!(firstMsg["accepted"] as Boolean)) {
        try {
          CheckForError(firstMsg)
        } catch (e: Exception) {
          msg.error = e.message
        }

      } else {
        msg.message_id = firstMsg["apiMessageId"] as String
      }
      messages.add(msg)
    }
    return messages.toTypedArray()
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
    val response = this.excute("coverage/$number", GET, null)
    val obj = JSONValue.parse(response) as JSONObject

    CheckForError(obj)
    val objData = obj["data"] as JSONObject

    return if (!(objData["routable"] as Boolean)) {
      -1.0
    } else java.lang.Double.parseDouble(objData["minimumCharge"].toString())
  }

  /**
   * This executes a POST query with the given parameters.
   *
   * @param resource
   * The URL that should get hit.
   * @param urlParameters
   * The data you want to send via the POST.
   *
   * @return The content of the request.
   * @throws UnknownHostException
   */
  @Throws(UnknownHostException::class)
  private fun excute(resource: String, method: String, dataPacket: String?): String {
    val url: URL
    var connection: HttpURLConnection? = null
    try {
      // Create connection
      url = URL(CLICKATELL_REST_BASE_URL + resource)
      connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = method
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Accept", "application/json")
      connection.setRequestProperty("X-Version", "1")
      connection.setRequestProperty("Authorization", "Bearer $apiKey")
      var l = "0"
      if (dataPacket != null) {
        l = Integer.toString(dataPacket.toByteArray().size)
      }
      connection.setRequestProperty("Content-Length", "" + l)
      connection.setRequestProperty("Content-Language", "en-US")

      connection.useCaches = false
      connection.doInput = true
      connection.doOutput = dataPacket != null

      // Send request
      if (dataPacket != null) {
        val wr = DataOutputStream(
            connection.outputStream)
        wr.writeBytes(dataPacket)
        wr.flush()
        wr.close()
      }

      // Get Response
      connection.responseCode
      var stream: InputStream? = connection.errorStream
      if (stream == null) {
        stream = connection.inputStream
      }
      val rd = BufferedReader(
          InputStreamReader(stream!!))
      var line: String
      val response = StringBuffer()
      //while ((line = rd.readLine()) != null) {
      for (line in rd.lines()) {
        response.append(line)
        response.append('\n')
      }
      rd.close()
      return response.toString().trim { it <= ' ' }
    } catch (e: UnknownHostException) {
      throw e
    } catch (e: Exception) {
      return ""
    } finally {
      connection?.disconnect()
    }
  }

  /**
   * This is an internal function used to shorten other functions. Checks for
   * an error object, and throws it.
   *
   * @param obj
   * The object that needs to be checked.
   * @throws Exception
   * The exception that was found.
   */
  @Throws(Exception::class)
  private fun CheckForError(obj: JSONObject) {
    val objError = obj["error"] as JSONObject
    if (objError != null) {
      throw Exception(objError["description"] as String)
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
    var statusString: String? = null

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
     * @var The URL to use for the base of the REST API.
     */
    private val CLICKATELL_REST_BASE_URL = "https://api.clickatell.com/rest/"

    private val POST = "POST"
    private val GET = "GET"
    private val DELETE = "DELETE"
  }
}
