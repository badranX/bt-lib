###### This is the source for the Asset "Android & Microcontrollers / Bluetooth" on the Unity Asset Store.
###### [Assetstore link](http://u3d.as/78c) 
###### [Docs link](https://techtweaking.github.io/docs/) (java souce code is still not documented) 

## Installation

1) If you just want to use the unity Project, then just open the Unity_BT_lib folder in your Unity Editor!.

2) If you need to change the java code, then read the following:

The repository contains two folders, one an AndroidStudio project and the other is a Unity Project. In the AndroidStudio project you need to run the task doRelease/doDebug from gradle. It will compile and move the required files to the Unity project.

**On Windows:**
gradlew task-name

**On Mac or Linux:**
./gradlew task-name

Read: https://developer.android.com/studio/build/building-cmdline

## Thanks
IOUtils file and the new gradle build method (previously python!) was first added by [Jerome Lacoste](https://github.com/lacostej).
