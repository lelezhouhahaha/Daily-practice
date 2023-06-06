
DEVICE_FRAMEWORK_MANIFEST_FILE += vendor/vendorcode/vendor_framework_compatibility_matrix.xml
BOARD_SEPOLICY_DIRS += $(TOPDIR)vendor/vendorcode/sepolicy
PRODUCT_PACKAGES += vendor.scan.hardware.scanservice@1.0-service
PRODUCT_PACKAGES += android.hardware.oem.customizescanservice-service
PRODUCT_PACKAGES += vendor.oem.hardware.customizeoemservice@1.0-impl

