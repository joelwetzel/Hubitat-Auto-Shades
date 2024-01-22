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

import spock.lang.Specification

/**
* Basic tests for autoShadesInstance.groovy
*/
class BasicTests extends Specification {
    private HubitatAppSandbox sandbox = new HubitatAppSandbox(new File('autoShadesInstance.groovy'))

    def log = Mock(Log)

    InstalledAppWrapper app = Mock{
        _ * getName() >> "MyAppName"
    }

    def appState = [lastAutoShade: null]

    def appExecutor = Spy(AppExecutorWithEventForwarding) {
        _*getLog() >> log
        _*getApp() >> app
        _*getState() >> appState
    }

    def shadeFixture = WindowShadeFixtureFactory.create('ws')
    def lightSensorFixture = LightSensorFixtureFactory.create('ls')

    def appScript = sandbox.run(api: appExecutor,
        userSettingValues: [wrappedShade: shadeFixture, lightSensor: lightSensorFixture, illuminanceThreshold: 400, delayAfterManualClose: 120, delayAfterAutoClose: 15, delayAfterAutoOpen: 3, enableDebugLogging: true])

    def setup() {
        appExecutor.setSubscribingScript(appScript)
    }

    void "Basic validation of app script"() {
        expect:
        sandbox.run()
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
