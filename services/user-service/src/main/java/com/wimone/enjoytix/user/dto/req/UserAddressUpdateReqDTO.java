package com.wimone.enjoytix.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "User address update request.")
public class UserAddressUpdateReqDTO extends UserAddressCreateReqDTO {

    @NotNull
    @Schema(description = "Address id.", example = "21")
    private Long addressId;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }
}
