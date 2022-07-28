package org.ccs.m3u8sync.downup.service;


import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.constants.RedisTimeType;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 正在下载或异常的下载任务信息
 *
 * @author chenjh
 */
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
        Map<String, Integer> countMap=new HashMap<>(downError.getTotalTsNums());
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
    public void getDownErrorEvict(String roomId){
        errorInfoMap.remove(roomId);
    }


    public Collection<DownErrorInfoVo> errorInfos(){
        return errorInfoMap.values();
    }
}
