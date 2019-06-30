metadata {
    definition (name: "Generic contact sensor (OpenMQTT - 433mhz)", namespace: "community", author: "cometfish") {
        capability "Initialize"
		capability "ContactSensor"
		
		attribute "contact", "enum", ["open", "closed"]

		command "disconnect"
    }
}

preferences {
    section("URIs") {
        input "mqttBroker", "string", title: "MQTT Broker Address", required: true
		input "mqttTopic", "string", title: "MQTT Topic", required: true
        input "mqttClientID", "string", title: "MQTT Client ID", required: true
        input "triggerValue", "number", title: "Contact Open Trigger Value", required: true
        input "triggerProtocol", "number", title: "Contact Open Trigger Protocol", required: true
        input "triggerLength", "number", title: "Contact Open Trigger Length", required: true
        input "closeAfterSeconds", "number", title: "Close contact after seconds", required: true
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
}

def parse(String description) {
    if (logEnable) log.debug description
	if (device.currentValue("contact") =="open") {
		//we don't care, we're already open
		return
	} 
	
    mqtt = alphaV1parseMqttMessage(description)
	if (logEnable) log.debug mqtt
	if (logEnable) log.debug mqtt.topic
	json = new groovy.json.JsonSlurper().parseText(mqtt.payload)
	log.debug json.value
	if (json.value == settings.triggerValue && json.protocol == settings.triggerProtocol && json.length == settings.triggerLength) {
		if (logEnable) log.debug "open" 
		
		sendEvent(name: "contact", value: "open", isStateChange: true)
		runIn(settings.closeAfterSeconds, closeContact)
		
	} 
}

def closeContact() {
	sendEvent(name: "contact", value: "closed", isStateChange: true)
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
        if (logEnable) log.info "connection established"
        alphaV1mqttSubscribe(device, settings.mqttTopic)
    } catch(e) {
        log.error "initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    log.error "mqttStatus- error: ${status}"
    //try and reconnect
    initialize()
}