package org.inwrap

import org.apache.log4j.*
import org.jdiameter.api.*
import org.jdiameter.api.EventListener
import org.jdiameter.api.Stack
import org.jdiameter.server.impl.StackImpl
import org.jdiameter.server.impl.helpers.XMLConfiguration
import org.mobicents.diameter.dictionary.AvpDictionary
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*


class Client() : EventListener<Request, Answer> {
	init {
		//configure logging.
		configLog4j();
	}

	private val log = Logger.getLogger(ExampleClient::class.java)

	private val configFile = "client-jdiameter-config.xml"
	private val dictionaryFile = "dictionary.xml"
	//our destination
	private val serverHost = "127.0.0.1"
	private val serverPort = "3868"
	private val serverURI = "aaa://$serverHost:$serverPort"
	//our realm
	private val realmName = "exchange.example.org"
	// definition of codes, IDs
	private val commandCode = 686
	private val vendorID: Long = 66666
	private val applicationID: Long = 33333
	private val authAppId = ApplicationId.createByAuthAppId(applicationID)
	private val exchangeTypeCode = 888
	private val exchangeDataCode = 999
	// enum values for Exchange-Type AVP
	private val EXCHANGE_TYPE_INITIAL = 0
	private val EXCHANGE_TYPE_INTERMEDIATE = 1
	private val EXCHANGE_TYPE_TERMINATING = 2
	//list of data we want to exchange.
	private val TO_SEND = arrayOf("I want to get 3 answers", "This is second message", "Bye bye")
	//Dictionary, for informational purposes.
	private val dictionary = AvpDictionary.INSTANCE
	//stack and session factory
	private var stack: Stack? = null
	private var factory: SessionFactory? = null

	// ////////////////////////////////////////
	// Objects which will be used in action //
	// ////////////////////////////////////////
	private var session: Session? = null  // session used as handle for communication
	private var toSendIndex = 0  //index in TO_SEND table
	private var finished = false  //boolean telling if we finished our interaction

	fun initStack() {
		if (log.isInfoEnabled) {
			log.info("Initializing Stack...")
		}
		var `is`: InputStream? = null
		try {
			//Parse dictionary, it is used for user friendly info.
			dictionary.parseDictionary(this.javaClass.getClassLoader().getResourceAsStream(dictionaryFile))
			log.info("AVP Dictionary successfully parsed.")

			this.stack = StackImpl()
			//Parse stack configuration
			`is` = this.javaClass.getClassLoader().getResourceAsStream(configFile)
			val config = XMLConfiguration(`is`!!)
			factory = stack!!.init(config)
			if (log.isInfoEnabled) {
				log.info("Stack Configuration successfully loaded.")
			}
			//Print info about application
			val appIds = stack!!.metaData.localPeer.commonApplications

			log.info("Diameter Stack  :: Supporting " + appIds.size + " applications.")
			for (x in appIds) {
				log.info("Diameter Stack  :: Common :: " + x)
			}
			`is`!!.close()
			//Register network req listener, even though we wont receive requests
			//this has to be done to inform stack that we support application
			val network = stack!!.unwrap(Network::class.java)
			network.addNetworkReqListener(NetworkReqListener {
				//this wontbe called.
				null
			}, this.authAppId) //passing our example app id.

		} catch (e: Exception) {
			e.printStackTrace()
			if (this.stack != null) {
				this.stack!!.destroy()
			}

			if (`is` != null) {
				try {
					`is`.close()
				} catch (e1: IOException) {
					// TODO Auto-generated catch block
					e1.printStackTrace()
				}

			}
			return
		}

		val metaData = stack!!.metaData
		//ignore for now.
		if (metaData.stackType != StackType.TYPE_SERVER || metaData.minorVersion <= 0) {
			stack!!.destroy()
			if (log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
				log.error("Incorrect driver")
			}
			return
		}

		try {
			if (log.isInfoEnabled) {
				log.info("Starting stack")
			}
			stack!!.start()
			if (log.isInfoEnabled) {
				log.info("Stack is running.")
			}
		} catch (e: Exception) {
			e.printStackTrace()
			stack!!.destroy()
			return
		}

		if (log.isInfoEnabled) {
			log.info("Stack initialization successfully completed.")
		}
	}

