package com.wimone.enjoytix.user.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Attendee response.")
public class AttendeeRespDTO {

    @Schema(description = "Attendee id.", example = "1001")
    private Long attendeeId;
    @Schema(description = "Owner user id.", example = "1")
    private Long userId;
    @Schema(description = "Real name of attendee.", example = "Alice")
    private String realName;
    @Schema(description = "Certificate type.", example = "ID_CARD")
    private String certificateType;
    @Schema(description = "Certificate number.", example = "110101199001011234")
    private String certificateNo;
    @Schema(description = "Mobile number.", example = "13800000000")
    private String mobile;
    @Schema(description = "Default attendee flag.", example = "0")
    private Integer defaultFlag;
    @Schema(description = "Attendee status.", example = "1")
    private Integer status;

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getCertificateNo() {
        return certificateNo;
    }

    public void setCertificateNo(String certificateNo) {
        this.certificateNo = certificateNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getDefaultFlag() {
        return defaultFlag;
    }

    public void setDefaultFlag(Integer defaultFlag) {
        this.defaultFlag = defaultFlag;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
