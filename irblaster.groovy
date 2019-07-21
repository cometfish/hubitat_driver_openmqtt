/*
 * IR Send and Receive via an OpenMQTT hub
 *
 * IR blaster with learning ability. Make sure to set your default protocol name (a protocol name is required by OpenMQTT for sending, but it does not currently provide it when receiving). Protocol name can be found here: https://docs.google.com/spreadsheets/d/1_5fQjAixzRtepkykmL-3uN3G5bLfQ0zMajM9OBZ1bx0/edit#gid=1910001295
 * 
*/
metadata {
    definition(name: "IR Blaster (OpenMQTT)", namespace: "community", author: "cometfish") {
        capability "Initialize"
        capability "Actuator"
        capability "Switch"
        capability "TV"
        
        attribute "switch", "enum", ["off", "on"] 
        attribute "channel", "number"
        attribute "movieMode", "string"
        attribute "picture", "string"
        attribute "power", "string"
        attribute "sound", "string"
        attribute "volume", "number"
        
        command "on"
        command "off"
        command "channelUp"
        command "channelDown"
        command "volumeUp"
        command "volumeDown"
        
        command "disconnect"
        command "transmit", ["String"]
        command "transmitLearnt", ["String"] 
        command "transmit", ["number", "number", "number", "string"]
        command "learn", ["string", "string"] 
        command "learnNext", ["string"] 
    }
}

preferences {
    section("URIs") {
        input name: "mqttBroker", type: "text", title: "MQTT broker IP:Port", required: true
        input name: "mqttClientID", type: "text", title: "MQTT Client ID", required: true, defaultValue: "hubitat_ir" 
        input name: "mqttReceivePath", type: "text", title: "MQTT Receive Path", required: true, defaultValue: "home/OpenMQTTGateway/IRtoMQTT" 
        input name: "mqttTransmitPath", type: "text", title: "MQTT Transmit Path", required: true, defaultValue: "home/OpenMQTTGateway/commands/MQTTtoIR"
        input name: "defaultProtocolName", type: "text", title: "Default Protocol Name", defaultValue: "IR_Raw"
        input name: "defaultRepeat", type: "number", title: "Default Repeat", defaultValue: 20
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
   } 
}

def installed() {
    log.warn "installed"
}

def parse(String description) {
    mqtt = interfaces.mqtt.parseMessage(description)
    if (logEnable)
        log.debug mqtt
	
    if (state.learningNext != "") {
        learn(state.learningNext, mqtt.payload)
        state.learningNext = ""
    } 
}

def transmit(String s) {
    if (logEnable)
       log.info s
    //set default fields if missing
    if (s.indexOf("protocol_name")<0)
        s = s.replace("}", ",\"protocol_name\":\"${settings.defaultProtocolName}\"}")
    if (s.indexOf("repeat")<0)
        s = s.replace("}", ",\"repeat\":${settings.defaultRepeat}}")
    if (logEnable)
        log.info s
    interfaces.mqtt.publish(settings.mqttTransmitPath, s)
}
def transmit(BigDecimal value, BigDecimal protocol, BigDecimal bits, String raw) {
    msg = "{\"value\":${value},\"protocol\":${protocol},\"bits\":${bits},\"raw\":\"${raw}\",\"protocol_name\":\"${settings.defaultProtocolName}\",\"repeat\": ${settings.defaultRepeat}}"
    if (logEnable)
       log.info msg
    interfaces.mqtt.publish(settings.mqttTransmitPath, msg)
}
def learn(button, msg) {
    if (button==null || button=="") {
        log.error "Missing button name"
        return
    } 
    
    state."key_${button}" = msg
}
def learnNext(String button) {
    if (button==null || button=="") {
        log.error "Missing button name"
        return
    } 
    
    state.learningNext = button
}
def connect() {
    try {
        //open connection
        def mqttInt = interfaces.mqtt
        mqttInt.connect("tcp://" + settings.mqttBroker, settings.mqttClientID, null, null)
        //give it a chance to start
        pauseExecution(1000)
        if (logEnable)
            log.info "connection established"
        mqttInt.subscribe(settings.mqttReceivePath)
    } catch(e) {
        log.error "connect error: ${e.message}"
        state.learningNext = ""
    }
}

def transmitLearnt(String button) {
    if (button==null || button=="") {
        log.error "Missing button name"
        return
    } 

    transmit(state."key_${button}")
} 

def updated() {
    log.info "updated"
    connect()
}

def disconnect() {
	if (logEnable)
       log.info "disconnecting from mqtt"
    interfaces.mqtt.disconnect()
    state.connected = false
}

def uninstalled() {
    disconnect() 
}

def initialize() {
    connect()
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
            runIn(5, connect)
            break
        default:
            log.info status
            break
    }
} 

def on() {
    transmitLearnt("on")
    sendEvent(name: "switch", value: "on", isStateChange: true) 
}
def off() {
    transmitLearnt("off")
    sendEvent(name: "switch", value: "off", isStateChange: true)
}
def channelUp() {
    transmitLearnt("channelUp")
}
def channelDown() {
    transmitLearnt("channelDown")
}
def volumeUp() {
    transmitLearnt("volumeUp")
}
def volumeDown() {
    transmitLearnt("volumeDown")
}