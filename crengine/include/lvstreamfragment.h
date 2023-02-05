/** @file lvstreamfragment.h

    CoolReader Engine

    (c) Vadim Lopatin, 2000-2006
    This source code is distributed under the terms of
    GNU General Public License.
    See LICENSE file for details.

 * You must obey the GNU General Public License in all respects
 * for all of the code used other than OpenSSL.  If you modify
 * file(s) with this exception, you may extend this exception to your
 * version of the file(s), but you are not obligated to do so.  If you
 * do not wish to do so, delete this exception statement from your
 * version.  If you delete this exception statement from all source
 * files in the program, then also delete it here.

*/

#ifndef __LVSTREAMFRAGMENT_H_INCLUDED__
#define __LVSTREAMFRAGMENT_H_INCLUDED__

#include "lvnamedstream.h"

class LVStreamFragment : public LVNamedStream
{
private:
    LVStreamRef m_stream;
    lvsize_t    m_start;
    lvsize_t    m_size;
    lvpos_t     m_pos;
public:
    LVStreamFragment( LVStreamRef stream, lvsize_t start, lvsize_t size )
        : m_stream(stream), m_start(start), m_size(size), m_pos(0)
    {
    }
    virtual lvopen_mode_t GetMode() {
        lvopen_mode_t mode = m_stream->GetMode();
        switch (m_mode) {
            case LVOM_ERROR:
            case LVOM_CLOSED:
            case LVOM_READ:
                return mode;
            case LVOM_READWRITE:
                return LVOM_READ;
            case LVOM_WRITE:
            case LVOM_APPEND:
            default:
                return LVOM_ERROR;
        }
    }
    virtual bool Eof()
    {
        return m_pos >= m_size;
    }
    virtual lvsize_t GetSize()
    {
        return m_size;
    }

    virtual lverror_t Seek(lvoffset_t offset, lvseek_origin_t origin, lvpos_t* newPos)
    {
        lvpos_t npos;
        switch (origin) {
            case LVSEEK_SET:
                npos = offset;
                break;
            case LVSEEK_CUR:
                npos = m_pos + offset;
                break;
            case LVSEEK_END:
                npos = m_size + offset;
                break;
            default:
                return LVERR_FAIL;
        }
        if ( npos > m_size )
            return LVERR_FAIL;
        lverror_t res = m_stream->Seek( npos + m_start, LVSEEK_SET, NULL );
        if ( res != LVERR_OK )
            return res;
        m_pos = npos;
        if ( newPos )
            *newPos = npos;
        return LVERR_OK;
    }
    virtual lverror_t Tell( lvpos_t * pPos ) {
        if ( pPos )
            *pPos = m_pos;
        return LVERR_OK;
    }
    virtual lverror_t Write(const void*, lvsize_t, lvsize_t*)
    {
        return LVERR_NOTIMPL;
    }
    virtual lverror_t Read(void* buf, lvsize_t size, lvsize_t* pBytesRead)
    {
        lverror_t res = m_stream->Seek( m_pos + m_start, LVSEEK_SET, NULL );
        if ( res != LVERR_OK )
            return res;
        lvsize_t bytesRead = 0;
        res = m_stream->Read( buf, size + m_pos > m_size ? m_size - m_pos : size, &bytesRead );
        if ( res != LVERR_OK )
            return res;
        m_pos += bytesRead;
        if ( pBytesRead )
            *pBytesRead = bytesRead;
        return LVERR_OK;
    }
    virtual lverror_t SetSize(lvsize_t)
    {
        return LVERR_NOTIMPL;
    }
};

#endif  // __LVSTREAMFRAGMENT_H_INCLUDED__
