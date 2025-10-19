javac -cp "lib/servlet-api.jar" -d build src/*.java
javac -cp "lib/servlet-api.jar" -d build src/annotation/*.java
jar -cvf framework-servlet.jar -C build .