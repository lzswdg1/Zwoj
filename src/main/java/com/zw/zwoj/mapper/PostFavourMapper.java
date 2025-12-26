package com.zw.zwoj.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.bean.PostFavour;
import io.lettuce.core.dynamic.annotation.Param;

public interface PostFavourMapper extends BaseMapper<PostFavour> {
    Page<Post> listFavourPostByPage(IPage<Post> post, @Param(Constants.WRAPPER)  Wrapper<Post> queryWrapper,
                                    long favourUserId);
}
