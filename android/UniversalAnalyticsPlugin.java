package com.danielcwilson.plugins.analytics;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;

// Para importar las librerias GTM
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.TagManager;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container.FunctionCallMacroCallback;
import com.google.android.gms.tagmanager.Container.FunctionCallTagCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import java.util.concurrent.TimeUnit;
import android.util.Log;
import com.google.android.gms.tagmanager.DataLayer;
import android.os.Handler;
import android.content.Context;


public class UniversalAnalyticsPlugin extends CordovaPlugin {
    public static final String START_TRACKER = "startTrackerWithId";
    public static final String TRACK_VIEW = "trackView";
    public static final String TRACK_EVENT = "trackEvent";
    public static final String TRACK_EXCEPTION = "trackException";
    public static final String TRACK_TIMING = "trackTiming";
    public static final String TRACK_METRIC = "trackMetric";
    public static final String ADD_DIMENSION = "addCustomDimension";
    public static final String ADD_TRANSACTION = "addTransaction";
    public static final String ADD_TRANSACTION_ITEM = "addTransactionItem";

    public static final String SET_ALLOW_IDFA_COLLECTION = "setAllowIDFACollection";
    public static final String SET_USER_ID = "setUserId";
    public static final String SET_ANONYMIZE_IP = "setAnonymizeIp";
    public static final String SET_APP_VERSION = "setAppVersion";
    public static final String DEBUG_MODE = "debugMode";
    public static final String ENABLE_UNCAUGHT_EXCEPTION_REPORTING = "enableUncaughtExceptionReporting";

    public static final String TAG_MANAGER_INIT = "initTagManager";
    public static final String TAG_MANAGER_PUSH = "pushTagManager";
    public static final String TAG_MANAGER_PUSH_EVENT = "pushEventTagManager";
    public static final String TAG_MANAGER_PUSH_SCREEN = "pushScreenTagManager";

    public Boolean trackerStarted = false;
    public Boolean debugModeEnabled = false;
    public HashMap<Integer, String> customDimensions = new HashMap<Integer, String>();

