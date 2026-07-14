/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.kde.necessitas.mucephi.android_xcas.inputtextautocomplete;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * Subclass of {@link InputTextView} that supports multi-line input but still
 * provides autocompletion of CAS keywords. The base class forces
 * {@code setSingleLine()}, which would break multi-line matrix entry.
 *
 * @author opencode
 */
public class MultiLineInputTextView extends InputTextView {

    public MultiLineInputTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSingleLine(false);
        setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setMinLines(1);
        setMaxLines(8);
        setHorizontallyScrolling(true);
    }

    public MultiLineInputTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSingleLine(false);
        setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setMinLines(1);
        setMaxLines(8);
        setHorizontallyScrolling(true);
    }

    public MultiLineInputTextView(Context context) {
        super(context);
        setSingleLine(false);
        setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setMinLines(1);
        setMaxLines(8);
        setHorizontallyScrolling(true);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        if (connection != null) {
            outAttrs.imeOptions = outAttrs.imeOptions
                    | EditorInfo.IME_FLAG_NO_ENTER_ACTION
                    | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        }
        return connection;
    }
}
