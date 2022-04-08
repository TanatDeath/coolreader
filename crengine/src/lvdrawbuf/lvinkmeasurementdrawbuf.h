#ifndef __LVINKMEASUREMENTDRAWBUF_H_INCLUDED__
#define __LVINKMEASUREMENTDRAWBUF_H_INCLUDED__

#include "lvbasedrawbuf.h"

/// Ink measurement buffer
class LVInkMeasurementDrawBuf : public LVBaseDrawBuf
{
private:
    int ink_top_y;
    int ink_bottom_y;
    int ink_left_x;
    int ink_right_x;
    bool has_ink;
    bool measure_hidden_content;
    bool ignore_decorations; // ignore borders and background
public:
    /// get buffer bits per pixel
    virtual int  GetBitsPerPixel() const { return 8; }

    /// wants to be fed hidden content (only LVInkMeasurementDrawBuf may return true)
    virtual bool WantsHiddenContent() const { return measure_hidden_content; }

    /// fills buffer with specified color
    virtual void Clear( lUInt32 color ) {
        has_ink = false;
    }

    /// fills rectangle with specified color
    void updateInkBounds( int x0, int y0, int x1, int y1 );

    bool getInkArea( lvRect &rect );

    /// fills rectangle with specified color
    virtual void FillRect( int x0, int y0, int x1, int y1, lUInt32 color );
    virtual void FillRectPattern( int x0, int y0, int x1, int y1, lUInt32 color0, lUInt32 color1, lUInt8 * pattern );
    /// draws image
    virtual void Draw( LVImageSourceRef img, int x, int y, int width, int height, bool dither );
    /// blend font bitmap using specified palette
    virtual void BlendBitmap( int x, int y, const lUInt8 * bitmap, FontBmpPixelFormat bitmap_fmt, int width, int height, int bmp_pitch, lUInt32 * palette );
    /// draw line
    virtual void DrawLine( int x0, int y0, int x1, int y1, lUInt32 color0, int length1=1, int length2=0, int direction=0 );

    virtual void GetClipRect( lvRect * clipRect ) const;

    /// create own draw buffer
    explicit LVInkMeasurementDrawBuf( bool measurehiddencontent=false, bool ignoredecorations=false)
        : ink_top_y(0), ink_bottom_y(0), ink_left_x(0), ink_right_x(0) , has_ink(false)
        , measure_hidden_content(measurehiddencontent) , ignore_decorations(ignoredecorations)
        {}
    /// destructor
    virtual ~LVInkMeasurementDrawBuf() {}

    // Unused methods in the context of lvrend that we need to have defined
    virtual void Rotate( cr_rotate_angle_t angle ) {}
    virtual lUInt32 GetWhiteColor() const { return 0; }
    virtual lUInt32 GetBlackColor() const { return 0; }
    virtual void DrawTo( LVDrawBuf * buf, int x, int y, int options, lUInt32 * palette ) {}
    virtual void DrawOnTop( LVDrawBuf * __restrict buf, int x, int y) {}
    virtual void DrawRescaled(LVDrawBuf * src, int x, int y, int dx, int dy, int options) {}
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
    virtual void DrawTo( HDC dc, int x, int y, int options, lUInt32 * palette ) {}
#endif
    virtual void Invert() {}
    virtual lUInt32 GetPixel( int x, int y ) const { return 0; }
    virtual void InvertRect( int x0, int y0, int x1, int y1 ) {}
    virtual void Resize( int dx, int dy ) {}
    virtual lUInt8 * GetScanLine( int y ) const { return 0; }
};

