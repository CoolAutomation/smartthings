/**
 *  CoolMaster Generic Indoor
 *	ver 1.0
 *
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
 import groovy.transform.Field
 import groovy.time.TimeCategory

// enummaps
@Field final Map      MODE = [
    COOL:  "cool",
    HEAT:  "heat",
    FAN:   "fan",
    AUTO:  "auto"
]

@Field final Map      OP_STATE = [
    IDLE:   "idle",
    COOL:  "cool",
    HEAT:  "heat",
    FAN:   "fan",
    AUTO:  "auto"
]

@Field final Map      FAN_MODE = [
	LOW:	"low",
    MED: 	"med",
    HIGH:   "high",
    AUTO:   "auto"
]

@Field final Map SETPOINT_TYPE = [
    COOLING: "cool",
    HEATING: "heat"
]

@Field List MODES = [MODE.COOL, MODE.HEAT, MODE.AUTO, MODE.FAN ]
@Field List FAN_MODES = [FAN_MODE.LOW, FAN_MODE.MED, FAN_MODE.HIGH,FAN_MODE.AUTO]
@Field List OP_STATES = [OP_STATE.IDLE, OP_STATE.COOL, OP_STATE.HEAT, OP_STATE.AUTO, OP_STATE.FAN]


@Field final Integer  MIN_SETPOINT = 16
@Field final Integer  MAX_SETPOINT = 35

@Field final Integer COOLING_SETPOINT_MAX = 28
@Field final Integer COOLING_SETPOINT_MIN = 16
@Field final Integer HEATING_SETPOINT_MAX = 35
@Field final Integer HEATING_SETPOINT_MIN = 20
@Field final Integer AUTO_SETPOINT_MAX = 32
@Field final Integer AUTO_SETPOINT_MIN = 16

// derivatives
@Field final IntRange FULL_SETPOINT_RANGE = (MIN_SETPOINT..MAX_SETPOINT)
@Field final IntRange HEATING_SETPOINT_RANGE = (20..35)
@Field final IntRange COOLING_SETPOINT_RANGE = (16..28)
@Field final IntRange AUTO_SETPOINT_RANGE = (16..32)

// defaults
@Field final String   DEFAULT_MODE = MODE.AUTO
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.LOW
@Field final String   DEFAULT_SETPOINT_TYPE = SETPOINT_TYPE.COOLING
@Field final Integer  DEFAULT_TEMPERATURE = 22
@Field final Integer  DEFAULT_HEATING_SETPOINT = 26
@Field final Integer  DEFAULT_COOLING_SETPOINT = 20
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = DEFAULT_COOLING_SETPOINT
@Field final String	  DEFAULT_TEMP_UNIT = "C"
@Field final String   DEFAULT_OP_STATE = OP_STATE.AUTO
@Field final Long	  CMD_TIMEOUT = 500

metadata {
  definition (name: "CoolMasterIndoor", namespace: "coolautomation", author: "coolautomation") {

    /**
     * List our capabilties. Doing so adds predefined command(s) which
     * belong to the capability.
     */
    capability "Switch"
    capability "Thermostat"
    capability "Temperature Measurement"
    capability "Health Check"
	
    command "cycleMode"
    command "cycleFanMode"
    command "setpointUp"
    command "setpointDown"
    command "reset"
    command "poll"
    
    
	/* For view */
    attribute "indoorOperatingState","string"
    attribute "temperatureUnit", "string"
}

  /**
   * Define the various tiles and the states that they can be in.
   * The 2nd parameter defines an event which the tile listens to,
   * if received, it tries to map it to a state.
   *
   * You can also use ${currentValue} for the value of the event
   * or ${name} for the name of the event. Just make SURE to use
   * single quotes, otherwise it will only be interpreted at time of
   * launch, instead of every time the event triggers.
   */
  tiles(scale: 2) {
    multiAttributeTile(name:"indoor", type:"thermostat", width:6, height:4) {
             tileAttribute("device.thermostatSetpoint", key: "PRIMARY_CONTROL") {
        		attributeState("temp", label:'${currentValue}', unit:"dC", defaultState: true)
    		}
            tileAttribute("device.temperatureUnit", key: "VALUE_CONTROL") {
                attributeState("VALUE_UP", action: "setpointUp",label:'${currentValue}')
                attributeState("VALUE_DOWN", action: "setpointDown",label:'${currentValue}')
            }
            tileAttribute("device.temperature", key: "SECONDARY_CONTROL") {
            	attributeState( "default",  label: '${currentValue}°', icon: "st.alarm.temperature.normal")
            }
            tileAttribute("device.indoorOperatingState", key: "OPERATING_STATE") {
                attributeState("idle", backgroundColor: "#D3D3D3")
                attributeState("cool", backgroundColor: "#00A0DC")
                attributeState("heat", backgroundColor: "#E86D13")
                attributeState("fan", backgroundColor: "#7D33E5")
                attributeState("auto", backgroundColor: "#CB0EF3")
                
            }
            tileAttribute("device.indoorOperatingState", key: "THERMOSTAT_MODE") {
                attributeState("idle",  label: '${name}')
                attributeState("cool", label: '${name}')
                attributeState("heat", label: '${name}')
                attributeState("fan", label: '${name}')
                attributeState("auto", label: '${name}')
            }
        }
		standardTile("powerButton", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "off", action: "switch.on", nextState: "on", label:'${name}', backgroundColor: "#CCCCCC"
            state "on",	action: "switch.off", nextState: "off", label:'${name}', backgroundColor: "#113079"
        }
        standardTile("mode", "device.thermostatMode", width: 2, height: 2, decoration: "flat") {
        	state "idle", action: "cycleMode", icon: "st.thermostat.heating-cooling-off", backgroundColor: "#CCCCCC", defaultState: true
            state "cool", action: "cycleMode", icon: "st.thermostat.cool", nextState:"updating"
            state "heat", action: "cycleMode", icon: "st.thermostat.heat", nextState:"updating"
            state "fan", action: "cycleMode", icon: "st.vents.vent-open", nextState:"updating"
            state "auto", action: "cycleMode", icon: "st.thermostat.auto", nextState:"updating"
            state "updating", label:"Updating..."
        }
        standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, decoration: "flat") {
        	state "off", action: "cycleFanMode", icon: "st.thermostat.fan-off"
            state "auto", action: "cycleFanMode", icon: "st.thermostat.fan-auto", nextState:"updating"
            state "low", action: "cycleFanMode", label:"low", icon: "st.thermostat.fan-on", nextState:"updating"
            state "med", action: "cycleFanMode", label:"med",icon: "st.thermostat.fan-on", nextState:"updating"
            state "high", action: "cycleFanMode", label:"high",icon: "st.thermostat.fan-on", nextState:"updating"
            state "updating", label:"Updating..."
            
        }
        valueTile("forMainPage", "device.thermostatSetpoint", decoration: "flat") {
            state "default",  label: '${currentValue}°',backgroundColor: "#113079"
        }
        standardTile("reset", "device.status",  width: 2, height:1,inactiveLabel: false, decoration: "flat") {
      		state "default", action:"reset", icon:"st.secondary.refresh", backgroundColor:"#ffffff"
    	}
        standardTile("status", "device.statusString",  width: 4, height: 1,decoration: "flat") {
            state "default",  label: 'Status: ${currentValue}',backgroundColor: "#ffffff"
        }
        
    main "forMainPage"

    details([
      "indoor",
      "powerButton",
      "mode",
      "fanMode",
      "reset",
      "status"
    ])
  }
 
 
}

