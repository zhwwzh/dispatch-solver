package net.mbi.wcloud.dispatch.solver.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_solve_job")
public class DispatchSolveJobDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long planId;

    private String taskId;
    private String status;
    private String message;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
