package net.mbi.wcloud.dispatch.solver.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dispatch_route")
public class DispatchRouteDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long planId;

    private Long vehicleId;
    private Long driverId;

    private Long totalDistanceM;
    private Long totalTimeSec;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
