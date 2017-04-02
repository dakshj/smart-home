# Smart Home

Collaborators: [Daksh Jotwani](https://github.com/dakshj), [Nidhi Mundra](https://github.com/nidhimundra)

## Building the Project
**Windows**

`gradlew clean`

`gradlew build`

**Unix**

`./gradlew clean`

`./gradlew build`

## Generating a JAR
**Windows**

`gradlew clean`

`gradlew fatJar`

**Unix**

`./gradlew clean`

`./gradlew fatJar`

## Starting a Gateway Server
`java -jar {jar-file-path} 0 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/GatewayConfig.json)]

## Starting a DB Server
`java -jar {jar-file-path} 1 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/DbConfig.json)]

## Starting a Temperature Sensor Server
`java -jar {jar-file-path} 2 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/TemperatureSensorConfig.json)]

## Starting a Motion Sensor Server
`java -jar {jar-file-path} 2 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/MotionSensorConfig.json)]

## Starting a Door Sensor Server
`java -jar {jar-file-path} 2 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/DoorSensorConfig.json)]

## Starting a Presence Sensor Server
`java -jar {jar-file-path} 2 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/PresenceSensorConfig.json)]

## Starting a Bulb Server
`java -jar {jar-file-path} 3 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/BulbConfig.json)]

## Starting an Outlet Server
`java -jar {jar-file-path} 3 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/OutletConfig.json)]

## Executing a User
`java -jar {jar-file-path} 4 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/UserConfig.json)]

## Executing an Intruder
`java -jar {jar-file-path} 4 {config-json-file-path}`
[[Sample Config JSON](https://github.com/umass-cs677/smart-home/blob/master/app/txt/config/IntruderConfig.json)]

## Project Execution Steps
1. Run Gateway
1. Run all the IoTs (DB, Bulb, Outlet, Door Sensor, Motion Sensor, Presence Sensor and Temperature Sensor)
1. Input anything on Gateway
1. Run User / Intruder

## DB Server APIs Consumed by Gateway Server
* `void temperatureChanged(final TemperatureSensor temperatureSensor, final long chronologicalTime, final long logicalTime)`
* `void motionDetected(final MotionSensor motionSensor, final long chronologicalTime, final long logicalTime)`
* `void doorToggled(final DoorSensor doorSensor, final long chronologicalTime, final long logicalTime)`
* `void deviceToggled(final Device device, final long chronologicalTime, final long logicalTime)`
* `LimitedSizeArrayList<Log> getYoungestLogsList()`
* `void intruderEntered(final long chronologicalTime, final long logicalTime)`
* `void userEntered(final boolean atHome, final long synchronizedTime, final long logicalTime)`
