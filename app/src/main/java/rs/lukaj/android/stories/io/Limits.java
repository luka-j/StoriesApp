package rs.lukaj.android.stories.io;

/**
 * Various limits (upper and lower) used throughout the app. Maximums are usually meant to be an order
 * of magnitude larger than the intended 'normal' input, to prevent potential abuse.
 * Created by luka on 15.7.17.
 */
public class Limits {
    public static final int USER_EMAIL_MAX_LENGTH    = 254;
    public static final int USER_PASSWORD_MIN_LENGTH = 6;
    public static final int USER_PASSWORD_MAX_LENGTH = 127;
    public static final int USER_NAME_MAX_LENGTH     = 127;
    public static final int BOOK_TITLE_MAX_LENGTH    = 255;
    public static final int GENRES_MAX_LENGTH        = 127;
    public static final int MAX_GENRES_COUNT         = 5;
    public static final int BOOK_DESC_MAX_LENGTH     = 1023;
    public static final int CHAPTER_NAME_MAX_LENGTH  = 511;
    public static final int CHAPTER_DESC_MAX_LENGTH  = 1023;

    public static final int MAX_BOOK_INPUT = 8192;
    public static final int VAR_MAX_LENGTH = 256;
    public static final int STMT_MAX_LENGTH = 512;
}
