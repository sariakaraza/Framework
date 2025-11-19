# Nettoyage et préparation
rm -rf build
mkdir -p build

# classpath (ajustez si besoin)
CP="lib/servlet-api.jar"

# Compilation de tous les fichiers .java (dans src et sous-dossiers)
echo "📦 Compilation des sources..."
find src -name "*.java" > sources.txt
javac -parameters -cp "$CP" -d build @sources.txt

# Création du JAR
echo "🪄 Création du JAR framework-servlet.jar..."
jar -cvf framework-servlet.jar -C build .

echo "✅ Compilation terminée : framework-servlet.jar"