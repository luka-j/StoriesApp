package rs.lukaj.android.stories.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import rs.lukaj.android.stories.R;

/**
 * Dialog showing some information and an OK button. Title and content are mandatory.
 * Created by luka on 2.1.16.
 */
public class InfoDialog extends DialogFragment {
    public static final String ARG_TITLE   = "stories.dialog.errortile";
    public static final String ARG_MESSAGE = "stories.dialog.errormsg";
    private Callbacks callbacks;

    public static InfoDialog newInstance(String title, String content) {
        if(title == null) title = "I've forgotten a title (please tell me!)";
        if(content == null) content = "I've forgotten to put text here. If I've forgotten the title too, "
                                      + "something is definitely wrong.";
        InfoDialog f    = new InfoDialog();
        Bundle     args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, content);
        f.setArguments(args);
        return f;
    }

    public InfoDialog registerCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        String                 text    = getArguments().getString(ARG_MESSAGE);
        builder.title(getArguments().getString(ARG_TITLE))
               .positiveText(R.string.ok)
               .autoDismiss(true)
               .onPositive((materialDialog, dialogAction) -> {
                   if(callbacks!=null) callbacks.onInfoDialogClosed(InfoDialog.this);
               });
        if(text != null && !text.isEmpty())
            builder.content(text);
        return builder.show();
    }

    public interface Callbacks {
        void onInfoDialogClosed(InfoDialog dialog);
    }
}