	fun configure(requestDo: RequestDo) {

	}

	/**
	 * @return
	 */
	fun finished(): Boolean {
		return this.finished
	}

	/**
	 *
	 */
	fun start() {
		try {
			//wait for connection to peer
			try {
				Thread.sleep(5000)
			} catch (e: InterruptedException) {
				// TODO Auto-generated catch block
				e.printStackTrace()
			}

			//do send
			this.session = this.factory!!.getNewSession("BadCustomSessionId;YesWeCanPassId;" + System.currentTimeMillis())
			sendNextRequest(EXCHANGE_TYPE_INITIAL)
		} catch (e: InternalException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		} catch (e: IllegalDiameterStateException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		} catch (e: RouteException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		} catch (e: OverloadException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		}

	}

	@Throws(InternalException::class, IllegalDiameterStateException::class, RouteException::class, OverloadException::class)
	private fun sendNextRequest(enumType: Int) {
		val r = this.session!!.createRequest(commandCode, this.authAppId, realmName, serverURI)
		// here we have all except our custom avps

		val requestAvps = r.getAvps()
		// code , value , vendor, mandatory,protected,isUnsigned32
		// (Enumerated)
		val exchangeType = requestAvps.addAvp(exchangeTypeCode, enumType.toLong(), vendorID, true, false, true) // value
		// is
		// set
		// on
		// creation
		// code , value , vendor, mandatory,protected, isOctetString
		val exchengeData = requestAvps.addAvp(exchangeDataCode, TO_SEND[toSendIndex++], vendorID, true, false, false) // value
		// is
		// set
		// on
		// creation
		// send
		this.session!!.send(r, this)
		dumpMessage(r, true) //dump info on console
	}

	/*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.EventListener#receivedSuccessMessage(org.jdiameter
   * .api.Message, org.jdiameter.api.Message)
   */
	override fun receivedSuccessMessage(request: Request, answer: Answer) {
		dumpMessage(answer, false)
		if (answer.commandCode != commandCode) {
			log.error("Received bad answer: " + answer.commandCode)
			return
		}
		val answerAvpSet = answer.avps

		val exchangeTypeAvp = answerAvpSet.getAvp(exchangeTypeCode, vendorID)
		val exchangeDataAvp = answerAvpSet.getAvp(exchangeDataCode, vendorID)
		val resultAvp = answer.resultCode


		try {
			//for bad formatted request.
			if (resultAvp.unsigned32 == 5005L || resultAvp.unsigned32 == 5004L) {
				// missing || bad value of avp
				this.session!!.release()
				this.session = null
				log.error("Something wrong happened at server side!")
				finished = true
			}
			var data: String
			when (exchangeTypeAvp.unsigned32.toInt()) {
				EXCHANGE_TYPE_INITIAL -> {
					// JIC check;
					data = exchangeDataAvp.utF8String
					if (data == TO_SEND[toSendIndex - 1]) {
						// ok :) send next;
						sendNextRequest(EXCHANGE_TYPE_INTERMEDIATE)
					} else {
						log.error("Received wrong Exchange-Data: " + data)
					}
				}
				EXCHANGE_TYPE_INTERMEDIATE -> {
					// JIC check;
					data = exchangeDataAvp.utF8String
					if (data == TO_SEND[toSendIndex - 1]) {
						// ok :) send next;
						sendNextRequest(EXCHANGE_TYPE_TERMINATING)
					} else {
						log.error("Received wrong Exchange-Data: " + data)
					}
				}
				EXCHANGE_TYPE_TERMINATING -> {
					data = exchangeDataAvp.utF8String
					if (data == TO_SEND[toSendIndex - 1]) {
						// good, we reached end of FSM.
						finished = true
						// release session and its resources.
						this.session!!.release()
						this.session = null
					} else {
						log.error("Received wrong Exchange-Data: " + data)
					}
				}
				else -> log.error("Bad value of Exchange-Type avp: " + exchangeTypeAvp.unsigned32)
			}
		} catch (e: AvpDataException) {
			// thrown when interpretation of byte[] fails
			e.printStackTrace()
		} catch (e: InternalException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		} catch (e: IllegalDiameterStateException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		} catch (e: RouteException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		} catch (e: OverloadException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		}

	}

