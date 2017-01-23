/**
 *  Hunter Douglas Platinum Gateway Bridge for SmartThings
 *  Schwark Satyavolu
 *  Originally based on: Allan Klein's (@allanak) and Mike Maxwell's code
 *
 *  Usage:
 *  1. Add this code as a device handler in the SmartThings IDE
 *  3. Create a device using PlatinumGatewayBridge as the device handler using a hexadecimal representation of IP:port as the device network ID value
 *  For example, a gateway at 192.168.1.222:522 would have a device network ID of C0A801DE:20A
 *  Note: Port 522 is the default Hunter Douglas Platinum Gateway port so you shouldn't need to change anything after the colon
 *  4. Enjoy the new functionality of the SmartThings app
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Platinum Gateway Bridge", namespace: "schwark", author: "Schwark Satyavolu") {
	command "makeNetworkId", ["string","string"]
	command "runScene", ["string"]
	command "windowControl", ["string", "string"]
}

simulator {
		// TODO: define status and reply messages here
	}

tiles {
		standardTile("icon", "icon", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "default", label: "Hue Bridge", action: "", icon: "st.Lighting.light99-hue", backgroundColor: "#FFFFFF"
		}
		valueTile("networkAddress", "device.networkAddress", decoration: "flat", height: 1, width: 2, inactiveLabel: false) {
			state "default", label:'${currentValue}', height: 1, width: 2, inactiveLabel: false
		}

}

preferences {
    input name: "gateway", type: "text", title: "Gateway IP", description: "Enter Platinum Gateway IP address", required: true,
          displayDuringSetup: true
}

		main (["icon"])
		details(["networkAddress"])
}

// parse events into attributes
def parse(description) {
	log.debug("description is ${description}")
    def msg = parseLanMessage(description)
    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)
    log.debug("body is ${body}")
    log.debug("data is ${data}")
}

def makeNetworkId(ipaddr, port) { 
	String hexIp = ipaddr.tokenize('.').collect {String.format('%02X', it.toInteger()) }.join() 
	String hexPort = String.format('%04X', port.toInteger()) 
	log.debug "The target device is configured as: ${hexIp}:${hexPort}" 
	return "${hexIp}:${hexPort}" 
}

def updated() {
	//device.deviceNetworkId should be writeable now..., and its not...
	//device.deviceNetworkId = makeNetworkId(gateway,522)	
}

def updateNetworkID() {
	def newDNI = makeNetworkId(gateway,522)
	device.deviceNetworkId = newDNI
	//device.setDeviceNetworkId(newDNI)
	return newDNI	
}

def runScene(sceneID) {
	log.debug("Running Scene ${sceneID}")
	def id = sceneID.toInteger()
	sceneID = String.format('%02d',sceneID.toInteger())
	def msg = "\$inm${sceneID}-"
	def newDNI = updateNetworkID()
	log.debug("sending ${msg} to ${newDNI}")
	def ha = new physicalgraph.device.HubAction(msg,physicalgraph.device.Protocol.LAN)
	//sendEvent(name: "switch", value: "off")
	return ha	
}

def windowControl(windowID, percent) {
	log.debug("Controlling ${windowID} to ${percent} up")
	def id = windowID.toInteger()
	windowID = String.format('%02d',windowID.toInteger())
	def pct = percent.toInteger()
	def windowVal = String.format('%03d', Integer.round(pct * 255))
	def msg = "\$pss${windowID}-04-${windowVal}"
	def newDNI = updateNetworkID()
	log.debug("sending ${msg} to ${newDNI}")
	def ha = new physicalgraph.device.HubAction(msg,physicalgraph.device.Protocol.LAN)
	msg = "\$rls"
	log.debug("sending ${msg} to ${newDNI}")
	ha = new physicalgraph.device.HubAction(msg,physicalgraph.device.Protocol.LAN)
	return ha
}
