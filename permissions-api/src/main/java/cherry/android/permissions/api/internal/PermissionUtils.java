package cherry.android.permissions.api.internal;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.AppOpsManagerCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/5/15.
 */

public class PermissionUtils {

    private static final String TAG = "PermissionUtils";

    public static boolean hasSelfPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && "Xiaomi".equalsIgnoreCase(Build.MANUFACTURER)) {
                if (!checkSelfPermissionForXiaomi(context, permission)) {
                    return false;
                }
            } else {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkSelfPermissionForXiaomi(Context context, String permission) {
        String permissionToOp = AppOpsManagerCompat.permissionToOp(permission);
        if (permissionToOp == null)
            return true;
        int noteOp = AppOpsManagerCompat.noteOp(context, permissionToOp, Process.myUid(), context.getPackageName());
        return noteOp == AppOpsManagerCompat.MODE_ALLOWED
                && PermissionChecker.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean shouldShowRequestPermissionRational(Object target, String... permissions) {
        if (target instanceof Activity) {
            Activity activity = (Activity) target;
            return shouldShowRequestPermissionRational(activity, permissions);
        } else if (target instanceof Fragment) {
            Fragment fragment = (Fragment) target;
            return shouldShowRequestPermissionRational(fragment, permissions);
        } else {
            throw new IllegalArgumentException("target must be Activity or Fragment :" + target);
        }
    }

    private static boolean shouldShowRequestPermissionRational(Activity activity, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission))
                return true;
        }
        return false;
    }

    private static boolean shouldShowRequestPermissionRational(Fragment fragment, String... permissions) {
        for (String permission : permissions) {
            if (fragment.shouldShowRequestPermissionRationale(permission))
                return true;
        }
        return false;
    }

    public static void requestPermissions(Object target, String[] permissions, int requestCode) {
        if (target instanceof Activity) {
            Activity activity = (Activity) target;
            requestPermissions(activity, permissions, requestCode);
        } else if (target instanceof Fragment) {
            Fragment fragment = (Fragment) target;
            requestPermissions(fragment, permissions, requestCode);
        } else {
            throw new IllegalArgumentException("target must be Activity or Fragment :" + target);
        }
    }

    private static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    private static void requestPermissions(Fragment fragment, String[] permissions, int requestCode) {
        fragment.requestPermissions(permissions, requestCode);
    }

    public static Context getContext(Object target) {
        if (target instanceof Activity) {
            Activity activity = (Activity) target;
            return activity.getBaseContext();
        } else if (target instanceof Fragment) {
            Fragment fragment = (Fragment) target;
            return fragment.getContext();
        } else if (target instanceof Context) {
            return (Context) target;
        } else {
            throw new IllegalArgumentException("cannot get Context from target: " + target);
        }
    }

    public static void permissionGranted(Object target, int requestCode) {
        createAction(target).permissionGranted(requestCode);
    }

    public static void permissionDenied(Object target, int requestCode, String[] permissions) {
        if (!shouldShowRequestPermissionRational(target, permissions)
                && createAction(target).shouldPermissionRationale(requestCode)) {
            createAction(target).showPermissionRationale(requestCode);
        } else {
            createAction(target).permissionDenied(requestCode);
        }
    }

    private static Map<Class<?>, Constructor<? extends Action>> PERMISSIONS_CONSTRUCTOR = new HashMap<>();
    private static Map<Class<?>, Action> PERMISSIONS = new HashMap<>();

    private static Action createAction(Object target) {
        Class<?> targetClass = target.getClass();
        Action action = PERMISSIONS.get(targetClass);
        if (action != null) {
            action.updateTarget(target);
            return action;
        }
        Constructor<? extends Action> constructor = findPermissionConstructor(targetClass);
        if (constructor == null) {
            Log.e(TAG, "No Constructor Find for " + targetClass.getName());
            return null;
        }
        try {
            action = constructor.newInstance(target);
            PERMISSIONS.put(targetClass, action);
            return action;
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create permissions instance.", cause);
        }
    }

    private static Constructor<? extends Action> findPermissionConstructor(Class<?> targetClass) {
        Constructor<? extends Action> constructor = PERMISSIONS_CONSTRUCTOR.get(targetClass);
        if (constructor != null) {
            return constructor;
        }
        String className = targetClass.getName();
        try {
            Class<?> permissionClass = Class.forName(className + "_Permissions");
            constructor = (Constructor<? extends Action>) permissionClass.getConstructor(targetClass);
        } catch (ClassNotFoundException e) {
            constructor = findPermissionConstructor(targetClass.getSuperclass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("cannot find constructor for " + className, e);
        }
        return constructor;
    }

    public static <T> T castTarget(Object target, Class<T> cls) {
        try {
            return cls.cast(target);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Target '"
                    + target
                    + " was of the wrong type. See cause for more info.", e);
        }
    }
}