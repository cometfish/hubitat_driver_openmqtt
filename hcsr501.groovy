metadata {
    definition (name: "Motion sensor (OpenMQTT - HCSR501)", namespace: "community", author: "cometfish") {
        capability "Sensor"
        capability "MotionSensor"
		
		attribute "motion", "enum", ["active", "inactive"]
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
import static hubitat.helper.InterfaceUtils.alphaV1parseMqttMessage

def installed() {
    log.warn "installed..."
}

def parse(String description) {
    if (logEnable) log.debug description
	
    mqtt = alphaV1parseMqttMessage(description)
	if (logEnable) log.debug mqtt
	if (logEnable) log.debug mqtt.topic
    
    json = new groovy.json.JsonSlurper().parseText(mqtt.payload)
	if (json.hcsr501 == 'true')
		sendEvent(name: "motion", value: "active", isStateChange: true)
	else if (json.hcsr501 == 'false')
		sendEvent(name: "motion", value: "inactive", isStateChange: true)
}

def updated() {
    log.info "updated..."
    initialize()
}

def uninstalled() {
    log.info "disconnecting from mqtt"
    alphaV1mqttDisconnect(device)
}

def initialize() {
    try {
        //open connection
        alphaV1mqttConnect(device, "tcp://" + settings.mqttBroker, settings.mqttClientID, null, null)
        //give it a chance to start
        pauseExecution(1000)
        if (logEnable) log.info "connection established"
        alphaV1mqttSubscribe(device, settings.mqttTopic)
    } catch(e) {
        log.debug "initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    log.debug "mqttStatus- error: ${status}"
    //try to reconnect
    initialize()
}