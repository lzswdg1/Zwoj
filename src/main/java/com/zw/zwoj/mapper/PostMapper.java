package com.zw.zwoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zw.zwoj.model.bean.Post;

import java.util.Date;
import java.util.List;

public interface PostMapper extends BaseMapper<Post> {
    List<Post> listPostWithDelete(Date minUpdateTime);
}
