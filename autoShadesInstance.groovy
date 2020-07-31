/**
 *  Auto Shades Instance v1.02
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.*
import groovy.time.*

definition(
	parent: "joelwetzel:Auto Shades",
    name: "Auto Shades Instance",
    namespace: "joelwetzel",
    author: "Joel Wetzel",
    description: "Child app that is instantiated by the Auto Shades app.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")


preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true) {
		section(getFormat("title", "Auto Shades Instance")) {
			input (
	            name:				"wrappedShade",
	            type:				"capability.windowShade",
	            title:				"Window Shade",
	            description:		"Select the shade or blind to automate.",
	            multiple:			false,
	            required:			true
            )
            input (
	            name:				"lightSensor",
	            type:				"capability.illuminanceMeasurement",
	            title:				"Light Sensor",
	            description:		"Select the light sensor that will determine whether to open/close the shade.",
	            multiple:			false,
	            required:			true
            )
			input (
                name:				"illuminanceThreshold",
	            type:				"number",
	            title:				"Define the illuminance threshold for opening/closing.  Auto-close if brighter than.",
	            defaultValue:		400,
	            required:			true
            )
            input (
                name:				"delayAfterManualClose",
	            type:				"number",
	            title:				"After manually adjusting the blinds, wait this many minutes before allowing any auto opens/closes.  (So that your manual adjustments will 'stick'.)",
	            defaultValue:		30,
	            required:			true
            )
            input (
                name:				"delayAfterAutoClose",
	            type:				"number",
	            title:				"After auto-closing the blinds, wait this many minutes before allowing an auto-open.  (This is to prevent excess activity.)",
	            defaultValue:		6,
	            required:			true
            )
            input (
                name:				"delayAfterAutoOpen",
	            type:				"number",
	            title:				"After auto-opening the blinds, wait this many minutes before allowing an auto-close.  (This is to prevent excess activity.)",
	            defaultValue:		3,
	            required:			true
            )


		}
        section() {
            input (
				type:               "bool",
				name:               "enableDebugLogging",
				title:              "Enable Debug Logging?",
				required:           true,
				defaultValue:       true
			)
        }
	}
}


def installed() {
	log.info "Installed with settings: ${settings}"

	initialize()
}


def uninstalled() {
}


def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}


def initialize() {
    log.info "initialize()"
    
	subscribe(wrappedShade, "windowShade", windowShadeHandler)
    
    subscribe(lightSensor, "illuminance", illuminanceHandler)

	// Generate a label for this child app
	app.updateLabel("Automated ${wrappedShade.displayName}")
    
    if (!state.lastAutoClose) {
        state.lastAutoClose = new Date()
    }
    
    if (!state.lastManualClose) {
        state.lastManualClose = new Date()
    }
}


def windowShadeHandler(e) {
    log "autoShades:windowShadeHandler(${e.value})"
    
    use (groovy.time.TimeCategory) {
        def secondsSinceLastAutoClose = (new Date() - toDateTime(state.lastAutoClose)).seconds
        
        //log "secondsSinceLastAutoClose: ${secondsSinceLastAutoClose}"
        
        if (secondsSinceLastAutoClose > 6) {
            // Don't update lastManualClose if we are just getting the event from an auto-close.
            log "Detected a manual event."
            state.lastManualClose = new Date()
        }
    }
}


def illuminanceHandler(e) {
    log "autoShades:illuminanceHandler(${e.value})"
    
    def illuminanceMeasurement = Double.parseDouble(e.value)
    def currentShadeValue = wrappedShade.currentWindowShade
    
    log "illuminanceMeasurement: ${illuminanceMeasurement}"
    log "currentShadeValue: ${currentShadeValue}"
    
    def disableAutoClose = false
    
    use (groovy.time.TimeCategory) {
        def minutesSinceLastManualClose = (new Date() - toDateTime(state.lastManualClose)).minutes + (new Date() - toDateTime(state.lastManualClose)).hours*60 + (new Date() - toDateTime(state.lastManualClose)).days*60*24
        def minutesSinceLastAutoClose = (new Date() - toDateTime(state.lastAutoClose)).minutes

        log "minutesSinceLastManualClose: ${minutesSinceLastManualClose}"
        log "minutesSinceLastAutoClose: ${minutesSinceLastAutoClose}"
        
        if (minutesSinceLastManualClose <= delayAfterManualClose) {
            // Manual open/close disables auto-close for a default of a half hour.
            disableAutoClose = true
        }
        
        if (currentShadeValue == "closed" && minutesSinceLastAutoClose <= delayAfterAutoClose) {
            // Delay (default 6 minutes) after auto-close, before auto-opening.
            disableAutoClose = true
        }
        
        if (currentShadeValue == "open" && minutesSinceLastAutoClose <= delayAfterAutoOpen) {
            // Delay (default 3 minutes) after auto-open, before auto-closing.
            disableAutoClose = true
        }
    }
    
    log "disableAutoClose: ${disableAutoClose}"
    
    if (!disableAutoClose) {
        if (illuminanceMeasurement > illuminanceThreshold && currentShadeValue != "closed") {
            log "auto-closing..."
            state.lastAutoClose = new Date()
            wrappedShade.close()
        }
        else if (illuminanceMeasurement <= illuminanceThreshold && currentShadeValue != "open") {
            log "auto-opening..."
            state.lastAutoClose = new Date()
            wrappedShade.open()
        }
    }
}


def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


def log(msg) {
	if (enableDebugLogging) {
		log.debug(msg)	
	}
}





