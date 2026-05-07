#!/bin/bash

echo "Начинается сборка..."
SDK="$HOME/android_sdk"
BUILD_TOOLS="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34"

echo "Запуск aapt..."
echo ""
$BUILD_TOOLS/aapt package -f -m -J build/gen -S ./res -M AndroidManifest.xml -I $PLATFORM/android.jar

echo "Запуск javac..."
echo ""

# javac -deprecation -cp $PLATFORM/android.jar -d build/obj build/gen/maz/calculator/R.java java/ReportActivity.java
javac -deprecation -cp $PLATFORM/android.jar -d build/obj build/gen/app/monitor/R.java java/app/monitor/*.java
#
echo "Запуск d8..."
echo ""
$BUILD_TOOLS/d8 --release --lib $PLATFORM/android.jar --output build/apk/ build/obj/app/monitor/*.class

echo "Упаковка в apk..."
echo ""
$BUILD_TOOLS/aapt package -f -M AndroidManifest.xml -S ./res -I $PLATFORM/android.jar -F build/climaMonitor.unsigned.apk build/apk/
#
$BUILD_TOOLS/zipalign -f -p 4 build/climaMonitor.unsigned.apk build/climaMonitor.aligned.apk
#
echo "Формируем подпись для Google Play..."
echo ""
# keytool запускается один раз когда генерится ключ. Если ключ сгенерирован, то keytool запускать не надо. Для запуска раскомментировать строку.
# keytool -genkeypair -keystore keystore.jks -alias androidkey -validity 10000 -keyalg RSA -keysize 2048 -storepass android -keypass android
#
$BUILD_TOOLS/apksigner sign --ks keystore.jks --ks-key-alias androidkey --ks-pass pass:android --out build/climaMonitor.apk build/climaMonitor.aligned.apk
#
./load.sh
