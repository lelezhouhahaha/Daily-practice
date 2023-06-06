// FIXME: license file if you have one

package android.hardware.oem.customizescanservice;
import android.hardware.oem.customizescanservice.Reg_value;

@VintfStability
parcelable Reg_data {
    int addr_type;
    int data_type;
    Reg_value[] regValueArray;
}

