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

import org.coolreader.utils.StrUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class IdxFileLoader extends FileBytesLoader {

    private String word;

    // Despite stardict uses 32 or 64 bits unsigned integer to represent data offset and size,
    // i just use java 32 bits signed int. It's ok because for most cases the .dict
    // file's size is not likely to go past 2^31 = 2GB.
    private int dataOffset;
    private int dataSize;

    public IdxFileLoader(File file) {
        super(file);
    }

    public boolean available() {
        return !finished;
    }

    public String getNextWord() {
        readNext();
        return word;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getDataSize() {
        return dataSize;
    }

    private void readNext() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        while (getNextByte() != StrUtils.UTF8_END_BYTE) {
            outStream.write(thisByte);
        }

        byte[] bword = outStream.toByteArray();
        try {
            word = new String(bword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        byte[] dataOffsetBytes = new byte[4];
        dataOffsetBytes[0] = getNextByte();
        dataOffsetBytes[1] = getNextByte();
        dataOffsetBytes[2] = getNextByte();
        dataOffsetBytes[3] = getNextByte();
        dataOffset = byteArrayToInt(dataOffsetBytes);

        byte[] dataSizeBytes = new byte[4];
        dataSizeBytes[0] = getNextByte();
        dataSizeBytes[1] = getNextByte();
        dataSizeBytes[2] = getNextByte();
        dataSizeBytes[3] = getNextByte();

        dataSize = byteArrayToInt(dataSizeBytes);

        try {
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
