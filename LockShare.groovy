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
        name: "LockShare",
        namespace: "re.locksha",
        author: "Eliot Stocker",
        description: "Create automations to set light states based on physical button presses",
        category: "Convenience",
        iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
        iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
        singleInstance: true
)


preferences {
    page(name: "mainPage", title: "LockShare", install: true, uninstall: true) {
        section {
            paragraph("<h1>Disclaimer</h1><p>Whilst we make every effort to add security measures where-ever possible, it is important to be aware that the purpose of the application is to share direct access to control your locks, this may mean that in the case of any security breach your physical location could be at risk, we do not store or interact with your locks in any way, all software is installed directly on your smart hub and the users mobile devices, LockShare and its  developers can not accept any responsibility for any unauthorised access to your smart hub, lock devices or physical location. The software is provided as is with no guarantee or license.")
            app(name: "lockShareSetup", appName: "LockShare Setup", namespace: "re.locksha", title: "New LockShare User", multiple: true)
        }
    }
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
}

