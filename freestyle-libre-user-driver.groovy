/**
 *  Freestyle Libre User
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

@Field TREND_ARROWS = ["↑", "↘", "→", "↗", "↑"]
@Field TREND_MESSAGES = ["Rising rapidly", "Rising", "Steady", "Dropping", "Dropping rapidly"]


metadata {
  definition(name: "Freestyle Libre User", namespace: "ady624", author: "Adrian Caramaliu") {
    capability "Refresh"

    attribute "firstName", "string"
    attribute "lastName", "string"
    attribute "country", "string"
    attribute "sensorSerialNumber", "string"
    attribute "sensorActivated", "long"
    attribute "glucoseTargetLow", "int"
    attribute "glucoseTargetHigh", "int"
    attribute "glucoseTimestamp", "long"
    attribute "glucose", "int"
    attribute "glucoseTrend", "int"
    attribute "glucoseTrentArrow", "enum", TREND_ARROWS
    attribute "glucoseTrendMessage", "enum", TREND_MESSAGES
}

  preferences {
  }
}

void update(user) {
    updateAttribute("firstName", user.firstName as String)
    updateAttribute("lastName", user.lastName as String)
    updateAttribute("country", user.country as String)
    updateAttribute("sensorSerialNumber", user.sensorSerialNumber as String)
    updateAttribute("sensorActivated", user.sensorActivated as long)
    updateAttribute("glucoseTargetLow", user.glucoseTargetLow as int)
    updateAttribute("glucoseTargetHigh", user.glucoseTargetHigh as int)
    updateAttribute("glucoseTimestamp", user.glucoseTimestamp as long)
    updateAttribute("glucose", user.glucose as int, "mg/dL")
    updateAttribute("glucoseTrend", user.glucoseTrend as int)
    updateAttribute("glucoseTrendArrow", getTrendArrow(user.glucoseTrend) as String)
    updateAttribute("glucoseTrendMessage", getTrendMessage(user.glucoseTrend) as String)
}

void updateAttribute(String name, value, unit = null) {
    if (device.currentValue(name) as String != value as String) {
        sendEvent(name: name, value: value, unit: unit)
    }
}

def String getTrendArrow(int trend) {
    switch (trend) {
        case 1..5: return TREND_ARROWS[trend - 1]
    }
    return ""
}

def String getTrendMessage(int trend) {
    switch (trend) {
        case 1..5: return TREND_MESSAGES[trend - 1]
    }
    return ""
}

void refresh() {
  parent.refreshUsers()
}