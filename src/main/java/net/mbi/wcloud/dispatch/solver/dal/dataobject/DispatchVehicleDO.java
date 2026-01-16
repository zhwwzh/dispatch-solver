package net.mbi.wcloud.dispatch.solver.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_vehicle")
public class DispatchVehicleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String vehicleCode;

    private Long startNodeId;
    private Long endNodeId;

    private Integer capacityWeight;
    private Integer capacityVolume;

    private Integer workStartSec;
    private Integer workEndSec;

    private Long fixedCost;
    private Double costPerMeter;

    private String status;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
