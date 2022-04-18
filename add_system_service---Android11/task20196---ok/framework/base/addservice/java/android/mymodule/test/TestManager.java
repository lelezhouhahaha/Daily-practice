package android.mymodule.test;
import android.util.Slog;
import android.os.RemoteException;
import android.mymodule.test.ITestManager;


public class TestManager {
    private final ITestManager mService;

	/** @hide */
    public TestManager(ITestManager mService) {
        this.mService = mService;
	}

	/** @hide */
    public void testMethod() {
        try {
            mService.testMethod();
            Slog.i("add_service_test", "TestManager testMethod");
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }        
    }
}
