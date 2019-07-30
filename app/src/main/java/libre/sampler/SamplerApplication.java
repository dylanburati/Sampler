package libre.sampler;

import android.app.Application;

public class SamplerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // if(BuildConfig.DEBUG) {
        //     LeakCanary.Config config = LeakCanary.INSTANCE.getConfig();
        //     LeakCanary.Config altConfig = config.copy(config.getDumpHeap(),
        //             config.getDumpHeapWhenDebugging(),
        //             5,
        //             config.getExclusionsFactory(),
        //             config.getComputeRetainedHeapSize(),
        //             config.getLeakInspectors(),
        //             config.getLabelers(),
        //             config.getAnalysisResultListener());
        //     LeakCanary.INSTANCE.setConfig(altConfig);
        // }
    }
}