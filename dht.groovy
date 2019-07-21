metadata {
    definition (name: "Temperature and Humidity sensor (OpenMQTT - DHT)", namespace: "community", author: "cometfish") {
        capability "Initialize"
		capability "RelativeHumidityMeasurement"
		capability "TemperatureMeasurement"
		
		attribute "humidity", "number"
		attribute "temperature", "number"

		command "disconnect" 
		
		//thermostat stuff to get gh to read it
		capability "Thermostat"
		attribute "coolingSetpoint", "number"
		attribute "heatingSetpoint", "number"
		attribute "schedule", "json_object"
		attribute "supportedThermostatFanModes", "enum", ["on", "circulate", "auto"]
		attribute "supportedThermostatModes", "enum", ["auto", "off", "heat", "emergency heat", "cool"]
		attribute "thermostatFanMode", "enum", ["on", "circulate", "auto"]
		attribute "thermostatMode", "enum", ["auto", "off", "heat", "emergency heat", "cool"]
		attribute "thermostatOperatingState", "enum", ["heating", "pending cool", "pending heat", "vent economizer", "idle", "cooling", "fan only"]
		attribute "thermostatSetpoint", "number"
		command "auto"
		command "cool"
		command "emergencyHeat"
		command "fanAuto"
		command "fanCirculate"
		command "fanOn"
		command "heat"
		command "off"
		command "setThermoStuff"
    }
}
preferences {
    section("URIs") {
        input "mqttBroker", "string", title: "MQTT Broker Address", required: true
		input "mqttTopic", "string", title: "MQTT Topic", required: true
        input "mqttClientID", "string", title: "MQTT Client ID", required: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() {
    log.warn "installed..."
}

// Parse incoming device messages to generate events
def parse(String description) {
    mqtt = interfaces.mqtt.parseMessage(description)
	json = new groovy.json.JsonSlurper().parseText(mqtt.payload)
	sendEvent(name: "humidity", value: json.hum, isStateChange: true)
	sendEvent(name: "temperature", value: json.temp, isStateChange: true)
}

def updated() {
    log.info "updated..."
    initialize()
}
def disconnect() {
	if (logEnable) log.info "disconnecting from mqtt"
    interfaces.mqtt.disconnect()
    state.connected = false
}

def uninstalled() {
    disconnect()
}

def initialize() {
    try {
        //open connection
        def mqttInt = interfaces.mqtt
        mqttInt.connect("tcp://" + settings.mqttBroker, settings.mqttClientID, null, null)
        //give it a chance to start
        pauseExecution(1000)
        if (logEnable) log.info "connection established"
        mqttInt.subscribe(settings.mqttTopic)
    } catch(e) {
        log.error "initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    if (logEnable) log.debug "mqttStatus: ${status}"
    switch (status) {
        case "Status: Connection succeeded":
            state.connected = true
            break
        case "disconnected":
            //note: this is NOT called when we deliberately disconnect, only on unexpected disconnect
            state.connected = false
            //try to reconnect after a small wait (so the broker being down doesn't send us into an endless loop of trying to reconnect and lock up the hub)
            runIn(5, initialize)
            break
        default:
            log.info status
            break
    }
}

//empty funcs for thermostat capability 
def setThermoStuff() {
	sendEvent(name: "supportedThermostatFanModes", value: ["auto"], isStateChange: true)
	sendEvent(name: "supportedThermostatModes", value: ["off", "auto", "heat"] , isStateChange: true)
	sendEvent(name: "thermostatMode", value: "off", isStateChange: true)
	sendEvent(name: "thermostatFanMode", value: "auto", isStateChange: true)
	sendEvent(name: "thermostatOperatingState", value: "idle", isStateChange: true)
	sendEvent(name: "thermostatSetpoint", value: 18, isStateChange: true)
	sendEvent(name: "heatingSetpoint", value: 18, isStateChange: true)
	sendEvent(name: "coolingSetpoint", value: 40, isStateChange: true)
	updateDataValue("lastRunningMode", "heat")
	
} 
def auto() {}
def cool() {}
def heat() {}
def off() {}
def fanCirculate() {}
def fanOn() {}
def fanAuto() {}
def emergencyHeat() {}
def setCoolingSetpoint(temperature) {}
def setHeatingSetpoint(temperature) {}
def setSchedule(schedule) {} 
def setThermostatFanMode(fanmode) {}
def setThermostatMode(thermostatmode) {} 