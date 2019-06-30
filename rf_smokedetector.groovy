metadata {
    definition (name: "Digoo Smoke Alarm (OpenMQTT - 433mhz)", namespace: "community", author: "cometfish") {
        capability "Initialize"
		capability "Sensor"
		capability "SmokeDetector"
		
		attribute "smoke", "enum", ["clear", "tested", "detected"]

		command "disconnect"
    }
}

preferences {
    section("URIs") {
        input "mqttBroker", "string", title: "MQTT Broker Address", required: true
		input "mqttTopic", "string", title: "MQTT Topic", required: true
        input "mqttClientID", "string", title: "MQTT Client ID", required: true
        input "triggerValue", "number", title: "Smoke Detected Trigger Value", required: true
        input "triggerProtocol", "number", title: "Smoke Detected Trigger Protocol", required: true
        input "triggerLength", "number", title: "Smoke Detected Trigger Length", required: true
        input "closeAfterSeconds", "number", title: "Smoke cleared after seconds", required: true
        input "logEnable", "bool", title: "Enable debug logging", defaultValue: true
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
	sendEvent(name: "smoke", value: "clear", isStateChange: true)
}

def parse(String description) {
	if (device.currentValue("smoke") =="detected") {
		//we don't care, we're already alarming
		return
	} 
	
    mqtt = alphaV1parseMqttMessage(description)
	json = new groovy.json.JsonSlurper().parseText(mqtt.payload)
	if (logEnable) log.debug json.value
	if (json.value == settings.triggerValue && json.protocol == settings.triggerProtocol && json.length == settings.triggerLength) {
		if (logEnable) log.debug "open" 
		
		sendEvent(name: "smoke", value: "detected", isStateChange: true)
		runIn(settings.closeAfterSeconds, smokeCleared)
		
	} 
}

def smokeCleared() {
	sendEvent(name: "smoke", value: "clear", isStateChange: true)
} 

def updated() {
    log.info "updated..."
    initialize()
}
def disconnect() {
	log.info "disconnecting from mqtt"
    alphaV1mqttDisconnect(device)
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