def installed() {
    log.trace "CM: Executing 'installed' ${device.label}"
    
    sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
    sendEvent(name: "healthStatus", value: "online")
    sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
    
    sendEvent( name: "supportedThermostatFanModes", value: supportedThermostatFanModes() )
    sendEvent(name: "supportedThermostatModes",value: MODES )
    
    sendEvent(name: "coolingSetpoint", value: 0, unit: "°C")
    sendEvent(name: "heatingSetpoint", value: 0, unit: "°C")
       
    state.lastOperatingState = DEFAULT_OP_STATE
    state.lastFanMode = FAN_MODE.AUTO
    state.coolMasterTempUnit = DEFAULT_TEMP_UNIT
    state.userTempUnit = DEFAULT_TEMP_UNIT
    state.isConversionNeed = false
    state.limits = [:]
    /* Temperature limits */
    setDefaultLimits()
    state.isTimerStoped = true
    state.startTime = 0
    state.setpoint = 0
    state.simulIsRunning = false
}

def updated(){
	log.trace "CM: executing 'updated'"
}

private setDefaultLimits(){
	state.limits.coolingSetpointMax = COOLING_SETPOINT_MAX
	state.limits.coolingSetpointMin = COOLING_SETPOINT_MIN
	state.limits.heatingSetpointMax = HEATING_SETPOINT_MAX
	state.limits.heatingSetpointMin = HEATING_SETPOINT_MIN
	state.limits.autoSetpointMax = AUTO_SETPOINT_MAX 
	state.limits.autoSetpointMin = AUTO_SETPOINT_MIN
}

