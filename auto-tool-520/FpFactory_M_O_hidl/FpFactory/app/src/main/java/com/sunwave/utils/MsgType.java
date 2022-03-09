package com.sunwave.utils;

/**
 * Created by yxf on 2018/5/28 0028.
 */

public class MsgType {
    public static final int FP_MSG_GET_COATING_FLAG = 599;//0x257;
    public static final int FP_MSG_GET_ID = 600;//0x258
    public static final int FP_MSG_GET_MODEL_ID = 601;//0x259

    public static final int FP_MSG_TEST_CMD_SENSITIVITY = 602;//0x25A
    public static final int FP_MSG_TEST_CMD_FP_LEAVE = 603;//0x25B
    public static final int FP_MSG_TEST_CMD_FP_TOUCH = 604;//0x25C

    public static final int FP_MSG_TEST_READ_FINGER   = 176;//0xB0
    public static final int FP_MSG_TEST_CHECK_CHIP   = 177;//0xB1
    public static final int FP_MSG_TEST_FACTORY_APK_ENTER = 208;//0xD0
    public static final int FP_MSG_TEST_FACTORY_APK_EXIT = 209;//0xD1

    public static final int FP_MSG_TEST_CMD_START_TEST_DATA = 701;//0x2BD
    public static final int FP_MSG_TEST_CMD_GET_TEST_DATA = 702;//0x2BE
    public static final int FP_MSG_TEST_CMD_START_TEST_SENSITIVITY = 703;//0x2BF
    public static final int FP_MSG_TEST_CMD_GET_SENSITIVITY_DATA = 704;//0x2C0
    public static final int FP_MSG_TEST_CMD_TOUCH_BASE = 705;//0x2C1
}
