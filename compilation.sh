# Nettoyage et préparation
rm -rf build
mkdir -p build

# classpath (ajustez si nécessaire)
CP="lib/servlet-api.jar"

# 1) compiler les annotations d'abord
javac -cp "$CP" -d build src/annotation/*.java

# 2) compiler les utilitaires (qui dépendent des annotations)
javac -cp "$CP:build" -d build src/util/*.java

# 3) compiler le reste des sources (FrontServlet, etc.)
#    on compile tous les fichiers restants en une passe en ajoutant build au classpath
javac -cp "$CP:build" -d build $(find src -maxdepth 1 -name "*.java")

# 4) créer le jar
jar -cvf framework-servlet.jar -C build .