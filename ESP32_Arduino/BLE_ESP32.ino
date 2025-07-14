#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID "dc94bd87-6388-4a52-a158-804e405df089"
#define CHARACTERISTIC_UUID "2ec0cef9-9e97-48a5-8d92-07bb60c0eae6"
#define LED 2

bool deviceConnected = false;
unsigned int previous_ms = 0;
const int interval = 500;

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *pServer) {
    deviceConnected = true;
    Serial.println("Client conectat!");
    Serial.println("--------------------");
  }

  void onDisconnect(BLEServer *pServer) {
    deviceConnected = false;
    Serial.println("--------------------");
    Serial.println("Client deconectat!");
  }
};

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String rxValue = String(pCharacteristic->getValue().c_str());

    if (rxValue.length() > 0) {


      int rssiIndex = rxValue.indexOf("RSSI:");
      int distIndex = rxValue.indexOf("Dist.:");

      if (rssiIndex != -1 && distIndex != -1) {
        String rssiStr = rxValue.substring(rssiIndex + 5, rxValue.indexOf(",", rssiIndex));
        String distStr = rxValue.substring(distIndex + 6);

        Serial.print("RSSI (dB): ");
        Serial.print(rssiStr);

        Serial.print("    Distanța estimată (m): ");
        Serial.println(distStr);
      }
    }
  }
};

void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);
  delay(1000);
  Serial.println("Pornire ESP32 BLE...");

  BLEDevice::init("ESP32_BLE");

  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);

  pCharacteristic->setCallbacks(new MyCallbacks());
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->start();

  Serial.println("BLE server pornit. Astept conexiuni...");
}

void loop() {
  unsigned int current_ms = millis();

  if (deviceConnected) {
    digitalWrite(LED, HIGH);
  } else {
    if (current_ms - previous_ms >= interval) {
      previous_ms = current_ms;
      digitalWrite(LED, !digitalRead(LED));
    }
  }
}