package rs.lukaj.android.stories.environment;

import java.io.File;

import rs.lukaj.stories.environment.DisplayProvider;

/**
 * Created by luka on 7.8.17..
 */

public class NullDisplay implements DisplayProvider {
    @Override
    public void showNarrative(String s) {

    }

    @Override
    public void showSpeech(String s, File file, String s1) {

    }

    @Override
    public int showQuestion(String s, String s1, File file, double v, String... strings) {
        return 0;
    }

    @Override
    public int showPictureQuestion(String s, String s1, File file, double v, File... files) {
        return 0;
    }

    @Override
    public String showInput(String s) {
        return null;
    }

    @Override
    public void onChapterBegin(int i, String s) {

    }

    @Override
    public void onChapterEnd(int i, String s) {

    }

    @Override
    public void onBookBegin(String s) {

    }

    @Override
    public void onBookEnd(String s) {

    }
}
