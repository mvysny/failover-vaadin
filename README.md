[![Build Status](https://travis-ci.org/mvysny/failover-vaadin.svg?branch=master)](https://travis-ci.org/mvysny/failover-vaadin)
[![Published on Vaadin  Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/failover-vaadin)
[![Stars on Vaadin Directory](https://img.shields.io/vaadin-directory/star/failover-vaadin.svg)](https://vaadin.com/directory/component/failover-vaadin)

# FailOver Vaadin Add-On

A Vaadin Add-on which automatically performs browser-side fail-over. If the connection to the primary server is lost,
the browser will automatically redirect itself to a fallback/spare server of your choosing.

To demonstrate, check out this [FailOver Vaadin Add-On Video](https://www.youtube.com/watch?v=hWkMIDWM-E8) on Youtube.
The video shows launching of four independent Jetty instances in Docker on four different ports, 8080-8083.

The main idea behind this add-on is explained in the [Client-side Fail-over](https://vaadin.com/blog/-/blogs/client-side-fail-over) blogpost.

Advantages:

* No single point of failure. Well, the browser is the single-point-of-failure, but that's user's responsibility ;)
* Incredibly easy setup - just add Maven dependency on this add-on and recompile your widgetset.
* No server-side clustering needed
* Servers are totally independent one from another, and may even use a mixture of server kinds,
  different versions of application, ...

Disadvantages:

* Session is lost
* If the main server dies, new clients cannot connect.
* If the main server dies and user presses F5 in the browser, she will just get "Connection Refused".
  This could be remedied by offline mode

Features:

* Supports multiple fallback servers to reconnect to, either in round-robin or random-robin.
* Prior reconnecting the URL is pinged first, whether the spare server is actually alive.
* A simple load-balancer, by selecting a random server from the list instead of always choosing the primary one.
* The user must initiate the failover process manually. This way she will understand that the server has crashed and that she may lose some data (that is, the session).

Future improvements:

* Offline mode of the bootstrap page, which will connect to the spare server even 
  in case when the primary server is down.

## Quickstart

Add the following to your pom.xml:
```xml
<dependency>
    <groupId>org.vaadin.addons.failover</groupId>
    <artifactId>failover-vaadin</artifactId>
    <version>0.1.2</version>
</dependency>
```
You'll need to add a Vaadin Add-on repository as well, please see [FailOver AddOn on Vaadin Directory](https://vaadin.com/directory#!addon/failover-vaadin) on how to do that.

Then, add the following code to your UI's `init()` method:

```java
@Widgetset("AppWidgetset")
public class MyUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final List<String> urls = Arrays.asList("http://localhost:8080", "http://localhost:8081", "http://localhost:8082", "http://localhost:8083");
        final FailoverReconnectExtension failoverExtension = FailoverReconnectExtension.addTo(this);
        failoverExtension.setUrls(urls);
        failoverExtension.setPingImagePath("/VAADIN/themes/dashboard/img/app-icon.png");
        getReconnectDialogConfiguration().setDialogText("Can't connect to the server. The network may be down, or the server has crashed. Press the 'Try Spare Servers' button to try to connect to fallback server.");
        ...
    }
}
```

You will now have to configure your app to allow to ping it properly from JavaScript. Please read on.

### Important - Ping in JavaScript

Prior failing over to a server, we actually need to know whether the server is actually alive. Thus, JavaScript needs to ping the server.
However, that's not easy to do. There are two viable workaround solutions, both with drawbacks:

#### The `image` ping

The easiest way is to use the `image` element to load arbitrary image from the server. If that succeeds, the server is online. 
If that fails, the server is down. The problem is that if the target image is 404 not found or
it is not an image (but, say, a CSS), the image loading will fail.

When employing this solution, just drop any png image e.g. to your theme, then make sure that:

1. the image exists, and
2. it is actually an image

To activate this ping type, just call
```java
failoverExtension.setPingImagePath("/VAADIN/themes/dashboard/img/app-icon.png");
```

This example code works for the Vaadin Dashboard demo. Please modify it to fit your application.

#### The Ajax/`XMLHttpRequest` ping

The idea is to open a http request to the target server. If the server responds in any way, it is alive.

The problem is that browser disallows to connect to another site because of [Cross-Origin resource sharing](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing). 

When employing this type of ping, don't call the `setPingImagePath` method to activate the Ajax ping. Then, make sure that:
 
* you have CORS configured correctly in your webapp. If the CORS is misconfigured, the ping will incorrectly report the server being down.

You can follow the following tutorial to set up CORS in your webapp: https://vaadin.com/blog/-/blogs/using-cors-with-vaadin

## Add-on Development instructions 

This is a Vaadin add-on project created with in.virit:vaadin-gwt-addon archetype.
The project supports GWT based extensions for Vaadin.

1. Import to your favourite IDE
2. Run main method of the Server class to launch embedded web server that lists all your test UIs at http://localhost:9998
3. Code and test
  * create UI's for various use cases for your add-ons, see examples. These can also work as usage examples for your add-on users.
  * create browser level and integration tests under src/test/java/
  * Browser level tests are executed manually from IDE (JUnit case) or with Maven profile "browsertests" (mvn verify -Pbrowsertests). If you have a setup for solidly working Selenium driver(s), consider enabling that profile by default.
4. Test also in real world projects, on good real integration test is to *create a separate demo project* using vaadin-archetype-application, build a snapshot release ("mvn install") of the add-on and use the snapshot build in it. Note, that you can save this demo project next to your add-on project and save it to same GIT(or some else SCM) repository, just keep them separated for perfect testing.


## GWT related stuff

* To recompile test widgetset, issue *mvn vaadin:compile*, if you think the widgetset changes are not picked up by Vaadin plugin, do a *mvn clean package* or try with parameter *mvn vaadin:compile -Dgwt.compiler.force=true*
* To use superdevmode, issue "mvn vaadin:run-codeserver" and then just open superdevmode like with any other project

## Creating releases

1. Push your changes to e.g. Github 
2. Update pom.xml to contain proper SCM coordinates (first time only)
3. Use Maven release plugin (mvn release:prepare; mvn release:perform)
4. Upload the ZIP file generated to target/checkout/target directory to https://vaadin.com/directory service (and/or optionally publish your add-on to Maven central)

