package com.zw.zwoj.job.once;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class FullSyncPostToEs {
    
    @Resource
    private PostService postService
}
