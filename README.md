# sicarius-monitor
## Generic purpose application monitor
This open source project is intended to simplify the monitorization of processes in a production environment. The user can easily define monitors and actions that trigger some events when the process under monitorization is no longer found.


## How does it work?
Set the monitors that you want under config.xml file. A monitor is a watcher process with some given parameters, an expected result and a list of actions to execute when the process is not found.

Here is an example of config file

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config>
	<node>SIC_1</node>
	<monitors>
		<monitor id="core" watcher="fs.sicarius.monitor.watchers.HTTPGetMonitor">
			<params>
				<path>http://${env.core.ip}/alps</path>
			</params>
			<expect>
				<status>200</status>
				<message>OK</message>
			</expect>
			<actions>
				<action actuator="fs.sicarius.monitor.actuators.LoggerAction">
					<message>Service [core] is not available</message>
				</action>
				<action actuator="fs.sicarius.monitor.actuators.ShellExecutorAction">
					<command>ping -c 1 localhost</command>
				</action>
			</actions>
		</monitor>
		<monitor id="procesos" watcher="fs.sicarius.monitor.watchers.ProcessMonitor">
			<params>
				<process>demo.DemoApplication</process>
			</params>
			<expect>
				<processes>2</processes>
			</expect>
			<actions>
				<action actuator="fs.sicarius.monitor.actuators.LoggerAction">
					<message>Service [procesos] is not available</message>
				</action>
			</actions>
		</monitor>
	</monitors>
</config>
```
As you can see, just define the type of monitor you want to use and specify its parameters. The existing watchers are

| Watcher        | Description                                                 |
|----------------|-------------------------------------------------------------|
| FileMonitor    | Watches for file system changes, allowing for file patterns |
| HTTPGetMonitor | Performs a GET/ request under the specified path            |
| ProcessMonitor | Checks for a process under Linux/MacOS                      |

Then define the actions to execute when the process is not found. The existing actions are

| Actuator            | Description                                                      |
|---------------------|------------------------------------------------------------------|
| FileCopy            | Copy a file from a destination to another, renaming if necessary |
| FileUploader        | Uses SFTP protocol to upload a file to another server            |
| ShellExecutorAction | Executes a console command under the operating system            |
| LoggerAction        | Logs an action                                                   |

## Environment ready
Use the environment variables to replace data inside the config.xml using the properties defined per environment inside the project.

In the properties

```
env.core.ip=localhost:8080
```

In the xml

```xml
<path>http://${env.core.ip}/alps</path>
```

In order to choose a desired environment properties (dev, prod, etc) run the project with the following program arguments

```
--spring.profiles.active=dani
```

## Distributed
The monitor application runs under jGroups to establish a communication channel where every monitor can see what is going on in the cluster (if any). It exposes a series of statistics and events log for a third party application to show.
