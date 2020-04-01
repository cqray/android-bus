package cn.cqray.android.bus;

import androidx.lifecycle.LifecycleOwner;

import io.reactivex.processors.FlowableProcessor;

/**
 * 转换器
 * @author Cqray
 */
public final class BusConverter {

    private FlowableProcessor<Object> mBus;
    private LifecycleOwner mLifecycleOwner;

    public BusConverter(FlowableProcessor<Object> bus, LifecycleOwner owner) {
        mBus = bus;
        mLifecycleOwner = owner;
    }

    public <T> Bus<T> of(Class<T> type) {
        return new BusImpl<>(mBus, mLifecycleOwner, type);
    }
}
