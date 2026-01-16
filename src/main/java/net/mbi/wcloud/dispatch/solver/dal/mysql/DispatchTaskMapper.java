package net.mbi.wcloud.dispatch.solver.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.mbi.wcloud.dispatch.solver.dal.dataobject.DispatchTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DispatchTaskMapper extends BaseMapper<DispatchTaskDO> {
}
