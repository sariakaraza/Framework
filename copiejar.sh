#!/bin/bash

# chemins sources et destination
SOURCE="/home/sariaka/Documents/L3/Framework/Framework/framework-servlet.jar"
DEST="/home/sariaka/Documents/L3/Framework/Test/lib/framework-servlet.jar"

# copie avec remplacement (-f = force, -v = verbose, -u = update)
cp -f -v "$SOURCE" "$DEST"

echo "✅ Copie terminée : $DEST"