/* Update device after changing of general CoolRemote application settings */
def updateSettings(newSettings){
		 
   if( newSettings.userTempUnit && (state.userTempUnit != newSettings.userTempUnit ) )
   {	
		state.userTempUnit = newSettings.userTempUnit
		state.isConversionNeed = true
   		
		def temperature = getTemperature()
		def setpoint = getThermostatSetpoint()
		
        setTemperatureParams(temperature, setpoint)
  	}
}

private setTemperatureParams(Integer temperature,  Integer setpoint)
{	
	if( state.isConversionNeed ){
		switchTempLimits(state.userTempUnit)
        temperature = convertTemperature(temperature,state.userTempUnit)
        setpoint = convertTemperature(setpoint,state.userTempUnit)
	}
	setpoint = setpointToLimits(setpoint,mode)
	sendEvent(name: "temperature", value: temperature)
	sendEvent(name: "temperatureUnit", value:"°${state.userTempUnit}")
	setThermostatSetpoint(setpoint)
    log.debug "CM: setTemperatureParams NEW setpoint is ${setpoint} rt is ${temperature}"
}

/* Parse device response */
def parseData(data) {
    log.trace "CM: executing 'parseData'"
    
    def fanMode = data.fspeed.toLowerCase()
    def mode = data.mode.toLowerCase()
    def onoff = data.onoff.toLowerCase()
    def flr = data.flr
    def temperature = parseToRoundedInt(data.rt[0..-2])
    def setpoint = parseToRoundedInt(data.st[0..-2])
    
    state.coolMasterTempUnit = data.rt[-1..-1]
    state.userTempUnit = data.userTempUnit
    state.isConversionNeed = ( state.userTempUnit!=state.coolMasterTempUnit )

	setTemperatureParams(temperature, setpoint)

     if( onoff == "off" ){
    	mode = onoff
      	fanMode = onoff
    }
    else{
    	if( !(mode in OP_STATES) ){
        	mode = DEFAULT_OP_STATE
    	}
    	state.lastOperatingState = mode 
    	    
    	if( !( fanMode in FAN_MODES) ){
    		fanMode = DEFAULT_FAN_MODE
    	}
        state.lastFanMode = fanMode
    }
   
    setOperatingState(mode)
    setThermostatMode(mode)
    setThermostatFanMode(fanMode)
    
    sendEvent( name: "switch", value: onoff )
    if(flr!="OK")
    	flr="error code ${flr}"
    	
    sendEvent( name:"statusString", value:flr )
}

private sendCommand(cmd) {
    log.debug "CM: sent command ${cmd}"
    cmd.put("deviceId",device.deviceNetworkId)
    
    if(parent)
  		parent.sendCommandToCoolMaster(cmd)
}

private sendCommandWithTimeout(cmd, timeout){
    if(state.isTimerStoped){
    /* Start timer */
    	state.startTime = now()
        state.isTimerStoped = false
    }
    
    Long date = now()
    if(((date as Long) - (state.startTime as Long))>timeout){
    	sendCommand(cmd)
        state.isTimerStoped = true
	}
}

/******************************************************************
* Implemented capability commands
*******************************************************************/

/* capability Switch */
def on(){
    log.debug "CM ON: last op state: ${state.lastOperatingState}"
    
    setThermostatFanMode(state.lastFanMode)
    setMode(state.lastOperatingState,"on")
}

def off(){
    state.lastFanMode = getFanMode()
    state.lastOperatingState = getOperatingState()
    
    log.debug "CM OFF: last op state: ${state.lastOperatingState}"
    
    setThermostatFanMode("off")
    setMode("off",null)
    
}

