void setup() {
  Serial.begin(9600);
  pinMode(12, OUTPUT);

}

void loop() {
  byte blinks;

  if (Serial.available()) {
     blinks = Serial.read();

     for (byte i = 1; i <= 2*blinks; i++) {
        digitalWrite(12, !digitalRead(12));
        delay(200);
     }
  }
}