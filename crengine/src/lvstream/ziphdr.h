/** @file ziphdr.h
    @brief library private stuff

    CoolReader Engine

    (c) Vadim Lopatin, 2000-2006
    This source code is distributed under the terms of
    GNU General Public License.
    See LICENSE file for details.

*/

#ifndef __ZIPHDR_H_INCLUDED__
#define __ZIPHDR_H_INCLUDED__

#include "lvtypes.h"

#pragma pack(push, 1)

typedef struct {
    lUInt32  Mark;      // 0
    lUInt8   UnpVer;    // 4
    lUInt8   UnpOS;     // 5
    lUInt16  Flags;     // 6
    lUInt16  others[11]; //
    //lUInt16  Method;    // 8
    //lUInt32  ftime;     // A
    //lUInt32  CRC;       // E
    //lUInt32  PackSize;  //12
    //lUInt32  UnpSize;   //16
    //lUInt16  NameLen;   //1A
    //lUInt16  AddLen;    //1C

    lUInt16  getMethod() { return others[0]; }    // 8
    lUInt32  getftime() { return others[1] | ( (lUInt32)others[2] << 16); }     // A
    lUInt32  getCRC() { return others[3] | ( (lUInt32)others[4] << 16); }       // E
    lUInt32  getPackSize() { return others[5] | ( (lUInt32)others[6] << 16); }  //12
    lUInt32  getUnpSize() { return others[7] | ( (lUInt32)others[8] << 16); }   //16
    lUInt16  getNameLen() { return others[9]; }   //1A
    lUInt16  getAddLen() { return others[10]; }    //1C
    void byteOrderConv()
    {
        //
        lvByteOrderConv cnv;
        if ( cnv.msf() )
        {
            cnv.rev( &Mark );
            cnv.rev( &Flags );
            for ( int i=0; i<11; i++) {
                cnv.rev( &others[i] );
            }
            //cnv.rev( &Method );
            //cnv.rev( &ftime );
            //cnv.rev( &CRC );
            //cnv.rev( &PackSize );
            //cnv.rev( &UnpSize );
            //cnv.rev( &NameLen );
            //cnv.rev( &AddLen );
        }
    }
    //  Omitted fields (which follow this structure):
    // FileName (size = NameLen)
    // ExtraField (size = AddLen)
} ZipLocalFileHdr;

struct ZipHd2
{
    lUInt32  Mark;      // 0
    lUInt8   PackVer;   // 4
    lUInt8   PackOS;
    lUInt8   UnpVer;
    lUInt8   UnpOS;
    lUInt16     Flags;  // 8
    lUInt16     Method; // A
    lUInt32    ftime;   // C
    lUInt32    CRC;     // 10
    lUInt32    PackSize;// 14, ZIP64: if == 0xFFFFFFFF use Zip64ExtInfo
    lUInt32    UnpSize; // 18, ZIP64: if == 0xFFFFFFFF use Zip64ExtInfo
    lUInt16     NameLen;// 1C
    lUInt16     AddLen; // 1E
    lUInt16     CommLen;// 20
    lUInt16     DiskNum;// 22
    //lUInt16     ZIPAttr;// 24
    //lUInt32     Attr;   // 26
    //lUInt32     Offset; // 2A
    lUInt16     _Attr_and_Offset[5];   // 24
    lUInt16     getZIPAttr() { return _Attr_and_Offset[0]; }
    lUInt32     getAttr() { return _Attr_and_Offset[1] | ((lUInt32)_Attr_and_Offset[2]<<16); }
    lUInt32     getOffset() { return _Attr_and_Offset[3] | ((lUInt32)_Attr_and_Offset[4]<<16); }    // ZIP64: if == 0xFFFFFFFF use Zip64ExtInfo
    void        setOffset(lUInt32 offset) {
        _Attr_and_Offset[3] = (lUInt16)(offset & 0xFFFF);
        _Attr_and_Offset[4] = (lUInt16)(offset >> 16);
    }
    void byteOrderConv()
    {
        //
        lvByteOrderConv cnv;
        if ( cnv.msf() )
        {
            cnv.rev( &Mark );
            cnv.rev( &Flags );
            cnv.rev( &Method );
            cnv.rev( &ftime );
            cnv.rev( &CRC );
            cnv.rev( &PackSize );
            cnv.rev( &UnpSize );
            cnv.rev( &NameLen );
            cnv.rev( &AddLen );
            cnv.rev( &CommLen );
            cnv.rev( &DiskNum );
            cnv.rev( &_Attr_and_Offset[0] );
            cnv.rev( &_Attr_and_Offset[1] );
            cnv.rev( &_Attr_and_Offset[2] );
            cnv.rev( &_Attr_and_Offset[3] );
            cnv.rev( &_Attr_and_Offset[4] );
        }
    }
    //  Omitted fields (which follow this structure):
    // FileName (size = NameLen)
    // ExtraField (size = AddLen)
    // FileComment (size = CommLen)
};

struct Zip64ExtInfo
{
    lUInt16 Tag;        // 0x0001
    lUInt16 Size;       // 4-28
    lUInt8 data[28];

    void byteOrderConv() {
        lvByteOrderConv cnv;
        if ( cnv.msf() ) {
            cnv.rev( &Tag );
            cnv.rev( &Size );
        }
    }
    lUInt32 getField32(int pos) {
        if (pos >= 0 && pos + 3 < Size) {
            return (lUInt32)data[pos] | (((lUInt32)data[pos + 1]) << 8) |
                   (((lUInt32)data[pos + 2]) << 16) | (((lUInt32)data[pos + 3]) << 24);
        }
        return 0UL;
    }
    lUInt64 getField64(int pos) {
        if (pos >= 0 && pos + 7 < Size) {
            return (lUInt64)data[pos] | (((lUInt64)data[pos + 1]) << 8) |
                   (((lUInt64)data[pos + 2]) << 16) | (((lUInt64)data[pos + 3]) << 24) |
                   (((lUInt64)data[pos + 4]) << 32) | (((lUInt64)data[pos + 5]) << 40) |
                   (((lUInt64)data[pos + 6]) << 48) | (((lUInt64)data[pos + 7]) << 56);
        }
        return 0UL;
    }
};

#pragma pack(pop)

#endif  // __ZIPHDR_H_INCLUDED__