/* capability Thermostat Mode  */
def cool(){
	setMode("cool",null)
}

def heat(){
	setMode("heat",null)
}

def auto(){
	setMode("auto",null)
}

def emergencyHeat(){
	log.debug "CM: emergencyHeat isn't emplemented yet"
	setMode("heat",null)
}

def supportedThermostatFanModes(){
	return ["on","auto"]
}

def fanAuto(){
	setThermostatFanMode("auto")
}

def fanOn(){
	setThermostatFanMode("on")
}

def getCoolingSetpoint(){
	return device.currentValue("coolingSetpoint")
}

def getHeatingSetpoint(){
	return device.currentValue("heatingSetpoint")
}

def setCoolingSetpoint(setpoint){
	log.debug "CM: setCoolingSetpoint ${setpoint}"
    dispatchCoolingHeating( "coolingSetpoint",setpoint )
}

def setHeatingSetpoint(setpoint){
	log.debug "CM: setHeatingSetpoint ${setpoint}"
    dispatchCoolingHeating( "heatingSetpoint",setpoint )
}

private dispatchCoolingHeating(mod, setpoint){
	sendEvent( name: mod, value: setpoint )
    def mode = (mod=="heatingSetpoint")? "heat":"cool"
    	
    if( getThermostatMode()==mode && setpoint!=0 )
		processSetpoint(setpoint)	
}

private setMode(mode,cmd){
	setOperatingState(mode)
    sendEvent(name: "thermostatMode", value: mode)
    def msg = (cmd)?[name:cmd]:[name:mode]
    sendCommand(msg)
}

/******************************************************************
* Tile actions
*******************************************************************/

private setpointUp(){
    def setpoint = getThermostatSetpoint()
    setpoint++
    processSetpoint(setpoint)
}

private setpointDown(){
	def setpoint = getThermostatSetpoint()
    setpoint--
    processSetpoint(setpoint)
}


private String cycleMode() {
    log.trace "CM: executing 'cycleMode'"
    String nextMode = "off"
    
    if( device.currentValue("switch")=="on" ){
    	nextMode = nextListElement(MODES, getThermostatMode())
    	setMode(nextMode,null)
		
        Integer setpoint 
		
        if(nextMode=="cool")
			setpoint = getCoolingSetpoint()
		else if(nextMode=="heat")
			setpoint = getHeatingSetpoint()
		
        if(setpoint==null)
			setpoint=getThermostatSetpoint()
		
        processSetpoint(setpoint)	
	}
        
    return nextMode
}

private String cycleFanMode() {
    def fanMode = getFanMode()
    String nextMode = "off"
    
    if( (device.currentValue("switch")=="on") && (fanMode != "off") ){
    	nextMode = nextListElement(FAN_MODES, fanMode)
    	setThermostatFanMode(nextMode)
        sendCommand(name:"fspeed",params:nextMode.take(1) )
    }
    return nextMode
}

def reset(){
	log.debug "CM: executing 'reset'"
    sendCommand(name:"ls")
}

/********************************************************************* 
* Setpoint functions
**********************************************************************/
private processSetpoint( Integer setpoint  )
{
    if(setpoint==0 || device.currentValue("switch")=="off")
    	return
        
    def tmpSP = setpoint
    def mode = getThermostatMode()    
    
    setpoint = setpointToLimits(setpoint,mode) 
    log.debug "processSetpoint ${tmpSP}->${setpoint} "
    setThermostatSetpoint(setpoint)
        
    /* Convert setpoint temperature back if it's necessary */
    if( state.isConversionNeed )
    	setpoint = convertTemperature(setpoint,state.coolMasterTempUnit)    
    
    sendCommandWithTimeout(name:"temp",params:"${setpoint}",CMD_TIMEOUT)
}

private Integer setpointToLimits(Integer setpoint, String mode){
    def range = ( state.limits.coolingSetpointMin..state.limits.coolingSetpointMax)
    
    switch( mode ){
    case MODE.HEAT:
    	range = (state.limits.heatingSetpointMin..state.limits.heatingSetpointMax)
    	break
    case MODE.AUTO:
    	range = (state.limits.autoSetpointMin..state.limits.autoSetpointMax)
    	break
    }
    return boundInt(setpoint,range)
}

