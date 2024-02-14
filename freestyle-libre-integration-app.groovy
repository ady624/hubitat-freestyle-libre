/**
 * -----------------------
 * ------ SMART APP ------
 * -----------------------
 *
 *  Freestyle Libre 3
 *
 *  Copyright 2024 Adrian Caramaliu
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
import groovy.transform.Field
import java.text.SimpleDateFormat

@Field String BASE_URI = "https://api.libreview.io"
@Field String PATH_LOGIN = "/llu/auth/login"
@Field String PATH_TOU = "/auth/continue/tou"
@Field String PATH_CONNECTIONS = "/llu/connections"
@Field String WARNING = "<div style='color:#ffffff;font-weight: bold;background-color:#ff0000;border:10px #ff0000 solid;box-shadow: 0px 0px 10px 10px #f00'>WARNING!<br/><br/>This integration is NOT to be used for real time monitoring of glucose levels. This is NOT a medical device and should not be used in life threatening situations.</div>"

String appVersion() { return "0.1.0" }
String appModified() { return "2024-02-13"}
String appAuthor() { return "Adrian Caramaliu" }
String gitBranch() { return "ady624" }
String getAppImg(imgName) 	{ return "https://raw.githubusercontent.com/${gitBranch()}/SmartThings/master/icons/$imgName" }

definition(
    name: "Freestyle Libre Link Up Integration",
    namespace: "ady624",
    author: "Adrian Caramaliu",
    description: "Integrate Freestyle Libre with Hubitat via LinkUp",
    category: "Health",
    importUrl: "https://www.hubitatcommunity.com/todo/todo.groovy",
    iconUrl:   "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "pgMain", title: "Freestyle Libre LinkUp")
    page(name: "pgLogin", title: "LinkUp Login")
    page(name: "pgLoginFailure", title: "Freestyle Libre LinkUp")
    page(name: "pgUninstall", title: "Uninstall")
}

def appInfoSect(sect=true)	{
    def str = ""
    str += "${app?.name} (v${appVersion()})"
    str += "\nAuthor: ${appAuthor()}"
    section() { paragraph str, image: getAppImg("libre@2x.png") }
}

def pgMain() {

    if (state.previousVersion == null){
        state.previousVersion = 0;
    }

    //Brand new install (need to grab version info)
    if (!state.latestVersion){
        state.currentVersion = [:]
        state.currentVersion['SmartApp'] = appVersion()
    }
    //Version updated
    else{
        state.previousVersion = appVersion()
    }

    state.lastPage = "pgMain"

    //If fresh install, go straight to login page
    if (!settings.username){
        return pgLogin()
    }
    
    dynamicPage(name: "pgMain", nextPage: "", uninstall: false, install: true) {
        section("") {
            paragraph WARNING
        }
        appInfoSect()      
        section("Freestyle Libre LinkUp Account"){
            href "pgLogin", title: settings.username, description: "Tap to modify", params: [nextPageName: "pgMain"]
        }
        section("Connected users:"){
            state.users.each { user -> 
                paragraph "• ${user.value.firstName} ${user.value.lastName}"
            }
            
        }
        section("") {
            paragraph "Tap below to completely uninstall this SmartApp and child devices"
            href(name: "", title: "",  description: "Tap to Uninstall", required: false, page: "pgUninstall")
        }
    }
}

/* Preferences */
def pgLogin(params) {
    state.installMsg = ""
    def showUninstall = username != null && password != null
    return dynamicPage(name: "pgLogin", title: "Connect to Freestyle Libre LinkUp", nextPage:"pgLoginFailure", uninstall:false, install: false, submitOnChange: true) {
        section("") {
            paragraph WARNING
        }
	section("Credentials"){
	    input("username", "email", title: "Username", description: "Freestyle Libre LinkUp email address")
	    input("password", "password", title: "Password", description: "Freestyle Libre LinkUp  password")
	}
    }
}

def pgLoginFailure(){
    if (doLogin()) {
        refreshUsers()
        return pgMain()
    }
    else{
	return dynamicPage(name: "pgLoginFailure", title: "Login Error", install:false, uninstall:false) {
            section("") {
                paragraph WARNING
            }
            section(""){
                paragraph "The username or password you entered is incorrect. Go back and try again. "
	    }
	}
    }
}

def pgUninstall() {
    def msg = ""
    childDevices.each {
	try{
	    deleteChildDevice(it.deviceNetworkId, true)
            msg = "Devices have been removed. Tap remove to complete the process."

	}
	catch (e) {
	    log.error "Error deleting ${it.deviceNetworkId}: ${e}"
            msg = "There was a problem removing your device(s). Check the IDE logs for details."
	}
    }

    return dynamicPage(name: "pgUninstall",  title: "Uninstall", install:false, uninstall:true) {
        section("") {
            paragraph WARNING
        }
        section("Uninstall"){
	    paragraph msg
	}
    }
}



