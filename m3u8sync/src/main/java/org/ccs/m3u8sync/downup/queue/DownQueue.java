package org.ccs.m3u8sync.downup.queue;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.downup.domain.DownBean;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.ccs.m3u8sync.downup.service.DownErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 下载队列，收到下载任务后，存放的下载队列；
 */
@Service
@Slf4j
public class DownQueue {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DownErrorService downErrorService;

    /** 永久key */
    private static final String HASH_KEY = "m3u8sync:downup:downhash";
    private static final String LIST_KEY = "m3u8sync:downup:downlist";
    private static final String ERR_LIST_KEY = "m3u8sync:downup:downerrlist";

    /**
     * 将下载任务放入到队列中,默认是插入到队尾
     * @param downBean
     */
    public void put(DownBean downBean){
        String roomId = downBean.getRoomId();
        boolean hasRoomId=redisTemplate.opsForHash().hasKey(HASH_KEY, roomId);
        if(hasRoomId){
            log.info("重复收到下载任务,DownBean={}",downBean);
        }
        redisTemplate.opsForHash().put(HASH_KEY, roomId,downBean);
        redisTemplate.opsForList().rightPush(LIST_KEY, roomId);
    }


    /**
     * 获取指定的bean
     * @param roomId
     * @return
     */
    public DownBean get(String roomId){
        return (DownBean) redisTemplate.opsForHash().get(HASH_KEY, roomId);
    }

    /**
     * 获取一个任务, 非阻塞; 交由业务组阻塞; 默认是从队列头部获取
     * @return
     */
    public DownBean getOne(){
        String roomId = (String) redisTemplate.opsForList().leftPop(LIST_KEY);
        if(StringUtils.isBlank(roomId)){
            return null;
        }
        return (DownBean) redisTemplate.opsForHash().get(HASH_KEY, roomId);
    }

    /**
     * 移动所有失败的任务到正常队列头部
     * @return
     */
    public int moveErr(){
        log.info("开始从失败队列迁移到正常队列中");
        int i = 0;
        while(true){
            String roomId = (String) redisTemplate.opsForList().leftPop(ERR_LIST_KEY);
            if(StringUtils.isBlank(roomId)){
                log.info("结束从失败队列迁移到正常队列中,一共迁移{}个",i);
                break;
            }
            i++;
            redisTemplate.opsForList().leftPush(LIST_KEY, roomId);
            log.info("roomId={}从异常队列插入到正常队列的头部,以供优先选择",roomId);
        }
        return i;
    }

    /**
     * 同步中断的任务,从Hash的长度和queue的size是否一致来判定
     * warn: 该方法不能频繁调度,且不适用海量hash任务时使用
     */
    public synchronized void syncBreak(){
        Long hashSize = redisTemplate.opsForHash().size(HASH_KEY);
        if(hashSize == 0){
            return;
        }
        Long listSize =  redisTemplate.opsForList().size(LIST_KEY);
        Long errlistSize = redisTemplate.opsForList().size(ERR_LIST_KEY);
        log.info("检测到有异常中断的任务,任务全集size={},待执行任务size={},失败任务size={}", hashSize, listSize, errlistSize);
        //将Hash有,但是queue没有的进行同步
        Set<String> all = redisTemplate.opsForHash().keys(HASH_KEY);
        List<String> successList = redisTemplate.opsForList().range(LIST_KEY, 0,listSize-1);
        List<String> errList = redisTemplate.opsForList().range(ERR_LIST_KEY, 0,errlistSize-1);
        Collection<String> main = CollUtil.union(successList,errList);
        Collection<String> del = CollUtil.subtract(all,main);
        log.info("一共需要同步{}个中断任务," , del.size());
        for (String roomId : del) {
            log.info("任务{}恢复到正常队列的头部中",roomId);
            redisTemplate.boundListOps(LIST_KEY).leftPush(roomId);
        }
    }


    /**
     * 将下载任务放入到失败队列中,以待恢复
     * @param roomId
     */
    public void putErr(String roomId){
        redisTemplate.boundListOps(ERR_LIST_KEY).rightPush(roomId);
    }

    public void putErr(String roomId, DownErrorInfoVo downErrorInfoVo){
        redisTemplate.boundListOps(ERR_LIST_KEY).rightPush(roomId);
        downErrorService.putDownError(roomId, downErrorInfoVo);
    }



    public Long size(){
        return  redisTemplate.opsForList().size(LIST_KEY);
    }

    public Long errSize(){
        return  redisTemplate.opsForList().size(ERR_LIST_KEY);
    }

    public List errors(int count){
        return  redisTemplate.boundListOps(ERR_LIST_KEY).range(0, count);
    }
    public List queues(int count){
        return  redisTemplate.boundListOps(LIST_KEY).range(0, count);
    }

    public void delete(String roomId){
        redisTemplate.boundHashOps(HASH_KEY).delete(roomId);
        downErrorService.getDownErrorEvict(roomId);
    }
}
