package com.arjanvlek.oxygenupdater.support;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;

import org.joda.time.LocalDateTime;
import org.json.JSONObject;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class LoggerService extends IntentService {

    public static String TAG = "LoggerService";


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public LoggerService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String logLevel = intent.getStringExtra("event_type");
        String tag = intent.getStringExtra("tag");
        String message = intent.getStringExtra("message");

        ApplicationData applicationData = (ApplicationData) getApplication();

        SettingsManager settingsManager = new SettingsManager(applicationData);

        ServerConnector serverConnector = applicationData.getServerConnector().clone();
        serverConnector.setUploadLog(false);

        SystemVersionProperties systemVersionProperties = new SystemVersionProperties(false);

        serverConnector.getDevices(devices -> {
            boolean deviceIsSupported = SupportedDeviceManager.isSupportedDevice(systemVersionProperties, devices);

            try {
                JSONObject logData = new JSONObject();
                logData.put("event_type", logLevel);
                logData.put("device_is_supported", deviceIsSupported);
                logData.put("device_id", settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L));
                logData.put("update_method_id", settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L));
                logData.put("device_name", Build.BRAND + " " + Build.PRODUCT + " (" + Build.BOARD + ")");
                logData.put("operating_system_version", !systemVersionProperties.getOxygenOSVersion().equals(NO_OXYGEN_OS) ? systemVersionProperties.getOxygenOSVersion() : Build.VERSION.RELEASE);
                logData.put("error_message", tag + " : " + message);
                logData.put("app_version", BuildConfig.VERSION_NAME);
                logData.put("event_date", LocalDateTime.now().toString());
                serverConnector.log(logData, (logResult) -> {
                    if (logResult != null && !logResult.isSuccess()) {
                        Logger.logError(false, TAG, "Error uploading log to server:" + logResult.getErrorMessage());
                    }
                });
            } catch (Exception e) {
                Logger.logError(false, TAG, "Error preparing log data for uploading to the server:", e);
            }
        });

    }
}
