metadata {
    definition (name: "OpenMQTT Gateway", namespace: "community", author: "cometfish") {
        capability "Sensor"
        capability "PresenceSensor"
        
        command "disconnect"
		
		attribute "presence", "enum", ["present", "not present"]
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

def parse(String description) {
    if (logEnable) log.debug description
	
    mqtt = interfaces.mqtt.parseMessage(description)
	if (logEnable) log.debug mqtt
	if (logEnable) log.debug mqtt.topic
	
	if (mqtt.payload == 'online') 
	    sendEvent(name: "presence", value: "present", isStateChange: true)
	else if (mqtt.payload == 'offline')
		sendEvent(name: "presence", value: "not present", isStateChange: true)
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
        log.debug "initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    if (logEnable) log.debug "mqttStatus- error: ${status}"
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