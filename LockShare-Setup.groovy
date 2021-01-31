/**
 *  LockShare
 *
 *  Copyright 2021 Eliot Stocker
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
definition(
        name: "LockShare Setup",
        namespace: "re.locksha",
        author: "Eliot Stocker",
        description: "Share Locks with people easy",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        parent: "re.locksha:LockShare",
)


preferences {
    page(name: "prefInit")
    page(name: "prefSettings")
    page(name: "prefUser")
    page(name: "prefLocks")
    page(name: "prefSchedule")
    page(name: "prefAppSetup")
    page(name: "prefNotifications")
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
    // TODO: dont need to do anything here?
    try {
        if (!state.accessToken) {
            createAccessToken()
        }
    }
    catch (ex) {
        log.warn "OAUTH is not enabled under the \"OAuth\" button in the app code editor.  This is required to use the app"
    }
}

version = "0.1.1"
qrSize = 200
messages = [
        deviceIdMismatch: "Unique Device ID does not match, please ask the location owner to reset your access",
        outsideOfSchedule: "You are currently outside of your granted access time",
        invalidInput: "App error, Input invalid, please contact the LockShare developers",
        unknownDevice: "This device has been removed, please contact location owner to check your access",
        invalidCommand: "Invalid command sent",
        codeUsed: "Code already used, please contact the homeowner in order to reset acceptance..."
]

def sectionHeader(description) {
    sectionHeader(null, description)
}

def sectionHeader(subtitle, description) {
    def t = "LockShare Settings"
    if(subtitle != null) {
        t += " | " + subtitle
    }

    section(getFormat("title", t)) {
        paragraph(description)
        paragraph getFormat("line")
    }
}

def prefInit() {
    if (app.getInstallationState() == "COMPLETE") {
        return prefSettings()
    }

    return dynamicPage(name: "prefInit", title: "", nextPage: "prefSettings", uninstall: false, install: true) {
        sectionHeader("Welcome to LockShare, Lets get started with some basic setup:")

        section() {
            label title: "Lock User", required: true, defaultValue: null
            paragraph("Select the locks you wish to share...")
            input "locks", "capability.lock", title: "Locks", submitOnChange: true, multiple: true, required: true
        }

        section() {
            paragraph("<sub>More settings and app setup info will be available once initial setup is complete...</sub>")
        }
    }
}

def prefSettings() {
    return dynamicPage(name: "prefSettings", title: "", uninstall: true, install: true) {
        sectionHeader(app.label, "Welcome to LockShare, What do you want to access?")
        section {
            href(name: "prefUser", title: "Edit User", required: false, page: "prefUser", description: "Edit User, see Acceptance data, and reset app installs.")
            href(name: "prefLocks", title: "Edit Locks", required: false, page: "prefLocks", description: "Change Locks available to this user.")
            href(name: "prefSchedule", title: "Edit Schedule", required: false, page: "prefSchedule", description: "Edit users access schedule.")
            href(name: "prefAppSetup", title: "Get App Setup Details", required: false, page: "prefAppSetup", description: "Get details required to setup the lockshare mobile app for this user.")
            href(name: "prefNotifications", title: "Notification Settings", required: false, page: "prefNotifications", description: "Setup notifications of this users activity.")
        }
    }
}

def prefUser() {
    return dynamicPage(name: "prefUser", title: "", uninstall: false, install: false) {
        sectionHeader("User Settings", "Set the name of the person you are sharing with,<br><b>It is best practice to create a new lock share instance per user</b>")

        section {
            label title: "Lock User", required: true, defaultValue: null
        }

        section("Security") {
            input "singleAcceptance", "bool", title: "Only allow code to be used once", submitOnChange: true, defaultValue: true
            if (state.accepted) {
                paragraph("Last Accepted Share on: ${state.acceptedDate} with Device: ${state.device} (${state.type})")
                input "resetAcceptance", "button", title: "Reset Share Acceptance", submitOnChange: true
                paragraph("<sub>this will revoke access, allowing the code to be used again if <b>only allow code to be used once</b> is enabled</sub>")
            }
        }
    }
}

def prefLocks() {
    return dynamicPage(name: "prefLocks", title: "", uninstall: false, install: false) {
        sectionHeader("Lock Selection", "Select which locks this user has access to")

        section {
            input "locks", "capability.lock", title: "Locks", submitOnChange: true, multiple: true, required: true
        }
    }
}

def prefSchedule() {
    return dynamicPage(name: "prefSchedule", title: "", uninstall: false, install: false) {
        sectionHeader("Schedule", "Here you can set a specific date, or days of the week and time when a user has access, you may also setup permanent access")

        section {
            input "scheduleType", "enum", title: "Access Schedule", submitOnChange: true, options: ["permanent": "Permanent", "daily": "Daily Schedule", "day": "Single Day"], required: true
            if(scheduleType == "daily") {
                input "scheduleDays", "enum", title: "Days of the Week", submitOnChange: true, options: ["mon": "Monday", "tue": "Tuesday", "wed": "Wednesday", "thu": "Thursday", "fri": "Friday", "sat": "Saturday", "sun": "Sunday"], multiple: true, required: true
            }
            if(scheduleType == "day") {
                input "scheduleDate", "date", title: "Date", submitOnChange: true, required: true
            }
            if(scheduleType == "daily" || scheduleType == "day") {
                input "scheduleStartTime", "time", title: "Start Time", submitOnChange: true, required: true
                input "scheduleEndTime", "time", title: "End Time", submitOnChange: true, required: true
            }
        }
    }
}

def prefAppSetup() {
    def lockShareUNIPrefix = "https://locksha.re/share/?"
    def serviceUrl = "${getFullApiServerUrl()}/lockShare?access_token=${atomicState.accessToken}";
    def encoded = serviceUrl.bytes.encodeBase64().toString()

    return dynamicPage(name: "prefAppSetup", title: "", uninstall: false, install: false) {
        sectionHeader("App Setup", "Bellow you will find a number of methods to setup a user with LockShare")

        if((singleAcceptance || singleAcceptance == null) && state.accepted) {
            section("Warning") {
                paragraph(getFormat("error", "This lockshare has been used, it will not work again until reset"))
                href(name: "prefUser", title: "Edit User", required: false, page: "prefUser", description: "Go to user Settings to reset the code.")
            }
        }

        section {
            paragraph("Please have the user install the LockShare app")
            paragraph("<center><a href='https://play.google.com/store/apps/details?id=re.locksha.mobile' target=\"_blank\"><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' style=\"width:200px\"/></a></center>")
        }

        section {
            paragraph("The user may scan the following QR code in LockShare or other QR Code Scanner app:")
            paragraph("<center><img src=\"https://chart.googleapis.com/chart?cht=qr&chs=${qrSize}x${qrSize}&chl=${lockShareUNIPrefix}${encoded}&choe=UTF-8\"/></center>")
            paragraph("<br>Or you can send them this special LockShare URL:<br><sub>the link will guide them to install the app if they havnt already</sub>")
            paragraph("<a href=\"${lockShareUNIPrefix}${encoded}\"><pre style=\"white-space: pre-wrap; word-wrap: break-word;\">${lockShareUNIPrefix}${encoded}</pre></a>")
            paragraph(linkButton("Send By SMS", "sms:?&body=Hi,%20Im%20sharing%20a%20lock%20with%20you%20from%20LockSha.re%20click%20this%20link%20to%20get%20access:%20${lockShareUNIPrefix}${encoded}", "with defualt SMS app"))
            paragraph(linkButton("Send By Email", "mailto:?&body=Hi,%20Im%20sharing%20a%20lock%20with%20you%20from%20LockSha.re%20click%20this%20link%20to%20get%20access:%20${lockShareUNIPrefix}${encoded}", "with defualt Email app"))
        }
    }
}

def prefNotifications() {
    return dynamicPage(name: "prefNotifications", title: "", uninstall: false, install: false) {
        sectionHeader("Notifications", "In order to monitor the use of the lock you may enable notification of Code useage and Lock opperations for this user")

        section {
            paragraph("Select a device to notify on share actions...")
            input "notifyDevice", "capability.notification", title: "Notification Device(s)", submitOnChange: true, multiple: true
        }

        section("Notifications") {
            input "notifyAccepted", "bool", title: "Notify On Share Acceptance", submitOnChange: true
            input "notifyOpperation", "bool", title: "Notify On Lock Opperation", submitOnChange: true
        }
    }
}

def appButtonHandler(btn) {
    switch (btn) {
        case "resetAcceptance":
            state.accepted = false
            state.acceptedDate = null
            state.device = null
            state.type = null
            state.deviceId = null
            break
    }
}

mappings {
    path("/share") {
        action:
        [
                GET: "listAvailableDevices"
        ]
    }
    path("/accept") {
        action:
        [
                POST: "processAcceptance"
        ]
    }
    path("/share/:id") {
        action:
        [
                GET: "getDevice",
                POST: "processLockCommand"
        ]
    }
    path("lockShare") {
        action:
        [
                GET: "getInfo"
        ]
    }
}

def checkDeviceIdHeader(req) {
    if(singleAcceptance || singleAcceptance == null) {
        if(!state.accepted) {
            return false;
        }
        if(!req.headers || !req.headers["X-unique-device"]) {
            return false;
        }
        if(req.headers["X-unique-device"] == state.deviceId) {
            return true;
        }
    }
    return true;
}

def isWithinSchedule() {
    def date = new Date()
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);

    if(scheduleType == "permanent" || scheduleType == null) {
        return true;
    } else if(scheduleType == "daily") {
        def dayMap = [
                1: "sun",
                2: "mon",
                3: "tue",
                4: "wed",
                5: "thur",
                6: "fri",
                7: "sat"
        ]

        def dow = calendar.get(Calendar.DAY_OF_WEEK)

        if(scheduleDays.contains(dayMap[dow])) {
            return isBetweenScheduledHours()
        }
    } else if(scheduleType == "day") {
        def dom = calendar.get(Calendar.DAY_OF_MONTH)
        def month = calendar.get(Calendar.MONTH) + 1
        def year = calendar.get(Calendar.YEAR)

        def dateString = "$year-$month-$dom"

        if(scheduleDate == dateString) {
            return isBetweenScheduledHours()
        }
    }

    return false;
}

def isBetweenScheduledHours() {
    return timeOfDayIsBetween(timeToday(scheduleStartTime), timeToday(scheduleEndTime), new Date());
}

def processLockCommand() {
    def json
    try {
        json = parseJson(request.body)
    }
    catch (e) {
        log.error "JSON received from app is invalid! ${request.body}"
        return renderError(400, messages.invalidInput)
    }

    if(!checkDeviceIdHeader(request)) {
        return renderError(401, messages.deviceIdMismatch)
    }


    def lock = locks.find {it.id == params.id}

    if(!lock) {
        return renderError(404, messages.unknownDevice);
    }

    if(!isWithinSchedule()) {
        return renderError(425, messages.outsideOfSchedule);
    }

    if(json.command == "unlock") {
        lock.unlock()
    } else if(json.command == "lock") {
        lock.lock()
    } else {
        return renderError(404, "${messages.invalidCommand}: ${json.command}");
    }

    if(notifyOpperation && notifyDevice) {
        notifyDevice.deviceNotification("LockShare User: \"${app.label}\" ${json.command}ed \"${lock.displayName}\"")
    }

    return [
            service: [
                    version: version
            ],
            id: lock.id,
            name: lock.displayName,
            status: json.command == "lock" ? "locking" : "unlocking"
    ]
}

def getDevice() {
    if(!checkDeviceIdHeader(request)) {
        return renderError(401, messages.deviceIdMismatch);
    }

    def device = locks.find {it.id == params.id}
    return [
            service: [
                    version: version
            ],
            name: device.displayName,
            id: device.id,
            status: isWithinSchedule() ? device.currentStates.find { it.name == "lock" }.value : 'unavailable'
    ]
}

def getSchedule() {
    def schedule = [
            type: scheduleType == null ? "permanent" : scheduleType
    ];

    if(scheduleType == "day") {
        schedule['date'] = scheduleDate
        schedule['startTime'] = scheduleStartTime
        schedule['endTime'] = scheduleEndTime
    }

    if(scheduleType == "daily") {
        schedule['days'] = scheduleDays
        schedule['startTime'] = scheduleStartTime
        schedule['endTime'] = scheduleEndTime
    }

    return schedule
}

def generateDeviceID() {
    def generator = { String alphabet, int n ->
        new Random().with {
            (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
        }
    }

    return generator((('A'..'Z')+('a'..'z')+('0'..'9')).join(), 24)
}

def processAcceptance() {
    def json
    try {
        json = parseJson(request.body)
    }
    catch (e) {
        log.error "JSON received from app is invalid! ${request.body}"
        return renderError(400, messages.invalidInput)
    }

    if((singleAcceptance || singleAcceptance == null) && state.accepted) {
        if(notifyDevice) {
            notifyDevice.deviceNotification("LockShare: \"${app.label}\" attempted to accept on Device: \"${deviceName}\"")
        }

        return renderError(401, messages.codeUsed)
    }

    def deviceName = json.device
    def type = json.type

    state.accepted = true
    state.acceptedDate = new Date()
    state.device = json.device
    state.type = json.type
    state.deviceId = generateDeviceID()

    log.debug "LockShare: \"${app.label}\" Accepted on Device: \"${deviceName}\""

    if(notifyAccepted && notifyDevice) {
        notifyDevice.deviceNotification("LockShare: \"${app.label}\" Accepted on Device: \"${deviceName}\"")
    }

    return [
            service: [
                    version: version
            ],
            status: "accepted",
            uuid: state.deviceId
    ]
}

def listAvailableDevices() {
    if(!checkDeviceIdHeader(request)) {
        return renderError(401, messages.deviceIdMismatch)
    }

    return [
            service: [
                    version: version
            ],
            name: app.label,
            location: getLocation().name,
            schedule: getSchedule(),
            currentAccess: isWithinSchedule(),
            devices: locks.collect {
                return [
                        name: it.displayName,
                        id: it.id,
                        status: isWithinSchedule() ? it.currentStates.find { it.name == "lock" }.value : 'unavailable'
                ]
            }
    ]
}

def getInfo() {
    if(!params.callback) {
        throw new Exception("no callback specified")
    }

    def data = [
            name: app.label,
            location: getLocation().name
    ]

    String json = new groovy.json.JsonBuilder(data).toString()

    render contentType: "application/javascript", data: "${params.callback}(${json});", status: 200
}

def linkButton(title, link, description = "click to navigate") {
    def classes = "btn btn-default btn-lg btn-block hrefElem  mdl-button--raised mdl-shadow--2dp"
    def start = "<a href=\"${link}\"><span class=\"${classes}\">"
    def end = "<span></a>"
    def titleBlock = "<span>${title}</span>"
    def descriptionBlock = "<span class=\"state-incomplete-text\">${description}</span>"

    return "${start}${titleBlock}<br>${descriptionBlock}${end}"
}

def getFormat(type, text=""){            // Modified from @Stephack Code
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${text}</h2>"
    if(type == "error") return "<h4 style='color:#FF0000;font-weight: bold'>${text}</h4"
}

def renderError(int code, String data) {
    return renderError(code, [
            error: true,
            message: data,
            service: [
                    version: version
            ],
    ])
}

def renderError(int code, Map data) {
    String json = new groovy.json.JsonBuilder(data).toString()

    render contentType: "application/json", data: json, status: code
}