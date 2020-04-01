package cn.cqray.android.bus;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;

import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;

/**
 * 事件处理
 * @author Cqray
 */
public interface Bus<T> extends LifecycleObserver {

    Bus<T> tag(@NonNull String tag);

    Bus<T> sticky();

    Bus<T> observeOn(@NonNull Scheduler scheduler);

    void subscribe(@NonNull Consumer<T> onNext);

    void subscribe(@NonNull Consumer<T> onNext, Consumer<Throwable> onError);
}
