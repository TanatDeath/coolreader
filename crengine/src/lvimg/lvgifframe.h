/** @file lvgifframe.h
    @brief library private stuff

    CoolReader Engine

    (c) Vadim Lopatin, 2000-2006
    This source code is distributed under the terms of
    GNU General Public License.

    See LICENSE file for details.

*/

#ifndef __LVGIFFRAME_H_INCLUDED__
#define __LVGIFFRAME_H_INCLUDED__

#include "crsetup.h"

#if (USE_GIF==1)

#include "lvtypes.h"

class LVGifImageSource;
class LVImageDecoderCallback;

// https://www.oreilly.com/library/view/programming-web-graphics/1565924789/ch01s02.html:
// "If a file does not have a global color table and does not have local color tables,
// the image will be rendered using the application's default color table, with unpredictable results"
// "The GIF specification suggests that the first two elements of a color table be black (0) and white (1),
// but this is not necessarily always the case".
// Let's get the same values as MuPDF's mupdf/source/fitz/load-gif.c (which does it via a hardcoded table).
#define GIF_DEFAULT_PALETTE_COLOR_VALUE(b) ( b == 0 ? 0x000000 : ( b == 1 ? 0xFFFFFF : (b<<16|b<<8|b) ) )

class LVGifFrame
{
protected:
    int        m_cx;
    int        m_cy;
    int m_left;
    int m_top;
    unsigned char m_bpp;     // bits per pixel
    unsigned char m_flg_ltc; // GTC (gobal table of colors) flag
    unsigned char m_flg_interlaced; // interlace flag

    LVGifImageSource * m_pImage;
    lUInt32 *    m_local_color_table;

    unsigned char * m_buffer;
public:
    int DecodeFromBuffer( unsigned char * buf, int buf_size, int &bytes_read );
    LVGifFrame(LVGifImageSource * pImage);
    ~LVGifFrame();
    void Clear();
    lUInt32 * GetColorTable();
    void Draw( LVImageDecoderCallback * callback );
};

#endif  // (USE_GIF==1)

#endif  // __LVGIFFRAME_H_INCLUDED__