    public Tracker tracker;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (START_TRACKER.equals(action)) {
            String id = args.getString(0);
            this.startTracker(id, callbackContext);
            return true;
        } else if (TRACK_VIEW.equals(action)) {
            int length = args.length();
            String screen = args.getString(0);
            this.trackView(screen, length > 1 ? args.getString(1) : "", callbackContext);
            return true;
        } else if (TRACK_EVENT.equals(action)) {
            int length = args.length();
            if (length > 0) {
                this.trackEvent(
                        args.getString(0),
                        length > 1 ? args.getString(1) : "",
                        length > 2 ? args.getString(2) : "",
                        length > 3 ? args.getLong(3) : 0,
                        callbackContext);
            }
            return true;
        } else if (TRACK_EXCEPTION.equals(action)) {
            String description = args.getString(0);
            Boolean fatal = args.getBoolean(1);
            this.trackException(description, fatal, callbackContext);
            return true;
        } else if (TRACK_TIMING.equals(action)) {
            int length = args.length();
            if (length > 0) {
                this.trackTiming(args.getString(0), length > 1 ? args.getLong(1) : 0, length > 2 ? args.getString(2) : "", length > 3 ? args.getString(3) : "", callbackContext);
            }
            return true;
        } else if (TRACK_METRIC.equals(action)) {
            int length = args.length();
            if (length > 0) {
                this.trackMetric(args.getInt(0), length > 1 ? args.getString(1) : "", callbackContext);
            }
            return true;
        } else if (ADD_DIMENSION.equals(action)) {
            Integer key = args.getInt(0);
            String value = args.getString(1);
            this.addCustomDimension(key, value, callbackContext);
            return true;
        } else if (ADD_TRANSACTION.equals(action)) {
            int length = args.length();
            if (length > 0) {
                this.addTransaction(
                        args.getString(0),
                        length > 1 ? args.getString(1) : "",
                        length > 2 ? args.getDouble(2) : 0,
                        length > 3 ? args.getDouble(3) : 0,
                        length > 4 ? args.getDouble(4) : 0,
                        length > 5 ? args.getString(5) : null,
                        callbackContext);
            }
            return true;
        } else if (ADD_TRANSACTION_ITEM.equals(action)) {
            int length = args.length();
            if (length > 0) {
                this.addTransactionItem(
                        args.getString(0),
                        length > 1 ? args.getString(1) : "",
                        length > 2 ? args.getString(2) : "",
                        length > 3 ? args.getString(3) : "",
                        length > 4 ? args.getDouble(4) : 0,
                        length > 5 ? args.getLong(5) : 0,
                        length > 6 ? args.getString(6) : null,
                        callbackContext);
            }
            return true;
        } else if (SET_ALLOW_IDFA_COLLECTION.equals(action)) {
            this.setAllowIDFACollection(args.getBoolean(0), callbackContext);
        } else if (SET_USER_ID.equals(action)) {
            String userId = args.getString(0);
            this.setUserId(userId, callbackContext);
        } else if (SET_ANONYMIZE_IP.equals(action)) {
            boolean anonymize = args.getBoolean(0);
            this.setAnonymizeIp(anonymize, callbackContext);
        } else if (SET_APP_VERSION.equals(action)) {
            String version = args.getString(0);
            this.setAppVersion(version, callbackContext);
        } else if (DEBUG_MODE.equals(action)) {
            this.debugMode(callbackContext);
        } else if (ENABLE_UNCAUGHT_EXCEPTION_REPORTING.equals(action)) {
            Boolean enable = args.getBoolean(0);
            this.enableUncaughtExceptionReporting(enable, callbackContext);
        } else if (TAG_MANAGER_INIT.equals(action)) {
            String containerId = args.getString(0);
            this.initTagManager(containerId, callbackContext);
            return true;
        } else if (TAG_MANAGER_PUSH_SCREEN.equals(action)) {
            System.out.println("[TAG_MANAGER] TAG_MANAGER_PUSH_SCREEN : " + args);
            String screenName = args.getString(0);
            this.pushScreen(screenName, callbackContext);
            return true;
        } else if (TAG_MANAGER_PUSH_EVENT.equals(action)) {
            String eventCategory = args.getString(0);
            String eventAction = args.getString(1);
            String eventLabel = args.getString(2);
            this.pushEvent(eventCategory, eventAction, eventLabel, callbackContext);
            return true;
        } else if (TAG_MANAGER_PUSH.equals(action)) {
            String key = args.getString(0);
            String value = args.getString(1);
            this.push(key, value, callbackContext);
            return true;
        }
        return false;
    }

    private void startTracker(String id, CallbackContext callbackContext) {
        if (null != id && id.length() > 0) {
            tracker = GoogleAnalytics.getInstance(this.cordova.getActivity()).newTracker(id);
            callbackContext.success("tracker started");
            trackerStarted = true;
            GoogleAnalytics.getInstance(this.cordova.getActivity()).setLocalDispatchPeriod(30);
        } else {
            callbackContext.error("tracker id is not valid");
        }
    }

    private void addCustomDimension(Integer key, String value, CallbackContext callbackContext) {
        if (key <= 0) {
            callbackContext.error("Expected positive integer argument for key.");
            return;
        }

        if (null == value || value.length() == 0) {
            callbackContext.error("Expected non-empty string argument for value.");
            return;
        }

        customDimensions.put(key, value);
        callbackContext.success("custom dimension started");
    }

    private <T> void addCustomDimensionsToHitBuilder(T builder) {
        //unfortunately the base HitBuilders.HitBuilder class is not public, therefore have to use reflection to use
        //the common setCustomDimension (int index, String dimension) method
        try {
            Method builderMethod = builder.getClass().getMethod("setCustomDimension", Integer.TYPE, String.class);

            for (Entry<Integer, String> entry : customDimensions.entrySet()) {
                Integer key = entry.getKey();
                String value = entry.getValue();
                try {
                    builderMethod.invoke(builder, (key), value);
                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
            }
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    private void trackView(String screenname, String campaignUrl, CallbackContext callbackContext) {
        if (! trackerStarted ) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (null != screenname && screenname.length() > 0) {
            tracker.setScreenName(screenname);

            HitBuilders.ScreenViewBuilder hitBuilder = new HitBuilders.ScreenViewBuilder();
            addCustomDimensionsToHitBuilder(hitBuilder);

            if(!campaignUrl.equals("")){
                hitBuilder.setCampaignParamsFromUrl(campaignUrl);
            }

            tracker.send(hitBuilder.build());
            callbackContext.success("Track Screen: " + screenname);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void trackEvent(String category, String action, String label, long value, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (null != category && category.length() > 0) {
            HitBuilders.EventBuilder hitBuilder = new HitBuilders.EventBuilder();
            addCustomDimensionsToHitBuilder(hitBuilder);

            tracker.send(hitBuilder
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(value)
                    .build()
            );
            callbackContext.success("Track Event: " + category);
        } else {
            callbackContext.error("Expected non-empty string arguments.");
        }
    }

    private void trackMetric(Integer key, String value, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (key >= 0) {
            HitBuilders.ScreenViewBuilder hitBuilder = new HitBuilders.ScreenViewBuilder();
            tracker.send(hitBuilder
                    .setCustomMetric(key, Float.parseFloat(value))
                    .build()
            );
            callbackContext.success("Track Metric: " + key + ", value: " + value);
        } else {
            callbackContext.error("Expected integer key: " + key + ", and string value: " + value);
        }
    }

    private void trackException(String description, Boolean fatal, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (null != description && description.length() > 0) {
            HitBuilders.ExceptionBuilder hitBuilder = new HitBuilders.ExceptionBuilder();
            addCustomDimensionsToHitBuilder(hitBuilder);

            tracker.send(hitBuilder
                    .setDescription(description)
                    .setFatal(fatal)
                    .build()
            );
            callbackContext.success("Track Exception: " + description);
        } else {
            callbackContext.error("Expected non-empty string arguments.");
        }
    }

    private void trackTiming(String category, long intervalInMilliseconds, String name, String label, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (null != category && category.length() > 0) {
            HitBuilders.TimingBuilder hitBuilder = new HitBuilders.TimingBuilder();
            addCustomDimensionsToHitBuilder(hitBuilder);

            tracker.send(hitBuilder
                    .setCategory(category)
                    .setValue(intervalInMilliseconds)
                    .setVariable(name)
                    .setLabel(label)
                    .build()
            );
            callbackContext.success("Track Timing: " + category);
        } else {
            callbackContext.error("Expected non-empty string arguments.");
        }
    }

    private void addTransaction(String id, String affiliation, double revenue, double tax, double shipping, String currencyCode, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (null != id && id.length() > 0) {
            HitBuilders.TransactionBuilder hitBuilder = new HitBuilders.TransactionBuilder();
            addCustomDimensionsToHitBuilder(hitBuilder);

            tracker.send(hitBuilder
                    .setTransactionId(id)
                    .setAffiliation(affiliation)
                    .setRevenue(revenue).setTax(tax)
                    .setShipping(shipping)
                    .setCurrencyCode(currencyCode)
                    .build()
            ); //Deprecated
            callbackContext.success("Add Transaction: " + id);
        } else {
            callbackContext.error("Expected non-empty ID.");
        }
    }

    private void addTransactionItem(String id, String name, String sku, String category, double price, long quantity, String currencyCode, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        if (null != id && id.length() > 0) {
            HitBuilders.ItemBuilder hitBuilder = new HitBuilders.ItemBuilder();
            addCustomDimensionsToHitBuilder(hitBuilder);

            tracker.send(hitBuilder
                    .setTransactionId(id)
                    .setName(name)
                    .setSku(sku)
                    .setCategory(category)
                    .setPrice(price)
                    .setQuantity(quantity)
                    .setCurrencyCode(currencyCode)
                    .build()
            ); //Deprecated
            callbackContext.success("Add Transaction Item: " + id);
        } else {
            callbackContext.error("Expected non-empty ID.");
        }
    }

    private void setAllowIDFACollection(Boolean enable, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        tracker.enableAdvertisingIdCollection(enable);
        callbackContext.success("Enable Advertising Id Collection: " + enable);
    }

    private void debugMode(CallbackContext callbackContext) {
        GoogleAnalytics.getInstance(this.cordova.getActivity()).getLogger().setLogLevel(LogLevel.VERBOSE);

        this.debugModeEnabled = true;
        callbackContext.success("debugMode enabled");
    }

    private void setAnonymizeIp(boolean anonymize, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        tracker.setAnonymizeIp(anonymize);
        callbackContext.success("Set AnonymizeIp " + anonymize);
    }

    private void setUserId(String userId, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        tracker.set("&uid", userId);
        callbackContext.success("Set user id" + userId);
    }

    private void setAppVersion(String version, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        tracker.set("&av", version);
        callbackContext.success("Set app version: " + version);
    }

    private void enableUncaughtExceptionReporting(Boolean enable, CallbackContext callbackContext) {
        if (!trackerStarted) {
            callbackContext.error("Tracker not started");
            return;
        }

        tracker.enableExceptionReporting(enable);
        callbackContext.success((enable ? "Enabled" : "Disabled") + " uncaught exception reporting");
    }

    private void initTagManager(String containerId, final CallbackContext callbackContext) {
        // Según https://developers.google.com/tag-manager/android/v4/
        TagManager tagManager = TagManager.getInstance(this.cordova.getActivity());        
        // Modify the log level of the logger to print out not only
        // warning and error messages, but also verbose, debug, info messages.
        tagManager.setVerboseLoggingEnabled(true);
        System.out.println("[TAG_MANAGER] tagManager: " + tagManager);

        /*
        Use the TagManager singleton to make a request to load a container, specifying a 
        Google Tag Manager container ID as well as your default container file. 
        The container ID should be uppercase and exactly match the container ID in 
        the Google Tag Manager web interface. The call to 
        loadContainerPreferNonDefault() is non-blocking and returns a PendingResult:
        */
        // Usado para pruebas
        // String CONTAINER_ID = "GTM-T4LQXP";
        // PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(
        //   CONTAINER_ID,
        //   R.raw.gtm_t4lqxp);

        // Este es el de fiber
        // String CONTAINER_ID = "GTM-5VHBS6";
        PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(
          containerId,
          ar.com.cablevisionfibertel.fibertelzoneapp.R.raw.default_bin_container);

        //Use a ResultCallback to return the ContainerHolder once it has finished loading or timed out:

        long TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS = 2000;

        // The onResult method will be called as soon as one of the following happens:
        //     1. a saved container is loaded
        //     2. if there is no saved container, a network container is loaded
        //     3. the 2-second timeout occurs
        pending.setResultCallback(new ResultCallback<ContainerHolder>() {
            @Override
            public void onResult(ContainerHolder containerHolder) {
                // GTM_ContainerHolderSingleton.setContainerHolder(containerHolder);
                Container container = containerHolder.getContainer();
                if (!containerHolder.getStatus().isSuccess()) {
                    Log.e("[TAG_MANAGER]", "failure loading container");
                    System.err.println("[TAG_MANAGER] ERROR!!!!");
                    callbackContext.error("[TAG_MANAGER] ERROR!!!!");
                    return;
                }
                System.out.println("[TAG_MANAGER] Ready to start");
                callbackContext.success("[TAG_MANAGER] Ready to start");        
            }
        }, TIMEOUT_FOR_CONTAINER_OPEN_MILLISECONDS, TimeUnit.MILLISECONDS);
        callbackContext.success("initTagManager");

    }

    private DataLayer getDataLayer() {
        Context context = this.cordova.getActivity();
        DataLayer dataLayer = TagManager.getInstance(context).getDataLayer();
        return dataLayer;
    }
    /**
     * Push an "ScreenView" event with the given screen name.
     */
    public void pushScreen(String screenName, CallbackContext callbackContext) {
		DataLayer dataLayer = getDataLayer();
        dataLayer.pushEvent("ScreenView", DataLayer.mapOf("ScreenName", screenName));
        System.out.println("[TAG_MANAGER] pushScreen: " + screenName);
        callbackContext.success("pushScreen: " + screenName);
    }

	/**
     * Push a custom event with Category, Action and label.
     */
    public void pushEvent(String eventCategory, String eventAction, String eventLabel, CallbackContext callbackContext) {
		DataLayer dataLayer = getDataLayer();
        dataLayer.pushEvent("CustomEvent", DataLayer.mapOf("eventCategory", eventCategory,"eventAction", eventAction,"eventLabel", eventLabel));
        System.out.println("[TAG_MANAGER] pushEvent: " + eventAction);
        callbackContext.success("pushEvent: " + eventAction);
    }

	/**
     * Push data to the Datalayer
     */
    public void push(String key, String value, CallbackContext callbackContext) {
		DataLayer dataLayer = getDataLayer();
        dataLayer.push(DataLayer.mapOf(key, value));
        System.out.println("[TAG_MANAGER] push: " + key);
        callbackContext.success("push: " + key);
    }
    
}
