package com.vikas.nfc.utils;

/**
 */

public class Constants {

    public final static String STATUS_TAG = "Status";

    // Root topics
    public static final String TOPIC_TURN_COFFEE_MACHINE = "qualcomm/CoffeeMachine/TurnMachine";
    public static final String TOPIC_MAKE_COFFEE = "qualcomm/CoffeeMachine/MakeCoffee";


    public static final String TOPIC_TURN_ON_OFF = "qualcomm/CoffeeMachine/TurnOnOff";
    public static final String TOPIC_SHORT_COFFE = "qualcomm/CoffeeMachine/ShortCoffee";
    public static final String TOPIC_LONG_COFFE = "qualcomm/CoffeeMachine/LongCoffee";

    public static final String TOPIC_LEVEL_COFFEE = "qualcomm/CoffeeMachine/CoffeeLevel";
    public static final String TOPIC_LEVEL_WATER = "qualcomm/CoffeeMachine/WaterLevel";
    public static final String TOPIC_GLASS_POSITION = "qualcomm/CoffeeMachine/GlassPosition";
    public static final String TOPIC_UPDATE = "qualcomm/CoffeeMachine/Update";
    public static final String TOPIC_ERROR = "qualcomm/CoffeeMachine/Error";
    public static final String ALL_TOPICS = "qualcomm/CoffeeMachine/+/IoT";

    public static final String IOT = "/IoT";
    public static final String ANDROID = "/Android";


    /**
     * Long and short coffee.
     */
    public static final String SHORT = "0";
    public static final String LONG = "1";

    //Status of glass position
    public static final int POSITIONED = 1;

    // Status of water level
    public static final int EMPTY = 0;
    public static final int FULL = 1;

    //Status of Coffee Machine
    public static final String ON = "1";
    public static final String OFF = "0";

    //Status of glass position
    public static final int NOT_POSITIONED = 0;

    //Google voice
    public static final int REQ_CODE_SPEECH_INPUT = 100;

}
