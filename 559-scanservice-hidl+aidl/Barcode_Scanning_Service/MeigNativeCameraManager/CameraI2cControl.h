
enum camera_sensor_i2c_cmd_info {
	CAMERA_STREAMON_CMD,
	CAMERA_STREAMOFF_CMD,
	CAMERA_SENSOR_CMD_MAX,
};

typedef struct {
	int32_t PreExposureTime;
	int32_t CurExppsureTime;
	int32_t PreGain;
	int32_t CurGain;
	int32_t FrameNum;
} SensorExposureTimeInfo_t;

bool OpenSensorI2cDevice(void);
void CloseSensorI2cDevice(void);
int CameraSensorI2CCfg(int32_t CameraId,int cmd);
int SetSensorExposureTime(int cameraId,int32_t time);
int SetSensorGain(int cameraId,int32_t gain);
int GetSensorFrameNum(int cameraId);
int SensorI2cCheckTest(int cameraId);