// This is to be used as the buffer provided to font->DrawTextString(). We based it
// on LVInkMeasurementDrawBuf just so that we don't have to redefine all the methods,
// even if none of them will be used (FillRect might be called when drawing underlines,
// but we're explicitely not handling it).
class LVHorizontalOverlapMeasurementDrawBuf : public LVInkMeasurementDrawBuf
{
private:
    bool drawing_right;
    bool by_line;
    lUInt8  min_opacity;
    int  buf_height;
    int  vertical_spread;
    int  whole_left_max_x;
    int  whole_right_min_x;
    int * left_max_x;
    int * right_min_x;
public:
    virtual void Draw( int x0, int y0, const lUInt8 * bitmap, int width, int height, const lUInt32 * __restrict palette ) {
        if ( width == 0 || height == 0)
            return;
        int y1 = y0 + height;
        int x1 = x0 + width;
        if (drawing_right) {
            for ( int y=y0; y<y1; y++ ) {
                if ( y >= 0 && y < buf_height ) {
                    int * const bucket = by_line ? &right_min_x[y] : &whole_right_min_x;
                    // Drawing a right word glyph: we want to catch its left edge:
                    // scan from the left to limit the amount of loops
                    const lUInt8 * __restrict tmp = bitmap + (y-y0)*width;
                    for ( int x=x0; x<x1; x++ ) {
                        if (*tmp >= min_opacity) { // (0 = blank pixel)
                            if ( by_line && vertical_spread > 0 ) {
                                for (int i=1; i<=vertical_spread; i++) {
                                    if (y+i < buf_height && right_min_x[y+i] > x)
                                        right_min_x[y+i] = x;
                                    if (y-i >= 0 && right_min_x[y-i] > x)
                                        right_min_x[y-i] = x;
                                }
                            }
                            if (*bucket > x) {
                                *bucket = x;
                                break; // No need to scan more of this line
                            }
                        }
                        tmp++;
                    }
                }
            }
        }
        else {
            for ( int y=y0; y<y1; y++ ) {
                if ( y >= 0 && y < buf_height ) {
                    int * const bucket = by_line ? &left_max_x[y] : &whole_left_max_x;
                    // Drawing a left word glyph: we want to catch its right edge:
                    // scan from the right to limit the amount of loops
                    const lUInt8 * __restrict tmp = bitmap + (y-y0+1)*width - 1;
                    for ( int x=x1-1; x>=x0; x-- ) {
                        if (*tmp >= min_opacity) {
                            if ( by_line && vertical_spread > 0 ) {
                                for (int i=1; i<=vertical_spread; i++) {
                                    if (y+i < buf_height && left_max_x[y+i] < x)
                                        left_max_x[y+i] = x;
                                    if (y-i >= 0 && left_max_x[y-i] < x)
                                        left_max_x[y-i] = x;
                                }
                            }
                            if (*bucket < x) {
                                *bucket = x;
                                break; // No need to scan more of this line
                            }
                        }
                        tmp--;
                    }
                }
            }
        }
    }
    int getDistance() {
        int min_distance = 0x7FFFFFFF;
        if (by_line) {
            for (int i=0; i<buf_height; i++) {
                // if right_min_x = left_max_x, they overlap, so this -1
                int distance = right_min_x[i] - left_max_x[i] - 1;
                if (min_distance > distance) {
                    min_distance = distance;
                }
            }
        }
        else {
            min_distance = whole_right_min_x - whole_left_max_x;
        }
        return min_distance;
    }
    void DrawingRight(bool right=true) {
        drawing_right = right;
    }
    /// create own draw buffer
    explicit LVHorizontalOverlapMeasurementDrawBuf( int h, bool byline, int vertspread=0, lUInt8 minopacity=1 )
            : LVInkMeasurementDrawBuf(false, false), buf_height(h), by_line(byline), vertical_spread(vertspread)
            , min_opacity(minopacity), drawing_right(false)
    {
        if ( by_line ) {
            left_max_x = (int*)malloc( sizeof(int) * buf_height );
            right_min_x = (int*)malloc( sizeof(int) * buf_height );
            for (int i=0; i<buf_height; i++) {
                left_max_x[i] = - 0x0FFFFFFF; // -infinity
                right_min_x[i] = 0x0FFFFFFF;  // +infinity
            }
        }
        else {
            whole_left_max_x = - 0x0FFFFFFF;
            whole_right_min_x = 0x0FFFFFFF;
        }
    }
    /// destructor
    virtual ~LVHorizontalOverlapMeasurementDrawBuf() {
        if ( by_line ) {
            free(left_max_x);
            free(right_min_x);
        }
    }
};

#endif  // __LVINKMEASUREMENTDRAWBUF_H_INCLUDED__
