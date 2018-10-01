package com.github.ahmadaghazadeh.editor.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.github.ahmadaghazadeh.editor.R;
import com.github.ahmadaghazadeh.editor.document.commons.LinesCollection;
import com.github.ahmadaghazadeh.editor.processor.TextNotFoundException;
import com.github.ahmadaghazadeh.editor.processor.TextProcessor;
import com.github.ahmadaghazadeh.editor.processor.language.Language;
import com.github.ahmadaghazadeh.editor.processor.language.LanguageProvider;
import com.github.ahmadaghazadeh.editor.processor.utils.DefaultSetting;
import com.github.ahmadaghazadeh.editor.processor.utils.ITextProcessorSetting;

import java.io.Serializable;

public class CodeEditor extends RelativeLayout implements Serializable {
    private Context context;
    private TextProcessor editor;
    private Language language;
    private LinesCollection lineNumbers;
    private Editable text;
    private ITextProcessorSetting setting;
    private boolean isDirty; //На данный момент не используется

    public CodeEditor(Context context) {
        super(context);
        init(context, null);
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        initEditor();
        String code = "";
        String lang = "js";
        if (attrs != null) {

        }
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.view_code_editor, this, true);
            editor = findViewById(R.id.editor);
            editor.init(this);
            final FastScrollerView mFastScrollerView = findViewById(R.id.fast_scroller);
            mFastScrollerView.link(editor); //подключаем FastScroller к редактору
            final GutterView mGutterView = findViewById(R.id.gutter);
            mGutterView.link(editor, lineNumbers); //подключаем Gutter к редактору
            LinesCollection lines = new LinesCollection();
            lines.add(0, 0);
            setLanguage(LanguageProvider.getLanguage(lang)); //ставим язык
            setText(code, 1); //заполняем поле текстом
            setLineStartsList(lines); //подгружаем линии
            refreshEditor(); //подключаем все настройки
            editor.enableUndoRedoStack();
        }
    }


    public void refreshEditor() {
        if (editor != null) {
            editor.setTextSize(setting.getFontSize());
            editor.setHorizontallyScrolling(!setting.getWrapContent());
            editor.setShowLineNumbers(setting.getShowLineNumbers());
            editor.setBracketMatching(setting.getBracketMatching());
            editor.setHighlightCurrentLine(setting.getHighlightCurrentLine());
            editor.setCodeCompletion(setting.getCodeCompletion());
            editor.setPinchZoom(setting.getPinchZoom());
            editor.setInsertBrackets(setting.getInsertBracket());
            editor.setIndentLine(setting.getIndentLine());
            editor.refreshTypeface();
            editor.refreshInputType();
        }
    }

    private void initEditor() {
        setting = new DefaultSetting(context);
        lineNumbers = new LinesCollection();
    }

    public ITextProcessorSetting getSetting() {
        return setting;
    }

    public void setSetting(ITextProcessorSetting setting) {
        this.setting = setting;
    }

    private void setDirty(boolean dirty) {
        isDirty = dirty;
        //тут будет добавление "*" после названия файла если документ был изменен
    }

    public String getText() {
        if (text != null)
            return text.toString();
        else
            return "";
    }

    @WorkerThread
    @Nullable
    public Language getLanguage() {
        return language;
    }

    public void setLanguage(@Nullable Language language) {
        this.language = language;
    }

    //region METHODS_DOC

    /**
     * Методы для редактора, чтобы менять их в "Runtime".
     */

    public void setReadOnly(boolean readOnly) {
        if (editor != null)
            editor.setReadOnly(readOnly);
    }

    public void setSyntaxHighlight(boolean syntaxHighlight) {
        if (editor != null)
            editor.setSyntaxHighlight(syntaxHighlight);
    }

    //endregion METHODS_DOC

    //region LINES

    public void setLineStartsList(LinesCollection list) {
        lineNumbers = list;
    }

    public LinesCollection getLinesCollection() {
        return lineNumbers;
    }

    public int getLineCount() {
        return lineNumbers.getLineCount();
    }

    public int getLineForIndex(int index) {
        return lineNumbers.getLineForIndex(index);
    }

    public int getIndexForStartOfLine(int line) {
        return lineNumbers.getIndexForLine(line);
    }

    public int getIndexForEndOfLine(int line) {
        if (line == getLineCount() - 1) {
            return text.length();
        }
        return lineNumbers.getIndexForLine(line + 1) - 1;
    }

    public void replaceText(int start, int end, Editable text) {
        replaceText(start, end, text.toString());
    }

    public void replaceText(int start, int end, String text) {
        int i;
        if (this.text == null) {
            this.text = Editable.Factory.getInstance().newEditable("");
        }
        if (end >= this.text.length()) {
            end = this.text.length();
        }
        int newCharCount = text.length() - (end - start);
        int startLine = getLineForIndex(start);
        for (i = start; i < end; i++) {
            if (this.text.charAt(i) == '\n') {
                lineNumbers.remove(startLine + 1);
            }
        }
        lineNumbers.shiftIndexes(getLineForIndex(start) + 1, newCharCount);
        for (i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineNumbers.add(getLineForIndex(start + i) + 1, (start + i) + 1);
            }
        }
        if (start > end) {
            end = start;
        }
        if (start > this.text.length()) {
            start = this.text.length();
        }
        if (end > this.text.length()) {
            end = this.text.length();
        }
        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }
        this.text.replace(start, end, text);
        setDirty(true);
    }

    public void setText(String text, int flag) {
        if (text != null) {
            setText(Editable.Factory.getInstance().newEditable(text), flag);
        } else {
            setText("", flag);
        }
    }

    public void setText(Editable text, int flag) {
        if (flag == 1) {
            this.text = text;
            if (editor != null)
                editor.setText(this.text);
            return;
        }
        int length = 0;
        if (this.text != null) {
            length = this.text.length();
        }
        replaceText(0, length, text);
        setDirty(false);
    }

    //endregion LINES

    //region METHODS

    public void insert(@NonNull CharSequence text) {
        if (editor != null)
            editor.insert(text);
    }

    public void cut() throws TextNotFoundException {
        if (editor != null)
            editor.cut();
        else
            throw new TextNotFoundException();
    }

    public void copy() throws TextNotFoundException {
        if (editor != null)
            editor.copy();
        else
            throw new TextNotFoundException();
    }

    public void paste() throws TextNotFoundException {
        if (editor != null)
            editor.paste();
        else
            throw new TextNotFoundException();
    }

    public void undo() throws TextNotFoundException {
        if (editor != null)
            editor.undo();
        else
            throw new TextNotFoundException();
    }

    public void redo() throws TextNotFoundException {
        if (editor != null)
            editor.redo();
        else
            throw new TextNotFoundException();
    }

    public void selectAll() throws TextNotFoundException {
        if (editor != null)
            editor.selectAll();
        else
            throw new TextNotFoundException();
    }

    public void selectLine() throws TextNotFoundException {
        if (editor != null)
            editor.selectLine();
        else
            throw new TextNotFoundException();
    }

    public void deleteLine() throws TextNotFoundException {
        if (editor != null)
            editor.deleteLine();
        else
            throw new TextNotFoundException();
    }

    public void duplicateLine() throws TextNotFoundException {
        if (editor != null)
            editor.duplicateLine();
        else
            throw new TextNotFoundException();
    }

    public void find(String what, boolean matchCase, boolean regex, boolean wordOnly, Runnable onComplete) throws TextNotFoundException {
        if (editor != null && !what.equals("")) {
            editor.find(what, matchCase, regex, wordOnly, editor.getEditableText());
            onComplete.run();
        } else {
            throw new TextNotFoundException();
        }
    }

    public void replaceAll(String what, String with, Runnable onComplete) throws TextNotFoundException {
        if (editor != null && !what.equals("") && !with.equals("")) {
            editor.replaceAll(what, with);
            onComplete.run();
        } else {
            throw new TextNotFoundException();
        }
    }

    public void gotoLine(int line) throws TextNotFoundException {
        if (editor != null)
            editor.gotoLine(line);
        else
            throw new TextNotFoundException();
    }

    public void showToast(String string, boolean b) {

    }
}