// FIXME: license file if you have one

package android.hardware.oem.customizescanservice;
import android.hardware.oem.customizescanservice.ICustomizeScanServiceCallback;
import android.hardware.oem.customizescanservice.Reg_data;
//import android.os.ParcelFileDescriptor;

@VintfStability
interface ICustomizeScanService {
    // Adding return type to method instead of out param int ret since there is only one return value.
    void customize_setCallback(in ICustomizeScanServiceCallback callback);

	/**
	 * description: open three cameras(two scms and one ccm)
	 * return:the result of opening camera
	 **/
    int customize_cam_open();

    /**
	 * description: close three cameras(two scms and one ccm)
	 * return:the result of close camera
	 **/
    int customize_cam_close();

    /**
	 * description: suspend three cameras(two scms and one ccm)
	 * return:the result of suspend camera
	 **/
    int customize_cam_suspend();

    /**
	 * description: resume three cameras(two scms and one ccm)
	 * return:the result of resume camera
	 **/
    int customize_cam_resume();

	/**
	 * description: Execute scm capture
	 * @target_cam: byte Select the target camera to capture image.
	 *              0: no scm; 1: scm1; 2: scm2; 3: scm1 and scm2
	 * return: the result of scm capturing
	 **/
    int customize_cam_scm_capture(in byte target_cam);

	/**
	 * description: Execute ccm capture
	 * @aimer_suspend: 
	 *          0: not control AIM_SUS GPIO during capture.
	 *          1: control AIM_SUS GPIO during capture. Set GPIO as L before starting capture, and set GPIO as H after end of capture .
	 * return: the result of ccm capturing
	 **/
    int customize_cam_ccm_capture(in byte aimer_suspend);

	/**
	 * description: Send more i2c commands to MCU at once.
	 * @SendData: the content of data which need to be sent.
	 * @length: the size of Reg_value in SendData
	 * return: the result of writing mcu by i2c
	 **/
    int customize_cam_scm_mcu_i2c_write(in Reg_data SendData, in int length);

    /**
	 * description: Send more i2c commands to SCM at once
	 * @slaveAddress: the slave address of scm which is written by i2c
	 * @SendData: the content of data which need to be sent.
	 * @length: the size of Reg_value in SendData
	 * return: the result of writing scm by i2c
	 **/
    int customize_cam_scm_i2c_write(in byte slaveAddress, in Reg_data SendData, in int length);
    
    /**
	 * description: Send more i2c commands to ccm at once.
	 * @SendData: the pointer of data which need to be sent.
	 * @length: the size of SendData
	 * return: the result of writing mcu by i2c
	 **/
    int customize_cam_ccm_i2c_write(in Reg_data SendData, in int length);

    /**
	 * description: Read register value from scm by i2c
	 * @slaveAddress: the slave address of scm which is read by i2c
	 * @SendData: the address of register which is read by i2c
	 * @ResData: the value is read from SendData by i2c
	 * @AddrType: the type of register address
	 * @DataType: the type of register value
	 * return: the result of reading scm by i2c
	 **/
    int customize_cam_scm_i2c_read(in byte slaveAddress, in int SendData, in int AddrType, in int DataType);

	/**
	 * description: Read register value from ccm by i2c
	 * @SendData: the address of register which is read by i2c
	 * @AddrType: the type of register address
	 * @DataType: the type of register value
	 * return: the result of reading ccm by i2c
	 **/
    int customize_cam_ccm_i2c_read(in int SendData, in int AddrType, in int DataType);

	/**
	 * description: Release the memory of target buffer from ccm
	 * @idx: the idx is the index of one image buffer from ccm
	 * return: judge the status of struct cameramanager--0: normal, -1:error
	 **/
    int customize_cam_ccm_return_buffer(in int idx);

    /**
	 * description: Release the memory of target buffer from scm1
	 * @idx: the idx is the index of one image buffer from scm1
	 * return: judge the status of struct cameramanager--0: normal, -1:error
	 **/
    int customize_cam_scm1_return_buffer(in int idx);

    /**
	 * description: Release the memory of target buffer from scm2
	 * @idx: the idx is the index of one image buffer from scm2
	 * return: judge the status of struct cameramanager--0: normal, -1:error
	 **/
    int customize_cam_scm2_return_buffer(in int idx);

