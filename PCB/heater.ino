// This example uses an ESP32 Development Board
// to connect to shiftr.io.
//
// You can check on your device after a successful
// connection here: https://www.shiftr.io/try.
//
// by Joël Gähwiler
// https://github.com/256dpi/arduino-mqtt

#include <WiFi.h>
#include <MQTT.h>
#include "max6675.h"
#include <driver/ledc.h>

int thermoDO = 19;
int thermoCS = 23;
int thermoCLK = 5;

const int pHeat1 = 12;
const int pHeat2 = 13;
const int pFan = 14;
const int pLed = 2;

const int cLed = 0;
const int cFan = 1;
const int cHeat1 = 2;
const int cHeat2 = 3;
const int frequency = 50;  // PWM frequency in Hz
const int resolution = 8;  // PWM resolution (bits)

int status = 0; 
int count = 0;

MAX6675 thermocouple(thermoCLK, thermoCS, thermoDO);



const char ssid1[] = "***";
const char password1[] = "8***";

const int maxRetryCount = 3;
int retryCount = 0;
WiFiClient net;
MQTTClient client;

unsigned long lastMillis = 0;


void connectToWiFi() {
  Serial.println("Connecting to Wi-Fi...");

  // Connect to the first Wi-Fi network
  WiFi.begin(ssid1, password1);
  retryCount = 0;
  while (WiFi.status() != WL_CONNECTED && retryCount < maxRetryCount) {
    retryCount++;
    Serial.print("Connection failed! Retrying...");
    delay(2000);
    WiFi.begin(ssid1, password1);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("Connected to: " + String(ssid1));
    return;
  }

  Serial.println("Failed to connect to: " + String(ssid1));
}



void connect() {
  Serial.print("checking wifi...");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(1000);
  }

  Serial.print("\nconnecting...");
  while (!client.connect("esp32", "ggyfdoic:ggyfdoic", "22AeiYRN7FGHZqXUn5Hlz1Ga5Kg-qfby")) {
    Serial.print(".");
    delay(1000);
  }

  Serial.println("\nconnected!");

  client.subscribe("/fan");
  client.subscribe("/heat1");
  client.subscribe("/heat2");
  client.subscribe("/led");
  client.subscribe("/hello");
  client.subscribe("/start");
}

void messageReceived(String &topic, String &payload) {
  if (topic == "/hello") {
    // Handle topic1
    Serial.println("incoming: " + topic + " - " + payload);
  } else if (topic == "/fan") {
    ledcWrite(cFan, constrain(payload.toInt(),0,255)) ;

    Serial.println("incoming: " + topic + " - " + payload);
  } else if (topic == "/heat1") {
    ledcWrite(cHeat1, constrain(payload.toInt(),0,255));
  } else if (topic == "/heat2") {
    ledcWrite(cHeat2, constrain(payload.toInt(),0,255));
    Serial.println("incoming: " + topic + " - " + payload);
  }
  else if (topic == "/led") {
    ledcWrite(cLed, constrain(payload.toInt(),0,255));
    Serial.println("incoming: " + topic + " - " + payload);
  }
  else if (topic == "/start") {
    int commaIndex = payload.indexOf(',');
    String sHeat = payload.substring(0, commaIndex);
    String sFan = payload.substring(commaIndex + 1);

    int iHeat = map(constrain(sHeat.toInt(),0,100), 0, 100, 0, 255);
    int iFan = map(constrain(sFan.toInt(),0,100), 0, 100, 0, 255);
    status = 1;
    ledcWrite(cHeat1, iHeat);
    ledcWrite(cHeat2, iHeat);
    ledcWrite(cFan, iFan );
    Serial.println("incoming: " + topic + " - " + payload);
  }
  else {
    // Handle other topics
    // ...
  }
 
}

void setup() {
  Serial.begin(115200);
  ledcSetup(cLed, frequency, resolution);
  ledcSetup(cHeat2, frequency, resolution);
  ledcSetup(cHeat1, frequency, resolution);
  ledcSetup(cFan, frequency, resolution);
  ledcAttachPin(pFan, cFan);  // Attach pin 12 to LEDC channel 0
  ledcAttachPin(pHeat1, cHeat1);  // Attach pin 13 to LEDC channel 0
  ledcAttachPin(pHeat2, cHeat2);  // Attach pin 14 to LEDC channel 0
  ledcAttachPin(pLed, cLed); 

  // Note: Local domain names (e.g. "Computer.local" on OSX) are not supported
  // by Arduino. You need to set the IP address directly.
  client.begin("mustang.rmq.cloudamqp.com", net);
  client.onMessage(messageReceived);
  connectToWiFi();
  connect();
}

void loop() {
  client.loop();
  delay(10);  // <- fixes some issues with WiFi stability

  if (!client.connected()) {
    connect();
  }
  // publish a message roughly every second.
  if (millis() - lastMillis > 500) {
    if( status != 0)
    count++;
    float thermal = thermocouple.readCelsius()*1.055-4.22;
    String value = String(thermal) + "," + String(count);
    lastMillis = millis();
    client.publish("/temp", value,true,1);
    client.publish("/count",String(count),true,1);
  }
}
