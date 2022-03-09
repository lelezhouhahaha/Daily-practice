adb remount
adb shell rm /system/priv-app/SDLGui/*.apk
adb push build/outputs/apk/debug/app-debug.apk /system/priv-app/SDLGui/SDLGui.apk
adb push c:/Develop/DCC/Framework.SDL_standard.develop/project/Android/proj_BarCodeReaderSDL/libs/arm64-v8a/libbarcodereader80.so /system/lib64
adb push c:/Develop/DCC/Framework.SDL_standard.develop/project/Android/proj_BarCodeReaderSDL/libs/armeabi-v7a/libbarcodereader80.so /system/lib
adb push c:/Develop/DCC/Framework.SDL_standard.develop/project/Android/proj_IAL/libs/arm64-v8a/libIAL.so /system/lib64
adb push c:/Develop/DCC/Framework.SDL_standard.develop/project/Android/proj_IAL/libs/armeabi-v7a/libIAL.so /system/lib
adb push c:/Develop/DCC/Framework.SDL_standard.develop/project/Android/proj_SDL/libs/arm64-v8a/libSDL.so /system/lib64
adb push c:/Develop/DCC/Framework.SDL_standard.develop/project/Android/proj_SDL/libs/armeabi-v7a/libSDL.so /system/lib
#adb reboot
