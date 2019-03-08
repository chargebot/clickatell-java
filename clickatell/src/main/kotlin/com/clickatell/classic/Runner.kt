package com.clickatell.classic

import java.net.UnknownHostException

/**
 * @date Dec 2, 2014
 * @author Dominic Schaff <dominic.schaff></dominic.schaff>@gmail.com>
 */
object Runner {

  var USERNAME = "YOUR_USERNAME"
  var APIID = "YOUR_API_ID"
  var PASSWORD = "YOUR_PASSWORD"
  var APIKEY = "YOUR_KEY"

  @JvmStatic
  fun main(args: Array<String>) {
    TestHttp()
    TestRest()
  }

  fun TestHttp() {
    println("STARTING WITH TESTING HTTP:")

    // Create New object (Assign auth straight away.):
    val click = ClickatellHttpClassic(
      USERNAME,
      APIID,
      PASSWORD
    )

    // Using click, test auth:
    println("TESTING GET AUTH")
    try {
      if (click.testAuth()) {
        println("Authentication was successful")
      } else {
        println("Your authentication details are not correct")
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    }

    // Assuming the auth was successful, lets send one message, to one
    // recipient:
    println("\nTESTING GET BALANCE")
    try {
      val response = click.balance
      println(response)

    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

    // Assuming the auth was successful, lets send one message, to one
    // recipient:
    println("\nTESTING SEND SINGLE MESSAGE")
    try {
      val response = click.sendMessage("27821234567",
                                       "Hello, this is a test message!")
      System.out.println(response)
      if (response.error != null) {
        System.out.println(response.error)
      } else {

        println("\nTESTING GET STATUS:")
        System.out.println(click.getMessageStatus(response.message_id!!))
        println("\nTESTING STOP:")
        System.out.println(click.stopMessage(response.message_id!!))
        println("\nTESTING GET CHARGE:")
        val replies = click.getMessageCharge(response.message_id!!)
        System.out.println("Charge: " + replies.charge)
        System.out.println("Status: " + replies.status)
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

    // Lets send one message to multiple people:
    println("\nTESTING SEND MULTIPLE NUMBERS ONE MESSAGE")
    try {
      val replies = click.sendMessage(arrayOf("27821234567", "27829876543"), "Test message")
      for (s in replies) {
        System.out.println(s)
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

    // Lets do a coverage test:
    println("\nTESTING COVERAGE")
    try {
      val reply = click.getCoverage("27820909090")
      if (reply < 0) {
        println("Route coverage failed")
      } else {
        println("Route coverage succeded, message could cost:$reply")
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

  }

  fun TestRest() {
    println("\n\nSTARTING WITH TESTING REST:")

    // Create New object (Assign auth straight away.):
    val click = ClickatellRestClassic(APIKEY)

    // We cannot test for auth, so lets start with balance:
    println("TESTING GET BALANCE")
    try {
      val response = click.balance
      println(response)
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

    // Assuming the auth was successful, lets send one message, to one
    // recipient:
    println("\nTESTING SEND SINGLE MESSAGE")
    try {
      val response = click.sendMessage("27821234567",
                                       "Hello, this is a test message!")
      println(response)
      if (response.error != null) {
        println(response.error)
      } else {
        println("\nTESTING STOP:")
        println(click.stopMessage(response.message_id!!))
        println("\nTESTING GET STATUS:")
        val msg = click
            .getMessageStatus(response.message_id!!)
        println("ID:" + msg.message_id!!)
        println("Status:" + msg.status!!)
        println("Status Description:" + msg.statusString!!)
        println("Charge:" + msg.charge!!)
        println("\nTESTING STOP MESSAGE")
        val msgStatus = click
            .stopMessage(response.message_id!!)
        println("ID:" + msgStatus.message_id!!)
        println("Status:" + msgStatus.status!!)
        println("Status Description:" + msgStatus.statusString!!)
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

    // Lets send one message to multiple people:
    println("\nTESTING SEND MULTIPLE NUMBERS ONE MESSAGE")
    try {
      val replies = click.sendMessage(arrayOf("27821234567", "27829876543", "000"), "Test message")
      for (s in replies) {
        println(s)
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

    // Lets do a coverage test:
    println("\nTESTING COVERAGE")
    try {
      val reply = click.getCoverage("27820909090")
      if (reply < 0) {
        println("Route coverage failed")
      } else {
        println("Route coverage succeded, message could cost:$reply")
      }
    } catch (e: UnknownHostException) {
      println("Host could not be found")
    } catch (e: Exception) {
      println(e.message)
    }

  }
}
