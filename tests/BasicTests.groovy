package joelwetzel.auto_shades.tests

import me.biocomp.hubitat_ci.util.device_fixtures.WindowShadeFixtureFactory
import me.biocomp.hubitat_ci.util.device_fixtures.LightSensorFixtureFactory
import me.biocomp.hubitat_ci.util.integration.IntegrationAppSpecification
import me.biocomp.hubitat_ci.util.integration.TimeKeeper

import spock.lang.Specification

/**
* Basic tests for autoShadesInstance.groovy
*/
class BasicTests extends IntegrationAppSpecification {
    def shadeFixture = WindowShadeFixtureFactory.create('ws')
    def lightSensorFixture = LightSensorFixtureFactory.create('ls')

    @Override
    def setup() {
        super.initializeEnvironment(appScriptFilename: "autoShadesInstance.groovy",
                                    userSettingValues: [wrappedShade: shadeFixture, lightSensor: lightSensorFixture, illuminanceThreshold: 400, delayAfterManualClose: 120, delayAfterAutoClose: 15, delayAfterAutoOpen: 3, enableDebugLogging: true])
    }

    void "installed() logs the settings"() {
        when:
        // Run installed() method on app script.
        appScript.installed()

        then:
        // Expect that log.info() was called with this string
        1 * log.info('Installed with settings: [wrappedShade:GeneratedDevice(input: ws, type: t), lightSensor:GeneratedDevice(input: ls, type: t), illuminanceThreshold:400, delayAfterManualClose:120, delayAfterAutoClose:15, delayAfterAutoOpen:3, enableDebugLogging:true]')
    }

    void "initialize() subscribes to events"() {
        when:
        appScript.initialize()

        then:
        // Expect that events are subscribe to
        1 * appExecutor.subscribe(shadeFixture, 'windowShade', 'windowShadeHandler')
        1 * appExecutor.subscribe(lightSensorFixture, 'illuminance', 'illuminanceHandler')
    }
}
