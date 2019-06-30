metadata {
    definition (name: "Button (OpenMQTT - 433mhz)", namespace: "community", author: "cometfish") {
        capability "Initialize"
		capability "PushableButton"
		
		attribute "numberOfButtons", "number"
		attribute "pushed", "number"
	
		command "disconnect"
    }
}

preferences {
    section("URIs") {
        input "mqttBroker", "string", title: "MQTT Broker Address", required: true
		input "mqttTopic", "string", title: "MQTT Topic", required: true
        input "mqttClientID", "string", title: "MQTT Client ID", required: true
        input "triggerValue", "number", title: "Button Trigger Value", required: true
        input "triggerProtocol", "number", title: "Button Trigger Protocol", required: true
        input "triggerLength", "number", title: "Button Trigger Length", required: true
        input "closeAfterSeconds", "number", title: "Release after seconds", required: true
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
    mqtt = alphaV1parseMqttMessage(description)
	if (logEnable) log.debug mqtt
	if (logEnable) log.debug mqtt.topic
	json = new groovy.json.JsonSlurper().parseText(mqtt.payload)
	if (logEnable) log.debug json.modules
	if (json.value==settings.triggerValue && json.protocol==settings.triggerProtocol && json.length==settings.triggerLength) {
		//doorbell rang! 
		sendEvent(name: "pushed", value: 1, isStateChange: true)
		pauseExecution(closeAfterSeconds*1000)
		sendEvent(name: "pushed", value: 0, isStateChange: true)
	}
}
def clearPush() {
	sendEvent(name: "pushed", value: 0, isStateChange: true)
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