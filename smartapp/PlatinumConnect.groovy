/**
 *  PlatinumGateway Service Manager
 *
 *  Author: Schwark Satyavolu
 *. nc -i3 <ip-address-of-gateway> 522 < input.txt > output.txt
 *
 */
definition(
    name: "Hunter Douglas Platinum Gateway",
    namespace: "schwark",
    author: "Schwark Satyavolu",
    description: "Allows you to connect your Hunter Douglas Platinum Gateway shades with SmartThings and control them from your Things area or Dashboard in the SmartThings Mobile app. Adjust colors by going to the Thing detail screen for your PlatinumGateway shades (tap the gear on PlatinumGateway tiles).",
    category: "SmartThings Labs",
    iconUrl: "https://lh5.ggpht.com/FN3-xG6R0q9VjJHYE1iK5K2J11rTphiDEePr8XluI6o_s52xfPoHwt0-TZxc0qlVSQ=w300",
    iconX2Url: "https://lh5.ggpht.com/FN3-xG6R0q9VjJHYE1iK5K2J11rTphiDEePr8XluI6o_s52xfPoHwt0-TZxc0qlVSQ=w300",
    singleInstance: true
)

preferences {
	input("gatewayIP", "string", title:"Gateway IP Address", description: "Please enter your gateway's IP Address", required: true, displayDuringSetup: true)
	input("statusURL", "string", title:"Gateway Status URL", description: "Please enter the URL to download status", required: false, displayDuringSetup: true)
	input("statusText", "string", title:"Gateway Status Text", description: "Please copy paste the status Text", required: false, displayDuringSetup: true)

}

def makeNetworkId(ipaddr, port) { 
	String hexIp = ipaddr.tokenize('.').collect {String.format('%02X', it.toInteger()) }.join() 
	String hexPort = String.format('%04X', port.toInteger()) 
	log.debug "The target device is configured as: ${hexIp}:${hexPort}" 
	return "${hexIp}:${hexPort}" 
}

/////////////////////////////////////
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def uninstalled() {
	log.debug("Uninstalling with settings: ${settings}")
	if(state.scenes) {
		// remove scene child devices
		state.scenes = []
	}
	if(state.windows) {
		// remove window child devices
		state.windows = []
	}
	removeChildDevices(getChildDevices())
}

/////////////////////////////////////
def updated() {
	//log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

/////////////////////////////////////
def initialize() {
	// remove location subscription aftwards
	unsubscribe()
	state.subscribe = false
	log.debug("gatewayIP is ${gatewayIP}")

	if (gatewayIP) {
		addgateway()
	}

	if(statusURL) {
		try {
			
    		httpGet(statusURL) { resp ->
        		resp.headers.each {
        			log.debug "${it.name} : ${it.value}"
    			}
    			log.debug "response contentType: ${resp.contentType}"
    			//log.trace "response data: ${resp.data}"
    			if(resp.status == 200) {
    				state.statusText = resp.data
    			}
    		}
    		
		} catch (e) {
    			log.error "something went wrong: $e"
		}
	}

	if(statusText) {
		state.statusText = new StringReader(statusText)
 	}

 	doDeviceSync()
}

def getHubId() {
	return state.hubId ? state.hubId : location.hubs[0].id
}

/////////////////////////////////////
def addshades() {
}

def addgateway() {
	if(!state.gatewayHex) {
		state.gatewayHex = makeNetworkId(gatewayIP,522)
	}
}


/////////////////////////////////////
def locationHandler(evt) {
	log.debug "$locationHandler(evt.description)"
	def description = evt.description
	def hub = evt?.hubId
	state.hubId = hub
	log.debug("location handler: event description is ${description}")

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent.headers && parsedEvent.body) { // HUE gateway RESPONSES
	}
	else {
		log.debug "GOT EVENT --- ${evt} --- NOT A HUE"
	}
}

/////////////////////////////////////
private def parseEventMessage(Map event) {
	//handles gateway attribute events
	return event
}

private def parseEventMessage(String description) {
	def event = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ip = valueString
			}
		}
		else if (part.startsWith('deviceAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.port = valueString
			}
		}
		else if (part.startsWith('ssdpPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpUSN = valueString
			}
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpTerm = valueString
			}
		}
		else if (part.startsWith('headers')) {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString) {
				event.headers = valueString
			}
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			def valueString = part.trim()
			if (valueString) {
				event.body = valueString
			}
		}
	}

	event
}

/////////////////////////////////////
def doDeviceSync(){
	log.debug "Doing Platinum Gateway Device Sync!"
	runIn(300, "doDeviceSync") //schedule to run again in 5 minutes

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	updateStatus()
}

