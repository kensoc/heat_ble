#include <SPI.h>
#include <Ethernet.h>
#include <ble.h>

//TODO set the base station number here
// zero (0) or one (1)
#define STATION_NUMBER 0

#if STATION_NUMBER == 0
byte mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xE1 };
IPAddress ip(192, 168, 1, 201);
#elif STATION_NUMBER == 1
byte mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xE2 };
IPAddress ip(192, 168, 1, 202);
#endif

struct base_station {
  char address[20];
  float x, y;
  float n, a;
  int last_rssi;
};

#define NUM_BASE_STATION 2
struct base_station base_stations[NUM_BASE_STATION];

// Initialize the Ethernet server library
// with the IP address and port you want to use
// (port 80 is default for HTTP):
EthernetServer server(80);

#define LED  3
#define ETHCS  10
#define BLEREQN 9


float distance(const char *address, const int rssi) {
  int index;
  for (index = 0;index < NUM_BASE_STATION;index ++) {
    if (strncmp(base_stations[index].address, address, 18) == 0) {
      break;
    }
  }
  if (index < NUM_BASE_STATION) {
    return pow(10.0, -(base_stations[index].a+rssi)/base_stations[index].n/10.0);
  } else {
    return 0;
  }
}


void setup() {
  Serial.begin(9600);
  pinMode(LED, OUTPUT);
  pinMode(ETHCS, OUTPUT);
  pinMode(BLEREQN, OUTPUT);
  pinMode(SS, OUTPUT);
  
  digitalWrite(LED, LOW);
  digitalWrite(ETHCS, HIGH);
  digitalWrite(BLEREQN, HIGH);
  digitalWrite(SS, HIGH);

  SPI.begin();
  SPI.setDataMode(SPI_MODE0);
  
  SPI.setBitOrder(LSBFIRST);
  SPI.setClockDivider(SPI_CLOCK_DIV16);
  ble_begin();  
  digitalWrite(BLEREQN, HIGH);
  digitalWrite(ETHCS, HIGH);
  delay(50);
  
  digitalWrite(ETHCS, LOW);
  SPI.setBitOrder(MSBFIRST);
  SPI.setClockDivider(SPI_CLOCK_DIV4);
  Ethernet.begin(mac, ip);
  server.begin();
  Serial.print("Web server IP:");
  Serial.println(Ethernet.localIP());
  digitalWrite(ETHCS, HIGH);
  digitalWrite(BLEREQN, HIGH);
  delay(50);
  
  Serial.print("This is station #");
  Serial.print(STATION_NUMBER);
  Serial.print("\n");
}

void do_ble_loop() {
  SPI.setBitOrder(LSBFIRST);
  SPI.setClockDivider(SPI_CLOCK_DIV16);
  delay(1);
  ble_do_events();
}

void do_web_loop() {
  SPI.setBitOrder(MSBFIRST);
  SPI.setClockDivider(SPI_CLOCK_DIV4);
  delay(1);
  // listen for incoming clients
  EthernetClient client = server.available();
  if (client) {
    Serial.println("new client");
    digitalWrite(LED, HIGH);
    // an http request ends with a blank line
    boolean currentLineIsBlank = true;
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        Serial.write(c);
        // if you've gotten to the end of the line (received a newline
        // character) and the line is blank, the http request has ended,
        // so you can send a reply
        if (c == '\n' && currentLineIsBlank) {
          // send a standard http response header
          client.println("HTTP/1.1 200 OK");
          client.println("Connection: close");
          client.println();
          break;
        }
        if (c == '\n') {
          // you're starting a new line
          currentLineIsBlank = true;
        }
        else if (c != '\r') {
          // you've gotten a character on the current line
          currentLineIsBlank = false;
        }
      }
    }
    // give the web browser time to receive the data
    delay(1);
    // close the connection:
    client.stop();
    Serial.println("client disconnected");
    digitalWrite(LED, LOW);
  }
}

void loop() {
  digitalWrite(BLEREQN, HIGH);
  digitalWrite(ETHCS, HIGH);
  delay(1);
  
  digitalWrite(ETHCS, LOW);
  do_web_loop();
  delay(5);
  digitalWrite(ETHCS, HIGH);
  digitalWrite(BLEREQN, HIGH);
  delay(1);

  digitalWrite(ETHCS, HIGH);  
  digitalWrite(BLEREQN, LOW);
  do_ble_loop();
  delay(5);
  digitalWrite(BLEREQN, HIGH);
  digitalWrite(ETHCS, HIGH);
  delay(1);
}

