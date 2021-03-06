/**
 *  Auto Shades Instance v1.03
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
	            defaultValue:		120,
	            required:			true
            )
            input (
                name:				"delayAfterAutoClose",
	            type:				"number",
	            title:				"After auto-closing the blinds, wait this many minutes before allowing an auto-open.  (This is to prevent excess activity.)",
	            defaultValue:		15,
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
	app.updateLabel("Auto Shades - ${wrappedShade.displayName}")
    
    if (!state.lastAutoShade) {
        state.lastAutoShade = new Date()
    }
    
    use (groovy.time.TimeCategory) {
        // Make it 24 hours ago so that Autoshades can start responding right away.
        state.lastManualClose = new Date()-24.hours
    }
}


def windowShadeHandler(e) {
    log "autoShades:windowShadeHandler(${e.value})"
    
    use (groovy.time.TimeCategory) {
        def secondsSinceLastAutoShade = (new Date() - toDateTime(state.lastAutoShade)).seconds
        
        if (secondsSinceLastAutoShade > 6) {
            // Don't update lastManualClose if we are just getting the event from an auto-close/open.
            log "Detected a manual event."
            state.lastManualClose = new Date()
        }
    }
}


def illuminanceHandler(e) {
    log "autoShades:illuminanceHandler(${e.value})"
    
    def illuminanceMeasurement = Double.parseDouble(e.value)
    def currentShadeValue = wrappedShade.currentWindowShade
    
    def disableAutoShades = false
    
    def minutesSinceLastManualClose = 0
    def minutesSinceLastAutoShade = 0
    
    use (groovy.time.TimeCategory) {
        minutesSinceLastManualClose = (new Date() - toDateTime(state.lastManualClose)).minutes + (new Date() - toDateTime(state.lastManualClose)).hours*60 + (new Date() - toDateTime(state.lastManualClose)).days*60*24
        minutesSinceLastAutoShade = (new Date() - toDateTime(state.lastAutoShade)).minutes

        if (minutesSinceLastManualClose <= delayAfterManualClose) {
            // Manual open/close disables auto-close for a default of a half hour.
            log "Skipping AutoShade because minutesSinceLastManualClose <= delayAfterManualClose (${minutesSinceLastManualClose} <= ${delayAfterManualClose})"
            disableAutoShades = true
        }
        else if (currentShadeValue == "closed" && minutesSinceLastAutoShade <= delayAfterAutoClose) {
            // Delay (default 6 minutes) after auto-close, before auto-opening.
            log "Skipping AutoShade because currentShadeValue == close && minutesSinceLastAutoShade <= delayAfterAutoClose (${minutesSinceLastAutoShade} <= ${delayAfterAutoClose})"
            disableAutoShades = true
        }
        else if (currentShadeValue == "open" && minutesSinceLastAutoShade <= delayAfterAutoOpen) {
            // Delay (default 3 minutes) after auto-open, before auto-closing.
            log "Skipping Autoshade because currentShadeValue == open && minutesSinceLastAutoShade <= delayAfterAutoOpen (${minutesSinceLastAutoShade} <= ${delayAfterAutoOpen})"
            disableAutoShades = true
        }
    }
    
    //log "disableAutoShades: ${disableAutoShades}"
    
    if (!disableAutoShades) {
        log "currentShadeValue: ${currentShadeValue}"
        log "minutesSinceLastManualClose: ${minutesSinceLastManualClose}"
        log "minutesSinceLastAutoShade: ${minutesSinceLastAutoShade}"

        if (illuminanceMeasurement > illuminanceThreshold && currentShadeValue != "closed") {
            log "Auto-closing..."
            state.lastAutoShade = new Date()
            wrappedShade.close()
        }
        else if (illuminanceMeasurement <= illuminanceThreshold && currentShadeValue != "open") {
            log "Auto-opening..."
            state.lastAutoShade = new Date()
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






