package com.enterprise.platform.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.platform.demo.entity.BizOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BizOrderMapper extends BaseMapper<BizOrder> {
}
