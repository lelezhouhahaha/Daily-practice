typedef enum {
	LASERAIMING_OFF = -1,
	LASERAIMING_0N,
	LIM_OUT_ON,
	LIM_OUT_OFF,
	AIM_SUS_LOW,
	AIM_SUS_HIGH,
}led_state_t;

void SanLightInit(void);
void Set_LaserAiming(bool on);
void Set_AIM_SUS(bool high);
void Set_ILM_OUT(bool on);

