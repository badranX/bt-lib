All other demos, reads one line per frame. This demo reads all available packets/lines per frame, which gives a higher bit rate.

Note : the Hightest rate possible is by avoiding using setEndByte(). So that BluetoothDevice.read() will read all available data whenever called, then you have to sort your data (either as lines or whatever packetization you want)!.

Note : 'HighRateTerminal.cs' script is already attached to the 'InfoController' gameobject in the scene.
