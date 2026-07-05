package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Attendee creation request.")
public class AttendeeCreateReqDTO {

    @NotBlank
    @Schema(description = "Real name of attendee.", example = "Alice")
    private String realName;

    @NotBlank
    @Schema(description = "Certificate type.", example = "ID_CARD")
    private String certificateType;

    @NotBlank
    @Schema(description = "Certificate number.", example = "110101199001011234")
    private String certificateNo;

    @NotBlank
    @Schema(description = "Mobile number.", example = "13800000000")
    private String mobile;

    @Schema(description = "Default attendee flag, 1 means default.", example = "0")
    private Integer defaultFlag = 0;

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
}
