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

import joelwetzel.auto_shades.utils.TimeKeeper

import spock.lang.Specification

class TimeKeeperTests extends Specification {
    def currentDate = new Date()

    def setup() {
    }

    void "Can make normal Date objects"() {
        when:
            def date = new Date()

        then:
            def diff = groovy.time.TimeCategory.minus(date, currentDate)
            diff.minutes == 0
            diff.hours == 0
            diff.days == 0
            diff.seconds < 1
    }

    void "Can use the TimeKeeper class to override default Date constructor"() {
        given:
            def timekeeper = new TimeKeeper(Date.parse("yyyy-MM-dd hh:mm:ss", "2014-08-31 8:23:45"))
            timekeeper.install()

        when:
            def date = new Date()

        then:
            date != currentDate
            date.toString() == "Sun Aug 31 08:23:45 CDT 2014"

        cleanup:
            timekeeper.uninstall()
    }

    void "Can set the TimeKeeper after it's already constructed"() {
        given:
            def timekeeper = new TimeKeeper()
            timekeeper.install()

        when:
            def date = new Date()

        then:
            date.toString() == currentDate.toString()

        when:
            timekeeper.set(Date.parse("yyyy-MM-dd hh:mm:ss", "2014-08-31 8:23:45"))
            def date2 = new Date()

        then:
            date2 != currentDate
            date2.toString() == "Sun Aug 31 08:23:45 CDT 2014"

        cleanup:
            timekeeper.uninstall()
    }

    void "Can advance the minutes of the TimeKeeper"() {
        given:
            def timekeeper = new TimeKeeper(Date.parse("yyyy-MM-dd hh:mm:ss", "2014-08-31 8:23:45"))
            timekeeper.install()

        when:
            def date = new Date()

        then:
            date != currentDate
            date.toString() == "Sun Aug 31 08:23:45 CDT 2014"

        when:
            timekeeper.advanceMinutes(5)
            def date2 = new Date()

        then:
            date2 != currentDate
            date2.toString() == "Sun Aug 31 08:28:45 CDT 2014"

        cleanup:
            timekeeper.uninstall()
    }

    void "Uninstalling the TimeKeeper makes the Date class return current date/time again"() {
        given:
            def timekeeper = new TimeKeeper(Date.parse("yyyy-MM-dd hh:mm:ss", "2014-08-31 8:23:45"))
            timekeeper.install()

        when:
            def date = new Date()

        then:
            date != currentDate
            date.toString() == "Sun Aug 31 08:23:45 CDT 2014"

        when:
            timekeeper.uninstall()
            def date2 = new Date()

        then:
            date2.toString() == currentDate.toString()
    }
}
