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
As you can see, just define the type of monitor you want to use and specify its parameters. Right now there are only two types of watchers

1. HTTPGetMonitor -> Performs a GET/ request under the specified path
2. ProcessMonitor -> Performs a ps -e command under UNIX/MacOS systems, looking for the given process


Then define the actions to execute when the process is not found. Right now there are only two types of actions

1. LoggerAction -> Prints the trace specified in params
2. ShellExecutorAction -> Executes the given command, printing the response

## Distributed
The monitor application runs under jGroups to establish a communication channel where every monitor can see what is going on in the cluster (if any). It exposes a series of statistics and events log for a third party application to show.
