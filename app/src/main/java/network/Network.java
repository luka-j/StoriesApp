package network;

/**
 * Created by luka on 17.12.17..
 */

public class Network {
    public static final boolean DEBUG = true;

    protected static final String HOST = DEBUG ? "http://192.168.1.15:9000/" : "https://todo.real-addr.here/";
    protected static final String V1 = DEBUG ? "api/v1/" : "v1/";

    public static boolean isOnline = true;
}
