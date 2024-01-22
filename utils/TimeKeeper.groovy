package joelwetzel.auto_shades.utils

import groovy.time.*

class TimeKeeper {
    private Date internalDate = null

    TimeKeeper() {
        internalDate = new Date()
    }

    TimeKeeper(Date startingDate) {
        internalDate = startingDate
    }

    def install() {
        Date.metaClass.constructor = { -> return this.now() }
    }

    def set(Date newDate) {
        internalDate = newDate
    }

    def advanceMinutes(int minutes) {
        internalDate = groovy.time.TimeCategory.plus(internalDate, new groovy.time.TimeDuration(0, 0, minutes, 0, 0))
    }

    def now() {
        return internalDate
    }

    def uninstall() {
        Date.metaClass = null
    }
}