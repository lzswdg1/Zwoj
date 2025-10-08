package com.zw.zwoj.model.dto.postfavour;

import com.zw.zwoj.common.PageRequest;
import com.zw.zwoj.model.dto.post.PostQueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper= true)
public class PostFavourQueryRequest extends PageRequest implements Serializable {
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
