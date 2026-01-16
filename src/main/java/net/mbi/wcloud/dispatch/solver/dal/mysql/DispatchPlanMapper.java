package net.mbi.wcloud.dispatch.solver.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DispatchPlanMapper extends BaseMapper<DispatchPlanDO> {
}
