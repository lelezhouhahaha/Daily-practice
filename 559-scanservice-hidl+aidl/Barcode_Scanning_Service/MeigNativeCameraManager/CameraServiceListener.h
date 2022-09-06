#include "CameraUtils.h"

class CameraServiceListener {
  public:

    static void onAvailable(void* obj, const char* cameraId);

    static void onUnavailable(void* obj, const char* cameraId);

    void resetCount();

    int getAvailableCount();

    int getUnavailableCount();

    bool isAvailable(const char* cameraId);

  private:
    std::mutex mMutex;
    int mOnAvailableCount = 0;
    int mOnUnavailableCount = 0;
    std::map<std::string, bool> mAvailableMap;
};

