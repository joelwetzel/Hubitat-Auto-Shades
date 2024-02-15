/**
 *  Auto Shades Instance v1.1
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
	page(name: "mainPage", title: "Preferences", install: true, uninstall: true) {
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

    use (groovy.time.TimeCategory) {
        // Make it 24 hours ago so that Autoshades can start responding right away.
        state.lastManualClose = formatDate(dateNow()-24.hours)

        if (!state.lastAutoShade) {
            state.lastAutoShade = formatDate(dateNow()-24.hours)
        }
    }
}

def dateNow() {
    return new Date()
}

def formatDate(date) {
    return date.format("yyyy-MM-dd'T'HH:mm:ssZ")
}

def windowShadeHandler(e) {
    log "autoShades:windowShadeHandler(${e.value})"

    use (groovy.time.TimeCategory) {
        def dn = dateNow()
        def sinceLastAutoShade = dn - toDateTime(state.lastAutoShade)
        def secondsSinceLastAutoShade = sinceLastAutoShade.seconds + sinceLastAutoShade.minutes*60 + sinceLastAutoShade.hours*60*60 + sinceLastAutoShade.days*60*60*24

        if (secondsSinceLastAutoShade > 6) {
            // Don't update lastManualClose if we are just getting the event from an auto-close/open.
            state.lastManualClose = formatDate(dateNow())
        }
    }
}


def illuminanceHandler(e) {
    log "autoShades:illuminanceHandler(${e.value})"

    def illuminanceMeasurement = (e.value instanceof String) ? Double.parseDouble(e.value) : e.value
    def currentShadeValue = wrappedShade.currentValue("windowShade")

    def disableAutoShades = false

    def minutesSinceLastManualClose = 0
    def minutesSinceLastAutoShade = 0

    use (groovy.time.TimeCategory) {
        def dn = dateNow()
        def sinceLastManualClose = dn - toDateTime(state.lastManualClose)
        def sinceLastAutoShade = dn - toDateTime(state.lastAutoShade)

        minutesSinceLastManualClose = sinceLastManualClose.minutes + sinceLastManualClose.hours*60 + sinceLastManualClose.days*60*24
        minutesSinceLastAutoShade = sinceLastAutoShade.minutes + sinceLastAutoShade.hours*60 + sinceLastAutoShade.days*60*24

        if (minutesSinceLastManualClose <= delayAfterManualClose) {
            // Manual open/close disables auto-close for a default of a half hour.
            log "Skipping AutoShade because minutesSinceLastManualClose <= delayAfterManualClose (${minutesSinceLastManualClose} <= ${delayAfterManualClose})"
            disableAutoShades = true
        }
        if (currentShadeValue == "closed" && minutesSinceLastAutoShade <= delayAfterAutoClose) {
            // Delay (default 6 minutes) after auto-close, before auto-opening.
            log "Skipping AutoShade because currentShadeValue == close && minutesSinceLastAutoShade <= delayAfterAutoClose (${minutesSinceLastAutoShade} <= ${delayAfterAutoClose})"
            disableAutoShades = true
        }
        if (currentShadeValue == "open" && minutesSinceLastAutoShade <= delayAfterAutoOpen) {
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
            state.lastAutoShade = formatDate(dateNow())
            wrappedShade.close()
        }
        else if (illuminanceMeasurement <= illuminanceThreshold && currentShadeValue != "open") {
            log "Auto-opening..."
            state.lastAutoShade = formatDate(dateNow())
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
