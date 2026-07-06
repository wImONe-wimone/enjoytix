package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User address creation request.")
public class UserAddressCreateReqDTO {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Receiver name.", example = "Alice")
    private String receiverName;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Receiver mobile.", example = "13800000000")
    private String receiverMobile;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Province.", example = "Shanghai")
    private String province;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "City.", example = "Shanghai")
    private String city;

    @Size(max = 64)
    @Schema(description = "District.", example = "Pudong")
    private String district;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Detailed street address.", example = "No. 1888 Expo Avenue")
    private String detailAddress;

    @Size(max = 16)
    @Schema(description = "Postal code.", example = "200120")
    private String postalCode;

    @Schema(description = "Default address flag, 1 means default.", example = "0")
    private Integer defaultFlag = 0;

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverMobile() {
        return receiverMobile;
    }

    public void setReceiverMobile(String receiverMobile) {
        this.receiverMobile = receiverMobile;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Integer getDefaultFlag() {
        return defaultFlag;
    }

    public void setDefaultFlag(Integer defaultFlag) {
        this.defaultFlag = defaultFlag;
    }
}
