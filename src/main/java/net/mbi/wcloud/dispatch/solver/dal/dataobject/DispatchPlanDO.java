package net.mbi.wcloud.dispatch.solver.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_plan")
public class DispatchPlanDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String planCode;

    private String status;
    private String message;

    private Integer timeLimitSec;
    private Long unassignedPenalty;
    private Integer allowDrop;

    private Long totalDistanceM;
    private Long totalTimeSec;
    private Integer assignedCount;
    private Integer unassignedCount;
    private Long solveMillis;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
