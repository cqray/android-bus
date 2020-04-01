package cn.cqray.android.bus;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.processors.FlowableProcessor;

final class BusImpl<T> implements Bus<T> {

    /** 默认的异常处理 **/
    private static final Consumer<Throwable> ON_ERROR = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) throws Exception {
            if (Rxbus.sDebug) {
                Log.e(Rxbus.TAG, throwable.getMessage(), throwable);
            }
        }
    };

    private boolean mSticky;
    private String mTag;
    private Class<T> mClass;
    private Lifecycle mLifecycle;
    private LifecycleOwner mOwner;
    private Scheduler mScheduler;
    private CompositeDisposable mDisposable;
    private FlowableProcessor<Object> mBus;

    public BusImpl(FlowableProcessor<Object> bus, LifecycleOwner owner, Class<T> clazz) {
        mBus = bus;
        mTag = BusEvent.EMPTY_TAG;
        mClass = clazz;
        mOwner = owner;
        mLifecycle = owner.getLifecycle();
        mLifecycle.addObserver(this);
        mDisposable = new CompositeDisposable();
        if (Rxbus.sDebug) {
            Log.i(Rxbus.TAG, owner.getClass().getName() + " is registered.");
        }
    }

    @Override
    public Bus<T> tag(@NonNull String tag) {
        mTag = tag;
        return this;
    }

    @Override
    public Bus<T> sticky() {
        mSticky = true;
        return this;
    }

    @Override
    public Bus<T> observeOn(@NonNull Scheduler scheduler) {
        mScheduler = scheduler;
        return this;
    }

    @Override
    public void subscribe(@NonNull Consumer<T> onNext) {
        subscribe(onNext, ON_ERROR);
    }

    @Override
    public void subscribe(@NonNull Consumer<T> onNext, Consumer<Throwable> onError) {
        if (mSticky) {
            final BusEvent stickyEvent = Rxbus.findStickyEvent(mTag, mClass);
            if (stickyEvent != null) {
                Flowable<T> stickyFlowable = Flowable.create(new FlowableOnSubscribe<T>() {
                    @Override
                    public void subscribe(FlowableEmitter<T> emitter) {
                        if (Rxbus.sDebug) {
                            String format = "received a sticky event(\"%s\", %s), content is %s.";
                            String message =  String.format(format,
                                    mTag,
                                    mClass.getName(),
                                    stickyEvent.mEvent.toString());
                            Log.i(Rxbus.TAG, message);
                        }
                        emitter.onNext(mClass.cast(stickyEvent.mEvent));
                    }
                }, BackpressureStrategy.LATEST);
                if (mScheduler != null) {
                    stickyFlowable = stickyFlowable.observeOn(mScheduler);
                } else {
                    stickyFlowable = stickyFlowable.observeOn(AndroidSchedulers.mainThread());
                }
                BusSubscriber<T> ls = new BusSubscriber<>(onNext, onError);
                stickyFlowable.subscribe(ls);
                mDisposable.add(ls);
            } else {
                if (Rxbus.sDebug) {
                    Log.w(Rxbus.TAG, "sticky event is empty.");
                }
            }
        }

        BusSubscriber<T> ls = new BusSubscriber<>(onNext, onError);
        toFlowable().subscribe(ls);
        mDisposable.add(ls);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy(LifecycleOwner owner) {
        // 取消订阅
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }
        // 取消生命周期监听订阅
        if (mLifecycle != null) {
            mLifecycle.removeObserver(this);
        }
        if (Rxbus.sDebug) {
            Log.i(Rxbus.TAG, owner.getClass().getName() + " is unregistered.");
        }
    }

    private Flowable<T> toFlowable() {
        Flowable<T> flowable = mBus.ofType(BusEvent.class)
                .filter(new Predicate<BusEvent>() {
                    @Override
                    public boolean test(BusEvent tagEvent) {
                        Class eventClass = BusUtils.getClassFromObject(tagEvent.mEvent);
                        boolean equal = mTag.equals(tagEvent.mTag) && mClass.equals(eventClass);
                        if (!equal && Rxbus.sDebug) {
                            String format = "%s filter an event(\"%s\", %s) " +
                                    "because it registered event(\"%s\", %s).";
                            String message = String.format(format,
                                    mOwner.getClass().getName(),
                                    tagEvent.mTag,
                                    eventClass == null ? null : eventClass.getName(),
                                    mTag,
                                    mClass.getName());
                            Log.i(Rxbus.TAG, message);
                        }
                        return equal;
                    }
                })
                .map(new Function<BusEvent, Object>() {
                    @Override
                    public Object apply(BusEvent tagEvent) {
                        if (Rxbus.sDebug) {
                            String format = "received an event(\"%s\", %s), content is %s.";
                            String message =  String.format(format,
                                    mTag,
                                    mClass.getName(),
                                    tagEvent.mEvent.toString());
                            Log.i(Rxbus.TAG, message);
                        }
                        return tagEvent.mEvent;
                    }
                })
                .cast(mClass);
        if (mScheduler != null) {
            return flowable.observeOn(mScheduler);
        }
        return flowable.observeOn(AndroidSchedulers.mainThread());
    }

}
