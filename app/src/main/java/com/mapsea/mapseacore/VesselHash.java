package com.mapsea.mapseacore;

public class VesselHash {
    public VesselNode[] _Table = new VesselNode[MSFINAL.DIV_PRIME];
    private int _outerIndex = 0;
    private int _innerIndex = 0;

    //생성자 VesselNode 배열을 초기화
    public VesselHash()
    {
        for(int i = 0; i < _Table.length; i++)
        {
            _Table[i] = new VesselNode();
        }
    }

    //해시 값을 계산
    public static int Hashing(String mmsi)
    {
        String paddedmmsi = mmsi;
        while(paddedmmsi.length() < MSFINAL.MMSI_LENGTH) {
            paddedmmsi = paddedmmsi + "0";
        }

        String tmp = paddedmmsi.substring(3, 9);
        long tmpInt = Integer.parseUnsignedInt(tmp);
        tmpInt = tmpInt * tmpInt;
        return (int)(tmpInt % MSFINAL.DIV_PRIME);
    }

    /**
    - 선박 정보를 해시 테이블에 업데이트<br>
    - 기존 mmsi가 없으면 새 위치에 저장하고
    있으면 기존 위치에 데이터 덮어씌움
     */
    public void AddOrRefresh(Vessel vessel)
    {
        if(vessel._sta == null && vessel._pos == null)
        {
            return;
        }
        int index = Seek(vessel);
        if(index > -1)
        {
            if(vessel._pos != null)
            {
                if(_Table[vessel._hash].get(index)._pos != null)
                {
                    _Table[vessel._hash].get(index)._prevPos = _Table[vessel._hash].get(index)._pos;
                }
                _Table[vessel._hash].get(index)._pos = vessel._pos;
            }

            if(vessel._sta != null)
            {
                _Table[vessel._hash].get(index)._sta = vessel._sta;
            }
            //_Table[vessel._hash].get(index)._mmsi = vessel._mmsi;
            //_Table[vessel._hash].get(index)._hash = vessel._hash;
            //_Table[vessel._hash].get(index)._class = vessel._class;
            //_Table[vessel._hash].get(index)._sender = vessel._sender;
        }
        else
        {
            _Table[vessel._hash].add(vessel);
        }
    }

    /** 순차적으로 모든 선박 테이블을 순회하여 다음 노드를 탐색 */
    public Vessel Next()
    {
        for(; _outerIndex < _Table.length; _outerIndex++)
        {
            if (_innerIndex < _Table[_outerIndex].size())
            {
                int tmpInner = _innerIndex;
                int tmpOuter = _outerIndex;
                _innerIndex++;
                if (_innerIndex >= _Table[_outerIndex].size())
                {
                    _innerIndex = 0;
                    _outerIndex++;
                }
                return _Table[tmpOuter].get(tmpInner);
            }
        }
        return null;
    }

    //현재 탐색 중인 해시 인덱스를 리셋함
    public void Reset()
    {
        _outerIndex = 0;
        _innerIndex = 0;
    }

    // 넘겨 받은 선박의 내부 인덱스를 반환 받음(외부 인덱스는 hash 값으로 바로 판단 가능)
    public int Seek(Vessel vessel)
    {
        for(int i = 0; i < _Table[vessel._hash].size(); i++)
        {
            if(_Table[vessel._hash].get(i)._mmsi.equals(vessel._mmsi))
            {
                return i;
            }
        }
        return -1;
    }

    // 넘겨 받은 mmsi의 선박 데이터를 반환함
    public Vessel Seek(String mmsi)
    {
        int hash = Hashing(mmsi);
        for(int i = 0; i < _Table[hash].size(); i++)
        {
            if(_Table[hash].get(i)._mmsi.equals(mmsi))
            {
                return _Table[hash].get(i);
            }
        }
        return null;
    }
}
