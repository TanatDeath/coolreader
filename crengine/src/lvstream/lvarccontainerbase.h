/** @file lvarccontainerbase.h

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

#ifndef __LVARCCONTAINERBASE_H_INCLUDED__
#define __LVARCCONTAINERBASE_H_INCLUDED__

#include "lvnamedcontainer.h"
#include "lvstring.h"

class LVArcContainerBase : public LVNamedContainer
{
protected:
    LVContainer * m_parent;
    LVStreamRef m_stream;
public:
    virtual LVStreamRef OpenStream( const char32_t *, lvopen_mode_t )
    {
        return LVStreamRef();
    }
    virtual LVContainer * GetParentContainer()
    {
        return (LVContainer*)m_parent;
    }
    virtual lverror_t GetSize( lvsize_t * pSize )
    {
        if (m_fname.empty())
            return LVERR_FAIL;
        *pSize = GetObjectCount();
        return LVERR_OK;
    }
    LVArcContainerBase( LVStreamRef stream ) : m_parent(NULL), m_stream(stream)
    {
    }
    virtual ~LVArcContainerBase()
    {
        SetName(NULL);
    }
    virtual int ReadContents() = 0;

};

#endif  // __LVARCCONTAINERBASE_H_INCLUDED__
