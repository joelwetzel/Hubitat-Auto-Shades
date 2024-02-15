package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.WindowShadeFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LightSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Behavior tests for autoShadesInstance.groovy
*/
class BehaviorTests extends IntegrationAppSpecification {
    def shadeFixture = WindowShadeFixtureFactory.create('ws')
    def lightSensorFixture = LightSensorFixtureFactory.create('ls')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "autoShadesInstance.groovy",
                                    userSettingValues: [wrappedShade: shadeFixture, lightSensor: lightSensorFixture, illuminanceThreshold: 400, delayAfterManualClose: 120, delayAfterAutoClose: 15, delayAfterAutoOpen: 3, enableDebugLogging: true])
        appScript.installed()
    }

    void "Shade should auto-open when it gets dark"() {
        given: "It's bright outside"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        when: "A cloud blocks the sun"
        lightSensorFixture.setIlluminance(200)

        then: "The shade should auto-open"
        shadeFixture.currentValue('windowShade') == "open"
        shadeFixture.currentValue('position') == 100
    }

    void "Shade should not auto-close too soon after auto-opening"() {
        given: "It's bright outside"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        and: "A cloud blocks the sun and the shades open"
        lightSensorFixture.setIlluminance(200)

        when: "Two minutes pass"
        TimeKeeper.advanceMinutes(2)

        and: "The cloud passes and the illuminance rises above the threshold"
        lightSensorFixture.setIlluminance(600)

        then: "The shade should not auto-close yet"
        shadeFixture.currentValue('windowShade') == "open"
        shadeFixture.currentValue('position') == 100
    }

    void "Shade can auto-close after an auto-open, if enough time has passed"() {
        given: "It's bright outside"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        and: "A cloud blocks the sun and the shades open"
        lightSensorFixture.setIlluminance(200)

        when: "Twenty minutes pass"
        TimeKeeper.advanceMinutes(20)

        and: "The cloud passes and the illuminance rises above the threshold"
        lightSensorFixture.setIlluminance(600)

        then: "The shade should auto-close"
        shadeFixture.currentValue('windowShade') == "closed"
        shadeFixture.currentValue('position') == 0
    }

    void "Shade should auto-close when it gets bright"() {
        given: "It's dark outside"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        when: "The sun comes out"
        lightSensorFixture.setIlluminance(600)

        then: "The shade should auto-close"
        shadeFixture.currentValue('windowShade') == "closed"
        shadeFixture.currentValue('position') == 0
    }

    void "Shade should not auto-open too soon after auto-closing"() {
        given: "It's dark outside"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        and: "The sun comes out and the shades close"
        lightSensorFixture.setIlluminance(600)

        when: "Two minutes pass"
        TimeKeeper.advanceMinutes(2)

        and: "The sun goes behind a cloud and the illuminance drops below the threshold"
        lightSensorFixture.setIlluminance(200)

        then: "The shade should not auto-open yet"
        shadeFixture.currentValue('windowShade') == "closed"
        shadeFixture.currentValue('position') == 0
    }

    void "Shade can auto-open after an auto-close, if enough time has passed"() {
        given: "It's dark outside"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        and: "The sun comes out and the shades close"
        lightSensorFixture.setIlluminance(600)

        when: "Twenty minutes pass"
        TimeKeeper.advanceMinutes(20)

        and: "The sun goes behind a cloud and the illuminance drops below the threshold"
        lightSensorFixture.setIlluminance(200)

        then: "The shade should auto-open"
        shadeFixture.currentValue('windowShade') == "open"
        shadeFixture.currentValue('position') == 100
    }

    void "Shade should not auto-open too soon after a manual close"() {
        given: "It's bright outside and the shades are closed"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        when: "A few minutes pass and the user manually opens the shade"
        TimeKeeper.advanceMinutes(5)
        shadeFixture.open()

        and: "Twenty minutes pass and it's still bright outside"
        TimeKeeper.advanceMinutes(20)
        lightSensorFixture.setIlluminance(600)

        then: "The shade should not auto-close"
        shadeFixture.currentValue('windowShade') == "open"
        shadeFixture.currentValue('position') == 100
    }

    void "Shade can auto-close after a manual open, if enough time has passed"() {
        given: "It's bright outside and the shades are closed"
        shadeFixture.initialize(appExecutor, [windowShade: "closed"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 600])

        when: "A few minutes pass and the user manually opens the shade"
        TimeKeeper.advanceMinutes(5)
        shadeFixture.open()

        and: "Two hours pass and it's still bright outside"
        TimeKeeper.advanceMinutes(121)
        lightSensorFixture.setIlluminance(600)

        then: "The shade should auto-close"
        shadeFixture.currentValue('windowShade') == "closed"
        shadeFixture.currentValue('position') == 0
    }

    void "Shade should not auto-close too soon after a manual open"() {
        given: "It's dark outside and the shades are open"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        when: "A few minutes pass and the user manually closes the shade"
        TimeKeeper.advanceMinutes(5)
        shadeFixture.close()

        and: "Twenty minutes pass and it's still dark outside"
        TimeKeeper.advanceMinutes(20)
        lightSensorFixture.setIlluminance(200)

        then: "The shade should not auto-open"
        shadeFixture.currentValue('windowShade') == "closed"
        shadeFixture.currentValue('position') == 0
    }

    void "Shade can auto-open after a manual close, if enough time has passed"() {
        given: "It's dark outside and the shades are open"
        shadeFixture.initialize(appExecutor, [windowShade: "open"])
        lightSensorFixture.initialize(appExecutor, [illuminance: 200])

        when: "A few minutes pass and the user manually closes the shade"
        TimeKeeper.advanceMinutes(5)
        shadeFixture.close()

        and: "Two hours pass and it's still dark outside"
        TimeKeeper.advanceMinutes(121)
        lightSensorFixture.setIlluminance(200)

        then: "The shade should auto-open"
        shadeFixture.currentValue('windowShade') == "open"
        shadeFixture.currentValue('position') == 100
    }

}
