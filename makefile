#!bin/bash

src=WEB-INF/src/borehole/
classes = WEB-INF/classes/

MainServlet.class: $(src)MainServlet.java DatabaseHelper.class
	javac -d $(classes)  $(src)MainServlet.java

DatabaseHelper.class: $(src)DatabaseHelper.java
	javac -d $(classes) $(src)DatabaseHelper.java



run:
	sudo service tomcat restart

clean:
	rm -rf $(classes)*

logs1:
	tail --lines=200 $(CATALINA_HOME)/logs/catalina.out
	
logs2:
	tail --lines=200  $(CATALINA_HOME)/logs/localhost.*.log