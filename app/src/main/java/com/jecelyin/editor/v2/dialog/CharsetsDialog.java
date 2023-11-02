/*
 * Copyright 2018 Mr Duy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jecelyin.editor.v2.dialog;

import android.app.AlertDialog;
import android.content.Context;

import com.jecelyin.editor.v2.common.Command;
import com.mrikso.apkrepacker.R;


import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

/**
 * @author Jecelyin Peng <jecelyin@gmail.com>
 */
public class CharsetsDialog extends AbstractDialog {
    private String[] names;

    public CharsetsDialog(Context context) {
        super(context);

        initCharsets();
    }

    private void initCharsets() {
        SortedMap m = Charset.availableCharsets();
        Set k = m.keySet();

        names = new String[m.size()];
        Iterator iterator = k.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String n = (String) iterator.next();
            names[i++] = n;
        }
    }

    @Override
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.reopen_with_encoding)
                .setItems(names, (dialog, which) -> {
                    Command command = new Command(Command.CommandEnum.RELOAD_WITH_ENCODING);
                    command.object = names[which];
                    getMainActivity().doCommand(command);
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        AlertDialog dlg = builder.create();
        dlg.show();
        handleDialog(dlg);
    }
}
