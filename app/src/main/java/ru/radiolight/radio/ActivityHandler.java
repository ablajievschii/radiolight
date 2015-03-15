package ru.radiolight.radio;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ActivityHandler extends Handler {

    RadioActivity mRadioActivity ;

    public ActivityHandler(RadioActivity activity){
        mRadioActivity = activity;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.d(ActivityHandler.class.getName(), "handleMessage");
        switch(msg.what){
        case Constants.UPDATE_TITLE:
            String title = (String) msg.obj;
            mRadioActivity.updateTitle(title);
            break;
        case Constants.SERVICE_STOPED:
            mRadioActivity.stopPlay();
            break;
        default:
            break;
        }
    }

}
