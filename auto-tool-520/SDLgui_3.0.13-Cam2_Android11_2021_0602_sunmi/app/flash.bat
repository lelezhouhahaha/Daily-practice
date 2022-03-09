adb reboot bootloader
sleep 1
fastboot flash boot_a boot.img
fastboot flash dtbo_a dt.img
fastboot flash vendor_a vendor.img
fastboot flash system_a system.img
sleep 1
fastboot reboot
