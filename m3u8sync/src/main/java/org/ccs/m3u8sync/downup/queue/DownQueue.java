package org.ccs.m3u8sync.downup.queue;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.downup.domain.DownBean;
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

    /** 永久key */
    private static final String HASH_KEY = "nfsync:downup:downhash";
    private static final String LIST_KEY = "nfsync:downup:downlist";
    private static final String ERR_LIST_KEY = "nfsync:downup:downerrlist";

    /**
     * 将下载任务放入到队列中,默认是插入到队尾
     * @param downBean
     */
    public void put(DownBean downBean){
        String roomId = downBean.getRoomId();
        if(redisTemplate.boundHashOps(HASH_KEY).hasKey(roomId)){
            log.info("重复收到下载任务,DownBean={}",downBean);
        }
        redisTemplate.boundHashOps(HASH_KEY).put(roomId,downBean);
        redisTemplate.boundListOps(LIST_KEY).rightPush(roomId);
    }


    /**
     * 获取指定的bean
     * @param roomId
     * @return
     */
    public DownBean get(String roomId){
        return (DownBean) redisTemplate.boundHashOps(HASH_KEY).get(roomId);
    }

    /**
     * 获取一个任务, 非阻塞; 交由业务组阻塞; 默认是从队列头部获取
     * @return
     */
    public DownBean getOne(){
        String roomId = (String) redisTemplate.boundListOps(LIST_KEY).leftPop();
        if(CharSequenceUtil.isBlank(roomId)){
            return null;
        }
        return (DownBean) redisTemplate.boundHashOps(HASH_KEY).get(roomId);
    }

    /**
     * 移动所有失败的任务到正常队列头部
     * @return
     */
    public void moveErr(){
        log.info("开始从失败队列迁移到正常队列中");
        int i = 0;
        while(true){
            String roomId = (String) redisTemplate.boundListOps(ERR_LIST_KEY).leftPop();
            if(CharSequenceUtil.isBlank(roomId)){
                log.info("结束从失败队列迁移到正常队列中,一共迁移{}个",i);
                break;
            }
            i++;
            redisTemplate.boundListOps(LIST_KEY).leftPush(roomId);
            log.info("roomId={}从异常队列插入到正常队列的头部,以供优先选择",roomId);
        }
    }

    /**
     * 同步中断的任务,从Hash的长度和queue的size是否一致来判定
     * warn: 该方法不能频繁调度,且不适用海量hash任务时使用
     * TODO 如果后续部署集群,此处需要修改为分布式锁
     */
    public synchronized void syncBreak(){
        Long hashSize = redisTemplate.boundHashOps(HASH_KEY).size();
        if(hashSize == 0){
            return;
        }
        Long listSize =  redisTemplate.boundListOps(LIST_KEY).size();
        Long errlistSize = redisTemplate.boundListOps(ERR_LIST_KEY).size();
        if(!hashSize.equals (listSize + errlistSize)){
            log.info("检测到有异常中断的任务,任务全集size={},待执行任务size={},失败任务size={}", listSize, errlistSize);
        }
        //将Hash有,但是queue没有的进行同步
        Set<String> all = redisTemplate.boundHashOps(HASH_KEY).keys();
        List<String> successList = redisTemplate.boundListOps(LIST_KEY).range(0,listSize-1);
        List<String> errList = redisTemplate.boundListOps(ERR_LIST_KEY).range(0,errlistSize-1);
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



    public Long size(){
        return  redisTemplate.boundListOps(LIST_KEY).size();
    }

    public Long errSize(){
        return  redisTemplate.boundListOps(ERR_LIST_KEY).size();
    }

    public List<String> errors(){
        return  redisTemplate.boundListOps(ERR_LIST_KEY).range(0, 20);
    }

    public void delete(String roomId){
        redisTemplate.boundHashOps(HASH_KEY).delete(roomId);
    }
}
