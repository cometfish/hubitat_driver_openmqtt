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

def installed() {
    log.warn "installed..."
	sendEvent(name: "smoke", value: "clear", isStateChange: true)
}

def parse(String description) {
	if (device.currentValue("smoke") =="detected") {
		//we don't care, we're already alarming
		return
	} 
	
    mqtt = interfaces.mqtt.parseMessage(description)
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