// SEServiceInterface.aidl
package cn.hsa.ctp.device.sdk.SEService;

// Declare any non-default types here with import statements

interface SEServiceInterface {
    // 写入时密钥数据类型 key type
    const int SM2_PUBLIC_KEY = 1;
    const int SM2_PRIVATE_KEY = 2;
    const int RSA1024_PUBLIC_KEY = 3;
    const int RSA1024_PRIVATE_KEY = 4;
    const int RSA2048_PUBLIC_KEY = 5;
    const int RSA2048_PRIVATE_KEY = 6;
    const int SM4_KEY = 7;
    const int SSF33_KEY = 8;
    const int DES_KEY = 9;
    const int AES_KEY = 10;

    // 哈希摘要类型 hash digest type
    const int DT_SM3 = 0;
    const int DT_SHA256 = 1;

    // 对称算法类型 symmetric algorithm type
    const int ST_SM4_ECB = 0;
    const int ST_DES_ECB = 1;
    const int ST_AES_ECB = 2;
    const int ST_SM4_CBC = 3;
    const int ST_DES_CBC = 4;
    const int ST_AES_CBC = 5;

    // 非对称算法类型 asymmetric algorithm type
    const int AT_SM2 = 6;
    const int AT_RSA1024 = 7;
    const int AT_RSA2048 = 8;


    String getVersion();
    int getLastErrorCode();

    int setDigitalCert(int index, in byte[] cert_info);
    byte[] getDigitalCert(int index);
    int generateKeyPair(int key_position, int key_type);
    int writeKey(int key_position, int key_type, in byte[] key_data);
    byte[] getPublicKey(int key_position, int key_type);
    byte[] getRandomNumber(int length);

    byte[] computeHashValue(int hash_type, in byte[] input_data);
    byte[] computeSM3HashWithID(int key_position, in byte[] input_data, in byte[] user_id);
    byte[] computeSignature(int key_position, int key_type, in byte[] hash_value);
    int verifySignature(int key_position, int key_type, in byte[] hash_value, in byte[] sign_value);

    byte[] computeMACValue(int key_position, in byte[] plain_data, in byte[] init_vector);
    Bundle encryptData(int key_position, int key_type, in byte[] plain_data);
    Bundle decryptData(int key_position, int key_type, in byte[] cipher_data);

    int openPreventDisassembly();
    int getDisassemblyStatus();

    int openPreventWrite();

    int setUserData(int start, int length, in byte[] data);
    byte[] getUserData(int start, int length);

    int getChipID(inout byte[] data, int data_size);
    int getCompanyName(inout byte[] data, int data_size);

    int clearData();
}
