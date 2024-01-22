package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.WindowShadeFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LightSensorFixtureFactory
import me.biocomp.hubitat_ci.util.AppExecutorWithEventForwarding

import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.Log
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import me.biocomp.hubitat_ci.api.common_api.DeviceWrapper
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.capabilities.GeneratedCapability
import me.biocomp.hubitat_ci.util.NullableOptional
import me.biocomp.hubitat_ci.validation.Flags

import groovy.time.*

import spock.lang.Specification

/**
* Behavior tests for autoShadesInstance.groovy
*/
class BehaviorTests extends Specification {
    private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('autoShadesInstance.groovy'))

    def log = Mock(Log)

    def installedApp = Mock(InstalledAppWrapper)

    def appState = [lastAutoShade: null, lastManualClose: null]

    def appExecutor = Spy(AppExecutorWithEventForwarding) {
        _*getLog() >> log
        _*getApp() >> installedApp
        _*getState() >> appState
    }

    def shadeFixture = WindowShadeFixtureFactory.create('ws')
    def lightSensorFixture = LightSensorFixtureFactory.create('ls')

    def appScript = sandbox.run(api: appExecutor,
        validationFlags: [Flags.AllowReadingNonInputSettings, Flags.AllowWritingToSettings],      // We have to set these flags so that we can write to the dateValue variable
        customizeScriptBeforeRun: { script ->
            script.getMetaClass().dateNow = { -> return dateValue ?: new Date() }       // Return current time if the tests don't set a date
            script.getMetaClass().addMinutes = { minutes -> dateValue = groovy.time.TimeCategory.plus(dateNow(), new groovy.time.TimeDuration(0, 0, minutes, 0, 0)) }     // Allow these tests to add minutes to the date
        },
        userSettingValues: [wrappedShade: shadeFixture, lightSensor: lightSensorFixture, illuminanceThreshold: 400, delayAfterManualClose: 120, delayAfterAutoClose: 15, delayAfterAutoOpen: 3, enableDebugLogging: true])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
        appScript.installed()
    }

    void "Test of my customizeScriptBeforeRun.  Can I push date forward on the script, to simulate time passing?"() {
        given:
        def currentDate = appScript.dateNow()

        when:
        appScript.addMinutes(2)

        then:
        groovy.time.TimeCategory.minus(appScript.dateNow(), currentDate).minutes == 2
    }

    void "Shade should auto-open when it gets dark"() {
        given: "It's bright outside"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        when: "A cloud blocks the sun"
        lightSensorFixture.setIlluminance(200)

        then: "The shade should auto-open"
        shadeFixture.state.windowShade == "open"
        shadeFixture.state.position == 100
    }

    void "Shade should not auto-close too soon after auto-opening"() {
        given: "It's bright outside"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        and: "A cloud blocks the sun and the shades open"
        lightSensorFixture.setIlluminance(200)

        when: "Two minutes pass"
        appScript.addMinutes(2)

        and: "The cloud passes and the illuminance rises above the threshold"
        lightSensorFixture.setIlluminance(600)

        then: "The shade should not auto-close yet"
        shadeFixture.state.windowShade == "open"
        shadeFixture.state.position == 100
    }

    void "Shade can auto-close after an auto-open, if enough time has passed"() {
        given: "It's bright outside"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        and: "A cloud blocks the sun and the shades open"
        lightSensorFixture.setIlluminance(200)

        when: "Twenty minutes pass"
        appScript.addMinutes(20)

        and: "The cloud passes and the illuminance rises above the threshold"
        lightSensorFixture.setIlluminance(600)

        then: "The shade should auto-close"
        shadeFixture.state.windowShade == "closed"
        shadeFixture.state.position == 0
    }

    void "Shade should auto-close when it gets bright"() {
        given: "It's dark outside"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        when: "The sun comes out"
        lightSensorFixture.setIlluminance(600)

        then: "The shade should auto-close"
        shadeFixture.state.windowShade == "closed"
        shadeFixture.state.position == 0
    }

    void "Shade should not auto-open too soon after auto-closing"() {
        given: "It's dark outside"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        and: "The sun comes out and the shades close"
        lightSensorFixture.setIlluminance(600)

        when: "Two minutes pass"
        appScript.addMinutes(2)

        and: "The sun goes behind a cloud and the illuminance drops below the threshold"
        lightSensorFixture.setIlluminance(200)

        then: "The shade should not auto-open yet"
        shadeFixture.state.windowShade == "closed"
        shadeFixture.state.position == 0
    }

    void "Shade can auto-open after an auto-close, if enough time has passed"() {
        given: "It's dark outside"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        and: "The sun comes out and the shades close"
        lightSensorFixture.setIlluminance(600)

        when: "Twenty minutes pass"
        appScript.addMinutes(20)

        and: "The sun goes behind a cloud and the illuminance drops below the threshold"
        lightSensorFixture.setIlluminance(200)

        then: "The shade should auto-open"
        shadeFixture.state.windowShade == "open"
        shadeFixture.state.position == 100
    }

    void "Shade should not auto-open too soon after a manual close"() {
        given: "It's bright outside and the shades are closed"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        when: "A few minutes pass and the user manually opens the shade"
        appScript.addMinutes(5)
        shadeFixture.open()

        and: "Twenty minutes pass and it's still bright outside"
        appScript.addMinutes(20)
        lightSensorFixture.setIlluminance(600)

        then: "The shade should not auto-close"
        shadeFixture.state.windowShade == "open"
        shadeFixture.state.position == 100
    }

    void "Shade can auto-close after a manual open, if enough time has passed"() {
        given: "It's bright outside and the shades are closed"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        when: "A few minutes pass and the user manually opens the shade"
        appScript.addMinutes(5)
        shadeFixture.open()

        and: "Two hours pass and it's still bright outside"
        appScript.addMinutes(121)
        lightSensorFixture.setIlluminance(600)

        then: "The shade should auto-close"
        shadeFixture.state.windowShade == "closed"
        shadeFixture.state.position == 0
    }

    void "Shade should not auto-close too soon after a manual open"() {
        given: "It's dark outside and the shades are open"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        when: "A few minutes pass and the user manually closes the shade"
        appScript.addMinutes(5)
        shadeFixture.close()

        and: "Twenty minutes pass and it's still dark outside"
        appScript.addMinutes(20)
        lightSensorFixture.setIlluminance(200)

        then: "The shade should not auto-open"
        shadeFixture.state.windowShade == "closed"
        shadeFixture.state.position == 0
    }

    void "Shade can auto-open after a manual close, if enough time has passed"() {
        given: "It's dark outside and the shades are open"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        when: "A few minutes pass and the user manually closes the shade"
        appScript.addMinutes(5)
        shadeFixture.close()

        and: "Two hours pass and it's still dark outside"
        appScript.addMinutes(121)
        lightSensorFixture.setIlluminance(200)

        then: "The shade should auto-open"
        shadeFixture.state.windowShade == "open"
        shadeFixture.state.position == 100
    }

}