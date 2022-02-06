/*
 * A simple tool converting stardict database format to SQLite.
 * Copyright (C) 2015, Nguyễn Anh Tuấn
 * Email: anhtuanbk57@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.coolreader.db.StarDict;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

public class DictFileLoader {

    RandomAccessFile fileR;
    int curPos = 0;

    public DictFileLoader(File file) {
        try {
            fileR = new RandomAccessFile(file.getAbsolutePath(), "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getDefinition(int offset, int size) {
        if (offset != curPos) {
            try {
                fileR.seek(offset);
                curPos = offset;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String definition = "";
        byte[] data = new byte[size];
        try {
            fileR.read(data);
            definition = new String(data, 0, size, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return definition;
    }
}
