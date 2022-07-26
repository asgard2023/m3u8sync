package org.ccs.m3u8sync.downup.service;


import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.constants.RedisTimeType;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DownErrorService {
    private static Map<String, DownErrorInfoVo> errorInfoMap = new ConcurrentHashMap<>();
    @Cacheable(value = RedisTimeType.CACHE1W, key = "'m3u8:getDownError:'+#roomId")
    public DownErrorInfoVo getDownError(String roomId){
        return null;
    }

    public DownErrorInfoVo getDownErrorLocalCache(String roomId){
        return errorInfoMap.get(roomId);
    }

    @CachePut(value = RedisTimeType.CACHE1W, key = "'m3u8:getDownError:'+#roomId")
    public DownErrorInfoVo putDownError(String roomId, DownErrorInfoVo downError){
        Map<String, Integer> countMap=new HashMap<>();
        Map<String, AtomicInteger> counterMap=downError.getTsErrorCounterMap();
        Set<Map.Entry<String, AtomicInteger>> counterSet=counterMap.entrySet();
        for(Map.Entry<String, AtomicInteger> counter: counterSet){
            countMap.put(counter.getKey(), counter.getValue().get());
        }
        counterMap.clear();
        if(downError.getErrUrls()!=null){
            downError.getErrUrls().clear();
        }
        if(downError.getSuccessLines()!=null){
            downError.getSuccessLines().clear();
        }
        if(downError.getSuccessLineMap()!=null){
            downError.getSuccessLineMap().clear();
        }
        errorInfoMap.put(roomId, downError);
        return downError;
    }

    @CacheEvict(value = RedisTimeType.CACHE1W, key = "'m3u8:getDownError:'+#roomId")
    public void getDownError_evict(String roomId){
        errorInfoMap.remove(roomId);
    }
}
