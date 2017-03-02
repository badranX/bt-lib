int joyPin1 = 0;                 // slider variable connecetd to analog pin 0 
int joyPin2 = 1;                 // slider variable connecetd to analog pin 1 
int x1 = 0;                  // variable to read the value from the analog pin 0 
int x2 = 0;                  // variable to read the value from the analog pin 1 

byte buf[5] ;

void setup() { 
  //the last byte equals 255, we will use it as a seperator between packets
  buf[4] = 255;

  Serial.begin(9600); 
} 
 
int fillBuffer(int x1, int x2) { 
  /* We want to make the byte 255 (11111111) a seperator between packets
   * So 255 must never transmited as an AnalogRead.
   * We will exploite the fact that x1 and x2 are 0 to 1023.
   * 0 to 1023 needs 10 bits because of (2^10 = 1024)
   * 
   * First Byte  | Second Byte
   * xx xx xx xx | xx 00 00 00 
   * 
   * where x is a used bit
   * 
   * by shifting 3 bits
   * 
   * First Byte  | Second Byte
   * 00 0x xx xx | xx xx x0 00 
   * 
   * Now it's impposible to have (11111111) or (255)
   */
   
  
  x1 = x1 << 3; 
  x2 = x2 << 3;


  buf[0] = lowByte(x1);
  buf[1] = highByte(x1);
  buf[2] = lowByte(x2);
  buf[3] = highByte(x2);
  
} 
 
void loop() { 
  // reads the value of the variable resistor 
  x1 = analogRead(joyPin1);   
  // this small pause is needed between reading 
  // analog pins, otherwise we get the same value twice 
  delay(100);             
  // reads the value of the variable resistor 
  x2 = analogRead(joyPin2);   

  fillBuffer(x1,x2);
  Serial.write(buf,5);

  delay(100); 
} 