	/*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.EventListener#timeoutExpired(org.jdiameter.api.
   * Message)
   */
	override fun timeoutExpired(request: Request) {


	}

	private fun dumpMessage(message: Message, sending: Boolean) {
		if (log.isInfoEnabled) {
			log.info((if (sending) "Sending " else "Received ") + (if (message.isRequest) "Request: " else "Answer: ") + message.commandCode + "\nE2E:"
					+ message.endToEndIdentifier + "\nHBH:" + message.hopByHopIdentifier + "\nAppID:" + message.applicationId)
			log.info("AVPS[" + message.avps.size() + "]: \n")
			try {
				printAvps(message.avps)
			} catch (e: AvpDataException) {
				// TODO Auto-generated catch block
				e.printStackTrace()
			}

		}
	}

	@Throws(AvpDataException::class)
	private fun printAvps(avpSet: AvpSet) {
		printAvpsAux(avpSet, 0)
	}

	/**
	 * Prints the AVPs present in an AvpSet with a specified 'tab' level
	 *
	 * @param avpSet
	 * the AvpSet containing the AVPs to be printed
	 * @param level
	 * an int representing the number of 'tabs' to make a pretty
	 * print
	 * @throws AvpDataException
	 */
	@Throws(AvpDataException::class)
	private fun printAvpsAux(avpSet: AvpSet, level: Int) {
		val prefix = "                      ".substring(0, level * 2)

		for (avp in avpSet) {
			val avpRep = AvpDictionary.INSTANCE.getAvp(avp.code, avp.vendorId)

			if (avpRep != null && avpRep.type == "Grouped") {
				log.info(prefix + "<avp name=\"" + avpRep.name + "\" code=\"" + avp.code + "\" vendor=\"" + avp.vendorId + "\">")
				printAvpsAux(avp.grouped, level + 1)
				log.info(prefix + "</avp>")
			} else if (avpRep != null) {
				var value = ""

				if (avpRep.type == "Integer32")
					value = avp.integer32.toString()
				else if (avpRep.type == "Integer64" || avpRep.type == "Unsigned64")
					value = avp.integer64.toString()
				else if (avpRep.type == "Unsigned32")
					value = avp.unsigned32.toString()
				else if (avpRep.type == "Float32")
					value = avp.float32.toString()
				else
				//value = avp.getOctetString();
					value = String(avp.octetString, StandardCharsets.UTF_8)

				log.info(prefix + "<avp name=\"" + avpRep.name + "\" code=\"" + avp.code + "\" vendor=\"" + avp.vendorId
						+ "\" value=\"" + value + "\" />")
			}
		}
	}

	private fun configLog4j() {
		val inStreamLog4j = ExampleClient::class.java.classLoader.getResourceAsStream("log4j.properties")
		val propertiesLog4j = Properties()
		try {
			propertiesLog4j.load(inStreamLog4j)
			PropertyConfigurator.configure(propertiesLog4j)
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			if (inStreamLog4j != null) {
				try {
					inStreamLog4j.close()
				} catch (e: IOException) {
					// TODO Auto-generated catch block
					e.printStackTrace()
				}

			}
		}
	}

}

fun main(args: Array<String>) {
	val ec = Client()
	ec.initStack()
	ec.start()

	while (!ec.finished()) {
		try {
			Thread.sleep(5000)
		} catch (e: InterruptedException) {
			// TODO Auto-generated catch block
			e.printStackTrace()
		}
	}
}