/**
 * Ensure an integer value is within the provided range, or set it to either extent if it is outside the range.
 * @param Number value         The integer to evaluate
 * @param IntRange theRange     The range within which the value must fall
 * @return Integer
 */
private Integer boundInt(Number value, IntRange theRange) {
    value = Math.max(theRange.getFrom(), Math.min(theRange.getTo(), value))
    return value.toInteger()
}
   

private switchTempLimits(tempUnit){
    def flag = ( tempUnit == "C" )
	setDefaultLimits()
    def limits = state.limits
    if(!flag){
		state.limits = limits.collectEntries([:]){ key,value -> 
			[key,convertTemperature(value,tempUnit)]
		}
	}
    log.debug "CM: switchTempLimits ${state.limits}"
}

/********************************************************************* 
* Setters/Getters
**********************************************************************/

// Fan mode
private String getFanMode() {
    return device.currentValue("thermostatFanMode") ?: DEFAULT_FAN_MODE
}

def setThermostatFanMode(String fanMode) {
	
	if( !(fanMode in FAN_MODES) && fanMode != "on" && fanMode !="off" ){
		log.warn "Fan mode ${fanMode} is not supported!"
		return
	}
	
	if( fanMode == "on" ){
		fanMode = DEFAULT_FAN_MODE
		setMode("fan",null)
	}
			
	sendEvent(name: "thermostatFanMode", value: fanMode)
}

// Room Temperature
private Integer getTemperature(){
	def strVal = device.currentState("temperature")
    return strVal.getIntegerValue() 
}

private setTemperature( int temperature ){
	sendEvent(name:"temperature", value: temperature)
}

// Setpoint
private Integer getThermostatSetpoint(){
	def strVal = device.currentState("thermostatSetpoint")
    return strVal.getIntegerValue() 
     
}

private setThermostatSetpoint( Integer setpoint ){
log.debug "CM: setOperatingState ${setpoint}"
	sendEvent(name:"thermostatSetpoint", value: setpoint)
}

// Operating state
private String getOperatingState() {
    return device.currentValue("indoorOperatingState")
}

private setOperatingState(String operatingState) {
log.debug "CM: setOperatingState"
	sendEvent(name: "indoorOperatingState", value: operatingState)
}

// Thermostat mode
private String getThermostatMode() {
    return device.currentValue("thermostatMode") ?: DEFAULT_MODE
}

def setThermostatMode(String value) {
    log.trace "CM: Executing 'setThermostatMode' $value"
    if(value in MODES )
    	sendEvent(name: "thermostatMode", value: value)
    else if(value == "off"){
    	if( isSwitchedOn() )
    		off()
    }
    else
    	log.warn "CM: '$value' is not a supported mode. Please set one of ${MODES.join(', ')}"
}

private isSwitchedOn(){
	return (device.currentValue("switch")=="on")
}

// Temperature unit (C/F)
private String getTempUnit(){
	return device.currentValue("temperatureUnit")
}

private setTempUnit(String tempUnit){
	   sendEvent(name: "temperatureUnit", value:tempUnit)
}

/********************************************************************* 
* Misc
*********************************************************************/
private String nextListElement(List uniqueList, currentElt) {
    if (uniqueList != uniqueList.unique().asList()) {
        throw InvalidPararmeterException("CM: Each element of the List argument must be unique.")
    } else if (!(currentElt in uniqueList)) {
        throw InvalidParameterException("CM: currentElt '$currentElt' must be a member element in List uniqueList, but was not found.")
    }
    Integer listIdxMax = uniqueList.size() -1
    Integer currentEltIdx = uniqueList.indexOf(currentElt)
    Integer nextEltIdx = currentEltIdx < listIdxMax ? ++currentEltIdx : 0
    String nextElt = uniqueList[nextEltIdx] as String
    return nextElt
}

Integer convertTemperature(Integer temperature, String tempUnit){
	int temp
    
	if( tempUnit=="C" ){
    	temp = ((( temperature - 32 ) * 5 ) / 9)
    }
    else{
        temp = ((( 9 * temperature ) / 5 ) + 32)
    }
    return Math.round(temp).toInteger()
}


private int parseToRoundedInt(String strTmp){
	double tmp = Double.parseDouble(strTmp) 
    return (int)Math.round(tmp)
}

def poll(){
log.debug "CM: polling..."
}



