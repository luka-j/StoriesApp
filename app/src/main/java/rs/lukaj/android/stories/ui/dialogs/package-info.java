/**
 * Contains dialogs used throughout the app. Dialogs are created using MaterialDialogs library to
 * ensure consistent look-and-feel across multiple Android versions and variants. Dialogs which
 * have a newInstance method shall be constructed in that way in order to pass in all necessary
 * data. Dialogs without it take no additional data and it's safe to call default constructor.
 *
 * In case dialog has defined Callbacks (interface Callbacks inside the dialog class), if activity
 * to which the dialog is being attached is extending the appropriate callbacks and no other
 * callbacks have been explicitly specified, it is used as Callbacks. In case dialog is constructed
 * from inside the Fragment and Fragment wishes to handle callbacks, it should call the
 * registerCallbacks(Callbacks) method.
 */
package rs.lukaj.android.stories.ui.dialogs;