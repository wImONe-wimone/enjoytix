package com.wimone.enjoytix.user.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User address response.")
public class UserAddressRespDTO {

    @Schema(description = "Address id.", example = "21")
    private Long addressId;
    @Schema(description = "Owner user id.", example = "1")
    private Long userId;
    @Schema(description = "Receiver name.", example = "Alice")
    private String receiverName;
    @Schema(description = "Receiver mobile.", example = "13800000000")
    private String receiverMobile;
    @Schema(description = "Province.", example = "Shanghai")
    private String province;
    @Schema(description = "City.", example = "Shanghai")
    private String city;
    @Schema(description = "District.", example = "Pudong")
    private String district;
    @Schema(description = "Detailed street address.", example = "No. 1888 Expo Avenue")
    private String detailAddress;
    @Schema(description = "Postal code.", example = "200120")
    private String postalCode;
    @Schema(description = "Default address flag.", example = "1")
    private Integer defaultFlag;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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
