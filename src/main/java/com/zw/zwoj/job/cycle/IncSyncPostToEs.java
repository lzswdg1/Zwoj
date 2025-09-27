package com.zw.zwoj.job.cycle;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zw.zwoj.mapper.PostMapper;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.dto.post.PostEsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.zw.zwoj.esdao.PostEsDao;
import com.zw.zwoj.model.dto.post.PostEsDTO;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IncSyncPostToEs {
    
    @Resource
    private PostMapper postMapper;
    
    @Resource
    private PostEsDao postEsDao;
    
    @Scheduled(fixedRate = 60* 1000)
    public void run(){
        Date fiveMinutesAgoDate = new Date(new Date().getTime() - 5*60*1000L);
        List<Post> postList = postMapper.listPostWithDelete(fiveMinutesAgoDate);
        if(CollectionUtils.isEmpty(postList)){
            log.info("no inc post");
            return;
        }
        List<PostEsDTO> postEsDTOList = postList.stream()
                .map(PostEsDTO::objToDto)
                .collect(Collectors.toList());
        final int pageSize = 500;
        int total = postEsDTOList.size();
        log.info("IncSyncPostToEs start, total {}", total);
        for(int i=0;i<total;i+=pageSize){
            int end =Math.min(i+pageSize,total);
            log.info("sync from {} to {}", i,end);
            postEsDao.saveAll(postEsDTOList.subList(i,end));
        }
        log.info("IncSyncPostToEs end, total {}", total);
    }
}
