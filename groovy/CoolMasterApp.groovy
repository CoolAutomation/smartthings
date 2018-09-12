
import groovy.json.JsonSlurper

definition(
  name: "CoolMaster Air Conditioner Hub",
  namespace: "coolautomation",
  author: "coolautomation",
  description: "CoolMaster SmartApp",
  category: "My Apps",
  singleInstance: true
)

preferences {
	page(name: "deviceProperty", title: "Device Setup", install: true, uninstall: true){
  		section("SmartThings Hub") {
        	input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
  		}
  		section("CoolMasterNet") {
    		input "CM_ip", "text", title: "IP Address", description: "(ie. 192.168.1.10)", required: true, defaultValue: "192.168.16.124"
    		input "CM_serial", "text", title: "Serial number", description: "(ie. 283B960004AA)", required: true, defaultValue: "283B960004AA"
  		}
        section("Misc"){
        	input "userTempUnit","enum",title: "User Temperature Unit", description: "Temperature Unit",requied: false, options:["C","F"], submitOnChange: true, defaultValue: "C"
        }
   }
}

def installed() {
	subscribeToEvents()
    pollDevices()
    schedule("0/20 * * * * ?", pollDevices)
	
}

def subscribeToEvents() {
	subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def uninstalled() {
	unschedule()
    removeChildDevices()
}

def updated(){
	getAllChildDevices().each{dev->
    	dev.updateSettings(settings)
    }
}

def lanResponseHandler(evt) {
	def map = stringToMap(evt.stringValue)
  	if (map.ip == convertIPtoHex(settings.CM_ip) &&
		map.port == convertPortToHex(settings.CM_port)) {
    	if (map.mac) {
    		state.proxyMac = map.mac
    	}
    }

	if (map.mac != state.proxyMac) {
    	return
  	}

  	def headers = getHttpHeaders(map.headers);
  	def body = getHttpBody(map.body);
  	log.debug "Body: ${body}"
  	processEvent(body)
}

def sendCommandToCoolMaster (Map data) {
	def path
    def deviceId = data.deviceId
    def params = data.params
    def command = data.name
    
	log.debug "CM send command: ${command} to indoor ${deviceId} with params ${params}"
    if( command.contains("ls") )
    	path = "/v2/device/${CM_serial}/${command}"
    else
    	path = "/v1/device/${CM_serial}/raw?command=${command}"
	
    if( deviceId ){
    	deviceId = deviceId.replaceAll('\\.','_')
    	path=path.concat("&${deviceId}")
    }
    
    if(params){
    	path=path.concat("&${params}")
    }
    
    if (settings.CM_ip.length() == 0 ||
    	settings.CM_port.length() == 0) {
    	log.error "SmartThings CoolAutomation configuration not set!"
    	return
  	}
	log.debug "path: ${path}"
  	def host = getProxyAddress()
  	def headers = [:]
  	headers.put("HOST", host)
   
	def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
    	path: path,
    	headers: headers
	)
	sendHubCommand(hubAction)
}

private processEvent(evt) {
	if( evt && evt.command ){
  		if (evt.command == "ls") {
    		updateChildDevices(evt.data)
  		}
    }
  
}

private updateChildDevices(units){
	log.debug "fillChildDevicesDB"
	units.each{
    		def device = getChildDevice(it.uid);
    		if(!device){
				device = addChildDevice("CoolMasterIndoor", it.uid, hostHub.id, ["name": "CoolMaster Device", label: "${it.uid}", completedSetup: true])
    			log.debug "Added device: ${it.uid} onoff: ${it.onoff} setpoint: ${it.st} temp:${it.rt} mode:${it.mode} fspeed: ${it.fspeed}"
            }
            it.put("userTempUnit",settings.userTempUnit)
            device.parseData(it)
  	}
    getAllChildDevices().each{dev->
    	if( !units.find{it.uid==dev.deviceNetworkId} ){
        	log.debug "Device ${dev.deviceNetworkId} was deleted"
        	deleteChildDevice(dev.deviceNetworkId)
        }
    }
}

private removeChildDevices() {
  	getAllChildDevices().each{dev-> 
  		deleteChildDevice(dev.deviceNetworkId) 
  	}
}

private getHttpHeaders(headers) {
	def obj = [:]
    if(headers)
    {
    	new String(headers.decodeBase64()).split("\r\n").each {param ->
    		def nameAndValue = param.split(":")
    		obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
		}
    }
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(new String(body.decodeBase64()))
  }
  return obj
}

private getProxyAddress() {
	return settings.CM_ip + ":" + settings.CM_port
}

private getNotifyAddress() {
  return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

private String convertIPtoHex(ipAddress) {
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() ).toUpperCase()
  return hexport
}

def pollDevices(){
	sendCommandToCoolMaster(name:"ls")
}
