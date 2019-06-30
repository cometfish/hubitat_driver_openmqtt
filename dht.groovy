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

import static hubitat.helper.InterfaceUtils.alphaV1mqttConnect
import static hubitat.helper.InterfaceUtils.alphaV1mqttDisconnect
import static hubitat.helper.InterfaceUtils.alphaV1mqttSubscribe
import static hubitat.helper.InterfaceUtils.alphaV1mqttUnsubscribe
import static hubitat.helper.InterfaceUtils.alphaV1parseMqttMessage
import static hubitat.helper.InterfaceUtils.alphaV1mqttPublish

def installed() {
    log.warn "installed..."
}

// Parse incoming device messages to generate events
def parse(String description) {
    mqtt = alphaV1parseMqttMessage(description)
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
    alphaV1mqttDisconnect(device)
}

def uninstalled() {
    if (logEnable) log.info "disconnecting from mqtt"
    alphaV1mqttDisconnect(device)
}

def initialize() {
    try {
        //open connection
        alphaV1mqttConnect(device, "tcp://" + settings.mqttBroker, settings.mqttClientID, null, null)
        //give it a chance to start
        pauseExecution(1000)
        log.info "connection established"
        alphaV1mqttSubscribe(device, settings.mqttTopic)
    } catch(e) {
        log.error "initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    log.error "mqttStatus- error: ${status}"
    //try to reconnect
    initialize()
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