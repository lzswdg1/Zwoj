package com.zw.zwoj.model.dto.postfavour;

import co.elastic.clients.elasticsearch.license.PostRequest;
import com.zw.zwoj.model.dto.post.PostQueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper= true)
public class PostFavourQueryRequest extends PostRequest implements Serializable {
    /**
     * 帖子查询请求
     */
    private PostQueryRequest postQueryRequest;
    
    /**
     * 用户 id
     */
    private Long userId;
    
    private static final long serialVersionUID = 1L;
}
