package ru.radiolight.radio;

public class Constants {

    static final String URL_SVET = "http://s03.radio-tochka.com:6480";
    static final String URL_SVOBODA = "http://s03.radio-tochka.com:6485";
    static final String URL_MIR = "http://s03.radio-tochka.com:6495";

    //protected static final String ACTIVITY_MESSENGER = "ACTIVITY_MESSENGER";
    protected static final int SERVICE_STOPED = 0;
    protected static final int UPDATE_TITLE = 1;
    protected static final int MESSAGE_SERVICE_CONNECTION = 2;
    protected static final int UPDATE_META = 100;
    protected static final int ACTIVITY_STOPED = 101;


    public static final long UPDATE_META_DELAY = 10000; //10 sec
    /*
static final String URL_SVET = "http://94.25.53.133/nashe-128.mp3";
static final String URL_SVOBODA = "http://94.25.53.133/nashe-128.mp3";
static final String URL_MIR = "http://94.25.53.133/nashe-128.mp3";
*/

    final static String CHECK_CONNECTION = "Check internet connection";

    // actions
    final static String RADIO_STARTED = "ru.radiolight.radio.RADIO_STARTED";
    final static String RADIO_STOPED = "ru.radiolight.radio.RADIO_STOPED";
}
