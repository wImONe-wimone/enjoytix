package com.wimone.enjoytix.performance.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Venue creation request.")
public class VenueCreateReqDTO {

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Venue name.", example = "Enjoy Arena")
    private String name;

    @Size(max = 64)
    @Schema(description = "Country.", example = "China")
    private String country;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Province.", example = "Beijing")
    private String province;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "City.", example = "Beijing")
    private String city;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "District or county.", example = "Chaoyang")
    private String district;

    @Size(max = 64)
    @Schema(description = "Town.")
    private String town;

    @Size(max = 64)
    @Schema(description = "Village.")
    private String village;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Street.", example = "Futong East Street")
    private String street;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "House number.", example = "6")
    private String houseNumber;

    @Size(max = 128)
    @Schema(description = "Estate or residential village.")
    private String estate;

    @Size(max = 128)
    @Schema(description = "Building.")
    private String building;

    @Size(max = 255)
    @Schema(description = "Formatted venue address. If empty, the service composes it from address parts.")
    private String address;

    @Min(1)
    @Max(200)
    @Schema(description = "Seat map row count. Defaults to 10 when absent.", example = "20")
    private Integer seatRowCount;

    @Min(1)
    @Max(200)
    @Schema(description = "Seat map column count. Defaults to 10 when absent.", example = "30")
    private Integer seatColumnCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getEstate() {
        return estate;
    }

    public void setEstate(String estate) {
        this.estate = estate;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getSeatRowCount() {
        return seatRowCount;
    }

    public void setSeatRowCount(Integer seatRowCount) {
        this.seatRowCount = seatRowCount;
    }

    public Integer getSeatColumnCount() {
        return seatColumnCount;
    }

    public void setSeatColumnCount(Integer seatColumnCount) {
        this.seatColumnCount = seatColumnCount;
    }
}
