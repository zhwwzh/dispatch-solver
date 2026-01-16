package net.mbi.wcloud.dispatch.solver.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_task")
public class DispatchTaskDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long planId;

    private String taskCode;

    private Long nodeId;
    private Double lat;
    private Double lng;

    private Integer twStartSec;
    private Integer twEndSec;

    private Integer serviceTimeSec;
    private Integer demandWeight;
    private Integer demandVolume;

    private Integer priority;
    private String status;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
