package fs.sicarius.monitor;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fs.sicarius.monitor.actuators.IAction;
import fs.sicarius.monitor.watchers.IAsyncMonitor;
import fs.sicarius.monitor.watchers.IMonitor;


@Component
public class Engine {
     private Logger log = LoggerFactory.getLogger(Engine.class);
     private List<Check> toCheck = new ArrayList<>();
     private Pattern pattern=null;
     private Matcher matcher=null;

     /** Map of async monitors and their callable function */
     private HashMap<String, Future<Boolean>> asyncMonitors = new HashMap<>();

     /** Thread executor for async monitors */
     private ExecutorService executor = Executors.newFixedThreadPool(1);

     @Autowired
     private Cluster cluster;

     @Autowired
     private Environment env;

     @SuppressWarnings("unchecked")
     @PostConstruct
     public void initialize() throws Exception {
          pattern=Pattern.compile("\\$\\{([a-zA-Z]+(\\.[a-zA-Z]+)*)\\}");
          DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
          String path = getClass().getClassLoader().getResource("config.xml").getFile();
          if (env.containsProperty("config.path") && !env.getProperty("config.path").isEmpty()) {
               path = env.getProperty("config.path") + "/config.xml";
          }
          File fXmlFile = new File(path);
          Document doc = dBuilder.parse(fXmlFile);

          // Get basic config
          String nodeName = doc.getElementsByTagName("node").item(0).getTextContent();
          cluster.syncMember(nodeName);

          // Get monitors
          NodeList nList = doc.getElementsByTagName("monitor");
          for (int temp = 0; temp < nList.getLength(); temp++) {
               Element nNode = (Element) nList.item(temp);
               if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (!eElement.hasAttribute("disabled") || (eElement.hasAttribute("disabled") && eElement.getAttribute("disabled").equals("false"))) {
                         String monitorURL = eElement.getAttribute("watcher");
                         String id = eElement.getAttribute("id");
                         HashMap<String, Object> params = new HashMap<>();
                         HashMap<String, Object> expect = new HashMap<>();

                         List<HashMap<String, Object>> actions = new ArrayList<>();
                         IMonitor mon = null;
                         IAction act = null;

                         // fetch params
                         Element pParam = (Element) eElement.getElementsByTagName("params").item(0);
                         NodeList lstParams = pParam.getElementsByTagName("*");
                         for (int index=0; index<lstParams.getLength(); index++) {
                              String key = lstParams.item(index).getNodeName();
                              String value = lstParams.item(index).getTextContent();
                              params.put(key, resolveVariables(value));
                         }
                         // fetch expect
                         if (eElement.getElementsByTagName("expect").item(0)!=null) {
                              Element pExpect = (Element) eElement.getElementsByTagName("expect").item(0);
                              NodeList lstExpect = pExpect.getElementsByTagName("*");
                              for (int index=0; index<lstExpect.getLength(); index++) {
                                   String key = lstExpect.item(index).getNodeName();
                                   String value = lstExpect.item(index).getTextContent();
                                   expect.put(key, resolveVariables(value));
                              }
                         }
                         // fetch actions
                         NodeList allActions = eElement.getElementsByTagName("action");
                         for (int index=0; index<allActions.getLength(); index++) {
                              HashMap<String, Object> action = new HashMap<>();
                              Element pAction = (Element) allActions.item(index);
                              String actuator = pAction.getAttribute("actuator");
                              NodeList lstActions = pAction.getElementsByTagName("*");
                              for (int a=0; a<lstActions.getLength(); a++) {
                                   String key = lstActions.item(a).getNodeName();
                                   String value = lstActions.item(a).getTextContent();
                                   action.put(key, resolveVariables(value));
                              }
                              action.put("actuator", actuator);
                              actions.add(action);
                         }

                         try {
                              // Instantiate the given monitor
                              Class<?> monitorClass = Class.forName(monitorURL);
                              mon = (IMonitor) monitorClass.newInstance();
                              // Set object attributes
                              if (!params.isEmpty()) {
                                   Iterator<Map.Entry<String, Object>> it = params.entrySet().iterator();
                                   while (it.hasNext()) {
                                        Map.Entry<String, Object> param = it.next();
                                        try {
                                             Field field = monitorClass.getDeclaredField(param.getKey());
                                             field.setAccessible(true);
                                             field.set(mon, param.getValue());
                                        } catch (Exception e) {
                                             log.error("Property {} is not accessible in {}", param.getKey(), monitorURL);
                                        }
                                   }
                              }

                              // Set expect object if any
                              if (!expect.isEmpty()) {
                                   try {
                                        Field field = mon.getClass().getSuperclass().getDeclaredField("expect");
                                        field.setAccessible(true);
                                        ((HashMap<String,Object>)field.get(mon)).putAll((Map<String,Object>) expect);
                                   } catch (Exception e) {
                                        log.error("Property expect is not accessible in {}", monitorURL);
                                   }
                              }

                              // Instantiate the given actions
                              List<IAction> toExecute = new ArrayList<>();
                              // Set action attributes
                              for (HashMap<String, Object> anAction : actions) {
                                   Class<?> actionClass = Class.forName(anAction.get("actuator").toString());
                                   anAction.remove("actuator");
                                   Iterator<Map.Entry<String, Object>> it = anAction.entrySet().iterator();
                                   act = (IAction) actionClass.newInstance();
                                   while (it.hasNext()) {
                                        Map.Entry<String, Object> param = it.next();
                                        try {
                                             Field field = actionClass.getDeclaredField(param.getKey());
                                             field.setAccessible(true);
                                             if (field.getType().isAssignableFrom(String.class)) {
                                                  field.set(act, param.getValue());
                                             } else if (field.getType().isAssignableFrom(Long.class)) {
                                                  field.set(act, Long.valueOf(param.getValue().toString()));
                                             } else if (field.getType().isAssignableFrom(Integer.class)) {
                                                  field.set(act, Integer.valueOf(param.getValue().toString()));
                                             }
                                        } catch (Exception e) {
                                             log.error("Property {} is not accessible in {}", param.getKey(), monitorURL);
                                        }
                                   }
                                   toExecute.add(act);
                              }

                              // Add it to the execution queue
                              toCheck.add(new Check(id, mon, toExecute));
                         } catch (Exception e) {
                              log.error("Unable to instantiate monitor {} due to {}", monitorURL, e.getMessage());
                         }
                    }
               }
          }
     }

     /**
     * Replaces ${env.variables} in the given text using the environment properties
     * @param text
     * @return
     */
     private String resolveVariables(String text) {
          matcher=pattern.matcher(text);
          if (matcher.find()) {
               // get the property
               String replace = env.getProperty(matcher.group(1));
               return text.replace(matcher.group(0), replace);
          }
          return text;
     }

     @Scheduled(fixedRateString="${delay.check}")
     public void checkAllMonitors() {
          for (int index=0; index<toCheck.size(); index++) {
               Check item = toCheck.get(index);
               boolean checks = false;

               // initialize async monitors if any
               if (item.monitor instanceof IAsyncMonitor) {
                    // If the task has not been executed before, do it
                    if (!asyncMonitors.containsKey(item.id)) {
                         Callable<Boolean> task = () -> {
                              return item.monitor.check();
                         };
                         Future<Boolean> future = executor.submit(task);
                         asyncMonitors.put(item.id, future);
                    }
                    // Get the defered result for the given watcher
                    Future<Boolean> future = asyncMonitors.get(item.id);

                    // Check the result
                    try {
                         // the async task is completed
                         if (future.isDone()) {
                              asyncMonitors.remove(item.id);
                              // the monitor returned false. Trigger the actions
                              if (!future.get()) {
                                   if (!item.triggered) {
                                        // execute the action
                                        item.trigger.forEach(action-> action.execute(item.monitor));
                                        // communicate the cluster failure
                                        cluster.registerEvent(item.id, new Event("Service did not respond. Executed " + item.trigger.size() + " actions", System.currentTimeMillis()));
                                        item.triggered = true;
                                   } else {
                                        log.debug("Ignoring trigger because monitor already executed the actions");
                                   }
                                   // The monitor returned true. Service is alive. Reset the triggered flag
                              } else {
                                   if (item.triggered)
                                        log.debug("Monitor {} reported that service is back online again", item.id);
                                   item.triggered = false;
                              }
                         }
                    } catch (Exception e) {
                         log.error("Error executing async monitor {}. Reason is {}", item.id, e.getMessage());
                    }
                    // check sync monitors if any
               } else {
                    // the monitor returned false. Trigger the actions
                    if (!(checks=item.monitor.check())){
                         if (!item.triggered) {
                              // execute the action
                              item.trigger.forEach(action-> action.execute(item.monitor));
                              // communicate the cluster failure
                              cluster.registerEvent(item.id, new Event("Service did not respond. Executed " + item.trigger.size() + " actions", System.currentTimeMillis()));
                              item.triggered = true;
                         } else {
                              log.debug("Ignoring trigger because monitor already executed the actions");

                         }
                         // The monitor returned true. Service is alive. Reset the triggered flag
                    } else {
                         if (item.triggered)
                              log.debug("Monitor {} reported that service is back online again", item.id);
                         item.triggered = false;
                    }
               }

               // communicate cluster the monitor status
               cluster.updateStats(item.id, new Stat(checks, System.currentTimeMillis()));
          }
     }
}
