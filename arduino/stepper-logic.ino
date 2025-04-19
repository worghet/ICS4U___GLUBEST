
// Import stepper library.
#include <Stepper.h>

// Setup constants.
const int STEPS_PER_REVOLUTION = 2048;
const int STEPPER_SPEED = 4;

// Declare Stepper.
Stepper stepper;

void setup() {

  // Open a serial to read from.
  Serial.begin(9600);

  // Initialize the Stepper object.
  stepper = Stepper(STEPS_PER_REVOLUTION, 8, 10, 9, 11);

  // Set the speed of the motor.
  stepper.setSpeed(STEPPER_SPEED);

}

void loop() {

  // Check if anything had been entered into the console.
  if (Serial.available()) {

    // Read the string sent by the Agent.
    String input = Serial.readString();

    // (Technically unnessicary) Ensure keyword matches requested one.
    if (input == "FEED") {
      stepper.step(STEPS_PER_REVOLUTION / 7);
    }
  }
}