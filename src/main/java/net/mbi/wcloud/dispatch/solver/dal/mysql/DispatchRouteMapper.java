package net.mbi.wcloud.dispatch.solver.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchRouteDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DispatchRouteMapper extends BaseMapper<DispatchRouteDO> {
}
