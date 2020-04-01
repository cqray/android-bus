package cn.cqray.android.bus;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;

/**
 * 事件分发
 * @author Cqray
 */
public class Rxbus {

    static final String TAG = "Rxbus";
    static boolean sDebug;
    private final FlowableProcessor<Object> mBus;
    private final Map<Class, List<BusEvent>> mStickyEventMap;

    private static class Holder {
        private static final Rxbus INSTANCE = new Rxbus();
    }

    private Rxbus() {
        mBus = PublishProcessor.create().toSerialized();
        mStickyEventMap = new ConcurrentHashMap<>();
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public static void post(@NonNull Object event) {
        post(BusEvent.EMPTY_TAG, event, false);
    }

    public static void post(@NonNull String tag, Object event) {
        post(tag, event, false);
    }

    public static void postSticky(@NonNull Object event) {
        post(BusEvent.EMPTY_TAG, event, true);
    }

    public static void postSticky(@NonNull String tag, @NonNull Object event) {
        post(tag, event, true);
    }

    public static void removeSticky(final Object event) {
        removeSticky(BusEvent.EMPTY_TAG, event);
    }

    public static void removeSticky(final String tag, final Object event) {
        requireNonNull(tag, event);
        removeStickyEvent(tag, event);
    }

    public static BusConverter with(@NonNull FragmentActivity activity) {
        return new BusConverter(Holder.INSTANCE.mBus, activity);
    }

    public static BusConverter with(@NonNull Fragment fragment) {
        return new BusConverter(Holder.INSTANCE.mBus, fragment);
    }

    public static BusConverter with(@NonNull LifecycleOwner owner) {
        return new BusConverter(Holder.INSTANCE.mBus, owner);
    }

    static void post(final String tag, final Object event, final boolean sticky) {
        requireNonNull(event, tag);
        BusEvent msgEvent = new BusEvent(tag, event);
        if (sticky) {
            addStickyEvent(tag, event);
        }

        // 打印调试日志
        if (sDebug) {
            Class clazz = BusUtils.getClassFromObject(event);
            String format = "post %s event(\"%s\", %s)";
            String message = String.format(format,
                    sticky? "a sticky" : "an",
                    tag,
                    clazz == null ? null : clazz.getName());
            Log.i(TAG, message);
        }

        Holder.INSTANCE.mBus.onNext(msgEvent);
    }

    static void requireNonNull(final Object... objects) {
        if (objects == null) {
            throw new NullPointerException();
        }
        for (Object object : objects) {
            if (object == null) {
                throw new NullPointerException();
            }
        }
    }

    static void addStickyEvent(final String tag, final Object event) {
        Class eventType = event.getClass();
        synchronized (Holder.INSTANCE.mStickyEventMap) {
            List<BusEvent> stickyEvents = Holder.INSTANCE.mStickyEventMap.get(eventType);
            if (stickyEvents == null) {
                stickyEvents = new ArrayList<>();
                stickyEvents.add(new BusEvent(tag, event));
                Holder.INSTANCE.mStickyEventMap.put(eventType, stickyEvents);
            } else {
                for (int i = stickyEvents.size() - 1; i >= 0; --i) {
                    BusEvent tmp = stickyEvents.get(i);
                    if (tmp.isSameType(tag, eventType) && sDebug) {
                        Log.w(TAG, "The sticky event already added.");
                        return;
                    }
                }
                stickyEvents.add(new BusEvent(tag, event));
            }
        }
    }

    static void removeStickyEvent(final String tag, final Object event) {
        Class eventType = event.getClass();
        synchronized (Holder.INSTANCE.mStickyEventMap) {
            List<BusEvent> stickyEvents = Holder.INSTANCE.mStickyEventMap.get(eventType);
            if (stickyEvents == null) {
                return;
            }
            for (int i = stickyEvents.size() - 1; i >= 0; --i) {
                BusEvent stickyEvent = stickyEvents.get(i);
                if (stickyEvent.isSameType(tag, eventType)) {
                    stickyEvents.remove(i);
                    break;
                }
            }
            if (stickyEvents.size() == 0) {
                Holder.INSTANCE.mStickyEventMap.remove(eventType);
            }
        }
    }

    static BusEvent findStickyEvent(final String tag, final Class eventType) {
        synchronized (Holder.INSTANCE.mStickyEventMap) {
            List<BusEvent> stickyEvents = Holder.INSTANCE.mStickyEventMap.get(eventType);
            if (stickyEvents == null) {
                return null;
            }
            int size = stickyEvents.size();
            BusEvent res = null;
            for (int i = size - 1; i >= 0; --i) {
                BusEvent stickyEvent = stickyEvents.get(i);
                if (stickyEvent.isSameType(tag, eventType)) {
                    res = stickyEvents.get(i);
                    break;
                }
            }
            return res;
        }
    }

}