    /**
	 * description: Read serial number of ccm
	 * return: the result of ccm_serial_number_read
	 **/
    @utf8InCpp String  customize_cam_ccm_serial_number_read();

	/**
	 * description: Read serial number of MCU
	 * return: the result of mcu_serial_number_read
	 **/
    @utf8InCpp String  customize_cam_scm_serial_number_read();

	/**
	 * description: Read FW version of MCU
	 * @serial: Pointer of char allay to store version
	 * return: the result of mcu_fw_version
	 **/
    @utf8InCpp String  customize_cam_scm_fw_version_read();

    /**
	 * description: move VCM of CCM to target focus distance
	 * @distancemm: Focus distance of CCM
	 * return: the result of Move Lens
	 **/
    int customize_cam_ccm_move_Focus(in int distancemm);

    /**
	 * description: flash at timing of comannd send. asynchronous with caputure
	 * @flash_status: flash_status--0:disable flash light; 1:enable flash light(weak); 2:enable flash light(strong) 
	 * @timeout: Make flash ON for the priod of "timeout". If "0" is set, keep flash ON.
	 * return: the result of Controlling flash
	 **/
    int customize_cam_ccm_flash(in int flash_status, in int timeout);

    /**
	 * description: Switch the resolution of ccm to type
	 * @type: 0: size 1M; 1:size 13M 
	 * return: the result of Controlling flash
	 **/
    int customize_cam_ccm_switch_size(in int type);

    /**
	 * description: Control AIM_SUS GPIO to force Aimer Off.
	 * @gpio_signal: 0: set AIM_SUS GPIO as H 1: set AIM_SUS GPIO as L
	 * return: the result of Controlling AIM_SUS GPIO
	 **/
    int customize_cam_aim_sus(in byte gpio_signal);

    /**
	 * description: Control WAKE GPIO to wake up module from deep sleep.
	 * @gpio_signal: 0: set WAKE GPIO as H 1: set WAKE GPIO as L
	 * return: the result of Controlling WAKE GPIO
	 **/
    int customize_cam_wake(in byte gpio_signal);

	/**
	 * description: Store char array data into non volatil area which is not cleared by factory reset.
	 * @param: char array data needs to be stored
	 * return: error code
	 **/
	int customize_non_volatail_param_write(@utf8InCpp String param);

	/**
	 * description: Read char array data from non volatil area.
	 * @param: Read char array data to param
	 * return: error code
	 **/
	@utf8InCpp String  customize_non_volatail_param_read();
	
	/**
	 * description: open quad camera
	 * return: the result of opening camera
	 **/
	int customize_quad_cam_open();

	/**
	 * description: close quad camera
	 * return: the result of closing camera
	 **/
	int customize_quad_cam_close();

	/**
	 * description: suspend quad cameras
	 * return:the result of suspend camera
	 **/
	int customize_quad_cam_suspend();

	/**
	 * description: resume quad cameras
	 * return:the result of resume camera
	 **/
	int customize_quad_cam_resume();

	/**
	 * description: capture quad cameras
	 * return:the result of capture camera, and the gettig camera data through callback.
	 **/
	int customize_quad_cam_capture();
	
    /**
	 * description: Send more i2c commands to quad scm at once
	 * @slaveAddress: the slave address of quad scm which is written by i2c
	 * @SendData: the content of data which need to be sent.
	 * @length: the size of Reg_value in SendData
	 * return: the result of writing scm by i2c
	 **/
	int customize_quad_cam_scm_i2c_write(in byte slaveAddress,in Reg_data SendData,in int length);

    /**
	 * description: Read register value from quad scm by i2c
	 * @slaveAddress: the slave address of quad scm which is read by i2c
	 * @SendData: the address of register which is read by i2c
	 * @ResData: the value is read from SendData by i2c
	 * @AddrType: the type of register address
	 * @DataType: the type of register value
	 * return: the result of reading scm by i2c
	 **/
	int customize_quad_cam_scm_i2c_read(in byte slaveAddress,in int SendData,in int AddrType,in int DataType);

    /**
	 * description: Release the memory of target buffer from quad scm
	 * @idx: the idx is the index of one image buffer from quad scm
	 * return: judge the status of struct cameramanager--0: normal, -1:error
	 **/
	int customize_quad_cam_scm_return_buffer(in int idx);
}