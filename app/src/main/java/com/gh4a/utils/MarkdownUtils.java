package com.gh4a.utils;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.widget.EditText;

public class MarkdownUtils {

    public static final int LIST_TYPE_BULLETS = 0;
    public static final int LIST_TYPE_NUMBERS = 1;
    public static final int LIST_TYPE_TASKS = 2;

    @IntDef({ LIST_TYPE_BULLETS, LIST_TYPE_NUMBERS, LIST_TYPE_TASKS })
    public @interface ListType {
    }

    private MarkdownUtils() {
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown list.
     *
     * @param editText The EditText to which to add markdown list.
     * @param listType The type of the list.
     */
    public static void addList(@NonNull EditText editText, @ListType int listType) {
        addList(editText, UiUtils.getSelectedText(editText), listType);
    }

    /**
     * Inserts a markdown list to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add markdown list.
     * @param text     The text to turn into the list. Each new line will become a separate list
     *                 entry.
     * @param listType The type of the list.
     */
    public static void addList(@NonNull EditText editText, @NonNull CharSequence text,
            @ListType int listType) {
        int tagCount = 1;
        String tag;
        if (listType == LIST_TYPE_NUMBERS) {
            tag = "1. ";
        } else if (listType == LIST_TYPE_TASKS) {
            tag = "- [ ] ";
        } else {
            tag = "- ";
        }

        String source = editText.getText().toString();
        int selectionStart = editText.getSelectionStart();

        if (text.length() == 0) {
            String substring = source.substring(0, selectionStart);

            int firstLine = substring.lastIndexOf('\n');
            if (firstLine != -1) {
                selectionStart = firstLine + 1;
            } else {
                selectionStart = 0;
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (!hasNewLineBeforeSelection(source, selectionStart)) {
            stringBuilder.append("\n");
        }

        String[] lines = text.toString().split("\n");
        if (lines.length > 0) {
            for (String line : lines) {
                if (line.length() == 0 && stringBuilder.length() != 0) {
                    stringBuilder.append("\n");
                    continue;
                }

                if (stringBuilder.length() > 0) {
                    stringBuilder.append("\n");
                }
                if (!line.trim().startsWith(tag)) {
                    stringBuilder.append(tag).append(line);
                } else {
                    stringBuilder.append(line);
                }

                if (listType == LIST_TYPE_NUMBERS) {
                    tagCount += 1;
                    tag = tagCount + ". ";
                }
            }
        }

        if (stringBuilder.length() == 0) {
            stringBuilder.append(tag);
        }

        if (text.length() == 0) {
            stringBuilder.append(source.substring(selectionStart, editText.getSelectionStart()));
        }

        editText.getText().replace(selectionStart, editText.getSelectionEnd(), stringBuilder);
        editText.setSelection(selectionStart + stringBuilder.length());
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown header tag.
     *
     * @param editText The EditText to which to add markdown tag.
     * @param level    The level of the header tag.
     */
    public static void addHeader(@NonNull EditText editText, int level) {
        addHeader(editText, UiUtils.getSelectedText(editText), level);
    }

    /**
     * Inserts a markdown header tag to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add markdown header tag.
     * @param level    The level of the header tag.
     * @param text     The text of the header tag.
     */
    public static void addHeader(@NonNull EditText editText, @NonNull CharSequence text,
            int level) {
        CharSequence source = editText.getText();
        int selectionStart = editText.getSelectionStart();

        StringBuilder result = new StringBuilder();
        if (!hasNewLineBeforeSelection(source, selectionStart)) {
            result.append("\n");
        }
        for (int i = 0; i < level; i++) {
            result.append("#");
        }
        result.append(" ");
        if (text.length() > 0) {
            result.append(text).append("\n\n");
        }

        UiUtils.replaceSelectionText(editText, result);
        editText.setSelection(selectionStart + result.length());
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown bold tag.
     *
     * @param editText The EditText to which to add markdown bold tag.
     */
    public static void addBold(@NonNull EditText editText) {
        addBold(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown bold to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add markdown bold tag.
     * @param text     The text of the bold tag.
     */
    public static void addBold(@NonNull EditText editText, @NonNull CharSequence text) {
        setSurroundText(editText, text, "**");
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown italic tag.
     *
     * @param editText The EditText to which to add markdown italic tag.
     */
    public static void addItalic(@NonNull EditText editText) {
        addItalic(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown italic to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add markdown italic tag.
     * @param text     The text of the italic tag.
     */
    public static void addItalic(@NonNull EditText editText, @NonNull CharSequence text) {
        setSurroundText(editText, text, "_");
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown strike-through
     * tag.
     *
     * @param editText The EditText to which to add markdown strike-through tag.
     */
    public static void addStrikeThrough(@NonNull EditText editText) {
        addStrikeThrough(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown strike-through to the specified EditText at the currently selected
     * position.
     *
     * @param editText The EditText to which to add markdown strike-through tag.
     * @param text     The text of the strike-through tag.
     */
    public static void addStrikeThrough(@NonNull EditText editText, @NonNull CharSequence text) {
        setSurroundText(editText, text, "~~");
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown code block.
     *
     * @param editText The EditText to which to add code block.
     */
    public static void addCode(@NonNull EditText editText) {
        addCode(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown code block to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add markdown code block.
     * @param text     The text of the code block.
     */
    public static void addCode(@NonNull EditText editText, @NonNull CharSequence text) {
        String source = editText.getText().toString();
        int selectionStart = editText.getSelectionStart();

        String result;
        if (hasNewLineBeforeSelection(source, selectionStart)) {
            result = "```\n" + text + "\n```\n";
        } else {
            result = "\n```\n" + text + "\n```\n";
        }

        UiUtils.replaceSelectionText(editText, result);
        editText.setSelection(selectionStart + result.length() - 5);
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown quote block.
     *
     * @param editText The EditText to which to add quote block.
     */
    public static void addQuote(@NonNull EditText editText) {
        addQuote(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown quote block to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add quote block.
     * @param text     The text of the quote block.
     */
    public static void addQuote(@NonNull EditText editText, @NonNull CharSequence text) {
        Editable source = editText.getText();
        int selectionStart = editText.getSelectionStart();

        StringBuilder result = new StringBuilder();
        if (selectionStart > 0 && (selectionStart < 2 ||
                source.charAt(selectionStart - 2) != '\n' ||
                source.charAt(selectionStart - 1) != '\n')) {
            result.append("\n");

            if (!hasNewLineBeforeSelection(source, selectionStart)) {
                result.append("\n");
            }
        }

        result.append("> ");
        if (text.length() > 0) {
            result.append(text.toString().replace("\n", "\n> "));
            result.append("\n\n");
        }

        UiUtils.replaceSelectionText(editText, result);
        editText.setSelection(selectionStart + result.length());
    }

    /**
     * Inserts a markdown divider to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add divider.
     */
    public static void addDivider(@NonNull EditText editText) {
        String source = editText.getText().toString();
        int selectionStart = editText.getSelectionStart();

        String result;
        if (hasNewLineBeforeSelection(source, selectionStart)) {
            result = "-------\n";
        } else {
            result = "\n-------\n";
        }

        UiUtils.replaceSelectionText(editText, result);
        editText.setSelection(selectionStart + result.length());
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown image tag.
     *
     * @param editText The EditText to which to add image tag.
     */
    public static void addImage(@NonNull EditText editText) {
        addImage(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown image tag to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add image tag.
     * @param link     The link to the image.
     */
    public static void addImage(@NonNull EditText editText, @NonNull CharSequence link) {
        int selectionStart = editText.getSelectionStart();

        UiUtils.replaceSelectionText(editText, "![](" + link + ") ");

        int selectionAdd = link.length() > 0 ? 2 : 4;
        editText.setSelection(selectionStart + selectionAdd);
    }

    /**
     * Turns the current selection inside of the specified EditText into a markdown link tag.
     *
     * @param editText The EditText to which to add link tag.
     */
    public static void addLink(@NonNull EditText editText) {
        addLink(editText, UiUtils.getSelectedText(editText));
    }

    /**
     * Inserts a markdown link tag to the specified EditText at the currently selected position.
     *
     * @param editText The EditText to which to add link tag.
     * @param link     The link.
     */
    public static void addLink(@NonNull EditText editText, @NonNull CharSequence link) {
        int selectionStart = editText.getSelectionStart();

        UiUtils.replaceSelectionText(editText, "[](" + link + ") ");

        int selectionAdd = link.length() > 0 ? 1 : 3;
        editText.setSelection(selectionStart + selectionAdd);
    }

    private static boolean hasNewLineBeforeSelection(@NonNull CharSequence text,
            int selectionStart) {
        return selectionStart <= 0 || text.charAt(selectionStart - 1) == '\n';
    }

    private static void setSurroundText(@NonNull EditText editText, @NonNull CharSequence text,
            String surroundText) {
        CharSequence source = editText.getText();
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();

        text = text.toString().trim();

        int charactersToGoBack = 0;
        if (text.length() == 0) {
            charactersToGoBack = surroundText.length();
        }

        StringBuilder result = new StringBuilder();
        if (selectionStart > 0 && !Character.isWhitespace(source.charAt(selectionStart - 1))) {
            result.append(" ");
        }
        result.append(surroundText).append(text).append(surroundText);
        if (selectionEnd < source.length() - 1 &&
                !Character.isWhitespace(source.charAt(selectionEnd + 1))) {
            result.append(" ");
            charactersToGoBack += 1;
        }

        UiUtils.replaceSelectionText(editText, result);

        editText.setSelection(selectionStart + result.length() - charactersToGoBack);
    }
}
