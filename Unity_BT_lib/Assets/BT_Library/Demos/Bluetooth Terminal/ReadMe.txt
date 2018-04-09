
In this demo the user will be able to select Bluetooth devices from a list and connect.  

Note : TerminalController.cs script is already attached to the 'InfoController' gameobject in the scene.  

Note : In this demo, and other terminal like demos (High Bit Rate Demo, Server-client and the JoyStick demo) the logs are being printed on a 'Text' element of the new Unity UI system   
 While the new UI system is all nice and fancy, using the old OnGUI system for loggin text would be better.    
 The new UI system has been built to exploit the fact that UI will go unchanged for a period of time, while here,   
 we are loggin data continuously every frame. The old OnGUI system is optemized to be updated continiously during the game life cycle.

