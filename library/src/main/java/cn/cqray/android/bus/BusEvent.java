package cn.cqray.android.bus;

final class BusEvent {

    static final String EMPTY_TAG = "";

    /** 事件 **/
    Object mEvent;
    /** 事件标签 **/
    String mTag;

    BusEvent(String tag, Object event) {
        mEvent = event;
        mTag = tag;
    }

    boolean isSameType(final String tag, final Class type) {
        return type.equals(BusUtils.getClassFromObject(mEvent)) &&
                mTag.equals(tag);
    }

    @Override
    public String toString() {
        return "event: " + mEvent + ", tag: " + mTag;
    }
}