def versionCompare(deviceName){
    if (!state.currentVersion || !state.latestVersion || state.latestVersion == [:]){
        return 'latest'
    }
    if (state.currentVersion[deviceName] == state.latestVersion[deviceName]){
	return 'latest'
    }
    else{
	return "${state.latestVersion[deviceName]} available"
    }
}

/* Initialization */
def installed() {
    initialize()
}

def updated() {
    initialize()
}

/* Version Checking */

def updateVersionInfo(){
}

def uninstall(){
    getChildDevices().each {
	try{
	    deleteChildDevice(it.deviceNetworkId, true)
	}
	catch (e) {
            log.error "Error deleting ${it.deviceNetworkId}: ${e}"
	}
    }
}

def uninstalled() {
    log.info "Freestyle Libre removal complete."
}


def initialize() {
    unschedule()
    runEvery1Minute("refreshUsers")
}

def String epochToDate( Number Epoch ){
    def date = use( groovy.time.TimeCategory ) {
          new Date( 0 ) + Epoch.seconds
    }
    return date
}
                        
private login() {
    if (!state.session || (now() / 1000 > state.session.expiration)) {
	log.warn "Token has expired. Logging in again."
        doLogin()
    }
    else{
	return true;
    }
}

private doLogin() {
    state.session = [ authToken: null, expiration: 0 ]
    return doUserNameAuth()
}

/* API Methods */
private getApiHeaders() {
    headers = [
        "product": "llu.android",
        "version": "4.7"
        ]
    if (state.session?.authToken) {
        headers["Authorization"] = "Bearer ${state.session.authToken}"
    }
    return headers
}

def doUserNameAuth() {
    def result = true
    log.info "Performing login..."
    try {
        httpPost([ 
            uri: BASE_URI, 
            path: PATH_LOGIN,
            headers: getApiHeaders(),
            contentType: 'application/json',
            requestContentType: 'application/json',
            body: [
                "email": settings.username,
                "password": settings.password
            ]
        ]) { resp ->            
	    if ((resp.status == 200) && resp.data && (resp.data.status == 0)) {
                state.session = [authToken: resp.data.data.authTicket.token, expiration: resp.data.data.authTicket.expires as long]
                log.info "Login successful"
                result = true
                try {
                    httpPost([ 
                        uri: BASE_URI, 
                        path: PATH_TOU, 
                        contentType: 'application/json',
                        requestContentType: 'application/json',
                        headers: getApiHeaders()
                    ]) {}           
                }
                catch (e) {
                    log.warn "Failed to accept TOU"
                }
            }
            else {
                log.error "Error logging in: ${resp.status}"
                result = false
            }
        }
    }
    catch (e) {
        log.error "Error logging in: ${e}"
        return false
    }
    return result
}

def refreshUsers(){
    state.currentVersion = [:]
    state.currentVersion['SmartApp'] = appVersion()
    state.users = [:]
    deviceIds = getChildDevices()*.deviceNetworkId
    if (login()) {
        httpGet([ 
            uri: BASE_URI, 
            path: PATH_CONNECTIONS,
            headers: getApiHeaders(),
            contentType: 'application/json',
            requestContentType: 'application/json'
        ]) { resp ->            
            if ((resp.status == 200) && resp.data && (resp.data.status == 0)) {
                resp.data.data.each { data -> 
                    userId = data.id
                    SimpleDateFormat df = new SimpleDateFormat("M/d/yyyy H:mm:ss a");
                    user = [
                        "id": userId,
                        "firstName": data.firstName,
                        "lastName": data.lastName,
                        "country": data.country,
                        "sensorSerialNumber": data.sensor.sn,
                        "sensorActivated": data.sensor.a,
                        "glucoseTargetLow": data.targetLow,
                        "glucoseTargetHigh": data.targetHigh,
                        "glucoseTimestamp": df.parse(data.glucoseMeasurement.FactoryTimestamp).getTime() / 1000 as int,
                        "glucose": data.glucoseMeasurement.ValueInMgPerDl,
                        "glucoseTrend": data.glucoseMeasurement.TrendArrow
                    ]
                    state.users[userId] = user
                    device = getChildDevice(userId)
                    if (device) {
                        device.update(user)
                    } else {
                        log.info "Adding new device for user ${user.firstName} ${user.lastName}"
                        addChildDevice("ady624", "Freestyle Libre User", userId, ["name": "${user.firstName} ${user.lastName}"]).update(user)
                    }
                    deviceIds -= userId
                }
            }
            deviceIds.each { deviceId -> 
                log.warn "Deleting device ${deviceId}"
                deleteChildDevice(deviceId)
            }
        }
    }
}