def processState(info) {
  log.debug("processing state...")
  def DB = ['rooms':[:], 'shades':[:], 'scenes':[:]]
  def prefix = ""
  //def lines = info.split(/[\n\r]+/)

  info.eachLine() { line ->
    line = line.trim()
    if(!prefix) {
      prefix = line[0..1]
      log.debug("prefix is set to ${prefix}")
    }
    else if(!line.startsWith(prefix)) {
      return
    }
    
    line = line.drop(2)
  	log.debug("processing line ${line}")
    if(line.startsWith("\$cr")) {
      // name of room
      def room_id = line[3..4]
      def room_name = line.split('-')[-1].trim()
      log.debug("found room with ${room_id} and ${room_name}")
      DB['rooms'][room_id] = ['name':room_name, 'id':room_id, 'search':room_name.toLowerCase()]
    } else if(line.startsWith("\$cm")) {
      // name of scene
      def scene_id = line[3..4]
      def scene_name = line.split('-')[-1].trim()
      log.debug("found scene with ${scene_id} and ${scene_name}")
      DB['scenes'][scene_id] = ['name':scene_name, 'id':scene_id, 'search':scene_name.toLowerCase()]
    } else if(line.startsWith("\$cs")) {
      // name of a shade
      def parts = line.split('-')
      def shade_id = line[3..4]
      def shade_name = parts[-1].trim()
      def room_id = parts[1]
      log.debug("found shade with ${shade_id} and ${shade_name}")
      DB['shades'][shade_id] = ['name':shade_name, 'id':shade_id, 'search':shade_name.toLowerCase(), 'room': room_id]
    } else if(line.startsWith("\$cp")) {
      // state of a shade
      def shade_id = line[3..4]
      def stateTxt = line[-4..-2]
      def state = stateTxt.toInteger()/255.0
      log.debug("found shade state with ${shade_id} and ${state}")
      def shade = DB['shades'][shade_id]
      if(shade) {
        DB['shades'][shade_id]['state'] = state
      }
    }
   }

    log.debug("DB is ${DB}")
    return DB
}



////////////////////////////////////////////
//CHILD DEVICE METHODS

/////////////////////////////////////
def parse(childDevice, description) {
	def parsedEvent = parseEventMessage(description)

	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		log.debug "parse() - ${bodyString}"

		def body = new groovy.json.JsonSlurper().parseText(bodyString)

		if (body instanceof java.util.HashMap)
		{ //poll response
			def shades = getChildDevices()
			def d = shades.find{it.label == body.name}
			if (d) {
				sendEvent(d.deviceNetworkId, [name: "switch", value: body?.action?.on ? "on" : "off"])
				sendEvent(d.deviceNetworkId, [name: "level", value: Math.round(body.action.bri * 100 / 255)])
				sendEvent(d.deviceNetworkId, [name: "saturation", value: Math.round(body.action.sat * 100 / 255)])
				sendEvent(d.deviceNetworkId, [name: "hue", value: Math.min(Math.round(body.action.hue * 100 / 65535), 65535)])
			}
		}
		else
		{ //put response
			body.each { payload ->
				log.debug $payload
				if (payload?.success)
				{
					def childDeviceNetworkId = app.id + "/"
					def eventType
					body?.success[0].each { k,v ->
						childDeviceNetworkId += k.split("/")[2]
						eventType = k.split("/")[4]
						log.debug "eventType: $eventType"
						switch(eventType) {
							case "on":
								sendEvent(childDeviceNetworkId, [name: "switch", value: (v == true) ? "on" : "off"])
								break
							case "bri":
								sendEvent(childDeviceNetworkId, [name: "level", value: Math.round(v * 100 / 255)])
								break
							case "sat":
								sendEvent(childDeviceNetworkId, [name: "saturation", value: Math.round(v * 100 / 255)])
								break
							case "hue":
								sendEvent(childDeviceNetworkId, [name: "hue", value: Math.min(Math.round(v * 100 / 65535), 65535)])
								break
						}
					}

				}
				else if (payload.error)
				{
					log.debug "JSON error - ${body?.error}"
				}

			}
		}
	} else {
		log.debug "parse - got something other than headers,body..."
		return []
	}
}

def sendMessage(msg) {
	def newDNI = state.gatewayHex
	if(newDNI) {
		log.debug("sending ${msg} to ${newDNI}")
		def ha = new physicalgraph.device.HubAction(msg,physicalgraph.device.Protocol.LAN, newDNI)
		sendHubCommand(ha)
	}
}

/////////////////////////////////////
def runScene(sceneID) {
	log.debug "Running Scene ${sceneID}"
	sceneID = String.format('%02d',sceneID.toInteger())
	def msg = "\$inm${sceneID}-"
	sendMessage(msg)
}

def updateStatus() {
	if(!state.statusText) return
	log.debug ("Updating status")


	def DB = processState(state.statusText)
	if(!state.scenes) {
		state.scenes = [:]
	}
	state.scenes.each() { id, sceneDevice ->
		if(DB['scenes'][id]) {
			// update device
			if(DB['scenes'][id]['name'] != sceneDevice.label) {
				log.debug("processing scene ${id} from name ${sceneDevice.label} to ${DB['scenes'][id]['name']}")
				sceneDevice.setLabel(DB['scenes'][id]['name'])
			}
			DB['scenes'].remove(id)
		} else {
			// remove device
			log.debug("removing scene ${id} from name ${sceneDevice.displayName}")
			deleteChildDevice(sceneDevice.deviceNetworkId)
		}
	}
	DB['scenes']?.each() { id, name ->
		log.debug("processing scene ${id} with name ${name}")
		def PREFIX = "PLATINUMGATEWAYSCENE"
		def sceneDevice = addChildDevice("schwark", "Platinum Gateway Scene Switch", "${PREFIX}${id}", getHubId(), ["label": name])
		sceneDevice.sceneNo = id
		state.scenes[id] = sceneDevice
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Boolean canInstallLabs()
{
	return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware)
{
	return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions()
{
	return location.hubs*.firmwareVersionString.findAll { it }
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
